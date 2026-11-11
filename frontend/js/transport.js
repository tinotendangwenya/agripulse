/* Transport: requesters (farmers, buyers, suppliers) post jobs; transporters list vehicles and quote.
   Helpers ($, view, esc, input, select, opts, alertBox, submit, run, num, PROVINCES, CURRENCIES, me) come from app.js. */
const TR_CARGO = ['PRODUCE', 'LIVESTOCK', 'INPUTS', 'OTHER'];
const TR_VEHICLES = ['PICKUP', 'LIGHT_TRUCK', 'TRUCK', 'TRUCK_TRAILER', 'LIVESTOCK_TRUCK', 'REFRIGERATED', 'TRACTOR_TRAILER', 'OTHER'];
const T_COLOR = { OPEN: 'primary', BOOKED: 'info', IN_TRANSIT: 'warning', DELIVERED: 'success', CANCELLED: 'secondary', EXPIRED: 'secondary',
  PENDING: 'warning', ACCEPTED: 'success', REJECTED: 'danger', WITHDRAWN: 'secondary', ACTIVE: 'success' };
const tBadge = s => `<span class="badge text-bg-${T_COLOR[s] || 'secondary'}">${esc(String(s).replace('_', ' '))}</span>`;
const tNice = s => esc(String(s).replace(/_/g, ' ').toLowerCase());
const tMoney = (v, c) => Number(v).toFixed(2) + ' ' + esc(c);
const tKg = kg => Number(kg) >= 1000 ? (Number(kg) / 1000).toFixed(2) + ' t' : Number(kg).toFixed(0) + ' kg';
const tWhen = s => esc(String(s || '').replace('T', ' ').slice(0, 16));
const tPlace = (p, d) => esc(p) + (d ? ', ' + esc(d) : '');
const tRoute = r => tPlace(r.pickupProvince, r.pickupDistrict) + ' &rarr; ' + tPlace(r.deliveryProvince, r.deliveryDistrict);
const T_ESTIMATE_NOTE = "Estimates are rough: the transporter's rate x the straight-line distance between province centres x 1.3 for winding roads, and never below the minimum charge. The real price is whatever the transporter quotes.";
const tContact = (c, who) => c ? `<div class="alert alert-success py-2 small mb-2"><strong>${who} contact (shared because a booking was agreed):</strong><br>
  ${esc(c.name)}${c.phone ? ' &middot; ' + esc(c.phone) : ''}${c.email ? ' &middot; ' + esc(c.email) : ''}</div>` : '';

