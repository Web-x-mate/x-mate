(function () {
    function togglePwd() {
        var i = document.getElementById('pwd');
        var t = document.getElementById('toggleEye');
        var showing = i.type === 'text';
        i.type = showing ? 'password' : 'text';
        t.textContent = showing ? 'Hiện' : 'Ẩn';
    }

    document.addEventListener('DOMContentLoaded', function () {
        var eye = document.getElementById('toggleEye');
        if (eye) eye.addEventListener('click', togglePwd);

        var form = document.getElementById('loginForm');
        if (form) {
            form.addEventListener('submit', function (e) {
                // Nếu muốn xử lý AJAX, bỏ comment 2 dòng dưới và tự gọi API
                // e.preventDefault();
                // axios.post('/api/auth/login', { ... })
            });
        }
    });
})();
