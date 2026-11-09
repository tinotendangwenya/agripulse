/* "Check a leaf" card on the farm page. Uses helpers from app.js ($, select, alertBox, esc, API). */
const LEAF_CROPS = ['maize', 'tomato', 'potato', 'pepper', 'soybean'];

function leafCard() {
  return `<div class="card mt-3"><div class="card-body"><h6>Check a leaf (AI, preliminary)</h6>
    <form id="leafForm" class="row g-2">
      ${select('Crop', 'crop', LEAF_CROPS)}
      <div class="col-6"><label class="form-label small mb-0">Photo</label>
        <input class="form-control" type="file" name="photo" accept="image/*" required></div>
      <div class="col-12"><button class="btn btn-success w-100">Check leaf</button></div>
    </form>
    <div id="leafOut" class="mt-2"></div>
    <p class="small text-muted mb-0">Take a close-up of one leaf in daylight. This is not a diagnosis:
      confirm with an agricultural extension officer. Tobacco and wheat are not supported yet.</p>
  </div></div>`;
}

/* Shrink the photo in the browser: smaller upload on weak connections */
function shrink(file, max = 1024) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      const s = Math.min(1, max / Math.max(img.width, img.height));
      const c = document.createElement('canvas');
      c.width = Math.round(img.width * s);
      c.height = Math.round(img.height * s);
      c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
      URL.revokeObjectURL(url);
      c.toBlob(b => b ? resolve(b) : reject(new Error('Could not read the photo')), 'image/jpeg', 0.85);
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Could not read the photo')); };
    img.src = url;
  });
}

function leafResult(d) {
  const type = d.status === 'OK' ? 'success' : 'warning';
  const list = d.predictions.length
    ? '<ul class="mb-2 mt-1">' + d.predictions.map(p =>
        `<li>${esc(p.label)} (${Math.round(p.confidence * 100)}%)</li>`).join('') + '</ul>' : '';
  return `<div class="alert alert-${type}"><strong>${esc(d.status.replace('_', ' '))}</strong>${list}
    <div>${esc(d.advice)}</div><div class="small mt-1">${esc(d.limitations)}</div></div>`;
}

function bindLeaf(farmId) {
  const f = $('leafForm');
  if (!f) return;
  f.onsubmit = async e => {
    e.preventDefault();
    const out = $('leafOut');
    out.innerHTML = '<p class="text-muted">Checking...</p>';
    try {
      const blob = await shrink(f.photo.files[0]);
      const fd = new FormData();
      fd.append('file', blob, 'leaf.jpg');
      fd.append('crop', f.crop.value);
      fd.append('farmId', farmId);
      const res = await fetch('/api/ai/crop-check', { method: 'POST', headers: { Authorization: 'Bearer ' + API.token() }, body: fd });
      if (res.status === 401) { API.logout(); return; }
      const text = await res.text();
      let d = {};
      try { d = JSON.parse(text); } catch (_) {}
      if (!res.ok) throw new Error(d.message || 'Error ' + res.status);
      out.innerHTML = leafResult(d);
    } catch (err) { out.innerHTML = alertBox(err.message); }
  };
}
