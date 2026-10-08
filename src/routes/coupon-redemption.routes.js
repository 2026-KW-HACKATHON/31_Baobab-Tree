const express = require('express');

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[char]));
}

function redemptionPage(coupon) {
  const available = coupon.status === 'AVAILABLE';
  const status = available ? '사용 가능한 쿠폰입니다.'
    : coupon.status === 'USED' ? '이미 사용한 쿠폰입니다.' : '사용할 수 없는 쿠폰입니다.';
  return `<!doctype html>
<html lang="ko"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="referrer" content="no-referrer"><title>바오밥 쿠폰 사용</title>
<style>
body{margin:0;padding:32px 20px;background:#fdf9f1;color:#26372b;font-family:system-ui,sans-serif}
main{max-width:420px;margin:24px auto;padding:28px;border-radius:24px;background:white}
h1{font-size:24px;line-height:1.5}p{line-height:1.6;overflow-wrap:anywhere}
button{width:100%;padding:16px;border:0;border-radius:14px;background:#2f5539;color:white;font-size:18px;cursor:pointer}
button:disabled{opacity:.5;cursor:default}.muted{color:#697369;font-size:13px}
</style></head><body><main>
<p>${escapeHtml(coupon.shop)}</p><h1>${escapeHtml(coupon.title)}</h1>
<p id="status" role="status">${status}</p>
<button id="redeem" ${available ? '' : 'disabled'}>${available ? '사용 처리 중…' : '사용 불가'}</button>
<p class="muted">사용 완료 후에는 다시 사용할 수 없습니다.</p>
<p class="muted">현재 상품은 테스트 예시이며 실제 매장 혜택은 제공되지 않습니다.</p>
</main><script>
const button = document.getElementById('redeem');
const status = document.getElementById('status');
async function redeemCoupon() {
  button.disabled = true;
  status.textContent = '사용 처리 중입니다…';
  try {
    const response = await fetch(window.location.pathname, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}'
    });
    const result = await response.json();
    if (!response.ok) {
      status.textContent = result.error || '처리하지 못했습니다. 다시 시도해주세요.';
      button.disabled = response.status === 409 || response.status === 404;
      button.textContent = button.disabled ? '사용 불가' : '다시 시도';
      return;
    }
    status.textContent = result.alreadyUsed ? '이미 사용한 쿠폰입니다.' : '쿠폰 사용이 완료됐습니다.';
    button.textContent = '사용 완료';
  } catch (error) {
    status.textContent = '연결을 확인하고 다시 시도해주세요.';
    button.disabled = false;
    button.textContent = '다시 시도';
  }
}
button.addEventListener('click', redeemCoupon);
const redemptionRequest = !button.disabled ? redeemCoupon() : null;
</script><noscript>자동 사용 처리를 위해 브라우저에서 JavaScript를 허용해주세요.</noscript>
</body></html>`;
}

function createCouponRedemptionRouter(prisma) {
  const router = express.Router();
  // The random UUID in the QR link grants access to this coupon only.
  router.use('/:id/redeem', (req, res, next) => {
    res.set({ 'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer', 'X-Robots-Tag': 'noindex, nofollow' });
    if (!/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i.test(req.params.id)) {
      return res.status(404).json({ error: '쿠폰을 찾을 수 없습니다.' });
    }
    next();
  });
  const select = { id: true, shop: true, title: true, status: true, usedAt: true };
  router.get('/:id/redeem', async (req, res, next) => {
    try {
      const coupon = await prisma.coupon.findUnique({ where: { id: req.params.id }, select });
      if (!coupon) return res.status(404).json({ error: '쿠폰을 찾을 수 없습니다.' });
      res.type('html').send(redemptionPage(coupon));
    } catch (error) { next(error); }
  });
  router.post('/:id/redeem', async (req, res, next) => {
    try {
      if (!req.is('application/json')) return res.status(415).json({ error: 'JSON 요청이 필요합니다.' });
      const updated = await prisma.coupon.updateMany({
        where: { id: req.params.id, status: 'AVAILABLE' },
        data: { status: 'USED', usedAt: new Date() },
      });
      const coupon = await prisma.coupon.findUnique({ where: { id: req.params.id }, select });
      if (!coupon) return res.status(404).json({ error: '쿠폰을 찾을 수 없습니다.' });
      if (coupon.status !== 'USED') return res.status(409).json({ error: '사용할 수 없는 쿠폰입니다.' });
      res.json({ coupon, alreadyUsed: updated.count === 0 });
    } catch (error) { next(error); }
  });
  return router;
}

module.exports = { createCouponRedemptionRouter };
