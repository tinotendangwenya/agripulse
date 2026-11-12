/* Farm supplies marketplace. Suppliers: business profile, products, stock. Farmers: search and compare. */
const SUPPLY_CATEGORIES = ['SEEDS', 'FERTILISERS', 'CHEMICALS', 'IRRIGATION', 'EQUIPMENT', 'LIVESTOCK', 'POULTRY', 'PROTECTIVE', 'GENERAL'];
const SUPPLY_UNITS = ['KG', 'TONNE', 'BAG', 'LITRE', 'PIECE', 'PACK', 'ROLL', 'METRE', 'SET'];

/* ---------- Supplier: my business, products and stock ---------- */
async function supplierShop() {
  const [profile, list] = await Promise.all([API.get('/api/supplies/profile'), API.get('/api/supplies/products/mine')]);
  const p = profile || {};
  const status = profile
    ? (profile.verified ? '<span class="badge text-bg-success">Verified</span>' : '<span class="badge text-bg-warning">Awaiting verification</span>')
    : '';

  let html = `<h5 class="mb-3">My supplies shop ${status}</h5>
    <div class="card mb-3"><div class="card-body"><h6>${profile ? 'Business profile' : 'Step 1: create your business profile'}</h6>
      <form id="spf" class="row g-2">
        ${input('Business name', 'businessName', `required value="${esc(p.businessName || '')}"`)}
        ${input('Phone', 'phone', `required value="${esc(p.phone || '')}"`)}
        ${select('Province', 'province', PROVINCES, p.province || '')}
        ${input('District', 'district', `value="${esc(p.district || '')}"`)}
        <div class="col-12"><label class="form-label small mb-0">Address</label><input class="form-control" name="address" value="${esc(p.address || '')}"></div>
        <div class="col-12"><label class="form-label small mb-0">About your business</label><textarea class="form-control" name="description" rows="2">${esc(p.description || '')}</textarea></div>
        <div class="col-12"><div class="form-check">
          <input class="form-check-input" type="checkbox" name="offersDelivery" id="od" ${p.offersDelivery ? 'checked' : ''}>
          <label class="form-check-label" for="od">We deliver to farms</label></div></div>
        <div class="col-12"><button class="btn btn-success w-100">${profile ? 'Save profile' : 'Create profile'}</button></div>
      </form></div></div>`;

  if (profile) {
    html += `<h6>My products</h6>` + (list.length ? list.map(x => `
      <div class="card mb-2"><div class="card-body">
        <div class="d-flex justify-content-between align-items-start">
          <div><strong>${esc(x.name)}</strong> ${x.brand ? esc(x.brand) : ''} <span class="badge text-bg-light">${esc(x.category)}</span><br>
            <small class="text-muted">${Number(x.price).toFixed(2)} ${esc(x.currency)} per ${esc(x.unit)}${x.expiryDate ? ' · expires ' + esc(x.expiryDate) : ''}${x.registrationNo ? ' · reg. ' + esc(x.registrationNo) : ''}</small></div>
          <button class="btn btn-outline-danger btn-sm" data-sprm="${x.id}">Remove</button>
        </div>
        <div class="input-group input-group-sm mt-2">
          <span class="input-group-text">In stock (${esc(x.unit)})</span>
          <input class="form-control" type="number" step="0.01" min="0" value="${esc(x.stockQty)}" id="stk-${x.id}">
          <button class="btn btn-outline-success" data-stk="${x.id}">Update stock</button>
        </div></div></div>`).join('') : '<p class="text-muted">No products yet. Add your first one below.</p>') + `
      <div class="card mt-3"><div class="card-body"><h6>Add a product</h6>
        <form id="spr" class="row g-2">
          ${input('Product name', 'name', 'required')}
          ${input('Brand', 'brand')}
          ${select('Category', 'category', SUPPLY_CATEGORIES)}
          ${select('Unit', 'unit', SUPPLY_UNITS)}
          ${input('Price per unit', 'price', 'type="number" step="0.01" min="0.01" required')}
          ${select('Currency', 'currency', CURRENCIES)}
          ${input('Quantity in stock', 'stockQty', 'type="number" step="0.01" min="0" required')}
          ${input('Registration no. (needed for chemicals)', 'registrationNo')}
          ${input('Expiry date (if any)', 'expiryDate', 'type="date"')}
          <div class="col-12"><label class="form-label small mb-0">Description</label><textarea class="form-control" name="description" rows="2"></textarea></div>
          <div class="col-12"><button class="btn btn-success w-100">Add product</button></div>
        </form></div></div>`;
  }
  view.innerHTML = html;

  $('spf').onsubmit = submit(async f => {
    await API.put('/api/supplies/profile', { businessName: f.businessName, province: f.province,
      district: f.district || null, address: f.address || null, phone: f.phone,
      description: f.description || null, offersDelivery: f.offersDelivery === 'on' });
    run(supplierShop());
  });

  view.querySelectorAll('[data-sprm]').forEach(b => b.onclick = async () => {
    if (!confirm('Remove this product?')) return;
    try { await API.del('/api/supplies/products/' + b.dataset.sprm); run(supplierShop()); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-stk]').forEach(b => b.onclick = async () => {
    const v = num($('stk-' + b.dataset.stk).value);
    if (v == null || v < 0) return alert('Enter the stock quantity');
    try { await API.put(`/api/supplies/products/${b.dataset.stk}/stock`, { stockQty: v }); alert('Stock updated'); }
    catch (e) { alert(e.message); }
  });

  if ($('spr')) $('spr').onsubmit = submit(async f => {
    await API.post('/api/supplies/products', { name: f.name, brand: f.brand || null, category: f.category,
      description: f.description || null, unit: f.unit, price: num(f.price), currency: f.currency,
      stockQty: num(f.stockQty), registrationNo: f.registrationNo || null, expiryDate: f.expiryDate || null });
    run(supplierShop());
  });
}

