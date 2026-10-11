/* =============================================
   EcoTrack — Frontend Application Logic
   ============================================= */

const API_BASE = 'http://localhost:8080';
let authToken = localStorage.getItem('ecotrack_token');
let currentUser = JSON.parse(localStorage.getItem('ecotrack_user') || 'null');
let currentPage = 1;

// =============================================
// App Initialization
// =============================================
document.addEventListener('DOMContentLoaded', () => {
    setDefaultDate();
    updateGreeting();

    if (authToken && currentUser) {
        showApp();
        loadDashboard();
    } else {
        showAuth();
    }
});

function setDefaultDate() {
    const dateInput = document.getElementById('recordDate');
    if (dateInput) {
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        dateInput.value = now.toISOString().slice(0, 16);
    }
}

function updateGreeting() {
    const hour = new Date().getHours();
    let greeting = 'Good evening';
    if (hour < 12) greeting = 'Good morning';
    else if (hour < 17) greeting = 'Good afternoon';

    const el = document.getElementById('greetingText');
    if (el) {
        el.textContent = currentUser ? `${greeting}, ${currentUser.displayName}` : greeting;
    }
}

// =============================================
// Auth Handlers
// =============================================
function showAuthTab(tab) {
    document.getElementById('loginForm').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('registerForm').style.display = tab === 'register' ? 'block' : 'none';
    document.getElementById('loginTab').classList.toggle('active', tab === 'login');
    document.getElementById('registerTab').classList.toggle('active', tab === 'register');
    document.getElementById('loginError').textContent = '';
    document.getElementById('registerError').textContent = '';
}

async function handleLogin(e) {
    e.preventDefault();
    const btn = document.getElementById('loginBtn');
    const errorEl = document.getElementById('loginError');
    errorEl.textContent = '';

    setButtonLoading(btn, true);

    try {
        const res = await apiCall('/api/v1/auth/login', 'POST', {
            email: document.getElementById('loginEmail').value,
            password: document.getElementById('loginPassword').value
        });

        authToken = res.token;
        currentUser = res.user;
        localStorage.setItem('ecotrack_token', authToken);
        localStorage.setItem('ecotrack_user', JSON.stringify(currentUser));

        showApp();
        loadDashboard();
        showToast('Welcome back! 🌿', 'success');
    } catch (err) {
        errorEl.textContent = err.message || 'Login failed';
    } finally {
        setButtonLoading(btn, false);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const btn = document.getElementById('regBtn');
    const errorEl = document.getElementById('registerError');
    errorEl.textContent = '';

    setButtonLoading(btn, true);

    try {
        const res = await apiCall('/api/v1/auth/register', 'POST', {
            email: document.getElementById('regEmail').value,
            password: document.getElementById('regPassword').value,
            displayName: document.getElementById('regName').value
        });

        authToken = res.token;
        currentUser = res.user;
        localStorage.setItem('ecotrack_token', authToken);
        localStorage.setItem('ecotrack_user', JSON.stringify(currentUser));

        showApp();
        loadDashboard();
        showToast('Account created! Welcome to EcoTrack 🌍', 'success');
    } catch (err) {
        errorEl.textContent = err.message || 'Registration failed';
    } finally {
        setButtonLoading(btn, false);
    }
}

function handleLogout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('ecotrack_token');
    localStorage.removeItem('ecotrack_user');
    showAuth();
    showToast('Signed out', 'info');
}

function showAuth() {
    document.getElementById('authModal').classList.add('active');
    document.getElementById('app').style.display = 'none';
}

function showApp() {
    document.getElementById('authModal').classList.remove('active');
    document.getElementById('app').style.display = 'flex';
    updateGreeting();
    updateUserAvatars();
}

function updateUserAvatars() {
    if (!currentUser) return;
    const initial = currentUser.displayName.charAt(0).toUpperCase();
    document.getElementById('userAvatar').textContent = initial;
    const profileAvatar = document.getElementById('profileAvatar');
    if (profileAvatar) profileAvatar.textContent = initial;
}

// =============================================
// View Navigation
// =============================================
function showView(view) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const viewEl = document.getElementById(view + 'View');
    const navEl = document.querySelector(`[data-view="${view}"]`);

    if (viewEl) viewEl.classList.add('active');
    if (navEl) navEl.classList.add('active');

    // Load data for views
    if (view === 'dashboard') loadDashboard();
    if (view === 'records') loadRecords();
    if (view === 'profile') loadProfile();
    if (view === 'add') setDefaultDate();
    if (view === 'wasteReports') loadWasteReports();
    if (view === 'scanner') initScannerView();
    if (view === 'pickups') loadLogisticsView();
    if (view === 'marketplace') loadMarketplaceView();

    // Close mobile sidebar
    document.getElementById('sidebar').classList.remove('open');
}

function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('open');
}

// =============================================
// Dashboard
// =============================================
async function loadDashboard() {
    try {
        const [summary, records] = await Promise.all([
            apiCall('/api/v1/summary'),
            apiCall('/api/v1/records?pageSize=5')
        ]);

        // Update stats
        document.getElementById('totalRecords').textContent = summary.totalRecords || 0;

        const totalCarbon = summary.totalCarbonImpact || 0;
        const positiveCarbon = Object.values(summary.byType || {})
            .reduce((sum, t) => sum + Math.max(0, t.totalCarbonImpact), 0);
        const negativeCarbonAbs = Math.abs(Object.values(summary.byType || {})
            .reduce((sum, t) => sum + Math.min(0, t.totalCarbonImpact), 0));

        document.getElementById('totalCarbon').textContent = formatCarbon(positiveCarbon);
        document.getElementById('carbonSaved').textContent = formatCarbon(negativeCarbonAbs);
        document.getElementById('activeTypes').textContent = Object.keys(summary.byType || {}).length;

        // Update charts
        renderCategoryChart(summary.byType || {});
        renderEcoScore(summary);

        // Update recent records
        renderRecordsList('recentRecords', records.data || [], true);

    } catch (err) {
        console.error('Dashboard load failed:', err);
        if (err.status === 401) {
            handleLogout();
        }
    }
}

