// address.js
//@ts-nocheck
// Quản lý modal địa chỉ + load danh sách tỉnh/thành cho trang tài khoản

document.addEventListener('DOMContentLoaded', () => {
  const overlay = document.querySelector('[data-address-overlay]');
  const modal = document.querySelector('[data-address-modal]');
  const form = document.querySelector('[data-address-form]');
  if (!overlay || !modal || !form) return;

  const openButtons = Array.from(document.querySelectorAll('[data-open-address-modal]'));
  const closeBtn = document.querySelector('[data-close-address-modal]');
  const submitBtn = form.querySelector('.btn--primary');
  const modalTitle = modal.querySelector('.address-modal__title');
  const idField = form.querySelector('[data-address-id-field]');
  const defaultCheckbox = form.querySelector('#is_default');
  const fullNameInput = form.querySelector('#full_name');
  const phoneInput = form.querySelector('#phone');
  const line1Input = form.querySelector('#line1');
  const editButtons = Array.from(document.querySelectorAll('[data-edit-address]'));

  const provinceSelect = document.getElementById('province');
  const districtSelect = document.getElementById('district');
  const wardSelect = document.getElementById('ward');

  const placeholders = {
    province: '-- Chọn Tỉnh / Thành phố --',
    district: '-- Chọn Quận / Huyện --',
    ward: '-- Chọn Phường / Xã --'
  };

  let locationData = [];
  let pendingLocation = null;

  const resetSelect = (el, placeholder) => {
    if (!el) return;
    el.innerHTML = '';
    const opt = document.createElement('option');
    opt.value = '';
    opt.disabled = true;
    opt.selected = true;
    opt.textContent = placeholder;
    el.appendChild(opt);
  };

  const fillOptions = (el, items) => {
    if (!el || !Array.isArray(items)) return;
    items.forEach((item) => {
      const opt = document.createElement('option');
      opt.value = item.name;
      opt.textContent = item.name;
      el.appendChild(opt);
    });
  };

  const fillProvinces = () => {
    if (!provinceSelect) return;
    resetSelect(provinceSelect, placeholders.province);
    fillOptions(provinceSelect, locationData);
  };

  const fillDistricts = (provinceName) => {
    if (!districtSelect || !wardSelect) return;
    resetSelect(districtSelect, placeholders.district);
    resetSelect(wardSelect, placeholders.ward);
    if (!provinceName) return;
    const province = locationData.find((x) => x.name === provinceName);
    fillOptions(districtSelect, province?.districts || []);
  };

  const fillWards = (provinceName, districtName) => {
    if (!wardSelect) return;
    resetSelect(wardSelect, placeholders.ward);
    if (!provinceName || !districtName) return;
    const province = locationData.find((x) => x.name === provinceName);
    const district = province?.districts?.find((x) => x.name === districtName);
    fillOptions(wardSelect, district?.wards || []);
  };

  const setLocationValues = (city = '', district = '', ward = '') => {
    if (!provinceSelect || !districtSelect || !wardSelect) return;

    const applyValues = () => {
      fillProvinces();
      if (city) {
        provinceSelect.value = city;
        fillDistricts(city);
      } else {
        resetSelect(districtSelect, placeholders.district);
        resetSelect(wardSelect, placeholders.ward);
        return;
      }

      if (district) {
        districtSelect.value = district;
        fillWards(city, district);
      } else {
        resetSelect(wardSelect, placeholders.ward);
        return;
      }

      if (ward) {
        wardSelect.value = ward;
      }
    };

    if (!locationData.length) {
      pendingLocation = { city, district, ward };
      return;
    }

    applyValues();
  };

  const resetLocationFields = () => {
    pendingLocation = null;
    if (!provinceSelect || !districtSelect || !wardSelect) return;
    if (locationData.length) {
      fillProvinces();
    } else {
      resetSelect(provinceSelect, placeholders.province);
    }
    resetSelect(districtSelect, placeholders.district);
    resetSelect(wardSelect, placeholders.ward);
  };

  provinceSelect?.addEventListener('change', () => {
    fillDistricts(provinceSelect.value);
  });

  districtSelect?.addEventListener('change', () => {
    fillWards(provinceSelect.value, districtSelect.value);
  });

  const loadLocations = async () => {
    if (!provinceSelect || !districtSelect || !wardSelect) return;
    resetLocationFields();
    try {
      const res = await fetch('https://provinces.open-api.vn/api/?depth=3');
      if (!res.ok) throw new Error(res.statusText);
      locationData = await res.json();
      fillProvinces();
      if (pendingLocation) {
        const { city, district, ward } = pendingLocation;
        pendingLocation = null;
        setLocationValues(city, district, ward);
      }
    } catch (error) {
      console.error('Lỗi tải danh sách tỉnh/huyện/xã:', error);
    }
  };

  const baseAction = form.getAttribute('action') || '/account/addresses';
  const updateAction = form.dataset.updateAction || baseAction;
  const baseTitle = modalTitle?.textContent?.trim() || 'Thêm địa chỉ mới';
  const baseSubmitLabel = submitBtn?.textContent?.trim() || 'Lưu địa chỉ';
  const updateTitle = 'Cập nhật địa chỉ';
  const updateSubmitLabel = 'Cập nhật';

  const setBaseState = () => {
    form.setAttribute('action', baseAction);
    modalTitle && (modalTitle.textContent = baseTitle);
    submitBtn && (submitBtn.textContent = baseSubmitLabel);
    idField && (idField.value = '');
    form.reset();
    if (defaultCheckbox) defaultCheckbox.checked = false;
    resetLocationFields();
  };

  const prepareEdit = (dataset = {}) => {
    form.reset();
    form.setAttribute('action', updateAction);
    modalTitle && (modalTitle.textContent = updateTitle);
    submitBtn && (submitBtn.textContent = updateSubmitLabel);
    idField && (idField.value = dataset.addressId || '');
    fullNameInput && (fullNameInput.value = dataset.fullName || '');
    phoneInput && (phoneInput.value = dataset.phone || '');
    line1Input && (line1Input.value = dataset.line1 || '');
    if (defaultCheckbox) {
      defaultCheckbox.checked = dataset.isDefault === 'true' || dataset.isDefault === true;
    }
    setLocationValues(dataset.city || '', dataset.district || '', dataset.ward || '');
  };

  const openModal = () => {
    overlay.classList.remove('hidden');
    modal.classList.remove('hidden');
    document.body.classList.add('modal-open');
  };

  const closeModal = () => {
    overlay.classList.add('hidden');
    modal.classList.add('hidden');
    document.body.classList.remove('modal-open');
    setBaseState();
  };

  openButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      setBaseState();
      openModal();
    });
  });

  editButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      const card = btn.closest('.address-card');
      if (!card) return;
      prepareEdit(card.dataset);
      openModal();
    });
  });

  closeBtn?.addEventListener('click', closeModal);
  overlay.addEventListener('click', (event) => {
    if (event.target === overlay) {
      closeModal();
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !modal.classList.contains('hidden')) {
      closeModal();
    }
  });

  loadLocations();
});

