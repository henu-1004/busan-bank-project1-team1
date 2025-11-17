////////////////////////////////////////////////////////////////////////////
// mypage.js — 통합버전 (원화 + 외화, 외화는 USD 고정)
////////////////////////////////////////////////////////////////////////////
let isPhoneVerified = false;
document.addEventListener("DOMContentLoaded", () => {

  ////////////////////////////////////////////////////////////////////////////
  // 2️⃣ 계좌개설 약관 전체 동의
  ////////////////////////////////////////////////////////////////////////////
  const agreeAll = document.getElementById("agreeAll");
  const checks = document.querySelectorAll(".term-check");

  if (agreeAll && checks.length > 0) {
    agreeAll.addEventListener("change", () => {
      checks.forEach(chk => (chk.checked = agreeAll.checked));
    });

    checks.forEach(chk => {
      chk.addEventListener("change", () => {
        agreeAll.checked = [...checks].every(c => c.checked);
      });
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  // 3️⃣ 원화 계좌 한도 설정 (원화 페이지 전용)
  ////////////////////////////////////////////////////////////////////////////
  const dayLimitBtn = document.querySelectorAll(".open2-btn-limit")[0];
  const onceLimitBtn = document.querySelectorAll(".open2-btn-limit")[1];
  const dayLimitInput = document.querySelectorAll(".open2-limit-input")[0];
  const onceLimitInput = document.querySelectorAll(".open2-limit-input")[1];

  if (dayLimitBtn && onceLimitBtn && !document.getElementById("currency-select")) {
    // 💡 원화 전용 페이지에서만 실행
    dayLimitBtn.addEventListener("click", () => {
      dayLimitInput.value = "500,000,000";
    });

    onceLimitBtn.addEventListener("click", () => {
      onceLimitInput.value = "100,000,000";
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  // 4️⃣ 외화 계좌 한도 설정 (USD 고정)
  ////////////////////////////////////////////////////////////////////////////
  const currencySelect = document.getElementById("currency-select");
  const dailyLimit = document.getElementById("daily-limit");
  const onceLimit = document.getElementById("once-limit");
  const currencyLabels = document.querySelectorAll(".currency-label");
  const guideTexts = document.querySelectorAll(".open2-guide-text");

  if (currencySelect && dailyLimit && onceLimit) {
    const usdLimits = {
      daily: "50,000",
      once: "10,000",
      min: "100"
    };

    // ✅ USD 기준으로만 설정
    const applyUsdLimits = () => {
      dailyLimit.value = usdLimits.daily;
      onceLimit.value = usdLimits.once;
      currencyLabels.forEach(label => (label.textContent = "USD"));

      if (guideTexts.length >= 2) {
        guideTexts[0].textContent = `최소 ${usdLimits.min} USD ~ 최대 ${usdLimits.daily} USD 이내 수정 가능`;
        guideTexts[1].textContent = `최소 ${usdLimits.min} USD ~ 최대 ${usdLimits.once} USD 이내`;
      }
    };

    // 초기 설정
    applyUsdLimits();

    // 통화 선택해도 무조건 USD 고정
    currencySelect.addEventListener("change", () => {
      applyUsdLimits();
    });

    // “최대” 버튼 클릭 시도 USD 기준 값 그대로
    const maxBtns = document.querySelectorAll(".open2-btn-limit");
    if (maxBtns.length >= 2) {
      maxBtns[0].addEventListener("click", () => {
        dailyLimit.value = usdLimits.daily;
      });
      maxBtns[1].addEventListener("click", () => {
        onceLimit.value = usdLimits.once;
      });
    }
  }

    ////////////////////////////////////////////////////////////////////////////
    // 5️⃣ '원화 계좌 개설 2단계' 전용 휴대폰 인증 (ko_account_open_2)
    ////////////////////////////////////////////////////////////////////////////

    // 1. HTML에서 수정한 새 ID로 요소를 정확히 선택합니다.
    const btnSendSms_ko = document.querySelector('#koAcctBtnSendCode');
    const btnVerifySms_ko = document.querySelector('#koAcctBtnVerifyCode');
    const inputSmsCode_ko = document.querySelector('#koAcctVerifyCodeInput');

    // 2. data-phone-number 속성을 가진 <main> 태그를 찾습니다.
    const mainContainer = document.querySelector('.open2-account-container[data-phone-number]');

    // 3. 이 요소들이 모두 존재하는 페이지에서만 (ko_account_open_2) 이 로직을 실행
    if (btnSendSms_ko && btnVerifySms_ko && inputSmsCode_ko && mainContainer) {

        // 4. HTML의 data 속성에 저장된 '원본 휴대폰 번호'를 가져옵니다.
        const unmaskedPhoneNumber = mainContainer.dataset.phoneNumber;

        if (!unmaskedPhoneNumber) {
            console.error("휴대폰 번호를 찾을 수 없습니다. (data-phone-number)");
            alert("오류: 고객 정보를 불러오지 못했습니다. 다시 시도해주세요.");
            return;
        }

        // [인증요청] 버튼 클릭
        btnSendSms_ko.addEventListener('click', async function (e) {
            e.preventDefault();

            btnSendSms_ko.disabled = true;
            btnSendSms_ko.textContent = '전송중...';

            try {
                // 5. (수정) fetch URL에 JS 변수가 아닌, data-속성에서 가져온 unmaskedPhoneNumber를 사용
                const response = await fetch(`/flobank/sms/send?phoneNumber=${encodeURIComponent(unmaskedPhoneNumber)}`, { method: 'POST' });

                if (!response.ok) {
                    throw new Error('SMS 전송 실패');
                }

                alert('인증번호가 전송되었습니다.');

                // 6. (수정) 새 ID를 사용
                inputSmsCode_ko.style.display = 'inline-block';
                btnVerifySms_ko.style.display = 'inline-block';
                inputSmsCode_ko.focus();
                btnSendSms_ko.textContent = '재전송';

            } catch (err) {
                console.error('SMS Send Error:', err);
                alert(`SMS 전송 중 오류 발생: ${err.message}`);
            } finally {
                btnSendSms_ko.disabled = false;
            }
        });

        // [확인] 버튼 클릭
        btnVerifySms_ko.addEventListener('click', async function (e) {
            e.preventDefault();
            const unmaskedPhoneNumber = mainContainer.dataset.phoneNumber; // 원본 번호 가져오기
            const code = inputSmsCode_ko.value.trim();

            if (!unmaskedPhoneNumber || !code) {
                alert('전화번호와 인증번호를 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`/flobank/sms/verify?phoneNumber=${encodeURIComponent(unmaskedPhoneNumber)}&code=${encodeURIComponent(code)}`, { method: 'POST' });
                const isValid = await response.json();

                if (isValid) {
                    alert('휴대폰 인증 완료!');

                    // [추가 2] 인증 성공 시 전역 변수를 true로 변경!
                    isPhoneVerified = true;

                    inputSmsCode_ko.readOnly = true;
                    btnSendSms_ko.disabled = true;
                    btnVerifySms_ko.disabled = true;
                    btnVerifySms_ko.textContent = '인증완료';
                } else {
                    alert('인증번호가 일치하지 않습니다.');
                    isPhoneVerified = false; // [추가] 실패 시 false로 유지
                }
            } catch (err) {
                console.error('SMS Verify Error:', err);
                alert('인증 확인 중 오류 발생');
                isPhoneVerified = false; // [추가] 실패 시 false
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // 6️⃣ '원화 계좌 개설 2단계' - 계좌 비밀번호 일치 확인
    ////////////////////////////////////////////////////////////////////////////

    // 1. HTML에 추가한 ID로 요소들을 선택
    const acctPwInput = document.getElementById('acctPw');
    const acctPwConfirmInput = document.getElementById('acctPwConfirm');
    const pwMatchMsg = document.getElementById('pwMatchMessage');

    // 2. 이 요소들이 모두 존재하는 페이지에서만 (ko_account_open_2) 로직 실행
    if (acctPwInput && acctPwConfirmInput && pwMatchMsg) {

        // 3. 비밀번호 검증 함수
        function checkAcctPasswordMatch() {
            const pw = acctPwInput.value;
            const confirmPw = acctPwConfirmInput.value;

            // 아직 확인란에 아무것도 안 썼으면 메시지 안 띄움
            if (confirmPw === '') {
                pwMatchMsg.textContent = '';
                return;
            }

            // 일치 여부 확인
            if (pw === confirmPw) {
                pwMatchMsg.textContent = '비밀번호가 일치합니다.';
                pwMatchMsg.style.color = '#4A6FA5'; // 파란색 (성공)
            } else {
                pwMatchMsg.textContent = '비밀번호가 일치하지 않습니다.';
                pwMatchMsg.style.color = '#E53935'; // 빨간색 (실패)
            }
        }

        // 4. 두 입력창에 'input' 이벤트(타이핑 할 때마다) 연결
        acctPwInput.addEventListener('input', checkAcctPasswordMatch);
        acctPwConfirmInput.addEventListener('input', checkAcctPasswordMatch);
    }

    ////////////////////////////////////////////////////////////////////////////
    // 7️⃣ '원화 계좌 개설 2단계' - 폼 제출(완료) 시 유효성 검사
    ////////////////////////////////////////////////////////////////////////////

    // 1. HTML에 추가한 form의 ID를 선택
    const koAccountForm = document.getElementById('koAccountOpenForm');

    if (koAccountForm) {
        koAccountForm.addEventListener('submit', function(e) {

            // 2. 검증에 사용할 요소들 선택
            const purposeSelect = document.getElementById('cddPurpose');
            const sourceSelect = document.getElementById('cddSource');
            const pw = acctPwInput.value;
            const pwConfirm = acctPwConfirmInput.value;

            // 검사 1: 휴대폰 인증 여부
            if (isPhoneVerified === false) {
                e.preventDefault(); // 폼 제출 중단
                alert('휴대폰 인증을 완료해주세요.');
                // 인증 섹션으로 스크롤 (선택사항)
                document.querySelector('.open2-verify-section').scrollIntoView({ behavior: 'smooth' });
                return; // 검사 중단
            }

            // 검사 2: 거래 목적 선택 여부
            if (purposeSelect.value === "") {
                e.preventDefault();
                alert('거래 목적을 선택해주세요.');
                purposeSelect.focus(); // 해당 select로 포커스 이동
                return;
            }

            // 검사 3: 자금 출처 선택 여부
            if (sourceSelect.value === "") {
                e.preventDefault();
                alert('자금 출처를 선택해주세요.');
                sourceSelect.focus();
                return;
            }

            // 검사 4: 비밀번호 입력 여부 (4자리)
            if (pw.length < 4 || !/^\d{4}$/.test(pw)) {
                e.preventDefault();
                alert('계좌 비밀번호 4자리를 정확히 입력해주세요.');
                acctPwInput.focus();
                return;
            }

            // 검사 5: 비밀번호 일치 여부
            if (pw !== pwConfirm) {
                e.preventDefault();
                alert('계좌 비밀번호가 일치하지 않습니다.');
                acctPwConfirmInput.focus();
                return;
            }
        });
    }
});


document.addEventListener("DOMContentLoaded", () => {
  const agreeAll = document.getElementById("agreeAll");
  const checks = document.querySelectorAll(".term-check");

  if (agreeAll && checks.length > 0) {
    agreeAll.addEventListener("change", () => {
      checks.forEach(chk => chk.checked = agreeAll.checked);
    });

    checks.forEach(chk => {
      chk.addEventListener("change", () => {
        agreeAll.checked = [...checks].every(c => c.checked);
      });
    });
  }
});