function formatCarbon(value) {
    if (value >= 1000) return (value / 1000).toFixed(1) + ' t';
    return value.toFixed(1) + ' kg';
}

// =============================================
// Category Chart
// =============================================
function renderCategoryChart(byType) {
    const container = document.getElementById('categoryChart');
    if (Object.keys(byType).length === 0) {
        container.innerHTML = '<div class="chart-placeholder">Add records to see your impact breakdown</div>';
        return;
    }

    const maxCount = Math.max(...Object.values(byType).map(v => v.count), 1);
    const typeEmojis = {
        carbon_footprint: '🏭', energy_usage: '⚡', water_usage: '💧',
        waste_recycled: '♻️', transport: '🚗', food_consumption: '🍽️',
        tree_planting: '🌳', other: '📋'
    };

    const typeLabels = {
        carbon_footprint: 'Carbon', energy_usage: 'Energy', water_usage: 'Water',
        waste_recycled: 'Recycled', transport: 'Transport', food_consumption: 'Food',
        tree_planting: 'Trees', other: 'Other'
    };

    container.innerHTML = Object.entries(byType)
        .sort((a, b) => b[1].count - a[1].count)
        .map(([type, data]) => {
            const pct = (data.count / maxCount) * 100;
            return `
                <div class="chart-bar-group">
                    <span class="chart-bar-label">${typeEmojis[type] || '📋'} ${typeLabels[type] || type}</span>
                    <div class="chart-bar-track">
                        <div class="chart-bar-fill bar-${type}" style="width: ${Math.max(pct, 8)}%">
                            ${data.count}
                        </div>
                    </div>
                </div>
            `;
        }).join('');
}

// =============================================
// Eco Score
// =============================================
function renderEcoScore(summary) {
    const totalRecords = summary.totalRecords || 0;
    const types = Object.keys(summary.byType || {}).length;
    const totalCarbon = summary.totalCarbonImpact || 0;

    // Score formula: diversity of tracking + volume + positive impact
    let score = 0;
    score += Math.min(totalRecords * 2, 30);       // Up to 30 for activity
    score += Math.min(types * 8, 40);               // Up to 40 for diversity
    score += totalCarbon < 0 ? Math.min(Math.abs(totalCarbon), 30) : 0; // Up to 30 for net positive

    score = Math.min(Math.round(score), 100);

    const ring = document.getElementById('scoreRing');
    const circumference = 2 * Math.PI * 85; // ~534
    const offset = circumference - (score / 100) * circumference;
    ring.style.strokeDashoffset = offset;

    document.getElementById('ecoScore').textContent = score;

    const labelEl = document.getElementById('scoreLabel');
    if (score === 0) labelEl.textContent = 'Start tracking to build your score';
    else if (score < 30) labelEl.textContent = 'Getting started — keep tracking! 🌱';
    else if (score < 60) labelEl.textContent = 'Great progress — diversify your actions 🌿';
    else if (score < 90) labelEl.textContent = 'Excellent environmental awareness! 🌍';
    else labelEl.textContent = 'Outstanding eco champion! 🏆';
}

// =============================================
// Records
// =============================================
async function loadRecords(page = 1) {
    currentPage = page;
    const type = document.getElementById('filterType')?.value || '';
    const listEl = document.getElementById('recordsList');
    listEl.innerHTML = '<div class="loading-spinner"><div class="spinner"></div></div>';

    try {
        const params = `?page=${page}&pageSize=15${type ? `&type=${type}` : ''}`;
        const data = await apiCall(`/api/v1/records${params}`);

        renderRecordsList('recordsList', data.data || [], false);
        renderPagination(data);
    } catch (err) {
        listEl.innerHTML = `<div class="empty-state"><div class="empty-icon">❌</div><h4>Failed to load records</h4><p>${err.message}</p></div>`;
    }
}

