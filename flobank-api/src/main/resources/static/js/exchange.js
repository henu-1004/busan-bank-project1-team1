/////////////////////////////////////////////////////////////
// 환전 약관 동의 (step1)
/////////////////////////////////////////////////////////////
document.addEventListener("DOMContentLoaded", () => {
  const agreeAll = document.getElementById("agreeAll");
  const termChecks = document.querySelectorAll(".term-check");

  if (agreeAll && termChecks.length > 0) {
    agreeAll.addEventListener("change", () => {
      termChecks.forEach(chk => (chk.checked = agreeAll.checked));
    });

    termChecks.forEach(chk => {
      chk.addEventListener("change", () => {
        const allChecked = [...termChecks].every(box => box.checked);
        agreeAll.checked = allChecked;
      });
    });
  }

  /////////////////////////////////////////////////////////////
  // 환전 예상 금액 확인 (step2)
  /////////////////////////////////////////////////////////////
  const calculateBtn = document.getElementById("calculateBtn");
  const exchangeResult = document.getElementById("exchangeResult");
  const amountInput = document.getElementById("exchangeAmount");
  const currencySelect = document.getElementById("currencySelect");

  // ✅ 쿠폰 관련 요소
  const couponBtn = document.getElementById("couponBtn");
  const couponModal = document.getElementById("couponModal");
  const couponOptions = document.querySelectorAll(".coupon-option");
  const couponClose = document.getElementById("couponClose");
  let selectedCouponRate = 0;

  // ✅ 쿠폰적용 버튼 클릭 → 모달 열기
  if (couponBtn && couponModal) {
    couponBtn.addEventListener("click", () => {
      couponModal.style.display = "flex";
    });
  }

  // ✅ 쿠폰 선택 시 할인율 적용
  couponOptions.forEach(btn => {
    btn.addEventListener("click", () => {
      selectedCouponRate = parseFloat(btn.dataset.discount);
      alert(`✅ ${selectedCouponRate * 100}% 수수료 할인 쿠폰이 적용되었습니다!`);
      couponModal.style.display = "none";
    });
  });

  // ✅ 닫기 버튼
  if (couponClose) {
    couponClose.addEventListener("click", () => {
      couponModal.style.display = "none";
    });
  }

  // ✅ 배경 클릭 시 닫기
  window.addEventListener("click", e => {
    if (e.target === couponModal) {
      couponModal.style.display = "none";
    }
  });

  // ✅ 환전 예상 금액 계산
  if (calculateBtn && exchangeResult && amountInput && currencySelect) {
    calculateBtn.addEventListener("click", () => {
      const amount = parseFloat(amountInput.value);
      const currency = currencySelect.value;

      if (!amount || amount <= 0) {
        alert("환전 금액을 입력해주세요.");
        return;
      }

      const rates = {
        USD: 1470.59,
        JPY: 9.62,
        EUR: 1600.12,
        CNH: 203.14
      };

      const rate = rates[currency];
      const baseFeeRate = 0.003; // 기본 수수료율 0.3%
      const discountedFeeRate = baseFeeRate * (1 - selectedCouponRate); // 쿠폰 적용
      const appliedRate = (rate * 0.985).toFixed(2); // 1.5% 우대
      const won = (amount * appliedRate).toLocaleString();

      document.getElementById("result-fee").innerText =
        (discountedFeeRate * 100).toFixed(2) + "%";

      document.getElementById("result-won").innerText = `${won}원`;
      document.getElementById("result-rate").innerText = `${rate.toLocaleString()}원`;
      document.getElementById("result-applied").innerText = `${appliedRate}원 (우대율 1.5%)`;

      exchangeResult.style.display = "block";
      exchangeResult.scrollIntoView({ behavior: "smooth", block: "center" });
    });
  }
});