/* ---------- Requesters: farmers, buyers, suppliers ---------- */
async function transportScreen() {
  const list = await API.get('/api/transport/requests/mine');
  view.innerHTML = `<h5 class="mb-3">Transport</h5>` + (list.length ? list.map(r => `
    <div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-center">
      <div><strong>${tNice(r.cargoType)}: ${esc(r.description)}</strong> ${tBadge(r.status)}<br>
        <small class="text-muted">${tKg(r.weightKg)} &middot; ${tRoute(r)} &middot; ${r.pendingQuotes} quote${r.pendingQuotes === 1 ? '' : 's'} waiting</small></div>
      <button class="btn btn-success btn-sm" data-r="${r.id}">Open</button></div></div>`).join('')
    : '<p class="text-muted">No transport requests yet. Post what you need moved, and transporters send quotes.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>Request transport</h6>
      <form id="tf" class="row g-2">
        ${select('What are you moving?', 'cargoType', TR_CARGO)}
        ${input('Description', 'description', 'required maxlength="200" placeholder="e.g. 80 bags of maize"')}
        ${input('Total weight', 'weight', 'type="number" step="0.01" min="0.01" required')}
        ${select('Unit', 'weightUnit', ['KG', 'TONNE'])}
        ${select('Pick up in', 'pickupProvince', PROVINCES)}
        ${input('Pickup district', 'pickupDistrict')}
        ${select('Deliver to', 'deliveryProvince', PROVINCES)}
        ${input('Delivery district', 'deliveryDistrict')}
        ${input('Needed by (optional)', 'neededBy', 'type="date"')}
        <div class="col-12"><label class="form-label small mb-0">Details (loading help, road access, timing)</label>
          <textarea class="form-control" name="note" rows="2" maxlength="500"></textarea></div>
        <div class="col-12"><button class="btn btn-success w-100">Post request</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">Transporters see your name and the provinces, not your phone number or email.
        Contact details are shared only after you accept a quote.</p></div></div>`;
  view.querySelectorAll('[data-r]').forEach(b => b.onclick = () => run(transportDetail(b.dataset.r)));
  $('tf').onsubmit = submit(async f => {
    const d = await API.post('/api/transport/requests', { cargoType: f.cargoType, description: f.description,
      weight: num(f.weight), weightUnit: f.weightUnit, pickupProvince: f.pickupProvince, pickupDistrict: f.pickupDistrict || null,
      deliveryProvince: f.deliveryProvince, deliveryDistrict: f.deliveryDistrict || null, neededBy: f.neededBy || null, note: f.note || null });
    run(transportDetail(d.id));
  });
}

async function transportDetail(id) {
  const d = await API.get('/api/transport/requests/' + id);
  const r = d.request;
  const quoteCards = d.quotes.length ? d.quotes.map(q => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(q.transporterName)}</strong>${tBadge(q.status)}</div>
      <div>${esc(q.vehicleDescription)} &middot; <strong>${tMoney(q.price, q.currency)}</strong></div>
      ${q.message ? `<div class="small mt-1">${esc(q.message)}</div>` : ''}
      ${q.status === 'PENDING' && r.status === 'OPEN' ? `<div class="mt-2">
        <button class="btn btn-success btn-sm me-1" data-accept="${q.id}">Accept</button>
        <button class="btn btn-outline-danger btn-sm" data-reject="${q.id}">Decline</button></div>` : ''}
    </div></div>`).join('') : '<p class="text-muted">No quotes yet.</p>';
  const options = d.vehicles.length ? d.vehicles.map(v => `<div class="card mb-2"><div class="card-body py-2">
      <strong>${esc(v.description)}</strong> <span class="badge text-bg-light">${tNice(v.vehicleType)}</span>
      ${v.nearest ? '<span class="badge text-bg-info ms-1">Closest to pickup</span>' : ''}
      ${v.cheapest ? `<span class="badge text-bg-success ms-1">Cheapest in ${esc(v.currency)}</span>` : ''}<br>
      <small class="text-muted">${esc(v.transporterName)} &middot; carries up to ${tKg(v.capacityKg)} &middot; based in ${esc(v.baseProvince)}
        ${v.distanceToPickupKm != null ? ' &middot; about ' + esc(v.distanceToPickupKm) + ' km from the pickup' : ''}</small><br>
      ${v.estimate != null ? `Rough estimate: <strong>${tMoney(v.estimate, v.currency)}</strong>` : ''}
    </div></div>`).join('') : '<p class="text-muted">No registered vehicle fits this load yet. Your request stays visible to transporters.</p>';

  view.innerHTML = `<button class="btn btn-link px-0" id="tb">&larr; My transport requests</button>
    <h5>${tNice(r.cargoType)}: ${esc(r.description)} ${tBadge(r.status)}</h5>
    <p class="small text-muted">${tKg(r.weightKg)} &middot; ${tRoute(r)}
      ${r.distanceKm != null ? ' &middot; about ' + esc(r.distanceKm) + ' km in a straight line' : ''}
      ${r.neededBy ? ' &middot; needed by ' + esc(r.neededBy) : ''}${r.note ? '<br>' + esc(r.note) : ''}</p>
    ${tContact(d.transporter, 'Transporter')}
    ${r.status === 'BOOKED' ? '<div class="alert alert-info py-2">Booked. The transporter will mark the load as on the way and delivered.</div>' : ''}
    ${r.status === 'IN_TRANSIT' ? '<div class="alert alert-warning py-2">Your load is on the way.</div>' : ''}
    ${r.status === 'DELIVERED' ? '<div class="alert alert-success py-2">Delivered.</div>' : ''}
    <h6>Quotes from transporters</h6>${quoteCards}
    ${r.status === 'OPEN' ? `<h6 class="mt-3">Vehicles that fit this load</h6>
      <p class="small text-muted">Transporters with a fitting vehicle were told about your request. ${esc(T_ESTIMATE_NOTE)} Prices in different currencies are not compared.</p>${options}` : ''}
    ${['OPEN', 'BOOKED'].includes(r.status) ? '<button class="btn btn-outline-secondary btn-sm mt-2" id="cancelT">Cancel this request</button>' : ''}
    <p class="small text-muted mt-3">Payment is arranged between you and the transporter. AgriPulse does not take payments yet.</p>`;
  $('tb').onclick = () => run(transportScreen());
  view.querySelectorAll('[data-accept]').forEach(b => b.onclick = async () => {
    if (!confirm('Accept this quote? Your contact details are shared with this transporter and the other quotes are declined.')) return;
    try { await API.post(`/api/transport/requests/${id}/quotes/${b.dataset.accept}/accept`); run(transportDetail(id)); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-reject]').forEach(b => b.onclick = async () => {
    try { await API.post(`/api/transport/requests/${id}/quotes/${b.dataset.reject}/reject`); run(transportDetail(id)); } catch (e) { alert(e.message); }
  });
  if ($('cancelT')) $('cancelT').onclick = async () => {
    if (!confirm('Cancel this transport request? Transporters with quotes are told.')) return;
    try { await API.post(`/api/transport/requests/${id}/cancel`); run(transportDetail(id)); } catch (e) { alert(e.message); }
  };
}

