// ========== Router ==========
const routes = {
    '/': 'list',
    '/upload': 'upload',
    '/chat': 'chat'
};

function navigate(hash) {
    const path = hash.replace('#', '') || '/';
    const pageId = routes[path] || 'list';

    document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));
    document.getElementById('page-' + pageId).classList.remove('hidden');

    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.dataset.page === pageId);
    });

    if (pageId === 'list') {
        loadFileList();
    }
    if (pageId === 'chat') {
        initChatPage();
    }
}

window.addEventListener('hashchange', () => navigate(location.hash));

// ========== Init ==========
document.addEventListener('DOMContentLoaded', () => {
    navigate(location.hash);
    initUploadForm();
    initDropZone();
    initChat();

    document.getElementById('btn-refresh').addEventListener('click', () => loadFileList(currentPage));
    document.getElementById('btn-prev').addEventListener('click', () => loadFileList(currentPage - 1));
    document.getElementById('btn-next').addEventListener('click', () => loadFileList(currentPage + 1));
});

// ========== File List ==========
let currentPage = 0;
const PAGE_SIZE = 10;

async function loadFileList(page = 0) {
    const tbody = document.getElementById('file-table-body');
    const emptyMsg = document.getElementById('empty-msg');
    const table = document.querySelector('.table');
    const pagination = document.getElementById('pagination');

    try {
        const res = await fetch(`/api/files?page=${page}&size=${PAGE_SIZE}`);
        const data = await res.json();
        const files = data.content;

        if (data.totalElements === 0) {
            table.classList.add('hidden');
            pagination.classList.add('hidden');
            emptyMsg.classList.remove('hidden');
            return;
        }

        table.classList.remove('hidden');
        emptyMsg.classList.add('hidden');

        tbody.innerHTML = files.map(file => `
            <tr>
                <td>${file.fileId}</td>
                <td>${escapeHtml(file.fileName)}</td>
                <td><span class="ext-badge">${escapeHtml(file.fileExtension)}</span></td>
                <td>${escapeHtml(file.fileVersion)}</td>
                <td>
                    <select class="category-select" data-file-id="${file.fileId}" onchange="updateCategory(this)">
                        ${categoryOptions(file.fileCategory)}
                    </select>
                </td>
                <td>${formatDate(file.uploadedAt)}</td>
                <td>
                    <a href="${file.downloadUrl}" class="btn btn-sm btn-download">다운로드</a>
                </td>
                <td>
                    <button class="btn btn-sm btn-delete" onclick="deleteFile(${file.fileId})">삭제</button>
                </td>
            </tr>
        `).join('');

        currentPage = data.page;
        updatePagination(data);
    } catch (err) {
        showToast('파일 목록을 불러오지 못했습니다.', 'error');
    }
}

function updatePagination(data) {
    const pagination = document.getElementById('pagination');
    const btnPrev = document.getElementById('btn-prev');
    const btnNext = document.getElementById('btn-next');
    const pageInfo = document.getElementById('page-info');

    if (data.totalPages <= 1) {
        pagination.classList.add('hidden');
        return;
    }

    pagination.classList.remove('hidden');
    pageInfo.textContent = `${data.page + 1} / ${data.totalPages}`;
    btnPrev.disabled = data.page === 0;
    btnNext.disabled = data.page >= data.totalPages - 1;
}

// ========== Category ==========
const CATEGORIES = ['매뉴얼', '가이드', '보고서', '기타'];

function categoryOptions(selected) {
    return CATEGORIES.map(c =>
        `<option value="${c}"${c === selected ? ' selected' : ''}>${c}</option>`
    ).join('');
}

async function updateCategory(selectEl) {
    const fileId = selectEl.dataset.fileId;
    const fileCategory = selectEl.value;

    try {
        const res = await fetch('/api/files/' + fileId + '/category', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fileCategory })
        });
        if (!res.ok) throw new Error('변경 실패');
        showToast('카테고리가 변경되었습니다.', 'success');
    } catch (err) {
        showToast('카테고리 변경에 실패했습니다.', 'error');
        loadFileList(currentPage);
    }
}