function renderRecordsList(containerId, records, isPreview) {
    const container = document.getElementById(containerId);

    if (records.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">🌱</div>
                <h4>No records yet</h4>
                <p>Start tracking your environmental activities</p>
                <button class="btn btn-primary" onclick="showView('add')">Add First Record</button>
            </div>
        `;
        return;
    }

    const typeEmojis = {
        carbon_footprint: '🏭', energy_usage: '⚡', water_usage: '💧',
        waste_recycled: '♻️', transport: '🚗', food_consumption: '🍽️',
        tree_planting: '🌳', other: '📋'
    };

    container.innerHTML = records.map(record => {
        const emoji = typeEmojis[record.recordType] || '📋';
        const impact = record.carbonImpact;
        const impactClass = impact < 0 ? 'negative' : 'positive';
        const impactSign = impact < 0 ? '' : '+';
        const date = new Date(record.occurredAt).toLocaleDateString('en-US', {
            month: 'short', day: 'numeric', year: 'numeric'
        });
        const quantity = record.quantity ? `${record.quantity} ${record.unit || ''}` : '';

        return `
            <div class="record-item">
                <div class="record-icon type-${record.recordType}">${emoji}</div>
                <div class="record-info">
                    <div class="record-title">${escapeHtml(record.title)}</div>
                    <div class="record-meta">
                        <span>${date}</span>
                        ${quantity ? `<span>${quantity}</span>` : ''}
                        ${record.notes ? `<span>${escapeHtml(record.notes.substring(0, 40))}${record.notes.length > 40 ? '...' : ''}</span>` : ''}
                    </div>
                </div>
                ${impact !== null && impact !== undefined ? `
                    <div class="record-impact">
                        <span class="impact-value ${impactClass}">${impactSign}${Math.abs(impact).toFixed(1)}</span>
                        <span class="impact-unit">kg CO₂</span>
                    </div>
                ` : ''}
                ${!isPreview ? `
                    <div class="record-actions">
                        <button onclick="deleteRecord('${record.id}')" title="Delete">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                        </button>
                    </div>
                ` : ''}
            </div>
        `;
    }).join('');
}

function renderPagination(data) {
    const container = document.getElementById('pagination');
    if (!data || data.totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = `<button ${data.page <= 1 ? 'disabled' : ''} onclick="loadRecords(${data.page - 1})">← Prev</button>`;

    for (let i = 1; i <= data.totalPages; i++) {
        if (i === data.page) {
            html += `<button class="active">${i}</button>`;
        } else if (Math.abs(i - data.page) <= 2 || i === 1 || i === data.totalPages) {
            html += `<button onclick="loadRecords(${i})">${i}</button>`;
        } else if (Math.abs(i - data.page) === 3) {
            html += `<button disabled>…</button>`;
        }
    }

    html += `<button ${data.page >= data.totalPages ? 'disabled' : ''} onclick="loadRecords(${data.page + 1})">Next →</button>`;
    container.innerHTML = html;
}

// =============================================
// Add Record
// =============================================
async function handleAddRecord(e) {
    e.preventDefault();
    const btn = document.getElementById('addRecordBtn');
    const errorEl = document.getElementById('addRecordError');
    const successEl = document.getElementById('addRecordSuccess');
    errorEl.textContent = '';
    successEl.textContent = '';

    setButtonLoading(btn, true);

    try {
        const dateValue = document.getElementById('recordDate').value;
        const occurredAt = new Date(dateValue).toISOString();
        const carbonImpact = document.getElementById('recordCarbonImpact').value;

        const body = {
            recordType: document.getElementById('recordType').value,
            title: document.getElementById('recordTitle').value,
            occurredAt: occurredAt,
        };

        const quantity = document.getElementById('recordQuantity').value;
        if (quantity) body.quantity = parseFloat(quantity);

        const unit = document.getElementById('recordUnit').value;
        if (unit) body.unit = unit;

        if (carbonImpact) body.carbonImpact = parseFloat(carbonImpact);

        const notes = document.getElementById('recordNotes').value;
        if (notes) body.notes = notes;

        await apiCall('/api/v1/records', 'POST', body);

        successEl.textContent = 'Record saved successfully!';
        showToast('Record saved! 🌿', 'success');

        // Reset form
        document.getElementById('addRecordForm').reset();
        setDefaultDate();

        // Switch to dashboard after delay
        setTimeout(() => {
            showView('dashboard');
            successEl.textContent = '';
        }, 1500);

    } catch (err) {
        errorEl.textContent = err.message || 'Failed to save record';
    } finally {
        setButtonLoading(btn, false);
    }
}

// =============================================
// Delete Record
// =============================================
async function deleteRecord(id) {
    if (!confirm('Delete this record? This action cannot be undone.')) return;

    try {
        await apiCall(`/api/v1/records/${id}`, 'DELETE');
        showToast('Record deleted', 'info');
        loadRecords(currentPage);
    } catch (err) {
        showToast('Failed to delete: ' + err.message, 'error');
    }
}

// =============================================
// Profile
// =============================================
function loadProfile() {
    if (!currentUser) return;
    document.getElementById('profileName').textContent = currentUser.displayName;
    document.getElementById('profileEmail').textContent = currentUser.email;
    document.getElementById('profileStatus').textContent = currentUser.status;
    document.getElementById('profileDisplayName').value = currentUser.displayName;
    const avatarEl = document.getElementById('profileAvatar');
    if (avatarEl) avatarEl.textContent = currentUser.displayName.charAt(0).toUpperCase();
}

async function handleUpdateProfile(e) {
    e.preventDefault();
    const errorEl = document.getElementById('profileError');
    const successEl = document.getElementById('profileSuccess');
    errorEl.textContent = '';
    successEl.textContent = '';

    try {
        const displayName = document.getElementById('profileDisplayName').value;
        const res = await apiCall('/api/v1/me', 'PATCH', { displayName });

        currentUser = res;
        localStorage.setItem('ecotrack_user', JSON.stringify(currentUser));
        updateGreeting();
        updateUserAvatars();
        loadProfile();

        successEl.textContent = 'Profile updated!';
        showToast('Profile updated ✅', 'success');
    } catch (err) {
        errorEl.textContent = err.message || 'Update failed';
    }
}

// =============================================
// API Utility
// =============================================
async function apiCall(path, method = 'GET', body = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    const config = { method, headers };
    if (body) config.body = JSON.stringify(body);

    const res = await fetch(`${API_BASE}${path}`, config);

    if (res.status === 204) return null;

    const data = await res.json().catch(() => null);

    if (!res.ok) {
        const error = new Error(
            data?.error?.message || data?.message || `Request failed (${res.status})`
        );
        error.status = res.status;
        error.details = data?.error?.details;
        throw error;
    }

    return data;
}

// =============================================
// Helpers
// =============================================
function setButtonLoading(btn, loading) {
    const span = btn.querySelector('span');
    const loader = btn.querySelector('.btn-loader');
    if (span) span.style.display = loading ? 'none' : 'inline';
    if (loader) loader.style.display = loading ? 'inline-block' : 'none';
    btn.disabled = loading;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// =============================================
// Phase 2: AI Waste Scanner & Reports
// =============================================
let currentScanResult = null;
let selectedImageBase64 = null;
let wasteReportsPage = 1;

function initScannerView() {
    const hintInput = document.getElementById('scannerHint');
    if (hintInput && !selectedImageBase64 && !hintInput.value) {
        selectPreset('plastic', 'Plastic PET water bottle with cap');
    }
}

function handleImageSelected(event) {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
        showToast('Please select a valid image file', 'error');
        return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
        selectedImageBase64 = e.target.result;
        const img = document.getElementById('imagePreview');
        img.src = selectedImageBase64;
        document.getElementById('dropzonePrompt').style.display = 'none';
        document.getElementById('imagePreviewContainer').style.display = 'block';

        const hint = document.getElementById('scannerHint');
        if (hint && !hint.value) {
            hint.value = file.name.replace(/\.[^/.]+$/, '').replace(/[-_]/g, ' ');
        }
    };
    reader.readAsDataURL(file);
}

function clearSelectedImage() {
    selectedImageBase64 = null;
    const fileInput = document.getElementById('wasteImageInput');
    if (fileInput) fileInput.value = '';
    document.getElementById('dropzonePrompt').style.display = 'flex';
    document.getElementById('imagePreviewContainer').style.display = 'none';
    document.getElementById('imagePreview').src = '';
}

function selectPreset(category, hintText) {
    document.querySelectorAll('.preset-pill').forEach(p => p.classList.remove('active'));
    event?.target?.classList.add('active');

    const hint = document.getElementById('scannerHint');
    if (hint) hint.value = hintText;

    runWasteScan();
}

async function runWasteScan() {
    const btn = document.getElementById('scanBtn');
    const laser = document.getElementById('scanLaser');
    const hint = document.getElementById('scannerHint')?.value || '';

    setButtonLoading(btn, true);
    if (laser) laser.classList.add('scanning');

    try {
        const payload = {
            description: hint,
            imageBase64: selectedImageBase64 || null
        };

        const result = await apiCall('/api/v1/waste/identify', 'POST', payload);
        currentScanResult = result;

        // Populate Result Card
        document.getElementById('resultPlaceholder').style.display = 'none';
        document.getElementById('resultContent').style.display = 'block';

        const categoryBadge = document.getElementById('resCategoryBadge');
        categoryBadge.textContent = result.category.toUpperCase().replace('_', ' ');
        categoryBadge.className = `badge badge-category-${result.category}`;

        const recyclableBadge = document.getElementById('resRecyclableBadge');
        if (result.recyclable) {
            recyclableBadge.textContent = 'Recyclable';
            recyclableBadge.className = 'badge badge-success';
        } else {
            recyclableBadge.textContent = 'Non-Recyclable';
            recyclableBadge.className = 'badge badge-danger';
        }

        const confFormatted = result.confidence.toFixed(1) + '%';
        document.getElementById('resConfidenceVal').textContent = confFormatted;
        document.getElementById('meterPercentText').textContent = confFormatted;
        document.getElementById('resConfidenceBar').style.width = confFormatted;

        document.getElementById('resSubType').textContent = result.subType;
        document.getElementById('resCarbonFactor').textContent = `${result.carbonFactorKg.toFixed(2)} kg CO₂e / kg`;
        document.getElementById('resRecommendedAction').textContent = result.recommendedAction;
        document.getElementById('resDisposalGuide').textContent = result.disposalGuide;

        const unitSelect = document.getElementById('reportUnit');
        if (unitSelect && result.acceptedUnits && result.acceptedUnits.length > 0) {
            unitSelect.value = result.acceptedUnits[0];
        }

        showToast(`Classified as ${result.category.toUpperCase()} (${confFormatted}) 🤖`, 'success');
    } catch (err) {
        console.error('Scan failed:', err);
        showToast(err.message || 'Scan failed', 'error');
    } finally {
        setButtonLoading(btn, false);
        if (laser) laser.classList.remove('scanning');
    }
}

async function handleSaveReport(e) {
    e.preventDefault();
    if (!currentScanResult) {
        showToast('Please run an AI scan first', 'error');
        return;
    }

    const btn = document.getElementById('saveReportBtn');
    setButtonLoading(btn, true);

    try {
        const qty = parseFloat(document.getElementById('reportQty').value) || 1.0;
        const unit = document.getElementById('reportUnit').value || 'kg';
        const carbonImpact = currentScanResult.carbonFactorKg * qty;

        const payload = {
            category: currentScanResult.category,
            subType: currentScanResult.subType,
            photoUrl: selectedImageBase64 ? 'uploaded_asset' : null,
            quantity: qty,
            unit: unit,
            confidence: currentScanResult.confidence,
            disposalGuide: currentScanResult.disposalGuide,
            carbonImpact: carbonImpact
        };

        await apiCall('/api/v1/waste/reports', 'POST', payload);
        showToast('Report saved to your profile! ♻️', 'success');

        setTimeout(() => {
            showView('wasteReports');
        }, 600);
    } catch (err) {
        console.error('Save report failed:', err);
        showToast(err.message || 'Could not save report', 'error');
    } finally {
        setButtonLoading(btn, false);
    }
}

async function loadWasteReports(page = 1) {
    wasteReportsPage = page;
    const listEl = document.getElementById('wasteReportsList');
    const filterCat = document.getElementById('filterWasteCategory')?.value || '';

    listEl.innerHTML = '<div class="loading-spinner"><div class="spinner"></div></div>';

    try {
        let url = `/api/v1/waste/reports?page=${page}&pageSize=10`;
        if (filterCat) url += `&category=${filterCat}`;

        const data = await apiCall(url);
        renderWasteReportsList(data.items || []);
        renderWasteReportsPagination(data.page, data.totalPages);
    } catch (err) {
        console.error('Load waste reports failed:', err);
        listEl.innerHTML = `<div class="empty-state"><h4>Error loading reports</h4><p>${err.message}</p></div>`;
    }
}

function renderWasteReportsList(items) {
    const listEl = document.getElementById('wasteReportsList');

    if (!items || items.length === 0) {
        listEl.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📦</div>
                <h4>No waste reports found</h4>
                <p>Run your first AI scan to log and categorize recyclable waste.</p>
                <button class="btn btn-primary" onclick="showView('scanner')">Open AI Scanner</button>
            </div>
        `;
        return;
    }

    listEl.innerHTML = items.map(report => `
        <div class="record-item">
            <div class="record-type-badge ${getCategoryBadgeClass(report.category)}">
                ${getCategoryEmoji(report.category)}
            </div>
            <div class="record-details">
                <div class="record-title-row">
                    <h4>${escapeHtml(report.subType || report.category)}</h4>
                    <span class="badge ${report.status === 'collected' ? 'badge-success' : 'badge-accent'}">
                        ${escapeHtml(report.status.toUpperCase())}
                    </span>
                </div>
                <div class="record-meta">
                    <span>${report.quantity ? `${report.quantity} ${report.unit || 'kg'}` : '—'}</span>
                    <span>•</span>
                    <span>${report.confidence ? `${report.confidence.toFixed(1)}% AI confidence` : ''}</span>
                    <span>•</span>
                    <span>${formatDate(report.createdAt)}</span>
                </div>
                ${report.disposalGuide ? `<p class="record-notes" style="margin-top:4px;">💡 ${escapeHtml(report.disposalGuide)}</p>` : ''}
            </div>
            <div class="record-impact">
                <span class="carbon-tag positive">
                    -${(report.carbonImpact || 0).toFixed(2)} kg CO₂
                </span>
            </div>
        </div>
    `).join('');
}

