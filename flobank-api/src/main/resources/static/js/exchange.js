/*
* 날짜 : 2025/11/22
* 이름 : 김대현, 이민준
* 내용 : 환전 기능 (변수 중복 제거, 동적 data-flag 적용 완료)
*/

document.addEventListener("DOMContentLoaded", () => {

    // =========================================
    // 서버 데이터 수신 및 처리 로직 이동
    // =========================================
    const frgnBalanceMap = {};
    let frgnAcctNo = '';

    if (window.exchData) {
        // 1. 외화 계좌번호 가져오기
        frgnAcctNo = window.exchData.frgnAcctNo || '';

        // 2. 잔액 리스트를 Map으로 변환 { 'USD': 100.00, ... }
        const rawList = window.exchData.balanceList;
        if (rawList && Array.isArray(rawList)) {
            rawList.forEach(dto => {
                frgnBalanceMap[dto.balCurrency] = dto.balBalance;
            });
        }
    }

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
    // const resultFee = document.getElementById("result-fee");
    const resultFinal = document.getElementById("result-final");
    const spreadRateDisplay = document.getElementById("spreadRateDisplay");

    const couponBtn = document.getElementById("couponBtn");
    const couponModal = document.getElementById("couponModal");
    const couponClose = document.getElementById("couponClose");
    const couponOptions = document.querySelectorAll(".coupon-option");

    // [수정] 전역 변수 및 상수 정의
    let selectedCouponRate = 0; // 쿠폰 할인율 (예: 50% -> 0.5)
    let currentBaseRate = 0;    // 매매기준율
    let currentTtsRate = 0;     // 전신환매도율 (TTS)
    let currentTtbRate = 0;     // 전신환매입율 (받으실 때)
    let currentMode = 'BUY';

    // 1. 우리 플로은행의 기본 환율 수수료는 0.05 (5%)
    const FLO_BASIC_FEE = 0.05;

    // 통화별 기본 우대율 설정
    // 이미지에 있는 통화별 우대율을 매핑 (달러 70%, 엔/유로 50%, 그외 30%)
    const BASE_PREF_RATES = {
        'USD': 0.70,
        'JPY': 0.50,
        'EUR': 0.50,
        'CNY': 0.30,
        'CNH': 0.30,
        'GBP': 0.30,
        'AUD': 0.30,
        'OTHERS': 0.30
    };

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

    // 쿠폰 선택 로직
    if (couponBtn) couponBtn.addEventListener("click", () => couponModal.style.display = "flex");
    if (couponClose) couponClose.addEventListener("click", () => couponModal.style.display = "none");
    couponOptions.forEach(btn => {
        btn.addEventListener("click", () => {
            // 버튼의 data-discount 값 (예: 0.5)
            selectedCouponRate = parseFloat(btn.dataset.discount);

            // 화면에 쿠폰 적용되었음을 알림 (옵션)
            if(couponBtn) couponBtn.innerText = `쿠폰적용(${(selectedCouponRate*100).toFixed(0)}%)`;

            alert("쿠폰이 적용되었습니다.");
            couponModal.style.display = "none";

            // 재계산 수행
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
            // SELL 모드일 때, 선택된 외화에 맞는 잔액 표시
            updateForeignAcctBalanceDisplay();
        }

        calculateExchange();
    }

    // [로직 이동 완료] 외화 계좌 잔액 표시 함수
    function updateForeignAcctBalanceDisplay() {
        if (!accountForeign || accountForeign.options.length === 0) return;

        // 현재 선택된 '보내는 통화' (예: USD)
        const targetCurrency = fromCurrency.value;

        // JS 내부 Map에서 조회
        const balance = frgnBalanceMap[targetCurrency] || 0;

        // 포맷팅
        const formattedBalance = balance.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});

        // 텍스트 업데이트
        const option = accountForeign.options[0];
        if(option) {
            option.text = `${frgnAcctNo} (잔액 ${formattedBalance} ${targetCurrency})`;
        }
    }

    // [서버/Redis API 호출]
    function fetchRate(currencyCode) {
        fetch(`/flobank/exchange/api/rate?currency=${currencyCode}`)
            .then(res => {
                if (!res.ok) throw new Error('Network response was not ok');
                return res.json();
            })
            .then(data => {
                if (data && data.rate) {
                    currentBaseRate = parseFloat(data.rate);

                    // TTS가 있으면 사용, 없으면 임시 계산 (안전장치)
                    if (data.tts) {
                        currentTtsRate = parseFloat(data.tts);
                    } else {
                        currentTtsRate = currentBaseRate * 1.0175; // 안전장치
                    }

                    // TTB 데이터 처리
                    if (data.ttb) {
                        currentTtbRate = parseFloat(data.ttb);
                    } else {
                        currentTtbRate = currentBaseRate * 0.9825; // 안전장치 (약 1.75% 차감)
                    }

                    calculateExchange();
                } else {
                    console.error("Invalid rate data received");
                }
            })
            .catch(err => {
                console.error("Error fetching rate:", err);
            });
    }

    // [수정] 환율 계산 로직
    function calculateExchange() {
        if (!currentBaseRate || !fromAmount.value) {
            toAmount.value = '';
            if(!fromAmount.value) resultFinal.innerText = '0';
            return;
        }

        const amount = parseFloat(fromAmount.value);
        const foreignCode = (currentMode === 'BUY') ? toCurrency.value : fromCurrency.value;

        // ----------------------------------------------------
        // 1. 스프레드(차액) 계산 (모드별 분기)
        // BUY (원->외): TTS - Rate
        // SELL (외->원): Rate - TTB
        // ----------------------------------------------------
        let spread = 0;
        if (currentMode === 'BUY') {
            spread = currentTtsRate - currentBaseRate;
        } else {
            // 외화 팔 때 (SELL)
            spread = currentBaseRate - currentTtbRate;
        }

        if (spread < 0) spread = 0; // 안전장치

        // ----------------------------------------------------
        // 2. 수수료율 계산 (동일)
        // ----------------------------------------------------
        let feeRate = FLO_BASIC_FEE * (1 - selectedCouponRate);

        // ----------------------------------------------------
        // 3. 총 수수료 금액 (공식 동일)
        // (차액) + (차액 * 수수료율)
        // ----------------------------------------------------
        let totalFeeAmount = spread + (spread * feeRate);

        // ----------------------------------------------------
        // 4. 최종 적용 환율 도출
        // ----------------------------------------------------
        let appliedRate = 0;

        if (currentMode === 'BUY') {
            // 살 때: 기준율 + 수수료 (고객이 비싸게 삼)
            appliedRate = currentBaseRate + totalFeeAmount;
        } else {
            // 팔 때: 기준율 - 수수료 (고객이 싸게 팖)
            appliedRate = currentBaseRate - totalFeeAmount;
        }

        // --- UI 표시 (기존 코드 유지) ---
        let convertVal = 0;
        if (currentMode === 'BUY') {
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                convertVal = amount / (appliedRate / 100);
            } else {
                convertVal = amount / appliedRate;
            }
            toAmount.value = convertVal.toFixed(2);
        } else {
            if (foreignCode === 'JPY' || foreignCode === 'CNH') {
                convertVal = amount * (appliedRate / 100);
            } else {
                convertVal = amount * appliedRate;
            }
            toAmount.value = Math.floor(convertVal).toLocaleString();
        }

        resultBaseRate.innerText = `${currentBaseRate.toLocaleString()} 원`;
        resultRate.innerText = `${appliedRate.toFixed(2)} 원`;

        if (currentMode === 'BUY') {
            resultFinal.innerText = `${convertVal.toFixed(2)} ${foreignCode}`;
        } else {
            resultFinal.innerText = `${Math.floor(convertVal).toLocaleString()} KRW`;
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
                acctPw: pwInput.value.trim(),
                mode: currentMode // BUY 또는 SELL 정보 전송
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
        // 1. 화면에서 최종 적용 환율 가져오기
        const appliedRateText = document.getElementById("result-rate").innerText.replace(/[^0-9.]/g, "");
        const foreignCode = (currentMode === 'BUY') ? toCurrency.value : fromCurrency.value;

        // 2. 외화 금액 파싱
        let finalForeignAmt = 0;
        if(currentMode === 'BUY') {
            // 살 때: 결과창의 외화 금액
            finalForeignAmt = parseFloat(document.getElementById("result-final").innerText.replace(/[^0-9.]/g, ""));
        } else {
            // 팔 때: 입력창의 외화 금액
            finalForeignAmt = parseFloat(fromAmount.value);
        }

        // 3. spread(차액) 변수 재계산
        let spread = 0;
        if (currentMode === 'BUY') {
            spread = currentTtsRate - currentBaseRate;
        } else {
            spread = currentBaseRate - currentTtbRate;
        }
        if (spread < 0) spread = 0;

        // 4. 총 수수료(원화) 계산
        const feeRate = FLO_BASIC_FEE * (1 - selectedCouponRate);
        const unitFee = spread + (spread * feeRate); // 1단위당 수수료

        let totalFeeKrw = unitFee * finalForeignAmt; // 전체 수수료 (원화)

        // 5. 데이터 세팅
        let exchangeData = {
            exchAcctNo: sourceAcctNo,
            exchToCurrency: foreignCode,
            exchAmount: finalForeignAmt,
            exchAppliedRate: parseFloat(appliedRateText),
            exchEsignYn: 'Y',
            exchType: currentMode,
            exchFee: Math.floor(totalFeeKrw) // 소수점 절사
        };

        // 6. 추가 정보 세팅 (수령지점 or 입금계좌)
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
            // 주소 필드에 입금 계좌 정보 저장
            exchangeData.exchAddr = `즉시입금:${depositAcct}`;
            // 입금은 즉시 처리되므로 오늘 날짜
            exchangeData.exchExpDy = new Date().toISOString().split("T")[0];
        }

        // 7. 서버 전송
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