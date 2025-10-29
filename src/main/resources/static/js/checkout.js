document.addEventListener('DOMContentLoaded', () => {
  const els = {
    // money & controls
    coupon: byId('coupon'),
    btnApply: byId('btnApply'),
    msg: byId('couponMsg'),
    pSub: byId('p-sub'),
    pDiscount: byId('p-discount'),
    pShip: byId('p-ship'),
    pTotal: byId('p-total'),
    dockTotal: byId('dockTotal'),
    btnPlace: byId('btnPlace'),
    dockPayChip: byId('dockPayChip'),
    dockCoupon: byId('dockCoupon'),
    // voucher strip
    voucherStrip: byId('voucherStrip'),
    couponList: byId('coupon-list'),
    navPrev: document.querySelector('.voucher-strip .prev'),
    navNext: document.querySelector('.voucher-strip .next'),
    // VietQR / Proof
    vietqrBox: byId('vietqrBox'),
    pfForm: byId('proofForm'),
    pfFile: byId('pfFile'),
    pfPreview: byId('pfPreview'),
    pfPreviewImg: byId('pfPreviewImg'),
    pfPreviewName: byId('pfPreviewName'),
    pfPreviewSize: byId('pfPreviewSize'),
    pfSend: byId('pfSend'),
    pfStatusText: byId('pfStatusText'),
    // profile
    fullname: byId('fullname'),
    email: byId('email'),
    phone: byId('phone'),
    gender: document.querySelector('section.card form select') || document.querySelector('select'),
    // address
    btnPickAddr: byId('btnPickAddr'),
    address1: byId('address1'),
    city: byId('city'),
    note: byId('note'),
  };

  const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
  const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  const csrfHeaders = CSRF_TOKEN ? { [CSRF_HEADER]: CSRF_TOKEN } : {};

  const state = { addresses: [], usingTypedAddress: true };
  let lastCouponsSubtotal = null;

  function byId(id){ return document.getElementById(id); }
  function fmtVnd(n){ return Number(n || 0).toLocaleString('vi-VN') + ' đ'; }
  function escapeHtml(s){ return (s || '').replace(/[&<>"'`=\/]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#x39;','/':'&#x2F;','`':'&#x60;','=':'&#x3D;'}[c])); }
  function disable(b){ if (els.btnApply) els.btnApply.disabled = b; }
  function cryptoRandom(){ return (crypto && crypto.randomUUID) ? crypto.randomUUID() : 'idem-' + Math.random().toString(36).slice(2) + Date.now(); }

  // profile
  async function loadProfile(){
    try{
      const r = await fetch('/api/me', { credentials:'include' });
      if(!r.ok) return;
      const me = await r.json();
      if (els.fullname && !els.fullname.value) els.fullname.value = me.fullname || '';
      if (els.email && !els.email.value) els.email.value = me.email || '';
      if (els.phone && !els.phone.value) els.phone.value = me.phone || '';
      if (els.gender){
        const want = (String(me.gender||'').toUpperCase()==='FEMALE'||me.gender==='Nữ')?'Nữ':'Nam';
        for(const opt of els.gender.options){ if(opt.text.trim().toLowerCase()===want.toLowerCase()){ opt.selected=true; break; } }
      }
    }catch{}
  }

  // ensure hidden addressId
  (function ensureHiddenAddressId(){
    const form = byId('shippingForm');
    if(form && !byId('addressId')){
      const hid = document.createElement('input');
      hid.type='hidden'; hid.id='addressId';
      form.prepend(hid);
    }
  })();

  function getAddressId(){
    const hid = byId('addressId');
    if (hid && hid.value && String(hid.value).trim()!=='') return Number(hid.value);
    const r = document.querySelector('input.addr-radio:checked');
    return r ? Number(r.value?.trim()) : null;
  }

  // typing new address
  ;['input','change'].forEach(ev=>{
    els.address1?.addEventListener(ev, ()=>{ const hid=byId('addressId'); if(hid) hid.value=''; state.usingTypedAddress=true; });
    els.city?.addEventListener(ev, ()=>{ const hid=byId('addressId'); if(hid) hid.value=''; state.usingTypedAddress=true; });
  });

  // address picker (simple modal inline)
  let pickerEl=null;
  async function openAddrPicker(){
    if(!state.addresses.length){
      try{ const res=await fetch('/api/me/addresses',{credentials:'include'}); if(res.ok) state.addresses=await res.json(); }catch{}
    }
    renderAddrPicker(); pickerEl.style.display='flex';
  }
  function closeAddrPicker(){ if(pickerEl) pickerEl.style.display='none'; }
  function renderAddrPicker(){
    if(!pickerEl){
      pickerEl=document.createElement('div'); pickerEl.id='addrPicker';
      pickerEl.style.cssText='position:fixed;inset:0;background:rgba(15,23,42,.35);display:none;align-items:center;justify-content:center;z-index:9999';
      const card=document.createElement('div');
      card.style.cssText='background:#fff;border:1px solid #e2e8f0;border-radius:14px;box-shadow:0 10px 30px rgba(2,6,23,.15);width:min(680px,92vw);max-height:80vh;overflow:hidden';
      card.innerHTML=`
        <div style="padding:14px 16px;border-bottom:1px solid #e2e8f0;display:flex;align-items:center;justify-content:space-between">
          <b>Chọn địa chỉ đã lưu</b>
          <button id="apClose" class="btn outline" style="padding:.4rem .7rem">Đóng</button>
        </div>
        <div id="apBody" style="padding:12px 16px;max-height:60vh;overflow:auto"></div>
        <div style="padding:12px 16px;border-top:1px solid #e2e8f0;display:flex;gap:8px;flex-wrap:wrap">
          <button id="apUseTyped" class="btn">Giữ địa chỉ đang nhập</button>
          <button id="apSaveTyped" class="btn outline">Lưu địa chỉ đang nhập</button>
        </div>`;
      pickerEl.appendChild(card); document.body.appendChild(pickerEl);
      pickerEl.addEventListener('click', e=>{ if(e.target===pickerEl) closeAddrPicker(); });
      pickerEl.querySelector('#apClose').onclick = closeAddrPicker;
      pickerEl.querySelector('#apUseTyped').onclick = ()=>{ const hid=byId('addressId'); if(hid) hid.value=''; state.usingTypedAddress=true; closeAddrPicker(); };
      pickerEl.querySelector('#apSaveTyped').onclick = saveTypedAddress;
    }
    const body = pickerEl.querySelector('#apBody'); body.innerHTML='';
    if(!Array.isArray(state.addresses)||!state.addresses.length){ body.innerHTML=`<div class="muted">Bạn chưa có địa chỉ nào được lưu.</div>`; return; }
    const wrap=document.createElement('div'); wrap.style.cssText='display:grid;grid-template-columns:1fr;gap:10px';
    state.addresses.forEach(addr=>{
      const item=document.createElement('div');
      item.style.cssText='border:1px solid #e2e8f0;border-radius:12px;padding:10px 12px;display:flex;align-items:center;justify-content:space-between';
      item.innerHTML=`
        <div><div><b>${escapeHtml(addr.line1||'')}</b></div><div class="muted">${escapeHtml(addr.city||'')}${addr.isDefault?' • Mặc định':''}</div></div>
        <div><button class="btn" data-id="${addr.id}">Chọn</button></div>`;
      item.querySelector('button').onclick=()=>{
        if(els.address1) els.address1.value=addr.line1||'';
        if(els.city) els.city.value=addr.city||'';
        const hid=byId('addressId'); if(hid) hid.value=addr.id;
        state.usingTypedAddress=false; closeAddrPicker(); pricing();
      };
      wrap.appendChild(item);
    });
    body.appendChild(wrap);
  }
  async function saveTypedAddress(){
    const line1=(els.address1?.value||'').trim();
    const city =(els.city?.value||'').trim();
    if(!line1){ alert('Vui lòng nhập địa chỉ trước.'); return; }
    try{
      const res=await fetch('/api/me/addresses',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json',...csrfHeaders},body:JSON.stringify({line1,city})});
      if(res.ok){
        const saved=await res.json();
        state.addresses.unshift(saved);
        const hid=byId('addressId'); if(hid) hid.value=saved.id;
        state.usingTypedAddress=false; closeAddrPicker(); pricing();
      }else alert('Không thể lưu địa chỉ (HTTP '+res.status+')');
    }catch{ alert('Không thể lưu địa chỉ lúc này.'); }
  }
  els.btnPickAddr?.addEventListener('click', openAddrPicker);

  // payment
  function getPaymentMethod(){ const r=document.querySelector('input[name="pay"]:checked'); return r ? r.value.toUpperCase() : 'COD'; }
  function getSelectedPayInfo(){
    const r=document.querySelector('input[name="pay"]:checked'); if(!r) return {label:'Thanh toán khi nhận hàng',logo:'/images/cod.png'};
    const item=r.closest('.payitem'); const label=item?.querySelector('.title')?.textContent?.trim()||r.value;
    const img=item?.querySelector('.pay-logo'); const logo=img?img.getAttribute('src'):null; return {label,logo};
  }
  function updateDockPayChip(){
    const info=getSelectedPayInfo();
    if(!els.dockPayChip) return;
    els.dockPayChip.innerHTML=`${info.logo?`<img src="${info.logo}" alt="">`:''}<b>${escapeHtml(info.label)}</b>`;
  }

  // pricing
  async function pricing(){
    const req={ addressId:getAddressId(), couponCode:(els.coupon?.value||'').trim()||null };
    try{
      disable(true);
      const res=await fetch('/api/checkout/pricing',{method:'POST',headers:{'Content-Type':'application/json',...csrfHeaders},credentials:'include',body:JSON.stringify(req)});
      if(!res.ok){ if(res.status===401) location.reload(); const t=await res.text().catch(()=> ''); throw new Error(t||('pricing failed '+res.status)); }
      const data=await res.json();
      renderPricing(data, !!req.couponCode);
    }catch(e){
      console.error(e);
      if(els.msg) els.msg.innerHTML = `<span class="err">Không thể tính toán giá</span>`;
    }finally{ disable(false); }
  }

  function renderPricing(p, hadCoupon){
    els.pSub && (els.pSub.textContent = fmtVnd(p.subtotal || 0));
    els.pDiscount && (els.pDiscount.textContent = fmtVnd(p.discount || 0));
    els.pShip && (els.pShip.textContent = fmtVnd(p.shipping || 0));
    els.pTotal && (els.pTotal.textContent = fmtVnd(p.total || 0));
    els.dockTotal && (els.dockTotal.textContent = fmtVnd(p.total || 0));

    if (els.msg){
      if (hadCoupon){
        els.msg.innerHTML = (p.discount || 0) > 0 ? `<span class="ok">Đã áp dụng mã giảm giá</span>` : `<span class="err">Mã giảm giá không hợp lệ!</span>`;
      } else els.msg.textContent = '';
    }
    if (els.dockCoupon){
      const code=(els.coupon?.value||'').trim();
      els.dockCoupon.textContent = (p.discount>0 && code) ? code : 'Chưa áp dụng';
    }

    // nạp danh sách coupon theo subtotal
    try { fetchCoupons(p.subtotal || 0); } catch {}
  }

  // coupons fetch & render
  async function fetchCoupons(subtotal){
    try{
      if(lastCouponsSubtotal === subtotal) return;
      lastCouponsSubtotal = subtotal;
      const res = await fetch(`/api/coupons?subtotal=${encodeURIComponent(subtotal)}`, { credentials:'include' });
      if(!res.ok) throw new Error('fetch coupons failed '+res.status);
      const list = await res.json();
      renderCoupons(list, subtotal);
    }catch(e){
      if(els.voucherStrip) els.voucherStrip.hidden = true;
      if(els.couponList) els.couponList.innerHTML = '';
    }
  }

  function formatDiscount(c){
    if (c.type === 'PERCENT') return `Giảm ${Number(c.value||0)}%`;
    if (c.type === 'AMOUNT')  return `Giảm ${Number(c.value||0).toLocaleString('vi-VN')}đ`;
    if (c.type === 'FREESHIP')return `Miễn phí vận chuyển`;
    return c.title || '';
  }

  function renderCoupons(list=[], subtotal=0){
    if(!els.couponList || !els.voucherStrip) return;
    const has = Array.isArray(list) && list.length>0;
    els.voucherStrip.hidden = !has;
    els.couponList.innerHTML = '';
    if(!has){ updateVoucherNav(); return; }

    const applied = (els.coupon?.value || '').trim().toUpperCase();

    els.couponList.innerHTML = list.map(c => {
      const minok = !c.minSubtotal || Number(subtotal)>=Number(c.minSubtotal);
      const disc  = formatDiscount(c);
      const remain= c.remaining ? ` (Còn ${c.remaining})` : '';
      const sel   = applied && applied===String(c.code||'').toUpperCase();
      return `
        <div class="ticket ${minok?'':'disabled'} ${sel?'selected':''}"
             data-code="${c.code||''}" ${minok?'':'aria-disabled="true"'}
             title="${minok?'':'Chưa đạt điều kiện áp dụng'}">
          <div class="code">${c.code||''}${remain}</div>
          <div class="desc">${disc}</div>
          <div class="meta">HSD: ${c.expiry||'—'}${c.minSubtotal?` • Min: ${Number(c.minSubtotal).toLocaleString('vi-VN')}đ`:''}</div>
        </div>`;
    }).join('');

    // click card
    els.couponList.querySelectorAll('.ticket').forEach(t => {
      t.addEventListener('click', () => {
        if (t.classList.contains('disabled')) return;
        [...els.couponList.children].forEach(x => x.classList.remove('selected'));
        t.classList.add('selected');
        if(els.coupon){ els.coupon.value = t.dataset.code || ''; }
        pricing();
      });
    });

    // auto-select nếu đã có appliedCouponCode
    if (applied) {
      const found = [...els.couponList.children].find(x => (x.dataset.code||'').toUpperCase()===applied);
      if (found && !found.classList.contains('disabled')) found.classList.add('selected');
    }

    updateVoucherNav();
  }

  function oneCardWidth(){ const t=els.couponList?.querySelector('.ticket'); return t ? t.getBoundingClientRect().width + 10 : 200; }
  function updateVoucherNav(){
    if(!els.couponList) return;
    const el=els.couponList, max=el.scrollWidth-el.clientWidth, x=Math.round(el.scrollLeft);
    els.navPrev && (els.navPrev.disabled = x <= 1);
    els.navNext && (els.navNext.disabled = x >= max - 1);
  }
  function scrollVoucher(dir){ els.couponList?.scrollBy({ left: dir*oneCardWidth(), behavior:'smooth' }); }

  // VietQR toggle
  function updatePayUI(){ const pay=document.querySelector('input[name="pay"]:checked')?.value; els.vietqrBox && els.vietqrBox.classList.toggle('hidden', pay!=='VIETQR'); }
  document.querySelectorAll('input[name="pay"]').forEach(r=>r.addEventListener('change', ()=>{ updatePayUI(); updateDockPayChip(); }));
  updatePayUI(); updateDockPayChip();

  // proof preview + submit
  els.pfFile?.addEventListener('change', e => {
    const f=e.target.files?.[0];
    if(!f){ if(els.pfPreview) els.pfPreview.hidden=true; return; }
    if(els.pfPreview) els.pfPreview.hidden=false;
    if(els.pfPreviewImg) els.pfPreviewImg.src=URL.createObjectURL(f);
    if(els.pfPreviewName) els.pfPreviewName.textContent=f.name;
    if(els.pfPreviewSize) els.pfPreviewSize.textContent=(f.size/1024).toFixed(1)+' KB';
  });
  els.pfForm?.addEventListener('submit', async e=>{
    e.preventDefault(); els.pfSend && (els.pfSend.disabled=true);
    try{
      const fd=new FormData(els.pfForm);
      const res=await fetch('/api/payment/proof',{method:'POST',body:fd,headers:csrfHeaders,credentials:'include'});
      if(!res.ok) throw new Error(await res.text());
      if(els.pfStatusText){ els.pfStatusText.textContent='Đã gửi, chờ admin duyệt'; els.pfStatusText.classList.remove('err'); els.pfStatusText.classList.add('ok'); }
    }catch(err){
      alert('Gửi biên lai thất bại: '+err.message);
      if(els.pfStatusText){ els.pfStatusText.textContent='Lỗi gửi. Thử lại'; els.pfStatusText.classList.add('err'); }
    }finally{ els.pfSend && (els.pfSend.disabled=false); }
  });

  // bindings
  els.btnApply?.addEventListener('click', pricing);
  els.btnPlace?.addEventListener('click', place);
  els.navPrev?.addEventListener('click', ()=>scrollVoucher(-1));
  els.navNext?.addEventListener('click', ()=>scrollVoucher(1));
  els.couponList?.addEventListener('scroll', updateVoucherNav, { passive:true });

  // place
  async function place(){
    let payload;
    const aid=getAddressId();
    if(aid){
      payload={ addressId:aid, couponCode:(els.coupon?.value||'').trim()||null, paymentMethod:getPaymentMethod(), note:els.note?.value||null };
    }else{
      payload={
        addressId:null, couponCode:(els.coupon?.value||'').trim()||null, paymentMethod:getPaymentMethod(), note:els.note?.value||null,
        newAddressFullName:els.fullname?.value, newAddressPhone:els.phone?.value, newAddressEmail:els.email?.value,
        newAddressLine1:els.address1?.value, newAddressCity:els.city?.value
      };
      if(!payload.newAddressFullName || !payload.newAddressPhone || !payload.newAddressLine1){
        alert('Vui lòng điền đầy đủ Họ tên, Số điện thoại và Địa chỉ.'); return;
      }
    }
    try{
      const res=await fetch('/api/checkout',{method:'POST',headers:{'Content-Type':'application/json','Idempotency-Key':cryptoRandom(),...csrfHeaders},credentials:'include',body:JSON.stringify(payload)});
      if(!res.ok){ let msg=''; try{ msg=await res.text(); }catch{} throw new Error(msg||('Đặt hàng thất bại (HTTP '+res.status+')')); }
      const placed=await res.json();
      if(placed.payUrl) location.href=placed.payUrl; else location.href=`/orders/${encodeURIComponent(placed.code||placed.id)}`;
    }catch(e){ console.error(e); alert(e?.message||'Đặt hàng thất bại. Vui lòng thử lại!'); }
  }

  (async function init(){
    await loadProfile();
    try{ await pricing(); }catch{}
  })();
});
