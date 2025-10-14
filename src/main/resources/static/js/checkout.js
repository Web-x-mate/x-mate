document.addEventListener('DOMContentLoaded', () => {
    const els = {
        radios: Array.from(document.querySelectorAll('.addr-radio')),
        coupon: byId('coupon'),
        btnApply: byId('btnApply'),
        msg: byId('couponMsg'),
        items: byId('items'),
        pSub: byId('p-sub'),
        pDiscount: byId('p-discount'),
        pShip: byId('p-ship'),
        pTotal: byId('p-total'),
        btnPlace: byId('btnPlace'),
        dockTotal: byId('dockTotal'),
        dockPayChip: byId('dockPayChip'),
        couponList: byId('coupon-list'),
        navPrev: document.querySelector('.voucher-strip .prev'),
        navNext: document.querySelector('.voucher-strip .next')
    };

    function byId(id){ return document.getElementById(id); }
    function fmtVnd(n){ return Number(n||0).toLocaleString('vi-VN')+' đ'; }
    function escapeHtml(s){ return (s||'').replace(/[&<>"'`=\/]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#x39;','/':'&#x2F;','`':'&#x60;','=':'&#x3D;'}[c])); }
    function disable(b){ [els.btnPlace, els.btnApply].forEach(x=>x && (x.disabled=b)); }
    function cryptoRandom(){ return (crypto && crypto.randomUUID) ? crypto.randomUUID() : 'idem-'+Math.random().toString(36).slice(2)+Date.now(); }

    function getAddressId(){
        const r=document.querySelector('input.addr-radio:checked');
        return r? Number(r.value?.trim()):null;
    }
    function getPaymentMethod(){
        const r=document.querySelector('input[name="pay"]:checked');
        return r? r.value.toUpperCase() : 'COD';
    }
    function getSelectedPayInfo(){
        const r=document.querySelector('input[name="pay"]:checked');
        if(!r) return {label:'Thanh toán khi nhận hàng',logo:'/images/cod.png'};
        const item=r.closest('.payitem');
        const label=item?.querySelector('.title')?.textContent?.trim()||r.value;
        const img=item?.querySelector('.pay-logo');
        const logo=img? img.getAttribute('src'):null;
        return {label,logo};
    }
    function updateDockPayChip(){
        const info=getSelectedPayInfo();
        if(!els.dockPayChip) return;
        els.dockPayChip.innerHTML = `${info.logo? `<img src="${info.logo}" alt="">` : ''}<b>${escapeHtml(info.label)}</b>`;
    }

    async function pricing(){
        const token=document.querySelector('meta[name="_csrf"]')?.content;
        const header=document.querySelector('meta[name="_csrf_header"]')?.content||'X-CSRF-TOKEN';
        const csrfHeaders=token? {[header]:token}:{};
        const req={ addressId:getAddressId(), couponCode:(els.coupon?.value||'').trim()||null };
        try{
            disable(true);
            const res=await fetch('/api/checkout/pricing',{
                method:'POST',
                headers:{'Content-Type':'application/json',...csrfHeaders},
                body:JSON.stringify(req),
            });
            if(!res.ok){ if(res.status===401) location.reload(); throw new Error('pricing failed '+res.status); }
            const data=await res.json();
            renderPricing(data, !!req.couponCode);
        }catch(e){
            console.error(e);
            if(els.msg) els.msg.innerHTML = `<span class="err">Không thể tính toán giá</span>`;
        }finally{ disable(false); }
    }

    function renderPricing(p, hadCoupon){
        els.pSub && (els.pSub.textContent=fmtVnd(p.subtotal||0));
        els.pDiscount && (els.pDiscount.textContent=fmtVnd(p.discount||0));
        els.pShip && (els.pShip.textContent=fmtVnd(p.shipping||0));
        els.pTotal && (els.pTotal.textContent=fmtVnd(p.total||0));
        els.dockTotal && (els.dockTotal.textContent=fmtVnd(p.total||0));

        if(els.msg){
            if(hadCoupon){
                els.msg.innerHTML = (p.discount||0)>0 ? `<span class="ok">Đã áp dụng mã giảm giá</span>` : `<span class="err">Mã giảm giá không hợp lệ</span>`;
            }else els.msg.textContent='';
        }
        const dockCoupon=document.getElementById('dockCoupon');
        if(dockCoupon){
            const code=(els.coupon?.value||'').trim();
            dockCoupon.textContent = (p.discount>0 && code) ? code : 'chưa áp dụng';
        }
    }

    async function place(){
        const token=document.querySelector('meta[name="_csrf"]')?.content;
        const header=document.querySelector('meta[name="_csrf_header"]')?.content||'X-CSRF-TOKEN';
        const csrfHeaders=token? {[header]:token}:{};

        let payload;
        const aid=getAddressId();
        if(aid){
            payload={ addressId:aid, couponCode:(els.coupon?.value||'').trim()||null, paymentMethod:getPaymentMethod(), note:byId('note')?.value||null };
        }else{
            payload={
                addressId:null,
                couponCode:(els.coupon?.value||'').trim()||null,
                paymentMethod:getPaymentMethod(),
                note:byId('note')?.value||null,
                newAddressFullName:byId('fullname')?.value,
                newAddressPhone:byId('phone')?.value,
                newAddressEmail:byId('email')?.value,
                newAddressLine1:byId('address1')?.value,
                newAddressCity:byId('city')?.value,
            };
            if(!payload.newAddressFullName || !payload.newAddressPhone || !payload.newAddressLine1){
                alert('Vui lòng điền đầy đủ Họ tên, Số điện thoại và Địa chỉ.'); return;
            }
        }

        try{
            disable(true);
            const res=await fetch('/api/checkout',{
                method:'POST',
                headers:{'Content-Type':'application/json','Idempotency-Key':cryptoRandom(),...csrfHeaders},
                body:JSON.stringify(payload),
            });
            if(!res.ok) throw new Error('Đặt hàng thất bại '+res.status);
            const placed=await res.json();
            if(placed.payUrl) location.href=placed.payUrl;
            else{
                const code=placed.code || placed.id;
                location.href=`/orders/${encodeURIComponent(code)}`;
            }
        }catch(e){
            console.error(e); alert('Đặt hàng thất bại. Vui lòng thử lại!');
        }finally{ disable(false); }
    }

    function oneCardWidth(){
        const t=els.couponList?.querySelector('.ticket');
        return t? t.getBoundingClientRect().width+10 : 200;
    }
    function updateVoucherNav(){
        if(!els.couponList) return;
        const el=els.couponList, max=el.scrollWidth-el.clientWidth, x=Math.round(el.scrollLeft);
        els.navPrev && (els.navPrev.disabled = x<=1);
        els.navNext && (els.navNext.disabled = x>=max-1);
    }
    function scrollVoucher(dir){
        els.couponList?.scrollBy({left:dir*oneCardWidth(),behavior:'smooth'});
    }

    // bindings
    els.btnApply?.addEventListener('click', pricing);
    els.btnPlace?.addEventListener('click', place);
    document.querySelectorAll('input[name="pay"]').forEach(r=>r.addEventListener('change',updateDockPayChip));
    els.radios.forEach(r=>r.addEventListener('change', pricing));
    els.navPrev?.addEventListener('click',()=>scrollVoucher(-1));
    els.navNext?.addEventListener('click',()=>scrollVoucher(1));
    els.couponList?.addEventListener('scroll',updateVoucherNav,{passive:true});
    els.couponList?.addEventListener('click',e=>{
        const card=e.target.closest('.ticket'); if(!card || !els.coupon) return;
        [...els.couponList.children].forEach(x=>x.classList.remove('selected'));
        card.classList.add('selected'); els.coupon.value=card.dataset.code||''; pricing();
    });

    updateDockPayChip();
    updateVoucherNav();
});
