/* Buyer purchase requests and farmer offers. Helpers ($, view, esc, input, select, opts, alertBox, submit, run, num) come from app.js. */
const R_COLOR = { OPEN: 'primary', FULFILLED: 'success', CLOSED: 'secondary', EXPIRED: 'secondary',
  PENDING: 'warning', ACCEPTED: 'success', REJECTED: 'danger', WITHDRAWN: 'secondary' };
const rBadge = s => `<span class="badge text-bg-${R_COLOR[s] || 'secondary'}">${esc(s)}</span>`;
const rPrice = (v, c) => Number(v).toFixed(2) + ' ' + esc(c);
const maxText = r => r.maxPrice != null ? 'max ' + rPrice(r.maxPrice, r.currency) + ' per ' + esc(r.unit) : 'no price limit';
const placeText = r => esc(r.province) + (r.district ? ', ' + esc(r.district) : '');
let openQ = '', openProv = '';

function requestsScreen() { return me.role === 'BUYER' ? buyerRequests() : farmerRequests(); }

/* ---------- Buyer ---------- */
async function buyerRequests() {
  const list = await API.get('/api/requests/mine');
  view.innerHTML = `<h5 class="mb-3">My purchase requests</h5>` + (list.length ? list.map(r => `
    <div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-center">
      <div><strong>${esc(r.productName)}</strong> ${rBadge(r.status)}<br>
        <small class="text-muted">${esc(r.quantity)} ${esc(r.unit)} · ${maxText(r)} · ${esc(r.province)}
        · ${r.pendingOffers} offer${r.pendingOffers === 1 ? '' : 's'} waiting</small></div>
      <button class="btn btn-success btn-sm" data-r="${r.id}">Open</button></div></div>`).join('')
    : '<p class="text-muted">You have no requests yet. Post what you want to buy and farmers will send offers.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>Post a purchase request</h6>
      <form id="rf" class="row g-2">
        ${input('What do you want?', 'productName', 'required maxlength="100" placeholder="e.g. maize"')}
        ${select('Category', 'category', CATEGORIES)}
        ${input('Quantity', 'quantity', 'type="number" step="0.01" min="0.01" required')}
        ${select('Unit', 'unit', UNITS)}
        ${input('Highest price per unit (optional)', 'maxPrice', 'type="number" step="0.01" min="0.01"')}
        ${select('Currency', 'currency', CURRENCIES)}
        ${select('Deliver to province', 'province', PROVINCES)}
        ${input('District', 'district')}
        ${input('Needed by (optional)', 'neededBy', 'type="date"')}
        <div class="col-12"><label class="form-label small mb-0">Details (quality, delivery, payment)</label>
          <textarea class="form-control" name="note" rows="2" maxlength="500"></textarea></div>
        <div class="col-12"><button class="btn btn-success w-100">Post request</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">Farmers see your name and province, not your phone number or email.
        Prices are compared only within the same currency and unit.</p></div></div>`;
  view.querySelectorAll('[data-r]').forEach(b => b.onclick = () => run(buyerRequestDetail(b.dataset.r)));
  $('rf').onsubmit = submit(async f => {
    const d = await API.post('/api/requests', { productName: f.productName, category: f.category, quantity: num(f.quantity),
      unit: f.unit, maxPrice: num(f.maxPrice), currency: f.currency, province: f.province, district: f.district || null,
      neededBy: f.neededBy || null, note: f.note || null });
    run(buyerRequestDetail(d.id));
  });
}

async function buyerRequestDetail(id) {
  const d = await API.get('/api/requests/' + id);
  const r = d.request;
  const offerCards = d.offers.length ? d.offers.map(o => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(o.farmerName)}</strong>${rBadge(o.status)}</div>
      <div>${esc(o.quantity)} ${esc(o.unit)} of ${esc(o.productName)} at ${rPrice(o.unitPrice, o.currency)} per ${esc(o.unit)}
        &middot; total <strong>${rPrice(o.total, o.currency)}</strong></div>
      <small class="text-muted">${o.listingProvince ? esc(o.listingProvince) : ''}</small>
      ${o.message ? `<div class="small mt-1">${esc(o.message)}</div>` : ''}
      ${o.status === 'PENDING' && r.status === 'OPEN' ? `<div class="mt-2">
        <button class="btn btn-success btn-sm me-1" data-accept="${o.id}">Accept</button>
        <button class="btn btn-outline-danger btn-sm" data-reject="${o.id}">Decline</button></div>` : ''}
      ${o.status === 'ACCEPTED' ? '<div class="small text-success mt-1">Accepted. A confirmed order was created, see My orders.</div>' : ''}
    </div></div>`).join('') : '<p class="text-muted">No offers yet.</p>';
  const listingCards = d.matchingListings.length ? d.matchingListings.map(p => `<div class="card mb-2"><div class="card-body py-2">
      <strong>${esc(p.name)}</strong>
      ${p.nearest ? '<span class="badge text-bg-info ms-1">Closest</span>' : ''}
      ${p.cheapest ? '<span class="badge text-bg-success ms-1">Cheapest</span>' : ''}
      ${p.coversQuantity ? '<span class="badge text-bg-light ms-1">Has enough</span>' : ''}<br>
      <span class="fs-6">${rPrice(p.price, p.currency)}</span> <small class="text-muted">per ${esc(p.unit)} · ${esc(p.quantity)} ${esc(p.unit)} available
        · ${esc(p.province)}${p.distanceKm != null ? ' · about ' + esc(p.distanceKm) + ' km' : ''} · ${esc(p.sellerName)}</small>
      <div class="input-group input-group-sm mt-2">
        <input class="form-control" type="number" step="0.01" min="0.01" id="oq-${p.productId}"
          value="${Math.min(Number(r.quantity), Number(p.quantity))}">
        <button class="btn btn-success" data-order="${p.productId}">Order this</button></div>
    </div></div>`).join('') : '<p class="text-muted">No listings match right now. Your request stays visible to farmers.</p>';

  view.innerHTML = `<button class="btn btn-link px-0" id="rb">&larr; My requests</button>
    <h5>${esc(r.productName)} ${rBadge(r.status)}</h5>
    <p class="small text-muted">${esc(r.quantity)} ${esc(r.unit)} · ${maxText(r)} · deliver to ${placeText(r)}
      ${r.neededBy ? ' · needed by ' + esc(r.neededBy) : ''}${r.note ? '<br>' + esc(r.note) : ''}</p>
    <h6>Offers from farmers</h6>${offerCards}
    ${r.status === 'OPEN' ? `<h6 class="mt-3">Matching listings now</h6>
      <p class="small text-muted">Same product, unit and currency, within your price limit. Distances are rough (between province centres).</p>${listingCards}
      <button class="btn btn-outline-secondary btn-sm mt-2" id="closeR">Close this request</button>` : ''}`;
  $('rb').onclick = () => run(buyerRequests());
  view.querySelectorAll('[data-accept]').forEach(b => b.onclick = async () => {
    if (!confirm('Accept this offer? A confirmed order is created and this request is closed.')) return;
    try { await API.post(`/api/requests/${id}/offers/${b.dataset.accept}/accept`); run(buyerRequestDetail(id)); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-reject]').forEach(b => b.onclick = async () => {
    try { await API.post(`/api/requests/${id}/offers/${b.dataset.reject}/reject`); run(buyerRequestDetail(id)); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-order]').forEach(b => b.onclick = async () => {
    const q = num($('oq-' + b.dataset.order).value);
    if (!q || q <= 0) return alert('Enter a quantity');
    try { await API.post('/api/orders', { productId: Number(b.dataset.order), quantity: q, note: 'From purchase request #' + id });
      alert('Order sent. The farmer must confirm it.'); location.hash = 'myorders'; } catch (e) { alert(e.message); }
  });
  if ($('closeR')) $('closeR').onclick = async () => {
    if (!confirm('Close this request? Waiting offers are declined.')) return;
    try { await API.post(`/api/requests/${id}/close`); run(buyerRequests()); } catch (e) { alert(e.message); }
  };
}

