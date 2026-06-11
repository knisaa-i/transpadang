// =====================================================================
// FE statis SPM Trans Padang — terhubung ke BE Spring Boot di :8080
// =====================================================================
const API = 'http://localhost:8080';

let token = localStorage.getItem('token') || null;
let user = JSON.parse(localStorage.getItem('user') || 'null');
let activeTab = 'input';

// ---- helper API ----
async function api(path, { method = 'GET', body = null } = {}) {
  const headers = {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (body) headers['Content-Type'] = 'application/json';
  let res;
  try {
    res = await fetch(API + path, { method, headers, body: body ? JSON.stringify(body) : null });
  } catch (e) {
    throw new Error('Tidak bisa terhubung ke server (' + API + '). Pastikan BE jalan.');
  }
  let json = null;
  try { json = await res.json(); } catch (e) {}
  if (res.status === 401) { logout(); throw new Error('Sesi berakhir, silakan login lagi.'); }
  if (!res.ok || (json && json.success === false)) {
    throw new Error((json && json.message) ? json.message : 'HTTP ' + res.status);
  }
  return json ? json.data : null;
}

function toast(msg, type = '') {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast ' + type;
  setTimeout(() => t.classList.add('hidden'), 3500);
}

function logout() {
  token = null; user = null;
  localStorage.removeItem('token'); localStorage.removeItem('user');
  render();
}

const HARI = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
const fmt = n => (n == null ? '-' : Number(n).toFixed(2));

// =====================================================================
// LOGIN VIEW
// =====================================================================
function renderLogin() {
  document.getElementById('topbar').classList.add('hidden');
  document.getElementById('app').innerHTML = `
    <div class="login-wrap card">
      <h1>Masuk</h1>
      <p class="sub">Sistem Penilaian SPM Trans Padang</p>
      <label>Username</label>
      <input id="u" value="staf01" autocomplete="username" />
      <label>Password</label>
      <input id="p" type="password" value="trans12345" autocomplete="current-password" />
      <button class="btn" id="loginBtn" style="margin-top:18px;width:100%">Masuk</button>
      <p class="sub" style="margin-top:14px">Contoh: staf01 / kadiv01 / manager01 / admin — password <b>trans12345</b></p>
    </div>`;
  const doLogin = async () => {
    const btn = document.getElementById('loginBtn');
    btn.disabled = true;
    try {
      const data = await api('/api/auth/login', {
        method: 'POST',
        body: { username: document.getElementById('u').value.trim(), password: document.getElementById('p').value }
      });
      token = data.accessToken; user = data.user;
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(user));
      toast('Selamat datang, ' + user.nama, 'ok');
      activeTab = (user.role === 'MAKER' || user.role === 'ADMIN') ? 'input' : 'daftar';
      render();
    } catch (e) { toast(e.message, 'err'); btn.disabled = false; }
  };
  document.getElementById('loginBtn').onclick = doLogin;
  document.getElementById('p').addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });
}

// =====================================================================
// SHELL (topbar + tabs)
// =====================================================================
function renderShell() {
  document.getElementById('topbar').classList.remove('hidden');
  document.getElementById('userInfo').textContent = `${user.nama} · ${user.role}`;
  document.getElementById('logoutBtn').onclick = logout;

  const canInput = user.role === 'MAKER' || user.role === 'ADMIN';
  const tabs = [];
  if (canInput) tabs.push(['input', 'Input Penilaian']);
  tabs.push(['daftar', 'Daftar Penilaian']);
  if (!canInput && activeTab === 'input') activeTab = 'daftar';

  document.getElementById('tabs').innerHTML = tabs.map(([id, label]) =>
    `<button class="tab ${activeTab === id ? 'active' : ''}" data-tab="${id}">${label}</button>`).join('');
  document.querySelectorAll('.tab').forEach(b => b.onclick = () => { activeTab = b.dataset.tab; render(); });
}

// =====================================================================
// INPUT PENILAIAN
// =====================================================================
let currentPenilaianId = null;
let currentBusId = null;
let currentBusLabel = '';