/* ---------- Transporters: vehicles ---------- */
async function vehiclesScreen() {
  const list = await API.get('/api/transport/vehicles');
  view.innerHTML = `<h5 class="mb-3">My vehicles</h5>` + (list.length ? list.map(v => `
    <div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-start">
      <div><strong>${esc(v.description)}</strong> <span class="badge text-bg-light">${tNice(v.vehicleType)}</span><br>
        <small class="text-muted">Up to ${tKg(v.capacityKg)} &middot; carries ${v.cargoTypes.map(tNice).join(', ')} &middot; based in ${tPlace(v.baseProvince, v.baseDistrict)}<br>
        ${tMoney(v.ratePerKm, v.currency)} per km, minimum ${tMoney(v.minCharge, v.currency)}</small></div>
      <button class="btn btn-outline-danger btn-sm" data-rm="${v.id}">Remove</button></div></div>`).join('')
    : '<p class="text-muted">No vehicles yet. Add one so farmers and buyers can find you.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>Add a vehicle</h6>
      <form id="vf" class="row g-2">
        ${select('Type', 'vehicleType', TR_VEHICLES, 'TRUCK')}
        ${input('Description', 'description', 'required maxlength="150" placeholder="e.g. 7-tonne truck"')}
        ${input('Capacity', 'capacity', 'type="number" step="0.01" min="0.01" required')}
        ${select('Capacity unit', 'capacityUnit', ['TONNE', 'KG'])}
        <div class="col-12"><label class="form-label small mb-0">It can carry</label><div>
          ${TR_CARGO.map(c => `<div class="form-check form-check-inline"><input class="form-check-input" type="checkbox" name="ct" value="${c}" id="ct-${c}" ${c === 'PRODUCE' ? 'checked' : ''}>
            <label class="form-check-label" for="ct-${c}">${tNice(c)}</label></div>`).join('')}</div></div>
        ${select('Based in', 'baseProvince', PROVINCES)}
        ${input('District', 'baseDistrict')}
        ${input('Rate per km', 'ratePerKm', 'type="number" step="0.01" min="0" required')}
        ${input('Minimum charge', 'minCharge', 'type="number" step="0.01" min="0" value="0"')}
        ${select('Currency', 'currency', CURRENCIES)}
        <div class="col-12"><button class="btn btn-success w-100">Add vehicle</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">Farmers see your name, vehicle, home province and rates. Your phone number and email are shown only after a quote is accepted.
        Quotes are always in this vehicle's currency.</p></div></div>`;
  view.querySelectorAll('[data-rm]').forEach(b => b.onclick = async () => {
    if (!confirm('Remove this vehicle? Existing bookings are not affected.')) return;
    try { await API.del('/api/transport/vehicles/' + b.dataset.rm); run(vehiclesScreen()); } catch (e) { alert(e.message); }
  });
  $('vf').onsubmit = async e => {
    e.preventDefault();
    const f = e.target.elements;
    const cargoTypes = [...e.target.querySelectorAll('input[name=ct]:checked')].map(x => x.value);
    if (!cargoTypes.length) return alert('Choose at least one kind of cargo');
    try {
      await API.post('/api/transport/vehicles', { vehicleType: f['vehicleType'].value, description: f['description'].value,
        capacity: num(f['capacity'].value), capacityUnit: f['capacityUnit'].value, cargoTypes,
        baseProvince: f['baseProvince'].value, baseDistrict: f['baseDistrict'].value || null,
        ratePerKm: num(f['ratePerKm'].value), minCharge: num(f['minCharge'].value) || 0, currency: f['currency'].value });
      run(vehiclesScreen());
    } catch (err) { alert(err.message); }
  };
}

/* ---------- Transporters: jobs and bookings ---------- */
function jobCard(j) {
  const r = j.request;
  let action;
  if (j.alreadyQuoted) action = '<div class="small text-success mt-2">You have a quote waiting on this job.</div>';
  else if (!j.myVehicles.length) action = `<div class="small text-muted mt-2">None of your vehicles fits (it needs ${tKg(r.weightKg)} of capacity and ${tNice(r.cargoType)} cargo).</div>`;
  else action = `<details class="mt-2"><summary class="btn btn-success btn-sm">Send a quote</summary>
    <form class="row g-2 mt-1" data-quote="${r.id}">
      <div class="col-12"><label class="form-label small mb-0">Vehicle</label><select class="form-select" name="vehicleId">
        ${j.myVehicles.map(v => `<option value="${v.vehicleId}" data-est="${v.estimate ?? ''}">${esc(v.description)}${v.estimate != null ? ' (estimate ' + tMoney(v.estimate, v.currency) + ')' : ''}</option>`).join('')}</select></div>
      ${input('Your price', 'price', `type="number" step="0.01" min="0.01" required value="${j.myVehicles[0].estimate ?? ''}"`)}
      <div class="col-12"><input class="form-control" name="message" maxlength="500" placeholder="Message (optional)"></div>
      <div class="col-12"><button class="btn btn-success w-100">Send quote</button></div>
      <div class="col-12 small text-muted">If the requester accepts, they will see your phone number and email. The price is in the vehicle's currency.</div>
    </form></details>`;
  return `<div class="card mb-2"><div class="card-body">
    <div class="d-flex justify-content-between"><strong>${tNice(r.cargoType)}: ${esc(r.description)}</strong><span class="small text-muted">${esc(r.requesterName)}</span></div>
    <div>${tKg(r.weightKg)} &middot; ${tRoute(r)}</div>
    <small class="text-muted">${r.distanceKm != null ? 'About ' + esc(r.distanceKm) + ' km straight line' : ''}${r.neededBy ? ' &middot; needed by ' + esc(r.neededBy) : ''}</small>
    ${r.note ? `<div class="small mt-1">${esc(r.note)}</div>` : ''}${action}</div></div>`;
}

async function jobsScreen() {
  const [jobs, quotes] = await Promise.all([API.get('/api/transport/jobs'), API.get('/api/transport/quotes/mine')]);
  const booked = quotes.filter(q => q.status === 'ACCEPTED' && ['BOOKED', 'IN_TRANSIT', 'DELIVERED'].includes(q.requestStatus));
  const others = quotes.filter(q => !booked.includes(q));
  view.innerHTML = `<h5 class="mb-2">Transport jobs</h5>
    ${booked.length ? '<h6 class="mt-3">My bookings</h6>' + booked.map(q => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(q.requestSummary)}</strong>${tBadge(q.requestStatus)}</div>
      <div class="small">${esc(q.vehicleDescription)} &middot; ${tMoney(q.price, q.currency)}</div>
      ${tContact(q.contact, 'Requester')}
      ${q.requestStatus === 'BOOKED' ? `<button class="btn btn-warning btn-sm" data-status="IN_TRANSIT" data-r="${q.requestId}">Mark as on the way</button>` : ''}
      ${q.requestStatus === 'IN_TRANSIT' ? `<button class="btn btn-success btn-sm" data-status="DELIVERED" data-r="${q.requestId}">Mark as delivered</button>` : ''}
    </div></div>`).join('') : ''}
    <h6 class="mt-3">Open jobs</h6>
    ${jobs.length ? jobs.map(jobCard).join('') : '<p class="text-muted">No open jobs right now.</p>'}
    <p class="small text-muted">Jobs that fit one of your vehicles are listed first. ${esc(T_ESTIMATE_NOTE)}</p>
    <h6 class="mt-3">My quotes</h6>` + (others.length ? others.map(q => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(q.requestSummary)}</strong>${tBadge(q.status)}</div>
      <small class="text-muted">${esc(q.vehicleDescription)} &middot; ${tMoney(q.price, q.currency)}</small>
      ${q.status === 'PENDING' ? `<div class="mt-1"><button class="btn btn-outline-danger btn-sm" data-wd="${q.id}">Withdraw</button></div>` : ''}
    </div></div>`).join('') : '<p class="text-muted">No other quotes.</p>');

  view.querySelectorAll('[data-status]').forEach(b => b.onclick = async () => {
    if (!confirm('Update this job to ' + b.dataset.status.replace('_', ' ').toLowerCase() + '? The requester is told.')) return;
    try { await API.post(`/api/transport/requests/${b.dataset.r}/status`, { status: b.dataset.status }); run(jobsScreen()); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-wd]').forEach(b => b.onclick = async () => {
    try { await API.post(`/api/transport/quotes/${b.dataset.wd}/withdraw`); run(jobsScreen()); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('form[data-quote]').forEach(f => {
    const sel = f.elements['vehicleId'];
    sel.onchange = () => { const est = sel.selectedOptions[0].dataset.est; if (est) f.elements['price'].value = est; };
    f.onsubmit = async e => {
      e.preventDefault();
      try {
        await API.post(`/api/transport/requests/${f.dataset.quote}/quotes`, { vehicleId: Number(sel.value),
          price: num(f.elements['price'].value), message: f.elements['message'].value || null });
        alert('Quote sent. The requester can accept or decline it.'); run(jobsScreen());
      } catch (err) { alert(err.message); }
    };
  });
}
