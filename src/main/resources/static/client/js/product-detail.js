//@ts-nocheck
;(function () {
  var detail = document.querySelector('.product-detail')
  if (!detail) return

  var mainImage = document.getElementById('mainImage')
  var defaultMainImage = mainImage ? mainImage.getAttribute('src') || '' : ''
  var thumbButtons = Array.from(document.querySelectorAll('.gallery__thumbs button'))
  var colorButtons = Array.from(detail.querySelectorAll('.product-detail__swatches button'))
  var sizeButtons = Array.from(detail.querySelectorAll('.product-detail__sizes button'))
  var colorValueLabel = detail.querySelector('[data-selected-color]')
  var sizeValueLabel = detail.querySelector('[data-selected-size]')
  var addToCartBtn = detail.querySelector('[data-action="add-to-cart"]')
  var priceCurrentEl = detail.querySelector('.product-detail__price-current')
  var priceOriginalEl = detail.querySelector('.product-detail__price-original')
  var priceDiscountEl = detail.querySelector('.product-detail__price-discount')
  var qtyInput = document.getElementById('qty')

  function formatCurrency (value) {
    if (typeof value !== 'number' || !Number.isFinite(value)) return null
    try {
      return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0
      }).format(value)
    } catch (err) {
      return value.toLocaleString('vi-VN')
    }
  }

  function updatePriceDisplay (price, compare) {
    if (priceCurrentEl && price != null) {
      priceCurrentEl.textContent = formatCurrency(price) || price
    }
    var showCompare = compare != null && price != null && compare > price
    if (priceOriginalEl) {
      priceOriginalEl.style.display = showCompare ? '' : 'none'
      if (showCompare) {
        priceOriginalEl.textContent = formatCurrency(compare) || compare
      }
    }
    if (priceDiscountEl) {
      if (showCompare) {
        var percent = Math.round(100 - (price / compare) * 100)
        priceDiscountEl.style.display = percent > 0 ? '' : 'none'
        priceDiscountEl.textContent = percent > 0 ? '-' + percent + '%' : ''
      } else {
        priceDiscountEl.style.display = 'none'
      }
    }
  }

  function parseSizes (attr) {
    if (!attr) return []
    return attr
      .split('|')
      .map(function (s) {
        return s.trim()
      })
      .filter(Boolean)
  }

  function applySizesForColor (allowedList) {
    if (!sizeButtons.length) return
    var allowSet = allowedList && allowedList.length ? new Set(allowedList) : null
    var replacement = null
    sizeButtons.forEach(function (btn) {
      var size = (btn.getAttribute('data-size') || '').trim()
      var ok = !allowSet || allowSet.has(size)
      btn.disabled = !ok
      btn.classList.toggle('is-disabled', !ok)
      if (ok && !replacement) replacement = btn
      if (!ok && btn.classList.contains('is-active')) {
        btn.classList.remove('is-active')
      }
    })

    var active = sizeButtons.find(function (btn) {
      return btn.classList.contains('is-active') && !btn.disabled
    })
    if (!active && replacement) {
      replacement.classList.add('is-active')
      if (sizeValueLabel) {
        sizeValueLabel.textContent =
          replacement.getAttribute('data-size') || replacement.textContent.trim()
      }
    } else if (active && sizeValueLabel) {
      sizeValueLabel.textContent =
        active.getAttribute('data-size') || active.textContent.trim()
    }
  }

  function setActiveColorImage (url) {
    var imageUrl = url || defaultMainImage
    if (mainImage && imageUrl) {
      mainImage.src = imageUrl
    }
    if (addToCartBtn && imageUrl) {
      addToCartBtn.setAttribute('data-product-image', imageUrl)
    }
  }

  function getSelectedColorBtn () {
    return colorButtons.find(function (btn) {
      return btn.classList.contains('is-active')
    })
  }

  function getSelectedSizeBtn () {
    return sizeButtons.find(function (btn) {
      return btn.classList.contains('is-active')
    })
  }

  function getQuantity () {
    if (!qtyInput) return 1
    var value = parseInt(qtyInput.value, 10)
    if (!Number.isFinite(value) || value <= 0) return 1
    return value
  }

  // Thumbnails
  if (thumbButtons.length && mainImage) {
    thumbButtons.forEach(function (btn) {
      btn.addEventListener('click', function () {
        var src = btn.getAttribute('data-src')
        if (src) {
          mainImage.src = src
          if (addToCartBtn) {
            addToCartBtn.setAttribute('data-product-image', src)
          }
        }
        thumbButtons.forEach(function (item) {
          if (item.parentElement) {
            item.parentElement.classList.remove('is-active')
          }
        })
        if (btn.parentElement) {
          btn.parentElement.classList.add('is-active')
        }
      })
    })
  }

  function handleColorSelection (btn) {
    if (!btn) return
    colorButtons.forEach(function (item) {
      item.classList.remove('is-active')
    })
    btn.classList.add('is-active')

    var colorName = btn.getAttribute('data-color-name') || ''
    if (colorValueLabel && colorName) {
      colorValueLabel.textContent = colorName
    }
    var img = btn.getAttribute('data-color-image') || ''
    setActiveColorImage(img)

    var variantId = btn.getAttribute('data-variant-id') || ''
    var variantPrice = parseFloat(btn.getAttribute('data-variant-price'))
    var variantCompare = parseFloat(btn.getAttribute('data-variant-compare'))
    if (Number.isFinite(variantPrice) || Number.isFinite(variantCompare)) {
      updatePriceDisplay(
        Number.isFinite(variantPrice) ? variantPrice : null,
        Number.isFinite(variantCompare) ? variantCompare : null
      )
    }

    var allowedSizes = parseSizes(btn.getAttribute('data-color-sizes'))
    applySizesForColor(allowedSizes)

    if (detail) {
      if (variantId) {
        detail.setAttribute('data-selected-variant', variantId)
      } else {
        detail.removeAttribute('data-selected-variant')
      }
      if (colorName) {
        detail.setAttribute('data-selected-color', colorName)
      }
    }
    if (addToCartBtn) {
      if (variantId) {
        addToCartBtn.setAttribute('data-selected-variant', variantId)
      } else {
        addToCartBtn.removeAttribute('data-selected-variant')
      }
      if (colorName) {
        addToCartBtn.setAttribute('data-selected-color', colorName)
      }
    }
  }

  if (colorButtons.length) {
    colorButtons.forEach(function (btn) {
      btn.addEventListener('click', function () {
        handleColorSelection(btn)
      })
    })
    var initial = getSelectedColorBtn() || colorButtons[0]
    if (initial) {
      handleColorSelection(initial)
    }
  } else if (addToCartBtn && defaultMainImage) {
    addToCartBtn.setAttribute('data-product-image', defaultMainImage)
  }

  if (sizeButtons.length) {
    sizeButtons.forEach(function (btn) {
      btn.addEventListener('click', function () {
        if (btn.disabled) return
        sizeButtons.forEach(function (item) {
          item.classList.remove('is-active')
        })
        btn.classList.add('is-active')
        if (sizeValueLabel) {
          sizeValueLabel.textContent =
            btn.getAttribute('data-size') || btn.textContent.trim()
        }
      })
    })
    var activeSize = getSelectedSizeBtn() || sizeButtons[0]
    if (activeSize) {
      activeSize.classList.add('is-active')
      if (sizeValueLabel) {
        sizeValueLabel.textContent =
          activeSize.getAttribute('data-size') || activeSize.textContent.trim()
      }
    }
  }

  if (qtyInput) {
    document.querySelectorAll('.qty-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var action = btn.getAttribute('data-action')
        var current = parseInt(qtyInput.value, 10) || 1
        if (action === 'increase') {
          qtyInput.value = current + 1
        } else if (action === 'decrease' && current > 1) {
          qtyInput.value = current - 1
        }
      })
    })
  }

  if (!addToCartBtn) return

  addToCartBtn.addEventListener('click', function () {
    if (addToCartBtn.disabled) return

    var productId = addToCartBtn.getAttribute('data-product-id') || ''
    if (!productId) {
      alert('Không xác định được sản phẩm.')
      return
    }

    var slug = addToCartBtn.getAttribute('data-product-slug') || ''
    var title = addToCartBtn.getAttribute('data-product-title') || ''
    var imageSrc = addToCartBtn.getAttribute('data-product-image') || defaultMainImage
    if (mainImage) {
      imageSrc = mainImage.getAttribute('src') || imageSrc
    }

    var colorBtn = getSelectedColorBtn()
    var sizeBtn = getSelectedSizeBtn()
    var payload = {
      productId: productId,
      slug: slug,
      title: title,
      size: sizeBtn ? sizeBtn.getAttribute('data-size') : undefined,
      color: colorBtn ? colorBtn.getAttribute('data-color-name') : undefined,
      quantity: getQuantity(),
      image: imageSrc || undefined
    }

    var variantId = colorBtn ? colorBtn.getAttribute('data-variant-id') : null
    if (variantId) {
      payload.variantId = variantId
    }

    addToCartBtn.disabled = true

    fetch('/cart/items', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify(payload)
    })
      .then(function (response) {
        return response
          .json()
          .catch(function () {
            return null
          })
          .then(function (data) {
            if (!response.ok || !data || data.success === false) {
              var message =
                (data && data.message) || 'Không thể thêm sản phẩm vào giỏ hàng.'
              throw new Error(message)
            }
            if (typeof data.cartQuantity === 'number') {
              var badge = document.querySelector('.cart__badge')
              if (badge) {
                badge.textContent = data.cartQuantity
                badge.style.display = data.cartQuantity > 0 ? '' : 'none'
              }
            }
            alert(data.message || 'Đã thêm sản phẩm vào giỏ hàng.')
          })
      })
      .catch(function (error) {
        console.error('ADD_TO_CART_ERROR:', error)
        alert(
          error && error.message
            ? error.message
            : 'Không thể thêm sản phẩm vào giỏ hàng.'
        )
      })
      .finally(function () {
        addToCartBtn.disabled = false
      })
  })
})()

