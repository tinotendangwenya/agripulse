/* AgriPulse frontend: plain JavaScript, no build step. */
if (!API.token() || !API.user()) API.logout();
const me = API.user() || { name: '', role: 'BUYER' };
document.getElementById('who').textContent = me.name + ' (' + me.role + ')';

const PROVINCES = ['Harare', 'Bulawayo', 'Manicaland', 'Mashonaland Central', 'Mashonaland East',
  'Mashonaland West', 'Masvingo', 'Matabeleland North', 'Matabeleland South', 'Midlands'];
const UNITS = ['KG', 'TONNE', 'BAG', 'LITRE', 'CRATE', 'HEAD', 'DOZEN'];
const CATEGORIES = ['CROP', 'LIVESTOCK', 'POULTRY', 'FISH', 'OTHER'];
const CURRENCIES = ['USD', 'ZIG', 'ZAR'];
const CROP_STATUSES = ['PLANNED', 'PLANTED', 'GROWING', 'HARVESTED', 'FAILED'];

const $ = id => document.getElementById(id);
const view = $('view');
const num = v => (v === '' || v == null) ? null : Number(v);
const opts = (list, sel) => list.map(x => `<option ${x === sel ? 'selected' : ''}>${esc(x)}</option>`).join('');
const alertBox = (m, t = 'danger') => `<div class="alert alert-${t}">${esc(m)}</div>`;
const input = (label, name, extra = '') =>
  `<div class="col-6"><label class="form-label small mb-0">${label}</label><input class="form-control" name="${name}" ${extra}></div>`;
const select = (label, name, list, sel = '') =>
  `<div class="col-6"><label class="form-label small mb-0">${label}</label><select class="form-select" name="${name}">${opts(list, sel)}</select></div>`;
const BADGE = { PENDING: 'warning', CONFIRMED: 'primary', COMPLETED: 'success', CANCELLED: 'secondary', REJECTED: 'danger', ACTIVE: 'success' };
const badge = s => `<span class="badge text-bg-${BADGE[s] || 'secondary'}">${esc(s)}</span>`;
const run = p => p.catch(e => { view.innerHTML = alertBox(e.message); });
const submit = fn => async e => {
  e.preventDefault();
  try { await fn(Object.fromEntries(new FormData(e.target))); } catch (err) { alert(err.message); }
};

