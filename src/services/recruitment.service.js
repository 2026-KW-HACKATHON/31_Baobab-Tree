const { transaction } = require('../lib/transaction');

function fail(status, message) {
  throw Object.assign(new Error(message), { status });
}

function positiveId(value) {
  const result = Number(value);

  if (
    !/^\d+$/.test(String(value)) ||
    !Number.isSafeInteger(result) ||
    result < 1 ||
    result > 2147483647
  ) {
    fail(400, '올바르지 않은 ID입니다.');
  }

  return result;
}

function requiredText(value, label, maxLength) {
  if (typeof value !== 'string') {
    fail(400, `${label}을 확인해주세요.`);
  }

  const result = value.trim();

  if (!result || result.length > maxLength) {
    fail(400, `${label}을 확인해주세요.`);
  }

  return result;
}

function integer(value, label, min, max) {
  if (!Number.isInteger(value) || value < min || value > max) {
    fail(400, `${label}을 확인해주세요.`);
  }

  return value;
}

function dateValue(value, label) {
  // 시간대가 명시된 날짜만 받습니다.
  if (
    typeof value !== 'string' ||
    !/(Z|[+-]\d{2}:\d{2})$/.test(value)
  ) {
    fail(400, `${label}에 시간대가 포함된 날짜를 넣어주세요.`);
  }

  const result = new Date(value);

  if (!Number.isFinite(result.getTime())) {
    fail(400, `${label}을 확인해주세요.`);
  }

  return result;
}

const recruitmentInclude = {
  author: {
    select: {
      id: true,
      name: true,
    },
  },
  slots: {
    orderBy: {
      startsAt: 'asc',
    },
  },
};

function publicRecruitment(row) {
  // 모집 예산 등의 내부 데이터는 공개 응답에서 제외합니다.
  return {
    id: row.id,
    authorId: row.authorId,
    author: row.author,
    title: row.title,
    organization: row.organization,
    activityType: row.activityType,
    participationMode: row.participationMode,
    description: row.description,
    eligibility: row.eligibility,
    location: row.location,
    durationMinutes: row.durationMinutes,
    targetCount: row.targetCount,
    acceptedCount: row.acceptedCount,
    rewardPoint: row.rewardPoint,
    applicationDeadline: row.applicationDeadline,
    status: row.status,
    createdAt: row.createdAt,
    slots: row.slots,
  };
}

class RecruitmentService {
  constructor(prisma) {
    this.prisma = prisma;
  }

  async list(query = {}) {
    const where = {
      status: 'OPEN',
      applicationDeadline: {
        gt: new Date(),
      },
    };

    if (query.activityType) {
      if (
        !['EXPERIMENT', 'INTERVIEW', 'USABILITY', 'OTHER']
          .includes(query.activityType)
      ) {
        fail(400, '활동 유형을 확인해주세요.');
      }

      where.activityType = query.activityType;
    }

    if (query.search) {
      const search = requiredText(query.search, '검색어', 100);

      where.OR = [
        { title: { contains: search } },
        { organization: { contains: search } },
      ];
    }

    const rows = await this.prisma.recruitment.findMany({
      where,
      include: recruitmentInclude,
      orderBy: {
        id: 'desc',
      },
      take: 100,
    });

    return rows.map(publicRecruitment);
  }

  async detail(recruitmentId) {
    const row = await this.prisma.recruitment.findUnique({
      where: {
        id: positiveId(recruitmentId),
      },
      include: recruitmentInclude,
    });

    if (!row) {
      fail(404, '모집글을 찾을 수 없습니다.');
    }

    return publicRecruitment(row);
  }

  async mine(userId) {
    const rows = await this.prisma.recruitment.findMany({
      where: {
        authorId: positiveId(userId),
      },
      include: recruitmentInclude,
      orderBy: {
        id: 'desc',
      },
    });

    return rows.map(publicRecruitment);
  }

