(function () {
    const launcher = document.getElementById('sbLauncher');
    const panel    = document.getElementById('sbPanel');

    if (!launcher || !panel) return; // fragment chưa render

    // Bốc ra body để tránh ancestor transform/overflow chặn fixed
    try{
        if (launcher.parentElement !== document.body) document.body.appendChild(launcher);
        if (panel.parentElement !== document.body)     document.body.appendChild(panel);
    } catch(_) {}

    const closeBtn = document.getElementById('sbClose');
    const form     = document.getElementById('sbForm');
    const input    = document.getElementById('sbQuestion');
    const msgs     = document.getElementById('sbMsgs');

    function toggle(open){
        if (open === true) panel.classList.add('open');
        else if (open === false) panel.classList.remove('open');
        else panel.classList.toggle('open');
        if (panel.classList.contains('open')) setTimeout(()=>input && input.focus(), 50);
    }
    launcher.addEventListener('click', ()=>toggle());
    closeBtn && closeBtn.addEventListener('click', ()=>toggle(false));

    function escapeHtml(text){ const map={'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}; return (''+text).replace(/[&<>"']/g, m=>map[m]); }
    function linkify(text){
        const e = escapeHtml(text || '');
        return e.replace(/\bhttps?:\/\/[^\s<>"')]+/gi, (u)=>{
            const d = u.length>80 ? u.slice(0,77)+'…' : u;
            return `<a href="${u}" target="_blank" rel="noopener noreferrer">${d}</a>`;
        });
    }
    function addMsg(content, me=false, meta){
        const w=document.createElement('div'); w.className='msg'+(me?' me':'');
        const b=document.createElement('div'); b.className='bubble'; b.textContent=content; w.appendChild(b);
        if(meta){ const m=document.createElement('div'); m.className='meta'; m.textContent=meta; w.appendChild(m); }
        msgs.appendChild(w); msgs.scrollTop=msgs.scrollHeight;
    }
    function addMsgHtml(html, me=false, meta){
        const w=document.createElement('div'); w.className='msg'+(me?' me':'');
        const b=document.createElement('div'); b.className='bubble'; b.innerHTML=html; w.appendChild(b);
        if(meta){ const m=document.createElement('div'); m.className='meta'; m.textContent=meta; w.appendChild(m); }
        msgs.appendChild(w); msgs.scrollTop=msgs.scrollHeight;
    }

    // CSRF (Cách A)
    function getCookie(name){ return document.cookie.split('; ').find(r=>r.startsWith(name+'='))?.split('=')[1]; }
    function buildCsrfHeaders(){
        const headers={'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'};
        const token  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        if (token && header){ headers[header]=token; return headers; }
        const xsrf=getCookie('XSRF-TOKEN'); if(xsrf) headers['X-XSRF-TOKEN']=decodeURIComponent(xsrf);
        return headers;
    }

    async function ask(q){
        try{
            const headers = buildCsrfHeaders();

            // loading bubble
            const loading = document.createElement('div'); loading.className='msg';
            const ld=document.createElement('div'); ld.className='bubble'; ld.innerHTML='<span class="dots"><span>.</span><span>.</span><span>.</span></span>';
            loading.appendChild(ld); msgs.appendChild(loading); msgs.scrollTop=msgs.scrollHeight;

            const res = await fetch('/widget/chatbot/chat', {
                method: 'POST',
                headers,
                body: new URLSearchParams({ question: q })
            });

            loading.remove();

            if (!res.ok) throw new Error('HTTP ' + res.status);
            const data = await res.json();

            const txt = (data?.answer ?? '').toString();
            const htmlAnswer = txt ? linkify(txt) : `<pre>${escapeHtml(JSON.stringify(data, null, 2))}</pre>`;
            addMsgHtml(htmlAnswer, false, data?.intent ? ('intent: ' + data.intent) : null);

            if (Array.isArray(data?.passages) && data.passages.length){
                const top = data.passages.slice(0,3).map(p=>{
                    const label = p?.slug || p?.url || p?.type || 'nguồn';
                    return p?.url ? `<a href="${p.url}" target="_blank" rel="noopener noreferrer">${escapeHtml(label)}</a>` : escapeHtml(label);
                }).join(' | ');
                addMsgHtml('Nguồn: ' + top, false);
            }
        }catch(e){
            addMsg('Lỗi gọi chatbot: ' + e.message, false);
        }
    }

    form.addEventListener('submit', function(ev){
        ev.preventDefault();
        const q = (input?.value || '').trim();
        if (!q) return;
        addMsg(q, true);
        input.value = '';
        ask(q);
    });
})();
