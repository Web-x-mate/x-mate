// /static/js/chat.js
document.addEventListener('DOMContentLoaded', function () {
    const room   = window.CHAT_ROOM;
    const me     = window.CHAT_ME;
    const meId   = window.CHAT_ME_ID;
    const wsUrl  = window.WS_URL || '/ws';

    const chatArea = document.getElementById('chatArea');
    const input    = document.getElementById('msg');
    const sendBtn  = document.getElementById('sendBtn');

    const escapeHtml = (s) => (s || '').replace(/[&<>"'`=\/]/g, ch => ({
        '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','/':'&#x2F;','`':'&#x60;','=':'&#x3D;'
    }[ch]));

    function appendMessage(sender, content, asSystem = false) {
        const div = document.createElement('div');
        if (asSystem) {
            div.className = 'msg system';
            div.innerHTML = `<div class="bubble"><div class="text">${escapeHtml(content)}</div></div>`;
        } else {
            div.className = 'msg ' + (sender === me ? 'me' : 'other');
            div.innerHTML = `
        <div class="bubble">
          <div class="sender">${escapeHtml(sender)}</div>
          <div class="text">${escapeHtml(content)}</div>
        </div>`;
        }
        chatArea.appendChild(div);
        chatArea.scrollTop = chatArea.scrollHeight;
    }

    function setSendingEnabled(connected) {
        sendBtn.disabled = !connected;
        sendBtn.title    = connected ? '' : 'Đang kết nối...';
    }

    let stomp = null;
    let connected = false;
    let retry = 0;
    const MAX_RETRY_DELAY = 15000;

    function connect() {
        setSendingEnabled(false);
        if (retry === 0) appendMessage('', 'Đang kết nối...', true);

        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            appendMessage('', 'Không tải được SockJS/STOMP. Kiểm tra thẻ <script>.', true);
            return;
        }

        const socket = new SockJS(wsUrl);
        stomp = Stomp.over(socket);
        stomp.debug = null;
        stomp.heartbeat.outgoing = 10000;
        stomp.heartbeat.incoming = 10000;
        stomp.connect({}, onConnected, onError);
    }

    function loadHistory() {
        // clear UI trước khi nạp lịch sử
        chatArea.innerHTML = '';
        fetch(`/api/chat/history?room=${encodeURIComponent(room)}`)
            .then(r => r.ok ? r.json() : [])
            .then(list => {
                const frag = document.createDocumentFragment();
                list.forEach(m => {
                    const name = m.sender || 'Unknown';
                    const div  = document.createElement('div');
                    const isMe = name === me;
                    div.className = 'msg ' + (isMe ? 'me' : 'other');
                    div.innerHTML = `
            <div class="bubble">
              <div class="sender">${escapeHtml(name)}</div>
              <div class="text">${escapeHtml(m.content || '')}</div>
            </div>`;
                    frag.appendChild(div);
                });
                chatArea.appendChild(frag);
                chatArea.scrollTop = chatArea.scrollHeight;
            })
            .catch(() => {});
    }

    function onConnected() {
        connected = true;
        retry = 0;
        setSendingEnabled(true);
        appendMessage('', 'Đã kết nối máy chủ.', true);

        // 1) Tải lịch sử
        loadHistory();

        // 2) Nhận realtime cho đúng room
        stomp.subscribe('/topic/room.' + room, frame => {
            let m;
            try { m = JSON.parse(frame.body); } catch { return; }

            // Sự kiện hệ thống (vd: thread đã bị xóa)
            if (m && m.system) {
                if (m.event === 'cleared' && m.room === room) {
                    chatArea.innerHTML = '';
                    appendMessage('', 'Cuộc trò chuyện đã được xóa.', true);
                }
                return;
            }

            appendMessage(m.sender || 'Unknown', m.content || '');
        });
    }

    function onError() {
        connected = false;
        setSendingEnabled(false);
        const delay = Math.min(1000 * Math.pow(2, retry++), MAX_RETRY_DELAY);
        appendMessage('', `Lỗi kết nối. Sẽ thử lại sau ${Math.round(delay/1000)}s.`, true);
        setTimeout(connect, delay);
    }

    function send() {
        if (!connected || !stomp) return;
        const content = input.value.trim();
        if (!content) return;

        const payload = { senderId: meId, sender: me, content, room };
        stomp.send('/app/chat.send/' + room, {}, JSON.stringify(payload));
        input.value = '';
        input.focus();
    }

    sendBtn.addEventListener('click', send);
    input.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); send(); } });

    window.addEventListener('beforeunload', () => {
        try { if (stomp && connected) stomp.disconnect(() => {}); } catch {}
    });

    connect();
});