  async create(userId, body) {
    if (!body || typeof body !== 'object' || Array.isArray(body)) {
      fail(400, '모집 정보를 확인해주세요.');
    }

    const authorId = positiveId(userId);
    const now = new Date();

    const activityType = body.activityType;
    const participationMode = body.participationMode;

    if (
      !['EXPERIMENT', 'INTERVIEW', 'USABILITY', 'OTHER']
        .includes(activityType)
    ) {
      fail(400, '활동 유형을 확인해주세요.');
    }

    if (!['ONLINE', 'OFFLINE'].includes(participationMode)) {
      fail(400, '참여 방식을 확인해주세요.');
    }

    const targetCount = integer(
      body.targetCount,
      '모집 인원',
      1,
      1000
    );

    const rewardPoint = integer(
      body.rewardPoint,
      '참여 보상',
      0,
      100000
    );

    const budget = targetCount * rewardPoint;

    const applicationDeadline = dateValue(
      body.applicationDeadline,
      '신청 마감일'
    );

    if (applicationDeadline <= now) {
      fail(400, '신청 마감일은 현재 시간 이후여야 합니다.');
    }

    if (
      !Array.isArray(body.slots) ||
      body.slots.length < 1 ||
      body.slots.length > 30
    ) {
      fail(400, '참여 일정은 1개 이상, 30개 이하로 등록해주세요.');
    }

    const slots = body.slots.map(slot => {
      const startsAt = dateValue(slot?.startsAt, '참여 시작 시간');
      const endsAt = dateValue(slot?.endsAt, '참여 종료 시간');

      if (
        startsAt <= applicationDeadline ||
        endsAt <= startsAt
      ) {
        fail(
          400,
          '참여 시작은 신청 마감 이후, 종료는 시작 이후여야 합니다.'
        );
      }

      return { startsAt, endsAt };
    });

    const slotKeys = slots.map(
      slot => `${slot.startsAt.toISOString()}/${slot.endsAt.toISOString()}`
    );

    if (new Set(slotKeys).size !== slotKeys.length) {
      fail(400, '중복된 참여 일정이 있습니다.');
    }

    const data = {
      authorId,
      title: requiredText(body.title, '모집 제목', 150),
      organization: requiredText(body.organization, '기관명', 100),
      activityType,
      participationMode,
      description: requiredText(body.description, '모집 설명', 5000),
      eligibility: requiredText(body.eligibility, '신청 조건', 2000),
      location: requiredText(body.location, '참여 장소 또는 방식', 300),
      durationMinutes: integer(
        body.durationMinutes,
        '소요 시간',
        1,
        1440
      ),
      targetCount,
      rewardPoint,
      fundedReward: budget,
      remainingReward: budget,
      applicationDeadline,
      slots: {
        create: slots,
      },
    };

    return transaction(this.prisma, async tx => {
      if (applicationDeadline <= new Date()) {
        fail(400, '신청 마감일이 지났습니다.');
      }

      const user = await tx.user.findUnique({
        where: { id: authorId },
        select: { id: true },
      });

      if (!user) {
        fail(401, '다시 로그인해주세요.');
      }

      if (budget > 0) {
        const charged = await tx.user.updateMany({
          where: {
            id: authorId,
            point: {
              gte: budget,
            },
          },
          data: {
            point: {
              decrement: budget,
            },
          },
        });

        if (charged.count !== 1) {
          fail(409, '모집 보상 예산을 확보할 포인트가 부족합니다.');
        }
      }

      const recruitment = await tx.recruitment.create({
        data,
        include: recruitmentInclude,
      });

      if (budget > 0) {
        await tx.pointHistory.create({
          data: {
            userId: authorId,
            amount: -budget,
            description:
              `참여자 모집 보상 예산: ${recruitment.title}`,
          },
        });
      }

      return publicRecruitment(recruitment);
    });
  }

  // 모집자가 해당 모집글의 주인인지 확인
  async ownedRecruitment(tx, recruitmentId, userId) {
    const recruitment = await tx.recruitment.findUnique({
      where: {
        id: positiveId(recruitmentId),
      },
    });

    if (!recruitment) {
      fail(404, '모집글을 찾을 수 없습니다.');
    }

    if (recruitment.authorId !== positiveId(userId)) {
      fail(403, '본인이 등록한 모집글만 관리할 수 있습니다.');
    }

    return recruitment;
  }

