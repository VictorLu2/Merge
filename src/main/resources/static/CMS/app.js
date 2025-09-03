// 基礎：API 與 Auth Helperconst
API = {
    authSignin: '/api/auth/public/signin',
    verifyTotpLogin: '/api/auth/public/verify-totp-login',
    users: '/api/admin/getusers',
    roles: '/api/admin/roles',
    addUser: '/api/admin/add-user',
    enableUser: '/api/admin/update-enabled-status',
    books: '/api/admin/books',
    adminLogs: '/api/admin/admin-logs',
    couponsAdmin: '/api/admin/coupons',
    couponsPublic: '/api/coupons',
    points: '/api/points',
    pointsRule: '/api/points/rules',
    supportAdmin: '/api/admin/support',
};

function authHeader() {
    const token = localStorage.getItem('jwt');
    return token ? {'Authorization': `Bearer ${token}`} : {};
}

// 改善：統一錯誤處理，保留 HTTP 狀態碼並嘗試提取後端的 message
function extractMessage(text) {
    try { const obj = JSON.parse(text); return obj.message || text; } catch { return text; }
}

async function apiGet(url) {
    const res = await fetch(url, {headers: {...authHeader()}});
    if (!res.ok) {
        const text = await res.text();
        const err = new Error(extractMessage(text));
        err.status = res.status; err.body = text;
        throw err;
    }
    return res.json();
}

async function apiJSON(method, url, bodyObj) {
    const res = await fetch(url, {
        method,
        headers: {'Content-Type': 'application/json', ...authHeader()},
        body: JSON.stringify(bodyObj || {})
    });
    if (!res.ok) {
        const text = await res.text();
        const err = new Error(extractMessage(text));
        err.status = res.status; err.body = text;
        throw err;
    }
    return res.json().catch(() => ({}));
}

async function apiForm(method, url, params) {
    const qs = new URLSearchParams(params).toString();
    const sep = url.includes('?') ? '&' : '?';
    const res = await fetch(url + sep + qs, {method, headers: {...authHeader()}});
    if (!res.ok) {
        const text = await res.text();
        const err = new Error(extractMessage(text));
        err.status = res.status; err.body = text;
        throw err;
    }
    return res.text();
}

// 首頁圖表實例與工具
let homeCharts = {trend: null, donut: null, topBooks: null};

function formatTWD(n) {
    return 'NT$' + (Number(n) || 0).toLocaleString('zh-Hant-TW');
}

