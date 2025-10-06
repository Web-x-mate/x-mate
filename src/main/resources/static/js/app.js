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
