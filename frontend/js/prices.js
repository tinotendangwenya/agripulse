/* Price boards: board with history chart, price alerts, price quotes, admin review.
   Helpers ($, view, esc, input, select, opts, alertBox, submit, run, num, PROVINCES, UNITS, CURRENCIES, me) come from app.js. */
const P_COLOR = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', ACTIVE: 'primary', TRIGGERED: 'success' };
const pBadge = s => `<span class="badge text-bg-${P_COLOR[s] || 'secondary'}">${esc(s)}</span>`;
const pMoney = (v, c) => Number(v).toFixed(2) + ' ' + esc(c);
const pDay = s => esc(String(s || '').replace('T', ' ').slice(0, 10));
const pToday = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
let priceChart = null, boardCommodity = '', boardProvince = '';

async function priceHub(tab) {
  const trader = ['FARMER', 'BUYER', 'SUPPLIER'].includes(me.role);
  const tabs = [['board', 'Board']];
  if (trader) tabs.push(['alerts', 'My alerts'], ['quote', 'Submit a price']);
  if (me.role === 'ADMIN') tabs.push(['review', 'Review quotes'], ['add', 'Add a price']);
  if (!tabs.some(t => t[0] === tab)) tab = 'board';
  view.innerHTML = `<h5 class="mb-2">Prices</h5>
    <ul class="nav nav-pills mb-3 flex-nowrap overflow-auto">${tabs.map(([k, l]) =>
      `<li class="nav-item"><button class="nav-link ${k === tab ? 'active' : ''}" data-ptab="${k}">${l}</button></li>`).join('')}</ul>
    <div id="pBody"><p>Loading...</p></div>`;
  view.querySelectorAll('[data-ptab]').forEach(b => b.onclick = () => run(priceHub(b.dataset.ptab)));
  try { await ({ board: pBoard, alerts: pAlerts, quote: pQuotes, review: pReview, add: pAdd })[tab](); }
  catch (e) { $('pBody').innerHTML = alertBox(e.message); }
}

/* ---------- Board ---------- */
function boardRow(r) {
  const n = Number(r.change);
  const arrow = n > 0 ? '▲' : n < 0 ? '▼' : '■';
  const sign = n > 0 ? '+' : '';
  const change = r.change == null
    ? '<span class="text-muted">No earlier price recorded</span>'
    : `${arrow} ${sign}${n.toFixed(2)} ${esc(r.currency)}${r.changePercent != null ? ' (' + sign + Number(r.changePercent).toFixed(1) + '%)' : ''}
       <span class="text-muted">since ${esc(r.previousDate)}</span>`;
  return `<div class="card mb-2"><div class="card-body py-2">
    <div class="d-flex justify-content-between align-items-start">
      <div><strong>${esc(r.commodity)}</strong><br><small class="text-muted">${esc(r.market)}, ${esc(r.province)}</small></div>
      <div class="text-end"><span class="fs-5">${pMoney(r.price, r.currency)}</span><br><small class="text-muted">per ${esc(r.unit)}</small></div>
    </div>
    <div class="small">${change}</div>
    <div class="small text-muted">Price date ${esc(r.priceDate)}
      ${r.stale ? `<span class="badge text-bg-warning">Old price (${esc(r.daysOld)} days)</span>` : ''}<br>Source: ${esc(r.source)}</div>
    <button class="btn btn-outline-success btn-sm mt-1" data-chart data-c="${esc(r.commodity)}" data-m="${esc(r.market)}"
      data-cur="${esc(r.currency)}" data-u="${esc(r.unit)}">History chart</button>
  </div></div>`;
}