// ========== Delete ==========
async function deleteFile(fileId) {
    if (!confirm('정말 삭제하시겠습니까?')) return;

    try {
        const res = await fetch('/api/files/' + fileId, { method: 'DELETE' });
        if (!res.ok) throw new Error('삭제 실패');
        showToast('파일이 삭제되었습니다.', 'success');
        loadFileList(currentPage);
    } catch (err) {
        showToast('삭제에 실패했습니다.', 'error');
    }
}

// ========== Upload ==========
function initUploadForm() {
    const form = document.getElementById('upload-form');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const fileCategory = document.getElementById('fileCategory').value;
        const fileInput = document.getElementById('file');

        if (!fileInput.files.length) {
            showToast('파일을 선택해주세요.', 'error');
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner"></span> 업로드 중...';

        const metadata = JSON.stringify({ fileCategory });
        const metadataBlob = new Blob([metadata], { type: 'application/json' });

        const formData = new FormData();
        formData.append('metadata', metadataBlob);
        formData.append('file', fileInput.files[0]);

        try {
            const res = await fetch('/api/files', {
                method: 'POST',
                body: formData
            });

            if (res.status === 409) {
                const err = await res.json();
                showToast(err.message || '동일한 파일이 이미 존재합니다.', 'error');
                return;
            }
            if (!res.ok) throw new Error('업로드 실패');

            showToast('파일이 업로드되었습니다.', 'success');
            form.reset();
            document.getElementById('selected-file-name').classList.add('hidden');
            document.querySelector('.drop-text').classList.remove('hidden');
            location.hash = '#/';
        } catch (err) {
            showToast('업로드에 실패했습니다.', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = '업로드';
        }
    });
}

// ========== Drag & Drop ==========
function initDropZone() {
    const dropZone = document.getElementById('drop-zone');
    const fileInput = document.getElementById('file');
    const fileNameDisplay = document.getElementById('selected-file-name');
    const dropText = dropZone.querySelector('.drop-text');

    fileInput.addEventListener('change', () => {
        if (fileInput.files.length) {
            fileNameDisplay.textContent = fileInput.files[0].name;
            fileNameDisplay.classList.remove('hidden');
            dropText.classList.add('hidden');
        }
    });

    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('drag-over');
    });

    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('drag-over');
    });

    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        if (e.dataTransfer.files.length) {
            fileInput.files = e.dataTransfer.files;
            fileNameDisplay.textContent = e.dataTransfer.files[0].name;
            fileNameDisplay.classList.remove('hidden');
            dropText.classList.add('hidden');
        }
    });
}

// ========== Chat ==========
let chatInitialized = false;
let conversationId = crypto.randomUUID();

function initChat() {
    const chatInput = document.getElementById('chatInput');
    const chatSendBtn = document.getElementById('chatSendBtn');

    document.getElementById('searchScope').addEventListener('change', onSearchScopeChange);
    document.getElementById('versionScope').addEventListener('change', onVersionScopeChange);
    document.getElementById('filterFile').addEventListener('change', onFileSelect);

    chatSendBtn.addEventListener('click', sendChat);
    chatInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendChat();
        }
    });

    document.getElementById('btn-new-chat').addEventListener('click', startNewConversation);
}

function startNewConversation() {
    conversationId = crypto.randomUUID();
    const messagesDiv = document.getElementById('chatMessages');
    messagesDiv.innerHTML = '<div class="chat-welcome">문서에 대해 질문해보세요.</div>';
    showToast('새 대화가 시작되었습니다.', 'success');
}

async function initChatPage() {
    if (!chatInitialized) {
        chatInitialized = true;
    }
    onSearchScopeChange();
}