function getCategoryEmoji(category) {
    switch(category) {
        case 'plastic': return '🥤';
        case 'paper': return '📦';
        case 'metal': return '🥫';
        case 'e_waste': return '📱';
        case 'glass': return '🍾';
        case 'organic': return '🍌';
        case 'hazardous': return '☣️';
        default: return '♻️';
    }
}

function getCategoryBadgeClass(category) {
    return `badge-category-${category}`;
}

function renderWasteReportsPagination(currentPage, totalPages) {
    const container = document.getElementById('wasteReportsPagination');
    if (!container) return;

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = `
        <button class="btn btn-sm btn-ghost" onclick="loadWasteReports(${currentPage - 1})" ${currentPage <= 1 ? 'disabled' : ''}>← Previous</button>
        <span class="page-indicator">Page ${currentPage} of ${totalPages}</span>
        <button class="btn btn-sm btn-ghost" onclick="loadWasteReports(${currentPage + 1})" ${currentPage >= totalPages ? 'disabled' : ''}>Next →</button>
    `;
    container.innerHTML = html;
}

// =============================================
// Phase 3: Recyclers & On-Demand Logistics
// =============================================
let currentFacilitiesList = [];
let currentMaterialFilter = '';
let currentGpsCoords = { lat: 40.7128, lng: -74.0060 }; // Default to Metropolis downtown

