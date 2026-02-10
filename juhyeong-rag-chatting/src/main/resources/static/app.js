// ========== Router ==========
const routes = {
    '/': 'list',
    '/upload': 'upload',
    '/chat': 'chat',
    '/detail': 'detail'
};

function navigate(hash) {
    const path = hash.replace('#', '').split('?')[0] || '/';
    const pageId = routes[path] || 'list';

    document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));
    document.getElementById('page-' + pageId).classList.remove('hidden');

    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.dataset.page === pageId);
    });

    if (pageId === 'list') loadFileList();
    if (pageId === 'chat') initChatPage();
    if (pageId === 'detail') initDetailPage();
}

window.addEventListener('hashchange', () => navigate(location.hash));

// ========== Init ==========
document.addEventListener('DOMContentLoaded', () => {
    navigate(location.hash);
    initUploadModal();
    initChat();

    document.getElementById('btn-refresh').addEventListener('click', () => loadFileList(currentPage));
    document.getElementById('btn-prev').addEventListener('click', () => loadFileList(currentPage - 1));
    document.getElementById('btn-next').addEventListener('click', () => loadFileList(currentPage + 1));

    const backBtn = document.getElementById('btn-detail-back');
    if (backBtn) {
        backBtn.addEventListener('click', () => {
            location.hash = '#/';
        });
    }

    const navUpload = document.querySelector('.nav-link[data-page="upload"]');
    if (navUpload) {
        navUpload.addEventListener('click', (e) => {
            e.preventDefault();
            openUploadModal('new');
        });
    }

    // Close modals on overlay click
    document.getElementById('modal-overlay').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) {
            document.getElementById('modal-cancel').click();
        }
    });
    document.getElementById('upload-modal').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) {
            resetUploadModal();
            closeUploadModal();
        }
    });

    // Close modals on Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            const uploadModal = document.getElementById('upload-modal');
            if (!uploadModal.classList.contains('hidden')) {
                resetUploadModal();
                closeUploadModal();
                return;
            }
            const confirmModal = document.getElementById('modal-overlay');
            if (!confirmModal.classList.contains('hidden')) {
                document.getElementById('modal-cancel').click();
            }
        }
    });

});

// ========== File List ==========
let currentPage = 0;
const PAGE_SIZE = 10;
let currentFilesById = {};
let editMode = null;