async function onSearchScopeChange() {
    const scope = document.getElementById('searchScope').value;
    const versionScopeSelect = document.getElementById('versionScope');

    document.getElementById('filter-file').classList.add('hidden');
    document.getElementById('filter-versions').classList.add('hidden');
    document.getElementById('filter-category').classList.add('hidden');

    // FILE 모드일 때 버전 범위 옵션 변경
    if (scope === 'FILE') {
        versionScopeSelect.innerHTML =
            '<option value="LATEST">최신 버전만</option>' +
            '<option value="ALL_VERSIONS">모든 버전</option>' +
            '<option value="SPECIFIC">버전 지정</option>';
        document.getElementById('filter-file').classList.remove('hidden');
        await loadFileNames();
    } else {
        versionScopeSelect.innerHTML =
            '<option value="LATEST">최신 버전만</option>' +
            '<option value="ALL_VERSIONS">모든 버전</option>';
    }

    if (scope === 'CATEGORY') {
        document.getElementById('filter-category').classList.remove('hidden');
        await loadCategories();
    }

    // 버전 체크박스 숨기기
    document.getElementById('filter-versions').classList.add('hidden');
    document.getElementById('versionCheckboxes').innerHTML = '';
}

function onVersionScopeChange() {
    const scope = document.getElementById('searchScope').value;
    const versionScope = document.getElementById('versionScope').value;

    if (scope === 'FILE' && versionScope === 'SPECIFIC') {
        const fileName = document.getElementById('filterFile').value;
        if (fileName) {
            loadVersionCheckboxes(fileName);
        }
    } else {
        document.getElementById('filter-versions').classList.add('hidden');
        document.getElementById('versionCheckboxes').innerHTML = '';
    }
}

async function loadFileNames() {
    try {
        const res = await fetch('/api/chat/files');
        const files = await res.json();
        const select = document.getElementById('filterFile');
        select.innerHTML = '<option value="">파일을 선택하세요</option>'
            + files.map(f => `<option value="${escapeHtml(f)}">${escapeHtml(f)}</option>`).join('');
    } catch (err) {
        showToast('파일 목록을 불러오지 못했습니다.', 'error');
    }
}

async function onFileSelect() {
    const fileName = document.getElementById('filterFile').value;
    const versionScope = document.getElementById('versionScope').value;

    document.getElementById('filter-versions').classList.add('hidden');
    document.getElementById('versionCheckboxes').innerHTML = '';

    if (!fileName) return;

    if (versionScope === 'SPECIFIC') {
        await loadVersionCheckboxes(fileName);
    }
}

async function loadVersionCheckboxes(fileName) {
    const versionsDiv = document.getElementById('filter-versions');
    const checkboxesDiv = document.getElementById('versionCheckboxes');

    try {
        const res = await fetch('/api/chat/files/' + encodeURIComponent(fileName) + '/versions');
        const versions = await res.json();

        if (versions.length > 0) {
            checkboxesDiv.innerHTML = versions.map(v =>
                `<label class="version-label">
                    <input type="checkbox" value="${escapeHtml(v)}" class="version-cb"> ${escapeHtml(v)}
                </label>`
            ).join('');
            versionsDiv.classList.remove('hidden');
        } else {
            versionsDiv.classList.add('hidden');
            checkboxesDiv.innerHTML = '';
        }
    } catch (err) {
        showToast('버전 목록을 불러오지 못했습니다.', 'error');
    }
}

async function loadCategories() {
    try {
        const res = await fetch('/api/chat/categories');
        const categories = await res.json();
        const select = document.getElementById('filterCategory');
        select.innerHTML = '<option value="">카테고리를 선택하세요</option>'
            + categories.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
    } catch (err) {
        showToast('카테고리 목록을 불러오지 못했습니다.', 'error');
    }
}

