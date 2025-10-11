document.addEventListener('DOMContentLoaded', () => {
    const els = {
        tbody: byId('cart-body'),
        empty: byId('empty'),
        sub: byId('subtotal'),
        ship: byId('shipping'),
        discount: byId('discount'),
        total: byId('total'),
        btnCoupon: byId('btnCoupon'),
        couponInput: byId('coupon'),
        couponList: byId('coupon-list'),
        navPrev: document.querySelector('.voucher-strip .prev'),
        navNext: document.querySelector('.voucher-strip .next')
    };

    function byId(id){ return document.getElementById(id); }
    function vnd(n){ return Number(n||0).toLocaleString('vi-VN')+' đ'; }
    function clampQty(q){ q=parseInt(q,10); return isNaN(q)||q<1?1:q; }
    function esc(s){ return String(s||'').replace(/[&<>"'`=\/]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#x27;','/':'&#x2F;','`':'&#x60;','=':'&#x3D;'}[c])); }

    async function apiFetch(url, opts = {}) {
        const o = { credentials: 'same-origin', method: 'GET', ...opts };
        o.headers = { 'Accept': 'application/json', ...(o.headers || {}) };
        if (o.body && !o.headers['Content-Type']) o.headers['Content-Type'] = 'application/json';

        const needsCsrf = !['GET','HEAD','OPTIONS'].includes(String(o.method||'GET').toUpperCase());
        if (needsCsrf) {
            const token = document.querySelector('meta[name="_csrf"]')?.content;
            const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
            if (token) o.headers[header] = token;
        }

        const res = await fetch(url, o);

        if (res.status === 401) {
            const next = encodeURIComponent(location.pathname + location.search);
            location.href = `/login?next=${next}`;
            throw new Error('Unauthorized');
        }

        if (!res.ok) throw new Error(await res.text());

        const ct = res.headers.get('content-type') || '';
        if (!ct.includes('application/json')) {
            const text = await res.text();
            throw new Error('Unexpected response (not JSON): ' + text.slice(0,120));
        }
        return await res.json();
    }

    async function loadCart() {
        try { render(await apiFetch('/api/cart')); }
        catch (err) { console.error('loadCart error:', err); }
    }

    async function patchQty(itemId, qty){
        try{
            if(!itemId) return;
            await apiFetch('/api/cart/qty', { method:'POST', body: JSON.stringify({ itemId, qty: clampQty(qty) }) });
            await loadCart();
        }catch(e){ console.error('patchQty', e); }
    }

    async function removeItem(itemId){
        try{
            if(!itemId) return;
            await apiFetch(`/api/cart/items/${itemId}`, { method:'DELETE' });
            await loadCart();
        }catch(e){ console.error('removeItem', e); }
    }

    async function loadCoupons(subtotal){
        try{
            const list = await apiFetch(`/api/coupons?subtotal=${encodeURIComponent(subtotal||0)}`);
            renderCoupons(list||[]); updateVoucherNav();
        }catch(e){ console.error('loadCoupons', e); }
    }

    async function applyPricing(code){
        try{
            const payload = { addressId:0, couponCode:(code||'').trim()||null };
            const p = await apiFetch('/api/checkout/pricing', { method:'POST', body: JSON.stringify(payload) });
            els.sub.textContent=vnd(p.subtotal);
            els.discount.textContent=vnd(p.discount);
            els.ship.textContent=vnd(p.shipping);
            els.total.textContent=vnd(p.total);
        }catch(e){ console.error('applyPricing', e); }
    }

    function render(dto){
        const items=dto.items||[];
        els.tbody.innerHTML='';
        els.empty.style.display=items.length?'none':'';
        for(const it of items){
            const id=it.itemId;
            const tr=document.createElement('tr');
            const imgUrl=esc(it.imageUrl||'/img/no-image.png');
            tr.innerHTML=`
        <td class="center"><input type="checkbox" data-id="${id}"></td>
        <td>
          <div class="prod">
            <img class="img" src="${imgUrl}">
            <div>
              <div class="title">${esc(it.productName||'')}</div>
              <div class="muted">${esc(it.size||'')}${it.color?' · '+esc(it.color):''}</div>
            </div>
          </div>
        </td>
        <td class="center">${vnd(it.price)}</td>
        <td class="center">
          <button type="button" class="btn btn-sm btn-minus" data-id="${id}">−</button>
          <input class="inp-qty" data-id="${id}" value="${it.qty}">
          <button type="button" class="btn btn-sm btn-plus" data-id="${id}">+</button>
        </td>
        <td class="right">${vnd(it.lineTotal)}</td>
        <td class="right"><button type="button" class="btn btn-danger btn-sm btn-remove" data-id="${id}">Xoá</button></td>`;
            els.tbody.appendChild(tr);
        }
        els.sub.textContent=vnd(dto.subtotal);
        els.discount.textContent=vnd(dto.discount);
        els.ship.textContent=vnd(dto.shipping);
        els.total.textContent=vnd(dto.total);
        loadCoupons(dto.subtotal);
    }

    function renderCoupons(list){
        els.couponList.innerHTML='';
        list.forEach(c=>{
            const el=document.createElement('div');
            el.className='ticket'; el.dataset.code=c.code;
            el.innerHTML=`<div class="code">${c.code} ${c.remaining?`<small class="muted">(Còn ${c.remaining})</small>`:''}</div>
        <div class="desc">${c.title}</div>
        <div class="meta">HSD: ${c.expiry||'—'}</div>`;
            els.couponList.appendChild(el);
        });
    }

    function oneCardWidth(){
        const t=els.couponList.querySelector('.ticket');
        return t?t.getBoundingClientRect().width+10:200;
    }
    function updateVoucherNav(){
        const el=els.couponList;
        const max=el.scrollWidth-el.clientWidth-1;
        const x=Math.round(el.scrollLeft);
        els.navPrev.disabled=x<=0;
        els.navNext.disabled=x>=max;
    }
    function scrollVoucher(dir){ els.couponList.scrollBy({left:dir*oneCardWidth(),behavior:'smooth'}); }

    els.navPrev?.addEventListener('click',()=>scrollVoucher(-1));
    els.navNext?.addEventListener('click',()=>scrollVoucher(1));
    els.couponList.addEventListener('scroll',updateVoucherNav);

    els.btnCoupon?.addEventListener('click',()=>applyPricing((els.couponInput.value||'').trim()));
    els.couponList.addEventListener('click',e=>{
        const card=e.target.closest('.ticket'); if(!card) return;
        [...els.couponList.children].forEach(x=>x.classList.remove('selected'));
        card.classList.add('selected'); els.couponInput.value=card.dataset.code||'';
        applyPricing(card.dataset.code);
    });

    els.tbody.addEventListener('click',e=>{
        const minus=e.target.closest('.btn-minus');
        const plus=e.target.closest('.btn-plus');
        const rm=e.target.closest('.btn-remove');
        if(minus){ const id=minus.dataset.id; const inp=minus.parentElement.querySelector('.inp-qty'); patchQty(id,(parseInt(inp.value||'1',10)-1)); }
        if(plus){ const id=plus.dataset.id; const inp=plus.parentElement.querySelector('.inp-qty'); patchQty(id,(parseInt(inp.value||'1',10)+1)); }
        if(rm){ removeItem(rm.dataset.id); }
    });

    loadCart();
});
