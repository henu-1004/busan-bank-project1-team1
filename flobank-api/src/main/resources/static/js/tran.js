document.addEventListener('DOMContentLoaded', function() {

    const form = document.getElementById('transferForm');
    const tranAmountInput = document.getElementById('tranAmount');      // 보여지는 입력창 (Text)
    const hiddenTranAmount = document.getElementById('hiddenTranAmount'); // 실제 전송용 (Hidden)
    const tranRecAcctNoInput = document.getElementById('tranRecAcctNo');

    const accountBalanceInput = document.getElementById('accountBalance');

    const tranAmountError = document.getElementById('tranAmountError');
    const tranRecAcctNoError = document.getElementById('tranRecAcctNoError');

    // [기능 1] 폼 제출 시 유효성 검사
    form.addEventListener('submit', function(event) {
        let isValid = true;

        const balance = accountBalanceInput ? Number(accountBalanceInput.value) : 0;

        // 입력창의 값에서 콤마를 제거하고 숫자로 변환
        const rawAmountStr = uncomma(tranAmountInput.value);
        const inputAmount = Number(rawAmountStr);

        // Hidden 필드에 실제 숫자값 동기화 (전송용)
        hiddenTranAmount.value = rawAmountStr;

        resetError(tranAmountInput, tranAmountError);
        resetError(tranRecAcctNoInput, tranRecAcctNoError);

        // 1. 이체 금액 확인
        if (!rawAmountStr || inputAmount <= 0) {
            showError(tranAmountInput, tranAmountError, "이체하실 금액을 입력해주세요.");
            isValid = false;
        }
        else if (inputAmount > balance) {
            showError(tranAmountInput, tranAmountError, "이체 금액이 잔액을 초과할 수 없습니다.");
            isValid = false;
        }

        // 2. 입금 계좌번호 확인
        if (!tranRecAcctNoInput.value.trim()) {
            showError(tranRecAcctNoInput, tranRecAcctNoError, "입금받으실 계좌번호를 입력해주세요.");
            if (isValid) {
                tranRecAcctNoInput.focus();
            }
            isValid = false;
        } else {
            if (!isValid) {
                tranAmountInput.focus();
            }
        }

        if (!isValid) {
            event.preventDefault();
            return false;
        }
    });

    // [기능 2] 에러 숨기기 (입력 시)
    tranAmountInput.addEventListener('input', function() {
        // 콤마 포맷팅 적용 (아래 inputNumberFormat 함수가 onkeyup으로도 호출되지만, input 이벤트에서도 처리)
        inputNumberFormat(this);

        const currentVal = Number(uncomma(this.value));
        if (currentVal > 0) {
            resetError(this, tranAmountError);
        }
    });

    tranRecAcctNoInput.addEventListener('input', function() {
        if (this.value.trim()) {
            resetError(this, tranRecAcctNoError);
        }
    });
});

// --- [콤마 관련 유틸리티 함수] ---

// 1. 콤마 찍기
function comma(str) {
    str = String(str);
    return str.replace(/(\d)(?=(?:\d{3})+(?!\d))/g, '$1,');
}

// 2. 콤마 풀기
function uncomma(str) {
    str = String(str);
    return str.replace(/[^\d]+/g, '');
}

// 3. 입력 시 포맷팅 적용 (HTML onkeyup 등에서 호출)
function inputNumberFormat(obj) {
    obj.value = comma(uncomma(obj.value));
    // Hidden 필드 값 동기화
    const hiddenInput = document.getElementById('hiddenTranAmount');
    if(hiddenInput) {
        hiddenInput.value = uncomma(obj.value);
    }
}


// --- [금액 조작 함수] ---

// [기능 3] 금액 더하기
function addAmount(amount) {
    const input = document.getElementById('tranAmount');
    const errorDiv = document.getElementById('tranAmountError');
    const accountBalanceInput = document.getElementById('accountBalance');

    // 현재 값 가져오기 (콤마 제거 후 숫자 변환)
    let currentVal = Number(uncomma(input.value)) || 0;
    let balance = accountBalanceInput ? Number(accountBalanceInput.value) : 0;

    let newVal = currentVal + amount;

    // 잔액 초과 체크
    if (newVal > balance) {
        showError(input, errorDiv, "이체 금액이 잔액을 초과하여 입력할 수 없습니다.");
        input.focus();
        return;
    }

    // 값 업데이트 (콤마 적용해서 보여주기)
    input.value = comma(newVal);

    // Hidden 필드 동기화
    document.getElementById('hiddenTranAmount').value = newVal;

    input.classList.remove('input-error');
    if(errorDiv) errorDiv.style.display = 'none';
}

// [기능 4] 전액 버튼
function setFullAmount(balance) {
    const input = document.getElementById('tranAmount');
    const errorDiv = document.getElementById('tranAmountError');

    // 잔액을 콤마 찍어서 입력
    input.value = comma(balance);

    // Hidden 필드 동기화
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