async function renderInput() {
  document.getElementById('app').innerHTML = `<div class="card" id="setupCard"></div><div id="indikatorArea"></div>`;
  await renderSetupCard();
  if (currentPenilaianId && currentBusId) await loadIndikator();
}

async function renderSetupCard() {
  const card = document.getElementById('setupCard');
  if (currentPenilaianId && currentBusId) {
    card.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap">
        <div>
          <h1 style="margin:0">Penilaian #${currentPenilaianId}</h1>
          <p class="sub" style="margin:4px 0 0">Bus ${currentBusLabel} — isi & simpan tiap item (bisa diedit kapan saja).</p>
        </div>
        <button class="btn-ghost" onclick="resetPenilaian()">+ Penilaian baru</button>
      </div>`;
    return;
  }
  card.innerHTML = `
    <h1>Input Penilaian SPM</h1>
    <p class="sub">Pilih koridor & bus → Mulai penilaian → isi & simpan nilai tiap indikator satu per satu.</p>
    <div class="row">
      <div><label>Koridor</label><select id="koridor"></select></div>
      <div><label>Bus (No. Lambung)</label><select id="bus"></select></div>
      <div><label>Tanggal</label><input type="date" id="tanggal" /></div>
      <div><label>Hari</label><input id="hari" readonly /></div>
    </div>
    <button class="btn" id="startBtn" style="margin-top:16px">Mulai penilaian</button>`;

  const today = new Date().toISOString().slice(0, 10);
  const tgl = document.getElementById('tanggal');
  tgl.value = today;
  const setHari = () => { const d = new Date(tgl.value); document.getElementById('hari').value = isNaN(d) ? '' : HARI[d.getDay()]; };
  setHari();
  tgl.onchange = setHari;

  try {
    const koridors = await api('/api/koridor');
    document.getElementById('koridor').innerHTML = koridors.map(k => `<option value="${k.id}">${k.nama}</option>`).join('');
    await loadBus();
    document.getElementById('koridor').onchange = loadBus;
  } catch (e) { toast(e.message, 'err'); }

  document.getElementById('startBtn').onclick = startPenilaian;
}

window.resetPenilaian = () => {
  currentPenilaianId = null; currentBusId = null; currentBusLabel = ''; indikatorCache = [];
  renderInput();
};

async function startPenilaian() {
  const busSel = document.getElementById('bus');
  const busId = busSel.value;
  if (!busId) { toast('Pilih bus dulu', 'err'); return; }
  const body = {
    koridorId: Number(document.getElementById('koridor').value),
    tanggal: document.getElementById('tanggal').value,
    hari: document.getElementById('hari').value,
    details: []
  };
  try {
    const res = await api('/api/penilaian', { method: 'POST', body });
    currentPenilaianId = res.id;
    currentBusId = Number(busId);
    currentBusLabel = busSel.options[busSel.selectedIndex].text;
    toast('Penilaian #' + currentPenilaianId + ' dibuat — mulai isi tiap item', 'ok');
    renderInput();
  } catch (e) { toast(e.message, 'err'); }
}

async function loadBus() {
  const koridorId = document.getElementById('koridor').value;
  const bus = await api('/api/bus?koridorId=' + koridorId);
  const sel = document.getElementById('bus');
  sel.innerHTML = bus.length
    ? bus.map(b => `<option value="${b.id}">${b.noLambung} — ${b.platNomor || ''}</option>`).join('')
    : '<option value="">(belum ada bus)</option>';
}

let indikatorCache = [];
let stepIndex = 0;
let stepValues = {}; // indikatorId -> { nilai, catatan }

async function loadIndikator() {
  const area = document.getElementById('indikatorArea');
  area.innerHTML = '<div class="card empty">Memuat indikator…</div>';
  try {
    const page = await api('/api/indikator-spm?page=0&size=200');
    indikatorCache = page.content || [];
    if (!indikatorCache.length) { area.innerHTML = '<div class="card empty">Belum ada indikator. Jalankan seed dulu.</div>'; return; }
    // ambil detail yang sudah tersimpan untuk bus ini (agar bisa diedit)
    const existing = {};
    try {
      const pen = await api('/api/penilaian/' + currentPenilaianId);
      (pen.details || []).forEach(d => { if (d.busId === currentBusId) existing[d.indikatorId] = d; });
    } catch (e) { /* abaikan, anggap belum ada */ }
    stepIndex = 0;
    stepValues = {};
    indikatorCache.forEach(i => {
      const ex = existing[i.id];
      stepValues[i.id] = ex
        ? { nilai: ex.nilaiCapaian, catatan: ex.catatan || '', saved: true }
        : { nilai: 100, catatan: '', saved: false };
    });
    renderStep();
  } catch (e) { area.innerHTML = `<div class="card empty">${e.message}</div>`; }
}

function totalSkor() {
  let t = 0;
  indikatorCache.forEach(i => { t += (parseFloat(stepValues[i.id].nilai) || 0) * (i.bobot || 0); });
  return t;
}

function saveCurrentStep() {
  const i = indikatorCache[stepIndex];
  if (!i) return;
  const n = document.getElementById('stepNilai');
  const c = document.getElementById('stepCatatan');
  if (n) stepValues[i.id].nilai = n.value;
  if (c) stepValues[i.id].catatan = c.value;
}

function renderStep() {
  const area = document.getElementById('indikatorArea');
  const total = indikatorCache.length;
  const i = indikatorCache[stepIndex];
  const v = stepValues[i.id];
  const parts = (i.uraian || '').split('\n');
  const judul = parts.shift();
  const desk = parts.join('\n');
  const isLast = stepIndex === total - 1;
  const pct = Math.round(((stepIndex + 1) / total) * 100);
  const options = indikatorCache.map((x, idx) =>
    `<option value="${idx}" ${idx === stepIndex ? 'selected' : ''}>${idx + 1}. ${(x.uraian || '').split('\n')[0]}</option>`).join('');

  area.innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;margin-bottom:8px">
        <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
          <span style="background:#eef4fb;color:var(--primary-dark);font-size:12px;font-weight:600;padding:3px 10px;border-radius:8px">${i.aspekNama || '-'}</span>
          <span style="background:var(--bg);color:var(--muted);font-size:12px;padding:3px 10px;border-radius:8px">${i.subKategoriNama || 'umum'}</span>
        </div>
        <span class="muted" style="font-size:13px">Item ${stepIndex + 1} dari ${total}${v.saved ? ' · <span style="color:var(--ok)">✓ tersimpan</span>' : ''}</span>
      </div>
      <div style="height:6px;background:var(--bg);border-radius:4px;overflow:hidden;margin-bottom:16px">
        <div style="height:100%;width:${pct}%;background:var(--primary)"></div>
      </div>

      <div class="judul" style="font-size:16px;font-weight:600">${i.nomorUrut || ''}. ${judul}</div>
      ${desk ? `<div class="desk" style="white-space:pre-line;color:var(--muted);font-size:14px;margin:6px 0 12px">${desk}</div>` : ''}
      <div class="standar">
        <div><b>Yang dinilai:</b> ${i.spmIndikator || '-'}</div>
        <div><b>Standar:</b> ${i.spmNilai || '-'} &nbsp;·&nbsp; <span class="muted">bobot ${fmt(i.bobot)}</span></div>
      </div>

      <div class="nilai-row" style="margin-top:14px">
        <label style="margin:0">Nilai (0–100)</label>
        <input type="number" min="0" max="100" step="5" id="stepNilai" value="${v.nilai}" style="width:110px" />
        <input id="stepCatatan" class="catatan" placeholder="Catatan (opsional)" value="${(v.catatan || '').replace(/"/g, '&quot;')}" />
        <span class="skor-chip" id="stepSkor">${fmt((parseFloat(v.nilai) || 0) * i.bobot)}</span>
      </div>

      <div style="margin-top:16px;display:flex;justify-content:space-between;gap:10px;align-items:center;flex-wrap:wrap">
        <button class="btn-ghost" onclick="stepPrev()" ${stepIndex === 0 ? 'disabled' : ''}>← Sebelumnya</button>
        <select id="stepJump" style="max-width:300px">${options}</select>
        <span style="display:flex;gap:8px;align-items:center">
          <button class="btn" onclick="saveItem()">${v.saved ? 'Perbarui item' : 'Simpan item'}</button>
          ${isLast
            ? `<button class="btn-ghost" onclick="finishInput()">Selesai →</button>`
            : `<button class="btn-ghost" onclick="stepNext()">Berikutnya →</button>`}
        </span>
      </div>
    </div>

    <div class="totalbar">
      <div><span class="muted">Total capaian sementara (Σ skor terbobot)</span><div class="total-num" id="totalNum">${fmt(totalSkor()).replace('.', ',')}</div></div>
      <span class="muted" style="font-size:13px">${Object.values(stepValues).filter(x => x.saved).length}/${total} item tersimpan</span>
    </div>`;

  const n = document.getElementById('stepNilai');
  n.addEventListener('input', () => {
    const val = parseFloat(n.value) || 0;
    stepValues[i.id].nilai = n.value;
    document.getElementById('stepSkor').textContent = fmt(val * i.bobot);
    document.getElementById('totalNum').textContent = fmt(totalSkor()).replace('.', ',');
  });
  document.getElementById('stepCatatan').addEventListener('input', e => { stepValues[i.id].catatan = e.target.value; });
  document.getElementById('stepJump').addEventListener('change', e => { saveCurrentStep(); stepIndex = Number(e.target.value); renderStep(); });
}

window.stepPrev = () => { saveCurrentStep(); if (stepIndex > 0) { stepIndex--; renderStep(); } };
window.stepNext = () => { saveCurrentStep(); if (stepIndex < indikatorCache.length - 1) { stepIndex++; renderStep(); } };

window.saveItem = async () => {
  saveCurrentStep();
  const i = indikatorCache[stepIndex];
  try {
    await api('/api/penilaian/' + currentPenilaianId + '/detail', {
      method: 'POST',
      body: {
        busId: currentBusId,
        indikatorId: Number(i.id),
        nilaiCapaian: parseFloat(stepValues[i.id].nilai) || 0,
        catatan: stepValues[i.id].catatan || null
      }
    });
    stepValues[i.id].saved = true;
    toast('Item ' + (stepIndex + 1) + ' tersimpan', 'ok');
    renderStep();
  } catch (e) { toast(e.message, 'err'); }
};

window.finishInput = () => { activeTab = 'daftar'; render(); };

// =====================================================================
// DAFTAR PENILAIAN + alur status
// =====================================================================
const NEXT = {
  DRAFT: [['SUBMITTED', 'Submit', ['MAKER', 'ADMIN']]],
  SUBMITTED: [['CHECKED', 'Periksa', ['CHECKER', 'ADMIN']], ['REJECTED', 'Tolak', ['CHECKER', 'ADMIN']]],
  CHECKED: [['APPROVED', 'Setujui', ['APPROVER', 'ADMIN']], ['REJECTED', 'Tolak', ['APPROVER', 'ADMIN']]],
  APPROVED: [],
  REJECTED: [['SUBMITTED', 'Ajukan ulang', ['MAKER', 'ADMIN']]]
};

async function renderDaftar() {
  const app = document.getElementById('app');
  app.innerHTML = '<div class="card"><div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap"><div><h1>Daftar Penilaian</h1><p class="sub" style="margin:0">Alur: DRAFT → SUBMITTED → CHECKED → APPROVED</p></div><button class="btn" onclick="exportAllExcel()">Unduh semua (Excel)</button></div><div id="list" class="empty" style="margin-top:14px">Memuat…</div></div><div id="detail"></div>';
  try {
    const page = await api('/api/penilaian?page=0&size=50');
    const rows = page.content || [];
    if (!rows.length) { document.getElementById('list').innerHTML = '<div class="empty">Belum ada penilaian.</div>'; return; }
    let html = `<table><thead><tr>
      <th>ID</th><th>Koridor</th><th>Tanggal</th><th>Status</th>
      <th>Maker</th><th>Checker</th><th>Approver</th><th>Total</th><th>Aksi</th></tr></thead><tbody>`;
    rows.forEach(p => {
      const acts = (NEXT[p.status] || []).filter(a => a[2].includes(user.role))
        .map(a => `<button class="btn-ghost btn-sm" onclick="changeStatus(${p.id},'${a[0]}')">${a[1]}</button>`).join(' ');
      const canEdit = (user.role === 'MAKER' || user.role === 'ADMIN')
        && (p.status === 'DRAFT' || p.status === 'REJECTED') && p.details && p.details.length;
      const editBtn = canEdit
        ? `<button class="btn-ghost btn-sm" onclick="editPenilaian(${p.id}, ${p.details[0].busId}, '${(p.details[0].noLambung || '').replace(/'/g, '')}')">Edit item</button>`
        : '';
      html += `<tr>
        <td>${p.id}</td><td>${p.koridorNama || '-'}</td><td>${p.tanggal || '-'}</td>
        <td><span class="badge b-${p.status}">${p.status}</span></td>
        <td>${p.makerNama || '-'}</td><td>${p.checkerNama || '-'}</td><td>${p.approverNama || '-'}</td>
        <td>${fmt(p.totalCapaian)}</td>
        <td><button class="btn-ghost btn-sm" onclick="viewDetail(${p.id})">Detail</button>
            <button class="btn-ghost btn-sm" onclick="exportExcel(${p.id})">Excel</button> ${editBtn} ${acts}</td>
      </tr>`;
    });
    html += '</tbody></table>';
    document.getElementById('list').innerHTML = html;
  } catch (e) { document.getElementById('list').innerHTML = `<div class="empty">${e.message}</div>`; }
}

window.editPenilaian = (id, busId, label) => {
  currentPenilaianId = id;
  currentBusId = busId;
  currentBusLabel = label;
  activeTab = 'input';
  render();
};

window.changeStatus = async (id, status) => {
  try {
    await api(`/api/penilaian/${id}/status?status=${status}`, { method: 'PATCH' });
    toast('Status → ' + status, 'ok');
    render();
  } catch (e) { toast(e.message, 'err'); }
};

window.exportAllExcel = async () => {
  try {
    const res = await fetch(API + '/api/penilaian/export-all', { headers: { Authorization: 'Bearer ' + token } });
    if (!res.ok) throw new Error('Gagal export (HTTP ' + res.status + ')');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'rekap-penilaian-spm.xlsx';
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
    toast('Rekap semua penilaian diunduh', 'ok');
  } catch (e) { toast(e.message, 'err'); }
};

window.exportExcel = async (id) => {
  try {
    const res = await fetch(API + '/api/penilaian/' + id + '/export', { headers: { Authorization: 'Bearer ' + token } });
    if (!res.ok) throw new Error('Gagal export (HTTP ' + res.status + ')');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'penilaian-spm-' + id + '.xlsx';
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
    toast('Excel diunduh', 'ok');
  } catch (e) { toast(e.message, 'err'); }
};

window.viewDetail = async (id) => {
  const box = document.getElementById('detail');
  box.innerHTML = '<div class="card empty">Memuat detail…</div>';
  try {
    const p = await api('/api/penilaian/' + id);
    let html = `<div class="card"><h2>Detail penilaian #${p.id}</h2>
      <p class="sub">${p.koridorNama} · ${p.tanggal} · status <b>${p.status}</b> · total <b>${fmt(p.totalCapaian)}</b></p>
      <table><thead><tr><th>Bus</th><th>Indikator</th><th>Nilai</th><th>Bobot</th><th>Skor</th><th>Catatan</th></tr></thead><tbody>`;
    (p.details || []).forEach(d => {
      html += `<tr><td>${d.noLambung}</td><td>${(d.indikatorUraian || '').split('\n')[0]}</td>
        <td>${fmt(d.nilaiCapaian)}</td><td>${fmt(d.bobot)}</td><td>${fmt(d.skorTerbobot)}</td><td>${d.catatan || '-'}</td></tr>`;
    });
    html += '</tbody></table></div>';
    box.innerHTML = html;
    box.scrollIntoView({ behavior: 'smooth' });
  } catch (e) { box.innerHTML = `<div class="card empty">${e.message}</div>`; }
};

// =====================================================================
// ROUTER
// =====================================================================
function render() {
  if (!token || !user) { renderLogin(); return; }
  renderShell();
  if (activeTab === 'input') renderInput();
  else renderDaftar();
}

render();
