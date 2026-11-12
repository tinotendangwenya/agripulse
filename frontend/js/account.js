/* Account: change password, deactivate. Helpers ($, view, esc, alertBox, run, me, API) come from app.js. */
function accountScreen() {
  view.innerHTML = `<h5 class="mb-1">My account</h5>
    <p class="text-muted small mb-3">${esc(me.name)} &middot; ${esc(me.role)}</p>
    <div class="card mb-3"><div class="card-body"><h6>Change password</h6>
      <form id="cpf" class="row g-2">
        <div class="col-12"><label class="form-label small mb-0">Current password</label>
          <input class="form-control" type="password" name="currentPassword" required autocomplete="current-password"></div>
        <div class="col-12"><label class="form-label small mb-0">New password (at least 8 characters)</label>
          <input class="form-control" type="password" name="newPassword" required minlength="8" maxlength="72" autocomplete="new-password"></div>
        <div class="col-12"><label class="form-label small mb-0">Repeat the new password</label>
          <input class="form-control" type="password" name="repeat" required minlength="8" maxlength="72" autocomplete="new-password"></div>
        <div class="col-12"><button class="btn btn-success w-100">Change password</button></div>
      </form><div id="cpOut" class="mt-2"></div>
      <p class="small text-muted mb-0 mt-2">Changing your password logs you out on every other device.</p></div></div>
    ${me.role === 'ADMIN' ? '' : `<div class="card mb-3"><div class="card-body"><h6>Deactivate my account</h6>
      <p class="small text-muted">You will not be able to log in any more. Your records are kept, and are only erased if you ask us (see the privacy notice).</p>
      <form id="dzf" class="row g-2">
        <div class="col-12"><input class="form-control" type="password" name="password" required placeholder="Your password" autocomplete="current-password"></div>
        <div class="col-12"><button class="btn btn-outline-danger w-100">Deactivate my account</button></div>
      </form><div id="dzOut" class="mt-2"></div></div></div>`}
    <p class="small"><a href="/privacy.html" target="_blank" rel="noopener">Read the privacy notice</a></p>`;

  $('cpf').onsubmit = async e => {
    e.preventDefault();
    const f = e.target.elements, out = $('cpOut');
    if (f['newPassword'].value !== f['repeat'].value) { out.innerHTML = alertBox('The new passwords do not match.'); return; }
    try {
      const r = await API.post('/api/account/change-password', { currentPassword: f['currentPassword'].value, newPassword: f['newPassword'].value });
      sessionStorage.setItem('token', r.token);          // this device stays logged in with a fresh token
      e.target.reset();
      out.innerHTML = alertBox('Password changed. Your other devices were logged out.', 'success');
    } catch (err) { out.innerHTML = alertBox(err.message); }
  };
  if ($('dzf')) $('dzf').onsubmit = async e => {
    e.preventDefault();
    if (!confirm('Deactivate your account? You will be logged out and cannot log in again.')) return;
    try { await API.post('/api/account/deactivate', { password: e.target.elements['password'].value }); API.logout(); }
    catch (err) { $('dzOut').innerHTML = alertBox(err.message); }
  };
}

(function () {
  const who = document.getElementById('who');
  if (!who || !API.token()) return;
  const btn = document.createElement('button');
  btn.className = 'btn btn-sm btn-outline-secondary me-1';
  btn.textContent = 'Account';
  btn.onclick = () => run(accountScreen());
  who.parentElement.insertBefore(btn, who.parentElement.querySelector('button'));
})();
