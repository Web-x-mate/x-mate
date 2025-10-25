function csrf() {
    const m = document.cookie.split('; ').find(x => x.startsWith('XSRF-TOKEN='));
    return m ? decodeURIComponent(m.split('=')[1]) : null;
}


function readInit() {
    const el = document.getElementById('initProfile');
    const d = (el?.dataset.dob || '').trim();
    const gd = (el?.dataset.gender || '').trim();
    const h = (el?.dataset.height || '').trim();
    const w = (el?.dataset.weight || '').trim();
    const p = (el?.dataset.phone || '').trim();
    const authm = (el?.dataset.authm || 'local').toLowerCase(); // ⬅️ mới

    return {
        dob: d || null, gender: gd || null,
        height: h ? parseInt(h, 10) : null,
        weight: w ? parseInt(w, 10) : null,
        phone: p || null,
        authm
    };
}

function openModal(id) {
    const m = document.getElementById(id);
    m.style.display = 'flex';
    m.setAttribute('aria-hidden', 'false');

    // ⬇️ Điều chỉnh riêng cho modal đổi mật khẩu
    if (id === 'pwdModal') {
        const {authm} = readInit();
        const oldWrap = document.getElementById('p_old')?.closest('.field');
        if (oldWrap) oldWrap.style.display = (authm === 'local') ? '' : 'none';

        // reset form + thông báo
        document.getElementById('p_old').value = '';
        document.getElementById('p_new').value = '';
        document.getElementById('p_new2').value = '';
        const err = document.getElementById('p_err');
        err.textContent = '';
        err.style.display = 'none';
    }
}

function closeModal(id) {
    const m = document.getElementById(id);
    m.style.display = 'none';
    m.setAttribute('aria-hidden', 'true');
}

async function submitChangePassword() {
    const {authm} = readInit();
    const oldPInput = document.getElementById('p_old'); // có thể ẩn khi oauth
    const oldP = oldPInput ? oldPInput.value.trim() : '';
    const newP = document.getElementById('p_new').value.trim();
    const newP2 = document.getElementById('p_new2').value.trim();
    const err = document.getElementById('p_err');

    // reset error
    err.style.display = 'none';
    err.textContent = '';

    // validate cơ bản
    if (!newP || !newP2) {
        err.textContent = 'Vui lòng nhập đầy đủ các trường.';
        err.style.display = 'block';
        return;
    }
    if (newP !== newP2) {
        err.textContent = 'Mật khẩu nhập lại không khớp.';
        err.style.display = 'block';
        return;
    }
    if (authm === 'local' && !oldP) {
        err.textContent = 'Vui lòng nhập mật khẩu cũ.';
        err.style.display = 'block';
        return;
    }

    // payload: local cần oldP, oauth thì không
    const payload = (authm === 'local') ? {oldP, newP} : {newP};

    try {
        const res = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}, // /api/** đã ignore CSRF
            credentials: 'same-origin',
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const msg = await res.text();
            // map message server → UI
            if (msg.includes('old password incorrect')) throw new Error('Mật khẩu cũ không đúng.');
            if (msg.includes('weak password')) throw new Error('Mật khẩu mới chưa đủ mạnh (>=8 ký tự, gồm hoa, thường, số, ký tự đặc biệt).');
            if (msg.includes('oldP required')) throw new Error('Vui lòng nhập mật khẩu cũ.');
            throw new Error(msg || 'Cập nhật thất bại');
        }

        location.reload();
    } catch (e) {
        err.textContent = e.message || 'Cập nhật mật khẩu thất bại';
        err.style.display = 'block';
    }
}

// Submit POST /logout (Spring Security)
document.getElementById('logoutBtn')?.addEventListener('click', function () {
    document.getElementById('logoutForm').submit();
});

function openEditModal() {
    const init = readInit();
    // DOB
    if (init.dob) {
        const [y, m, d] = init.dob.split('-').map(n => parseInt(n, 10));
        document.getElementById('m_y').value = y;
        document.getElementById('m_m').value = m;
        document.getElementById('m_d').value = d;
    } else {
        document.getElementById('m_y').value = '';
        document.getElementById('m_m').value = '';
        document.getElementById('m_d').value = '';
    }
    // Gender
    document.querySelectorAll('input[name="m_gender"]').forEach(x => x.checked = false);
    if (init.gender) {
        const r = document.querySelector(`input[name="m_gender"][value="${init.gender}"]`);
        if (r) r.checked = true;
    }
    // Phone
    document.getElementById('m_phone').value = init.phone || '';
    // H/W
    const h = document.getElementById('m_h');
    const w = document.getElementById('m_w');
    h.value = (init.height ?? 160);
    w.value = (init.weight ?? 50);
    document.getElementById('m_h_out').textContent = h.value;
    document.getElementById('m_w_out').textContent = w.value;
    h.oninput = () => document.getElementById('m_h_out').textContent = h.value;
    w.oninput = () => document.getElementById('m_w_out').textContent = w.value;

    document.getElementById('editModal').style.display = 'flex';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

async function submitEdit() {
    const err = document.getElementById('m_err');
    err.style.display = 'none';
    err.textContent = '';

    const d = document.getElementById('m_d').value;
    const m = document.getElementById('m_m').value;
    const y = document.getElementById('m_y').value;
    const dob = (d && m && y) ? `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}` : null;
    const genderRadio = document.querySelector('input[name="m_gender"]:checked');

    const payload = {
        phone: document.getElementById('m_phone').value || null,
        gender: genderRadio ? genderRadio.value : null,
        dob: dob,
        heightCm: Number(document.getElementById('m_h').value),
        weightKg: Number(document.getElementById('m_w').value)
    };

    try {
        const res = await fetch('/api/profile/complete', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrf() || ''},
            body: JSON.stringify(payload)
        });
        if (!res.ok) {
            throw new Error(await res.text());
        }
        location.reload();
    } catch (e) {
        err.textContent = e.message || 'Cập nhật thất bại';
        err.style.display = 'block';
    }
}