async function pBoard() {
  const commodities = await API.get('/api/prices/commodities');
  $('pBody').innerHTML = `<form id="bf" class="row g-2 mb-2">
      <div class="col-6"><input class="form-control" name="commodity" list="pcl" placeholder="Commodity (blank = all)" value="${esc(boardCommodity)}">
        <datalist id="pcl">${commodities.map(c => `<option value="${esc(c)}">`).join('')}</datalist></div>
      <div class="col-6"><select class="form-select" name="province"><option value="">All provinces</option>${opts(PROVINCES, boardProvince)}</select></div>
      <div class="col-12"><button class="btn btn-success btn-sm w-100">Show prices</button></div></form>
    <div id="bRows"><p class="text-muted">Loading...</p></div><div id="bChart" class="mt-3"></div>
    <p class="small text-muted">Only recorded prices are shown, nothing is estimated. Each row is one market in one currency and unit, and
      currencies and units are never converted. Prices come from admins and from admin-approved quotes. Check the date before you rely on one.</p>`;
  $('bf').onsubmit = submit(async f => { boardCommodity = f.commodity.trim(); boardProvince = f.province; await loadBoard(); });
  await loadBoard();
}

async function loadBoard() {
  const qs = new URLSearchParams();
  if (boardCommodity) qs.set('commodity', boardCommodity);
  if (boardProvince) qs.set('province', boardProvince);
  const rows = await API.get('/api/prices/board?' + qs);
  $('bRows').innerHTML = rows.length ? rows.map(boardRow).join('')
    : '<p class="text-muted">No prices recorded in the last 120 days for this search.</p>';
  $('bRows').querySelectorAll('[data-chart]').forEach(b => b.onclick = () =>
    pShowChart({ c: b.dataset.c, m: b.dataset.m, cur: b.dataset.cur, u: b.dataset.u }, 90)
      .catch(e => { $('bChart').innerHTML = alertBox(e.message); }));
}

async function pShowChart(sel, days) {
  const box = $('bChart');
  if (priceChart) { priceChart.destroy(); priceChart = null; }
  box.innerHTML = '<p class="text-muted">Loading chart...</p>';
  const qs = new URLSearchParams({ commodity: sel.c, market: sel.m, currency: sel.cur, unit: sel.u, days });
  const pts = await API.get('/api/prices/series?' + qs);
  box.innerHTML = `<div class="card"><div class="card-body">
    <h6>${esc(sel.c)} at ${esc(sel.m)} <small class="text-muted">(${esc(sel.cur)} per ${esc(sel.u)})</small></h6>
    <div class="btn-group btn-group-sm mb-2">${[30, 90, 365].map(d =>
      `<button class="btn btn-outline-secondary ${d === days ? 'active' : ''}" data-days="${d}">${d} days</button>`).join('')}</div>
    ${pts.length ? '<canvas id="pcanvas" height="180"></canvas>' : '<p class="text-muted">No prices recorded in this period.</p>'}
    ${pts.length === 1 ? '<p class="small text-muted">Only one price so far, so there is no line to draw yet.</p>' : ''}
    ${pts.length ? `<details class="mt-2"><summary class="small">Show the numbers</summary><table class="table table-sm mt-1"><tbody>
      ${pts.map(p => `<tr><td>${esc(p.date)}</td><td>${pMoney(p.price, sel.cur)}</td></tr>`).join('')}</tbody></table></details>` : ''}
  </div></div>`;
  box.querySelectorAll('[data-days]').forEach(b => b.onclick = () =>
    pShowChart(sel, Number(b.dataset.days)).catch(e => { box.innerHTML = alertBox(e.message); }));
  box.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  if (!pts.length) return;
  if (typeof Chart === 'undefined') {
    box.insertAdjacentHTML('beforeend', '<p class="small text-muted">The chart library could not load (it needs internet), so the numbers are listed instead.</p>');
    return;
  }
  priceChart = new Chart($('pcanvas'), {
    type: 'line',
    data: { labels: pts.map(p => p.date), datasets: [{ label: sel.cur + ' per ' + sel.u, data: pts.map(p => Number(p.price)),
      tension: 0.2, borderColor: '#2e7d32', backgroundColor: '#2e7d32' }] },
    options: { responsive: true, plugins: { legend: { display: false } },
      scales: { y: { title: { display: true, text: sel.cur + ' per ' + sel.u } } } }
  });
}

