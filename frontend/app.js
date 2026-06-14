// =====================================================================
// FE statis SPM Trans Padang — terhubung ke BE Spring Boot di :8080
// Alur: login > pilih koridor (DRAFT) > kategori Bus/Halte > pilih unit
//       > isi pertanyaan per aspek > simpan draft > submit (bila lengkap)
// =====================================================================
const API = 'http://localhost:8080';

let token = localStorage.getItem('token') || null;
let user = JSON.parse(localStorage.getItem('user') || 'null');
let activeTab = 'input';
let activeModule = 'dashboard';   // 'dashboard' | 'spm' | 'checklist'

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
const esc = s => (s == null ? '' : String(s).replace(/"/g, '&quot;'));

// =====================================================================
// LOGIN
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
      activeModule = 'dashboard';
      activeTab = (user.role === 'MAKER' || user.role === 'ADMIN') ? 'input' : 'daftar';
      render();
    } catch (e) { toast(e.message, 'err'); btn.disabled = false; }
  };
  document.getElementById('loginBtn').onclick = doLogin;
  document.getElementById('p').addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });
}

// =====================================================================
// SHELL
// =====================================================================
function renderShell() {
  document.getElementById('topbar').classList.remove('hidden');
  document.getElementById('userInfo').textContent = `${user.nama} · ${user.role}`;
  document.getElementById('logoutBtn').onclick = logout;

  const brand = document.querySelector('#topbar .brand');
  if (brand) { brand.style.cursor = 'pointer'; brand.title = 'Ke Dashboard'; brand.onclick = goDashboard; }

  const nav = document.getElementById('tabs');
  if (activeModule === 'dashboard') { nav.innerHTML = ''; return; }

  let tabs = [];
  let cur = activeTab;
  if (activeModule === 'spm') {
    const canInput = user.role === 'MAKER' || user.role === 'ADMIN';
    if (canInput) tabs.push(['input', 'Input Penilaian']);
    tabs.push(['daftar', 'Daftar Penilaian']);
    if (!canInput && activeTab === 'input') activeTab = 'daftar';
    cur = activeTab;
  } else if (activeModule === 'checklist') {
    tabs = [['isi', 'Isi Checklist'], ['daftar-cl', 'Daftar Checklist']];
    cur = clActiveTab;
  }

  nav.innerHTML = `<button class="tab" data-tab="__home">← Dashboard</button>` +
    tabs.map(([id, label]) => `<button class="tab ${cur === id ? 'active' : ''}" data-tab="${id}">${label}</button>`).join('');
  nav.querySelectorAll('.tab').forEach(b => b.onclick = () => {
    const t = b.dataset.tab;
    if (t === '__home') { goDashboard(); return; }
    if (activeModule === 'spm') activeTab = t; else clActiveTab = t;
    render();
  });
}

// =====================================================================
// INPUT PENILAIAN
// =====================================================================
let pn = null;          // penilaian aktif (PenilaianSpmView)
let activeCat = null;   // 'BUS' | 'HALTE'
let units = [];         // daftar unit (bus/halte) utk kategori aktif
let curUnitId = null;
let qIndikators = [];   // indikator utk kategori aktif
let unitFormOpen = false; // true = sedang isi/edit satu unit; false = papan daftar unit
let qIndex = 0;           // indikator yang sedang ditampilkan (wizard satu-per-satu)

async function renderInput() {
  if (!pn) { await renderSetup(); return; }
  await renderWorkspace();
}

async function renderSetup() {
  document.getElementById('app').innerHTML = `
    <div class="card">
      <h1>Input Penilaian SPM</h1>
      <p class="sub">Pilih koridor lalu mulai — penilaian dibuat sebagai DRAFT (per koridor).</p>
      <div class="row">
        <div><label>Koridor</label><select id="koridor"></select></div>
        <div><label>Tanggal</label><input type="date" id="tanggal" /></div>
        <div><label>Hari</label><input id="hari" readonly /></div>
      </div>
      <button class="btn" id="startBtn" style="margin-top:16px">Mulai penilaian</button>
    </div>`;
  const today = new Date().toISOString().slice(0, 10);
  const tgl = document.getElementById('tanggal');
  tgl.value = today;
  const setHari = () => { const d = new Date(tgl.value); document.getElementById('hari').value = isNaN(d) ? '' : HARI[d.getDay()]; };
  setHari();
  tgl.onchange = setHari;
  try {
    const koridors = await api('/api/koridor/daftar');
    document.getElementById('koridor').innerHTML = koridors.map(k => `<option value="${k.id}">${k.nama}</option>`).join('');
  } catch (e) { toast(e.message, 'err'); }
  document.getElementById('startBtn').onclick = startPenilaian;
}

async function startPenilaian() {
  try {
    pn = await api('/api/penilaian/new-penilaian', {
      method: 'POST',
      body: {
        koridorId: Number(document.getElementById('koridor').value),
        tanggal: document.getElementById('tanggal').value,
        hari: document.getElementById('hari').value,
        details: []
      }
    });
    activeCat = null;
    toast('Penilaian #' + pn.id + ' dibuat (DRAFT)', 'ok');
    renderInput();
  } catch (e) { toast(e.message, 'err'); }
}

function gateReady(pen) {
  const ds = pen.details || [];
  return ds.some(d => d.bus) && ds.some(d => d.halte);
}