async function loadFileList(page = 0) {
    const tbody = document.getElementById('file-table-body');
    const emptyMsg = document.getElementById('empty-msg');
    const table = document.querySelector('.table');
    const pagination = document.getElementById('pagination');

    try {
        const res = await fetch(`/api/files?page=${page}&size=${PAGE_SIZE}`);
        const data = await res.json();
        const files = data.content || [];
        currentFilesById = Object.fromEntries(files.map(f => [f.fileId, f]));

        if (data.totalElements === 0) {
            table.classList.add('hidden');
            pagination.classList.add('hidden');
            emptyMsg.classList.remove('hidden');
            return;
        }

        table.classList.remove('hidden');
        emptyMsg.classList.add('hidden');

        tbody.innerHTML = files.map(file => `
            <tr class="file-row">
                <td class="col-name">${escapeHtml(file.groupName || file.originalFileName)}</td>
                <td>${escapeHtml(file.fileCategory ?? '-')}</td>
                <td><span class="version-tag">${escapeHtml(file.fileVersion)}</span></td>
                <td>${file.fileCount ?? '-'}</td>
                <td>${formatUpdatedAt(file.updatedAt, file.uploadedAt)}</td>
                <td>
                    <button class="btn btn-sm btn-version" onclick="uploadNewVersion(${file.fileId})">버전추가</button>
                </td>
                <td>
                    <button class="btn btn-sm btn-secondary" onclick="toggleVersions(${file.fileId}, this)">▼</button>
                </td>
            </tr>
            <tr class="version-row hidden" id="versions-${file.fileId}">
                <td colspan="7">
                    <div class="version-list">로딩 중...</div>
                </td>
            </tr>
        `).join('');

        currentPage = data.number ?? data.page ?? 0;
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

    const pageNumber = data.number ?? data.page ?? 0;
    const totalPages = data.totalPages ?? Math.max(1, Math.ceil((data.totalElements || 0) / PAGE_SIZE));
    pagination.classList.remove('hidden');
    pageInfo.textContent = `${pageNumber + 1} / ${totalPages}`;
    btnPrev.disabled = pageNumber === 0;
    btnNext.disabled = pageNumber >= totalPages - 1;
}

// ========== Detail ==========
async function initDetailPage() {
    const fileId = getHashParam('id');
    if (!fileId) {
        showToast('파일 ID가 없습니다.', 'error');
        location.hash = '#/';
        return;
    }

    try {
        const res = await fetch('/api/files/' + encodeURIComponent(fileId));
        if (!res.ok) throw new Error('상세 조회 실패');
        const file = await res.json();
        renderDetail(file);
        await loadDetailVersions(file.groupId, file.fileId);
        await loadPreview(file);
    } catch (err) {
        showToast('상세 정보를 불러오지 못했습니다.', 'error');
        location.hash = '#/';
    }
}

function renderDetail(file) {
    const fullName = [file.fileName, file.fileExtension].filter(Boolean).join('.');
    document.getElementById('detail-title').textContent = fullName || file.fileName || '-';
    document.getElementById('detail-sub').textContent = '';
    document.getElementById('detail-version').textContent = file.fileVersion || '-';
    document.getElementById('detail-category').textContent = file.fileCategory || '-';
    document.getElementById('detail-uploaded').textContent = formatDateTime(file.uploadedAt);
    document.getElementById('detail-updated').textContent = formatUpdatedAt(file.updatedAt, file.uploadedAt);

    // 상세보기 상단에는 액션 버튼을 표시하지 않음 (버전 목록에서만 제공)
}

async function loadDetailVersions(groupId, currentFileId) {
    const list = document.getElementById('detail-versions-list');
    list.innerHTML = '로딩 중...';
    try {
        const res = await fetch('/api/files/groups/' + encodeURIComponent(groupId) + '/versions');
        if (!res.ok) throw new Error('버전 조회 실패');
        const versions = await res.json();
        list.innerHTML = renderVersionItems(versions, true, currentFileId) || '버전이 없습니다.';
    } catch (err) {
        list.textContent = '버전을 불러오지 못했습니다.';
    }
}

async function loadPreview(file) {
    const body = document.getElementById('detail-preview-body');
    const ext = (file.fileExtension || '').toLowerCase();
    const previewUrl = '/api/files/' + file.fileId + '/preview';

    if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext)) {
        body.innerHTML = `<img class="preview-image" src="${previewUrl}" alt="preview">`;
        return;
    }
    if (ext === 'pdf') {
        body.innerHTML = `<iframe class="preview-frame" src="${previewUrl}" title="preview"></iframe>`;
        return;
    }
    if (['txt', 'md', 'csv', 'json', 'log'].includes(ext)) {
        try {
            const res = await fetch(previewUrl);
            const text = await res.text();
            body.innerHTML = `<pre class="preview-text">${escapeHtml(text)}</pre>`;
        } catch (err) {
            body.textContent = '미리보기를 불러오지 못했습니다.';
        }
        return;
    }

    body.textContent = '미리보기를 지원하지 않는 파일입니다.';
}

function renderVersionItems(versions, includeActions, currentFileId = null) {
    return versions.map(v => `
        <div class="version-item ${currentFileId && String(currentFileId) === String(v.fileId) ? 'current' : ''}">
            <span class="version-tag">${escapeHtml(v.fileVersion)}</span>
            <a class="version-name file-link" href="#/detail?id=${v.fileId}">
                ${escapeHtml([v.fileName, v.fileExtension].filter(Boolean).join('.'))}
            </a>
            ${includeActions ? `
                <div class="version-actions">
                    <button class="btn btn-sm btn-edit btn-tight" onclick="editFile(${v.fileId})">파일수정</button>
                    <a class="btn btn-sm btn-download btn-tight" href="${v.downloadUrl}">다운로드</a>
                    <button class="btn btn-sm btn-delete btn-tight" onclick="deleteFile(${v.fileId})">삭제</button>
                </div>
            ` : ''}
        </div>
    `).join('');
}

