/* Equipment hire: farmers search and book; owners (and farmers who rent out machines) list equipment and answer bookings.
   Helpers ($, view, esc, input, select, opts, alertBox, submit, run, num, PROVINCES, CURRENCIES, me) come from app.js. */
const EQ_CATEGORIES = ['TRACTOR', 'PLOUGH', 'PLANTER', 'HARVESTER', 'SPRAYER', 'IRRIGATION', 'TRAILER', 'GENERATOR', 'OTHER'];
const EQ_COLOR = { PENDING: 'warning', CONFIRMED: 'success', REJECTED: 'danger', CANCELLED: 'secondary', COMPLETED: 'primary', ACTIVE: 'success' };
const eqBadge = s => `<span class="badge text-bg-${EQ_COLOR[s] || 'secondary'}">${esc(s)}</span>`;
const eqNice = s => esc(String(s).replace(/_/g, ' ').toLowerCase());
const eqMoney = (v, c) => Number(v).toFixed(2) + ' ' + esc(c);
const eqToday = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const eqRange = (a, b) => a === b ? esc(a) : esc(a) + ' to ' + esc(b);
const eqPlace = (p, d) => esc(p) + (d ? ', ' + esc(d) : '');
const eqContact = (c, who) => c ? `<div class="alert alert-success py-2 small mb-2"><strong>${who} contact (shared because the booking is confirmed):</strong><br>
  ${esc(c.name)}${c.phone ? ' &middot; ' + esc(c.phone) : ''}${c.email ? ' &middot; ' + esc(c.email) : ''}</div>` : '';
let eqFilter = { category: '', near: '', currency: '', maxRate: '', from: '', to: '' };

function eqBookingCard(b, owner) {
  const actions = owner
    ? (b.status === 'PENDING' ? `<button class="btn btn-success btn-sm me-1" data-act="confirm" data-id="${b.id}">Confirm</button>
        <button class="btn btn-outline-danger btn-sm" data-act="reject" data-id="${b.id}">Decline</button>` : '') +
      (b.status === 'CONFIRMED' ? `<button class="btn btn-primary btn-sm me-1" data-act="complete" data-id="${b.id}">Mark completed</button>
        <button class="btn btn-outline-danger btn-sm" data-act="owner-cancel" data-id="${b.id}">Cancel booking</button>` : '')
    : ((b.status === 'PENDING' || b.status === 'CONFIRMED') ? `<button class="btn btn-outline-danger btn-sm" data-cancel="${b.id}">Cancel</button>` : '');
  return `<div class="card mb-2"><div class="card-body py-2">
    <div class="d-flex justify-content-between"><strong>${esc(b.equipmentName)}</strong>${eqBadge(b.status)}</div>
    <div class="small">${owner ? 'Hirer' : 'Owner'}: ${esc(b.counterpartName)} &middot; ${eqRange(b.startDate, b.endDate)} (${esc(b.days)} day${b.days === 1 ? '' : 's'})</div>
    <div class="small text-muted">${eqMoney(b.dailyRate, b.currency)} per day &middot; total <strong>${eqMoney(b.totalPrice, b.currency)}</strong>
      ${Number(b.deposit) > 0 ? ' &middot; deposit asked ' + eqMoney(b.deposit, b.currency) : ''}</div>
    ${b.note ? `<div class="small mt-1">${esc(b.note)}</div>` : ''}
    ${eqContact(b.contact, owner ? 'Hirer' : 'Owner')}
    ${actions ? `<div class="mt-2">${actions}</div>` : ''}</div></div>`;
}

