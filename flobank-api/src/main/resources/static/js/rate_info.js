




function getToday() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

//  테이블에 환율 데이터 렌더링 함수
function renderRateTable(data) {
    const tbody = document.querySelector(".rateinfo-table tbody");
    tbody.innerHTML = "";

    data.forEach(item => {
        const row = `
            <tr>
                <td>${item.cur_unit}</td>
                <td>${item.cur_nm}</td>
                <td>${item.ttb || "-"}</td>
                <td>${item.tts || "-"}</td>
                <td>${item.deal_bas_r}</td>
                <td>${item.bkpr}</td>
                <td>${item.kftc_deal_bas_r || "-"}</td>
                <td>${item.kftc_bkpr || "-"}</td>
            </tr>
        `;
        tbody.insertAdjacentHTML("beforeend", row);
    });
}

//  날짜 기반으로 환율 요청 함수
async function loadRate(date) {
    try {
        const res = await fetch(`/flobank/rate/data?date=${date}`);
        const data = await res.json();
        renderRateTable(data);
    } catch (err) {
        console.error(err);
        alert("환율 정보를 불러올 수 없습니다.");
    }
}

//  페이지 로드 시 오늘 날짜 설정 + 자동 요청
document.addEventListener("DOMContentLoaded", () => {
    const today = getToday();

    // input type=date 에 오늘날짜 넣기
    document.querySelector("#rateinfo-date").value = today;

    // 오늘 날짜 환율 자동 로드
    loadRate(today);
});

//  조회 버튼 클릭 시 선택 날짜로 요청
document.querySelector(".rateinfo-btn-search").addEventListener("click", function () {
    const date = document.querySelector("#rateinfo-date").value;
    loadRate(date);
});