// 新增：HTML 轉義（避免長字串/特殊符號破版或注入）
function escHtml(s) {
    if (s === undefined || s === null) return '';
    return String(s)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function generateSalesTrend(days) {
    const labels = [];
    const values = [];
    const now = new Date();
    let base = 400 + Math.random() * 400;
    for (let i = days - 1; i >= 0; i--) {
        const d = new Date(now);
        d.setDate(now.getDate() - i);
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        labels.push(`${mm}-${dd}`);
        base += (Math.random() - 0.4) * 80; // 小幅波動
        values.push(Math.max(0, Math.round(base + Math.sin(i / 2) * 120)));
    }
    return {labels, values};
}

function generateOrderDistribution() {
    const labels = ['FULFILLED', 'PAID', 'PAYING', 'CREATED', 'FAILED'];
    const values = labels.map(() => Math.floor(5 + Math.random() * 25));
    return {labels, values};
}

function generateTopBooks() {
    const labels = ['深入Java', 'Spring實戰', 'MySQL設計', '演算法圖解', 'Clean Code'];
    const values = labels.map(() => Math.floor(50 + Math.random() * 160));
    return {labels, values};
}

// 初始化：填入使用者資訊，載入首頁
(function init() {
    const me = localStorage.getItem('email') || '';
    const el = document.getElementById('me');
    if (el) el.textContent = me;
    renderHome();
    renderUsers();
    renderBooks();
    renderPoints();
    renderCoupons();
    renderSupport();
    renderOrders();
    renderAdminLogs();
})();

// 首頁：簡要統計 + 圖表
async function renderHome() {
    const root = document.getElementById('home');
    if (!root) return;
    root.innerHTML = `<div class="grid cols-3">  
    <div class="card" id="stat-users">Users</div>  
    <div class="card" id="stat-books">Books</div>  
    <div class="card" id="stat-coupons">Coupons</div>  
  </div>  
  <div class="grid cols-2" style="margin-top:16px">  
    <div class="grid" style="gap:16px">  
      <div class="card">  
        <div class="muted" style="margin-bottom:6px">銷售趨勢（最近 30 天）</div>  
        <canvas id="salesTrendChart" height="140"></canvas>  
      </div>  
      <div class="card">  
        <div class="muted" style="margin-bottom:6px">熱門書籍銷量 Top 5（示意）</div>  
        <canvas id="topBooksChart" height="140"></canvas>  
      </div>  
    </div>  
    <div class="card">  
      <div class="muted" style="margin-bottom:6px">訂單狀態分佈（示意）</div>  
      <canvas id="orderStatusChart" height="140"></canvas>  
    </div>  
  </div>`;
    try {
        const [users, books, couponsPage] = await Promise.all([
            apiGet(API.users),
            apiGet(API.books + '?size=1'),
            apiGet(API.couponsAdmin + '?size=1')
        ]);
        document.getElementById('stat-users').innerHTML = `<div class="muted">使用者數</div><div style="font-size:24px;font-weight:700">${users.length}</div>`;
        document.getElementById('stat-books').innerHTML = `<div class="muted">書籍數</div><div style="font-size:24px;font-weight:700">${books.totalElements || 0}</div>`;
        document.getElementById('stat-coupons').innerHTML = `<div class="muted">優惠券</div><div style="font-size:24px;font-weight:700">${couponsPage.totalElements || 0}</div>`;

        // 示意資料（可改為真實訂單 API）
        const trend = generateSalesTrend(30);
        const orderDist = generateOrderDistribution();
        const topBooks = generateTopBooks();

        // 折線圖：銷售趨勢
        const ctx1 = document.getElementById('salesTrendChart').getContext('2d');
        const grad = ctx1.createLinearGradient(0, 0, 0, 180);
        grad.addColorStop(0, 'rgba(31,111,235,0.35)');
        grad.addColorStop(1, 'rgba(31,111,235,0.00)');
        if (homeCharts.trend) homeCharts.trend.destroy();
        homeCharts.trend = new Chart(ctx1, {
            type: 'line',
            data: {
                labels: trend.labels,
                datasets: [{
                    label: '銷售額（元）',
                    data: trend.values,
                    borderColor: '#1f6feb',
                    backgroundColor: grad,
                    fill: true,
                    borderWidth: 2,
                    tension: 0.35,
                    pointRadius: 2.5,
                    pointHoverRadius: 5
                }]
            },
            options: {
                responsive: true,
                interaction: {mode: 'index', intersect: false},
                plugins: {
                    legend: {display: false},
                    tooltip: {callbacks: {label: ctx => ` ${formatTWD(ctx.parsed.y || 0)}`}}
                },
                scales: {x: {grid: {display: false}}, y: {beginAtZero: true}}
            }
        });

        // 甜甜圈圖：訂單狀態分佈
        const ctx2 = document.getElementById('orderStatusChart').getContext('2d');
        if (homeCharts.donut) homeCharts.donut.destroy();
        homeCharts.donut = new Chart(ctx2, {
            type: 'doughnut',
            data: {
                labels: orderDist.labels,
                datasets: [{
                    data: orderDist.values,
                    backgroundColor: ['#238636', '#1f6feb', '#d29922', '#7d8590', '#b42318']
                }]
            },
            options: {cutout: '60%', plugins: {legend: {position: 'bottom'}}}
        });

        // 橫向長條：熱門書籍 Top5
        const ctx3 = document.getElementById('topBooksChart').getContext('2d');
        if (homeCharts.topBooks) homeCharts.topBooks.destroy();
        homeCharts.topBooks = new Chart(ctx3, {
            type: 'bar',
            data: {
                labels: topBooks.labels,
                datasets: [{
                    label: '銷售量（本）',
                    data: topBooks.values,
                    backgroundColor: '#238636'
                }]
            },
            options: {
                indexAxis: 'y',
                plugins: { legend: { display: false } },
                scales: { x: { beginAtZero: true } }
            }
        });
    } catch (e) {
        root.innerHTML += `<div class="muted">載入統計失敗：${e.message}</div>`;
    }
}

// 使用者管理
async function renderUsers() {
    const root = document.getElementById('users');
    if (!root) return;
    root.innerHTML = `<div class="actions">  
      <button class="btn" onclick="openUserModal()">新增使用者</button>  
    </div>  
    <div class="card" style="margin-top:12px">  
      <table>  
        <thead><tr>  
          <th>ID</th><th>電子郵件</th><th>使用者名稱</th><th>角色</th><th>啟用狀態</th><th>會員等級</th><th>操作</th>  
        </tr></thead>  
        <tbody id="usersTbody"></tbody>  
      </table>  
    </div>  
    <div id="userModal" class="card" style="display:none; margin-top:12px">  
      <h3 style="margin-top:0">新增使用者</h3>  
      <div class="form-grid">  
        <div><label>電子郵件</label><input id="u_email" type="email" required></div>  
        <div><label>使用者名稱</label><input id="u_username" type="text" required></div>  
        <div><label>密碼</label><input id="u_password" type="password" required></div>  
        <div><label>性別</label>  
          <select id="u_gender" required><option value="male">男</option><option value="female">女</option></select>  
        </div>  
        <div><label>生日</label><input id="u_birth" type="date" required></div>  
        <div><label>角色</label>  
          <select id="u_role"><option value="user">會員</option><option value="admin">管理員</option></select>  
        </div>  
      </div>  
      <div class="actions" style="margin-top:12px">  
        <button id="u_submit" class="btn" onclick="submitCreateUser()">送出</button>  
        <button class="btn secondary" onclick="closeUserModal()">取消</button>  
        <span id="u_msg" class="muted"></span>  
      </div>  
    </div>`;

    // 表單前端約束：生日不可晚於今天；必填欄位
    const birth = document.getElementById('u_birth');
    if (birth) {
        const today = new Date();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        birth.max = `${today.getFullYear()}-${mm}-${dd}`;
        birth.required = true;
    }
    ['u_email','u_username','u_password','u_gender'].forEach(id => { const el = document.getElementById(id); if (el) el.required = true; });
    try {
        const list = await apiGet(API.users);
        const tbody = document.getElementById('usersTbody');
        tbody.innerHTML = list.map(u => `<tr>  
      <td>${u.userId}</td>  
      <td>${u.email}</td>  
      <td>${u.userName || ''}</td>  
      <td>${(u.role && (u.role.roleName || u.roleName)) || ''}</td>  
      <td>${u.enabled ? '是' : '否'}</td>  
      <td>${u.membershipTier || ''}</td>  
      <td class="inline">  
        <button class="btn small" onclick="toggleEnable(${u.userId}, ${!u.enabled})">${u.enabled ? '停用' : '啟用'}</button>  
      </td>  
    </tr>`).join('');
    } catch (e) {
        root.innerHTML += `<div class="muted">載入使用者失敗：${e.message}</div>`;
    }
}

function openUserModal() {
    document.getElementById('userModal').style.display = 'block';
}

function closeUserModal() {
    document.getElementById('userModal').style.display = 'none';
}

async function submitCreateUser() {
    const email = document.getElementById('u_email').value.trim();
    const username = document.getElementById('u_username').value.trim();
    const password = document.getElementById('u_password').value;
    const gender = document.getElementById('u_gender').value;
    const birthdate = document.getElementById('u_birth').value;
    const role = [document.getElementById('u_role').value];

    const msg = document.getElementById('u_msg');
    const btn = document.getElementById('u_submit');

    function setBtnLoading(loading) {
        if (!btn) return;
        btn.disabled = !!loading;
        btn.textContent = loading ? '送出中…' : '送出';
    }

    // 基本前端驗證
    const todayStr = new Date().toISOString().slice(0, 10);
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        msg.textContent = '請輸入有效的電子郵件';
        return;
    }
    if (!username || username.length < 3) {
        msg.textContent = '使用者名稱至少 3 個字元';
        return;
    }
    if (!password || password.length < 6) {
        msg.textContent = '密碼至少 6 個字元';
        return;
    }
    if (!birthdate) {
        msg.textContent = '請選擇生日';
        return;
    }
    if (birthdate > todayStr) {
        msg.textContent = '生日需早於今天';
        return;
    }

    const body = { email, username, password, gender, birthdate, role };

    try {
        msg.textContent = '';
        setBtnLoading(true);
        await apiJSON('POST', API.addUser, body);
        msg.textContent = '新增成功';
        await renderUsers();
        closeUserModal();
    } catch (e) {
        const statusInfo = e && e.status ? ` (HTTP ${e.status})` : '';
        msg.textContent = '錯誤：' + (e && e.message ? e.message : '未知錯誤') + statusInfo;
        if (e && (e.status === 401 || e.status === 403)) {
            msg.textContent += '，請確認已以管理員身份登入';
        }
        console.error('新增使用者失敗', e);
    } finally {
        setBtnLoading(false);
    }
}

