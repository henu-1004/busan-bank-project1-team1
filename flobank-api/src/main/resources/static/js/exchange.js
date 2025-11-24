/*
* 날짜 : 2025/11/22
* 이름 : 김대현, 이민준
* 내용 : 환전 기능 (변수 중복 제거, 동적 data-flag 적용 완료)
*/

document.addEventListener("DOMContentLoaded", () => {

    // =========================================
    // 1. DOM 요소 선택 (중복 선언 통합 완료)
    // =========================================
    const fromCurrency = document.getElementById("fromCurrency");
    const toCurrency = document.getElementById("toCurrency");
    const fromAmount = document.getElementById("fromAmount");
    const toAmount = document.getElementById("toAmount");

    const receiveDateInput = document.getElementById("receiveDate");

    const fromFlag = document.getElementById("fromFlag"); // 국기 이미지 태그
    const toFlag = document.getElementById("toFlag");     // 국기 이미지 태그

    const accountWon = document.getElementById("accountSelectWon");
    const accountForeign = document.getElementById("accountSelectForeign");
    const withdrawLabel = document.getElementById("withdrawLabel");
    const pwInput = document.getElementById("pwInput");

    const buySection = document.getElementById("buySection");
    const sellSection = document.getElementById("sellSection");

    const resultBaseRate = document.getElementById("result-base-rate");
    const resultRate = document.getElementById("result-rate");
    const resultFee = document.getElementById("result-fee");
    const resultFinal = document.getElementById("result-final");
    const spreadRateDisplay = document.getElementById("spreadRateDisplay");

    const couponBtn = document.getElementById("couponBtn");
    const couponModal = document.getElementById("couponModal");
    const couponClose = document.getElementById("couponClose");
    const couponOptions = document.querySelectorAll(".coupon-option");

    // 전역 변수
    let selectedCouponRate = 0;
    let currentBaseRate = 0;
    let currentMode = 'BUY'; // 'BUY' or 'SELL'

    // =========================================
    // 2. 초기화 및 헬퍼 함수
    // =========================================

    // [핵심 함수] 선택된 옵션의 data-flag 값을 읽어서 이미지 변경
    function changeFlag(selectElement, imgElement) {
        if (!selectElement || !imgElement) return;

        // 현재 선택된 option 태그 찾기
        const selectedOption = selectElement.options[selectElement.selectedIndex];

        // option 태그에 심어둔 data-flag 값 가져오기
        if (selectedOption) {
            const flagPath = selectedOption.getAttribute('data-flag');
            // 이미지 경로 교체
            if (flagPath) {
                imgElement.src = flagPath;
            }
        }
    }

    // 휴대폰 번호 초기화
    const phoneInputs = document.querySelectorAll('.step2-input.small');
    if (phoneInputs.length >= 3) {
        phoneInputs[0].value = "010";
    }

    // ---------------------------------------------------------
    // 수령일(receiveDate) 과거 날짜 선택 방지
    // ---------------------------------------------------------
    if (receiveDateInput) {
        const today = new Date();
        const year = today.getFullYear();
        // 월과 일은 2자리(01, 02...)로 맞춰야 input type="date"가 인식함
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');

        const minDate = `${year}-${month}-${day}`; // 예: "2025-11-24"

        // min 속성에 오늘 날짜 할당 -> 오늘 이전 날짜는 선택 불가(회색 처리)됨
        receiveDateInput.setAttribute("min", minDate);

        // (선택사항) 기본값을 오늘로 설정하고 싶다면 아래 주석 해제
        // receiveDateInput.value = minDate;
    }

    // 초기 화면 설정
    updateUIState();

    // 초기 국기 설정 (HTML에 설정된 data-flag 기반)
    changeFlag(fromCurrency, fromFlag);
    changeFlag(toCurrency, toFlag);

    // 초기 환율 가져오기
    fetchRate(toCurrency.value);


    // =========================================
    // 3. 이벤트 리스너
    // =========================================

    // (1) 왼쪽(From) 통화 변경 시
    fromCurrency.addEventListener("change", () => {
        const val = fromCurrency.value;
        changeFlag(fromCurrency, fromFlag); // 국기 업데이트

        if (val === 'KRW') {
            // 원화 -> 외화 (BUY 모드)
            updateSelectOptions(toCurrency, 'FOREIGN');
        } else {
            // 외화 -> 원화 (SELL 모드)
            // 오른쪽을 KRW로 고정하면서 data-flag도 같이 넣어줘야 함
            toCurrency.innerHTML = '';
            const opt = document.createElement('option');
            opt.value = "KRW";
            opt.text = "KRW (대한민국 원)";
            opt.setAttribute("data-flag", "/flobank/images/krw.png"); // 이미지 경로 주의
            toCurrency.appendChild(opt);
        }

        // 오른쪽 국기도 업데이트 (옵션 내용이 바뀌었으므로)
        changeFlag(toCurrency, toFlag);

        updateUIState();

        const foreignCode = (val === 'KRW') ? toCurrency.value : val;
        fetchRate(foreignCode);
    });

    // (2) 오른쪽(To) 통화 변경 시
    toCurrency.addEventListener("change", () => {
        changeFlag(toCurrency, toFlag); // 국기 업데이트
        fetchRate(toCurrency.value);
    });

    // (3) 금액 입력 시 실시간 계산
    fromAmount.addEventListener("input", () => {
        calculateExchange();
    });

    // (4) 쿠폰 관련
    if (couponBtn) couponBtn.addEventListener("click", () => couponModal.style.display = "flex");
    if (couponClose) couponClose.addEventListener("click", () => couponModal.style.display = "none");
    couponOptions.forEach(btn => {
        btn.addEventListener("click", () => {
            selectedCouponRate = parseFloat(btn.dataset.discount);
            if(spreadRateDisplay) spreadRateDisplay.innerText = (selectedCouponRate * 100).toFixed(0);
            alert("쿠폰이 적용되었습니다.");
            couponModal.style.display = "none";
            calculateExchange();
        });
    });


    // =========================================
    // 4. 기능 함수들
    // =========================================

    function updateUIState() {
        currentMode = (fromCurrency.value === 'KRW') ? 'BUY' : 'SELL';

        if (currentMode === 'BUY') {
            withdrawLabel.innerText = '출금계좌선택 (원화)';
            accountWon.style.display = 'block';
            accountForeign.style.display = 'none';
            buySection.style.display = 'block';
            sellSection.style.display = 'none';
        } else {
            withdrawLabel.innerText = '출금계좌선택 (외화)';
            accountWon.style.display = 'none';
            accountForeign.style.display = 'block';
            buySection.style.display = 'none';
            sellSection.style.display = 'block';
        }

        calculateExchange();
    }

    // [서버/Redis API 호출]
    function fetchRate(currencyCode) {
        // 실제 API 호출
        fetch(`/flobank/exchange/api/rate?currency=${currencyCode}`)
            .then(res => {
                if (!res.ok) throw new Error('Network response was not ok');
                return res.json();
            })
            .then(data => {
                if (data && data.rate) {
                    currentBaseRate = parseFloat(data.rate);
                    calculateExchange();
                } else {
                    console.error("Invalid rate data received");
                }
            })
            .catch(err => {
                console.error("Error fetching rate:", err);
            });
    }

    function calculateExchange() {
        if (!currentBaseRate || !fromAmount.value) {
            toAmount.value = '';
            if(!fromAmount.value) resultFinal.innerText = '0';
            return;
        }

        const amount = parseFloat(fromAmount.value);
        const spreadRate = 0.0175;
        const appliedSpread = spreadRate * (1 - selectedCouponRate);
        const foreignCode = (currentMode === 'BUY') ? toCurrency.value : fromCurrency.value;

        // 1. 단순 환산 (Input 박스용)
        let baseVal = 0;
        if (currentMode === 'BUY') {
            // 원화 -> 외화
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                baseVal = amount / (currentBaseRate / 100);
            } else {
                baseVal = amount / currentBaseRate;
            }
        } else {
            // 외화 -> 원화
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                baseVal = amount * (currentBaseRate / 100);
            } else {
                baseVal = amount * currentBaseRate;
            }
        }

        if (currentMode === 'BUY') {
            toAmount.value = baseVal.toFixed(2);
        } else {
            toAmount.value = Math.floor(baseVal).toLocaleString();
        }

        // 2. 우대율 적용 최종 금액 (결과창용)
        let finalRate = 0;
        let resultVal = 0;

        if (currentMode === 'BUY') {
            finalRate = currentBaseRate * (1 + appliedSpread);
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                resultVal = amount / (finalRate / 100);
            } else {
                resultVal = amount / finalRate;
            }
        } else {
            finalRate = currentBaseRate * (1 - appliedSpread);
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                resultVal = amount * (finalRate / 100);
            } else {
                resultVal = amount * finalRate;
            }
        }

        // 화면 표시
        resultBaseRate.innerText = `${currentBaseRate.toLocaleString()} 원`;
        resultRate.innerText = `${finalRate.toFixed(2)} 원`;

        if (currentMode === 'BUY') {
            resultFinal.innerText = `${resultVal.toFixed(2)} ${foreignCode}`;
        } else {
            resultFinal.innerText = `${Math.floor(resultVal).toLocaleString()} KRW`;
        }

        if (resultFee) {
            resultFee.innerText = `${(spreadRate * 100).toFixed(2)}% → ${(appliedSpread * 100).toFixed(2)}%`;
        }
    }

    // [드롭다운 옵션 생성 시 data-flag 속성 추가 필수]
    function updateSelectOptions(selectElement, type) {
        selectElement.innerHTML = '';
        if (type === 'FOREIGN') {
            const currencies = [
                {val:'USD', txt:'USD (미국 달러)'},
                {val:'JPY', txt:'JPY (일본 엔)'},
                {val:'EUR', txt:'EUR (유럽 유로)'},
                {val:'CNH', txt:'CNH (중국 위안)'},
                {val:'GBP', txt:'GBP (영국 파운드)'},
                {val:'AUD', txt:'AUD (호주 달러)'}
            ];

            currencies.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.val;
                opt.text = c.txt;

                // [중요] 동적으로 생성할 때도 이미지 경로를 넣어줘야 함!
                // 파일명이 소문자라고 가정 (예: USD -> /images/usd.png)
                opt.setAttribute('data-flag', `/flobank/images/${c.val.toLowerCase()}.png`);

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
            if (!fromAmount.value || fromAmount.value <= 0) { alert("금액을 입력해주세요."); return; }
            if (!pwInput.value) { alert("비밀번호를 입력해주세요."); return; }

            let selectedAccount = (currentMode === 'BUY') ? accountWon.value : accountForeign.value;
            if (!selectedAccount) { alert("출금 계좌를 선택해주세요."); return; }

            // 수령일 필수 선택 체크 (BUY 모드일 때만)
            if (currentMode === 'BUY' && !receiveDateInput.value) {
                alert("수령 희망일을 선택해주세요.");
                return;
            }

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

    function submitExchangeRequest(sourceAcctNo) {
        const appliedRateText = document.getElementById("result-rate").innerText.replace(/[^0-9.]/g, "");
        const foreignCode = (currentMode === 'BUY') ? toCurrency.value : fromCurrency.value;
        let finalForeignAmt = 0;

        if(currentMode === 'BUY') {
            finalForeignAmt = parseFloat(document.getElementById("result-final").innerText.replace(/[^0-9.]/g, ""));
        } else {
            finalForeignAmt = parseFloat(fromAmount.value);
        }

        let exchangeData = {
            exchAcctNo: sourceAcctNo,
            exchToCurrency: foreignCode,
            exchAmount: finalForeignAmt,
            exchAppliedRate: parseFloat(appliedRateText),
            exchEsignYn: 'Y',
            exchType: currentMode
        };

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