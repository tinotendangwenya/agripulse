/* Alerts: a button with an unread count, and a list screen. Checks once a minute while the tab is visible. */
async function notificationsScreen() {
  const d = await API.get('/api/notifications');
  setBell(d.unread);
  view.innerHTML = `<div class="d-flex justify-content-between align-items-center mb-3"><h5 class="mb-0">Alerts</h5>
    ${d.unread ? '<button class="btn btn-outline-secondary btn-sm" id="readAll">Mark all as read</button>' : ''}</div>` +
    (d.items.length ? d.items.map(n => `<div class="card mb-2 ${n.read ? '' : 'border border-success'}"><div class="card-body py-2 d-flex justify-content-between align-items-center">
      <div><strong>${esc(n.title)}</strong>${n.read ? '' : ' <span class="badge text-bg-success">New</span>'}<br>
        <small>${esc(n.body || '')}</small><br><small class="text-muted">${qWhen(n.createdAt)}</small></div>
      ${n.link ? `<button class="btn btn-outline-success btn-sm ms-2" data-n="${n.id}" data-link="${esc(n.link)}">Open</button>` : ''}
    </div></div>`).join('') : '<p class="text-muted">No alerts yet. New offers, orders and answers appear here.</p>');
  view.querySelectorAll('[data-n]').forEach(b => b.onclick = async () => {
    try { await API.post(`/api/notifications/${b.dataset.n}/read`); } catch (e) { /* still go to the page */ }
    const target = '#' + b.dataset.link;
    if (location.hash === target) nav(); else location.hash = b.dataset.link;
  });
  if ($('readAll')) $('readAll').onclick = async () => {
    try { await API.post('/api/notifications/read-all'); run(notificationsScreen()); } catch (e) { alert(e.message); }
  };
}

function setBell(n) {
  const b = document.getElementById('bellN');
  if (!b) return;
  b.textContent = n > 99 ? '99+' : n;
  b.classList.toggle('d-none', !n);
}

(function () {
  const who = document.getElementById('who');
  if (!who || !API.token()) return;
  const btn = document.createElement('button');
  btn.id = 'bell';
  btn.className = 'btn btn-sm btn-outline-secondary me-1';
  btn.innerHTML = 'Alerts <span id="bellN" class="badge text-bg-danger d-none"></span>';
  btn.onclick = () => run(notificationsScreen());
  who.parentElement.insertBefore(btn, who.parentElement.querySelector('button'));

  async function refresh() {
    if (document.visibilityState !== 'visible') return;
    try { setBell((await API.get('/api/notifications/unread-count')).unread); } catch (e) { /* try again next time */ }
  }
  setInterval(refresh, 60000);
  document.addEventListener('visibilitychange', refresh);
  refresh();
})();