async function toggleEnable(userId, enabled) {
    try {
        await apiForm('PUT', API.enableUser, {userId, enabled});
        await renderUsers();
    } catch (e) {
        alert('更新狀態失敗：' + e.message);
    }
}

// 書籍管理
async function renderBooks() {
    const root = document.getElementById('books');
    if (!root) return;
    root.innerHTML = `<div class="actions">  
      <button class="btn" onclick="openCreateBookModal()">新增書籍</button>  
    </div>  
    <div class="card" style="margin-top:12px">  
      <table>  
        <thead><tr>  
          <th>ID</th><th>封面</th><th>標題</th><th>作者</th><th>價格</th><th>狀態</th><th>操作</th>  
        </tr></thead>  
        <tbody id="booksTbody"></tbody>  
      </table>  
    </div>  
    <div id="bookModal" class="card" style="display:none; margin-top:12px">  
      <h3 style="margin-top:0">新增 / 編輯書籍</h3>  
      <div class="form-grid">  
        <div><label>標題</label><input id="b_title"></div>  
        <div><label>作者</label><input id="b_author"></div>  
        <div><label>定價</label><input id="b_price" type="number" min="0"></div>  
        <div><label>狀態</label><input id="b_status" placeholder="販售中/下架"></div>  
        <div><label>出版社</label><input id="b_pub"></div>  
        <div><label>封面 URL</label><input id="b_cover"><div class="muted" style="font-size:12px;margin-top:4px">或使用下方上傳至 S3，成功後會自動填入此欄位</div><div id="b_cover_preview" class="muted" style="margin-top:4px"></div></div>  
        <div><label>語言</label><input id="b_lang"></div>  
        <div><label>格式</label><input id="b_format"></div>  
        <div style="grid-column:1/-1"><label>上傳封面至 S3</label><input id="b_cover_file" type="file" accept="image/*" onchange="uploadBookCover(event)"><div class="muted" style="font-size:12px;margin-top:4px">支援 JPG/PNG/GIF/WebP，最大 5MB；上傳成功後自動填入封面 URL</div></div>  
      </div>  
      <div class="actions" style="margin-top:12px">  
        <button class="btn" onclick="submitBook()">送出</button>  
        <button class="btn secondary" onclick="closeBookModal()">取消</button>  
        <span id="b_msg" class="muted"></span>  
      </div>  
    </div>`;
    // 綁定封面 URL 變動以更新預覽
    const coverInput = document.getElementById('b_cover');
    if (coverInput) coverInput.addEventListener('input', updateCoverPreview);
    updateCoverPreview();
    try {
        const page = await apiGet(API.books + '?size=100');
        const tbody = document.getElementById('booksTbody');
        tbody.innerHTML = page.content.map(b => `<tr>  
      <td>${b.id}</td>  
      <td>${b.coverImageUrl ? `<img src="${b.coverImageUrl}" alt="cover" style="width:70px;height:100px;object-fit:cover;border-radius:4px;border:1px solid #30363d"/>` : ''}</td>  
      <td>${b.title}</td>  
      <td>${b.author}</td>  
      <td>${b.listPrice}</td>  
      <td>${b.status}</td>  
      <td class="inline">  
        <button class="btn small" onclick="editBook(${b.id})">編輯</button>  
        <button class="btn small danger" onclick="deleteBook(${b.id})">刪除</button>  
      </td>  
    </tr>`).join('');
    } catch (e) {
        root.innerHTML += `<div class="muted">載入書籍失敗：${e.message}</div>`;
    }
}

