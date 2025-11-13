// 오늘 날짜 yyyy-MM-dd
function getToday() {
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth() + 1).padStart(2, "0");
    const d = String(today.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

// yyyy-MM-dd 형식 변환
function formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

// API 조회 날짜 계산 로직
function getApiDate() {
    const now = new Date();
    const day = now.getDay();  // 0=일, 6=토
    const hour = now.getHours();
    let target = new Date(now);

    // ★ 토요일 → 금요일
    if (day === 6) {
        target.setDate(target.getDate() - 1);
        return formatDate(target);
    }

    // ★ 일요일 → 금요일
    if (day === 0) {
        target.setDate(target.getDate() - 2);
        return formatDate(target);
    }

    // ★ 월요일 오전 11시 이전 → 금요일
    if (day === 1 && hour < 11) {
        target.setDate(target.getDate() - 3);
        return formatDate(target);
    }

    // ★ 화~금 오전 11시 이전 → 전날
    if (day >= 2 && day <= 5 && hour < 11) {
        target.setDate(target.getDate() - 1);
        return formatDate(target);
    }

    // ★ 그 외(평일 11시 이후) → 오늘
    return formatDate(target);
}

let exchangeRates = [];

// 통화 환율값 추출
function getRate(currency, type) {
    const cur = exchangeRates.find(c =>
        c.cur_unit === currency || c.cur_unit.startsWith(currency)
    );

    if (!cur) return NaN;

    const toNum = (val) => {
        if (!val || val === "-" || val.trim() === "") return NaN;
        return parseFloat(val.replace(/,/g, ""));
    };

    const base = toNum(cur.deal_bas_r);
    const ttb  = toNum(cur.ttb);
    const tts  = toNum(cur.tts);

    switch (type) {
        case "매매기준율": return base;
        case "송금보낼때": return !isNaN(tts) ? tts : base;
        case "송금받을때": return !isNaN(ttb) ? ttb : base;
        default: return base;
    }
}

// 환율 계산
function calculate() {
    const from = document.getElementById("fromCurrency").value;
    const to = document.getElementById("toCurrency").value;
    const amount = parseFloat(document.getElementById("amount").value);
    const type = document.getElementById("rateStandard").value;

    if (!amount || amount <= 0) return;

    const fromRate = getRate(from, type);
    const toRate   = getRate(to, type);

    if (isNaN(fromRate) || isNaN(toRate)) {
        document.getElementById("convertedValue").innerText = "-";
        return;
    }

    if (from === to) {
        document.getElementById("convertedValue").innerText = amount.toLocaleString();
        document.querySelector(".unit").innerText = to;
        return;
    }

    const converted = amount * (fromRate / toRate);

    document.getElementById("convertedValue").innerText = converted.toFixed(2).toLocaleString();
    document.querySelector(".unit").innerText = to;
}

// 페이지 로딩 시
document.addEventListener("DOMContentLoaded", async () => {
    const apiDate = getApiDate();

    const displayDate = apiDate.replace(/-/g, ".");
    const dateTextEl = document.getElementById("rate-date-text");
    if (dateTextEl) {
        dateTextEl.innerText = `${displayDate} 기준 환율입니다.`;
    }

    const res = await fetch(`/flobank/rate/data?date=${apiDate}`);
    exchangeRates = await res.json();

    calculate();
});

// 이벤트
document.getElementById("fromCurrency").addEventListener("change", calculate);
document.getElementById("toCurrency").addEventListener("change", calculate);
document.getElementById("rateStandard").addEventListener("change", calculate);
document.getElementById("amount").addEventListener("input", calculate);