  // 참가 신청
  async apply(userId, recruitmentId, body) {
    const applicantId = positiveId(userId);
    const recruitmentKey = positiveId(recruitmentId);
    const slotId = positiveId(body?.slotId);

    if (body?.agreed !== true) {
      fail(400, '참여 안내와 개인정보 이용에 동의해주세요.');
    }

    let message = null;

    if (body.message !== undefined && body.message !== null) {
      if (
        typeof body.message !== 'string' ||
        body.message.length > 2000
      ) {
        fail(400, '신청 내용은 2000자 이하로 입력해주세요.');
      }

      message = body.message.trim() || null;
    }

    return transaction(this.prisma, async tx => {
      const recruitment = await tx.recruitment.findUnique({
        where: { id: recruitmentKey },
      });

      if (!recruitment) {
        fail(404, '모집글을 찾을 수 없습니다.');
      }

      if (recruitment.authorId === applicantId) {
        fail(400, '본인의 모집글에는 신청할 수 없습니다.');
      }

      const slot = await tx.recruitmentSlot.findUnique({
        where: { id: slotId },
      });

      if (!slot || slot.recruitmentId !== recruitmentKey) {
        fail(400, '해당 모집글의 참여 일정을 선택해주세요.');
      }

      const previous = await tx.recruitmentApplication.findUnique({
        where: {
          recruitmentId_userId: {
            recruitmentId: recruitmentKey,
            userId: applicantId,
          },
        },
      });

      // 같은 신청을 다시 전송한 경우 기존 신청을 반환
      if (
        previous &&
        previous.status !== 'CANCELLED'
      ) {
        if (
          previous.slotId === slotId &&
          previous.message === message
        ) {
          return previous;
        }

        fail(409, '이미 신청한 모집글입니다.');
      }

      const now = new Date();

      if (
        recruitment.status !== 'OPEN' ||
        recruitment.applicationDeadline <= now ||
        slot.startsAt <= now
      ) {
        fail(409, '신청이 마감된 모집글입니다.');
      }

      const data = {
        slotId,
        message,
        status: 'APPLIED',
        agreedAt: now,
        selectedAt: null,
      };

      // 취소한 신청은 같은 기록으로 재신청
      if (previous) {
        return tx.recruitmentApplication.update({
          where: { id: previous.id },
          data,
        });
      }

      return tx.recruitmentApplication.create({
        data: {
          ...data,
          recruitmentId: recruitmentKey,
          userId: applicantId,
        },
      });
    });
  }

  // 내 신청 내역: 다른 신청자의 정보는 포함하지 않음
  async myApplications(userId) {
    const rows = await this.prisma.recruitmentApplication.findMany({
      where: {
        userId: positiveId(userId),
      },
      include: {
        slot: true,
        recruitment: {
          include: recruitmentInclude,
        },
      },
      orderBy: {
        id: 'desc',
      },
    });

    return rows.map(row => ({
      id: row.id,
      status: row.status,
      message: row.message,
      createdAt: row.createdAt,
      selectedAt: row.selectedAt,
      completedAt: row.completedAt,
      paidAt: row.paidAt,
      slot: row.slot,
      recruitment: publicRecruitment(row.recruitment),
    }));
  }

  // 모집자의 신청자 목록
  async applicants(userId, recruitmentId) {
    return transaction(this.prisma, async tx => {
      const recruitment = await this.ownedRecruitment(
        tx,
        recruitmentId,
        userId
      );

      return tx.recruitmentApplication.findMany({
        where: {
          recruitmentId: recruitment.id,
        },
        select: {
          id: true,
          status: true,
          message: true,
          createdAt: true,
          selectedAt: true,
          completedAt: true,
          paidAt: true,
          slot: true,
          user: {
            select: {
              id: true,
              name: true,
              ageGroup: true,
              region: true,
              memberType: true,
            },
          },
        },
        orderBy: {
          id: 'desc',
        },
      });
    });
  }

