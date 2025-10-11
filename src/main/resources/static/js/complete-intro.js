(function () {
    // ===== CSRF =====
    function getCsrf() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        return tokenMeta && headerMeta
            ? { header: headerMeta.getAttribute("content"), token: tokenMeta.getAttribute("content") }
            : null;
    }

    // ===== DOB selects =====
    const dSel = document.getElementById("dob-day");
    const mSel = document.getElementById("dob-month");
    const ySel = document.getElementById("dob-year");

    function fillSelects() {
        dSel.innerHTML = '<option value="">Ngày</option>';
        for (let d = 1; d <= 31; d++) {
            const opt = document.createElement("option");
            opt.value = String(d).padStart(2, "0");
            opt.textContent = d;
            dSel.appendChild(opt);
        }
        mSel.innerHTML = '<option value="">Tháng</option>';
        for (let m = 1; m <= 12; m++) {
            const opt = document.createElement("option");
            opt.value = String(m).padStart(2, "0");
            opt.textContent = m;
            mSel.appendChild(opt);
        }
        ySel.innerHTML = '<option value="">Năm</option>';
        const cur = new Date().getFullYear();
        for (let y = cur; y >= 1900; y--) {
            const opt = document.createElement("option");
            opt.value = String(y);
            opt.textContent = String(y);
            ySel.appendChild(opt);
        }
    }
    fillSelects();

    // ===== Sliders =====
    const hInput = document.getElementById("height");
    const wInput = document.getElementById("weight");
    const hOut = document.getElementById("heightVal");
    const wOut = document.getElementById("weightVal");
    const sync = () => { hOut.textContent = hInput.value; wOut.textContent = wInput.value; };
    hInput.addEventListener("input", sync);
    wInput.addEventListener("input", sync);
    sync();

    // ===== Submit =====
    document.getElementById("doneBtn")?.addEventListener("click", async () => {
        const gender = (document.querySelector('input[name="gender"]:checked') || {}).value || "";
        const d = dSel.value, m = mSel.value, y = ySel.value;
        const dob = d && m && y ? `${y}-${m}-${d}` : ""; // yyyy-MM-dd hoặc rỗng

        const payload = new URLSearchParams();
        if (hInput.value) payload.append("height", hInput.value);
        if (wInput.value) payload.append("weight", wInput.value);
        if (gender) payload.append("gender", gender);
        if (dob) payload.append("dob", dob);

        const csrf = getCsrf();
        const headers = { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" };
        if (csrf) headers[csrf.header] = csrf.token;

        try {
            const res = await fetch("/auth/complete/intro", {
                method: "POST",
                headers,
                body: payload.toString()
            });
            if (res.redirected) {
                window.location.href = res.url;       // theo redirect của server
            } else {
                window.location.href = "/user/profile"; // fallback
            }
        } catch (e) {
            console.error(e);
            alert("Có lỗi khi lưu thông tin. Vui lòng thử lại.");
        }
    });
})();
