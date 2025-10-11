(function(){
    try{
        const sock = new SockJS('/ws');
        const stomp = Stomp.over(sock);
        stomp.connect({}, () => {
            stomp.subscribe('/topic/notice', (msg)=> console.log('WS:', msg.body));
        });
    }catch(e){}
})();