let editingBookId = null;

function openCreateBookModal() {
    editingBookId = null;
    openBookModal();
}

function openBookModal() {
    document.getElementById('bookModal').style.display = 'block';
    // 不再重設 editingBookId，避免編輯被誤判為新增
    clearBookForm();
}

function closeBookModal() {
    document.getElementById('bookModal').style.display = 'none';
}

function clearBookForm() {
    ['b_title', 'b_author', 'b_price', 'b_status', 'b_pub', 'b_cover', 'b_lang', 'b_format'].forEach(id => document.getElementById(id).value = '');
    updateCoverPreview();
}

async function editBook(id) {
    try {
        const b = await apiGet(API.books + '/' + id);
        editingBookId = id;
        openBookModal();
        document.getElementById('b_title').value = b.title || '';
        document.getElementById('b_author').value = b.author || '';
        document.getElementById('b_price').value = b.listPrice || 0;
        document.getElementById('b_status').value = b.status || '';
        document.getElementById('b_pub').value = b.publisherName || '';
        document.getElementById('b_cover').value = b.coverImageUrl || '';
        document.getElementById('b_lang').value = b.language || '';
        document.getElementById('b_format').value = b.format || '';
        updateCoverPreview();
    } catch (e) {
        alert('讀取書籍失敗：' + e.message);
    }
}

async function submitBook() {
    const body = {
        title: document.getElementById('b_title').value.trim(),
        author: document.getElementById('b_author').value.trim(),
        listPrice: parseInt(document.getElementById('b_price').value || '0', 10),
        status: document.getElementById('b_status').value.trim() || '販售中',
        publisherName: document.getElementById('b_pub').value.trim(),
        coverImageUrl: document.getElementById('b_cover').value.trim(),
        language: document.getElementById('b_lang').value.trim(),
        format: document.getElementById('b_format').value.trim()
    };
    try {
        if (editingBookId) {
            await apiJSON('PUT', API.books + '/' + editingBookId, body);
        } else {
            await apiJSON('POST', API.books, body);
        }
        document.getElementById('b_msg').textContent = '儲存成功';
        await renderBooks();
        closeBookModal();
    } catch (e) {
        document.getElementById('b_msg').textContent = '錯誤：' + e.message;
    }
}

async function deleteBook(id) {
    if (!confirm('確定刪除？')) return;
    try {
        await fetch(API.books + '/' + id, {method: 'DELETE', headers: {...authHeader()}});
        await renderBooks();
    } catch (e) {
        alert('刪除失敗：' + e.message);
    }
}

// 上傳書籍封面：選取檔案後自動呼叫
async function uploadBookCover(e) {
    const file = e && e.target && e.target.files && e.target.files[0];
    if (!file) return;
    // 前置檢查：大小與格式
    const max = 5 * 1024 * 1024; // 5MB
    if (file.size > max) {
        alert('上傳封面失敗：檔案超過 5MB 上限');
        return;
    }
    const okTypes = ['image/jpeg','image/jpg','image/pjpeg','image/png','image/x-png','image/gif','image/webp'];
    if (file.type && !okTypes.includes(file.type.toLowerCase())) {
        alert('上傳封面失敗：僅支援 JPG/PNG/GIF/WebP');
        return;
    }
    try {
        const form = new FormData();
        form.append('file', file);
        const res = await fetch(API.books + '/upload-cover', { method: 'POST', headers: { ...authHeader() }, body: form });
        if (!res.ok) throw new Error(await res.text());
        const url = await res.text();
        document.getElementById('b_cover').value = url.replaceAll('"','');
        updateCoverPreview();
    } catch (err) {
        alert('上傳封面失敗：' + err.message);
    }
}

function updateCoverPreview() {
    const box = document.getElementById('b_cover_preview');
    if (!box) return;
    const url = (document.getElementById('b_cover') && document.getElementById('b_cover').value.trim()) || '';
    if (url) {
        box.innerHTML = `<img src="${url}" alt="封面預覽" style="width:120px;height:168px;object-fit:cover;border-radius:4px;border:1px solid #30363d"/>`;
    } else {
        box.innerHTML = '<span class="muted">尚無預覽</span>';
    }
}