/* ---------- Farmer ---------- */
function offerForm(r, listings) {
  const ok = listings.filter(p => p.unit === r.unit && p.currency === r.currency);
  if (!ok.length) return `<p class="small text-muted mb-0">To offer on this, you need an active listing priced in ${esc(r.currency)} per ${esc(r.unit)}. Add one in My listings.</p>`;
  return `<details class="mt-2"><summary class="btn btn-success btn-sm">Make an offer</summary>
    <form class="row g-2 mt-1" data-offer="${r.id}">
      <div class="col-12"><label class="form-label small mb-0">From which listing?</label><select class="form-select" name="productId">
        ${ok.map(p => `<option value="${p.id}">${esc(p.name)} (${esc(p.quantity)} ${esc(p.unit)} at ${rPrice(p.price, p.currency)})</option>`).join('')}</select></div>
      ${input('Quantity (' + esc(r.unit) + ')', 'quantity', `type="number" step="0.01" min="0.01" required value="${esc(r.quantity)}"`)}
      ${input('Price per ' + esc(r.unit) + ' (optional)', 'unitPrice', 'type="number" step="0.01" min="0.01" placeholder="listing price"')}
      <div class="col-12"><input class="form-control" name="message" maxlength="500" placeholder="Message to the buyer (optional)"></div>
      <div class="col-12"><button class="btn btn-success w-100">Send offer</button></div></form></details>`;
}