/* ---------- Farmers: search and compare supplies ---------- */
async function suppliesBrowse() {
  view.innerHTML = `<h5 class="mb-3">Farm supplies</h5>
    <form id="sf" class="row g-2 mb-2">
      <div class="col-12"><input class="form-control" name="q" placeholder="Search, e.g. Compound D, maize seed"></div>
      <div class="col-6"><select class="form-select" name="category"><option value="">All categories</option>${opts(SUPPLY_CATEGORIES)}</select></div>
      <div class="col-6"><select class="form-select" name="province"><option value="">All provinces</option>${opts(PROVINCES)}</select></div>
      <div class="col-6"><select class="form-select" name="currency"><option value="">Any currency</option>${opts(CURRENCIES)}</select></div>
      <div class="col-6"><input class="form-control" name="maxPrice" type="number" step="0.01" placeholder="Max price"></div>
      <div class="col-6"><select class="form-select" name="sort"><option value="price">Cheapest first</option><option value="newest">Newest first</option></select></div>
      <div class="col-6 d-flex align-items-center"><div class="form-check">
        <input class="form-check-input" type="checkbox" name="deliveryOnly" id="dOnly">
        <label class="form-check-label" for="dOnly">Delivers to farms</label></div></div>
      <div class="col-12"><button class="btn btn-success w-100">Search</button></div>
    </form>
    <p class="small text-muted">Prices are in each supplier's own currency and are not converted.</p>
    <div id="sres"></div>`;

  const load = async f => {
    const qs = new URLSearchParams();
    Object.entries(f).forEach(([k, v]) => { if (v) qs.set(k, k === 'deliveryOnly' ? 'true' : v); });
    const list = await API.get('/api/supplies/products?' + qs);
    $('sres').innerHTML = list.length ? list.map(p => `
      <div class="card mb-2"><div class="card-body">
        <strong>${esc(p.name)}</strong>${p.brand ? ' <span class="text-muted">' + esc(p.brand) + '</span>' : ''} <span class="badge text-bg-light">${esc(p.category)}</span><br>
        <span class="fs-5">${Number(p.price).toFixed(2)} ${esc(p.currency)}</span> <small class="text-muted">per ${esc(p.unit)}</small><br>
        ${p.inStock ? `<span class="badge text-bg-success">${esc(p.stockQty)} ${esc(p.unit)} in stock</span>` : '<span class="badge text-bg-secondary">Out of stock</span>'}
        ${p.supplier.offersDelivery ? '<span class="badge text-bg-info">Delivers</span>' : ''}<br>
        <small class="text-muted">${esc(p.supplier.businessName)} ${p.supplier.verified ? '<span class="badge text-bg-success">Verified</span>' : ''} · ${esc(p.supplier.province)}${p.supplier.district ? ', ' + esc(p.supplier.district) : ''}${p.expiryDate ? ' · expires ' + esc(p.expiryDate) : ''}</small>
        ${p.description ? `<div class="small mt-1">${esc(p.description)}</div>` : ''}
      </div></div>`).join('') : '<p class="text-muted">No supplies match your search.</p>';
  };
  $('sf').onsubmit = submit(f => load(f));
  await load({});
}