// ========== Delete ==========
async function deleteFile(fileId) {
    const ok = await showConfirmModal({
        title: '파일 삭제',
        body: '정말 삭제하시겠습니까?',
        confirmText: '삭제',
        cancelText: '취소'
    });
    if (!ok) return;

    try {
        const res = await fetch('/api/files/' + fileId, { method: 'DELETE' });
        if (!res.ok) throw new Error('삭제 실패');
        showToast('파일이 삭제되었습니다.', 'success');
        if (isDetailPage()) {
            location.hash = '#/';
        } else {
            loadFileList(currentPage);
        }
    } catch (err) {
        showToast('삭제에 실패했습니다.', 'error');
    }
}

// ========== Versions ==========
async function toggleVersions(fileId, button) {
    const row = document.getElementById('versions-' + fileId);
    if (!row) return;

    const isHidden = row.classList.contains('hidden');
    if (!isHidden) {
        row.classList.add('hidden');
        if (button) button.textContent = '▼';
        return;
    }

    row.classList.remove('hidden');
    if (button) button.textContent = '▲';

    const fileInfo = currentFilesById[fileId];
    if (!fileInfo) {
        row.querySelector('.version-list').textContent = '버전 정보를 찾을 수 없습니다.';
        return;
    }

    const groupId = fileInfo.groupId;

    try {
        const res = await fetch('/api/files/groups/' + encodeURIComponent(groupId) + '/versions');
        const versions = await res.json();
        const html = renderExpandedRows(versions);
        row.querySelector('.version-list').innerHTML = html || '버전이 없습니다.';
    } catch (err) {
        row.querySelector('.version-list').textContent = '버전을 불러오지 못했습니다.';
    }
}

