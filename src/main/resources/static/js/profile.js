// /js/profile.js
// @ts-nocheck

/* ---------- Helpers ---------- */

function csrf() {
  const m = document.cookie.split('; ').find(x => x.startsWith('XSRF-TOKEN='));
  return m ? decodeURIComponent(m.split('=')[1]) : null;
}

function readInit() {
  const el = document.getElementById('initProfile');
  if (!el) {
    return { dob: null, gender: null, height: null, weight: null, phone: null, authm: 'local', name: '' };
  }
  const toNumber = (value) => {
    if (value === undefined || value === null || value.trim() === '') return null;
    const num = Number(value);
    return Number.isFinite(num) ? num : null;
  };
  return {
    dob: (el.dataset.dob || '').trim() || null,
    gender: (el.dataset.gender || '').trim() || null,
    height: toNumber(el.dataset.height || ''),
    weight: toNumber(el.dataset.weight || ''),
    phone: (el.dataset.phone || '').trim() || null,
    authm: (el.dataset.authm || 'local').toLowerCase(),
    name: el.dataset.name || ''
  };
}

/* ---------- Modal controller ---------- */

function openModal(id) {
  const modal = document.getElementById(id);
  if (!modal) return;
  modal.classList.add('is-open');
  modal.setAttribute('aria-hidden', 'false');
  document.body.classList.add('modal-lock');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (!modal) return;
  modal.classList.remove('is-open');
  modal.setAttribute('aria-hidden', 'true');
  document.body.classList.remove('modal-lock');
}

window.openModal = openModal;
window.closeModal = closeModal;

/* ---------- Profile modal ---------- */

window.openEditModal = function () {
  const init = readInit();

  const nameInput = document.getElementById('m_fullname');
  if (nameInput) nameInput.value = init.name || '';

  const dobInput = document.getElementById('m_dob');
  if (dobInput) dobInput.value = init.dob || '';

  const genderSelect = document.getElementById('m_gender');
  if (genderSelect) genderSelect.value = init.gender || '';

  const phoneInput = document.getElementById('m_phone');
  if (phoneInput) phoneInput.value = init.phone || '';

  const heightInput = document.getElementById('m_height');
  if (heightInput) heightInput.value = init.height ?? '';

  const weightInput = document.getElementById('m_weight');
  if (weightInput) weightInput.value = init.weight ?? '';

  const err = document.getElementById('m_err');
  if (err) {
    err.style.display = 'none';
    err.textContent = '';
  }

  openModal('modal-info');
};

window.submitEdit = async function () {
  const err = document.getElementById('m_err');
  if (err) {
    err.style.display = 'none';
    err.textContent = '';
  }

  const phoneRaw = document.getElementById('m_phone')?.value?.trim() || '';
  const genderVal = document.getElementById('m_gender')?.value?.trim() || '';
  const dobVal = document.getElementById('m_dob')?.value?.trim() || '';

  const heightRaw = document.getElementById('m_height')?.value?.trim() || '';
  const weightRaw = document.getElementById('m_weight')?.value?.trim() || '';

  const toNullableNumber = (value) => {
    if (!value) return null;
    const num = Number(value);
    return Number.isFinite(num) ? num : null;
  };

  const payload = {
    phone: phoneRaw || null,
    gender: genderVal || null,
    dob: dobVal || null,
    heightCm: toNullableNumber(heightRaw),
    weightKg: toNullableNumber(weightRaw)
  };

  try {
    const res = await fetch('/api/profile/complete', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrf() || ''
      },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const msg = await res.text();
      throw new Error(msg || 'Cập nhật thất bại');
    }
    location.reload();
  } catch (e) {
    if (err) {
      err.textContent = e.message || 'Cập nhật thất bại';
      err.style.display = 'block';
    }
  }
};

/* ---------- Password modal ---------- */

window.openPasswordModal = function () {
  const init = readInit();

  const oldGroup = document.getElementById('old-password-group');
  if (oldGroup) {
    oldGroup.style.display = init.authm === 'local' ? '' : 'none';
  }

  const clear = (id) => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  };
  clear('p_old');
  clear('p_new');
  clear('p_new2');

  const err = document.getElementById('p_err');
  if (err) {
    err.style.display = 'none';
    err.textContent = '';
  }

  openModal('modal-password');
};

window.submitChangePassword = async function () {
  const init = readInit();
  const authm = init.authm || 'local';

  const oldPInput = document.getElementById('p_old');
  const oldP = oldPInput ? oldPInput.value.trim() : '';
  const newP = document.getElementById('p_new')?.value?.trim() || '';
  const newP2 = document.getElementById('p_new2')?.value?.trim() || '';
  const err = document.getElementById('p_err');

  if (err) {
    err.style.display = 'none';
    err.textContent = '';
  }

  if (!newP || !newP2) {
    if (err) {
      err.textContent = 'Vui lòng nhập đầy đủ thông tin.';
      err.style.display = 'block';
    }
    return;
  }

  if (newP !== newP2) {
    if (err) {
      err.textContent = 'Mật khẩu nhập lại không khớp.';
      err.style.display = 'block';
    }
    return;
  }

  if (authm === 'local' && !oldP) {
    if (err) {
      err.textContent = 'Vui lòng nhập mật khẩu hiện tại.';
      err.style.display = 'block';
    }
    return;
  }

  const payload = authm === 'local' ? { oldP, newP } : { newP };

  try {
    const res = await fetch('/api/auth/change-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const msg = await res.text();
      if (err) {
        if (msg.includes('old password incorrect')) {
          err.textContent = 'Mật khẩu hiện tại không đúng.';
        } else if (msg.includes('weak password')) {
          err.textContent = 'Mật khẩu mới chưa đủ mạnh.';
        } else if (msg.includes('oldP required')) {
          err.textContent = 'Vui lòng nhập mật khẩu hiện tại.';
        } else {
          err.textContent = msg || 'Cập nhật thất bại';
        }
        err.style.display = 'block';
      }
      return;
    }
    location.reload();
  } catch (e) {
    if (err) {
      err.textContent = e.message || 'Cập nhật thất bại';
      err.style.display = 'block';
    }
  }
};

/* ---------- Global events ---------- */

document.addEventListener('DOMContentLoaded', function () {
  closeModal('modal-info');
  closeModal('modal-password');

  document.addEventListener('click', function (e) {
    if (e.target.matches('[data-close], .modal__overlay')) {
      const modal = e.target.closest('.modal');
      if (modal) {
        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-lock');
      }
    }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal.is-open').forEach(modal => {
        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
      });
      document.body.classList.remove('modal-lock');
    }
  });

  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', function () {
      const form = document.getElementById('logoutForm');
      if (form) form.submit();
    });
  }
});

