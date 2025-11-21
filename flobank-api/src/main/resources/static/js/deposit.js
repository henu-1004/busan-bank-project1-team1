
function openTab(evt, tabName) {
    var i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName("view-content");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }
    tablinks = document.getElementsByClassName("view-tab");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].classList.remove("active");
    }
    document.getElementById(tabName).style.display = "block";
    evt.currentTarget.classList.add("active");
}


function fetchRateData(baseDate) {
    const tableBody = document.getElementById('rateTableBody');
    if (!tableBody) return; // 테이블이 없는 페이지에서는 실행하지 않음

    // 로딩 상태 표시
    tableBody.innerHTML = `<tr><td colspan="13" class="no-data" style="padding: 30px; text-align: center; color: #999;">데이터를 불러오는 중...</td></tr>`;

    // AJAX를 사용하여 백엔드(Controller)에 요청
    fetch(`/flobank/deposit/rates?baseDate=${baseDate}`)
        .then(response => {
            if (!response.ok) {
                // HTTP 오류 상태 (404, 500 등) 처리
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="13" class="no-data" style="padding: 30px; text-align: center; color: #999;">${baseDate}에 조회된 금리 데이터가 없습니다.</td></tr>`;
                return;
            }

            let html = '';

            // 데이터를 테이블 행으로 변환
            data.forEach(item => {
                // 금리를 소수점 2자리까지 표시하는 보조 함수
                const formatRate = (rate) => {
                    if (rate == null || parseFloat(rate) === 0 || isNaN(parseFloat(rate))) {
                        return '0';
                    }
                    return parseFloat(rate).toFixed(2);
                };

                html += '<tr>';
                html += `<th>${item.currency}</th>`;
                // DTO 필드명 (rate1M ~ rate12M)을 사용하여 값 표시
                html += `<td>${formatRate(item.rate1M)}</td>`;
                html += `<td>${formatRate(item.rate2M)}</td>`;
                html += `<td>${formatRate(item.rate3M)}</td>`;
                html += `<td>${formatRate(item.rate4M)}</td>`;
                html += `<td>${formatRate(item.rate5M)}</td>`;
                html += `<td>${formatRate(item.rate6M)}</td>`;
                html += `<td>${formatRate(item.rate7M)}</td>`;
                html += `<td>${formatRate(item.rate8M)}</td>`;
                html += `<td>${formatRate(item.rate9M)}</td>`;
                html += `<td>${formatRate(item.rate10M)}</td>`;
                html += `<td>${formatRate(item.rate11M)}</td>`;
                html += `<td>${formatRate(item.rate12M)}</td>`;
                html += '</tr>';
            });

            tableBody.innerHTML = html;
        })
        .catch(error => {
            console.error('금리 데이터 조회 에러:', error);
            tableBody.innerHTML = `<tr><td colspan="13" class="no-data" style="padding: 30px; text-align: center; color: #d12a2a;">데이터를 불러오는 중 오류가 발생했습니다. (콘솔 확인)</td></tr>`;
        });
}

// ==========================================================
// 2. DOMContentLoaded 영역: 페이지 로드 후 이벤트 리스너 설정
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {


    const tabs = document.querySelectorAll('.view-tab');
    const contents = document.querySelectorAll('.view-content');

    tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));

            tab.classList.add('active');
            contents[index].classList.add('active');
        });
    });

    // 🔹 만기자동연장신청 토글
    const radioApply = document.querySelector('input[name="autoRenew"][value="apply"]');
    const radioNo = document.querySelector('input[name="autoRenew"][value="no"]');
    const extraFields = document.getElementById("autoRenewFields");

    if (radioApply && radioNo && extraFields) {
        radioApply.addEventListener("change", () => {
            if (radioApply.checked) extraFields.classList.remove("hidden");
        });
        radioNo.addEventListener("change", () => {
            if (radioNo.checked) extraFields.classList.add("hidden");
        });
    }

    // 🔹 이메일 / 문자 수령방법 전환
    const emailRadio = document.querySelector('input[name="receiveMethod"][value="email"]');
    const smsRadio = document.querySelector('input[name="receiveMethod"][value="sms"]');
    const emailFields = document.getElementById("emailFields");
    const smsHint = document.getElementById("smsHint");

    if (emailRadio && smsRadio && emailFields && smsHint) {
        emailRadio.addEventListener("change", () => {
            if (emailRadio.checked) {
                emailFields.classList.remove("hidden");
                smsHint.classList.add("hidden");
            }
        });
        smsRadio.addEventListener("change", () => {
            if (smsRadio.checked) {
                emailFields.classList.add("hidden");
                smsHint.classList.remove("hidden");
            }
        });
    }

    // 🔹 원화/외화 출금계좌 토글
    const krwRadio = document.querySelector('input[name="withdrawType"][value="krw"]');
    const fxRadio = document.querySelector('input[name="withdrawType"][value="fx"]');
    const krwFields = document.getElementById("krwFields");
    const fxFields = document.getElementById("fxFields");

    if (krwRadio && fxRadio && krwFields && fxFields) {
        krwRadio.addEventListener("change", () => {
            if (krwRadio.checked) {
                krwFields.classList.remove("hidden");
                fxFields.classList.add("hidden");
            }
        });
        fxRadio.addEventListener("change", () => {
            if (fxRadio.checked) {
                fxFields.classList.remove("hidden");
                krwFields.classList.add("hidden");
            }
        });
    }

    // 🔹 원화 출금계좌 잔액 힌트 업데이트
    const select = document.getElementById("withdrawAccount");
    const balanceHint = document.getElementById("balanceHint");

    if (select){
        select.addEventListener("change", () => {
            const selectedOption = select.options[select.selectedIndex];
            const balance = selectedOption.getAttribute("data-balance") || "0";
            console.log("선택 변경됨");
            console.log("선택된 balance = ", selectedOption.getAttribute("data-balance"));

            const formattedBalance = Number(balance).toLocaleString()
            balanceHint.textContent = `출금가능금액 ${formattedBalance}원`;
        });
    }

    // 🔹 외화 출금계좌 잔액 힌트 업데이트
    const frgnSelect = document.getElementById("withdrawFrgnAccount");
    const frgnBalanceHint = document.getElementById("frgnBalanceHint");

    if (frgnSelect){
        frgnSelect.addEventListener("change", () => {
            const fselectedOption = frgnSelect.options[frgnSelect.selectedIndex];
            const fbalance = fselectedOption.getAttribute("data-balance") || "0";
            const fcurrency = fselectedOption.getAttribute("data-currency");

            const fformattedBalance = Number(fbalance).toLocaleString()
            frgnBalanceHint.textContent = `출금가능금액 ${fformattedBalance} ${fcurrency}`;
        });
    }


    const curSelect = document.getElementById("curSelect");
    const amountInput = document.querySelector("#lmtAmtInput .form-input");

    if (curSelect && amountInput) {
        curSelect.addEventListener("change", () => {
            const opt = curSelect.options[curSelect.selectedIndex];

            const curName = opt.getAttribute("data-curName");
            const curCode = opt.getAttribute("data-curCode");
            const minAmount = opt.getAttribute("data-minAmount");
            const maxAmount = opt.getAttribute("data-maxAmount");

            if (minAmount && !isNaN(minAmount) && maxAmount && !isNaN(maxAmount)) {
                amountInput.placeholder = `${Number(minAmount).toLocaleString()}${curCode} 이상, ${Number(maxAmount).toLocaleString()}${curCode} 미만`;
            } else if (minAmount && !isNaN(minAmount)) {
                amountInput.placeholder = `${Number(minAmount).toLocaleString()}${curCode} 이상`;
            } else if (maxAmount && !isNaN(maxAmount)) {
                amountInput.placeholder = `${Number(maxAmount).toLocaleString()}${curCode} 미만`;
            } else {
                amountInput.placeholder = `${curName} 금액 입력`;
            }
        });
    }


    // 🔹 금리 조회 (날짜 변경 이벤트 리스너)
    const searchDateInput = document.getElementById('searchDate');

    if (searchDateInput) {
        // 날짜를 선택(change)하면 fetchRateData 함수(전역)를 호출합니다.
        searchDateInput.addEventListener('change', (e) => {
            const selectedDate = e.target.value;
            if (selectedDate) {
                fetchRateData(selectedDate);
            }
        });
    }
});


    const calcBtn = document.getElementById("calcBtn");
    if (calcBtn) {
      calcBtn.addEventListener("click", async () => {
        const curS = document.getElementById("curSelect");
        const curSelected = curS.options[curS.selectedIndex];
        const currencyCode = curSelected.value;
        const foreignAmountInput = document.getElementById("foreignAmount");
        const foreignAmount = document.getElementById("foreignAmount").value;

        if (!foreignAmount || isNaN(foreignAmount)) {
            alert("외화 금액을 올바르게 입력해 주세요.");
            foreignAmountInput.value = "";
            foreignAmountInput.focus();
            return;
        }

        const minAmount = Number(curSelected.getAttribute("data-minAmount"));
        const maxAmount = Number(curSelected.getAttribute("data-maxAmount"));

        // 입력값 숫자 체크
        if (!foreignAmount || isNaN(foreignAmount)) {
            alert("금액을 숫자로 입력해 주세요.");
            return;
        }

        const amt = Number(foreignAmount);

        // 🔥 범위 검사
        if ((minAmount && amt < minAmount) || (maxAmount && amt >= maxAmount)) {
            alert(`입력 가능 금액은 ${minAmount.toLocaleString()}${currencyCode} 이상 ${
                maxAmount ? maxAmount.toLocaleString() + currencyCode + " 미만" : ""
            }입니다.`);
            foreignAmountInput.value = "";
            foreignAmountInput.focus();
            return;
        }

        try {
            const res = await fetch("/flobank/deposit/calc", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    amount: foreignAmount,
                    currency : currencyCode
                })
            });


            const data = await res.json();  // 백엔드 반환값
            updateTable(data, foreignAmount);              // 값 반영

        } catch (e) {
            console.error(e);
            alert("계산 중 오류가 발생했습니다.");
        }
      });
    }

    function numberFormat(value) {
        if (value === null || value === undefined) return "0";
        return Number(value).toLocaleString("ko-KR");
    }

    function updateTable(data, foreignAmount) {
        const table = document.getElementById("calcResultTable");
        const cells = table.querySelectorAll(".prod-amt-right");

        cells[0].textContent = numberFormat(data.baseRate) + " 원";
        cells[1].textContent = numberFormat(data.appliedRate) + " 원";
        cells[2].textContent = data.prefRate + "%";
        cells[3].textContent = numberFormat(Number(data.spreadHalfPref) * Number(foreignAmount)) + " 원";
        cells[4].textContent = numberFormat(data.krwAmount) + " 원";

        // 테이블 표시
        table.style.display = "table";
    }

    const periodInput = document.getElementById("periodInput");

    if (periodInput) {
        periodInput.addEventListener("change", () => {
            const min = Number(periodInput.dataset.min);
            const max = Number(periodInput.dataset.max);
            const value = Number(periodInput.value);

            if (value < min || value > max) {
                alert(`가입 가능한 기간은 ${min}개월 이상 ${max}개월 이하입니다.`);
                periodInput.value = ""; // 입력값 초기화
                periodInput.focus();    // 다시 입력하도록 포커스 이동
            }
        });
    }

