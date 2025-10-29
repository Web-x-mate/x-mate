//@ts-nocheck
;(function () {
  var overlay = document.querySelector('[data-address-overlay]')
  var modal = document.querySelector('[data-address-modal]')
  var openButtons = document.querySelectorAll('[data-open-address-modal]')
  var closeBtn = document.querySelector('[data-close-address-modal]')
  var form = document.querySelector('[data-address-form]')
  if (!form || !modal || !overlay) return

  var modalTitle = modal.querySelector('.address-modal__title')
  var submitBtn = form.querySelector('.btn-big')
  var idField = form.querySelector('[data-address-id-field]')
  var defaultCheckbox = form.querySelector('#is_default')
  var fullNameInput = form.querySelector('#full_name')
  var phoneInput = form.querySelector('#phone')
  var line1Input = form.querySelector('#line1')
  var provinceSelect = document.getElementById('province')
  var districtSelect = document.getElementById('district')
  var wardSelect = document.getElementById('ward')

  var editButtons = document.querySelectorAll('[data-edit-address]')

  var baseAction = form.getAttribute('action') || '/account/addresses'
  var updateAction = form.dataset.updateAction || baseAction
  var baseSubmitLabel = submitBtn ? submitBtn.textContent.trim() : 'Lưu địa chỉ'
  var updateSubmitLabel = 'Cập nhật'
  var baseTitle = modalTitle ? modalTitle.textContent.trim() : 'Thêm địa chỉ mới'
  var updateTitle = 'Cập nhật địa chỉ'

  var placeholders = {
    province: 'Chọn Tỉnh / Thành phố',
    district: 'Chọn Quận / Huyện',
    ward: 'Chọn Phường / Xã'
  }

  var locationData = []
  var pendingLocation = null

  function resetSelect(el, placeholder) {
    if (!el) return
    el.innerHTML = ''
    var opt = document.createElement('option')
    opt.value = ''
    opt.disabled = true
    opt.selected = true
    opt.textContent = placeholder
    el.appendChild(opt)
  }

  function fillOptions(el, items) {
    if (!el || !Array.isArray(items)) return
    items.forEach(function (item) {
      var opt = document.createElement('option')
      opt.value = item.name
      opt.textContent = item.name
      el.appendChild(opt)
    })
  }

  function fillProvinces() {
    resetSelect(provinceSelect, placeholders.province)
    fillOptions(provinceSelect, locationData)
  }

  function fillDistricts(provinceName) {
    resetSelect(districtSelect, placeholders.district)
    resetSelect(wardSelect, placeholders.ward)
    if (!provinceName) return
    var province = locationData.find(function (x) { return x.name === provinceName })
    fillOptions(districtSelect, (province && province.districts) || [])
  }

  function fillWards(provinceName, districtName) {
    resetSelect(wardSelect, placeholders.ward)
    if (!provinceName || !districtName) return
    var province = locationData.find(function (x) { return x.name === provinceName })
    var district = province && province.districts ? province.districts.find(function (x) { return x.name === districtName }) : null
    fillOptions(wardSelect, (district && district.wards) || [])
  }

  function setLocationValues(city, district, ward) {
    if (!provinceSelect || !districtSelect || !wardSelect) return
    if (!locationData.length) {
      pendingLocation = { city: city, district: district, ward: ward }
      return
    }
    fillProvinces()
    if (city) {
      provinceSelect.value = city
      fillDistricts(city)
    }
    if (district) {
      districtSelect.value = district
      fillWards(city, district)
    }
    if (ward) {
      wardSelect.value = ward
    }
  }

  function resetLocationFields() {
    pendingLocation = null
    if (locationData.length) {
      fillProvinces()
    } else {
      resetSelect(provinceSelect, placeholders.province)
    }
    resetSelect(districtSelect, placeholders.district)
    resetSelect(wardSelect, placeholders.ward)
  }

  provinceSelect && provinceSelect.addEventListener('change', function () {
    fillDistricts(provinceSelect.value)
  })
  districtSelect && districtSelect.addEventListener('change', function () {
    fillWards(provinceSelect.value, districtSelect.value)
  })

  function loadLocations() {
    if (!provinceSelect) return
    resetLocationFields()
    fetch('https://provinces.open-api.vn/api/?depth=3')
      .then(function (res) { return res.json() })
      .then(function (data) {
        locationData = data || []
        fillProvinces()
        if (pendingLocation) {
          setLocationValues(pendingLocation.city, pendingLocation.district, pendingLocation.ward)
          pendingLocation = null
        }
      })
      .catch(function (err) {
        console.error('Lỗi tải danh sách địa phương', err)
      })
  }

  function openModal(isEdit) {
    overlay.classList.remove('hidden')
    modal.classList.remove('hidden')
    document.body.classList.add('modal-open')
    if (!isEdit) {
      modalTitle && (modalTitle.textContent = baseTitle)
      submitBtn && (submitBtn.textContent = baseSubmitLabel)
    }
  }

  function closeModal() {
    overlay.classList.add('hidden')
    modal.classList.add('hidden')
    document.body.classList.remove('modal-open')
    form.reset()
    resetLocationFields()
  }

  function prepareCreate() {
    form.reset()
    form.setAttribute('action', baseAction)
    if (idField) idField.value = ''
    if (defaultCheckbox) defaultCheckbox.checked = false
    modalTitle && (modalTitle.textContent = baseTitle)
    submitBtn && (submitBtn.textContent = baseSubmitLabel)
    resetLocationFields()
    openModal(false)
  }

  function prepareEdit(dataset) {
    form.reset()
    form.setAttribute('action', updateAction)
    modalTitle && (modalTitle.textContent = updateTitle)
    submitBtn && (submitBtn.textContent = updateSubmitLabel)
    idField && (idField.value = dataset.addressId || '')
    if (fullNameInput) fullNameInput.value = dataset.fullName || ''
    if (phoneInput) phoneInput.value = dataset.phone || ''
    if (line1Input) line1Input.value = dataset.line1 || ''
    if (defaultCheckbox) {
      defaultCheckbox.checked = dataset.isDefault === 'true' || dataset.isDefault === true
    }
    setLocationValues(dataset.city || '', dataset.district || '', dataset.ward || '')
    openModal(true)
  }

  openButtons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      prepareCreate()
    })
  })

  editButtons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      var card = btn.closest('.address-card')
      if (!card) return
      prepareEdit(card.dataset)
    })
  })

  closeBtn && closeBtn.addEventListener('click', closeModal)
  overlay.addEventListener('click', function (evt) {
    if (evt.target === overlay) closeModal()
  })
  document.addEventListener('keydown', function (evt) {
    if (evt.key === 'Escape' && !modal.classList.contains('hidden')) {
      closeModal()
    }
  })

  loadLocations()
})()