// 點數管理
async function renderPoints() {
    const root = document.getElementById('points');
    if (!root) return;
    root.innerHTML = `  
  <div class="card">  
    <h3 style="margin-top:0">查詢用戶點數</h3>  
    <div class="inline">  
      <input id="p_user_id" placeholder="User ID">  
      <button class="btn" onclick="loadPointsUser()">載入</button>  
    </div>  
    <div id="p_balance" class="muted" style="margin-top:8px"></div>  
    <div class="grid cols-2" style="margin-top:12px">  
      <div>  
        <h4>點數流水</h4>  
        <table class="compact"><thead><tr><th>時間</th><th>變動</th><th>原因</th><th>餘額</th></tr></thead><tbody id="p_ledger"></tbody></table>  
      </div>  
      <div>          <h4>點數批次</h4>  
        <table class="compact"><thead><tr><th>ID</th><th>獲得</th><th>已用</th><th>到期</th></tr></thead><tbody id="p_lots"></tbody></table>  
      </div>  
    </div>  
    <div style="margin-top:12px">  
      <div class="inline">  
        <input id="p_adjust_amt" type="number" placeholder="調整點數，可正可負">  
        <input id="p_adjust_note" placeholder="備註">  
        <button class="btn" onclick="adjustPoints()">調整</button>  
      </div>  
    </div>  
  </div>
  <div class="card" style="margin-top:16px">  
    <h3 style="margin-top:0">平台點數規則</h3>  
    <div class="form-grid">  
        <div><label>回饋萬分比</label><input id="r_reward" type="number" min="0"></div>  
        <div><label>每筆回饋上限</label><input id="r_max" type="number" min="0"></div>  
        <div><label>1點折抵金額</label><input id="r_rate" type="number" step="0.0001"></div>  
        <div><label>最大折抵比例(萬分比)</label><input id="r_ratio" type="number" min="0"></div>  
        <div><label>到期策略</label>  
          <select id="r_policy"><option value="NONE">NONE</option><option value="FIXED_DATE">FIXED_DATE</option><option value="ROLLING_DAYS">ROLLING_DAYS</option></select>  
        </div>  
        <div><label>Rolling 天數</label><input id="r_days" type="number" min="0"></div>  
        <div><label>固定到期日(如1231)</label><input id="r_fixed" type="number" min="101" max="1231"></div>  
    </div>  
    <div class="actions" style="margin-top:12px">  
      <button class="btn" onclick="saveRule()">儲存</button>  
      <span id="r_msg" class="muted"></span>  
    </div>  
  </div>`;
    try {
        const rule = await apiGet(API.pointsRule + '/current');
        if (rule) {
            document.getElementById('r_reward').value = rule.rewardRateBp || 100;
            document.getElementById('r_max').value = rule.maxRewardPoints || '';
            document.getElementById('r_rate').value = (rule.redeemRate && rule.redeemRate) || 1;
            document.getElementById('r_ratio').value = rule.maxRedeemRatioBp || '';
            document.getElementById('r_policy').value = rule.expiryPolicy || 'NONE';
            document.getElementById('r_days').value = rule.rollingDays || '';
            document.getElementById('r_fixed').value = rule.fixedExpireDay || '';
        }
    } catch (e) { /* ignore */
    }
}

async function loadPointsUser() {
    const uid = document.getElementById('p_user_id').value.trim();
    if (!uid) return;
    try {
        const bal = await apiGet(`${API.points}/${uid}/balance`);
        document.getElementById('p_balance').textContent = `餘額：${bal.balance}`;
        const ledger = await apiGet(`${API.points}/${uid}/ledger?page=0&size=50`);
        document.getElementById('p_ledger').innerHTML = (ledger.content || []).map(i => `<tr>  
      <td>${i.createdAt || ''}</td><td>${i.changeAmount}</td><td>${i.reason}</td><td>${i.balanceAfter}</td>  
    </tr>`).join('');
        const lots = await apiGet(`${API.points}/${uid}/lots?page=0&size=50`);
        document.getElementById('p_lots').innerHTML = (lots.content || []).map(l => `<tr>  
      <td>${l.id}</td><td>${l.earnedPoints}</td><td>${l.usedPoints}</td><td>${l.expiresAt || ''}</td>  
    </tr>`).join('');
    } catch (e) {
        alert('載入點數資料失敗：' + e.message);
    }
}

async function adjustPoints() {
    const uid = document.getElementById('p_user_id').value.trim();
    const amount = parseInt(document.getElementById('p_adjust_amt').value || '0', 10);
    const note = document.getElementById('p_adjust_note').value;
    if (!uid || !amount) return;
    try {
        await apiJSON('POST', `${API.points}/adjust`, {userId: Number(uid), amount, note});
        await loadPointsUser();
    } catch (e) {
        alert('調整失敗：' + e.message);
    }
}

async function saveRule() {
    const body = {
        rewardRateBp: parseInt(document.getElementById('r_reward').value || '100', 10),
        maxRewardPoints: valOrNull(document.getElementById('r_max').value),
        redeemRate: document.getElementById('r_rate').value,
        maxRedeemRatioBp: valOrNull(document.getElementById('r_ratio').value),
        expiryPolicy: document.getElementById('r_policy').value,
        rollingDays: valOrNull(document.getElementById('r_days').value),
        fixedExpireDay: valOrNull(document.getElementById('r_fixed').value),
    };
    try {
        const rule = await apiJSON('PUT', API.pointsRule, body);
        document.getElementById('r_msg').textContent = '已儲存';
    } catch (e) {
        document.getElementById('r_msg').textContent = '錯誤：' + e.message;
    }
}

function valOrNull(v) {
    return (v === undefined || v === null || String(v).trim() === '') ? null : Number(v);
}