/* ---------- Alerts ---------- */
async function pAlerts() {
  const [list, commodities] = await Promise.all([API.get('/api/prices/alerts'), API.get('/api/prices/commodities')]);
  $('pBody').innerHTML = (list.length ? list.map(a => `<div class="card mb-2"><div class="card-body py-2 d-flex justify-content-between align-items-start">
      <div><strong>${esc(a.commodity)}</strong> ${pBadge(a.status)}<br>
        <small>${a.market ? 'At ' + esc(a.market) : 'In any market'}: ${a.direction === 'ABOVE' ? 'goes above' : 'falls below'}
          ${pMoney(a.threshold, a.currency)} per ${esc(a.unit)}</small>
        ${a.status === 'TRIGGERED' ? `<br><small class="text-success">Triggered ${pDay(a.triggeredAt)} at ${pMoney(a.triggeredPrice, a.currency)}</small>` : ''}</div>
      <button class="btn btn-outline-danger btn-sm" data-del="${a.id}">Delete</button></div></div>`).join('')
    : '<p class="text-muted">You have no price alerts yet.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>New price alert</h6>
      <form id="af" class="row g-2">
        <div class="col-6"><label class="form-label small mb-0">Commodity</label>
          <input class="form-control" name="commodity" list="acl" required maxlength="80">
          <datalist id="acl">${commodities.map(c => `<option value="${esc(c)}">`).join('')}</datalist></div>
        ${input('Market (optional)', 'market', 'maxlength="100" placeholder="any market"')}
        ${select('Tell me when it', 'direction', ['ABOVE', 'BELOW'])}
        ${input('Price limit', 'threshold', 'type="number" step="0.01" min="0.01" required')}
        ${select('Currency', 'currency', CURRENCIES)}
        ${select('Per', 'unit', UNITS)}
        <div class="col-12"><button class="btn btn-success w-100">Create alert</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">An alert fires once, when a new price is recorded that goes above or below your limit,
        and then shows as TRIGGERED. It compares only the same currency and unit. You get it under Alerts at the top.</p>
    </div></div>`;
  view.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => {
    try { await API.del('/api/prices/alerts/' + b.dataset.del); run(priceHub('alerts')); } catch (e) { alert(e.message); }
  });
  $('af').onsubmit = submit(async f => {
    await API.post('/api/prices/alerts', { commodity: f.commodity, market: f.market || null, direction: f.direction,
      threshold: num(f.threshold), currency: f.currency, unit: f.unit });
    run(priceHub('alerts'));
  });
}

/* ---------- Quotes (farmers, buyers, suppliers) ---------- */
async function pQuotes() {
  const [list, commodities] = await Promise.all([API.get('/api/prices/quotes/mine'), API.get('/api/prices/commodities')]);
  $('pBody').innerHTML = `<div class="card mb-3"><div class="card-body"><h6>Submit a price you have seen</h6>
      <form id="qf" class="row g-2">
        <div class="col-6"><label class="form-label small mb-0">Commodity</label>
          <input class="form-control" name="commodity" list="qcl" required maxlength="80">
          <datalist id="qcl">${commodities.map(c => `<option value="${esc(c)}">`).join('')}</datalist></div>
        ${input('Market', 'market', 'required maxlength="100" placeholder="e.g. Mbare Musika"')}
        ${select('Province', 'province', PROVINCES)}
        ${input('Price', 'price', 'type="number" step="0.01" min="0.01" required')}
        ${select('Currency', 'currency', CURRENCIES)}
        ${select('Per', 'unit', UNITS)}
        ${input('Date you saw it', 'priceDate', `type="date" value="${pToday()}" required`)}
        <div class="col-12"><label class="form-label small mb-0">Note (optional)</label><input class="form-control" name="note" maxlength="300" placeholder="e.g. grade, bag size, buyer or seller"></div>
        <div class="col-12"><button class="btn btn-success w-100">Send for review</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">An admin checks every quote before it appears on the board. The board says only "a registered ${esc(me.role.toLowerCase())}", never your name. Only submit prices you actually saw in the last 30 days.</p>
    </div></div><h6>My quotes</h6>` + (list.length ? list.map(q => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(q.commodity)}</strong>${pBadge(q.status)}</div>
      <small class="text-muted">${esc(q.market)}, ${esc(q.province)} · ${pMoney(q.price, q.currency)} per ${esc(q.unit)} · ${esc(q.priceDate)}</small>
      ${q.reviewNote ? `<div class="small mt-1">Admin note: ${esc(q.reviewNote)}</div>` : ''}</div></div>`).join('')
    : '<p class="text-muted">You have not submitted any quotes.</p>');
  $('qf').onsubmit = submit(async f => {
    await API.post('/api/prices/quotes', { commodity: f.commodity, market: f.market, province: f.province, price: num(f.price),
      currency: f.currency, unit: f.unit, priceDate: f.priceDate, note: f.note || null });
    alert('Thank you. An admin will review your quote.'); run(priceHub('quote'));
  });
}

