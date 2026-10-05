const express = require('express');
const { transaction } = require('../lib/transaction');
const { randomBytes, createHash } = require('node:crypto');
const { SurveyService } = require('../services/survey.service');
const auth = require('../middlewares/auth.middleware');

const allowedAmounts = [1000, 3000, 5000];

function fail(status, message, code) {
  throw Object.assign(new Error(message), { status, ...(code ? { code } : {}) });
}

function hashToken(token) {
  return createHash('sha256').update(token).digest('hex');
}

function requireToken(token) {
  if (
    typeof token !== 'string' ||
    !/^[a-f0-9]{64}$/.test(token)
  ) {
    fail(400, '주문 확인 토큰이 올바르지 않습니다.');
  }
}

function publicBaseUrl() {
  const value = process.env.PAYMENT_PUBLIC_BASE_URL?.trim();

  if (!value) {
    fail(503, '결제창 연결 주소가 설정되지 않았습니다.', 'PAYMENT_URL_NOT_CONFIGURED');
  }

  return value.replace(/\/+$/, '');
}

function kakaoConfig() {
  const secretKey = process.env.KAKAOPAY_SECRET_KEY?.trim();
  const cid = process.env.KAKAOPAY_CID?.trim();

  if (!secretKey || !cid) {
    fail(503, '카카오페이 결제 연동 설정이 완료되지 않았습니다.', 'KAKAO_NOT_CONFIGURED');
  }

  return { secretKey, cid };
}

function tossClientKey() {
  const key = process.env.TOSS_CLIENT_KEY?.trim();

  if (!key?.startsWith('test_ck_')) {
    fail(503, '토스 결제 연동 설정이 완료되지 않았습니다.', 'TOSS_NOT_CONFIGURED');
  }

  return key;
}