async function renderWorkspace() {
  const ready = gateReady(pn);
  document.getElementById('app').innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap">
        <div>
          <h1 style="margin:0">Penilaian #${pn.id}</h1>
          <p class="sub" style="margin:4px 0 0">${pn.koridor ? pn.koridor.nama : ''} · ${pn.tanggal || ''} · status <b>${pn.status}</b></p>
        </div>
        <div style="display:flex;gap:8px">
          <button class="btn-ghost" onclick="resetPenilaian()">+ Penilaian baru</button>
          <button class="btn" onclick="submitPenilaian()" ${ready ? '' : 'disabled'}>Submit ke atasan</button>
        </div>
      </div>
      <div style="margin-top:12px;display:flex;gap:8px">
        <button class="${activeCat === 'BUS' ? 'btn' : 'btn-ghost'}" onclick="pilihKategori('BUS')">Bus</button>
        <button class="${activeCat === 'HALTE' ? 'btn' : 'btn-ghost'}" onclick="pilihKategori('HALTE')">Halte</button>
      </div>
      <p class="sub" style="margin:10px 0 0">${ready
        ? 'Kedua kategori sudah terisi — siap submit.'
        : 'Isi kategori Bus dan Halte (minimal satu unit tersimpan di masing-masing) untuk mengaktifkan Submit.'}</p>
    </div>
    <div id="catArea"></div>`;
  if (activeCat) await renderCategory();
}

window.pilihKategori = async (cat) => {
  activeCat = cat;
  try {
    units = await api((cat === 'BUS' ? '/api/bus/daftar' : '/api/halte/daftar') + '?koridorId=' + pn.koridor.id);
  } catch (e) { toast(e.message, 'err'); units = []; }
  curUnitId = units.length ? units[0].id : null;
  try {
    const page = await api('/api/indikator-spm/indikator-filter?kategori=' + cat + '&size=200');
    qIndikators = page.content || [];
  } catch (e) { toast(e.message, 'err'); qIndikators = []; }
  unitFormOpen = false;
  renderWorkspace();
};

async function renderCategory() {
  const area = document.getElementById('catArea');
  if (!units.length) {
    area.innerHTML = '<div class="card empty">Belum ada ' + (activeCat === 'BUS' ? 'bus' : 'halte') + ' untuk koridor ini.</div>';
    return;
  }
  if (unitFormOpen && curUnitId) renderUnitForm();
  else renderUnitBoard();
}

function unitLabelOf(u) {
  return activeCat === 'BUS' ? ('Lambung ' + u.noLambung) : ('Halte ' + u.nomor);
}

function unitSavedCount(unitId) {
  let n = 0;
  (pn.details || []).forEach(d => {
    const uid = activeCat === 'BUS' ? (d.bus && d.bus.id) : (d.halte && d.halte.id);
    if (uid === unitId) n++;
  });
  return n;
}

function renderUnitBoard() {
  const area = document.getElementById('catArea');
  const total = qIndikators.length;
  let lengkap = 0;
  const rows = units.map(u => {
    const terisi = unitSavedCount(u.id);
    const done = total > 0 && terisi >= total;
    if (done) lengkap++;
    const badge = done
      ? '<span class="badge b-APPROVED">Lengkap</span>'
      : (terisi > 0 ? `<span class="badge b-SUBMITTED">${terisi}/${total}</span>` : '<span class="badge b-DRAFT">Belum diisi</span>');
    return `<tr>
      <td>${unitLabelOf(u)}</td>
      <td>${badge}</td>
      <td style="text-align:right"><button class="btn-ghost btn-sm" onclick="openUnit(${u.id})">${terisi > 0 ? 'Edit' : 'Isi'}</button></td>
    </tr>`;
  }).join('');
  area.innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
        <h2 style="margin:0">${activeCat === 'BUS' ? 'Daftar Bus' : 'Daftar Halte'}</h2>
        <span class="muted" style="font-size:13px">${lengkap}/${units.length} unit lengkap</span>
      </div>
      <table style="margin-top:10px"><tbody>${rows}</tbody></table>
    </div>`;
}

window.openUnit = (id) => { curUnitId = Number(id); unitFormOpen = true; qIndex = 0; renderCategory(); };
window.backToBoard = () => { unitFormOpen = false; renderCategory(); };

