/* Farm records: summary report, money, harvests, animals. Helpers ($, view, esc, input, select, ...) come from app.js. */
const INCOME_CATS = ['SALES', 'OTHER_INCOME'];
const EXPENSE_CATS = ['SEEDS', 'FERTILISER', 'CHEMICALS', 'LABOUR', 'FEED', 'VETERINARY', 'MACHINERY', 'FUEL', 'TRANSPORT', 'OTHER'];
const ANIMALS = ['CATTLE', 'GOAT', 'SHEEP', 'PIG', 'POULTRY', 'FISH', 'OTHER'];
const EVENTS = ['OPENING', 'PURCHASE', 'BIRTH', 'DEATH', 'SALE', 'VACCINATION', 'TREATMENT', 'FEEDING', 'OTHER'];
const EVENT_HELP = 'OPENING means animals already on the farm when you start recording.';

const localDay = offsetDays => new Date(Date.now() - new Date().getTimezoneOffset() * 60000 - offsetDays * 86400000).toISOString().slice(0, 10);
const nice = s => esc(String(s).replace(/_/g, ' ').toLowerCase());
const amt = (v, c) => Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ' + esc(c);
const kgText = kg => Number(kg) >= 1000 ? (Number(kg) / 1000).toFixed(2) + ' t' : Number(kg).toFixed(0) + ' kg';
let recFrom = localDay(364), recTo = localDay(0);

const rangeForm = () => `<form id="rangeForm" class="row g-2 mb-3 align-items-end">
  ${input('From', 'from', `type="date" value="${recFrom}" required`)}${input('To', 'to', `type="date" value="${recTo}" required`)}
  <div class="col-12"><button class="btn btn-outline-success btn-sm w-100">Show this period</button></div></form>`;
const bindRange = (id, tab) => { $('rangeForm').onsubmit = submit(async f => { recFrom = f.from; recTo = f.to; run(records(id, tab)); }); };

async function records(farmId, tab = 'summary') {
  const tabs = [['summary', 'Summary'], ['money', 'Money'], ['harvests', 'Harvests'], ['animals', 'Animals']];
  view.innerHTML = `<button class="btn btn-link px-0" id="rBack">&larr; Back to farm</button>
    <h5>Farm records</h5>
    <ul class="nav nav-pills nav-fill mb-3">${tabs.map(([k, l]) =>
      `<li class="nav-item"><button class="nav-link ${k === tab ? 'active' : ''}" data-tab="${k}">${l}</button></li>`).join('')}</ul>
    <div id="rBody"><p>Loading...</p></div>`;
  $('rBack').onclick = () => run(farmDetail(farmId));
  view.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => run(records(farmId, b.dataset.tab)));
  const fn = { summary: recSummary, money: recMoney, harvests: recHarvests, animals: recAnimals }[tab];
  try { await fn(farmId); } catch (e) { $('rBody').innerHTML = alertBox(e.message); }
}

/* ---------- Summary report ---------- */
async function recSummary(id) {
  const d = await API.get(`/api/farms/${id}/records/summary?from=${recFrom}&to=${recTo}`);
  const prev = Object.fromEntries(d.previousMoney.map(m => [m.currency, m]));
  const money = d.money.length ? d.money.map(m => {
    const p = prev[m.currency];
    return `<div class="card mb-2"><div class="card-body"><h6>${esc(m.currency)}</h6>
      <div class="d-flex justify-content-between"><span>Income</span><strong>${amt(m.income, m.currency)}</strong></div>
      <div class="d-flex justify-content-between"><span>Expenses</span><strong>${amt(m.expenses, m.currency)}</strong></div>
      <div class="d-flex justify-content-between border-top mt-1 pt-1"><span>Profit / loss</span>
        <strong class="${Number(m.profit) < 0 ? 'text-danger' : 'text-success'}">${amt(m.profit, m.currency)}</strong></div>
      ${p ? `<div class="small text-muted mt-1">Previous period (${esc(d.previousPeriod.from)} to ${esc(d.previousPeriod.to)}):
        income ${amt(p.income, m.currency)}, expenses ${amt(p.expenses, m.currency)}, profit ${amt(p.profit, m.currency)}</div>`
        : '<div class="small text-muted mt-1">Nothing recorded in the previous period.</div>'}
      ${m.expensesByCategory.length ? '<div class="small mt-2">Expenses by type:<ul class="mb-0">' +
        m.expensesByCategory.map(c => `<li>${nice(c.category)}: ${amt(c.amount, m.currency)}</li>`).join('') + '</ul></div>' : ''}
    </div></div>`;
  }).join('') : '<p class="text-muted">No money recorded in this period.</p>';

  const crops = d.crops.length ? `<div class="table-responsive"><table class="table table-sm bg-white">
    <thead><tr><th>Crop</th><th>Area</th><th>Harvested</th><th>Yield</th><th>Linked money</th></tr></thead><tbody>` +
    d.crops.map(c => `<tr><td>${esc(c.cropName)}</td><td>${c.areaHectares ? esc(c.areaHectares) + ' ha' : '-'}</td>
      <td>${kgText(c.harvestedKg)}</td><td>${c.yieldTonnesPerHa != null ? esc(c.yieldTonnesPerHa) + ' t/ha' : '-'}</td>
      <td>${c.linkedMoney.map(x => `${amt(x.income, x.currency)} in, ${amt(x.expenses, x.currency)} out`).join('<br>') || '-'}</td></tr>`).join('') +
    '</tbody></table></div>' : '<p class="text-muted">No harvests or crop-linked money in this period.</p>';

  const herd = d.herd.length ? '<ul class="mb-0">' + d.herd.map(h => `<li>${nice(h.animalType)}: ${esc(h.head)}</li>`).join('') + '</ul>'
    : '<p class="text-muted mb-0">No animals recorded.</p>';

  $('rBody').innerHTML = rangeForm() + `<h6>Money</h6>${money}<h6 class="mt-3">Crops</h6>${crops}
    <h6 class="mt-3">Animals on the farm today</h6>${herd}
    <p class="small text-muted mt-3">${d.notes.map(esc).join('<br>')}</p>
    <button class="btn btn-outline-secondary btn-sm" onclick="window.print()">Print or save as PDF</button>`;
  bindRange(id, 'summary');
}

