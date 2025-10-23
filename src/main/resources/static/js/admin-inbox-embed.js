// document.addEventListener('DOMContentLoaded', () => {
//     if (!window.IS_ADMIN) return;
//
//     const wsUrl  = window.WS_URL || '/ws';
//     const list   = document.getElementById('inboxList');
//     const search = document.getElementById('inboxSearch');
//     const total  = document.getElementById('inboxTotal');
//
//     const chats = new Map(); // room -> {room, userId, userEmail, preview, time, unread}
//     const CURRENT_ROOM = String(window.CHAT_ROOM || '');
//     let stomp = null, retry = 0;
//     const MAX_RETRY_DELAY = 15000;
//
//     // --- fetch danh sách ban đầu ---
//     fetch('/api/chat/admin/inbox')
//         .then(r => r.ok ? r.json() : [])
//         .then(rows => {
//             rows.forEach(row => upsert(row, false));
//             render();
//         })
//         .catch(() => { if (total) total.textContent = 'Không có tin nhắn'; });
//
//     // --- WebSocket ---
//     connect();
//     function connect() {
//         const sock = new SockJS(wsUrl);
//         stomp = Stomp.over(sock);
//         stomp.debug = null;
//         stomp.connect({}, () => {
//             retry = 0;
//             stomp.subscribe('/topic/admin.inbox', frame => {
//                 try {
//                     const msg = JSON.parse(frame.body);
//                     if (msg && msg.room && msg.userId) {
//                         upsert(msg, true);
//                         render();
//                     }
//                 } catch (err) {
//                     console.error("Parse error:", err);
//                 }
//             });
//         }, () => {
//             const delay = Math.min(1000 * Math.pow(2, retry++), MAX_RETRY_DELAY);
//             setTimeout(connect, delay);
//         });
//     }
//
//     // --- cập nhật dữ liệu ---
//     function upsert(n, addUnread = true) {
//         if (!n.room) return;
//         const key = n.room;
//         const old = chats.get(key);
//         const isCurrentRoom = n.room === CURRENT_ROOM;
//
//         chats.set(key, {
//             room: n.room,
//             userId: n.userId || old?.userId,
//             userEmail: n.userEmail || old?.userEmail || '',
//             preview: n.preview || old?.preview || '',
//             time: n.time || old?.time || Date.now(),
//             unread: isCurrentRoom ? 0 : ((old?.unread || 0) + (addUnread ? 1 : 0))
//         });
//     }
//
//     // --- render UI ---
//     function render() {
//         const q = (search?.value || '').trim().toLowerCase();
//         const items = [...chats.values()]
//             .filter(x => !q || x.userEmail.toLowerCase().includes(q))
//             .sort((a, b) => b.time - a.time);
//
//         if (total) total.textContent = items.length
//             ? `${items.length} cuộc hội thoại`
//             : 'Không có tin nhắn';
//
//         list.innerHTML = '';
//
//         for (const c of items) {
//             if (!c.userEmail) continue;
//
//             const div = document.createElement('div');
//             div.className = 'inbox-item' + (c.room === CURRENT_ROOM ? ' active' : '');
//             div.innerHTML = `
//               <div class="email">${escapeHtml(c.userEmail)}
//                 ${c.unread && c.room !== CURRENT_ROOM ? `<span class="badge">${c.unread}</span>` : ''}
//               </div>
//               <div class="preview">${escapeHtml(c.preview)}</div>
//               <button class="del" aria-label="Xóa">×</button>
//             `;
//
//             // mở phòng
//             div.addEventListener('click', (e) => {
//                 if (e.target.closest('.del')) return;
//                 c.unread = 0;
//                 chats.set(c.room, c);
//                 render();
//                 window.location.href = '/admin/chat?userId=' + c.userId;
//             });
//
//             // xóa hội thoại
//             div.querySelector('.del').addEventListener('click', async (e) => {
//                 e.stopPropagation();
//                 if (!confirm(`Xóa toàn bộ cuộc trò chuyện với ${c.userEmail}?`)) return;
//                 try {
//                     const res = await fetch(`/api/chat/thread/${c.userId}`, { method: 'DELETE' });
//                     if (res.ok) {
//                         chats.delete(c.room);
//                         render();
//                     } else {
//                         alert('Xóa thất bại!');
//                     }
//                 } catch {
//                     alert('Lỗi kết nối khi xóa!');
//                 }
//             });
//
//             list.appendChild(div);
//         }
//     }
//
//     function escapeHtml(s) {
//         return (s || '').replace(/[&<>"'`=\/]/g, ch => ({
//             '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;',
//             "'": '&#39;', '/': '&#x2F;', '`': '&#x60;', '=': '&#x3D;'
//         }[ch]));
//     }
//
//     search?.addEventListener('input', render);
// });
document.addEventListener('DOMContentLoaded', () => {
    if (!window.IS_ADMIN) return;

    const wsUrl  = window.WS_URL || '/ws';
    const list   = document.getElementById('inboxList');
    const search = document.getElementById('inboxSearch');
    const total  = document.getElementById('inboxTotal');

    const chats = new Map(); // room -> {room, userId, userEmail, preview, time, unread}
    const CURRENT_ROOM = String(window.CHAT_ROOM || '');
    let stomp = null, retry = 0;
    const MAX_RETRY_DELAY = 15000;

    // --- fetch danh sách ban đầu ---
    fetch('/api/chat/admin/inbox')
        .then(r => r.ok ? r.json() : [])
        .then(rows => {
            rows.forEach(row => upsert(row, false));
            render();

            const roomInfo = document.getElementById('roomInfo');
            const userInfo = document.getElementById('userInfo');
            if (CURRENT_ROOM && roomInfo) roomInfo.textContent = `Phòng: ${CURRENT_ROOM}`;
            if (window.CURRENT_USER_EMAIL && userInfo) userInfo.textContent = `Đang chat với: ${window.CURRENT_USER_EMAIL}`;

        })
        .catch(() => { if (total) total.textContent = 'Không có tin nhắn'; });

    // --- WebSocket ---
    connect();
    function connect() {
        const sock = new SockJS(wsUrl);
        stomp = Stomp.over(sock);
        stomp.debug = null;
        stomp.connect({}, () => {
            retry = 0;
            stomp.subscribe('/topic/admin.inbox', frame => {
                try {
                    const msg = JSON.parse(frame.body);
                    if (msg && msg.room && msg.userId) {
                        upsert(msg, true);
                        render();
                    }
                } catch (err) {
                    console.error("Parse error:", err);
                }
            });
        }, () => {
            const delay = Math.min(1000 * Math.pow(2, retry++), MAX_RETRY_DELAY);
            setTimeout(connect, delay);
        });
    }

    // --- cập nhật dữ liệu ---
    function upsert(n, addUnread = true) {
        if (!n.room) return;
        const key = n.room;
        const old = chats.get(key);
        const isCurrentRoom = n.room === CURRENT_ROOM;

        chats.set(key, {
            room: n.room,
            userId: n.userId || old?.userId,
            userEmail: n.userEmail || old?.userEmail || '',
            preview: n.preview || old?.preview || '',
            time: n.time || old?.time || Date.now(),
            unread: isCurrentRoom ? 0 : ((old?.unread || 0) + (addUnread ? 1 : 0))
        });
    }

    // --- render UI ---
    function render() {
        const q = (search?.value || '').trim().toLowerCase();
        const items = [...chats.values()]
            .filter(x => !q || x.userEmail.toLowerCase().includes(q))
            .sort((a, b) => b.time - a.time);

        if (total) total.textContent = items.length
            ? `${items.length} cuộc hội thoại`
            : 'Không có tin nhắn';

        list.innerHTML = '';

        for (const c of items) {
            if (!c.userEmail) continue;

            const div = document.createElement('div');
            div.className = 'inbox-item' + (c.room === CURRENT_ROOM ? ' active' : '');
            div.innerHTML = `
              <div class="email">${escapeHtml(c.userEmail)}</div>
              <div class="preview">${escapeHtml(c.preview)}</div>
              <button class="del" aria-label="Xóa">×</button>
            `;

            if (c.unread && c.room !== CURRENT_ROOM) {
                const badge = document.createElement('span');
                badge.className = 'badge';
                badge.textContent = c.unread;
                div.querySelector('.email').insertAdjacentElement('afterend', badge);
            }

            // mở phòng
            div.addEventListener('click', (e) => {
                if (e.target.closest('.del')) return;
                c.unread = 0;
                chats.set(c.room, c);
                render();
                window.location.href = '/admin/chat?userId=' + c.userId;
            });

            // xóa hội thoại
            div.querySelector('.del').addEventListener('click', async (e) => {
                e.stopPropagation();
                if (!confirm(`Xóa toàn bộ cuộc trò chuyện với ${c.userEmail}?`)) return;
                try {
                    const res = await fetch(`/api/chat/thread/${c.userId}`, { method: 'DELETE' });
                    if (res.ok) {
                        chats.delete(c.room);
                        render();
                    } else {
                        alert('Xóa thất bại!');
                    }
                } catch {
                    alert('Lỗi kết nối khi xóa!');
                }
            });

            list.appendChild(div);
        }
    }

    function escapeHtml(s) {
        return (s || '').replace(/[&<>"'`=\/]/g, ch => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;',
            "'": '&#39;', '/': '&#x2F;', '`': '&#x60;', '=': '&#x3D;'
        }[ch]));
    }

    search?.addEventListener('input', render);
});