async function loadLogisticsView() {
    // Set default pickup date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateInput = document.getElementById('pickupDate');
    if (dateInput) {
        dateInput.value = tomorrow.toISOString().split('T')[0];
        dateInput.min = new Date().toISOString().split('T')[0];
    }

    await Promise.all([
        loadFacilities(currentMaterialFilter),
        loadMyCollections()
    ]);
}

function switchLogisticsTab(tab) {
    document.querySelectorAll('.logistics-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.logistics-pane').forEach(p => p.style.display = 'none');

    if (tab === 'facilities') {
        document.getElementById('tabFacilities').classList.add('active');
        document.getElementById('facilitiesPane').style.display = 'block';
    } else {
        document.getElementById('tabCollections').classList.add('active');
        document.getElementById('collectionsPane').style.display = 'block';
    }
}

async function loadFacilities(material = '') {
    const grid = document.getElementById('facilitiesGrid');
    if (!grid) return;
    grid.innerHTML = '<div class="loading-spinner"><div class="spinner"></div></div>';

    try {
        let url = `/api/v1/facilities/nearby?lat=${currentGpsCoords.lat}&lng=${currentGpsCoords.lng}&radius=30`;
        if (material) url += `&material=${encodeURIComponent(material)}`;

        const data = await apiCall(url, 'GET');
        currentFacilitiesList = data || [];

        const countEl = document.getElementById('facilitiesCount');
        if (countEl) countEl.textContent = currentFacilitiesList.length;

        // Populate modal select dropdown
        populateFacilitySelect();

        renderFacilities(currentFacilitiesList);
    } catch (err) {
        grid.innerHTML = `
            <div class="empty-state">
                <p>Unable to load recycling facilities: ${err.message}</p>
                <button class="btn btn-secondary btn-sm" onclick="loadFacilities('${material}')">Retry</button>
            </div>
        `;
    }
}

