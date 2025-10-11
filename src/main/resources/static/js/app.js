document.addEventListener('DOMContentLoaded', () => {
    console.log('UI ready');

    // Lưu & phục hồi trạng thái <details> theo localStorage
    document.querySelectorAll('.sidebar .mgroup').forEach(d => {
        const key = d.getAttribute('data-key');
        const saved = localStorage.getItem(key);
        if (saved !== null) d.open = saved === '1';
        d.addEventListener('toggle', () => {
            localStorage.setItem(key, d.open ? '1' : '0');
        });
    });
});


// (function(){
//     try{
//         const sock = new SockJS('/ws');
//         const stomp = Stomp.over(sock);
//         stomp.connect({}, () => {
//             stomp.subscribe('/topic/notice', (msg)=> console.log('WS:', msg.body));
//         });
//     }catch(e){}
// })();