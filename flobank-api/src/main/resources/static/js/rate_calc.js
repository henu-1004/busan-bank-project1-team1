// 오늘 날짜 yyyy-MM-dd 구하기
function getToday() {
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth() + 1).padStart(2, "0");
    const d = String(today.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
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

// 초기 로딩
document.addEventListener("DOMContentLoaded", async () => {
    const today = getToday();

    // 🔹 화면 상단에 날짜 표시: yyyy-MM-dd → yyyy.MM.dd
    const displayDate = today.replace(/-/g, ".");
    const dateTextEl = document.getElementById("rate-date-text");
    if (dateTextEl) {
        dateTextEl.innerText = `${displayDate} 기준 환율입니다.`;
    }

    // 🔹 환율 요청
    const res = await fetch(`/flobank/rate/data?date=${today}`);
    exchangeRates = await res.json();

    // 🔹 초기 계산
    calculate();
});

// 이벤트
document.getElementById("fromCurrency").addEventListener("change", calculate);
document.getElementById("toCurrency").addEventListener("change", calculate);
document.getElementById("rateStandard").addEventListener("change", calculate);
document.getElementById("amount").addEventListener("input", calculate);