async function sendChat() {
    const input = document.getElementById('chatInput');
    const message = input.value.trim();
    if (!message) return;

    const scope = document.getElementById('searchScope').value;
    const versionPolicy = document.getElementById('versionScope').value;
    let filterValue = '';
    let versions = [];

    if (scope === 'CATEGORY') {
        filterValue = document.getElementById('filterCategory').value;
        if (!filterValue) {
            showToast('카테고리를 선택해주세요.', 'error');
            return;
        }
    } else if (scope === 'FILE') {
        filterValue = document.getElementById('filterFile').value;
        if (!filterValue) {
            showToast('파일을 선택해주세요.', 'error');
            return;
        }
        if (versionPolicy === 'SPECIFIC') {
            versions = Array.from(document.querySelectorAll('.version-cb:checked')).map(cb => cb.value);
            if (versions.length === 0) {
                showToast('버전을 선택해주세요.', 'error');
                return;
            }
        }
    }

    const systemMessage = document.getElementById('userSystemMessage').value.trim() || null;

    const messagesDiv = document.getElementById('chatMessages');
    const welcome = messagesDiv.querySelector('.chat-welcome');
    if (welcome) welcome.remove();

    appendMessage('user', escapeHtml(message));
    input.value = '';

    const sendBtn = document.getElementById('chatSendBtn');
    sendBtn.disabled = true;
    input.disabled = true;

    const bubble = appendMessage('ai', '<span class="spinner"></span> 답변 생성 중...');

    try {
        const res = await fetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message, conversationId, systemMessage, scope, versionPolicy, filterValue, versions })
        });

        if (!res.ok) throw new Error('채팅 실패');

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let answerText = '';
        let started = false;
        let currentEvent = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop();

            for (const rawLine of lines) {
                const line = rawLine.replace(/\r$/, '');
                if (line.startsWith('event:')) {
                    currentEvent = line.substring(6).trim();
                } else if (line.startsWith('data:')) {
                    const data = line.substring(5);

                    if (currentEvent === 'token') {
                        if (!started) {
                            bubble.innerHTML = '';
                            started = true;
                        }
                        answerText += data;
                        bubble.innerHTML = '<div class="bubble-answer">' + escapeHtml(answerText) + '</div>';
                        messagesDiv.scrollTop = messagesDiv.scrollHeight;
                    } else if (currentEvent === 'references') {
                        try {
                            const refs = JSON.parse(data);
                            if (refs.length > 0) {
                                bubble.innerHTML += renderReferences(refs);
                            }
                        } catch (e) { /* ignore parse error */ }
                    } else if (currentEvent === 'done') {
                        if (data) {
                            bubble.innerHTML += '<div class="bubble-filter">' + escapeHtml(data) + '</div>';
                        }
                    } else if (currentEvent === 'error') {
                        if (!started) {
                            bubble.innerHTML = '';
                            started = true;
                        }
                        bubble.innerHTML = '<div class="bubble-answer bubble-error">' + escapeHtml(data) + '</div>';
                    }
                    currentEvent = '';
                }
            }
        }

        if (!started) {
            bubble.innerHTML = '<div class="bubble-answer bubble-error">응답을 받지 못했습니다.</div>';
        }
    } catch (err) {
        bubble.innerHTML = '<div class="bubble-answer bubble-error">응답을 받지 못했습니다. 다시 시도해주세요.</div>';
    } finally {
        sendBtn.disabled = false;
        input.disabled = false;
        input.focus();
    }
}

function renderReferences(refs) {
    let html = '<div class="chat-references">';
    html += '<details>';
    html += '<summary class="ref-toggle">참조 문서 (' + refs.length + ')</summary>';
    html += '<div class="ref-list">';
    for (const ref of refs) {
        html += '<div class="ref-item">';
        html += '<div class="ref-file">' + escapeHtml(ref.originalFileName)
            + ' <span class="ref-version">' + escapeHtml(ref.fileVersion) + '</span>'
            + ' <span class="ref-category">' + escapeHtml(ref.fileCategory) + '</span></div>';
        if (ref.chunkText) {
            html += '<div class="ref-preview">' + escapeHtml(ref.chunkText) + '</div>';
        }
        html += '</div>';
    }
    html += '</div></details></div>';
    return html;
}

function appendMessage(role, content) {
    const messagesDiv = document.getElementById('chatMessages');
    const bubble = document.createElement('div');
    bubble.className = 'chat-bubble chat-' + role;
    bubble.innerHTML = content;
    messagesDiv.appendChild(bubble);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
    return bubble;
}

// ========== Toast ==========
function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type;

    setTimeout(() => {
        toast.classList.add('hidden');
    }, 2500);
}

// ========== Util ==========
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function formatDate(value) {
    if (!value) return '-';
    const dt = new Date(value);
    const y = dt.getFullYear();
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const d = String(dt.getDate()).padStart(2, '0');
    const h = String(dt.getHours()).padStart(2, '0');
    const min = String(dt.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}`;
}