/* ---------- Farmers: find and book equipment ---------- */
async function hireScreen() {
  view.innerHTML = `<h5 class="mb-3">Hire equipment</h5>
    <form id="ef" class="row g-2 mb-2">
      <div class="col-6"><label class="form-label small mb-0">Equipment</label><select class="form-select" name="category">
        <option value="">Any</option>${opts(EQ_CATEGORIES, eqFilter.category)}</select></div>
      <div class="col-6"><label class="form-label small mb-0">Near (for distances)</label><select class="form-select" name="near">
        <option value="">Not set</option>${opts(PROVINCES, eqFilter.near)}</select></div>
      <div class="col-6"><label class="form-label small mb-0">Currency</label><select class="form-select" name="currency">
        <option value="">Any</option>${opts(CURRENCIES, eqFilter.currency)}</select></div>
      ${input('Highest rate per day', 'maxRate', `type="number" step="0.01" min="0.01" value="${esc(eqFilter.maxRate)}"`)}
      ${input('From', 'from', `type="date" min="${eqToday()}" value="${esc(eqFilter.from)}"`)}
      ${input('To', 'to', `type="date" min="${eqToday()}" value="${esc(eqFilter.to)}"`)}
      <div class="col-12"><button class="btn btn-success btn-sm w-100">Search</button></div></form>
    <div id="eqRes"><p class="text-muted">Loading...</p></div>
    <h6 class="mt-4">My bookings</h6><div id="eqMine"></div>
    <p class="small text-muted">Add dates to see which machines are free. Prices are per day in the owner's currency and are not converted.
      Distances are rough, between province centres. Owners see your name; contact details are shared only after a booking is confirmed.
      Payment and any deposit are arranged directly with the owner.</p>`;
  $('ef').onsubmit = submit(async f => {
    eqFilter = { category: f.category, near: f.near, currency: f.currency, maxRate: f.maxRate, from: f.from, to: f.to };
    await eqLoad();
  });
  await Promise.all([eqLoad(), eqMyBookings()]);
}

async function eqLoad() {
  if (!eqFilter.from !== !eqFilter.to) { $('eqRes').innerHTML = alertBox('Choose both dates, or neither.'); return; }
  const qs = new URLSearchParams();
  Object.entries(eqFilter).forEach(([k, v]) => { if (v) qs.set(k, v); });
  const list = await API.get('/api/equipment?' + qs);
  $('eqRes').innerHTML = list.length ? list.map(o => {
    const e = o.equipment;
    return `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between align-items-start">
        <div><strong>${esc(e.name)}</strong> <span class="badge text-bg-light">${eqNice(e.category)}</span><br>
          <small class="text-muted">${esc(e.ownerName)} &middot; ${eqPlace(e.province, e.district)}${o.distanceKm != null ? ' &middot; about ' + esc(o.distanceKm) + ' km' : ''}</small></div>
        <div class="text-end"><span class="fs-5">${eqMoney(e.dailyRate, e.currency)}</span><br><small class="text-muted">per day</small></div></div>
      <div class="mt-1">
        ${o.available === true ? '<span class="badge text-bg-success">Free on your dates</span>' : ''}
        ${o.available === false ? '<span class="badge text-bg-danger">Not free on your dates</span>' : ''}
        ${o.nearest ? '<span class="badge text-bg-info ms-1">Closest</span>' : ''}
        ${o.cheapest ? `<span class="badge text-bg-success ms-1">Cheapest in ${esc(e.currency)}</span>` : ''}
        ${e.operatorIncluded ? '<span class="badge text-bg-light ms-1">Operator included</span>' : ''}
        ${e.fuelIncluded ? '<span class="badge text-bg-light ms-1">Fuel included</span>' : ''}</div>
      ${e.description ? `<div class="small mt-1">${esc(e.description)}</div>` : ''}
      <div class="small text-muted">${Number(e.deposit) > 0 ? 'Deposit asked: ' + eqMoney(e.deposit, e.currency) : 'No deposit asked'}
        ${o.estimatedTotal != null ? ' &middot; for your dates about <strong>' + eqMoney(o.estimatedTotal, e.currency) + '</strong>' : ''}</div>
      ${o.busy.length ? `<details class="small mt-1"><summary>Dates already taken</summary>${o.busy.map(b =>
        eqRange(b.from, b.to) + (b.kind === 'BOOKED' ? ' (booked)' : ' (not available)')).join('<br>')}</details>` : ''}
      <details class="mt-2"><summary class="btn btn-success btn-sm">Request booking</summary>
        <form class="row g-2 mt-1" data-book="${e.id}">
          ${input('First day', 'startDate', `type="date" required min="${eqToday()}" value="${esc(eqFilter.from)}"`)}
          ${input('Last day', 'endDate', `type="date" required min="${eqToday()}" value="${esc(eqFilter.to)}"`)}
          <div class="col-12"><input class="form-control" name="note" maxlength="500" placeholder="What is it for? (optional)"></div>
          <div class="col-12"><button class="btn btn-success w-100">Send request</button></div>
          <div class="col-12 small text-muted">Both days count. If the owner confirms, you will see each other's contact details.</div>
        </form></details>
    </div></div>`;
  }).join('') : '<p class="text-muted">No equipment matches. Try fewer filters.</p>';
  $('eqRes').querySelectorAll('form[data-book]').forEach(f => f.onsubmit = async e => {
    e.preventDefault();
    const x = f.elements;
    try {
      await API.post('/api/equipment/bookings', { equipmentId: Number(f.dataset.book), startDate: x['startDate'].value,
        endDate: x['endDate'].value, note: x['note'].value || null });
      alert('Request sent. The owner can confirm or decline it.'); run(hireScreen());
    } catch (err) { alert(err.message); }
  });
}