/* ---------- Money ---------- */
async function recMoney(id) {
  const [list, crops] = await Promise.all([
    API.get(`/api/farms/${id}/records/transactions?from=${recFrom}&to=${recTo}`), API.get(`/api/farms/${id}/crops`)]);
  $('rBody').innerHTML = rangeForm() + (list.length ? `<div class="table-responsive"><table class="table table-sm bg-white">
    <thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Note</th><th></th></tr></thead><tbody>` +
    list.map(t => `<tr><td>${esc(t.txDate)}</td>
      <td><span class="badge text-bg-${t.type === 'INCOME' ? 'success' : 'secondary'}">${nice(t.type)}</span> ${nice(t.category)}</td>
      <td>${amt(t.amount, t.currency)}</td><td>${esc(t.description || '')}</td>
      <td><button class="btn btn-outline-danger btn-sm" data-del="${t.id}">x</button></td></tr>`).join('') +
    '</tbody></table></div>' : '<p class="text-muted">No money recorded in this period.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>Add income or an expense</h6>
      <form id="tf" class="row g-2">
        ${select('Type', 'type', ['EXPENSE', 'INCOME'])}
        <div class="col-6"><label class="form-label small mb-0">Category</label><select class="form-select" name="category" id="catSel"></select></div>
        ${input('Amount', 'amount', 'type="number" step="0.01" min="0.01" required')}
        ${select('Currency', 'currency', CURRENCIES)}
        ${input('Date', 'txDate', `type="date" value="${localDay(0)}" required`)}
        <div class="col-6"><label class="form-label small mb-0">For which crop? (optional)</label>
          <select class="form-select" name="cropRecordId"><option value="">Whole farm</option>
          ${crops.map(c => `<option value="${c.id}">${esc(c.cropName)}</option>`).join('')}</select></div>
        <div class="col-12"><label class="form-label small mb-0">Note (optional)</label><input class="form-control" name="description" maxlength="300" placeholder="e.g. 20 bags of fertiliser"></div>
        <div class="col-12"><button class="btn btn-success w-100">Save</button></div>
      </form></div></div>`;
  bindRange(id, 'money');
  const fillCats = () => { $('catSel').innerHTML = opts($('tf').elements['type'].value === 'INCOME' ? INCOME_CATS : EXPENSE_CATS); };
  fillCats();
  $('tf').elements['type'].onchange = fillCats;
  view.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => {
    if (!confirm('Delete this record?')) return;
    try { await API.del(`/api/farms/${id}/records/transactions/${b.dataset.del}`); run(records(id, 'money')); } catch (e) { alert(e.message); }
  });
  $('tf').onsubmit = submit(async f => {
    await API.post(`/api/farms/${id}/records/transactions`, { type: f.type, category: f.category, amount: num(f.amount),
      currency: f.currency, txDate: f.txDate, description: f.description || null,
      cropRecordId: f.cropRecordId ? Number(f.cropRecordId) : null });
    run(records(id, 'money'));
  });
}

/* ---------- Harvests ---------- */
async function recHarvests(id) {
  const [list, crops] = await Promise.all([
    API.get(`/api/farms/${id}/records/harvests?from=${recFrom}&to=${recTo}`), API.get(`/api/farms/${id}/crops`)]);
  $('rBody').innerHTML = rangeForm() + (list.length ? `<div class="table-responsive"><table class="table table-sm bg-white">
    <thead><tr><th>Date</th><th>Crop</th><th>Quantity</th><th>Note</th><th></th></tr></thead><tbody>` +
    list.map(h => `<tr><td>${esc(h.harvestDate)}</td><td>${esc(h.cropName)}</td><td>${kgText(h.quantityKg)}</td>
      <td>${esc(h.note || '')}</td><td><button class="btn btn-outline-danger btn-sm" data-del="${h.id}">x</button></td></tr>`).join('') +
    '</tbody></table></div>' : '<p class="text-muted">No harvests recorded in this period.</p>') + (crops.length ? `
    <div class="card mt-3"><div class="card-body"><h6>Record a harvest</h6>
      <form id="hf" class="row g-2">
        <div class="col-6"><label class="form-label small mb-0">Crop</label><select class="form-select" name="cropRecordId">
          ${crops.map(c => `<option value="${c.id}">${esc(c.cropName)}${c.variety ? ' (' + esc(c.variety) + ')' : ''}</option>`).join('')}</select></div>
        ${input('Date', 'harvestDate', `type="date" value="${localDay(0)}" required`)}
        ${input('Quantity', 'quantity', 'type="number" step="0.01" min="0.01" required')}
        ${select('Unit', 'unit', ['KG', 'TONNE'])}
        <div class="col-12"><label class="form-label small mb-0">Note (optional)</label><input class="form-control" name="note" maxlength="300"></div>
        <div class="col-12"><button class="btn btn-success w-100">Save harvest</button></div>
      </form>
      <p class="small text-muted mt-2 mb-0">Yield per hectare uses the area you gave when you added the crop.</p></div></div>`
    : '<p class="text-muted mt-3">Add a crop to the farm first, then you can record its harvest.</p>');
  bindRange(id, 'harvests');
  view.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => {
    if (!confirm('Delete this harvest record?')) return;
    try { await API.del(`/api/farms/${id}/records/harvests/${b.dataset.del}`); run(records(id, 'harvests')); } catch (e) { alert(e.message); }
  });
  if ($('hf')) $('hf').onsubmit = submit(async f => {
    await API.post(`/api/farms/${id}/records/harvests`, { cropRecordId: Number(f.cropRecordId), harvestDate: f.harvestDate,
      quantity: num(f.quantity), unit: f.unit, note: f.note || null });
    run(records(id, 'harvests'));
  });
}

/* ---------- Animals ---------- */
async function recAnimals(id) {
  const [list, sum] = await Promise.all([
    API.get(`/api/farms/${id}/records/livestock`), API.get(`/api/farms/${id}/records/summary`)]);
  const herd = sum.herd.length ? sum.herd.map(h => `<span class="badge text-bg-success me-1">${nice(h.animalType)}: ${esc(h.head)}</span>`).join('')
    : '<span class="text-muted">No animals recorded yet.</span>';
  $('rBody').innerHTML = `<div class="card mb-3"><div class="card-body"><h6>On the farm today</h6>${herd}</div></div>` +
    (list.length ? `<div class="table-responsive"><table class="table table-sm bg-white">
    <thead><tr><th>Date</th><th>Event</th><th>Animals</th><th>Note</th><th></th></tr></thead><tbody>` +
    list.map(e => `<tr><td>${esc(e.eventDate)}</td><td>${nice(e.eventType)}</td><td>${esc(e.headCount)} ${nice(e.animalType)}</td>
      <td>${esc(e.note || '')}</td><td><button class="btn btn-outline-danger btn-sm" data-del="${e.id}">x</button></td></tr>`).join('') +
    '</tbody></table></div>' : '<p class="text-muted">No animal events yet.</p>') + `
    <div class="card mt-3"><div class="card-body"><h6>Record an animal event</h6>
      <form id="lf" class="row g-2">
        ${select('Animal', 'animalType', ANIMALS)}
        ${select('Event', 'eventType', EVENTS, 'OPENING')}
        ${input('Date', 'eventDate', `type="date" value="${localDay(0)}" required`)}
        ${input('Number of animals', 'headCount', 'type="number" min="1" step="1" required')}
        <div class="col-12"><label class="form-label small mb-0">Note (optional)</label><input class="form-control" name="note" maxlength="300" placeholder="e.g. foot-and-mouth vaccine, batch number"></div>
        <div class="col-12"><button class="btn btn-success w-100">Save</button></div>
      </form><p class="small text-muted mt-2 mb-0">${EVENT_HELP}</p></div></div>`;
  view.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => {
    if (!confirm('Delete this record?')) return;
    try { await API.del(`/api/farms/${id}/records/livestock/${b.dataset.del}`); run(records(id, 'animals')); } catch (e) { alert(e.message); }
  });
  $('lf').onsubmit = submit(async f => {
    await API.post(`/api/farms/${id}/records/livestock`, { animalType: f.animalType, eventType: f.eventType,
      eventDate: f.eventDate, headCount: Number(f.headCount), note: f.note || null });
    run(records(id, 'animals'));
  });
}