// 優惠券管理
async function renderCoupons() {
    const root = document.getElementById('coupons');
    if (!root) return;
    root.innerHTML = `  
  <div class="actions">  
    <button class="btn" onclick="openCouponModal()">新增優惠券</button>  
  </div>  
  <div class="card" style="margin-top:12px">  
    <table>  
      <thead><tr>  
        <th>ID</th><th>名稱</th><th>代碼</th><th>型態</th><th>折扣</th><th>狀態</th><th>期間</th><th>操作</th>  
      </tr></thead>  
      <tbody id="couponTbody"></tbody>  
    </table>  
  </div>  
  <div id="couponModal" class="card" style="display:none;margin-top:12px">  
    <h3 style="margin-top:0">新增 / 編輯優惠券</h3>  
    <div class="form-grid">  
      <div><label>名稱</label><input id="c_name" placeholder="如：春季滿額折扣"></div>  
      <div><label>代碼型態</label><select id="c_code_type"><option value="GENERIC">GENERIC</option></select><div class="muted" style="font-size:12px;margin-top:4px">目前僅支援通用代碼</div></div>  
      <div><label>通用代碼</label><input id="c_generic" placeholder="例：SPRING2025（GENERIC 時必填）"></div>  
      <div><label>折扣型態</label><select id="c_discount_type"><option value="FIXED">FIXED</option><option value="PERCENT">PERCENT</option></select></div>  
      <div><label>折扣值</label><input id="c_value" type="number" placeholder="金額或百分比值，如 100 或 15"><div class="muted" style="font-size:12px;margin-top:4px">FIXED：折抵金額（元）；PERCENT：折抵百分比（0-100）</div></div>  
      <div><label>最大折扣</label><input id="c_max" type="number" placeholder="選填，僅百分比時常用"><div class="muted" style="font-size:12px;margin-top:4px">可留空；限制單筆折抵上限</div></div>  
      <div><label>最低消費</label><input id="c_min" type="number" placeholder="選填，門檻金額"><div class="muted" style="font-size:12px;margin-top:4px">可留空；達到門檻才可用</div></div>  
      <div><label>開始時間</label><input id="c_start" type="datetime-local" title="必填：活動開始時間"><div class="muted" style="font-size:12px;margin-top:4px">必填</div></div>  
      <div><label>結束時間</label><input id="c_end" type="datetime-local" title="必填：活動結束時間"><div class="muted" style="font-size:12px;margin-top:4px">必填</div></div>  
      <div><label>狀態</label><select id="c_status"><option value="DRAFT">DRAFT</option><option value="ACTIVE">ACTIVE</option><option value="PAUSED">PAUSED</option><option value="EXPIRED">EXPIRED</option></select></div>  
      <div><label>總量限制</label><input id="c_total" type="number" placeholder="選填，總發放上限"><div class="muted" style="font-size:12px;margin-top:4px">可留空；不限制總量</div></div>  
      <div><label>每用戶限制</label><input id="c_user" type="number" placeholder="選填，每人可用次數"><div class="muted" style="font-size:12px;margin-top:4px">可留空；不限制每人次數</div></div>  
    </div>  
    <div class="actions" style="margin-top:12px">  
      <button class="btn" onclick="submitCoupon()">送出</button>  
      <button class="btn secondary" onclick="closeCouponModal()">取消</button>  
      <span id="c_msg" class="muted"></span>  
    </div>  
  </div>`;
    try {
        const page = await apiGet(API.couponsAdmin + '?size=100');
        const tbody = document.getElementById('couponTbody');
        tbody.innerHTML = page.content.map(c => `<tr>  
      <td>${c.id}</td>  
      <td>${c.name}</td>  
      <td>${c.genericCode || ''}</td>  
      <td>${c.codeType}</td>  
      <td>${c.discountType} ${c.discountValue}</td>  
      <td>${c.status}</td>  
      <td>${c.startsAt || ''} ~ ${c.endsAt || ''}</td>  
      <td class="inline">  
        <button class="btn small" onclick="editCoupon(${c.id})">編輯</button>  
        <button class="btn small danger" onclick="deleteCoupon(${c.id})">刪除</button>  
      </td>  
    </tr>`).join('');
    } catch (e) {
        root.innerHTML += `<div class="muted">載入優惠券失敗：${e.message}</div>`;
    }
}

let editingCouponId = null;

function openCouponModal() {
    document.getElementById('couponModal').style.display = 'block';
    editingCouponId = null;
    clearCouponForm();
}

function closeCouponModal() {
    document.getElementById('couponModal').style.display = 'none';
}