function renderExpandedRows(versions) {
    if (!versions || versions.length === 0) return '';
    return `
        <table class="table expanded-table">
            <colgroup>
                <col style="width:6%">
                <col style="width:22%">
                <col style="width:8%">
                <col style="width:8%">
                <col style="width:12%">
                <col style="width:12%">
                <col style="width:10%">
                <col style="width:10%">
            </colgroup>
            <thead>
                <tr class="expanded-header">
                    <th>No.</th>
                    <th>파일명</th>
                    <th>확장자</th>
                    <th>버전</th>
                    <th>업로드일시</th>
                    <th>수정일시</th>
                    <th>다운로드</th>
                    <th>삭제</th>
                </tr>
            </thead>
            <tbody>
                ${versions.map(v => `
                    <tr>
                        <td>${v.fileId}</td>
                        <td class="col-name">
                            <a class="file-link" href="#/detail?id=${v.fileId}">
                                ${escapeHtml(v.fileName)}
                            </a>
                        </td>
                        <td class="col-ext"><span class="ext-badge">${escapeHtml(v.fileExtension)}</span></td>
                        <td><span class="version-tag">${escapeHtml(v.fileVersion)}</span></td>
                        <td>${formatDateTime(v.uploadedAt)}</td>
                        <td>${formatUpdatedAt(v.updatedAt, v.uploadedAt)}</td>
                        <td>
                            <a href="${v.downloadUrl}" class="btn btn-sm btn-download">다운로드</a>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-delete" onclick="deleteFile(${v.fileId})">삭제</button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

// ========== New Version ==========
async function uploadNewVersion(fileId) {
    let fileInfo = currentFilesById[fileId];
    if (!fileInfo) {
        try {
            const res = await fetch('/api/files/' + fileId);
            if (!res.ok) throw new Error('파일 조회 실패');
            fileInfo = await res.json();
        } catch (err) {
            showToast('파일 정보를 찾을 수 없습니다.', 'error');
            return;
        }
    }
    openUploadModal('new-version', fileInfo);
}

// ========== Edit ==========
async function editFile(fileId) {
    let fileInfo = currentFilesById[fileId];
    if (!fileInfo) {
        try {
            const res = await fetch('/api/files/' + fileId);
            if (!res.ok) throw new Error('파일 조회 실패');
            fileInfo = await res.json();
        } catch (err) {
            showToast('파일 정보를 찾을 수 없습니다.', 'error');
            return;
        }
    }

    openUploadModal('edit', fileInfo);
}

// ========== Upload Modal ==========
function initUploadModal() {
    const cancelBtn = document.getElementById('upload-modal-cancel');
    const confirmBtn = document.getElementById('upload-modal-confirm');
    const form = document.getElementById('upload-modal-form');

    cancelBtn.addEventListener('click', () => {
        resetUploadModal();
        closeUploadModal();
    });
    confirmBtn.addEventListener('click', () => form.requestSubmit());

    initModalDropZone();

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const fileCategory = document.getElementById('modalFileCategory').value;
        const versionInput = readVersionInputs(true);
        if (!versionInput) return;

        const fileInput = document.getElementById('modalFile');
        const hasFile = fileInput.files.length > 0;

        if (editMode?.type !== 'edit' && !hasFile) {
            showToast('파일을 선택해 주세요.', 'error');
            return;
        }

        const groupNameInput = document.getElementById('modalGroupName');
        const groupName = groupNameInput ? groupNameInput.value.trim() : null;
        if (editMode?.type === 'new' && (!groupName || groupName === '')) {
            showToast('그룹명을 입력해 주세요.', 'error');
            return;
        }

        confirmBtn.disabled = true;
        confirmBtn.innerHTML = `<span class="spinner"></span> ${editMode?.type === 'edit' ? '수정 중...' : '업로드 중...'}`;

        const metadataObj = {
            fileCategory,
            majorVersion: versionInput.major,
            minorVersion: versionInput.minor,
            patchVersion: versionInput.patch
        };
        if (editMode?.type === 'new') {
            metadataObj.groupName = groupName;
        }
        const metadata = JSON.stringify(metadataObj);
        const metadataBlob = new Blob([metadata], { type: 'application/json' });

        const formData = new FormData();
        formData.append('metadata', metadataBlob);
        if (hasFile) {
            formData.append('file', fileInput.files[0]);
        }

        const resetBtn = () => {
            confirmBtn.disabled = false;
            confirmBtn.textContent = editMode?.type === 'edit' ? '수정하기' : '업로드';
        };

        try {
            let res;
            if (editMode?.type === 'edit') {
                if (!hasFile
                    && fileCategory === editMode.fileCategory
                    && versionInput.major === editMode.majorVersion
                    && versionInput.minor === editMode.minorVersion
                    && versionInput.patch === editMode.patchVersion
                ) {
                    showToast('변경할 내용이 없습니다.', 'error');
                    resetBtn();
                    return;
                }
                res = await fetch('/api/files/' + editMode.fileId, {
                    method: 'PATCH',
                    body: formData
                });
            } else if (editMode?.type === 'new-version') {
                res = await fetch('/api/files/' + editMode.fileId + '/versions', {
                    method: 'POST',
                    body: formData
                });
            } else {
                res = await fetch('/api/files', {
                    method: 'POST',
                    body: formData
                });
            }

            if (res.status === 409) {
                const err = await res.json();
                if (err.code === 'DUPLICATE_CONTENT') {
                    resetBtn();
                    const info = err.duplicateFileName && err.duplicateVersion
                        ? `${err.duplicateFileName} (${err.duplicateVersion})`
                        : '기존 파일';
                    const goDetail = await showConfirmModal({
                        title: '동일한 파일이 이미 존재합니다',
                        body: `동일 파일: ${info}\n상세보기 화면으로 이동할까요?`,
                        confirmText: '이동',
                        cancelText: '취소'
                    });
                    if (goDetail) {
                        resetUploadModal();
                        closeUploadModal();
                        if (err.duplicateFileId) {
                            location.hash = '#/detail?id=' + err.duplicateFileId;
                        } else {
                            location.hash = '#/';
                        }
                    }
                    return;
                }
                resetBtn();
                if (err.code === 'DUPLICATE_GROUP') {
                    showToast(err.message || '동일한 그룹명이 이미 존재합니다.', 'error');
                    return;
                }
                if (err.code === 'DUPLICATE_NAME') {
                    showToast(err.message || '동일한 파일명이 존재합니다. 새 버전 업로드를 사용하세요.', 'error');
                    return;
                }
                if (err.code === 'DUPLICATE_VERSION') {
                    showToast(err.message || '동일한 버전이 이미 존재합니다.', 'error');
                    return;
                }
                showToast(err.message || '동일한 파일이 이미 존재합니다.', 'error');
                return;
            }
            if (!res.ok) throw new Error('업로드 실패');

            if (editMode?.type === 'edit') {
                showToast('문서가 수정되었습니다.', 'success');
                location.hash = '#/detail?id=' + editMode.fileId;
            } else if (editMode?.type === 'new-version') {
                showToast('새 버전이 업로드되었습니다.', 'success');
                if (isDetailPage()) {
                    initDetailPage();
                } else {
                    loadFileList(currentPage);
                }
            } else {
                showToast('파일이 업로드되었습니다.', 'success');
                loadFileList(currentPage);
            }

            resetUploadModal();
            closeUploadModal();
        } catch (err) {
            showToast(editMode?.type === 'edit' ? '문서 수정에 실패했습니다.' : '업로드에 실패했습니다.', 'error');
        } finally {
            resetBtn();
        }
    });
}

