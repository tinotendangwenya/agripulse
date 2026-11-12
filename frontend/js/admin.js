/* Admin: expert applications and user management. Helpers ($, view, esc, alertBox, run, me, API) come from app.js. */
const A_COLOR = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' };
const aBadge = s => `<span class="badge text-bg-${A_COLOR[s] || 'secondary'}">${esc(s)}</span>`;
const aWhen = s => esc(String(s || '').replace('T', ' ').slice(0, 16));
const aContact = x => [x.email, x.phone].filter(Boolean).map(esc).join(' &middot; ') || 'no contact details';

async function adminApplications(status = 'PENDING') {
  const list = await API.get('/api/admin/applications?status=' + status);
  view.innerHTML = `<h5 class="mb-2">Expert applications</h5>
    <p class="small text-muted">AgriPulse cannot verify a qualification by itself. Check it with the issuing body or the organisation named before you approve.
      Approving lets the person log in as an expert and see farmers' questions and photos.</p>
    <select class="form-select mb-3" id="as"><option value="PENDING">Waiting for review</option><option value="APPROVED">Approved</option>
      <option value="REJECTED">Rejected</option><option value="ALL">All</option></select>` +
    (list.length ? list.map(a => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(a.fullName)}</strong>${aBadge(a.status)}</div>
      <div class="small">${aContact(a)}</div>
      <div class="small mt-1"><strong>Expertise:</strong> ${esc(a.speciality)} &middot; ${esc(a.province)}</div>
      <div class="small"><strong>Qualification:</strong> ${esc(a.qualification)}</div>
      ${a.organisation ? `<div class="small"><strong>Organisation:</strong> ${esc(a.organisation)}</div>` : ''}
      ${a.registrationNumber ? `<div class="small"><strong>Registration number:</strong> ${esc(a.registrationNumber)}</div>` : ''}
      ${a.statement ? `<div class="small mt-1">${esc(a.statement)}</div>` : ''}
      <div class="small text-muted">Applied ${aWhen(a.createdAt)}${a.reviewNote ? ' &middot; note: ' + esc(a.reviewNote) : ''}</div>
      ${a.status === 'PENDING' ? `<div class="mt-2"><button class="btn btn-success btn-sm me-1" data-ok="${a.id}">Approve</button>
        <button class="btn btn-outline-danger btn-sm" data-no="${a.id}">Reject</button></div>` : ''}
    </div></div>`).join('') : '<p class="text-muted">Nothing here.</p>');
  $('as').value = status;
  $('as').onchange = e => run(adminApplications(e.target.value));
  view.querySelectorAll('[data-ok]').forEach(b => b.onclick = async () => {
    if (!confirm('Approve this expert? They will be able to see farmers\' questions and photos.')) return;
    try { await API.post(`/api/admin/applications/${b.dataset.ok}/approve`); run(adminApplications(status)); } catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-no]').forEach(b => b.onclick = async () => {
    const note = prompt('Reason (the applicant will see this when they try to log in):');
    if (note === null) return;
    try { await API.post(`/api/admin/applications/${b.dataset.no}/reject`, { note }); run(adminApplications(status)); } catch (e) { alert(e.message); }
  });
}

async function adminUsers(q = '') {
  const list = await API.get('/api/admin/users?q=' + encodeURIComponent(q));
  view.innerHTML = `<h5 class="mb-2">Users</h5>
    <form id="uf" class="input-group mb-3"><input class="form-control" name="q" placeholder="Search name, email or phone" value="${esc(q)}">
      <button class="btn btn-success">Search</button></form>` +
    (list.length ? list.map(u => `<div class="card mb-2"><div class="card-body py-2">
      <div class="d-flex justify-content-between"><strong>${esc(u.fullName)}</strong>
        <span><span class="badge text-bg-light">${esc(u.role)}</span>
        <span class="badge text-bg-${u.enabled ? 'success' : 'secondary'}">${u.enabled ? 'ACTIVE' : 'DISABLED'}</span></span></div>
      <div class="small">${aContact(u)}</div>
      <div class="small text-muted">${u.consentAt ? 'Accepted the privacy notice ' + aWhen(u.consentAt) : 'No privacy consent recorded'}</div>
      ${u.id === me.id ? '<div class="small text-muted mt-1">This is you.</div>' : `<div class="mt-2">
        <button class="btn btn-outline-secondary btn-sm me-1" data-reset="${u.id}">Reset password</button>
        <button class="btn btn-outline-${u.enabled ? 'danger' : 'success'} btn-sm" data-toggle="${u.id}" data-on="${u.enabled}">${u.enabled ? 'Disable account' : 'Enable account'}</button></div>`}
    </div></div>`).join('') : '<p class="text-muted">No users found.</p>') +
    '<p class="small text-muted">Resetting a password logs the person out everywhere. Tell them the temporary password privately and ask them to change it under Account. Every action here is recorded in the audit log.</p>';
  $('uf').onsubmit = e => { e.preventDefault(); run(adminUsers(e.target.elements['q'].value.trim())); };
  view.querySelectorAll('[data-reset]').forEach(b => b.onclick = async () => {
    const pw = prompt('Temporary password for this person (at least 8 characters):');
    if (pw === null) return;
    if (pw.length < 8) return alert('Use at least 8 characters.');
    try { await API.post(`/api/admin/users/${b.dataset.reset}/reset-password`, { temporaryPassword: pw }); alert('Password reset. Give the person the temporary password privately.'); }
    catch (e) { alert(e.message); }
  });
  view.querySelectorAll('[data-toggle]').forEach(b => b.onclick = async () => {
    const turnOn = b.dataset.on !== 'true';
    if (!confirm(turnOn ? 'Enable this account?' : 'Disable this account? They are logged out straight away.')) return;
    try { await API.post(`/api/admin/users/${b.dataset.toggle}/enabled`, { enabled: turnOn }); run(adminUsers(q)); } catch (e) { alert(e.message); }
  });
}