function reqCard(r, listings, extra = '') {
  return `<div class="card mb-2"><div class="card-body">
    <div class="d-flex justify-content-between"><strong>${esc(r.productName)}</strong><span class="small text-muted">${esc(r.buyerName)}</span></div>
    <div>${esc(r.quantity)} ${esc(r.unit)} · ${maxText(r)}</div>
    <small class="text-muted">Deliver to ${placeText(r)}${r.neededBy ? ' · needed by ' + esc(r.neededBy) : ''}</small>
    ${r.note ? `<div class="small mt-1">${esc(r.note)}</div>` : ''}${extra}${offerForm(r, listings)}</div></div>`;
}

async function farmerRequests() {
  const qs = new URLSearchParams();
  if (openQ) qs.set('q', openQ);
  if (openProv) qs.set('province', openProv);
  const [matches, open, myOffers, mine] = await Promise.all([API.get('/api/requests/matches'),
    API.get('/api/requests/open?' + qs), API.get('/api/requests/offers/mine'), API.get('/api/products/mine')]);
  const listings = mine.filter(p => p.status === 'ACTIVE' && Number(p.quantity) > 0);
  const matchedIds = new Set(matches.map(m => m.request.id));

  view.innerHTML = `<h5 class="mb-2">Buyer requests</h5>
    ${matches.length ? `<h6 class="mt-3">These match your listings</h6>` + matches.map(m => reqCard(m.request, listings,
      '<div class="small text-success">Matches your listing: ' + m.myListings.map(l => esc(l.name) + (l.coversQuantity ? '' : ' (not enough stock for all of it)')).join(', ') + '</div>')).join('') : ''}
    <h6 class="mt-3">All open requests</h6>
    <form id="of" class="row g-2 mb-2">
      <div class="col-6"><input class="form-control" name="q" placeholder="Search, e.g. maize" value="${esc(openQ)}"></div>
      <div class="col-6"><select class="form-select" name="province"><option value="">All provinces</option>${opts(PROVINCES, openProv)}</select></div>
      <div class="col-12"><button class="btn btn-outline-success btn-sm w-100">Search</button></div></form>
    ${open.filter(r => !matchedIds.has(r.id)).map(r => reqCard(r, listings)).join('') ||
      (matches.length ? '' : '<p class="text-muted">No open requests found.</p>')}
    <p class="small text-muted">Matching compares the product name, unit, currency and the buyer's price limit. It is not AI, and nothing is converted between currencies.</p>
    <h6 class="mt-3">My offers</h6>` + (myOffers.length ? myOffers.map(o => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(o.requestProduct || 'Request')}</strong>${rBadge(o.status)}</div>
      <small class="text-muted">${esc(o.quantity)} ${esc(o.unit)} at ${rPrice(o.unitPrice, o.currency)} · total ${rPrice(o.total, o.currency)}</small>
      ${o.status === 'PENDING' ? `<div class="mt-1"><button class="btn btn-outline-danger btn-sm" data-wd="${o.id}">Withdraw</button></div>` : ''}
      ${o.status === 'ACCEPTED' ? '<div class="small text-success">Accepted. See the confirmed order in Orders.</div>' : ''}
    </div></div>`).join('') : '<p class="text-muted">You have not made any offers yet.</p>');

  $('of').onsubmit = submit(async f => { openQ = f.q.trim(); openProv = f.province; run(farmerRequests()); });
  view.querySelectorAll('[data-wd]').forEach(b => b.onclick = async () => {
    try { await API.post(`/api/requests/offers/${b.dataset.wd}/withdraw`); run(farmerRequests()); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('form[data-offer]').forEach(f => f.onsubmit = async e => {
    e.preventDefault();
    const x = f.elements;
    try {
      await API.post(`/api/requests/${f.dataset.offer}/offers`, { productId: Number(x['productId'].value),
        quantity: num(x['quantity'].value), unitPrice: num(x['unitPrice'].value), message: x['message'].value || null });
      alert('Offer sent. The buyer can accept or decline it.'); run(farmerRequests());
    } catch (err) { alert(err.message); }
  });
}