async function kakaoRequest(action, body) {
  const { secretKey, cid } = kakaoConfig();

  const response = await fetch(
    `https://open-api.kakaopay.com/online/v1/payment/${action}`,
    {
      method: 'POST',
      headers: {
        Authorization: `SECRET_KEY ${secretKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ ...body, cid }),
      signal: AbortSignal.timeout(15000),
    },
  );

  const result = await response.json();

  if (!response.ok) {
    const error = Object.assign(
      new Error('카카오페이 요청이 거절됐습니다.'),
      {
        status: 502,
        providerRejected: true,
        providerCode: result.error_code ?? result.code,
        providerMessage:
          result.error_message ?? result.msg ?? result.message,
      },
    );

    throw error;
  }

  return result;
}

function createSurveyRouter(service = new SurveyService()) {
  const router = express.Router();
  const prisma = service.prisma;

  const handle = (status, action) => async (req, res, next) => {
    try {
      res.status(status).json(await action(req));
    } catch (error) {
      next(error);
    }
  };

  router.get('/health', handle(200, async () => {
    await prisma.$queryRaw`SELECT 1`;
    return { status: 'ok', database: 'connected' };
  }));

  // 브라우저 결제 페이지의 응답 설정
  function paymentPage(req, res, next) {
    res.set('Cache-Control', 'no-store');
    res.set('Referrer-Policy', 'no-referrer');
    next();
  }

  // 콜백 토큰으로 주문 확인
  async function callbackOrder(orderId, token, provider) {
    if (typeof orderId !== 'string' || !orderId) {
      fail(400, '주문 ID가 필요합니다.');
    }

    requireToken(token);

    const order = await prisma.paymentOrder.findFirst({
      where: {
        id: orderId,
        provider,
        callbackHash: hashToken(token),
      },
    });

    if (!order) {
      fail(404, '충전 주문을 찾을 수 없습니다.');
    }

    return order;
  }

  // 주문 완료 / 포인트 증가 / 적립 내역을 함께 저장
  async function creditOrder(order, approvedAtValue, description) {
    const now = new Date();
    const approvedAt = approvedAtValue
      ? new Date(approvedAtValue)
      : now;

    await transaction(prisma, async tx => {
      const credited = await tx.paymentOrder.updateMany({
        where: {
          id: order.id,
          creditedAt: null,
          status: {
            in: ['READY', 'APPROVING', 'APPROVAL_UNKNOWN'],
          },
        },
        data: {
          status: 'COMPLETED',
          approvedAt: Number.isNaN(approvedAt.getTime())
            ? now
            : approvedAt,
          creditedAt: now,
        },
      });

      // 이미 다른 요청에서 지급했다면 추가 지급하지 않음
      if (credited.count !== 1) return;

      await tx.user.update({
        where: { id: order.userId },
        data: {
          point: { increment: order.creditPoint },
        },
      });

      await tx.pointHistory.create({
        data: {
          userId: order.userId,
          amount: order.creditPoint,
          description,
          paymentOrderId: order.id,
        },
      });
    });
  }

  // ─────────────────────────────
  // 회원 인증
  // ─────────────────────────────

  router.post(
    '/auth/signup',
    handle(201, req => service.signup(req.body || {})),
  );

  router.post(
    '/auth/login',
    handle(200, req => service.login(req.body || {})),
  );

  router.post('/auth/verify-password', auth,
    handle(200, req => service.verifyPassword(req.user.userId, req.body?.password)));

  router.post(
    '/auth/logout',
    auth,
    handle(200, () => ({
      message: 'Discard the access token on the client',
    })),
  );

  // ─────────────────────────────
  // 내 정보 / 포인트
  // ─────────────────────────────

  router.get(
    '/users/me',
    auth,
    handle(200, req => service.getUserMe(req.user.userId)),
  );

  router.patch(
    '/users/me',
    auth,
    handle(200, req =>
      service.updateUserMe(req.user.userId, req.body || {}),
    ),
  );

  router.get(
    '/users/me/responses',
    auth,
    handle(200, req => service.getUserResponses(req.user.userId)),
  );

  router.get(
    '/users/me/points',
    auth,
    handle(200, req => service.getUserPoints(req.user.userId)),
  );

  // ─────────────────────────────
  // 설문
  // ─────────────────────────────

  router.get(
    '/surveys',
    handle(200, req => service.getSurveys(req.query)),
  );

  router.get(
    '/surveys/:id',
    handle(200, req => service.getSurveyDetail(req.params.id)),
  );

  router.post(
    '/surveys',
    auth,
    handle(201, req =>
      service.createSurvey(req.user.userId, req.body || {}),
    ),
  );

  router.patch(
    '/surveys/:id',
    auth,
    handle(200, req =>
      service.updateSurvey(
        req.user.userId,
        req.params.id,
        req.body || {},
      ),
    ),
  );

  router.delete(
    '/surveys/:id',
    auth,
    handle(200, req =>
      service.deleteSurvey(req.user.userId, req.params.id),
    ),
  );

  // ─────────────────────────────
  // 설문 문항
  // ─────────────────────────────

  router.post(
    '/surveys/:id/questions',
    auth,
    handle(201, req =>
      service.addQuestion(
        req.user.userId,
        req.params.id,
        req.body || {},
      ),
    ),
  );

  router.patch(
    '/questions/:id',
    auth,
    handle(200, req =>
      service.updateQuestion(
        req.user.userId,
        req.params.id,
        req.body || {},
      ),
    ),
  );

  router.delete(
    '/questions/:id',
    auth,
    handle(200, req =>
      service.deleteQuestion(req.user.userId, req.params.id),
    ),
  );

  // ─────────────────────────────
  // 설문 응답 / 결과
  // ─────────────────────────────

  router.post(
    '/surveys/:id/responses',
    auth,
    handle(201, req =>
      service.submitResponse(
        req.user.userId,
        req.params.id,
        (req.body || {}).answers,
      ),
    ),
  );

  router.get(
    '/surveys/:id/results',
    auth,
    handle(200, req =>
      service.getSurveyResults(req.user.userId, req.params.id),
    ),
  );

  // ─────────────────────────────
  // 충전 주문 생성 / 조회
  // ─────────────────────────────

  router.post(
    '/payments/orders',
    auth,
    handle(201, async req => {
      const { provider, amount } = req.body || {};

      if (!['KAKAOPAY', 'TOSS'].includes(provider)) {
        fail(400, 'provider는 KAKAOPAY 또는 TOSS여야 합니다.');
      }

      if (!allowedAmounts.includes(amount)) {
        fail(400, '충전 금액은 1000, 3000, 5000원 중 선택해주세요.');
      }

      const callbackToken = randomBytes(32).toString('hex');

      const order = await prisma.paymentOrder.create({
        data: {
          userId: req.user.userId,
          provider,
          amount,
          creditPoint: amount,
          callbackHash: hashToken(callbackToken),
        },
        select: {
          id: true,
          provider: true,
          amount: true,
          creditPoint: true,
          status: true,
          createdAt: true,
        },
      });

      return {
        ...order,
        callbackToken,
      };
    }),
  );

  router.get(
    '/payments/orders/:orderId',
    auth,
    handle(200, async req => {
      const order = await prisma.paymentOrder.findFirst({
        where: {
          id: req.params.orderId,
          userId: req.user.userId,
        },
        select: {
          id: true,
          provider: true,
          amount: true,
          creditPoint: true,
          status: true,
          approvedAt: true,
          creditedAt: true,
          createdAt: true,
        },
      });

      if (!order) {
        fail(404, '충전 주문을 찾을 수 없습니다.');
      }

      return order;
    }),
  );

  // ─────────────────────────────
  // 카카오페이 결제 준비
  // ─────────────────────────────

  router.post(
    '/payments/orders/:orderId/kakao/ready',
    auth,
    async (req, res, next) => {
      try {
        kakaoConfig();
        const baseUrl = publicBaseUrl();
        const callbackToken = req.body?.callbackToken;

        const order = await callbackOrder(
          req.params.orderId,
          callbackToken,
          'KAKAOPAY',
        );

        if (order.userId !== req.user.userId) {
          fail(404, '충전 주문을 찾을 수 없습니다.');
        }

        if (order.status === 'READY' && order.checkoutUrl) {
          return res.json({
            orderId: order.id,
            status: order.status,
            checkoutUrl: order.checkoutUrl,
          });
        }

        if (order.status !== 'CREATED') {
          fail(409, '현재 상태에서는 결제를 준비할 수 없습니다.');
        }

        const claimed = await prisma.paymentOrder.updateMany({
          where: {
            id: order.id,
            status: 'CREATED',
          },
          data: {
            status: 'PREPARING',
          },
        });

        if (claimed.count !== 1) {
          fail(409, '이미 결제 준비가 진행 중입니다.');
        }

        const query = new URLSearchParams({
          orderId: order.id,
          state: callbackToken,
        }).toString();

        let result;

        try {
          result = await kakaoRequest('ready', {
            partner_order_id: order.id,
            partner_user_id: String(order.userId),
            item_name: `바오밥 ${order.creditPoint}포인트 충전`,
            quantity: 1,
            total_amount: order.amount,
            tax_free_amount: 0,
            approval_url:
              `${baseUrl}/api/payments/kakao/success?${query}`,
            cancel_url:
              `${baseUrl}/api/payments/kakao/cancel?${query}`,
            fail_url:
              `${baseUrl}/api/payments/kakao/fail?${query}`,
          });
        } catch (error) {
          await prisma.paymentOrder.update({
            where: { id: order.id },
            data: {
              status: error.providerRejected
                ? 'READY_FAILED'
                : 'READY_UNKNOWN',
            },
          });

          return res.status(502).json({
            error: error.providerRejected
              ? '카카오페이 결제 준비 요청이 거절됐습니다.'
              : '카카오페이 응답을 확인하지 못했습니다. 새 주문을 생성해주세요.',
            providerCode: error.providerCode,
            providerMessage: error.providerMessage,
          });
        }

        if (
          typeof result.tid !== 'string' ||
          typeof result.next_redirect_pc_url !== 'string'
        ) {
          await prisma.paymentOrder.update({
            where: { id: order.id },
            data: { status: 'READY_UNKNOWN' },
          });

          fail(502, '카카오페이 결제 준비 응답이 올바르지 않습니다.');
        }

        const prepared = await prisma.paymentOrder.update({
          where: { id: order.id },
          data: {
            providerRef: result.tid,
            checkoutUrl: result.next_redirect_pc_url,
            status: 'READY',
          },
        });

        res.json({
          orderId: prepared.id,
          status: prepared.status,
          checkoutUrl: prepared.checkoutUrl,
        });
      } catch (error) {
        next(error);
      }
    },
  );

  // ─────────────────────────────
  // 카카오페이 승인 / 포인트 지급
  // ─────────────────────────────

  router.get(
    '/payments/kakao/success',
    paymentPage,
    async (req, res, next) => {
      try {
        const { orderId, state, pg_token: pgToken } = req.query;

        const order = await callbackOrder(
          orderId,
          state,
          'KAKAOPAY',
        );

        if (order.creditedAt) {
          return res.send('이미 포인트 충전이 완료된 주문입니다.');
        }

        if (
          !order.providerRef ||
          !['READY', 'APPROVING', 'APPROVAL_UNKNOWN'].includes(
            order.status,
          )
        ) {
          fail(409, '결제를 승인할 수 없는 주문 상태입니다.');
        }

        // 이미 승인된 결제인지 먼저 조회
        let payment = await kakaoRequest('order', {
          tid: order.providerRef,
        });

        if (payment.status !== 'SUCCESS_PAYMENT') {
          if (order.status !== 'READY') {
            return res.status(202).send(
              '결제 승인 결과를 확인 중입니다. 잠시 후 새로고침해주세요.',
            );
          }

          if (typeof pgToken !== 'string' || !pgToken) {
            fail(400, '결제 승인 토큰이 없습니다.');
          }

          const claimed = await prisma.paymentOrder.updateMany({
            where: {
              id: order.id,
              status: 'READY',
              creditedAt: null,
            },
            data: {
              status: 'APPROVING',
            },
          });

          if (claimed.count !== 1) {
            return res.status(202).send(
              '결제 처리 중입니다. 잠시 후 새로고침해주세요.',
            );
          }

          try {
            await kakaoRequest('approve', {
              tid: order.providerRef,
              partner_order_id: order.id,
              partner_user_id: String(order.userId),
              pg_token: pgToken,
              total_amount: order.amount,
            });
          } catch {
            // 승인 요청의 응답이 없어도 조회로 결과 확인
          }

          try {
            payment = await kakaoRequest('order', {
              tid: order.providerRef,
            });
          } catch {
            await prisma.paymentOrder.update({
              where: { id: order.id },
              data: { status: 'APPROVAL_UNKNOWN' },
            });

            return res.status(202).send(
              '결제 결과 확인이 지연되고 있습니다. 잠시 후 새로고침해주세요.',
            );
          }

          if (payment.status !== 'SUCCESS_PAYMENT') {
            await prisma.paymentOrder.update({
              where: { id: order.id },
              data: { status: 'APPROVAL_UNKNOWN' },
            });

            return res.status(202).send(
              '승인 완료를 확인하지 못했습니다. 포인트는 아직 지급되지 않았습니다.',
            );
          }
        }

        const { cid } = kakaoConfig();

        if (
          payment.tid !== order.providerRef ||
          payment.cid !== cid ||
          payment.partner_order_id !== order.id ||
          payment.partner_user_id !== String(order.userId) ||
          payment.amount?.total !== order.amount
        ) {
          fail(
            409,
            '결제 정보가 주문과 일치하지 않아 포인트를 지급하지 않았습니다.',
          );
        }

        await creditOrder(
          order,
          payment.approved_at,
          '카카오페이 포인트 충전',
        );

        res.send(`${order.creditPoint}포인트 충전이 완료됐습니다.`);
      } catch (error) {
        next(error);
      }
    },
  );

  router.get('/payments/kakao/cancel', paymentPage, (req, res) => {
    res.send('결제창에서 결제를 취소했습니다.');
  });

  router.get('/payments/kakao/fail', paymentPage, (req, res) => {
    res.send('결제 인증을 완료하지 못했습니다.');
  });

  // ─────────────────────────────
  // 토스 결제창 주소 발급
  // ─────────────────────────────

  router.post(
    '/payments/orders/:orderId/toss/ready',
    auth,
    handle(200, async req => {
      tossClientKey();
      const baseUrl = publicBaseUrl();
      const callbackToken = req.body?.callbackToken;

      const order = await callbackOrder(
        req.params.orderId,
        callbackToken,
        'TOSS',
      );

      if (order.userId !== req.user.userId) {
        fail(404, '충전 주문을 찾을 수 없습니다.');
      }

      const updated = await prisma.paymentOrder.updateMany({
        where: {
          id: order.id,
          status: { in: ['CREATED', 'READY'] },
          creditedAt: null,
        },
        data: {
          status: 'READY',
        },
      });

      if (updated.count !== 1) {
        fail(409, '현재 상태에서는 결제창을 열 수 없습니다.');
      }

      const query = new URLSearchParams({
        orderId: order.id,
        state: callbackToken,
      }).toString();

      return {
        orderId: order.id,
        status: 'READY',
        checkoutUrl: `${baseUrl}/api/payments/toss/checkout?${query}`,
      };
    }),
  );

  // ─────────────────────────────
  // 토스 결제창 페이지
  // ─────────────────────────────

  router.get(
    '/payments/toss/checkout',
    paymentPage,
    async (req, res, next) => {
      try {
        const { orderId, state } = req.query;

        const order = await callbackOrder(
          orderId,
          state,
          'TOSS',
        );

        if (order.status !== 'READY' || order.creditedAt) {
          fail(409, '결제 가능한 주문 상태가 아닙니다.');
        }

        const clientKey = tossClientKey();
        const baseUrl = publicBaseUrl();
        const query = new URLSearchParams({ state }).toString();

        const config = JSON.stringify({
          clientKey,
          customerKey: `baobab_user_${order.userId}`,
          amount: order.amount,
          orderId: order.id,
          orderName: `바오밥 ${order.creditPoint}포인트 충전`,
          successUrl: `${baseUrl}/api/payments/toss/success?${query}`,
          failUrl: `${baseUrl}/api/payments/toss/fail?${query}`,
        }).replace(/</g, '\\u003c');

        res.type('html').send(`
          <!doctype html>
          <html lang="ko">
          <head>
            <meta charset="utf-8">
            <meta
              name="viewport"
              content="width=device-width, initial-scale=1"
            >
            <title>바오밥 포인트 충전</title>
            <script src="https://js.tosspayments.com/v2/standard"></script>
          </head>
          <body>
            <h1>바오밥 포인트 충전</h1>
            <p>${order.creditPoint}포인트 / ${order.amount}원</p>

            <button id="pay" type="button">결제하기</button>
            <p id="message" role="status"></p>

            <script>
              const config = ${config};
              const button = document.getElementById('pay');
              const message = document.getElementById('message');

              button.addEventListener('click', async () => {
                button.disabled = true;

                try {
                  const tossPayments = TossPayments(config.clientKey);

                  const payment = tossPayments.payment({
                    customerKey: config.customerKey
                  });

                  await payment.requestPayment({
                    method: 'CARD',
                    amount: {
                      currency: 'KRW',
                      value: config.amount
                    },
                    orderId: config.orderId,
                    orderName: config.orderName,
                    successUrl: config.successUrl,
                    failUrl: config.failUrl
                  });
                } catch (error) {
                  message.textContent =
                    error.message || '결제창을 열지 못했습니다.';

                  button.disabled = false;
                }
              });
            </script>
          </body>
          </html>
        `);
      } catch (error) {
        next(error);
      }
    },
  );
      // 토스 서버 API 호출
      async function tossRequest(path, options = {}) {
        const secretKey = process.env.TOSS_SECRET_KEY?.trim();

        if (!secretKey?.startsWith('test_sk_')) {
          fail(503, '토스 테스트 시크릿 키를 확인해주세요.');
        }

        const headers = {
          Authorization:
            `Basic ${Buffer.from(`${secretKey}:`).toString('base64')}`,
          'Content-Type': 'application/json',
        };

        if (options.idempotencyKey) {
          headers['Idempotency-Key'] = options.idempotencyKey;
        }

        const response = await fetch(
          `https://api.tosspayments.com${path}`,
          {
            method: options.method || 'GET',
            headers,
            ...(options.body
              ? { body: JSON.stringify(options.body) }
              : {}),
            signal: AbortSignal.timeout(15000),
          },
        );

        const result = await response.json();

        if (!response.ok) {
          throw Object.assign(
            new Error('토스 결제 요청이 거절됐습니다.'),
            {
              status: 502,
              providerCode: result.code,
            },
          );
        }

        return result;
      }

      // 토스 결제 승인 / 포인트 지급
      router.get(
        '/payments/toss/success',
        paymentPage,
        async (req, res, next) => {
          try {
            const { orderId, state, paymentKey, amount } = req.query;

            const order = await callbackOrder(
              orderId,
              state,
              'TOSS',
            );

            if (
              typeof paymentKey !== 'string' ||
              !paymentKey ||
              paymentKey.length > 200 ||
              typeof amount !== 'string' ||
              !/^\d+$/.test(amount) ||
              Number(amount) !== order.amount
            ) {
              fail(400, '결제 키 또는 결제 금액이 올바르지 않습니다.');
            }

            if (
              order.providerRef &&
              order.providerRef !== paymentKey
            ) {
              fail(409, '주문에 저장된 결제 키와 일치하지 않습니다.');
            }

            if (order.creditedAt) {
              return res.send('이미 포인트 충전이 완료된 주문입니다.');
            }

            if (
              !['READY', 'APPROVING', 'APPROVAL_UNKNOWN'].includes(
                order.status,
              )
            ) {
              fail(409, '결제를 승인할 수 없는 주문 상태입니다.');
            }

            // 승인 전에 환경변수 확인
            const secretKey = process.env.TOSS_SECRET_KEY?.trim();

            if (!secretKey?.startsWith('test_sk_')) {
              fail(503, '토스 테스트 시크릿 키를 확인해주세요.');
            }

            let payment;
            let approvalErrorCode;

            if (order.status === 'READY') {
              // 중복 승인 요청 방지 및 paymentKey 저장
              const claimed = await prisma.paymentOrder.updateMany({
                where: {
                  id: order.id,
                  status: 'READY',
                  creditedAt: null,
                  providerRef: null,
                },
                data: {
                  status: 'APPROVING',
                  providerRef: paymentKey,
                },
              });

              if (claimed.count !== 1) {
                return res.status(202).send(
                  '결제 처리 중입니다. 잠시 후 새로고침해주세요.',
                );
              }

              try {
                payment = await tossRequest('/v1/payments/confirm', {
                  method: 'POST',
                  idempotencyKey: `toss-confirm-${order.id}`,
                  body: {
                    paymentKey,
                    orderId: order.id,
                    amount: order.amount,
                  },
                });
              } catch (error) {
                approvalErrorCode = error.providerCode;
              }
            }

            // 응답을 못 받았거나 이전 요청에서 승인된 경우 조회
            if (!payment) {
              try {
                payment = await tossRequest(
                  `/v1/payments/${encodeURIComponent(paymentKey)}`,
                );
              } catch {
                await prisma.paymentOrder.updateMany({
                  where: {
                    id: order.id,
                    creditedAt: null,
                    status: {
                      in: ['APPROVING', 'APPROVAL_UNKNOWN'],
                    },
                  },
                  data: {
                    status: 'APPROVAL_UNKNOWN',
                  },
                });

                return res.status(202).json({
                  message:
                    '결제 결과를 확인하지 못했습니다. 포인트는 아직 지급되지 않았습니다.',
                  providerCode: approvalErrorCode,
                });
              }
            }

            // 토스가 확인한 결제 정보와 DB 주문 대조
            if (
              payment.paymentKey !== paymentKey ||
              payment.orderId !== order.id ||
              payment.totalAmount !== order.amount ||
              payment.currency !== 'KRW'
            ) {
              fail(
                409,
                '결제 정보가 주문과 일치하지 않아 포인트를 지급하지 않았습니다.',
              );
            }

            if (payment.status !== 'DONE') {
              await prisma.paymentOrder.updateMany({
                where: {
                  id: order.id,
                  creditedAt: null,
                  status: {
                    in: ['APPROVING', 'APPROVAL_UNKNOWN'],
                  },
                },
                data: {
                  status: 'APPROVAL_UNKNOWN',
                },
              });

              return res.status(202).send(
                '결제 완료를 확인하지 못했습니다. 포인트는 아직 지급되지 않았습니다.',
              );
            }

            // 기존 공통 함수로 주문 / 잔액 / 적립 내역 저장
            await creditOrder(
              order,
              payment.approvedAt,
              '토스페이먼츠 포인트 충전',
            );

            res.send(`${order.creditPoint}포인트 충전이 완료됐습니다.`);
          } catch (error) {
            next(error);
          }
        },
      );

      // 결제창 취소 / 인증 실패 안내
      router.get('/payments/toss/fail', paymentPage, (req, res) => {
        res.send('결제를 취소했거나 결제 인증을 완료하지 못했습니다.');
      });
        // 쿠폰 교환 상품
        const couponItems = [
          {
            id: 'cafe-americano',
            shop: '월계동 바오밥 카페',
            title: '아메리카노 500원 할인',
            cost: 500,
            category: '카페',
          },
          {
            id: 'snack-discount',
            shop: '월계 골목 분식',
            title: '분식 메뉴 1,000원 할인',
            cost: 1000,
            category: '먹거리',
          },
          {
            id: 'book-discount',
            shop: '동네 책방',
            title: '도서 구매 1,000원 할인',
            cost: 1000,
            category: '문화',
          },
          {
            id: 'culture-priority',
            shop: '월계 문화센터',
            title: '문화 프로그램 우선 신청권',
            cost: 1500,
            category: '문화',
          },
          {
            id: 'workshop-discount',
            shop: '초록 동네 공방',
            title: '원데이 클래스 2,000원 할인',
            cost: 2000,
            category: '문화',
          },
          {
            id: 'bakery-discount',
            shop: '바오밥 베이커리',
            title: '베이커리 1,000원 할인',
            cost: 1000,
            category: '먹거리',
          },
        ];

        const couponSelect = {
          id: true,
          itemId: true,
          shop: true,
          title: true,
          cost: true,
          status: true,
          createdAt: true,
          usedAt: true,
        };

        // 교환 가능한 상품 목록
        router.get(
          '/coupons/items',
          handle(200, () => couponItems),
        );

        // 로그인한 사용자의 쿠폰 목록
        router.get(
          '/users/me/coupons',
          auth,
          handle(200, req =>
            prisma.coupon.findMany({
              where: {
                userId: req.user.userId,
              },
              orderBy: [
                { createdAt: 'desc' },
                { id: 'desc' },
              ],
              select: couponSelect,
            }),
          ),
        );

        // 쿠폰 교환
        router.post(
          '/coupons/exchange',
          auth,
          async (req, res, next) => {
            try {
              const userId = req.user.userId;
              const { itemId, requestKey } = req.body || {};

              const item = couponItems.find(product => product.id === itemId);

              if (!item) {
                fail(400, '교환할 상품을 찾을 수 없습니다.');
              }

              // 한 번의 교환에는 하나의 UUID 사용
              if (
                typeof requestKey !== 'string' ||
                !/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i
                  .test(requestKey)
              ) {
                fail(400, '교환 요청 ID가 올바르지 않습니다.');
              }

              // UUID의 대소문자 차이로 중복 처리되지 않도록 통일
              const normalizedKey = requestKey.toLowerCase();

              async function existingExchange() {
                const coupon = await prisma.coupon.findUnique({
                  where: {
                    userId_requestKey: {
                      userId,
                      requestKey: normalizedKey,
                    },
                  },
                  select: couponSelect,
                });

                if (!coupon) return null;

                if (coupon.itemId !== item.id) {
                  fail(409, '같은 요청 ID로 다른 상품을 교환할 수 없습니다.');
                }

                const user = await prisma.user.findUnique({
                  where: { id: userId },
                  select: { point: true },
                });

                if (!user) {
                  fail(404, '사용자를 찾을 수 없습니다.');
                }

                return {
                  coupon,
                  point: user.point,
                  alreadyProcessed: true,
                };
              }

              // 응답을 못 받아 재시도한 요청이면 기존 결과 반환
              const existing = await existingExchange();

              if (existing) {
                return res.status(200).json(existing);
              }

              let result;

              try {
                result = await transaction(prisma, async tx => {
                  // 잔액 확인과 차감을 하나의 DB 명령으로 처리
                  const deducted = await tx.user.updateMany({
                    where: {
                      id: userId,
                      point: { gte: item.cost },
                    },
                    data: {
                      point: { decrement: item.cost },
                    },
                  });

                  if (deducted.count !== 1) {
                    fail(409, '보유 포인트가 부족합니다.');
                  }

                  // 중복 요청이면 unique 제약으로 실패하며 차감도 롤백
                  const coupon = await tx.coupon.create({
                    data: {
                      userId,
                      itemId: item.id,
                      shop: item.shop,
                      title: item.title,
                      cost: item.cost,
                      requestKey: normalizedKey,
                    },
                    select: couponSelect,
                  });

                  await tx.pointHistory.create({
                    data: {
                      userId,
                      amount: -item.cost,
                      description: `쿠폰 교환: ${item.shop} / ${item.title}`,
                    },
                  });

                  const user = await tx.user.findUnique({
                    where: { id: userId },
                    select: { point: true },
                  });

                  return {
                    coupon,
                    point: user.point,
                    alreadyProcessed: false,
                  };
                });
              } catch (error) {
                // 동시에 들어온 같은 요청이 먼저 완료됐는지 확인
                const recovered = await existingExchange();

                if (recovered) {
                  return res.status(200).json(recovered);
                }

                if (['P2034', 'P1008', 'P2028'].includes(error.code)) {
                  return res.status(503).json({
                    error:
                      '교환 처리가 지연되고 있습니다. 같은 requestKey로 다시 요청해주세요.',
                  });
                }

                throw error;
              }

              res.status(201).json(result);
            } catch (error) {
              next(error);
            }
          },
        );

  return router;
}

module.exports = { createSurveyRouter };