/* ---------- Farms ---------- */
async function farms() {
  const list = await API.get('/api/farms');
  view.innerHTML = `<h5 class="mb-3">My farms</h5>` +
    (list.length ? list.map(f => `
      <div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-center">
        <div><strong>${esc(f.name)}</strong><br><small class="text-muted">${esc(f.province)}${f.district ? ', ' + esc(f.district) : ''}${f.sizeHectares ? ' · ' + esc(f.sizeHectares) + ' ha' : ''}</small></div>
        <button class="btn btn-success btn-sm" data-farm="${f.id}">Open</button>
      </div></div>`).join('') : '<p class="text-muted">No farms yet. Add your first one below.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>Add a farm</h6>
      <form id="farmForm" class="row g-2">
        ${input('Farm name', 'name', 'required')}
        ${select('Province', 'province', PROVINCES)}
        ${input('District', 'district')}
        ${input('Size (hectares)', 'sizeHectares', 'type="number" step="0.01" min="0.01"')}
        ${input('Latitude (optional)', 'latitude', 'type="number" step="0.000001" min="-90" max="90"')}
        ${input('Longitude (optional)', 'longitude', 'type="number" step="0.000001" min="-180" max="180"')}
        <div class="col-12"><button class="btn btn-success w-100">Save farm</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">Add coordinates for a more accurate weather forecast.</p>
    </div></div>`;
  view.querySelectorAll('[data-farm]').forEach(b => b.onclick = () => run(farmDetail(b.dataset.farm)));
  $('farmForm').onsubmit = submit(async f => {
    await API.post('/api/farms', { name: f.name, province: f.province, district: f.district || null,
      sizeHectares: num(f.sizeHectares), latitude: num(f.latitude), longitude: num(f.longitude) });
    run(farms());
  });
}

async function farmDetail(id) {
  view.innerHTML = '<p>Loading...</p>';
  const [farm, crops] = await Promise.all([API.get('/api/farms/' + id), API.get(`/api/farms/${id}/crops`)]);
  let weather;
  try { weather = weatherCard(await API.get(`/api/farms/${id}/weather`)); }
  catch (e) { weather = alertBox('Weather unavailable: ' + e.message, 'warning'); }

  view.innerHTML = `
    <button class="btn btn-link px-0" id="back">&larr; All farms</button>
    <h5>${esc(farm.name)}</h5>
    <p class="text-muted">${esc(farm.province)}${farm.district ? ', ' + esc(farm.district) : ''}</p>
    <div class="d-flex gap-2 mb-3">
      <button class="btn btn-success flex-fill" id="recBtn">Farm records</button>
      <button class="btn btn-outline-success flex-fill" id="askBtn">Ask an expert</button></div>
    <h6>Crops</h6>
    ${crops.length ? crops.map(c => `
      <div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-start">
        <div><strong>${esc(c.cropName)}</strong> ${esc(c.variety || '')} ${badge(c.status)}<br>
          <small class="text-muted">${c.areaHectares ? esc(c.areaHectares) + ' ha · ' : ''}${esc(c.plantingDate || 'no planting date')}${c.expectedHarvestDate ? ' to ' + esc(c.expectedHarvestDate) : ''}</small></div>
        <button class="btn btn-outline-danger btn-sm" data-delcrop="${c.id}">Delete</button>
      </div></div>`).join('') : '<p class="text-muted">No crops recorded.</p>'}
    <div class="card mt-2"><div class="card-body"><h6>Add a crop</h6>
      <form id="cropForm" class="row g-2">
        ${input('Crop', 'cropName', 'required')}
        ${input('Variety', 'variety')}
        ${input('Area (ha)', 'areaHectares', 'type="number" step="0.01" min="0.01"')}
        ${select('Status', 'status', CROP_STATUSES)}
        ${input('Planting date', 'plantingDate', 'type="date"')}
        ${input('Expected harvest', 'expectedHarvestDate', 'type="date"')}
        <div class="col-12"><button class="btn btn-success w-100">Add crop</button></div>
      </form></div></div>
    ${weather}
    <button class="btn btn-outline-danger btn-sm mt-3" id="delFarm">Delete this farm</button>`;

  $('back').onclick = () => run(farms());
  $('recBtn').onclick = () => run(records(id));
  $('askBtn').onclick = () => run(askScreen(id));
  view.querySelectorAll('[data-delcrop]').forEach(b => b.onclick = async () => {
    if (!confirm('Delete this crop?')) return;
    try { await API.del(`/api/farms/${id}/crops/${b.dataset.delcrop}`); run(farmDetail(id)); } catch (e) { alert(e.message); }
  });
  $('delFarm').onclick = async () => {
    if (!confirm('Delete this farm, its crops and ALL its records (money, harvests, animals)? This cannot be undone.')) return;
    try { await API.del('/api/farms/' + id); run(farms()); } catch (e) { alert(e.message); }
  };
  $('cropForm').onsubmit = submit(async f => {
    await API.post(`/api/farms/${id}/crops`, { cropName: f.cropName, variety: f.variety || null,
      areaHectares: num(f.areaHectares), plantingDate: f.plantingDate || null,
      expectedHarvestDate: f.expectedHarvestDate || null, status: f.status });
    run(farmDetail(id));
  });
}

function weatherCard(w) {
  const cell = v => v == null ? '-' : esc(v);
  const alerts = w.alerts.length ? w.alerts.map(a => `
    <div class="alert alert-${a.severity === 'HIGH' ? 'danger' : 'warning'} py-2 mb-2">
      <strong>${esc(a.type.replace('_', ' '))}</strong> &middot; ${esc(a.date)}<br>${esc(a.message)}</div>`).join('')
    : '<p class="text-success mb-2">No alerts from the rules this week.</p>';
  const rows = w.days.map(d => `<tr><td>${esc(d.date)}</td><td>${cell(d.tempMaxC)}</td><td>${cell(d.tempMinC)}</td><td>${cell(d.rainMm)}</td><td>${cell(d.rainProbabilityPercent)}</td></tr>`).join('');
  return `<div class="card mt-3"><div class="card-body"><h6>Weather (7 days)</h6>
    <p class="small text-muted mb-2">${esc(w.locationSource)}<br>${esc(w.forecastSource)}<br>${esc(w.alertsSource)}</p>
    ${alerts}
    <div class="table-responsive"><table class="table table-sm mb-0">
      <thead><tr><th>Date</th><th>Max &deg;C</th><th>Min &deg;C</th><th>Rain mm</th><th>Rain %</th></tr></thead>
      <tbody>${rows}</tbody></table></div></div></div>`;
}

/* ---------- Farmer: my listings and incoming orders ---------- */
async function listings() {
  const list = await API.get('/api/products/mine');
  view.innerHTML = `<h5 class="mb-3">My listings</h5>` +
    (list.length ? list.map(p => `
      <div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-start">
        <div><strong>${esc(p.name)}</strong> ${badge(p.status)}<br>
          <small class="text-muted">${esc(p.quantity)} ${esc(p.unit)} · ${Number(p.price).toFixed(2)} ${esc(p.currency)} per ${esc(p.unit)} · ${esc(p.province)}</small></div>
        <button class="btn btn-outline-danger btn-sm" data-rm="${p.id}">Remove</button>
      </div></div>`).join('') : '<p class="text-muted">You have no listings yet.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>New listing</h6>
      <form id="pf" class="row g-2">
        ${input('Product', 'name', 'required')}
        ${select('Category', 'category', CATEGORIES)}
        ${input('Quantity', 'quantity', 'type="number" step="0.01" min="0.01" required')}
        ${select('Unit', 'unit', UNITS)}
        ${input('Price per unit', 'price', 'type="number" step="0.01" min="0.01" required')}
        ${select('Currency', 'currency', CURRENCIES)}
        ${select('Province', 'province', PROVINCES)}
        ${input('District', 'district')}
        <div class="col-12"><label class="form-label small mb-0">Description</label><textarea class="form-control" name="description" rows="2"></textarea></div>
        <div class="col-12"><button class="btn btn-success w-100">Publish listing</button></div>
      </form></div></div>`;
  view.querySelectorAll('[data-rm]').forEach(b => b.onclick = async () => {
    if (!confirm('Remove this listing?')) return;
    try { await API.del('/api/products/' + b.dataset.rm); run(listings()); } catch (e) { alert(e.message); }
  });
  $('pf').onsubmit = submit(async f => {
    await API.post('/api/products', { name: f.name, category: f.category, quantity: num(f.quantity), unit: f.unit,
      price: num(f.price), currency: f.currency, province: f.province, district: f.district || null,
      description: f.description || null });
    run(listings());
  });
}

async function orders() {
  const list = await API.get('/api/orders/incoming');
  view.innerHTML = `<h5 class="mb-3">Incoming orders</h5>` + (list.length ? list.map(o => `
    <div class="card mb-2"><div class="card-body">
      <div class="d-flex justify-content-between"><strong>${esc(o.productName)}</strong>${badge(o.status)}</div>
      <small class="text-muted">${esc(o.buyerName)} · ${esc(o.quantity)} ${esc(o.unit)} · total ${Number(o.totalPrice).toFixed(2)} ${esc(o.currency)}</small>
      ${o.note ? `<div class="small mt-1">Note: ${esc(o.note)}</div>` : ''}
      <div class="mt-2">
        ${o.status === 'PENDING' ? `<button class="btn btn-success btn-sm me-1" data-act="confirm" data-id="${o.id}">Confirm</button>
          <button class="btn btn-outline-danger btn-sm" data-act="reject" data-id="${o.id}">Reject</button>` : ''}
        ${o.status === 'CONFIRMED' ? `<button class="btn btn-primary btn-sm" data-act="complete" data-id="${o.id}">Mark completed</button>` : ''}
      </div></div></div>`).join('') : '<p class="text-muted">No orders yet.</p>');
  view.querySelectorAll('[data-act]').forEach(b => b.onclick = async () => {
    try { await API.post(`/api/orders/${b.dataset.id}/${b.dataset.act}`); run(orders()); } catch (e) { alert(e.message); }
  });
}

/* ---------- Marketplace (everyone) and buyer orders ---------- */
async function market() {
  view.innerHTML = `<h5 class="mb-3">Marketplace</h5>
    <form id="mf" class="row g-2 mb-3">
      <div class="col-12"><input class="form-control" name="q" placeholder="Search, e.g. maize"></div>
      <div class="col-6"><select class="form-select" name="province"><option value="">All provinces</option>${opts(PROVINCES)}</select></div>
      <div class="col-6"><select class="form-select" name="currency"><option value="">Any currency</option>${opts(CURRENCIES)}</select></div>
      <div class="col-6"><input class="form-control" name="maxPrice" type="number" step="0.01" placeholder="Max price"></div>
      <div class="col-6"><button class="btn btn-success w-100">Search</button></div>
    </form>
    <p class="small text-muted">Prices are shown in the seller's currency. Currencies are not converted.</p>
    <div id="results"></div>`;
  const load = async f => {
    const qs = new URLSearchParams();
    Object.entries(f).forEach(([k, v]) => { if (v) qs.set(k, v); });
    const list = await API.get('/api/products?' + qs);
    $('results').innerHTML = list.length ? list.map(p => `
      <div class="card mb-2"><div class="card-body">
        <strong>${esc(p.name)}</strong> <span class="badge text-bg-light">${esc(p.category)}</span><br>
        <span class="fs-5">${Number(p.price).toFixed(2)} ${esc(p.currency)}</span> <small class="text-muted">per ${esc(p.unit)}</small><br>
        <small class="text-muted">${esc(p.quantity)} ${esc(p.unit)} available · ${esc(p.province)}${p.district ? ', ' + esc(p.district) : ''} · seller ${esc(p.sellerName)}</small>
        ${p.description ? `<div class="small mt-1">${esc(p.description)}</div>` : ''}
        ${me.role === 'BUYER' ? `<div class="input-group input-group-sm mt-2">
          <input class="form-control" type="number" step="0.01" min="0.01" placeholder="Quantity (${esc(p.unit)})" id="qty-${p.id}">
          <button class="btn btn-success" data-order="${p.id}">Place order</button></div>` : ''}
      </div></div>`).join('') : '<p class="text-muted">No listings match.</p>';
    $('results').querySelectorAll('[data-order]').forEach(b => b.onclick = async () => {
      const q = num($('qty-' + b.dataset.order).value);
      if (!q || q <= 0) return alert('Enter a quantity');
      try { await API.post('/api/orders', { productId: Number(b.dataset.order), quantity: q });
        alert('Order sent. The seller must confirm it.'); location.hash = 'myorders'; } catch (e) { alert(e.message); }
    });
  };
  $('mf').onsubmit = submit(f => load(f));
  await load({});
}

async function myorders() {
  const list = await API.get('/api/orders/mine');
  view.innerHTML = `<h5 class="mb-3">My orders</h5>` + (list.length ? list.map(o => `
    <div class="card mb-2"><div class="card-body">
      <div class="d-flex justify-content-between"><strong>${esc(o.productName)}</strong>${badge(o.status)}</div>
      <small class="text-muted">${esc(o.quantity)} ${esc(o.unit)} · total ${Number(o.totalPrice).toFixed(2)} ${esc(o.currency)}</small>
      ${o.status === 'PENDING' ? `<div class="mt-2"><button class="btn btn-outline-danger btn-sm" data-cancel="${o.id}">Cancel</button></div>` : ''}
    </div></div>`).join('') : '<p class="text-muted">You have not ordered anything yet.</p>');
  view.querySelectorAll('[data-cancel]').forEach(b => b.onclick = async () => {
    try { await API.post(`/api/orders/${b.dataset.cancel}/cancel`); run(myorders()); } catch (e) { alert(e.message); }
  });
}

/* ---------- Market prices ---------- */
async function prices() {
  const commodities = await API.get('/api/prices/commodities');
  view.innerHTML = `<h5 class="mb-3">Market prices</h5>
    <form id="cf" class="row g-2 mb-3">
      <div class="col-6"><input class="form-control" name="commodity" list="cl" placeholder="Commodity" required>
        <datalist id="cl">${commodities.map(c => `<option value="${esc(c)}">`).join('')}</datalist></div>
      <div class="col-3"><select class="form-select" name="currency">${opts(CURRENCIES)}</select></div>
      <div class="col-3"><button class="btn btn-success w-100">Compare</button></div>
    </form>
    <div id="pr"></div>
    ${me.role === 'ADMIN' ? `
    <div class="card mt-3"><div class="card-body"><h6>Add a verified price (admin)</h6>
      <form id="af" class="row g-2">
        ${input('Commodity', 'commodity', 'required')}
        ${input('Market', 'market', 'required')}
        ${select('Province', 'province', PROVINCES)}
        ${input('Price', 'price', 'type="number" step="0.01" min="0.01" required')}
        ${select('Currency', 'currency', CURRENCIES)}
        ${select('Unit', 'unit', UNITS)}
        ${input('Price date', 'priceDate', 'type="date" required')}
        ${input('Source', 'source', 'required placeholder="Who reported this price"')}
        <div class="col-12"><button class="btn btn-success w-100">Save price</button></div>
      </form></div></div>` : ''}`;
  $('cf').onsubmit = submit(async f => {
    const list = await API.get(`/api/prices/compare?commodity=${encodeURIComponent(f.commodity)}&currency=${f.currency}`);
    $('pr').innerHTML = list.length ? `<div class="table-responsive"><table class="table table-sm bg-white">
      <thead><tr><th>Market</th><th>Price</th><th>Date</th><th>Source</th></tr></thead><tbody>` +
      list.map(p => `<tr><td>${esc(p.market)}<br><small class="text-muted">${esc(p.province)}</small></td>
        <td>${Number(p.price).toFixed(2)} ${esc(p.currency)}/${esc(p.unit)}</td><td>${esc(p.priceDate)}</td><td>${esc(p.source)}</td></tr>`).join('') +
      '</tbody></table></div><p class="small text-muted">Cheapest first. Latest price in each market.</p>'
      : '<p class="text-muted">No prices recorded for that commodity and currency.</p>';
  });
  if ($('af')) $('af').onsubmit = submit(async f => {
    await API.post('/api/admin/prices', { commodity: f.commodity, market: f.market, province: f.province,
      price: num(f.price), currency: f.currency, unit: f.unit, priceDate: f.priceDate, source: f.source });
    alert('Price saved'); run(prices());
  });
}

/* ---------- Navigation: tabs depend on the user's role ---------- */
const TABS = {
  FARMER: [['farms', 'Farms'], ['questions', 'Ask an expert'], ['listings', 'My listings'], ['orders', 'Orders'], ['requests', 'Buyer requests'], ['transport', 'Transport'], ['market', 'Market'], ['prices', 'Prices']],
  BUYER: [['market', 'Market'], ['requests', 'My requests'], ['myorders', 'My orders'], ['transport', 'Transport'], ['prices', 'Prices']],
  SUPPLIER: [['transport', 'Transport'], ['market', 'Market'], ['prices', 'Prices']],
  TRANSPORTER: [['jobs', 'Jobs'], ['vehicles', 'My vehicles'], ['prices', 'Prices']],
  EXPERT: [['queue', 'Questions'], ['market', 'Market'], ['prices', 'Prices']],
  ADMIN: [['prices', 'Prices'], ['experts', 'Experts']]
};
const SCREENS = { farms, questions: questionsList, requests: requestsScreen, listings, orders, market, myorders, prices: priceHub, queue: expertQueue, experts: adminExperts, transport: transportScreen, vehicles: vehiclesScreen, jobs: jobsScreen };

function nav() {
  const tabs = TABS[me.role] || TABS.BUYER;
  let cur = location.hash.slice(1);
  if (!tabs.some(t => t[0] === cur)) cur = tabs[0][0];
  $('tabs').innerHTML = tabs.map(([k, l]) =>
    `<a class="nav-link ${k === cur ? 'active' : ''}" href="#${k}">${l}</a>`).join('');
  run(SCREENS[cur]());
}
window.addEventListener('hashchange', nav);
nav();