async function eqMyBookings() {
  const list = await API.get('/api/equipment/bookings/mine');
  $('eqMine').innerHTML = list.length ? list.map(b => eqBookingCard(b, false)).join('') : '<p class="text-muted">You have no bookings yet.</p>';
  $('eqMine').querySelectorAll('[data-cancel]').forEach(b => b.onclick = async () => {
    if (!confirm('Cancel this booking? The owner is told.')) return;
    try { await API.post(`/api/equipment/bookings/${b.dataset.cancel}/cancel`); run(hireScreen()); } catch (e) { alert(e.message); }
  });
}

/* ---------- Owners: my equipment and incoming bookings ---------- */
async function ownerEquipmentScreen() {
  const [mine, incoming] = await Promise.all([API.get('/api/equipment/mine'), API.get('/api/equipment/bookings/incoming')]);
  const open = incoming.filter(b => b.status === 'PENDING' || b.status === 'CONFIRMED');
  const past = incoming.filter(b => !open.includes(b));
  view.innerHTML = `<h5 class="mb-3">My equipment</h5>
    <h6>Bookings</h6>${open.length ? open.map(b => eqBookingCard(b, true)).join('') : '<p class="text-muted">No bookings waiting or coming up.</p>'}
    ${past.length ? `<details class="mb-3"><summary class="small">Earlier bookings (${past.length})</summary>${past.map(b => eqBookingCard(b, true)).join('')}</details>` : ''}
    <h6 class="mt-3">My listings</h6>` + (mine.length ? mine.map(m => { const e = m.equipment; return `
    <div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between align-items-start">
        <div><strong>${esc(e.name)}</strong> <span class="badge text-bg-light">${eqNice(e.category)}</span><br>
          <small class="text-muted">${eqMoney(e.dailyRate, e.currency)} per day &middot; ${eqPlace(e.province, e.district)}
            &middot; ${e.operatorIncluded ? 'operator included' : 'no operator'} &middot; ${e.fuelIncluded ? 'fuel included' : 'fuel not included'}
            ${Number(e.deposit) > 0 ? ' &middot; deposit ' + eqMoney(e.deposit, e.currency) : ''}</small></div>
        <button class="btn btn-outline-danger btn-sm" data-rm="${e.id}">Remove</button></div>
      ${m.blocks.length ? '<div class="small mt-1">Marked unavailable:<ul class="mb-1">' + m.blocks.map(k =>
        `<li>${eqRange(k.startDate, k.endDate)}${k.reason ? ' (' + esc(k.reason) + ')' : ''}
          <button class="btn btn-link btn-sm p-0 ms-1" data-delblock="${e.id}:${k.id}">remove</button></li>`).join('') + '</ul></div>' : ''}
      <details class="mt-1"><summary class="small">Mark dates as unavailable</summary>
        <form class="row g-2 mt-1" data-block="${e.id}">
          ${input('From', 'startDate', `type="date" required min="${eqToday()}"`)}${input('To', 'endDate', `type="date" required min="${eqToday()}"`)}
          <div class="col-12"><input class="form-control" name="reason" maxlength="200" placeholder="Reason (optional), e.g. servicing"></div>
          <div class="col-12"><button class="btn btn-outline-secondary btn-sm w-100">Block these dates</button></div></form></details>
    </div></div>`; }).join('') : '<p class="text-muted">You have no equipment listed yet.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>Add equipment</h6>
      <form id="nf" class="row g-2">
        ${select('Type', 'category', EQ_CATEGORIES, 'TRACTOR')}
        ${input('Name', 'name', 'required maxlength="150" placeholder="e.g. 75hp tractor"')}
        ${input('Rate per day', 'dailyRate', 'type="number" step="0.01" min="0.01" required')}
        ${select('Currency', 'currency', CURRENCIES)}
        ${select('Based in', 'province', PROVINCES)}
        ${input('District', 'district')}
        ${input('Deposit (optional)', 'deposit', 'type="number" step="0.01" min="0" value="0"')}
        <div class="col-6 d-flex align-items-end"><div>
          <div class="form-check"><input class="form-check-input" type="checkbox" name="operator" id="eqop"><label class="form-check-label" for="eqop">Operator included</label></div>
          <div class="form-check"><input class="form-check-input" type="checkbox" name="fuel" id="eqfuel"><label class="form-check-label" for="eqfuel">Fuel included</label></div></div></div>
        <div class="col-12"><textarea class="form-control" name="description" rows="2" maxlength="500" placeholder="Condition, attachments, what it can do"></textarea></div>
        <div class="col-12"><button class="btn btn-success w-100">Add equipment</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">Farmers see your name, the machine, its rate and your province. Your phone number and email are shown only to a hirer whose booking you confirmed.
        AgriPulse does not collect payments or deposits.</p></div></div>`;

  view.querySelectorAll('[data-act]').forEach(b => b.onclick = async () => {
    const words = { confirm: 'Confirm this booking?', reject: 'Decline this request?', complete: 'Mark this hire as completed?', 'owner-cancel': 'Cancel this confirmed booking? The hirer is told.' };
    if (!confirm(words[b.dataset.act])) return;
    try { await API.post(`/api/equipment/bookings/${b.dataset.id}/${b.dataset.act}`); run(ownerEquipmentScreen()); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-rm]').forEach(b => b.onclick = async () => {
    if (!confirm('Remove this equipment? Waiting requests are declined.')) return;
    try { await API.del('/api/equipment/' + b.dataset.rm); run(ownerEquipmentScreen()); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-delblock]').forEach(b => b.onclick = async () => {
    const [eid, kid] = b.dataset.delblock.split(':');
    try { await API.del(`/api/equipment/${eid}/blocks/${kid}`); run(ownerEquipmentScreen()); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('form[data-block]').forEach(f => f.onsubmit = async e => {
    e.preventDefault();
    const x = f.elements;
    try {
      await API.post(`/api/equipment/${f.dataset.block}/blocks`, { startDate: x['startDate'].value, endDate: x['endDate'].value, reason: x['reason'].value || null });
      run(ownerEquipmentScreen());
    } catch (err) { alert(err.message); }
  });
  $('nf').onsubmit = async e => {
    e.preventDefault();
    const x = e.target.elements;
    try {
      await API.post('/api/equipment', { category: x['category'].value, name: x['name'].value, description: x['description'].value || null,
        province: x['province'].value, district: x['district'].value || null, dailyRate: num(x['dailyRate'].value), currency: x['currency'].value,
        operatorIncluded: x['operator'].checked, fuelIncluded: x['fuel'].checked, deposit: num(x['deposit'].value) || 0 });
      run(ownerEquipmentScreen());
    } catch (err) { alert(err.message); }
  };
}
