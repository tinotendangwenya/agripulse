/* Small helper around fetch(). The token lives in sessionStorage (cleared when the tab closes). */
const API = {
  token: () => sessionStorage.getItem('token'),
  user: () => JSON.parse(sessionStorage.getItem('user') || 'null'),
  async call(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const t = API.token();
    if (t) headers.Authorization = 'Bearer ' + t;
    const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
    if (res.status === 401 && t) { API.logout(); throw new Error('Session expired, please log in again'); }
    const text = await res.text();
    let data = null;
    if (text) { try { data = JSON.parse(text); } catch (e) { data = { message: text }; } }
    if (!res.ok) throw new Error((data && (data.message || data.error)) || 'Error ' + res.status);
    return data;
  },
  get: (p) => API.call('GET', p),
  post: (p, b) => API.call('POST', p, b || {}),
  put: (p, b) => API.call('PUT', p, b),
  del: (p) => API.call('DELETE', p),
  logout() { sessionStorage.clear(); location.href = '/index.html'; }
};

/* Escape anything that came from the server before putting it in HTML */
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
