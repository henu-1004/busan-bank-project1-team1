/*
* 날짜 : 2025/11/21
* 이름 : 김대현
* 내용 : 환전 기능 (약관 동의, 실시간 환율 계산, 쿠폰 적용)
* */

document.addEventListener("DOMContentLoaded", () => {

    /////////////////////////////////////////////////////////////
    // 1. 환전 약관 동의 (step1)
    /////////////////////////////////////////////////////////////
    const agreeAll = document.getElementById("agreeAll");
    const termChecks = document.querySelectorAll(".term-check");
    const nextBtnStep1 = document.querySelector(".step1-btn-next"); // 클래스명 주의

    if (agreeAll && termChecks.length > 0) {
        // 전체 동의 체크박스
        agreeAll.addEventListener("change", () => {
            termChecks.forEach(chk => (chk.checked = agreeAll.checked));
        });

        // 개별 체크박스
        termChecks.forEach(chk => {
            chk.addEventListener("change", () => {
                const allChecked = [...termChecks].every(box => box.checked);
                agreeAll.checked = allChecked;
            });
        });
    }

    // step1 다음 버튼 이벤트
    if (nextBtnStep1) {
        nextBtnStep1.addEventListener("click", e => {
            if (!agreeAll.checked) {
                e.preventDefault();
                alert("모든 약관에 동의해야 다음 단계로 진행할 수 있습니다.");
                return;
            }
            window.location.href = "/exchange/step2";
        });
    }


    /////////////////////////////////////////////////////////////
    // 2. 환전 정보 입력 및 계산 (step2)
    /////////////////////////////////////////////////////////////
    const calculateBtn = document.getElementById("calculateBtn");
    const exchangeResult = document.getElementById("exchangeResult");
    const amountInput = document.getElementById("exchangeAmount");
    const currencySelect = document.getElementById("currencySelect");
    const nextBtnStep2 = document.querySelector(".step2-btn-next");

    // 결과 출력 요소
    const resultFee = document.getElementById("result-fee");
    const resultWon = document.getElementById("result-won");
    const resultRate = document.getElementById("result-rate");
    const resultApplied = document.getElementById("result-applied");

    // 쿠폰 관련
    const couponBtn = document.getElementById("couponBtn");
    const couponModal = document.getElementById("couponModal");
    const couponOptions = document.querySelectorAll(".coupon-option");
    const couponClose = document.getElementById("couponClose");

    // 상태 변수
    let selectedCouponRate = 0; // 쿠폰 우대율 (예: 0.5 -> 50%)
    let currentBaseRate = 0;    // 서버에서 받아온 기준 환율

    // =========================================
    // [쿠폰 모달 로직]
    // =========================================
    if (couponBtn && couponModal) {
        couponBtn.addEventListener("click", () => {
            couponModal.style.display = "flex";
        });
    }

    if (couponClose) {
        couponClose.addEventListener("click", () => {
            couponModal.style.display = "none";
        });
    }

    window.addEventListener("click", e => {
        if (e.target === couponModal) {
            couponModal.style.display = "none";
        }
    });

    // 쿠폰 선택 시
    couponOptions.forEach(btn => {
        btn.addEventListener("click", () => {
            // data-discount="0.20" -> 20% 우대
            selectedCouponRate = parseFloat(btn.dataset.discount);
            alert(`✅ 환율 우대 쿠폰(${selectedCouponRate * 100}%)이 적용되었습니다!`);
            couponModal.style.display = "none";

            // 이미 계산된 내역이 있다면 재계산 수행
            if (currentBaseRate > 0 && amountInput.value) {
                calculateExchange();
            }
        });
    });


    // =========================================
    // [환율 계산 로직 (API 연동)]
    // =========================================
    if (calculateBtn) {
        calculateBtn.addEventListener("click", () => {
            const amount = parseFloat(amountInput.value);
            const currency = currencySelect.value;

            if (!amount || amount <= 0) {
                alert("환전 금액을 올바르게 입력해주세요.");
                amountInput.focus();
                return;
            }

            // 1. 서버 API 호출 (Redis 데이터 가져오기)
            fetch(`/flobank/exchange/api/rate?currency=${currency}`)
                .then(response => response.json())
                .then(data => {
                    if (data.status === 'success') {
                        currentBaseRate = data.rate; // API에서 받은 환율 저장
                        calculateExchange(); // 계산 함수 호출
                    } else {
                        alert("환율 정보를 불러오는데 실패했습니다.");
                    }
                })
                .catch(error => {
                    console.error("Error fetching rate:", error);
                    alert("서버 통신 중 오류가 발생했습니다.");
                });
        });
    }

    // 실제 계산 수행 함수
    function calculateExchange() {
        const amount = parseFloat(amountInput.value);
        const currency = currencySelect.value;

        // 1. 은행 기본 수수료율 (Spread) 설정 (예: 1.75%)
        const spreadRate = 0.0175;

        // 2. 우대율 적용된 수수료율 계산
        // (기본수수료) * (1 - 쿠폰할인율)
        // 예: 1.75% * (1 - 0.5) = 0.875% 수수료만 부과
        const appliedSpreadRate = spreadRate * (1 - selectedCouponRate);

        // 3. 고객 적용 환율 (살 때 환율)
        // 기준환율 + (기준환율 * 적용수수료율)
        // 예: 1400 + (1400 * 0.00875) = 1412.25
        let appliedRate = currentBaseRate + (currentBaseRate * appliedSpreadRate);

        // 4. 최종 원화 금액
        let totalWon = 0;

        // JPY, VND 등 100단위 통화 처리 (보통 API가 100단위 환율을 주면 그대로 쓰고, 1단위면 곱함)
        // 여기서는 API가 '100엔당 원화'를 준다고 가정하고, 입력 금액도 '엔' 단위이므로 보정 필요
        if (currency === 'JPY' || currency === 'CNH') {
            // 예: 100엔이 900원이면, 1000엔 환전 시 -> 1000 * (900 / 100)
            totalWon = amount * (appliedRate / 100);
        } else {
            totalWon = amount * appliedRate;
        }

        // 5. 화면 출력 (소수점 및 콤마 처리)
        // (1) 적용된 수수료(우대율 반영) 표시
        const discountPercent = (selectedCouponRate * 100).toFixed(0);
        if(selectedCouponRate > 0){
            resultFee.innerHTML = `<span style="color:blue; font-weight:bold;">${discountPercent}% 우대 적용</span>`;
        } else {
            resultFee.innerText = "우대 없음 (기본 수수료)";
        }

        // (2) 현재 고시 환율
        resultRate.innerText = `${currentBaseRate.toLocaleString()} 원`;

        // (3) 적용 환율
        resultApplied.innerText = `${appliedRate.toFixed(2)} 원`;

        // (4) 최종 필요 원화 금액 (십원 단위 절사 등 정책에 따라 조정 가능, 여기선 정수표시)
        resultWon.innerText = `${Math.floor(totalWon).toLocaleString()} 원`;

        // 결과창 보이기
        exchangeResult.style.display = "block";
        // 부드럽게 스크롤 이동
        // exchangeResult.scrollIntoView({ behavior: "smooth", block: "center" });
    }


    // =========================================
    // [다음 단계 진행 (step3)]
    // =========================================
    if (nextBtnStep2) {
        nextBtnStep2.addEventListener("click", () => {
            // 필수 입력값 검증
            if (!exchangeResult.style.display || exchangeResult.style.display === "none") {
                alert("환전 예상금액 확인을 먼저 진행해주세요.");
                return;
            }

            // 폼 전송 또는 다음 페이지 이동 로직
            // document.querySelector(".step2-form").submit(); // 실제 전송 시 사용
            alert("다음 단계로 이동합니다 (구현 예정)");
            // window.location.href = "/exchange/step3";
        });
    }

    // =========================================
    // [수령 희망일 제한 (오늘 이전 선택 불가)]
    // =========================================
    const dateInput = document.querySelector('input[type="date"]');
    if(dateInput) {
        const today = new Date().toISOString().split("T")[0];
        dateInput.setAttribute("min", today);

        // 최대 1개월 설정
        let maxDate = new Date();
        maxDate.setMonth(maxDate.getMonth() + 1);
        dateInput.setAttribute("max", maxDate.toISOString().split("T")[0]);
    }

});