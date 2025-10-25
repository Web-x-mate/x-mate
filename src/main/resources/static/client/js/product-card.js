// @ts-nocheck
;(function () {
  // ===== Helpers chung =====
  function $(root, sel) { return (root || document).querySelector(sel) }
  function $all(root, sel) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)) }

  function toAbs(u) {
    if (!u || typeof u !== 'string') return ''
    u = u.trim()
    if (!u) return ''
    if (/^https?:\/\//i.test(u) || u.startsWith('/')) return u
    return '/' + u.replace(/^\/+/, '')
  }

  // Giữ tham số 'aio' (kích thước) từ ảnh hiện tại để tránh giật layout
  function carryAioParam(fromUrl, toUrl) {
    try {
      var uFrom = new URL(fromUrl, window.location.origin)
      var uTo = new URL(toUrl, window.location.origin)
      var aio = uFrom.searchParams.get('aio')
      if (aio && !uTo.searchParams.get('aio')) {
        uTo.searchParams.set('aio', aio)
      }
      return uTo.toString()
    } catch (e) {
      return toUrl
    }
  }

  function qPrimaryImg(card) {
    return card.querySelector('.product-thumb:not(.product-thumb--hover)') || card.querySelector('.product-thumb')
  }

  function cacheDefaultImages(card) {
    var img = qPrimaryImg(card)
    if (img && !img.dataset.originalSrc) {
      img.dataset.originalSrc = img.getAttribute('src') || ''
    }
    if (img) {
      var def = img.dataset.originalSrc || img.getAttribute('src') || ''
      if (def) {
        card.dataset.defaultImage = def
        if (!card.getAttribute('data-product-image')) {
          card.setAttribute('data-product-image', def)
        }
      }
    }
  }

  function updateCartBadge(count) {
    var cartBadge = document.querySelector('.cart__badge')
    if (!cartBadge) return
    if (typeof count !== 'number' || Number.isNaN(count)) return
    var n = Math.max(0, Math.floor(count))
    cartBadge.textContent = n > 0 ? String(n) : ''
    cartBadge.style.display = n > 0 ? '' : 'none'
  }

  function ensureFeedbackNode(card) {
    var quickAdd = card.querySelector('.quick-add')
    if (!quickAdd) return null
    var node = quickAdd.querySelector('.quick-add__feedback')
    if (!node) {
      node = document.createElement('div')
      node.className = 'quick-add__feedback'
      quickAdd.appendChild(node)
    }
    return node
  }

  function showFeedback(card, message, isError) {
    var feedback = ensureFeedbackNode(card)
    if (!feedback || !message) return
    feedback.textContent = message
    feedback.classList.toggle('is-error', !!isError)
    feedback.classList.add('is-visible')
    clearTimeout(feedback._hideTimer)
    feedback._hideTimer = setTimeout(function () {
      feedback.classList.remove('is-visible')
    }, 2200)
  }

  function parseResponse(response) {
    return response
      .json()
      .catch(function () { return {} })
      .then(function (data) { return { ok: response.ok, status: response.status, data: data } })
  }

  // ===== Đổi ảnh mượt: Preload rồi mới thay src =====
  function preloadAndSwap(card, targetUrl) {
    var img = qPrimaryImg(card)
    if (!img || !targetUrl) return

    var abs = toAbs(targetUrl)

    // dùng cùng 'aio' với ảnh hiện tại để tránh reflow
    var currentSrc = img.currentSrc || img.src || ''
    var withAio = carryAioParam(currentSrc, abs)

    // cache-buster để chắc chắn nạp
    var next = withAio + (withAio.includes('?') ? '&' : '?') + '_v=' + Date.now()

    // ưu tiên tải ảnh mới
    img.setAttribute('loading', 'eager')
    img.setAttribute('fetchpriority', 'high')

    var pre = new Image()
    pre.decoding = 'sync'
    pre.fetchPriority = 'high'
    pre.onload = function () {
      // fade rất nhẹ (optional)
      img.style.transition = 'opacity .12s ease'
      // thay src khi ảnh mới đã sẵn sàng -> nhìn như đổi ngay
      requestAnimationFrame(function () {
        img.src = next
        card.setAttribute('data-product-image', abs)
      })
      console.log('[ProductCard] PRELOAD SWAP ->', next)
    }
    pre.onerror = function () {
      console.warn('[ProductCard] preload error, keep current', next)
    }
    pre.src = next
  }

  // ===== Chọn màu =====
  function setSelectedColor(card, button) {
    $all(card, '.swatch').forEach(function (item) { item.classList.remove('is-active') })
    button.classList.add('is-active')

    var color = button.getAttribute('data-color') || ''
    var variantId = button.getAttribute('data-variant-id') || ''

    if (color) card.setAttribute('data-selected-color', color)
    else card.removeAttribute('data-selected-color')

    if (variantId) card.setAttribute('data-selected-variant', variantId)
    else card.removeAttribute('data-selected-variant')

    var colorImage = (button.getAttribute('data-color-image') || '').trim()
    console.log('[ProductCard] Swatch selected', {
      slug: card.getAttribute('data-product-slug') || null,
      color: color || null,
      variantId: variantId || null,
      image: colorImage || null
    })

    if (colorImage) {
      preloadAndSwap(card, colorImage)
    }
  }

  function getSelectedColor(card) {
    var color = card.getAttribute('data-selected-color')
    if (color && color.trim() !== '') return color
    var active = card.querySelector('.swatch.is-active')
    return active ? active.getAttribute('data-color') || null : null
  }

  // ===== Quick Add =====
  function handleQuickAdd(card, button) {
    var size = button.getAttribute('data-size')
    if (!size) {
      showFeedback(card, 'Vui lòng chọn size hợp lệ.', true)
      return
    }
    var productId = card.getAttribute('data-product-id') || ''
    if (!productId) {
      showFeedback(card, 'Không tìm thấy mã sản phẩm.', true)
      return
    }

    var payload = {
      productId: productId,
      slug: card.getAttribute('data-product-slug') || '',
      title: card.getAttribute('data-product-title') || '',
      price: Number(card.getAttribute('data-product-price') || 0),
      size: size,
      color: getSelectedColor(card),
      variantId: card.getAttribute('data-selected-variant') || null,
      quantity: 1,
      image: card.getAttribute('data-product-image') || ''
    }

    button.disabled = true
    button.classList.add('is-loading')
    showFeedback(card, 'Đang thêm vào giỏ hàng...', false)

    fetch('/cart/items', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: JSON.stringify(payload)
    })
      .then(parseResponse)
      .then(function (result) {
        if (!result) return
        if (result.ok && result.data && result.data.success) {
          updateCartBadge(result.data.cartQuantity)
          showFeedback(card, 'Đã thêm vào giỏ hàng!', false)
        } else {
          var message = (result && result.data && result.data.message) || 'Không thể thêm vào giỏ hàng.'
          showFeedback(card, message, true)
        }
      })
      .catch(function () {
        showFeedback(card, 'Có lỗi xảy ra. Vui lòng thử lại.', true)
      })
      .finally(function () {
        button.disabled = false
        button.classList.remove('is-loading')
      })
  }

  // ===== Khởi tạo từng card =====
  function initCard(card) {
    cacheDefaultImages(card)

    // thiết lập từ swatch đang active (nếu có)
    var initial = card.querySelector('.swatch.is-active')
    if (initial) {
      var colorValue = initial.getAttribute('data-color')
      if (colorValue) card.setAttribute('data-selected-color', colorValue)
      var initialVariantId = initial.getAttribute('data-variant-id')
      if (initialVariantId) card.setAttribute('data-selected-variant', initialVariantId)
      // KHÔNG preload ngay để giảm tải lúc page load.
      // Nếu muốn hiện đúng ảnh màu active từ đầu, bật dòng dưới:
      // preloadAndSwap(card, initial.getAttribute('data-color-image') || '')
    } else if (card.dataset.defaultImage) {
      card.setAttribute('data-product-image', card.dataset.defaultImage)
    }

    // lắng nghe click swatch
    $all(card, '.swatch').forEach(function (btn) {
      btn.addEventListener('click', function () { setSelectedColor(card, btn) })
    })

    // lắng nghe quick add
    $all(card, '.quick-add__btn').forEach(function (btn) {
      btn.addEventListener('click', function () { handleQuickAdd(card, btn) })
    })
  }

  // ===== Entry =====
  function initAll() {
    var cards = document.querySelectorAll('.product-card')
    if (!cards.length) return
    cards.forEach(initCard)
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAll, { once: true })
  } else {
    initAll()
  }
})()
