/* Ask an expert (farmers), answer questions (experts), create expert accounts (admin). */
const Q_COLOR = { OPEN: 'warning', ANSWERED: 'success', CLOSED: 'secondary' };
const qBadge = s => `<span class="badge text-bg-${Q_COLOR[s] || 'secondary'}">${esc(s)}</span>`;
const qWhen = s => esc(String(s || '').replace('T', ' ').slice(0, 16));
const ROLE_LABEL = { FARMER: 'Farmer', EXPERT: 'Expert', ADMIN: 'Admin' };

/* Shrink the photo in the browser: smaller upload on weak connections */
function shrinkImage(file, max = 1024) {
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

async function sendForm(path, fd) {
  const res = await fetch(path, { method: 'POST', headers: { Authorization: 'Bearer ' + API.token() }, body: fd });
  if (res.status === 401) { API.logout(); throw new Error('Session expired, please log in again'); }
  const text = await res.text();
  let d = null;
  try { d = text ? JSON.parse(text) : null; } catch (e) { d = { message: text }; }
  if (!res.ok) throw new Error((d && (d.message || d.error)) || 'Error ' + res.status);
  return d;
}

/* Photos need the login token, so they are fetched and shown from memory */
async function loadImages(root) {
  for (const img of root.querySelectorAll('img[data-src]')) {
    try {
      const res = await fetch(img.dataset.src, { headers: { Authorization: 'Bearer ' + API.token() } });
      if (!res.ok) throw new Error('x');
      img.src = URL.createObjectURL(await res.blob());
      img.onclick = () => window.open(img.src);
    } catch (e) { img.alt = 'Photo could not be loaded'; }
  }
}

/* ---------- Farmer: my questions ---------- */
async function questionsList() {
  const list = await API.get('/api/questions/mine');
  view.innerHTML = `<div class="d-flex justify-content-between align-items-center mb-3">
      <h5 class="mb-0">Ask an expert</h5><button class="btn btn-success btn-sm" id="newQ">New question</button></div>` +
    (list.length ? list.map(q => `<div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-center">
        <div><strong>${esc(q.title)}</strong> ${qBadge(q.status)}<br>
          <small class="text-muted">${esc(q.subject)}${q.topic ? ' · ' + esc(q.topic) : ''} · updated ${qWhen(q.updatedAt)}</small></div>
        <button class="btn btn-outline-success btn-sm" data-q="${q.id}">Open</button></div></div>`).join('')
      : '<p class="text-muted">You have not asked anything yet. Agricultural experts answer here, and you can add photos.</p>');
  $('newQ').onclick = () => run(askScreen(null));
  view.querySelectorAll('[data-q]').forEach(b => b.onclick = () => run(qThread(b.dataset.q)));
}

async function askScreen(farmId) {
  const farmList = await API.get('/api/farms');
  view.innerHTML = `<button class="btn btn-link px-0" id="qBack">&larr; Back</button>
    <h5 class="mb-3">New question for the experts</h5>
    <form id="qf" class="card card-body">
      <label class="form-label small mb-0">About</label>
      <select class="form-select mb-2" name="subject"><option value="CROP">Crops</option><option value="LIVESTOCK">Livestock</option><option value="OTHER">Something else</option></select>
      <label class="form-label small mb-0">Which crop or animal? (optional)</label>
      <input class="form-control mb-2" name="topic" maxlength="80" placeholder="e.g. maize, cattle, tomatoes">
      <label class="form-label small mb-0">Which farm? (optional)</label>
      <select class="form-select mb-2" name="farmId"><option value="">Not about a particular farm</option>
        ${farmList.map(f => `<option value="${f.id}" ${String(f.id) === String(farmId) ? 'selected' : ''}>${esc(f.name)}</option>`).join('')}</select>
      <label class="form-label small mb-0">Short title</label>
      <input class="form-control mb-2" name="title" required minlength="3" maxlength="150" placeholder="e.g. Orange spots on maize leaves">
      <label class="form-label small mb-0">Describe what you see</label>
      <textarea class="form-control mb-2" name="body" rows="4" maxlength="4000" placeholder="When did it start? How many plants or animals? Any change in weather or feed?"></textarea>
      <label class="form-label small mb-0">Photo (optional)</label>
      <input class="form-control mb-3" type="file" name="photo" accept="image/*">
      <button class="btn btn-success">Send to the experts</button>
    </form>
    <p class="small text-muted mt-2">Experts see your name, your farm's province and your question. They do not see your phone number or email. For a sick animal that may be a serious disease, also contact a vet right away.</p>`;
  $('qBack').onclick = () => run(questionsList());
  $('qf').onsubmit = async e => {
    e.preventDefault();
    const f = e.target, el = f.elements, btn = f.querySelector('button');
    btn.disabled = true;
    try {
      const fd = new FormData();
      fd.append('subject', el['subject'].value);
      fd.append('topic', el['topic'].value);
      fd.append('title', el['title'].value);
      fd.append('body', el['body'].value);
      if (el['farmId'].value) fd.append('farmId', el['farmId'].value);
      const photo = el['photo'].files[0];
      if (photo) fd.append('file', await shrinkImage(photo), 'photo.jpg');
      const d = await sendForm('/api/questions', fd);
      run(qThread(d.question.id));
    } catch (err) { alert(err.message); btn.disabled = false; }
  };
}

/* ---------- A conversation (farmer or expert) ---------- */
async function qThread(id) {
  const d = await API.get('/api/questions/' + id);
  const q = d.question;
  const staff = me.role !== 'FARMER';
  view.innerHTML = `<button class="btn btn-link px-0" id="qBack">&larr; Back</button>
    <h5>${esc(q.title)} ${qBadge(q.status)}</h5>
    <p class="small text-muted">${esc(q.subject)}${q.topic ? ' · ' + esc(q.topic) : ''}${staff ? ' · ' + esc(q.farmerName) + (q.province ? ' (' + esc(q.province) + ')' : '') : ''}</p>
    ${d.messages.map(m => `<div class="card mb-2 ${m.authorRole === 'FARMER' ? '' : 'border border-success'}"><div class="card-body py-2">
        <div class="small text-muted">${esc(m.authorName)} · ${ROLE_LABEL[m.authorRole] || ''} · ${qWhen(m.createdAt)}</div>
        ${m.body ? `<div style="white-space:pre-wrap">${esc(m.body)}</div>` : ''}
        ${m.hasImage ? `<img class="qimg mt-2" data-src="/api/questions/${q.id}/messages/${m.id}/image" alt="Loading photo...">` : ''}
      </div></div>`).join('')}
    ${q.status === 'CLOSED' ? '<p class="text-muted">This question is closed.</p>' : `
    <form id="rf" class="card card-body mt-3">
      <label class="form-label small mb-0">${staff ? 'Your answer' : 'Add more information or ask a follow-up'}</label>
      <textarea class="form-control mb-2" name="body" rows="3" maxlength="4000"></textarea>
      <input class="form-control mb-2" type="file" name="photo" accept="image/*">
      <button class="btn btn-success">Send</button>
    </form>`}
    ${!staff && q.status !== 'CLOSED' ? '<button class="btn btn-outline-secondary btn-sm mt-3" id="closeQ">Mark as solved</button>' : ''}`;
  loadImages(view);
  $('qBack').onclick = () => run(staff ? expertQueue() : questionsList());
  if ($('rf')) $('rf').onsubmit = async e => {
    e.preventDefault();
    const f = e.target, btn = f.querySelector('button');
    btn.disabled = true;
    try {
      const fd = new FormData();
      fd.append('body', f.elements['body'].value);
      const photo = f.elements['photo'].files[0];
      if (photo) fd.append('file', await shrinkImage(photo), 'photo.jpg');
      await sendForm(`/api/questions/${id}/messages`, fd);
      run(qThread(id));
    } catch (err) { alert(err.message); btn.disabled = false; }
  };
  if ($('closeQ')) $('closeQ').onclick = async () => {
    if (!confirm('Mark this question as solved? You will not be able to reply afterwards.')) return;
    try { await API.post(`/api/questions/${id}/close`); run(qThread(id)); } catch (e) { alert(e.message); }
  };
}

/* ---------- Expert: the queue ---------- */
async function expertQueue(status = 'OPEN') {
  const list = await API.get('/api/questions/queue?status=' + status);
  view.innerHTML = `<h5 class="mb-2">Farmers' questions</h5>
    <select class="form-select mb-3" id="qStatus">
      <option value="OPEN">Waiting for an answer (oldest first)</option><option value="ANSWERED">Answered</option>
      <option value="CLOSED">Closed</option><option value="ALL">All</option></select>` +
    (list.length ? list.map(q => `<div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-center">
        <div><strong>${esc(q.title)}</strong> ${qBadge(q.status)}<br>
          <small class="text-muted">${esc(q.farmerName)}${q.province ? ' (' + esc(q.province) + ')' : ''} · ${esc(q.subject)}${q.topic ? ' · ' + esc(q.topic) : ''} · ${qWhen(q.updatedAt)}</small></div>
        <button class="btn btn-success btn-sm" data-q="${q.id}">Open</button></div></div>`).join('')
      : '<p class="text-muted">No questions here.</p>');
  $('qStatus').value = status;
  $('qStatus').onchange = e => run(expertQueue(e.target.value));
  view.querySelectorAll('[data-q]').forEach(b => b.onclick = () => run(qThread(b.dataset.q)));
}

/* ---------- Admin: create expert accounts ---------- */
async function adminExperts() {
  view.innerHTML = `<h5 class="mb-3">Create an expert account</h5>
    <div class="card"><div class="card-body"><form id="xf" class="row g-2">
      <div class="col-12"><label class="form-label small mb-0">Full name</label><input class="form-control" name="fullName" required></div>
      ${input('Email', 'email', 'type="email"')}
      ${input('Phone', 'phone', 'placeholder="0771234567"')}
      <div class="col-12"><label class="form-label small mb-0">Temporary password (at least 8 characters)</label>
        <input class="form-control" name="password" minlength="8" required autocomplete="off"></div>
      <div class="col-12"><button class="btn btn-success w-100">Create expert account</button></div>
    </form></div></div>
    <div id="xout" class="mt-2"></div>
    <p class="small text-muted mt-2">Experts sign in on the normal login page. Give them the temporary password privately.
      A change-password screen is not built yet.</p>`;
  $('xf').onsubmit = async e => {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.target));
    try {
      const r = await API.post('/api/admin/experts', { fullName: f.fullName, email: f.email || null,
        phone: f.phone || null, password: f.password });
      $('xout').innerHTML = alertBox('Created expert account for ' + r.fullName + ' (id ' + r.id + ')', 'success');
      e.target.reset();
    } catch (err) { $('xout').innerHTML = alertBox(err.message); }
  };
}