function openUploadModal(mode, fileInfo) {
    const modal = document.getElementById('upload-modal');
    const titleEl = document.getElementById('upload-modal-title');
    const subtitleEl = document.getElementById('upload-modal-subtitle');
    const badgeEl = document.getElementById('upload-modal-badge');
    const confirmBtn = document.getElementById('upload-modal-confirm');
    const fileInput = document.getElementById('modalFile');
    const groupNameGroup = document.getElementById('modalGroupNameGroup');
    const groupNameInput = document.getElementById('modalGroupName');

    loadModalCategories();

    if (mode === 'edit') {
        const parsed = parseVersionString(fileInfo.fileVersion);
        editMode = {
            type: 'edit',
            fileId: fileInfo.fileId,
            fileCategory: fileInfo.fileCategory || '',
            majorVersion: parsed.major,
            minorVersion: parsed.minor,
            patchVersion: parsed.patch
        };
        titleEl.textContent = '파일 수정하기';
        subtitleEl.textContent = `${fileInfo.originalFileName || fileInfo.fileName || ''} · ${fileInfo.fileVersion || ''}`;
        badgeEl.textContent = 'EDIT';
        badgeEl.className = 'modal-badge edit';
        confirmBtn.textContent = '수정하기';
        setModalCategory(editMode.fileCategory);
        setModalVersionFromString(fileInfo.fileVersion || '0.0.0');
        fileInput.required = false;
        groupNameGroup.classList.add('hidden');
        groupNameInput.required = false;
    } else if (mode === 'new-version') {
        editMode = {
            type: 'new-version',
            fileId: fileInfo.fileId
        };
        titleEl.textContent = '새 버전 업로드';
        subtitleEl.textContent = `그룹: ${fileInfo.groupName || ''} · 현재 ${fileInfo.fileVersion || ''}`;
        badgeEl.textContent = 'VERSION';
        badgeEl.className = 'modal-badge version';
        confirmBtn.textContent = '업로드';
        setModalCategory(fileInfo.fileCategory || '');
        setModalVersionFromString(fileInfo.fileVersion || '0.0.0');
        fileInput.required = true;
        groupNameGroup.classList.add('hidden');
        groupNameInput.required = false;
    } else {
        editMode = { type: 'new' };
        titleEl.textContent = '새 파일 업로드';
        subtitleEl.textContent = '';
        badgeEl.textContent = 'NEW';
        badgeEl.className = 'modal-badge';
        confirmBtn.textContent = '업로드';
        setModalCategory('');
        setModalVersionFromString('0.0.0');
        fileInput.required = true;
        groupNameGroup.classList.remove('hidden');
        groupNameInput.required = true;
        groupNameInput.value = '';
    }

    modal.classList.remove('hidden');
}

