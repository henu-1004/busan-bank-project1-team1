/*
* 날짜 : 2025/11/22
* 이름 : 김대현, 이민준
* 내용 :
* 환전 기능 (좌우 입력 방식, 고시환율 기준 표시, 실시간 계산)
*/

document.addEventListener("DOMContentLoaded", () => {

    // =========================================
    // 1. DOM 요소 선택
    // =========================================
    const fromCurrency = document.getElementById("fromCurrency"); // 보내는 통화
    const toCurrency = document.getElementById("toCurrency");     // 받는 통화
    const fromAmount = document.getElementById("fromAmount");     // 입력 금액 (From)
    const toAmount = document.getElementById("toAmount");         // 환산 금액 (To - 고시환율 기준)

    const accountWon = document.getElementById("accountSelectWon");
    const accountForeign = document.getElementById("accountSelectForeign");
    const withdrawLabel = document.getElementById("withdrawLabel");
    const pwInput = document.getElementById("pwInput");

    const buySection = document.getElementById("buySection");
    const sellSection = document.getElementById("sellSection");

    // 결과 표시 요소
    const resultBaseRate = document.getElementById("result-base-rate"); // 고시환율
    const resultRate = document.getElementById("result-rate");         // 우대 적용 환율
    const resultFee = document.getElementById("result-fee");
    const resultFinal = document.getElementById("result-final");       // 최종 예상 금액
    const spreadRateDisplay = document.getElementById("spreadRateDisplay");

    // 쿠폰 모달 관련
    const couponBtn = document.getElementById("couponBtn");
    const couponModal = document.getElementById("couponModal");
    const couponClose = document.getElementById("couponClose");
    const couponOptions = document.querySelectorAll(".coupon-option");

    // 전역 변수
    let selectedCouponRate = 0; // 쿠폰 우대율 (0.5 = 50%)
    let currentBaseRate = 0;    // 기준 환율 (API 호출값)
    let currentMode = 'BUY';    // 'BUY'(원화->외화) or 'SELL'(외화->원화)

    // =========================================
    // 2. 초기화
    // =========================================
    // 휴대폰 번호 010 자동 입력
    const phoneInputs = document.querySelectorAll('.step2-input.small');
    if (phoneInputs.length >= 3) {
        phoneInputs[0].value = "010";
    }

    // 초기 화면 상태 설정 (기본 USD)
    updateUIState();
    fetchRate('USD');


    // =========================================
    // 3. 이벤트 리스너
    // =========================================

    // (1) 왼쪽(From) 통화 변경 시
    fromCurrency.addEventListener("change", () => {
        const val = fromCurrency.value;

        if (val === 'KRW') {
            // 왼쪽이 KRW -> 'BUY' 모드 (외화 사기)
            // 오른쪽을 외화 목록으로 채움
            updateSelectOptions(toCurrency, 'FOREIGN');
        } else {
            // 왼쪽이 외화 -> 'SELL' 모드 (외화 팔기)
            // 오른쪽을 KRW로 고정
            toCurrency.innerHTML = '<option value="KRW">KRW (원)</option>';
        }

        updateUIState(); // UI(계좌, 섹션) 갱신

        // 환율 가져오기 (KRW가 아닌 쪽이 외화)
        const foreignCode = (val === 'KRW') ? toCurrency.value : val;
        fetchRate(foreignCode);
    });

    // (2) 오른쪽(To) 통화 변경 시 (BUY 모드일 때만 동작)
    toCurrency.addEventListener("change", () => {
        fetchRate(toCurrency.value);
    });

    // (3) 금액 입력 시 실시간 계산
    fromAmount.addEventListener("input", () => {
        calculateExchange();
    });

    // (4) 쿠폰 모달 열기/닫기/선택
    if (couponBtn) couponBtn.addEventListener("click", () => couponModal.style.display = "flex");
    if (couponClose) couponClose.addEventListener("click", () => couponModal.style.display = "none");

    couponOptions.forEach(btn => {
        btn.addEventListener("click", () => {
            selectedCouponRate = parseFloat(btn.dataset.discount);
            // 화면에 우대율 표시 업데이트
            if(spreadRateDisplay) spreadRateDisplay.innerText = (selectedCouponRate * 100).toFixed(0);

            alert("쿠폰이 적용되었습니다.");
            couponModal.style.display = "none";
            calculateExchange(); // 쿠폰 적용 후 재계산
        });
    });


    // =========================================
    // 4. 핵심 기능 함수들
    // =========================================

    // [UI 상태 업데이트]
    function updateUIState() {
        currentMode = (fromCurrency.value === 'KRW') ? 'BUY' : 'SELL';

        if (currentMode === 'BUY') {
            // [살 때]
            withdrawLabel.innerText = '출금계좌선택 (원화)';
            accountWon.style.display = 'block';
            accountForeign.style.display = 'none';
            buySection.style.display = 'block';
            sellSection.style.display = 'none';
        } else {
            // [팔 때]
            withdrawLabel.innerText = '출금계좌선택 (외화)';
            accountWon.style.display = 'none';
            accountForeign.style.display = 'block';
            buySection.style.display = 'none';
            sellSection.style.display = 'block';
        }

        // 값 초기화
        fromAmount.value = '';
        toAmount.value = '';
        resultFinal.innerText = '0';
    }

    // [환율 정보 가져오기]
    function fetchRate(currencyCode) {
        // TODO: 실제 서버 API 호출로 변경
        /*
        fetch(`/flobank/exchange/api/rate?currency=${currencyCode}`)
            .then(res => res.json())
            .then(data => {
                currentBaseRate = data.rate;
                calculateExchange();
            });
        */

        // Mock Data (테스트용)
        const mockRates = { 'USD': 1400, 'JPY': 900, 'EUR': 1500, 'CNH': 190, 'GBP': 1700, 'AUD': 900 };
        currentBaseRate = mockRates[currencyCode] || 1000;

        calculateExchange();
    }

    // [환율 계산 로직 - 핵심]
    function calculateExchange() {
        if (!currentBaseRate || !fromAmount.value) {
            toAmount.value = '';
            return;
        }

        const amount = parseFloat(fromAmount.value);
        const spreadRate = 0.0175; // 기본 수수료율 (1.75%)
        const appliedSpread = spreadRate * (1 - selectedCouponRate); // 우대율 적용된 수수료
        const foreignCode = (currentMode === 'BUY') ? toCurrency.value : fromCurrency.value;

        // -----------------------------------------------------
        // 1. 인풋박스용 (To Amount): 순수 고시환율 기준 계산
        //    (쿠폰/수수료 미적용 금액을 보여줌)
        // -----------------------------------------------------
        let baseVal = 0;
        if (currentMode === 'BUY') {
            // 원화 -> 외화 (나누기)
            // JPY, CNH는 100단위
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                baseVal = amount / (currentBaseRate / 100);
            } else {
                baseVal = amount / currentBaseRate;
            }
        } else {
            // 외화 -> 원화 (곱하기)
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                baseVal = amount * (currentBaseRate / 100);
            } else {
                baseVal = amount * currentBaseRate;
            }
        }

        // 인풋박스 업데이트
        if (currentMode === 'BUY') {
            toAmount.value = baseVal.toFixed(2); // 외화는 소수점
        } else {
            toAmount.value = Math.floor(baseVal).toLocaleString(); // 원화는 정수
        }


        // -----------------------------------------------------
        // 2. 결과창용 (Final Result): 실제 거래 환율(우대적용) 계산
        // -----------------------------------------------------
        let finalRate = 0; // 적용 환율
        let resultVal = 0; // 최종 금액

        if (currentMode === 'BUY') {
            // 살 때: 환율 = 기준율 + 수수료
            finalRate = currentBaseRate * (1 + appliedSpread);

            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                resultVal = amount / (finalRate / 100);
            } else {
                resultVal = amount / finalRate;
            }
        } else {
            // 팔 때: 환율 = 기준율 - 수수료
            finalRate = currentBaseRate * (1 - appliedSpread);

            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                resultVal = amount * (finalRate / 100);
            } else {
                resultVal = amount * finalRate;
            }
        }

        // 결과창 업데이트
        resultBaseRate.innerText = `${currentBaseRate.toLocaleString()} 원`; // 고시환율
        resultRate.innerText = `${finalRate.toFixed(2)} 원`;               // 적용환율

        // 최종 금액 표시
        if (currentMode === 'BUY') {
            resultFinal.innerText = `${resultVal.toFixed(2)} ${foreignCode}`;
        } else {
            resultFinal.innerText = `${Math.floor(resultVal).toLocaleString()} KRW`;
        }

        // 수수료율 정보
        if (resultFee) {
            resultFee.innerText = `${(spreadRate * 100).toFixed(2)}% → ${(appliedSpread * 100).toFixed(2)}%`;
        }
    }

    // [드롭다운 옵션 생성 헬퍼]
    function updateSelectOptions(selectElement, type) {
        selectElement.innerHTML = '';
        if (type === 'FOREIGN') {
            const currencies = [
                {val:'USD', txt:'USD (달러)'}, {val:'JPY', txt:'JPY (엔)'},
                {val:'EUR', txt:'EUR (유로)'}, {val:'CNH', txt:'CNH (위안)'},
                {val:'GBP', txt:'GBP (파운드)'}, {val:'AUD', txt:'AUD (호주달러)'}
            ];
            currencies.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.val; opt.text = c.txt;
                selectElement.appendChild(opt);
            });
        }
    }

    // =========================================
    // 5. 환전 신청 (서버 전송)
    // =========================================
    const nextBtnStep2 = document.querySelector(".step2-btn-next");

    if (nextBtnStep2) {
        nextBtnStep2.addEventListener("click", () => {

            // 유효성 검사
            if (!fromAmount.value || fromAmount.value <= 0) { alert("금액을 입력해주세요."); return; }
            if (!pwInput.value) { alert("비밀번호를 입력해주세요."); return; }

            // 출금 계좌 확인
            let selectedAccount = (currentMode === 'BUY') ? accountWon.value : accountForeign.value;
            if (!selectedAccount) { alert("출금 계좌를 선택해주세요."); return; }

            // 비밀번호 확인 요청
            const requestData = {
                acctNo: selectedAccount,
                acctPw: pwInput.value.trim()
            };

            fetch('/flobank/exchange/passcheck', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestData)
            })
                .then(res => res.json())
                .then(data => {
                    if (data.status === 'success') {
                        submitExchangeRequest(selectedAccount);
                    } else {
                        alert(data.message || "비밀번호가 일치하지 않습니다.");
                        pwInput.value = "";
                        pwInput.focus();
                    }
                })
                .catch(error => {
                    console.error("Error:", error);
                    alert("시스템 오류가 발생했습니다.");
                });
        });
    }

    // 실제 데이터 전송 함수
    function submitExchangeRequest(sourceAcctNo) {
        const appliedRateText = document.getElementById("result-rate").innerText.replace(/[^0-9.]/g, "");
        const foreignCode = (currentMode === 'BUY') ? toCurrency.value : fromCurrency.value;

        // DB 저장 로직
        // BUY(살 때): 최종적으로 받는 외화 금액 (수수료 적용 후 금액) -> resultFinal 파싱
        // SELL(팔 때): 판매한 외화 금액 (입력 금액 그대로) -> fromAmount
        let finalForeignAmt = 0;

        if(currentMode === 'BUY') {
            // 예: "100.00 USD" 에서 숫자만 추출
            finalForeignAmt = parseFloat(document.getElementById("result-final").innerText.replace(/[^0-9.]/g, ""));
        } else {
            finalForeignAmt = parseFloat(fromAmount.value);
        }

        let exchangeData = {
            exchAcctNo: sourceAcctNo,
            exchToCurrency: foreignCode,
            exchAmount: finalForeignAmt, // 실제 거래되는 외화 양
            exchAppliedRate: parseFloat(appliedRateText),
            exchEsignYn: 'Y',
            exchType: currentMode
        };

        // 주소 및 수령일 설정
        if (currentMode === 'BUY') {
            const branchSelect = document.getElementById("receiveBranch");
            if (!branchSelect.value) { alert("수령 지점을 선택해주세요."); return; }

            const branchName = branchSelect.options[branchSelect.selectedIndex].text;
            const method = document.getElementById("receiveMethod").value;

            exchangeData.exchAddr = `${branchName} (${method === 'ATM' ? 'ATM' : '영업점'})`;
            exchangeData.exchExpDy = document.getElementById("receiveDate").value;

            if (!exchangeData.exchExpDy) { alert("수령일을 선택해주세요."); return; }

        } else {
            const depositAcct = document.getElementById("depositAccountSelect").value;
            exchangeData.exchAddr = `즉시입금:${depositAcct}`;
            exchangeData.exchExpDy = new Date().toISOString().split("T")[0];
        }

        // 최종 요청
        fetch('/flobank/exchange/process', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(exchangeData)
        })
            .then(res => res.json())
            .then(resData => {
                if (resData.status === 'success') {
                    alert("환전 처리가 완료되었습니다.");
                    window.location.href = "/flobank/exchange/step3";
                } else {
                    alert("처리 실패: " + resData.message);
                }
            })
            .catch(err => {
                console.error("Exchange Process Error:", err);
                alert("통신 오류가 발생했습니다.");
            });
    }

    // 수령 방법 버튼 클릭 UI 이벤트 (기존 유지)
    const methodBtns = document.querySelectorAll('.method-btn');
    const receiveMethodInput = document.getElementById('receiveMethod');

    if(methodBtns) {
        methodBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                methodBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                receiveMethodInput.value = btn.getAttribute('data-value');
            });
        });
    }
});