  // 신청자가 본인의 신청 취소
  async cancel(userId, applicationId) {
    return transaction(this.prisma, async tx => {
      const application = await tx.recruitmentApplication.findUnique({
        where: {
          id: positiveId(applicationId),
        },
        include: {
          slot: true,
        },
      });

      if (
        !application ||
        application.userId !== positiveId(userId)
      ) {
        fail(404, '본인의 신청 내역을 찾을 수 없습니다.');
      }

      if (application.status === 'CANCELLED') {
        return { id: application.id, status: 'CANCELLED' };
      }

      if (
        !['APPLIED', 'ACCEPTED'].includes(application.status)
      ) {
        fail(409, '현재 상태에서는 취소할 수 없습니다.');
      }

      if (application.slot.startsAt <= new Date()) {
        fail(409, '참여 시작 이후에는 모집자에게 문의해주세요.');
      }

      if (application.status === 'ACCEPTED') {
        await tx.recruitment.update({
          where: {
            id: application.recruitmentId,
          },
          data: {
            acceptedCount: {
              decrement: 1,
            },
          },
        });
      }

      return tx.recruitmentApplication.update({
        where: {
          id: application.id,
        },
        data: {
          status: 'CANCELLED',
        },
        select: {
          id: true,
          status: true,
        },
      });
    });
  }

  // 모집자가 신청자를 선정하거나 미선정 처리
  async decide(userId, applicationId, status) {
    if (!['ACCEPTED', 'REJECTED'].includes(status)) {
      fail(400, '선정 상태를 확인해주세요.');
    }

    return transaction(this.prisma, async tx => {
      const application = await tx.recruitmentApplication.findUnique({
        where: {
          id: positiveId(applicationId),
        },
        include: {
          slot: true,
        },
      });

      if (!application) {
        fail(404, '신청 내역을 찾을 수 없습니다.');
      }

      const recruitment = await this.ownedRecruitment(
        tx,
        application.recruitmentId,
        userId
      );

      // 같은 결정을 다시 요청해도 인원은 중복 변경하지 않음
      if (application.status === status) {
        return { id: application.id, status };
      }

      if (recruitment.status !== 'OPEN') {
        fail(409, '종료된 모집글입니다.');
      }

      if (
        status === 'ACCEPTED' &&
        application.status !== 'APPLIED'
      ) {
        fail(409, '신청 중인 참가자만 선정할 수 있습니다.');
      }

      if (
        status === 'REJECTED' &&
        !['APPLIED', 'ACCEPTED'].includes(application.status)
      ) {
        fail(409, '현재 상태에서는 미선정 처리할 수 없습니다.');
      }

      if (status === 'ACCEPTED') {
        if (application.slot.startsAt <= new Date()) {
          fail(409, '이미 시작된 일정에는 선정할 수 없습니다.');
        }

        const reserved = await tx.recruitment.updateMany({
          where: {
            id: recruitment.id,
            status: 'OPEN',
            acceptedCount: {
              lt: recruitment.targetCount,
            },
          },
          data: {
            acceptedCount: {
              increment: 1,
            },
          },
        });

        if (reserved.count !== 1) {
          fail(409, '모집 인원이 모두 찼습니다.');
        }
      } else if (application.status === 'ACCEPTED') {
        await tx.recruitment.update({
          where: { id: recruitment.id },
          data: {
            acceptedCount: {
              decrement: 1,
            },
          },
        });
      }

      return tx.recruitmentApplication.update({
        where: {
          id: application.id,
        },
        data: {
          status,
          selectedAt: status === 'ACCEPTED' ? new Date() : null,
        },
        select: {
          id: true,
          status: true,
          selectedAt: true,
        },
      });
    });
  }