function filterFacilitiesByMaterial(mat) {
    currentMaterialFilter = mat;
    document.querySelectorAll('#materialFilterChips .chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-mat') === mat);
    });
    loadFacilities(mat);
}

function fetchNearbyFacilitiesWithGps() {
    if (navigator.geolocation) {
        showToast('Acquiring GPS coordinates... 🛰️', 'info');
        navigator.geolocation.getCurrentPosition(
            pos => {
                currentGpsCoords = {
                    lat: pos.coords.latitude,
                    lng: pos.coords.longitude
                };
                showToast('Location updated! Searching nearby hubs...', 'success');
                loadFacilities(currentMaterialFilter);
            },
            err => {
                showToast('Using default Metropolis coordinates.', 'info');
                loadFacilities(currentMaterialFilter);
            },
            { timeout: 5000 }
        );
    } else {
        showToast('Geolocation not supported by browser. Using default city map.', 'info');
        loadFacilities(currentMaterialFilter);
    }
}

function renderFacilities(list) {
    const grid = document.getElementById('facilitiesGrid');
    if (!grid) return;

    if (!list || list.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <p>No certified recyclers found for the selected material filter.</p>
                <button class="btn btn-secondary btn-sm" onclick="filterFacilitiesByMaterial('')">Show All Facilities</button>
            </div>
        `;
        return;
    }

    grid.innerHTML = list.map(f => {
        const distanceStr = f.distanceKm != null ? `${f.distanceKm} km away` : 'Nearby';
        const typeLabel = (f.facilityType || 'center').replace(/_/g, ' ');
        const materialsHtml = (f.acceptedMaterials || []).map(m => `<span class="mat-tag">${m}</span>`).join('');

        return `
            <div class="facility-card">
                <div>
                    <div class="facility-top">
                        <div>
                            <div class="facility-name">${escapeHtml(f.name)}</div>
                            <div class="facility-type">${escapeHtml(typeLabel)}</div>
                        </div>
                        <span class="facility-distance-badge">📍 ${distanceStr}</span>
                    </div>

                    <div class="facility-address">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                        <span>${escapeHtml(f.address)}</span>
                    </div>

                    ${f.pricingInfo ? `<div class="facility-pricing">💰 ${escapeHtml(f.pricingInfo)}</div>` : ''}

                    <div class="facility-materials">
                        ${materialsHtml}
                    </div>
                </div>

                <div class="facility-footer">
                    <div class="facility-rating">
                        <span>★</span>
                        <span>${f.rating || 4.8}</span>
                        ${f.verified ? '<span title="Verified Recycler">✓</span>' : ''}
                    </div>
                    <button class="btn btn-primary btn-sm" onclick="openScheduleModal('${f.id}')">
                        Request Pickup
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

async function loadMyCollections() {
    const listEl = document.getElementById('collectionsList');
    if (!listEl) return;
    listEl.innerHTML = '<div class="loading-spinner"><div class="spinner"></div></div>';

    try {
        const collections = await apiCall('/api/v1/collections', 'GET');
        const countEl = document.getElementById('collectionsCount');
        if (countEl) countEl.textContent = (collections || []).length;

        renderCollections(collections || []);
    } catch (err) {
        listEl.innerHTML = `
            <div class="empty-state">
                <p>Failed to load pickup requests: ${err.message}</p>
            </div>
        `;
    }
}

function renderCollections(list) {
    const listEl = document.getElementById('collectionsList');
    if (!listEl) return;

    if (!list || list.length === 0) {
        listEl.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="margin-bottom: 1rem; color: var(--text-muted);"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                <h3>No Scheduled Pickups</h3>
                <p>Book a door-to-door collector to pickup recyclables and earn rewards!</p>
                <button class="btn btn-primary btn-sm" onclick="openScheduleModal()" style="margin-top: 1rem;">Schedule First Pickup</button>
            </div>
        `;
        return;
    }

    listEl.innerHTML = list.map(c => {
        const status = c.status || 'submitted';
        const isDoneSubmitted = true;
        const isDoneAssigned = ['assigned', 'in_transit', 'completed'].includes(status);
        const isDoneTransit = ['in_transit', 'completed'].includes(status);
        const isDoneCompleted = status === 'completed';

        return `
            <div class="collection-card">
                <div class="collection-header">
                    <div>
                        <span class="collection-slot">📅 ${c.preferredDate} (${c.preferredSlot})</span>
                        <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 2px;">
                            📍 ${escapeHtml(c.pickupAddress)}
                        </div>
                    </div>
                    <span class="collection-status-badge status-${status}">${status.replace(/_/g, ' ')}</span>
                </div>

                <!-- Live Logistics Stepper -->
                <div class="status-stepper">
                    <span class="step-node ${isDoneSubmitted ? 'done' : ''}">1. Submitted</span>
                    <div class="step-line ${isDoneAssigned ? 'done' : ''}"></div>
                    <span class="step-node ${isDoneAssigned ? 'done' : ''}">2. Collector Assigned</span>
                    <div class="step-line ${isDoneTransit ? 'done' : ''}"></div>
                    <span class="step-node ${isDoneTransit ? 'done' : ''}">3. In Transit</span>
                    <div class="step-line ${isDoneCompleted ? 'done' : ''}"></div>
                    <span class="step-node ${isDoneCompleted ? 'done' : ''}">4. Completed</span>
                </div>

                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 0.75rem; font-size: 0.85rem;">
                    <div>
                        <strong style="color: var(--text-secondary);">Materials:</strong>
                        <span style="color: var(--text-primary); text-transform: capitalize;">${(c.materials || []).join(', ')}</span>
                    </div>
                    <div>
                        <span style="color: var(--accent-green); font-weight: 700;">+${c.ecoPointsEarned || 50} Eco Points</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function populateFacilitySelect() {
    const select = document.getElementById('pickupFacilitySelect');
    if (!select) return;

    let html = '<option value="">Auto-Dispatch Nearest Recycler</option>';
    currentFacilitiesList.forEach(f => {
        html += `<option value="${f.id}">${escapeHtml(f.name)} (${f.facilityType.replace(/_/g, ' ')})</option>`;
    });
    select.innerHTML = html;
}

function openScheduleModal(presetFacilityId = null) {
    const modal = document.getElementById('schedulePickupModal');
    if (!modal) return;

    if (presetFacilityId) {
        const select = document.getElementById('pickupFacilitySelect');
        if (select) select.value = presetFacilityId;
    }

    modal.classList.add('active');
}

function closeScheduleModal() {
    const modal = document.getElementById('schedulePickupModal');
    if (modal) modal.classList.remove('active');
}

async function handleSchedulePickupSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('scheduleSubmitBtn');
    const loader = document.getElementById('scheduleLoader');

    const address = document.getElementById('pickupAddress').value.trim();
    const date = document.getElementById('pickupDate').value;
    const slot = document.getElementById('pickupSlot').value;
    const weight = parseFloat(document.getElementById('pickupEstimatedWeight').value) || 5.0;
    const facilityId = document.getElementById('pickupFacilitySelect').value || null;
    const instructions = document.getElementById('pickupInstructions').value.trim();

    const checkedMaterials = Array.from(document.querySelectorAll('input[name="pickupMaterials"]:checked')).map(cb => cb.value);

    if (checkedMaterials.length === 0) {
        showToast('Please check at least one material category.', 'error');
        return;
    }

    if (btn) btn.disabled = true;
    if (loader) loader.style.display = 'inline-block';

    try {
        const payload = {
            pickupAddress: address,
            preferredDate: date,
            preferredSlot: slot,
            materials: checkedMaterials,
            estimatedWeightKg: weight,
            specialInstructions: instructions || null,
            facilityId: facilityId
        };

        const result = await apiCall('/api/v1/collections', 'POST', payload);
        showToast('Pickup successfully scheduled! 🚚', 'success');
        closeScheduleModal();

        // Switch to collections tab and reload
        switchLogisticsTab('collections');
        loadMyCollections();
    } catch (err) {
        showToast(err.message || 'Failed to schedule pickup', 'error');
    } finally {
        if (btn) btn.disabled = false;
        if (loader) loader.style.display = 'none';
    }
}

// =============================================
// Phase 4: Circular Marketplace Logic
// =============================================
let currentMarketplaceList = [];
let currentMarketMaterialFilter = '';
let activeBiddingListing = null;

async function loadMarketplaceView(material = '') {
    const grid = document.getElementById('marketplaceGrid');
    if (!grid) return;
    grid.innerHTML = '<div class="loading-spinner"><div class="spinner"></div></div>';

    try {
        let url = '/api/v1/marketplace/listings';
        if (material) url += `?material=${encodeURIComponent(material)}`;

        const [listings, stats] = await Promise.all([
            apiCall(url, 'GET'),
            apiCall('/api/v1/marketplace/stats', 'GET').catch(() => null)
        ]);

        currentMarketplaceList = listings || [];

        // Update stats ticker if available
        if (stats) {
            const activeEl = document.getElementById('mActiveCount');
            const volAvailEl = document.getElementById('mVolumeAvailable');
            const volTradedEl = document.getElementById('mVolumeTraded');
            const valTradedEl = document.getElementById('mValueTraded');

            if (activeEl) activeEl.textContent = `${stats.activeListingsCount} Lots`;
            if (volAvailEl) volAvailEl.textContent = `${stats.totalMaterialsAvailableKg.toLocaleString()} kg`;
            if (volTradedEl) volTradedEl.textContent = `${stats.totalVolumeTradedKg.toLocaleString()} kg`;
            if (valTradedEl) valTradedEl.textContent = `$${stats.totalValueTraded.toLocaleString(undefined, {minimumFractionDigits: 2})}`;
        }

        renderMarketplaceListings(currentMarketplaceList);
    } catch (err) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <p>Unable to load marketplace listings: ${err.message}</p>
                <button class="btn btn-secondary btn-sm" onclick="loadMarketplaceView('${material}')">Retry</button>
            </div>
        `;
    }
}

function filterMarketplaceByMaterial(mat) {
    currentMarketMaterialFilter = mat;
    document.querySelectorAll('#marketFilterChips .chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-mat') === mat);
    });
    loadMarketplaceView(mat);
}

function renderMarketplaceListings(list) {
    const grid = document.getElementById('marketplaceGrid');
    if (!grid) return;

    if (!list || list.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="margin-bottom: 1rem; color: var(--text-muted);"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
                <h3>No Recyclable Lots Available</h3>
                <p>No active material listings match the current filter criteria.</p>
                <button class="btn btn-primary btn-sm" onclick="openCreateListingModal()" style="margin-top: 1rem;">Post First Lot</button>
            </div>
        `;
        return;
    }

    grid.innerHTML = list.map(item => {
        const mat = item.materialType || 'plastic';
        const pricePerKg = item.askingPricePerKg != null ? `$${item.askingPricePerKg.toFixed(2)}/kg` : 'Offer Based';
        const totalPrice = item.totalAskingPrice != null ? `$${item.totalAskingPrice.toFixed(2)}` : '-';
        const badgeClass = `badge-category-${mat}`;

        return `
            <div class="market-card">
                <div>
                    <div class="market-card-top">
                        <span class="market-tag ${badgeClass}">${escapeHtml(mat)}</span>
                        <span class="market-price-tag">${pricePerKg}</span>
                    </div>

                    <div class="market-title">${escapeHtml(item.title)}</div>

                    ${item.description ? `<p class="market-desc">${escapeHtml(item.description)}</p>` : ''}

                    <div class="market-location">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="13" height="13"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                        <span>${escapeHtml(item.pickupAddress)}</span>
                    </div>
                </div>

                <div class="market-footer">
                    <div class="market-totals">
                        <span class="market-total-price">${totalPrice}</span>
                        <span class="market-volume-bids">${item.quantityKg} kg • ${item.offerCount || 0} bids</span>
                    </div>
                    <button class="btn btn-primary btn-sm" onclick="openPlaceOfferModal('${item.id}')">
                        Place Bid
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function openCreateListingModal() {
    const modal = document.getElementById('createListingModal');
    if (modal) modal.classList.add('active');
}

function closeCreateListingModal() {
    const modal = document.getElementById('createListingModal');
    if (modal) modal.classList.remove('active');
}

async function handleCreateListingSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('createListingBtn');
    const loader = document.getElementById('createListingLoader');

    const title = document.getElementById('listingTitle').value.trim();
    const material = document.getElementById('listingMaterial').value;
    const subType = document.getElementById('listingSubType').value.trim();
    const qty = parseFloat(document.getElementById('listingQuantity').value) || 100;
    const pricePerKg = parseFloat(document.getElementById('listingPricePerKg').value) || 0.30;
    const address = document.getElementById('listingAddress').value.trim();
    const desc = document.getElementById('listingDesc').value.trim();

    if (btn) btn.disabled = true;
    if (loader) loader.style.display = 'inline-block';

    try {
        const payload = {
            title: title,
            materialType: material,
            subType: subType || null,
            quantityKg: qty,
            askingPricePerKg: pricePerKg,
            totalAskingPrice: Math.round(qty * pricePerKg * 100) / 100,
            pickupAddress: address,
            description: desc || null
        };

        await apiCall('/api/v1/marketplace/listings', 'POST', payload);
        showToast('Lot successfully listed on Circular Marketplace! 📦', 'success');
        closeCreateListingModal();
        loadMarketplaceView(currentMarketMaterialFilter);
    } catch (err) {
        showToast(err.message || 'Failed to publish listing', 'error');
    } finally {
        if (btn) btn.disabled = false;
        if (loader) loader.style.display = 'none';
    }
}

function openPlaceOfferModal(listingId) {
    const listing = currentMarketplaceList.find(l => l.id === listingId);
    if (!listing) return;
    activeBiddingListing = listing;

    document.getElementById('bidListingId').value = listing.id;
    document.getElementById('bidListingTitle').textContent = listing.title;
    document.getElementById('bidListingInfo').textContent = `${listing.quantityKg} kg batch • Asking: $${listing.askingPricePerKg || 0.30}/kg (Total: $${listing.totalAskingPrice})`;

    const priceInput = document.getElementById('bidPricePerKg');
    priceInput.value = listing.askingPricePerKg || 0.30;
    calculateBidTotal();

    const modal = document.getElementById('placeOfferModal');
    if (modal) modal.classList.add('active');
}

function closePlaceOfferModal() {
    const modal = document.getElementById('placeOfferModal');
    if (modal) modal.classList.remove('active');
    activeBiddingListing = null;
}

function calculateBidTotal() {
    if (!activeBiddingListing) return;
    const p = parseFloat(document.getElementById('bidPricePerKg').value) || 0;
    const total = Math.round(p * activeBiddingListing.quantityKg * 100) / 100;
    document.getElementById('bidTotalAmount').value = total.toFixed(2);
}

async function handlePlaceOfferSubmit(e) {
    e.preventDefault();
    if (!activeBiddingListing) return;

    const btn = document.getElementById('placeOfferBtn');
    const loader = document.getElementById('placeOfferLoader');
    const p = parseFloat(document.getElementById('bidPricePerKg').value);
    const total = parseFloat(document.getElementById('bidTotalAmount').value);
    const msg = document.getElementById('bidMessage').value.trim();

    if (!p || p <= 0) {
        showToast('Please enter a valid offer price per kg.', 'error');
        return;
    }

    if (btn) btn.disabled = true;
    if (loader) loader.style.display = 'inline-block';

    try {
        const payload = {
            offeredPricePerKg: p,
            totalOfferedPrice: total,
            message: msg || null
        };

        await apiCall(`/api/v1/marketplace/listings/${activeBiddingListing.id}/offers`, 'POST', payload);
        showToast('Purchase bid successfully submitted to seller! 🤝', 'success');
        closePlaceOfferModal();
        loadMarketplaceView(currentMarketMaterialFilter);
    } catch (err) {
        showToast(err.message || 'Failed to submit bid', 'error');
    } finally {
        if (btn) btn.disabled = false;
        if (loader) loader.style.display = 'none';
    }
}