function clearCouponForm() {
    ['c_name', 'c_generic', 'c_value', 'c_max', 'c_min', 'c_start', 'c_end', 'c_total', 'c_user'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('c_code_type').value = 'GENERIC';
    document.getElementById('c_discount_type').value = 'FIXED';
    document.getElementById('c_status').value = 'DRAFT';
}

async function editCoupon(id) {
    try {
        const page = await apiGet(API.couponsAdmin + `?size=1&page=0&id=${id}`); // 若無單筆查詢，使用已載入資料
        const item = (page.content || []).find(x => x.id === id) || null;
        if (!item) {
            alert('暫無讀取單筆API，請以列表資料為準');
            return;
        }
        editingCouponId = id;
        openCouponModal();
        document.getElementById('c_name').value = item.name || '';
        document.getElementById('c_code_type').value = item.codeType || 'GENERIC';
        document.getElementById('c_generic').value = item.genericCode || '';
        document.getElementById('c_discount_type').value = item.discountType || 'FIXED';
        document.getElementById('c_value').value = item.discountValue || 0;
        document.getElementById('c_max').value = item.maxDiscountAmount || '';
        document.getElementById('c_min').value = item.minSpendAmount || '';
        document.getElementById('c_start').value = toLocal(item.startsAt);
        document.getElementById('c_end').value = toLocal(item.endsAt);
        document.getElementById('c_status').value = item.status || 'DRAFT';
        document.getElementById('c_total').value = item.totalUsageLimit || '';
        document.getElementById('c_user').value = item.perUserLimit || '';
    } catch (e) {
        alert('讀取優惠券失敗：' + e.message);
    }
}

function toLocal(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function submitCoupon() {
    const bodyBase = {
        name: document.getElementById('c_name').value.trim(),
        discountType: document.getElementById('c_discount_type').value,
        discountValue: parseInt(document.getElementById('c_value').value || '0', 10),
        maxDiscountAmount: valOrNull(document.getElementById('c_max').value),
        minSpendAmount: valOrNull(document.getElementById('c_min').value),
        startsAt: new Date(document.getElementById('c_start').value).toISOString(),
        endsAt: new Date(document.getElementById('c_end').value).toISOString(),
        status: document.getElementById('c_status').value,
        totalUsageLimit: valOrNull(document.getElementById('c_total').value),
        perUserLimit: valOrNull(document.getElementById('c_user').value),
    };
    try {
        if (editingCouponId) {
            await apiJSON('PUT', `${API.couponsAdmin}/${editingCouponId}`, bodyBase);
        } else {
            const body = {
                ...bodyBase,
                codeType: document.getElementById('c_code_type').value,
                genericCode: document.getElementById('c_generic').value.trim(),
                createdBy: null
            };
            await apiJSON('POST', API.couponsAdmin, body);
        }
        document.getElementById('c_msg').textContent = '儲存成功';
        await renderCoupons();
        closeCouponModal();
    } catch (e) {
        document.getElementById('c_msg').textContent = '錯誤：' + e.message;
    }
}

async function deleteCoupon(id) {
    if (!confirm('確定刪除？')) return;
    try {
        await fetch(`${API.couponsAdmin}/${id}`, {method: 'DELETE', headers: {...authHeader()}});
        await renderCoupons();
    } catch (e) {
        alert('刪除失敗：' + e.message);
    }
}

// 訊息管理（Support）
async function renderSupport() {
    const root = document.getElementById('support');
    if (!root) return;
    root.innerHTML = `  
  <div class="card">  
    <h3 style="margin-top:0">工單列表</h3>  
    <div class="inline" style="margin-bottom:8px">  
      <select id="s_status"><option value="">全部</option><option>OPEN</option><option>IN_PROGRESS</option><option>RESOLVED</option><option>CLOSED</option></select>  
      <button class="btn" onclick="loadTickets()">載入</button>  
    </div>  
    <table><thead><tr><th>ID</th><th>主旨</th><th>狀態</th><th>建立</th><th>操作</th></tr></thead><tbody id="s_tbody"></tbody></table>  
  </div>  
  <div id="s_detail" class="card" style="margin-top:12px;display:none"></div>`;
}

async function loadTickets() {
    const status = document.getElementById('s_status').value;
    const url = status ? `${API.supportAdmin}/tickets?status=${status}` : `${API.supportAdmin}/tickets`;
    try {
        const list = await apiGet(url);
        const tbody = document.getElementById('s_tbody');
        tbody.innerHTML = list.map(t => `<tr>  
      <td>${t.id}</td><td>${t.subject}</td><td>${t.status}</td><td>${t.createdAt || ''}</td>  
      <td><button class="btn small" onclick="openTicket(${t.id})">檢視</button></td>  
    </tr>`).join('');
    } catch (e) {
        alert('載入工單失敗：' + e.message);
    }
}

async function openTicket(id) {
    try {
        const d = await apiGet(`${API.supportAdmin}/tickets/${id}`);
        const box = document.getElementById('s_detail');
        box.style.display = 'block';
        box.innerHTML = `  
      <h3 style="margin-top:0">工單 #${d.id} - ${d.subject}</h3>  
      <div class="muted">${d.contactName} (${d.contactEmail}) · 狀態：${d.status}</div>  
      <div style="margin-top:8px">  
        ${(d.messages || []).map(m => `<div style="padding:8px;border-bottom:1px solid #30363d"><b>${m.sender}</b> <span class="muted">${m.createdAt || ''}</span><div>${m.content}</div></div>`).join('')}  
      </div>  
      <div class="inline" style="margin-top:8px">  
        <input id="s_reply" placeholder="回覆內容">  
        <select id="s_next"><option value="">不變更</option><option>OPEN</option><option>IN_PROGRESS</option><option>RESOLVED</option><option>CLOSED</option></select>  
        <button class="btn" onclick="replyTicket(${d.id})">送出回覆</button>  
      </div>  
    `;
    } catch (e) {
        alert('讀取工單失敗：' + e.message);
    }
}

async function replyTicket(id) {
    const content = document.getElementById('s_reply').value.trim();
    const next = document.getElementById('s_next').value || null;
    if (!content) return;
    try {
        await apiJSON('POST', `${API.supportAdmin}/tickets/${id}/reply`, {content, nextStatus: next});
        await openTicket(id);
    } catch (e) {
        alert('回覆失敗：' + e.message);
    }
}

// 訂單管理（無API，僅表格骨架）
function renderOrders() {
    const root = document.getElementById('orders');
    if (!root) return;
    root.innerHTML = `  
    <div class="card">  
      <h3 style="margin-top:0">訂單管理（待串接API）</h3>  
      <table>        <thead><tr>  
          <th>ID</th><th>訂單編號</th><th>用戶ID</th><th>總金額</th><th>折扣</th><th>應付</th><th>狀態</th><th>付款</th><th>建立時間</th>  
        </tr></thead>  
        <tbody>          <tr><td colspan="9" class="muted">尚未提供API，待後續串接</td></tr>  
        </tbody>  
      </table>  
    </div>`;
}

// 管理操作記錄
async function renderAdminLogs() {
    const root = document.getElementById('admin_logs');
    if (!root) return;
    root.innerHTML = `  
  <div class="card">  
    <h3 style="margin-top:0">管理操作記錄</h3>  
    <div class="inline" style="margin-bottom:8px">  
      <input id="al_admin" type="number" placeholder="依 管理員ID 過濾（可空）">  
      <button class="btn" onclick="loadAdminLogs(0)">查詢</button>  
      <button class="btn secondary" onclick="openAdminLogModal()">新增記錄</button>  
    </div>  
    <table class="tbl-logs">  
      <thead><tr>  
        <th>編號</th><th>管理員ID</th><th>動作</th><th>對象</th><th>詳細</th><th>時間</th><th>操作</th>  
      </tr></thead>  
      <tbody id="al_tbody"></tbody>  
    </table>  
    <div class="inline" style="margin-top:8px">  
      <button class="btn small" id="al_prev" disabled>上一頁</button>  
      <span id="al_page" class="muted"></span>  
      <button class="btn small" id="al_next" disabled>下一頁</button>  
    </div>  
  </div>  
  <div id="al_modal" class="card" style="display:none;margin-top:12px">  
    <h3 style="margin-top:0">新增操作記錄</h3>  
    <div class="form-grid">  
      <div><label>管理員ID</label><input id="alm_admin" type="number"></div>  
      <div><label>動作</label><input id="alm_action"></div>  
      <div><label>目標ID</label><input id="alm_target_id" type="number"></div>  
      <div><label>對象類型</label><input id="alm_target_type"></div>  
      <div style="grid-column:1/-1"><label>詳細內容</label><textarea id="alm_details" rows="3"></textarea></div>  
    </div>  
    <div class="actions" style="margin-top:12px">  
      <button class="btn" onclick="submitAdminLog()">送出</button>  
      <button class="btn secondary" onclick="closeAdminLogModal()">取消</button>  
      <span id="alm_msg" class="muted"></span>  
    </div>  
  </div>`;

    // 綁定分頁按鈕
    document.getElementById('al_prev').onclick = () => changeAdminLogPage(-1);
    document.getElementById('al_next').onclick = () => changeAdminLogPage(1);

    // 初次載入
    await loadAdminLogs(0);
}

let adminLogPage = {number: 0, totalPages: 0};

async function loadAdminLogs(page) {
    const adminId = (document.getElementById('al_admin') && document.getElementById('al_admin').value) || '';
    const qs = `?page=${page || 0}&size=20&sort=createdAt,desc`;
    const url = adminId ? `${API.adminLogs}/by-admin/${adminId}${qs}` : `${API.adminLogs}${qs}`;
    try {
        const res = await apiGet(url);
        const tbody = document.getElementById('al_tbody');
        const content = res.content || [];
        tbody.innerHTML = content.map(x => {
            const target = `${escHtml(x.targetType || '')}#${escHtml(x.targetId || '')}`;
            const details = escHtml(x.details || '');
            const time = escHtml(x.createdAt || '');
            return `<tr>  
      <td>${x.id}</td>  
      <td>${x.adminId}</td>  
      <td>${escHtml(x.action)}</td>  
      <td><span class="ellipsis" title="${target}">${target}</span></td>  
      <td><span class="ellipsis" title="${details}">${details}</span></td>
      <td class="nowrap">${time}</td>  
      <td class="inline">  
        <button class="btn small danger" onclick="deleteAdminLog(${x.id})">刪除</button>  
      </td>  
    </tr>`;
        }).join('');
        adminLogPage = {number: res.number, totalPages: res.totalPages};
        document.getElementById('al_page').textContent = `第 ${res.number + 1} / ${Math.max(res.totalPages, 1)} 頁`;
        document.getElementById('al_prev').disabled = res.number <= 0;
        document.getElementById('al_next').disabled = res.number >= res.totalPages - 1;
    } catch (e) {
        alert('載入操作記錄失敗：' + e.message);
    }
}

function changeAdminLogPage(delta) {
    const next = Math.max(0, Math.min((adminLogPage.number || 0) + delta, Math.max((adminLogPage.totalPages || 1) - 1, 0)));
    loadAdminLogs(next);
}

function openAdminLogModal() {
    const m = document.getElementById('al_modal');
    if (m) m.style.display = 'block';
}

function closeAdminLogModal() {
    const m = document.getElementById('al_modal');
    if (m) m.style.display = 'none';
}

async function submitAdminLog() {
    const body = {
        adminId: valOrNull(document.getElementById('alm_admin').value),
        action: document.getElementById('alm_action').value.trim(),
        targetId: valOrNull(document.getElementById('alm_target_id').value),
        targetType: document.getElementById('alm_target_type').value.trim() || null,
        details: document.getElementById('alm_details').value || null
    };
    try {
        await apiJSON('POST', API.adminLogs, body);
        document.getElementById('alm_msg').textContent = '新增成功';
        await loadAdminLogs(0);
        closeAdminLogModal();
    } catch (e) {
        document.getElementById('alm_msg').textContent = '錯誤：' + e.message;
    }
}

async function deleteAdminLog(id) {
    if (!confirm('確定刪除此記錄？')) return;
    try {
        await fetch(`${API.adminLogs}/${id}`, {method: 'DELETE', headers: {...authHeader()}});
        await loadAdminLogs(0);
    } catch (e) {
        alert('刪除失敗：' + e.message);
    }
}