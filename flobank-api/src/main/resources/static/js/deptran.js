document.addEventListener('DOMContentLoaded', function() {
    const calcBtn = document.getElementById("calcBtn");
    if (calcBtn) {
        calcBtn.addEventListener("click", async () => {

            const rateType = document.getElementById("rateType").value;   // 🔥 추가
            const currencyCode = document.getElementById("productCurrency").value;
            const dpstAcctNo = document.getElementById("dpstAcctNo").value;
            const foreignAmount = document.getElementById("foreignAmount").value;



            try {
                let url = "";

                let body ;
                if (rateType === "1") {
                    // 가입시점 환율 (db)
                    url = `/flobank/mypage/calcRate`;
                    body = JSON.stringify({
                        amount: foreignAmount,
                        currency : currencyCode,
                        dpstAcctNo : dpstAcctNo
                    })
                } else {
                    // 납입시점 환율 (api)
                    url = `/flobank/deposit/calc`;
                    body = JSON.stringify({
                        amount: foreignAmount,
                        currency : currencyCode
                    })
                }

                const res = await fetch(url, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: body
                });

                const data = await res.json();
                if (rateType==="3"){
                    updateTable(data, foreignAmount, currencyCode);
                }else {
                    updateStTable(data, foreignAmount, currencyCode);
                }

            } catch (e) {
                alert("환율 계산 중 오류 발생");
            }
        });
    }

    function numberFormat(value) {
        if (value === null || value === undefined) return "0";
        return Number(value).toLocaleString("ko-KR");
    }

    function updateTable(data, foreignAmount, curName) {
        const table = document.getElementById("calcResultTable");

        table.innerHTML = `
           <input type="hidden" name="selectedCurName" value="${curName}">
        <tr>
                <td class="prod-amt-left">
                    송금보내실때환율
                </td>
                <td class="prod-amt-right">
                    ${numberFormat(data.baseRate)} 원
                </td>
            </tr>
            <tr>
                <td class="prod-amt-left">
                    우대적용환율
                </td>
                <td class="prod-amt-right">
                    <input type="hidden" name="dpstDtlAppliedRate" value="${Number(data.appliedRate)}">
                    ${numberFormat(data.appliedRate)} 원
                </td>
            </tr>
            <tr>
                <td class="prod-amt-left">
                    우대율
                </td>
                <td class="prod-amt-right">
                    ${data.prefRate}%
                </td>
            </tr>
            <tr>
                <td class="prod-amt-left">
                    우대받는금액
                </td>
                <td class="prod-amt-right">
                    ${numberFormat(Number(data.spreadHalfPref) * Number(foreignAmount))} 원
                </td>
            </tr>
            <tr>
                <td class="prod-amt-left">
                    예상원화금액
                </td>
                <td class="prod-amt-right" style="color: #ef0909; font-weight: bold">
                    <input type="hidden" id="hiddenTranAmount" name="tranAmount" value="${Number(data.krwAmount)}">
                    ${numberFormat(data.krwAmount)} 원
                </td>
            </tr>
            <tr>
                <td class="prod-amt-left" colspan="2" style="color: gray">
                    상기 예상금액은 실제 가입 시점의 환율 변동에 따라 달라질 수 있습니다.(수수료 미포함)
                </td>
            </tr>
        `;


        // 테이블 표시
        table.style.display = "table";
    }

    function updateStTable(data, foreignAmount, curName) {
        const table = document.getElementById("calcResultTable");

        table.innerHTML = `
           <input type="hidden" name="selectedCurName" value="${curName}">
            <tr>
                <td class="prod-amt-left">
                    가입시점환율
                </td>
                <td class="prod-amt-right">
                    <input type="hidden" name="dpstDtlAppliedRate" value="${Number(data.appliedRate)}">
                    ${numberFormat(data.appliedRate)} 원
                </td>
            </tr>

            <tr>
                <td class="prod-amt-left">
                    예상원화금액
                </td>
                <td class="prod-amt-right" style="color: #ef0909; font-weight: bold">
                    <input type="hidden" id="hiddenTranAmount" name="tranAmount" value="${Number(data.krwAmount)}">
                    ${numberFormat(data.krwAmount)} 원
                </td>
            </tr>
            <tr>
                <td class="prod-amt-left" colspan="2" style="color: gray">
                    상기 예상금액은 실제 가입 시점의 환율 변동에 따라 달라질 수 있습니다.(수수료 미포함)
                </td>
            </tr>
        `;


        // 테이블 표시
        table.style.display = "table";
    }









    const form = document.getElementById('transferForm');


    const tranRecAcctNoInput = document.getElementById('tranRecAcctNo');
    const tranRecBkCodeInput = document.getElementById('tranRecBkCode'); // [추가] 은행 코드 선택값 가져오기 위함

    const accountBalanceInput = document.getElementById('accountBalance');

    const tranAmountError = document.getElementById('tranAmountError');
    const tranRecAcctNoError = document.getElementById('tranRecAcctNoError');

    // [기능 1] 폼 제출 시 유효성 검사 및 서버 계좌 확인
    form.addEventListener('submit', async function(event) {
        // 1. 일단 폼의 자동 제출을 막습니다.
        event.preventDefault();
        const hiddenTranAmount = document.getElementById('hiddenTranAmount'); // 실제 전송용 (Hidden)
        const frgnAmountInput = document.getElementById('foreignAmount');      // 보여지는 입력창 (Text)

        let isValid = true;

        const balance = accountBalanceInput ? Number(accountBalanceInput.value) : 0;

        // 입력창의 값에서 콤마를 제거하고 숫자로 변환
        if (frgnAmountInput){
            const rawAmountStr = Number(frgnAmountInput.value);
            const inputAmount = Number(rawAmountStr);

            // Hidden 필드에 실제 숫자값 동기화 (전송용)
            hiddenTranAmount.value = rawAmountStr;

            resetError(frgnAmountInput, tranAmountError);
            resetError(tranRecAcctNoInput, tranRecAcctNoError);

            // --- [클라이언트 측 검사 시작] ---

            // 1. 이체 금액 확인
            if (!rawAmountStr || inputAmount <= 0) {
                showError(frgnAmountInput, tranAmountError, "이체하실 금액을 입력해주세요.");
                isValid = false;
            }
            else if (inputAmount > balance) {
                showError(frgnAmountInput, tranAmountError, "이체 금액이 잔액을 초과할 수 없습니다.");
                isValid = false;
            }
        }

        // 2. 입금 계좌번호 입력 확인
        if (!tranRecAcctNoInput.value.trim()) {
            showError(tranRecAcctNoInput, tranRecAcctNoError, "입금받으실 계좌번호를 입력해주세요.");
            if (isValid) {
                tranRecAcctNoInput.focus();
            }
            isValid = false;
        }

        // 클라이언트 검사에서 실패하면 종료
        if (!isValid) {
            return false;
        }

        // --- [서버 측 계좌 실존 여부 확인 (AJAX)] ---



        try {
            // CSRF 토큰 가져오기 (Spring Security 사용 시 필수)
            // _template/_header.html <head> 내에 <meta name="_csrf" ...> 태그가 있어야 함
            const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
            const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

            const headers = {
                'Content-Type': 'application/json'
            };

            if (csrfTokenMeta && csrfHeaderMeta) {
                headers[csrfHeaderMeta.content] = csrfTokenMeta.content;
            }

            form.submit();

        } catch (error) {
            console.error('Error:', error);
            alert("계좌 확인 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    });



    if (tranRecAcctNoInput){
        tranRecAcctNoInput.addEventListener('input', function() {
            if (this.value.trim()) {
                resetError(this, tranRecAcctNoError);
            }
        });
    }
});

// --- [콤마 관련 유틸리티 함수] ---

function comma(str) {
    str = String(str);
    return str.replace(/(\d)(?=(?:\d{3})+(?!\d))/g, '$1,');
}

function uncomma(str) {
    str = String(str);
    return str.replace(/[^\d]+/g, '');
}

function inputNumberFormat(obj) {
    obj.value = comma(uncomma(obj.value));
    const hiddenInput = document.getElementById('hiddenTranAmount');
    if(hiddenInput) {
        hiddenInput.value = uncomma(obj.value);
    }
}


// --- [금액 조작 함수] ---

function addAmount(amount) {
    const input = document.getElementById('tranAmount');
    const errorDiv = document.getElementById('tranAmountError');
    const accountBalanceInput = document.getElementById('accountBalance');

    let currentVal = Number(uncomma(input.value)) || 0;
    let balance = accountBalanceInput ? Number(accountBalanceInput.value) : 0;

    let newVal = currentVal + amount;

    if (newVal > balance) {
        showError(input, errorDiv, "이체 금액이 잔액을 초과하여 입력할 수 없습니다.");
        input.focus();
        return;
    }

    input.value = comma(newVal);
    document.getElementById('hiddenTranAmount').value = newVal;

    input.classList.remove('input-error');
    if(errorDiv) errorDiv.style.display = 'none';
}

function setFullAmount(balance) {
    const input = document.getElementById('tranAmount');
    const errorDiv = document.getElementById('tranAmountError');

    input.value = comma(balance);
    document.getElementById('hiddenTranAmount').value = balance;

    input.classList.remove('input-error');
    if(errorDiv) errorDiv.style.display = 'none';
}

// [헬퍼] 에러 표시
function showError(inputElement, errorElement, message) {
    inputElement.classList.add('input-error');
    errorElement.textContent = message;
    errorElement.style.display = 'block';
}

// [헬퍼] 에러 초기화
function resetError(inputElement, errorElement) {
    inputElement.classList.remove('input-error');
    errorElement.style.display = 'none';
}