// Wizard: tampilkan indikator satu-per-satu. Tiap klik "Selanjutnya"/"Sebelumnya"
// nilai yang terisi langsung disimpan ke draft; yang dikosongkan dilewati (null).
function renderUnitForm() {
  const u = units.find(x => x.id === curUnitId);
  const area = document.getElementById('catArea');

  if (!qIndikators.length) {
    area.innerHTML = `
      <div class="card">
        <div class="empty">Belum ada indikator untuk kategori ini.</div>
        <div style="margin-top:12px"><button class="btn-ghost btn-sm" onclick="backToBoard()">← Kembali ke daftar</button></div>
      </div>`;
    return;
  }

  if (qIndex < 0) qIndex = 0;
  if (qIndex > qIndikators.length - 1) qIndex = qIndikators.length - 1;

  const total = qIndikators.length;
  const i = qIndikators[qIndex];

  const existing = {};
  (pn.details || []).forEach(d => {
    const uid = activeCat === 'BUS' ? (d.bus && d.bus.id) : (d.halte && d.halte.id);
    if (uid === curUnitId && d.indikator) existing[d.indikator.id] = d;
  });
  const ex = existing[i.id];
  const val = ex && ex.nilaiCapaian != null ? ex.nilaiCapaian : '';
  const cat = ex && ex.catatan ? ex.catatan : '';
  const terisi = Object.keys(existing).length;

  const aspek = i.aspek ? i.aspek.nama : '-';
  const sub = i.subKategori ? i.subKategori.nama : '(umum)';
  const parts = (i.uraian || '').split('\n');
  const judul = parts.shift();
  const desk = parts.join('\n');
  const isLast = qIndex >= total - 1;
  const pct = Math.round(((qIndex + 1) / total) * 100);

  area.innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;margin-bottom:8px">
        <h2 style="margin:0">${u ? unitLabelOf(u) : ''}</h2>
        <button class="btn-ghost btn-sm" onclick="backToBoard()">← Kembali ke daftar</button>
      </div>
      <div class="sub" style="margin:0 0 6px">Indikator ${qIndex + 1} dari ${total} · ${terisi}/${total} tersimpan</div>
      <div style="height:6px;background:var(--border);border-radius:99px;overflow:hidden;margin-bottom:14px">
        <div style="height:100%;width:${pct}%;background:var(--primary)"></div>
      </div>

      <div class="aspek-group" style="margin-top:0">
        <div class="aspek-title">${aspek}</div>
        <div class="sub-title">${sub}</div>
        <div class="indikator">
          <div class="judul">${i.nomorUrut || ''}. ${judul} ${ex ? '<span style="color:var(--ok);font-size:12px">✓ tersimpan</span>' : ''}</div>
          ${desk ? `<div class="desk">${desk}</div>` : ''}
          <div class="standar">
            <div><b>Yang dinilai:</b> ${i.spmIndikator || '-'}</div>
            <div><b>Standar:</b> ${i.spmNilai || '-'} &nbsp;·&nbsp; <span class="muted">bobot ${fmt(i.bobot)}</span></div>
          </div>
          <div class="nilai-row">
            <label style="margin:0">Nilai</label>
            <input type="number" min="0" max="100" step="5" id="n-${i.id}" value="${val}" placeholder="0" style="width:90px" />
            <input class="catatan" id="c-${i.id}" placeholder="Catatan (opsional)" value="${esc(cat)}" />
          </div>
        </div>
      </div>

      <div class="totalbar" style="margin-top:16px">
        <button class="btn-ghost" onclick="wizardPrev()" ${qIndex === 0 ? 'disabled' : ''}>← Sebelumnya</button>
        <span class="muted">Kosongkan Nilai untuk melewati indikator ini.</span>
        <button class="btn" onclick="wizardNext()">${isLast ? 'Simpan & Selesai' : 'Selanjutnya →'}</button>
      </div>
    </div>`;

  const nEl = document.getElementById('n-' + i.id);
  if (nEl) {
    nEl.focus();
    nEl.addEventListener('keydown', e => { if (e.key === 'Enter') wizardNext(); });
  }
}

// Simpan indikator yang sedang tampil ke draft (kalau Nilai terisi). Kosong = dilewati.
async function saveCurrentIndikator() {
  const i = qIndikators[qIndex];
  const nEl = document.getElementById('n-' + i.id);
  if (!nEl || nEl.value === '') return false;      // kosong = belum dijawab → tidak disimpan
  const cEl = document.getElementById('c-' + i.id);
  const body = {
    indikatorId: Number(i.id),
    nilaiCapaian: parseFloat(nEl.value),           // termasuk ketik 0 (nilai nol yang sengaja)
    catatan: (cEl && cEl.value) ? cEl.value : null
  };
  if (activeCat === 'BUS') body.busId = curUnitId; else body.halteId = curUnitId;
  await api('/api/penilaian/tambah-detail/' + pn.id, { method: 'POST', body });
  pn = await api('/api/penilaian/get-penilaian/' + pn.id);   // refresh untuk perbarui gate & status unit
  return true;
}

window.wizardNext = async () => {
  if (!curUnitId) { toast('Pilih unit dulu', 'err'); return; }
  try {
    await saveCurrentIndikator();                 // simpan kalau terisi; lewati kalau kosong
    if (qIndex >= qIndikators.length - 1) {        // indikator terakhir → selesai
      toast('Tersimpan ke draft', 'ok');
      unitFormOpen = false;                        // kembali ke papan daftar unit
      renderWorkspace();
      return;
    }
    qIndex++;
    renderCategory();
  } catch (e) { toast(e.message, 'err'); }
};

window.wizardPrev = async () => {
  try {
    await saveCurrentIndikator();                 // simpan dulu yang sedang tampil
    if (qIndex > 0) qIndex--;
    renderCategory();
  } catch (e) { toast(e.message, 'err'); }
};

window.submitPenilaian = async () => {
  if (!gateReady(pn)) { toast('Lengkapi Bus & Halte dulu', 'err'); return; }
  try {
    await api('/api/penilaian/ubah-status/' + pn.id + '?status=SUBMITTED', { method: 'PATCH' });
    toast('Penilaian #' + pn.id + ' disubmit ke atasan', 'ok');
    pn = null; activeCat = null; units = []; qIndikators = []; curUnitId = null;
    activeTab = 'daftar';
    render();
  } catch (e) { toast(e.message, 'err'); }
};

window.resetPenilaian = () => {
  pn = null; activeCat = null; units = []; qIndikators = []; curUnitId = null;
  renderInput();
};

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
    const page = await api('/api/penilaian/daftar?page=0&size=50');
    const rows = page.content || [];
    if (!rows.length) { document.getElementById('list').innerHTML = '<div class="empty">Belum ada penilaian.</div>'; return; }
    let html = `<table><thead><tr>
      <th>ID</th><th>Koridor</th><th>Tanggal</th><th>Status</th>
      <th>Maker</th><th>Checker</th><th>Approver</th><th>Total</th><th>Aksi</th></tr></thead><tbody>`;
    rows.forEach(p => {
      const acts = (NEXT[p.status] || []).filter(a => a[2].includes(user.role))
        .map(a => `<button class="btn-ghost btn-sm" onclick="changeStatus(${p.id},'${a[0]}')">${a[1]}</button>`).join(' ');
      const canEdit = (user.role === 'MAKER' || user.role === 'ADMIN')
        && (p.status === 'DRAFT' || p.status === 'REJECTED');
      const editBtn = canEdit ? `<button class="btn-ghost btn-sm" onclick="editPenilaian(${p.id})">Edit</button>` : '';
      html += `<tr>
        <td>${p.id}</td><td>${p.koridor ? p.koridor.nama : '-'}</td><td>${p.tanggal || '-'}</td>
        <td><span class="badge b-${p.status}">${p.status}</span></td>
        <td>${p.maker ? p.maker.nama : '-'}</td><td>${p.checker ? p.checker.nama : '-'}</td><td>${p.approver ? p.approver.nama : '-'}</td>
        <td>${fmt(p.totalCapaian)}</td>
        <td><button class="btn-ghost btn-sm" onclick="viewDetail(${p.id})">Detail</button>
            <button class="btn-ghost btn-sm" onclick="exportExcel(${p.id})">Excel</button> ${editBtn} ${acts}</td>
      </tr>`;
    });
    html += '</tbody></table>';
    document.getElementById('list').innerHTML = html;
  } catch (e) { document.getElementById('list').innerHTML = `<div class="empty">${e.message}</div>`; }
}

window.editPenilaian = async (id) => {
  try {
    pn = await api('/api/penilaian/get-penilaian/' + id);
    activeCat = null; units = []; qIndikators = []; curUnitId = null;
    activeTab = 'input';
    render();
  } catch (e) { toast(e.message, 'err'); }
};

window.changeStatus = async (id, status) => {
  try {
    await api(`/api/penilaian/ubah-status/${id}?status=${status}`, { method: 'PATCH' });
    toast('Status → ' + status, 'ok');
    render();
  } catch (e) { toast(e.message, 'err'); }
};

window.exportAllExcel = async () => downloadXlsx('/api/penilaian/export-all', 'rekap-penilaian-spm.xlsx');
window.exportExcel = async (id) => downloadXlsx('/api/penilaian/export-penilaian/' + id, 'penilaian-spm-' + id + '.xlsx');

async function downloadXlsx(path, filename) {
  try {
    const res = await fetch(API + path, { headers: { Authorization: 'Bearer ' + token } });
    if (!res.ok) throw new Error('Gagal export (HTTP ' + res.status + ')');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename;
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
    toast('Excel diunduh', 'ok');
  } catch (e) { toast(e.message, 'err'); }
}

window.viewDetail = async (id) => {
  const box = document.getElementById('detail');
  box.innerHTML = '<div class="card empty">Memuat detail…</div>';
  try {
    const p = await api('/api/penilaian/get-penilaian/' + id);
    let html = `<div class="card"><h2>Detail penilaian #${p.id}</h2>
      <p class="sub">${p.koridor ? p.koridor.nama : ''} · ${p.tanggal || ''} · status <b>${p.status}</b> · total <b>${fmt(p.totalCapaian)}</b></p>
      <table><thead><tr><th>Unit</th><th>Indikator</th><th>Nilai</th><th>Bobot</th><th>Skor</th><th>Catatan</th></tr></thead><tbody>`;
    (p.details || []).forEach(d => {
      const unit = d.bus ? ('Bus ' + d.bus.noLambung) : (d.halte ? ('Halte ' + d.halte.nomor) : '-');
      const ur = d.indikator ? (d.indikator.uraian || '').split('\n')[0] : '-';
      const bobot = d.indikator ? d.indikator.bobot : null;
      html += `<tr><td>${unit}</td><td>${ur}</td>
        <td>${fmt(d.nilaiCapaian)}</td><td>${fmt(bobot)}</td><td>${fmt(d.skorTerbobot)}</td><td>${d.catatan || '-'}</td></tr>`;
    });
    html += '</tbody></table></div>';
    box.innerHTML = html;
    box.scrollIntoView({ behavior: 'smooth' });
  } catch (e) { box.innerHTML = `<div class="card empty">${e.message}</div>`; }
};

// =====================================================================
// DASHBOARD (pilih modul)
// =====================================================================
function goDashboard() { activeModule = 'dashboard'; render(); }

window.openModule = (m) => {
  activeModule = m;
  if (m === 'spm') activeTab = (user.role === 'MAKER' || user.role === 'ADMIN') ? 'input' : 'daftar';
  else if (m === 'checklist') clActiveTab = 'isi';
  render();
};

function renderDashboard() {
  document.getElementById('app').innerHTML = `
    <div class="card">
      <h1>Halo, ${user.nama}</h1>
      <p class="sub">${user.jabatan ? user.jabatan + ' · ' : ''}${user.role} — pilih modul yang ingin dikerjakan.</p>
      <div class="modules">
        <div class="mod-card" onclick="openModule('spm')">
          <h2>Penilaian SPM</h2>
          <p class="sub" style="margin:0">Penilaian berkala berbobot (skor). Alur Maker–Checker–Approver.</p>
          <span class="btn btn-sm" style="margin-top:14px;display:inline-block">Buka modul →</span>
        </div>
        <div class="mod-card" onclick="openModule('checklist')">
          <h2>Checklist Harian</h2>
          <p class="sub" style="margin:0">4 form harian: Kendaraan, Pramugara, Bus &amp; Driver, Laporan Korlap.</p>
          <span class="btn btn-sm" style="margin-top:14px;display:inline-block">Buka modul →</span>
        </div>
      </div>
    </div>`;
}

// =====================================================================
// MODUL CHECKLIST HARIAN
// =====================================================================
let clActiveTab = 'isi';   // 'isi' | 'daftar-cl'
let clTemplates = [];
let cl = null;             // checklist header view aktif
let clItems = [];          // master item template aktif
let clAnswers = {};        // non-bus: itemId -> {hasil,keterangan} ; bus: busId -> {itemId -> {hasil,keterangan}}
let clBuses = [];          // daftar bus koridor (khusus form objek Bus)
let clCurBus = null;       // bus id yang sedang diisi; null = tampil board bus

const NEXT_CL = {
  DRAFT: [['SUBMITTED', 'Submit']],
  SUBMITTED: [['DIKETAHUI', 'Diketahui']],
  DIKETAHUI: []
};

function clIsBus() { return !!(cl && cl.template && cl.template.subjek === 'BUS'); }

// item yang harus diisi = leaf (tanpa anak); item induk (mis. "Pakaian Pramugara") tidak dihitung
function clLeafItems() {
  const childIds = new Set(clItems.filter(it => it.parent).map(it => it.parent.id));
  return clItems.filter(it => !childIds.has(it.id));
}

function clBuildAnswers() {
  clAnswers = {};
  const isBus = clIsBus();
  (cl.details || []).forEach(d => {
    if (!d.item) return;
    const rec = { hasil: d.hasil, keterangan: d.keterangan };
    if (isBus) { const b = d.bus ? d.bus.id : 0; (clAnswers[b] = clAnswers[b] || {})[d.item.id] = rec; }
    else clAnswers[d.item.id] = rec;
  });
}

function clBusFilledCount(busId) {
  const ans = clAnswers[busId] || {};
  return clLeafItems().filter(it => { const a = ans[it.id]; return a && (a.hasil === true || a.hasil === false); }).length;
}

// total denda satu bus = jumlah nilai_denda item yang NOT OK (hasil=false) pada bus itu
function clBusDenda(busId) {
  const ans = clAnswers[busId] || {};
  let total = 0;
  clItems.forEach(it => { const a = ans[it.id]; if (a && a.hasil === false && it.nilaiDenda != null) total += Number(it.nilaiDenda); });
  return total;
}

function renderChecklist() {
  if (clActiveTab === 'daftar-cl') { renderClDaftar(); return; }
  if (!cl) { renderClSetup(); return; }
  if (clIsBus() && clCurBus == null) { renderClBusBoard(); return; }
  renderClForm();
}

function clLabels(tipe) {
  if (tipe === 'BAIK_RUSAK') return ['Baik', 'Rusak'];
  if (tipe === 'OK_NOTOK') return ['OK', 'Not OK'];
  return ['Ada', 'Tidak'];
}

function clCurrentTemplate() {
  const v = document.getElementById('cl-tpl');
  if (!v) return null;
  return clTemplates.find(t => String(t.id) === String(v.value)) || null;
}

// ---- Setup: pilih form, koridor, tanggal, identitas ----
async function renderClSetup() {
  document.getElementById('app').innerHTML = `
    <div class="card">
      <h1>Isi Checklist Harian</h1>
      <p class="sub">Pilih jenis form, koridor, dan tanggal — checklist dibuat sebagai DRAFT.</p>
      <div class="row">
        <div><label>Jenis form</label><select id="cl-tpl"></select></div>
        <div><label>Koridor</label><select id="cl-koridor"></select></div>
        <div><label>Tanggal</label><input type="date" id="cl-tanggal" /></div>
        <div><label>Hari</label><input id="cl-hari" readonly /></div>
      </div>
      <div id="cl-identity" class="row" style="margin-top:4px"></div>
      <button class="btn" id="cl-start" style="margin-top:16px">Mulai isi</button>
    </div>`;
  const tgl = document.getElementById('cl-tanggal');
  tgl.value = new Date().toISOString().slice(0, 10);
  const setHari = () => { const d = new Date(tgl.value); document.getElementById('cl-hari').value = isNaN(d) ? '' : HARI[d.getDay()]; };
  setHari();
  tgl.onchange = setHari;
  try {
    if (!clTemplates.length) clTemplates = await api('/api/checklist/template/daftar');
    const tplSel = document.getElementById('cl-tpl');
    if (!clTemplates.length) {
      tplSel.innerHTML = '<option value="">(belum ada data)</option>';
      document.getElementById('cl-start').disabled = true;
      toast('Master checklist kosong — jalankan seed_checklist.sql dulu', 'err');
    } else {
      tplSel.innerHTML = clTemplates.map(t => `<option value="${t.id}">${t.nama}</option>`).join('');
    }
  } catch (e) { toast(e.message, 'err'); }
  try {
    const koridors = await api('/api/koridor/daftar');
    document.getElementById('cl-koridor').innerHTML = koridors.map(k => `<option value="${k.id}">${k.nama}</option>`).join('');
  } catch (e) { toast(e.message, 'err'); }
  document.getElementById('cl-tpl').onchange = clRenderIdentity;
  document.getElementById('cl-koridor').onchange = clRenderIdentity;
  clRenderIdentity();
  document.getElementById('cl-start').onclick = clStart;
}

async function clRenderIdentity() {
  const box = document.getElementById('cl-identity');
  const t = clCurrentTemplate();
  if (!box || !t) { if (box) box.innerHTML = ''; return; }
  if (t.subjek === 'PRAMUGARA') {
    box.innerHTML = `
      <div><label>Nama Pramugara</label><input id="cl-nama" placeholder="Nama pramugara" /></div>
      <div><label>Shift</label><input id="cl-shift" placeholder="Pagi / Siang" /></div>`;
  } else if (t.subjek === 'BUS') {
    box.innerHTML = `<div class="sub" style="align-self:flex-end">Sesi mencakup semua bus di koridor — bus dipilih di langkah berikutnya.</div>`;
  } else {
    box.innerHTML = `<div class="sub" style="align-self:flex-end">Form ini cukup koridor (laporan pengawasan korlap).</div>`;
  }
}

async function clStart() {
  const t = clCurrentTemplate();
  if (!t) { toast('Pilih jenis form', 'err'); return; }
  const koridorId = Number(document.getElementById('cl-koridor').value);
  const body = {
    templateId: Number(document.getElementById('cl-tpl').value),
    koridorId,
    tanggal: document.getElementById('cl-tanggal').value,
    hari: document.getElementById('cl-hari').value,
    details: []
  };
  if (t.subjek === 'PRAMUGARA') {
    const nama = (document.getElementById('cl-nama') || {}).value || '';
    if (!nama.trim()) { toast('Isi nama pramugara', 'err'); return; }
    body.namaPramugara = nama.trim();
    body.shift = (document.getElementById('cl-shift') || {}).value || null;
  }
  try {
    cl = await api('/api/checklist/new-checklist', { method: 'POST', body });
    clItems = await api('/api/checklist/template/' + t.kode + '/items');
    clCurBus = null; clBuses = [];
    clBuildAnswers();
    if (t.subjek === 'BUS') clBuses = await api('/api/bus/daftar?koridorId=' + koridorId);
    toast('Checklist #' + cl.id + ' dibuat (DRAFT)', 'ok');
    render();
  } catch (e) { toast(e.message, 'err'); }
}

// ---- Form pengisian ----
function clTotalDenda() {
  let total = 0;
  const add = (itemId, a) => {
    const it = clItems.find(x => x.id === itemId);
    if (it && a && a.hasil === false && it.nilaiDenda != null) total += Number(it.nilaiDenda);
  };
  if (clIsBus()) Object.values(clAnswers).forEach(m => Object.entries(m).forEach(([iid, a]) => add(Number(iid), a)));
  else Object.entries(clAnswers).forEach(([iid, a]) => add(Number(iid), a));
  return total;
}

function clItemRow(it, ya, tidak, pakaiDenda, ans) {
  const a = ans[it.id] || {};
  const onYa = a.hasil === true, onTidak = a.hasil === false;
  const denda = pakaiDenda && it.nilaiDenda != null
    ? ` · <span class="muted">denda Rp ${Number(it.nilaiDenda).toLocaleString('id-ID')}</span>` : '';
  const sanksi = pakaiDenda && it.sanksiDenda ? `<div class="desk">Sanksi: ${it.sanksiDenda}</div>` : '';
  return `<div class="indikator">
    <div class="judul">${it.nomorUrut ? it.nomorUrut + '. ' : ''}${it.uraian || ''}${denda}</div>
    ${sanksi}
    <div class="nilai-row" style="margin-top:8px">
      <div class="seg">
        <button class="seg-btn ${onYa ? 'on-ok' : ''}" onclick="clSetHasil(${it.id}, true)">${ya}</button>
        <button class="seg-btn ${onTidak ? 'on-bad' : ''}" onclick="clSetHasil(${it.id}, false)">${tidak}</button>
      </div>
      <input class="catatan" placeholder="Keterangan (opsional)" value="${esc(a.keterangan || '')}" onchange="clSetKet(${it.id}, this.value)" />
    </div>
  </div>`;
}

function clRenderItems(ya, tidak, pakaiDenda, ans) {
  const childrenOf = {};
  clItems.forEach(it => { const p = it.parent && it.parent.id; if (p) (childrenOf[p] = childrenOf[p] || []).push(it); });
  let body = '';
  clItems.filter(it => !it.parent).forEach(it => {
    const kids = childrenOf[it.id];
    if (kids && kids.length) {
      body += `<div class="aspek-title" style="margin-top:14px">${it.nomorUrut ? it.nomorUrut + '. ' : ''}${it.uraian || ''}</div>`;
      const order = [], byGrup = {};
      kids.forEach(k => { const g = k.grup || ''; if (!byGrup[g]) { byGrup[g] = []; order.push(g); } byGrup[g].push(k); });
      order.forEach(g => {
        if (g) body += `<div class="sub-title">${g}</div>`;
        byGrup[g].forEach(k => body += clItemRow(k, ya, tidak, pakaiDenda, ans));
      });
    } else {
      body += clItemRow(it, ya, tidak, pakaiDenda, ans);
    }
  });
  return body;
}

function clBelumCount(ans) {
  return clLeafItems().filter(it => { const a = ans[it.id]; return !a || (a.hasil !== true && a.hasil !== false); }).length;
}

// Papan daftar bus (form objek Bus) — mirip papan unit SPM
function renderClBusBoard() {
  const t = cl.template || {};
  const leaves = clLeafItems().length;
  const pakaiDenda = !!t.pakaiDenda;
  let lengkap = 0;
  const rows = clBuses.map(b => {
    const terisi = clBusFilledCount(b.id);
    const done = leaves > 0 && terisi >= leaves;
    if (done) lengkap++;
    const badge = done ? '<span class="badge b-APPROVED">Lengkap</span>'
      : (terisi > 0 ? `<span class="badge b-SUBMITTED">${terisi}/${leaves}</span>` : '<span class="badge b-DRAFT">Belum diisi</span>');
    const dendaCell = pakaiDenda ? `<td>Rp ${clBusDenda(b.id).toLocaleString('id-ID')}</td>` : '';
    return `<tr><td>Lambung ${b.noLambung}${b.platNomor ? ' · ' + b.platNomor : ''}</td><td>${badge}</td>${dendaCell}
      <td style="text-align:right"><button class="btn-ghost btn-sm" onclick="clOpenBus(${b.id})">${terisi > 0 ? 'Edit' : 'Isi'}</button></td></tr>`;
  }).join('');
  const allComplete = clBuses.length > 0 && lengkap === clBuses.length;
  const dendaBar = pakaiDenda ? `<span class="skor-chip">Total denda semua bus: Rp ${clTotalDenda().toLocaleString('id-ID')}</span>` : '';
  const thDenda = pakaiDenda ? '<th>Denda</th>' : '';

  document.getElementById('app').innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap">
        <div>
          <h1 style="margin:0">${t.nama || 'Checklist'} #${cl.id}</h1>
          <p class="sub" style="margin:4px 0 0">${cl.koridor ? cl.koridor.nama : ''} · ${cl.tanggal || ''} · status <b>${cl.status}</b></p>
        </div>
        <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
          ${dendaBar}
          <button class="btn-ghost" onclick="clReset()">+ Checklist baru</button>
          ${cl.status === 'DRAFT' ? `<button class="btn-ghost" onclick="clSaveDraft()">Simpan draft</button>` : ''}
          ${cl.status === 'DRAFT' ? `<button class="btn" onclick="clSubmit()" ${allComplete ? '' : 'disabled'}>Submit</button>` : ''}
        </div>
      </div>
      <p class="sub" style="margin:10px 0 0">${allComplete
        ? 'Semua bus terisi lengkap — siap Submit. '
        : `<b style="color:var(--danger)">${lengkap}/${clBuses.length} bus lengkap</b> — semua bus harus terisi untuk Submit. `}Jawaban tersimpan otomatis.</p>
    </div>
    <div class="card">
      <h2>Daftar Bus — ${cl.koridor ? cl.koridor.nama : ''}</h2>
      ${clBuses.length
        ? `<table style="margin-top:10px"><thead><tr><th>Bus</th><th>Status</th>${thDenda}<th></th></tr></thead><tbody>${rows}</tbody></table>`
        : '<div class="empty">Belum ada bus untuk koridor ini.</div>'}
    </div>`;
}

