document.addEventListener('DOMContentLoaded', () => {
    const wsUrl = window.WS_URL || '/ws';
    const list  = document.getElementById('list');
    const search = document.getElementById('search');
    const total  = document.getElementById('total');
    const chats = new Map(); // userId -> { userEmail, preview, time, unread }

    connect();

    function connect() {
        const sock = new SockJS(wsUrl);
        const stomp = Stomp.over(sock);
        stomp.debug = null;

        stomp.connect({}, () => {
            console.log("✅ Connected to admin inbox WS");
            stomp.subscribe('/topic/admin.inbox', frame => {
                try {
                    const n = JSON.parse(frame.body);
                    upsert(n);
                } catch (err) {
                    console.error('Invalid message', err);
                }
            });
        });
    }

    function upsert(n) {
        const old = chats.get(n.userId);
        chats.set(n.userId, {
            userId: n.userId,
            userEmail: n.userEmail,
            preview: n.preview,
            room: n.room,
            time: n.time || Date.now(),
            unread: (old ? old.unread : 0) + 1
        });
        render();
    }

    function render() {
        const q = (search.value || '').trim().toLowerCase();
        const items = [...chats.values()]
            .filter(x => !q || x.userEmail.toLowerCase().includes(q))
            .sort((a,b) => b.time - a.time);

        total.textContent = items.length ? `${items.length} hội thoại` : 'Không có tin nhắn';
        list.innerHTML = '';

        for (const c of items) {
            const div = document.createElement('div');
            div.className = 'chat-item';
            div.innerHTML = `
              <div class="email">${escape(c.userEmail)}</div>
              <div class="preview">${escape(c.preview || '')}</div>
              <div class="time">${formatTime(c.time)}</div>
              <div class="open">
                <a href="/admin/chat?userId=${c.userId}">Mở</a>
                ${c.unread ? `<span class="badge">${c.unread}</span>` : ''}
              </div>
            `;
            div.addEventListener('click', () => {
                window.location.href = `/admin/chat?userId=${c.userId}`;
            });
            list.appendChild(div);
        }
    }

    function escape(s){return (s||'').replace(/[&<>"'`=\/]/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','/':'&#x2F;','`':'&#x60;','=':'&#x3D;'}[ch]));}
    function two(n){return (n<10?'0':'')+n;}
    function formatTime(t){const d=new Date(t);return `${two(d.getHours())}:${two(d.getMinutes())}`;}

    search.addEventListener('input', render);
});