  // 모집자가 실제 참여를 확인하고 보상 지급
  async complete(userId, applicationId) {
    return transaction(this.prisma, async tx => {
      const application = await tx.recruitmentApplication.findUnique({
        where: {
          id: positiveId(applicationId),
        },
        include: {
          slot: true,
        },
      });

      if (!application) {
        fail(404, '신청 내역을 찾을 수 없습니다.');
      }

      const recruitment = await this.ownedRecruitment(
        tx,
        application.recruitmentId,
        userId
      );

      if (application.status === 'COMPLETED') {
        return {
          id: application.id,
          status: 'COMPLETED',
          rewardPoint: recruitment.rewardPoint,
        };
      }

      if (
        recruitment.status !== 'OPEN' ||
        application.status !== 'ACCEPTED'
      ) {
        fail(409, '선정된 참가자만 참여 완료 처리할 수 있습니다.');
      }

      if (application.slot.endsAt > new Date()) {
        fail(409, '참여 일정 종료 후 완료 처리해주세요.');
      }

      const reward = recruitment.rewardPoint;

      const reserved = await tx.recruitment.updateMany({
        where: {
          id: recruitment.id,
          status: 'OPEN',
          remainingReward: {
            gte: reward,
          },
        },
        data: {
          remainingReward: {
            decrement: reward,
          },
        },
      });

      if (reserved.count !== 1) {
        fail(409, '남은 보상 예산을 확인해주세요.');
      }

      const now = new Date();

      await tx.recruitmentApplication.update({
        where: {
          id: application.id,
        },
        data: {
          status: 'COMPLETED',
          completedAt: now,
          paidAt: reward > 0 ? now : null,
        },
      });

      if (reward > 0) {
        const credited = await tx.user.updateMany({
          where: {
            id: application.userId,
            point: {
              lte: 2147483647 - reward,
            },
          },
          data: {
            point: {
              increment: reward,
            },
          },
        });

        if (credited.count !== 1) {
          fail(409, '참가자 포인트 잔액을 확인해주세요.');
        }

        await tx.pointHistory.create({
          data: {
            userId: application.userId,
            amount: reward,
            description:
              `연구·활동 참여 보상: ${recruitment.title}`,
            recruitmentApplicationId: application.id,
          },
        });
      }

      return {
        id: application.id,
        status: 'COMPLETED',
        rewardPoint: reward,
      };
    });
  }

  // 모집 종료 및 미사용 보상 예산 환급
  async close(userId, recruitmentId) {
    return transaction(this.prisma, async tx => {
      const recruitment = await this.ownedRecruitment(
        tx,
        recruitmentId,
        userId
      );

      if (recruitment.status === 'CLOSED') {
        return {
          id: recruitment.id,
          status: 'CLOSED',
          refundedPoint: 0,
        };
      }

      const outstanding = await tx.recruitmentApplication.count({
        where: {
          recruitmentId: recruitment.id,
          status: 'ACCEPTED',
        },
      });

      if (outstanding > 0) {
        fail(
          409,
          '선정된 참가자의 참여 완료 또는 미선정 처리를 먼저 해주세요.'
        );
      }

      const refund = recruitment.remainingReward;

      await tx.recruitment.update({
        where: {
          id: recruitment.id,
        },
        data: {
          status: 'CLOSED',
          remainingReward: 0,
        },
      });

      await tx.recruitmentApplication.updateMany({
        where: {
          recruitmentId: recruitment.id,
          status: 'APPLIED',
        },
        data: {
          status: 'REJECTED',
        },
      });

      if (refund > 0) {
        const refunded = await tx.user.updateMany({
          where: {
            id: recruitment.authorId,
            point: {
              lte: 2147483647 - refund,
            },
          },
          data: {
            point: {
              increment: refund,
            },
          },
        });

        if (refunded.count !== 1) {
          fail(409, '모집자 포인트 잔액을 확인해주세요.');
        }

        await tx.pointHistory.create({
          data: {
            userId: recruitment.authorId,
            amount: refund,
            description:
              `참여자 모집 미사용 보상 환급: ${recruitment.title}`,
          },
        });
      }

      return {
        id: recruitment.id,
        status: 'CLOSED',
        refundedPoint: refund,
      };
    });
  }
}

module.exports = { RecruitmentService };