window.clOpenBus = (busId) => { clCurBus = Number(busId); render(); };
window.clBackBoard = () => { clCurBus = null; render(); };

function renderClForm() {
  const t = cl.template || {};
  const isBus = clIsBus();
  const ans = isBus ? (clAnswers[clCurBus] || {}) : clAnswers;
  const [ya, tidak] = clLabels(t.tipeJawaban);
  const pakaiDenda = !!t.pakaiDenda;
  const belum = clBelumCount(ans);
  const body = clRenderItems(ya, tidak, pakaiDenda, ans);

  // Form per-bus (objek Bus): submit/draft ada di papan bus, di sini cukup isi + kembali
  if (isBus) {
    const bus = clBuses.find(b => b.id === clCurBus);
    const busLabel = bus ? ('Lambung ' + bus.noLambung + (bus.platNomor ? ' · ' + bus.platNomor : '')) : 'Bus';
    document.getElementById('app').innerHTML = `
      <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap">
          <div>
            <h1 style="margin:0">${busLabel}</h1>
            <p class="sub" style="margin:4px 0 0">${t.nama || ''} · ${cl.koridor ? cl.koridor.nama : ''} · ${cl.tanggal || ''}</p>
          </div>
          <button class="btn-ghost" onclick="clBackBoard()">← Daftar bus</button>
        </div>
        <p class="sub" style="margin:10px 0 0">${belum
          ? `<b style="color:var(--danger)">${belum} item belum diisi</b> untuk bus ini. `
          : 'Bus ini sudah lengkap. '}${pakaiDenda ? `Denda bus ini: <b>Rp ${clBusDenda(clCurBus).toLocaleString('id-ID')}</b>. ` : ''}Jawaban tersimpan otomatis.</p>
      </div>
      <div class="card">${body || '<div class="empty">Tidak ada item.</div>'}</div>`;
    return;
  }

  // Form non-bus (Pramugara/Korlap) — tidak diubah
  const identitas = t.subjek === 'PRAMUGARA' ? ((cl.namaPramugara || '') + (cl.shift ? ' · ' + cl.shift : '')) : '';
  const dendaBar = pakaiDenda
    ? `<span class="skor-chip" id="cl-denda">Total denda: Rp ${clTotalDenda().toLocaleString('id-ID')}</span>` : '';

  document.getElementById('app').innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap">
        <div>
          <h1 style="margin:0">${t.nama || 'Checklist'} #${cl.id}</h1>
          <p class="sub" style="margin:4px 0 0">${cl.koridor ? cl.koridor.nama : ''}${identitas ? ' · ' + identitas : ''} · ${cl.tanggal || ''} · status <b>${cl.status}</b></p>
        </div>
        <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
          ${dendaBar}
          <button class="btn-ghost" onclick="clReset()">+ Checklist baru</button>
          ${cl.status === 'DRAFT' ? `<button class="btn-ghost" onclick="clSaveDraft()">Simpan draft</button>` : ''}
          ${cl.status === 'DRAFT' ? `<button class="btn" onclick="clSubmit()" ${belum ? 'disabled' : ''}>Submit</button>` : ''}
        </div>
      </div>
      <p class="sub" style="margin:10px 0 0">${belum
        ? `<b style="color:var(--danger)">${belum} item belum diisi</b> — lengkapi semua untuk bisa Submit. `
        : 'Semua item terisi — siap Submit. '}Jawaban tersimpan otomatis; "Simpan draft" untuk lanjut nanti.</p>
    </div>
    <div class="card">${body || '<div class="empty">Tidak ada item.</div>'}</div>`;
}

function clStore() {
  return clIsBus() ? (clAnswers[clCurBus] = clAnswers[clCurBus] || {}) : clAnswers;
}

window.clSetHasil = async (id, val) => {
  const store = clStore();
  store[id] = store[id] || {};
  store[id].hasil = (store[id].hasil === val) ? null : val;  // klik lagi = batal
  renderClForm();
  try { await clUpsert(id); } catch (e) { toast(e.message, 'err'); }
};

window.clSetKet = async (id, val) => {
  const store = clStore();
  store[id] = store[id] || {};
  store[id].keterangan = val;
  try { await clUpsert(id); } catch (e) { toast(e.message, 'err'); }
};

async function clUpsert(id) {
  const store = clIsBus() ? (clAnswers[clCurBus] || {}) : clAnswers;
  const a = store[id] || {};
  await api('/api/checklist/tambah-detail/' + cl.id, {
    method: 'POST',
    body: {
      itemId: Number(id),
      busId: clIsBus() ? Number(clCurBus) : null,
      hasil: (a.hasil === undefined ? null : a.hasil),
      keterangan: a.keterangan || null
    }
  });
}

window.clReset = () => { cl = null; clItems = []; clAnswers = {}; clBuses = []; clCurBus = null; render(); };

window.clSaveDraft = () => {
  cl = null; clItems = []; clAnswers = {}; clBuses = []; clCurBus = null;
  clActiveTab = 'daftar-cl';
  toast('Tersimpan sebagai draft', 'ok');
  render();
};

window.clSubmit = async () => {
  if (clIsBus()) {
    const leaves = clLeafItems().length;
    const belumBus = clBuses.filter(b => clBusFilledCount(b.id) < leaves).length;
    if (!clBuses.length || belumBus) { toast('Masih ada ' + belumBus + ' bus belum lengkap', 'err'); return; }
  } else {
    const belum = clBelumCount(clAnswers);
    if (belum) { toast('Masih ada ' + belum + ' item belum diisi', 'err'); return; }
  }
  try {
    await api('/api/checklist/ubah-status/' + cl.id + '?status=SUBMITTED', { method: 'PATCH' });
    toast('Checklist #' + cl.id + ' disubmit', 'ok');
    cl = null; clItems = []; clAnswers = {}; clBuses = []; clCurBus = null; clActiveTab = 'daftar-cl';
    render();
  } catch (e) { toast(e.message, 'err'); }
};

// ---- Daftar checklist ----
async function renderClDaftar() {
  document.getElementById('app').innerHTML = `
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap">
        <div><h1>Daftar Checklist Harian</h1><p class="sub" style="margin:0">Alur: DRAFT → SUBMITTED → DIKETAHUI</p></div>
        <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
          <span><label style="display:inline;margin:0 6px 0 0">Form</label><select id="cl-filter" style="width:auto;display:inline-block"></select></span>
          <button class="btn" onclick="clExportAll()">Unduh Excel (4 sheet)</button>
        </div>
      </div>
      <div id="cl-list" class="empty" style="margin-top:14px">Memuat…</div>
    </div>
    <div id="cl-detail"></div>`;
  try { if (!clTemplates.length) clTemplates = await api('/api/checklist/template/daftar'); } catch (e) {}
  const fsel = document.getElementById('cl-filter');
  fsel.innerHTML = `<option value="">Semua</option>` + clTemplates.map(t => `<option value="${t.id}">${t.nama}</option>`).join('');
  fsel.onchange = () => clLoadList(fsel.value);
  clLoadList('');
}

async function clLoadList(templateId) {
  const list = document.getElementById('cl-list');
  list.innerHTML = 'Memuat…';
  try {
    const page = await api('/api/checklist/daftar?page=0&size=50' + (templateId ? '&templateId=' + templateId : ''));
    const rows = page.content || [];
    if (!rows.length) { list.innerHTML = '<div class="empty">Belum ada checklist.</div>'; return; }
    let html = `<table><thead><tr>
      <th>ID</th><th>Form</th><th>Koridor</th><th>Objek</th><th>Tanggal</th><th>Status</th><th>Denda</th><th>Aksi</th></tr></thead><tbody>`;
    rows.forEach(c => {
      const objek = c.bus ? ('Lambung ' + c.bus.noLambung)
        : (c.namaPramugara ? c.namaPramugara
        : ((c.template && c.template.subjek === 'BUS') ? 'Semua bus' : '-'));
      const acts = (NEXT_CL[c.status] || []).map(a => `<button class="btn-ghost btn-sm" onclick="clChangeStatus(${c.id},'${a[0]}')">${a[1]}</button>`).join(' ');
      const editBtn = c.status === 'DRAFT' ? `<button class="btn-ghost btn-sm" onclick="clOpen(${c.id})">Edit</button>` : '';
      const denda = (c.template && c.template.pakaiDenda) ? ('Rp ' + Number(c.totalDenda || 0).toLocaleString('id-ID')) : '-';
      html += `<tr>
        <td>${c.id}</td><td>${c.template ? c.template.nama : '-'}</td><td>${c.koridor ? c.koridor.nama : '-'}</td>
        <td>${objek}</td><td>${c.tanggal || '-'}</td>
        <td><span class="badge b-${c.status}">${c.status}</span></td><td>${denda}</td>
        <td><button class="btn-ghost btn-sm" onclick="clViewDetail(${c.id})">Detail</button> ${editBtn} ${acts}
            <button class="btn-ghost btn-sm" onclick="clDelete(${c.id})">Hapus</button></td>
      </tr>`;
    });
    html += '</tbody></table>';
    list.innerHTML = html;
  } catch (e) { list.innerHTML = `<div class="empty">${e.message}</div>`; }
}

window.clOpen = async (id) => {
  try {
    cl = await api('/api/checklist/get-checklist/' + id);
    clItems = await api('/api/checklist/template/' + cl.template.kode + '/items');
    clCurBus = null; clBuses = [];
    clBuildAnswers();
    if (clIsBus()) clBuses = await api('/api/bus/daftar?koridorId=' + (cl.koridor ? cl.koridor.id : ''));
    clActiveTab = 'isi';
    render();
  } catch (e) { toast(e.message, 'err'); }
};

window.clChangeStatus = async (id, status) => {
  try {
    await api('/api/checklist/ubah-status/' + id + '?status=' + status, { method: 'PATCH' });
    toast('Status → ' + status, 'ok');
    render();
  } catch (e) { toast(e.message, 'err'); }
};

window.clExportAll = () => downloadXlsx('/api/checklist/export-all', 'rekap-checklist-harian.xlsx');

window.clDelete = async (id) => {
  if (!confirm('Hapus checklist #' + id + '?')) return;
  try {
    await api('/api/checklist/hapus/' + id, { method: 'DELETE' });
    toast('Checklist dihapus', 'ok');
    render();
  } catch (e) { toast(e.message, 'err'); }
};

window.clViewDetail = async (id) => {
  const box = document.getElementById('cl-detail');
  box.innerHTML = '<div class="card empty">Memuat detail…</div>';
  try {
    const c = await api('/api/checklist/get-checklist/' + id);
    const [ya, tidak] = clLabels(c.template ? c.template.tipeJawaban : '');
    const isBus = c.template && c.template.subjek === 'BUS';
    const objek = c.bus ? ('Lambung ' + c.bus.noLambung)
      : (c.namaPramugara ? (c.namaPramugara + (c.shift ? ' · ' + c.shift : ''))
      : (isBus ? 'Semua bus' : '-'));
    const dendaInfo = (c.template && c.template.pakaiDenda)
      ? ` · total denda <b>Rp ${Number(c.totalDenda || 0).toLocaleString('id-ID')}</b>` : '';
    const det = (c.details || []).slice().sort((a, b) =>
      ((a.bus ? a.bus.id : 0) - (b.bus ? b.bus.id : 0)) ||
      String(a.item ? a.item.nomorUrut : '').localeCompare(String(b.item ? b.item.nomorUrut : ''), 'id', { numeric: true }));
    let html = `<div class="card"><h2>${c.template ? c.template.nama : 'Checklist'} #${c.id}</h2>
      <p class="sub">${c.koridor ? c.koridor.nama : ''} · ${objek} · ${c.tanggal || ''} · status <b>${c.status}</b>${dendaInfo}</p>
      <table><thead><tr>${isBus ? '<th>Bus</th>' : ''}<th>No</th><th>Uraian</th><th>Hasil</th><th>Keterangan</th></tr></thead><tbody>`;
    det.forEach(d => {
      const it = d.item || {};
      const hasil = d.hasil === true ? ya : (d.hasil === false ? `<b style="color:var(--danger)">${tidak}</b>` : '-');
      const busCell = isBus ? `<td>${d.bus ? ('Lambung ' + d.bus.noLambung) : '-'}</td>` : '';
      html += `<tr>${busCell}<td>${it.nomorUrut || ''}</td><td>${it.uraian || '-'}</td><td>${hasil}</td><td>${d.keterangan || '-'}</td></tr>`;
    });
    html += '</tbody></table>';
    if (isBus && c.template && c.template.pakaiDenda) {
      const perBus = {};
      (c.details || []).forEach(d => {
        if (d.bus && d.hasil === false && d.item && d.item.nilaiDenda != null) {
          perBus[d.bus.id] = perBus[d.bus.id] || { label: 'Lambung ' + d.bus.noLambung, total: 0 };
          perBus[d.bus.id].total += Number(d.item.nilaiDenda);
        }
      });
      const list = Object.values(perBus);
      if (list.length) {
        html += `<h3 style="margin:16px 0 6px">Denda per bus</h3>
          <table><thead><tr><th>Bus</th><th>Total denda</th></tr></thead><tbody>${
            list.map(x => `<tr><td>${x.label}</td><td>Rp ${x.total.toLocaleString('id-ID')}</td></tr>`).join('')}</tbody></table>`;
      }
    }
    html += '</div>';
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
  if (activeModule === 'dashboard') { renderDashboard(); return; }
  if (activeModule === 'checklist') { renderChecklist(); return; }
  if (activeTab === 'input') renderInput();
  else renderDaftar();
}

render();