function closeUploadModal() {
    document.getElementById('upload-modal').classList.add('hidden');
}

function resetUploadModal() {
    const form = document.getElementById('upload-modal-form');
    form.reset();
    document.getElementById('modal-selected-file-name').classList.add('hidden');
    document.querySelector('#modal-drop-zone .drop-text').classList.remove('hidden');
    document.getElementById('upload-modal-subtitle').textContent = '';
    editMode = null;
}

function initModalDropZone() {
    const dropZone = document.getElementById('modal-drop-zone');
    const fileInput = document.getElementById('modalFile');
    const fileNameDisplay = document.getElementById('modal-selected-file-name');
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

async function loadModalCategories() {
    const select = document.getElementById('modalFileCategory');
    if (!select) return;
    try {
        const res = await fetch('/api/chat/categories');
        const categories = await res.json();
        const base = ['기획서', '기타'];
        const merged = [...base, ...categories.filter(c => !base.includes(c))];
        select.innerHTML = '<option value="">선택해 주세요.</option>'
            + merged.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
    } catch (err) {
        select.innerHTML = '<option value="">선택해 주세요.</option>'
            + ['기획서', '기타'].map(c => `<option value="${c}">${c}</option>`).join('');
    }
}

// ========== Chat ==========
let chatInitialized = false;
let conversationId = crypto.randomUUID();

function initChat() {
    const chatInput = document.getElementById('chatInput');
    const chatSendBtn = document.getElementById('chatSendBtn');

    document.getElementById('searchScope').addEventListener('change', onSearchScopeChange);
    document.getElementById('versionScope').addEventListener('change', onVersionScopeChange);
    document.getElementById('filterGroup').addEventListener('change', onGroupSelect);

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
    messagesDiv.innerHTML = `<div class="chat-welcome">
        <p class="welcome-title">문서 기반 질문을 해보세요</p>
        <p class="welcome-subtitle">업로드된 문서를 기반으로 답변합니다</p>
    </div>`;
    showToast('새 대화를 시작했습니다.', 'success');
}

async function initChatPage() {
    if (!chatInitialized) {
        chatInitialized = true;
    }
    onSearchScopeChange();
    setTimeout(() => document.getElementById('chatInput').focus(), 100);
}

async function onSearchScopeChange() {
    const scope = document.getElementById('searchScope').value;
    const versionScopeSelect = document.getElementById('versionScope');

    document.getElementById('filter-group').classList.add('hidden');
    document.getElementById('filter-versions').classList.add('hidden');
    document.getElementById('filter-category').classList.add('hidden');
    document.getElementById('versionRadios').innerHTML = '';

    if (scope === 'ALL' || scope === 'CATEGORY') {
        versionScopeSelect.innerHTML =
            '<option value="ALL_VERSIONS">전체 문서</option>' +
            '<option value="LATEST">최신 문서만</option>';
    } else if (scope === 'GROUP') {
        versionScopeSelect.innerHTML =
            '<option value="ALL_VERSIONS">전체 문서</option>' +
            '<option value="SPECIFIC">특정 파일</option>';
        document.getElementById('filter-group').classList.remove('hidden');
        await loadGroupKeys();
    }

    if (scope === 'CATEGORY') {
        document.getElementById('filter-category').classList.remove('hidden');
        await loadCategories();
    }
}

function onVersionScopeChange() {
    const scope = document.getElementById('searchScope').value;
    const versionScope = document.getElementById('versionScope').value;

    if (scope === 'GROUP' && versionScope === 'SPECIFIC') {
        const groupId = document.getElementById('filterGroup').value;
        if (groupId) {
            loadVersionRadios(groupId);
        }
    } else {
        document.getElementById('filter-versions').classList.add('hidden');
        document.getElementById('versionRadios').innerHTML = '';
    }
}

async function loadGroupKeys() {
    try {
        const res = await fetch('/api/chat/groups');
        const groups = await res.json();
        const select = document.getElementById('filterGroup');
        select.innerHTML = '<option value="">그룹을 선택해 주세요.</option>'
            + groups.map(g => `<option value="${g.groupId}">${escapeHtml(g.groupName)} (${escapeHtml(g.category)})</option>`).join('');
    } catch (err) {
        showToast('그룹 목록을 불러오지 못했습니다.', 'error');
    }
}

async function onGroupSelect() {
    const groupId = document.getElementById('filterGroup').value;
    const versionScope = document.getElementById('versionScope').value;

    document.getElementById('filter-versions').classList.add('hidden');
    document.getElementById('versionRadios').innerHTML = '';

    if (!groupId) return;

    if (versionScope === 'SPECIFIC') {
        await loadVersionRadios(groupId);
    }
}

async function loadVersionRadios(groupId) {
    const versionsDiv = document.getElementById('filter-versions');
    const radiosDiv = document.getElementById('versionRadios');

    try {
        const res = await fetch('/api/chat/groups/' + encodeURIComponent(groupId) + '/versions');
        const versions = await res.json();

        if (versions.length > 0) {
            radiosDiv.innerHTML = versions.map(v =>
                `<label class="version-label">
                    <input type="radio" name="versionSelect" value="${escapeHtml(v)}" class="version-rb"> ${escapeHtml(v)}
                </label>`
            ).join('');
            versionsDiv.classList.remove('hidden');
        } else {
            versionsDiv.classList.add('hidden');
            radiosDiv.innerHTML = '';
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
        select.innerHTML = '<option value="">카테고리를 선택해 주세요.</option>'
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
            showToast('카테고리를 선택해 주세요.', 'error');
            return;
        }
    } else if (scope === 'GROUP') {
        filterValue = document.getElementById('filterGroup').value;
        if (!filterValue) {
            showToast('그룹을 선택해 주세요.', 'error');
            return;
        }
        if (versionPolicy === 'SPECIFIC') {
            const selected = document.querySelector('.version-rb:checked');
            if (!selected) {
                showToast('파일을 선택해 주세요.', 'error');
                return;
            }
            versions = [selected.value];
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

    const bubble = appendMessage('ai', '<span class="spinner"></span> 응답 생성 중...');

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
        bubble.innerHTML = '<div class="bubble-answer bubble-error">응답을 받지 못했습니다. 다시 시도해 주세요.</div>';
    } finally {
        sendBtn.disabled = false;
        input.disabled = false;
        input.focus();
    }
}

function renderReferences(refs) {
    let html = '<div class="chat-references">';
    html += '<details>';
    html += '<summary class="ref-toggle">참고 문서 (' + refs.length + ')</summary>';
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
    bubble.scrollIntoView({ behavior: 'smooth', block: 'end' });
    return bubble;
}

// ========== Utils ==========
function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type;
    toast.classList.remove('fade-out');

    clearTimeout(toast._hideTimer);
    toast._hideTimer = setTimeout(() => {
        toast.classList.add('fade-out');
        setTimeout(() => toast.classList.add('hidden'), 400);
    }, 2500);
}

function showModal({
    title,
    body,
    confirmText = '확인',
    cancelText = '취소',
    altText = null,
    showCancel = true,
    showAlt = false
}) {
    return new Promise((resolve) => {
        const overlay = document.getElementById('modal-overlay');
        const titleEl = document.getElementById('modal-title');
        const bodyEl = document.getElementById('modal-body');
        const confirmBtn = document.getElementById('modal-confirm');
        const cancelBtn = document.getElementById('modal-cancel');
        const altBtn = document.getElementById('modal-alt');

        titleEl.textContent = title || '알림';
        bodyEl.textContent = body || '';
        confirmBtn.textContent = confirmText;
        cancelBtn.textContent = cancelText;
        altBtn.textContent = altText || '';
        cancelBtn.classList.toggle('hidden', !showCancel);
        altBtn.classList.toggle('hidden', !showAlt);

        const cleanup = () => {
            confirmBtn.onclick = null;
            cancelBtn.onclick = null;
            altBtn.onclick = null;
            overlay.classList.add('hidden');
        };

        confirmBtn.onclick = () => {
            cleanup();
            resolve('confirm');
        };
        cancelBtn.onclick = () => {
            cleanup();
            resolve('cancel');
        };
        altBtn.onclick = () => {
            cleanup();
            resolve('alt');
        };

        overlay.classList.remove('hidden');
    });
}

async function showConfirmModal(options) {
    const result = await showModal(options);
    return result === 'confirm';
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function getHashParam(key) {
    const hash = location.hash.replace('#', '');
    const queryIndex = hash.indexOf('?');
    if (queryIndex === -1) return null;
    const query = hash.substring(queryIndex + 1);
    const params = new URLSearchParams(query);
    return params.get(key);
}

function isDetailPage() {
    return location.hash.startsWith('#/detail');
}

function formatDateTime(value) {
    const dt = toDate(value);
    if (!dt) return '-';
    const y = dt.getFullYear();
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const d = String(dt.getDate()).padStart(2, '0');
    const h = String(dt.getHours()).padStart(2, '0');
    const min = String(dt.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}`;
}

function formatUpdatedAt(updatedAt, uploadedAt) {
    const updated = toDate(updatedAt);
    if (!updated) return '-';
    const uploaded = toDate(uploadedAt);
    if (uploaded && updated.getTime() === uploaded.getTime()) return '-';
    return formatDateTime(updatedAt);
}

function toDate(value) {
    if (!value) return null;
    if (Array.isArray(value)) {
        const [y, m, d, h = 0, min = 0, s = 0] = value;
        if (!y || !m || !d) return null;
        return new Date(y, m - 1, d, h, min, s);
    }
    if (typeof value === 'object') {
        const y = value.year ?? value.y;
        const m = value.month ?? value.m;
        const d = value.day ?? value.d ?? value.dayOfMonth;
        if (!y || !m || !d) return null;
        const h = value.hour ?? 0;
        const min = value.minute ?? value.min ?? 0;
        const s = value.second ?? 0;
        return new Date(y, m - 1, d, h, min, s);
    }
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) return null;
    return dt;
}

function readVersionInputs(fromModal) {
    const majorEl = document.getElementById(fromModal ? 'modalVersionMajor' : 'versionMajor');
    const minorEl = document.getElementById(fromModal ? 'modalVersionMinor' : 'versionMinor');
    const patchEl = document.getElementById(fromModal ? 'modalVersionPatch' : 'versionPatch');
    const major = parseInt(majorEl.value, 10);
    const minor = parseInt(minorEl.value, 10);
    const patch = parseInt(patchEl.value, 10);
    if (Number.isNaN(major) || Number.isNaN(minor) || Number.isNaN(patch)) {
        showToast('버전은 숫자만 입력해 주세요.', 'error');
        return null;
    }
    if (major < 0 || minor < 0 || patch < 0) {
        showToast('버전은 0 이상의 숫자만 가능합니다.', 'error');
        return null;
    }
    return { major, minor, patch };
}

function parseVersionString(version) {
    const parts = String(version || '0.0.0').split('.');
    const major = parseInt(parts[0] || '0', 10);
    const minor = parseInt(parts[1] || '0', 10);
    const patch = parseInt(parts[2] || '0', 10);
    return {
        major: Number.isNaN(major) ? 0 : major,
        minor: Number.isNaN(minor) ? 0 : minor,
        patch: Number.isNaN(patch) ? 0 : patch
    };
}
function setModalCategory(value) {
    const categorySelect = document.getElementById('modalFileCategory');
    if (categorySelect) {
        categorySelect.value = value ?? '';
    }
}

function setModalVersionFromString(version) {
    const parsed = parseVersionString(version);
    const majorEl = document.getElementById('modalVersionMajor');
    const minorEl = document.getElementById('modalVersionMinor');
    const patchEl = document.getElementById('modalVersionPatch');
    if (!majorEl || !minorEl || !patchEl) return;
    majorEl.value = parsed.major;
    minorEl.value = parsed.minor;
    patchEl.value = parsed.patch;
}