/* ---------- Admin: review quotes ---------- */
async function pReview(status = 'PENDING') {
  const list = await API.get('/api/admin/quotes?status=' + status);
  $('pBody').innerHTML = `<select class="form-select mb-3" id="rs">
      <option value="PENDING">Waiting for review</option><option value="APPROVED">Approved</option>
      <option value="REJECTED">Rejected</option><option value="ALL">All</option></select>` +
    (list.length ? list.map(q => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(q.commodity)} at ${esc(q.market)}</strong>${pBadge(q.status)}</div>
      <div>${pMoney(q.price, q.currency)} per ${esc(q.unit)} <small class="text-muted">· ${esc(q.province)} · seen ${esc(q.priceDate)}</small></div>
      <small class="text-muted">From ${esc(q.submitterName)} (${esc(q.submitterRole)})</small>
      ${q.note ? `<div class="small mt-1">Note: ${esc(q.note)}</div>` : ''}
      ${q.reviewNote ? `<div class="small mt-1">Review note: ${esc(q.reviewNote)}</div>` : ''}
      ${q.status === 'PENDING' ? `<div class="mt-2"><button class="btn btn-success btn-sm me-1" data-ok="${q.id}">Approve</button>
        <button class="btn btn-outline-danger btn-sm" data-no="${q.id}">Reject</button></div>` : ''}
    </div></div>`).join('') : '<p class="text-muted">Nothing here.</p>') +
    '<p class="small text-muted">Approving publishes the price on the board, where everyone can see it with its date. Check it against what you know of the market first.</p>';
  $('rs').value = status;
  $('rs').onchange = e => run2(pReview(e.target.value));
  view.querySelectorAll('[data-ok]').forEach(b => b.onclick = async () => {
    if (!confirm('Approve this quote and publish it as a price?')) return;
    try { await API.post(`/api/admin/quotes/${b.dataset.ok}/approve`); run2(pReview(status)); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-no]').forEach(b => b.onclick = async () => {
    const note = prompt('Reason for rejecting (the sender will see this):');
    if (note === null) return;
    try { await API.post(`/api/admin/quotes/${b.dataset.no}/reject`, { note }); run2(pReview(status)); } catch (e) { alert(e.message); }
  });
}
const run2 = p => p.catch(e => { $('pBody').innerHTML = alertBox(e.message); });

/* ---------- Admin: add a verified price ---------- */
async function pAdd() {
  $('pBody').innerHTML = `<div class="card"><div class="card-body"><h6>Add a verified price</h6>
    <form id="df" class="row g-2">
      ${input('Commodity', 'commodity', 'required maxlength="80"')}
      ${input('Market', 'market', 'required maxlength="100"')}
      ${select('Province', 'province', PROVINCES)}
      ${input('Price', 'price', 'type="number" step="0.01" min="0.01" required')}
      ${select('Currency', 'currency', CURRENCIES)}
      ${select('Unit', 'unit', UNITS)}
      ${input('Price date', 'priceDate', `type="date" value="${pToday()}" required`)}
      ${input('Source', 'source', 'required maxlength="200" placeholder="Who reported this price"')}
      <div class="col-12"><button class="btn btn-success w-100">Save price</button></div>
    </form>
    <p class="small text-muted mt-2 mb-0">Saving a price also checks everyone's price alerts and notifies the people whose limit it crosses.</p></div></div>`;
  $('df').onsubmit = submit(async f => {
    await API.post('/api/admin/prices', { commodity: f.commodity, market: f.market, province: f.province, price: num(f.price),
      currency: f.currency, unit: f.unit, priceDate: f.priceDate, source: f.source });
    alert('Price saved'); run(priceHub('board'));
  });
}
