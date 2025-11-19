////////////////////////////////////////////////////////////////////////////
// mypage.js — 통합버전 (원화 + 외화, 외화는 USD 고정)
////////////////////////////////////////////////////////////////////////////
let isPhoneVerified = false; // [수정] 'ko'와 'en'이 공용으로 사용할 전역 변수

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
    // 4️⃣ 외화 계좌 한도 설정 (외화 페이지 전용)
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
                // (수정) 이체 한도 input이 readonly가 되면서 guide text가 필요 없어졌지만, 로직은 유지합니다.
                // guideTexts[0].textContent = `최소 ${usdLimits.min} USD ~ 최대 ${usdLimits.daily} USD 이내 수정 가능`;
                // guideTexts[1].textContent = `최소 ${usdLimits.min} USD ~ 최대 ${usdLimits.once} USD 이내`;
            }
        };

        // (수정) 외화 계좌개설 2단계에서는 이체 한도 input이 readonly이므로 '최대' 버튼 로직이 필요 없습니다.
        applyUsdLimits();
        currencySelect.addEventListener("change", () => {
            applyUsdLimits();
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // 5️⃣ [수정됨] '계좌 개설 2단계' (원화/외화) 공용 휴대폰 인증
    ////////////////////////////////////////////////////////////////////////////

    // 1. HTML에서 수정한 '공용 class'로 요소를 정확히 선택합니다.
    const btnSendSms_Acct = document.querySelector('.js-btn-send-code');
    const btnVerifySms_Acct = document.querySelector('.js-btn-verify-code');
    const inputSmsCode_Acct = document.querySelector('.js-verify-code-input');

    // 2. data-phone-number 속성을 가진 <main> 태그를 찾습니다.
    const mainContainer = document.querySelector('.open2-account-container[data-phone-number]');

    // 3. 이 요소들이 모두 존재하는 페이지에서만 (ko_account_open_2, en_account_open_2) 이 로직을 실행
    if (btnSendSms_Acct && btnVerifySms_Acct && inputSmsCode_Acct && mainContainer) {

        // 4. HTML의 data 속성에 저장된 '원본 휴대폰 번호'를 가져옵니다.
        const unmaskedPhoneNumber = mainContainer.dataset.phoneNumber;

        if (!unmaskedPhoneNumber) {
            console.error("휴대폰 번호를 찾을 수 없습니다. (data-phone-number)");
            alert("오류: 고객 정보를 불러오지 못했습니다. 다시 시도해주세요.");
            return;
        }

        // [인증요청] 버튼 클릭
        btnSendSms_Acct.addEventListener('click', async function (e) {
            e.preventDefault();
            btnSendSms_Acct.disabled = true;
            btnSendSms_Acct.textContent = '전송중...';

            try {
                const response = await fetch(`/flobank/sms/send?phoneNumber=${encodeURIComponent(unmaskedPhoneNumber)}`, { method: 'POST' });
                if (!response.ok) { throw new Error('SMS 전송 실패'); }
                alert('인증번호가 전송되었습니다.');

                inputSmsCode_Acct.style.display = 'inline-block';
                btnVerifySms_Acct.style.display = 'inline-block';
                inputSmsCode_Acct.focus();
                btnSendSms_Acct.textContent = '재전송';

            } catch (err) {
                console.error('SMS Send Error:', err);
                alert(`SMS 전송 중 오류 발생: ${err.message}`);
            } finally {
                btnSendSms_Acct.disabled = false;
            }
        });

        // [확인] 버튼 클릭
        btnVerifySms_Acct.addEventListener('click', async function (e) {
            e.preventDefault();
            const code = inputSmsCode_Acct.value.trim();
            if (!unmaskedPhoneNumber || !code) {
                alert('전화번호와 인증번호를 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`/flobank/sms/verify?phoneNumber=${encodeURIComponent(unmaskedPhoneNumber)}&code=${encodeURIComponent(code)}`, { method: 'POST' });
                const isValid = await response.json();

                if (isValid) {
                    alert('휴대폰 인증 완료!');
                    isPhoneVerified = true; // [중요] 전역 변수 true로 설정

                    inputSmsCode_Acct.readOnly = true;
                    btnSendSms_Acct.disabled = true;
                    btnVerifySms_Acct.disabled = true;
                    btnVerifySms_Acct.textContent = '인증완료';
                } else {
                    alert('인증번호가 일치하지 않습니다.');
                    isPhoneVerified = false;
                }
            } catch (err) {
                console.error('SMS Verify Error:', err);
                alert('인증 확인 중 오류 발생');
                isPhoneVerified = false;
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // 6️⃣ '원화 계좌 개설 2단계' - 계좌 비밀번호 일치 확인 (ko_account_open_2 전용)
    ////////////////////////////////////////////////////////////////////////////
    const acctPwInput = document.getElementById('acctPw');
    const acctPwConfirmInput = document.getElementById('acctPwConfirm');
    const pwMatchMsg = document.getElementById('pwMatchMessage');

    if (acctPwInput && acctPwConfirmInput && pwMatchMsg) {
        function checkKoAcctPasswordMatch() {
            const pw = acctPwInput.value;
            const confirmPw = acctPwConfirmInput.value;
            if (confirmPw === '') { pwMatchMsg.textContent = ''; return; }

            // (숫자 4자리 검증 추가)
            const numPattern = /^\d{4}$/;
            if (pw.length > 0 && !numPattern.test(pw)) {
                pwMatchMsg.textContent = '비밀번호는 숫자 4자리여야 합니다.';
                pwMatchMsg.style.color = '#E53935';
                return;
            }

            if (pw === confirmPw) {
                pwMatchMsg.textContent = '비밀번호가 일치합니다.';
                pwMatchMsg.style.color = '#4A6FA5';
            } else {
                pwMatchMsg.textContent = '비밀번호가 일치하지 않습니다.';
                pwMatchMsg.style.color = '#E53935';
            }
        }
        acctPwInput.addEventListener('input', checkKoAcctPasswordMatch);
        acctPwConfirmInput.addEventListener('input', checkKoAcctPasswordMatch);
    }

    ////////////////////////////////////////////////////////////////////////////
    // 7️⃣ '원화 계좌 개설 2단계' - 폼 제출(완료) 시 유효성 검사 (ko_account_open_2 전용)
    ////////////////////////////////////////////////////////////////////////////
    const koAccountForm = document.getElementById('koAccountOpenForm');
    if (koAccountForm) {
        koAccountForm.addEventListener('submit', function(e) {
            const purposeSelect = document.getElementById('cddPurpose');
            const sourceSelect = document.getElementById('cddSource');
            const pw = acctPwInput ? acctPwInput.value : "";
            const pwConfirm = acctPwConfirmInput ? acctPwConfirmInput.value : "";

            if (isPhoneVerified === false) {
                e.preventDefault();
                alert('휴대폰 인증을 완료해주세요.');
                document.querySelector('.open2-verify-section').scrollIntoView({ behavior: 'smooth' });
                return;
            }
            if (purposeSelect && purposeSelect.value === "") {
                e.preventDefault();
                alert('거래 목적을 선택해주세요.');
                purposeSelect.focus();
                return;
            }
            if (sourceSelect && sourceSelect.value === "") {
                e.preventDefault();
                alert('자금 출처를 선택해주세요.');
                sourceSelect.focus();
                return;
            }
            if (pw.length < 4 || !/^\d{4}$/.test(pw)) {
                e.preventDefault();
                alert('계좌 비밀번호 4자리를 정확히 입력해주세요.');
                if(acctPwInput) acctPwInput.focus();
                return;
            }
            if (pw !== pwConfirm) {
                e.preventDefault();
                alert('계좌 비밀번호가 일치하지 않습니다.');
                if(acctPwConfirmInput) acctPwConfirmInput.focus();
                return;
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    // 8️⃣ '외화 계좌 개설 2단계' - 계좌 비밀번호 일치 확인 (en_account_open_2 전용)
    ////////////////////////////////////////////////////////////////////////////

    const enAcctPwInput = document.getElementById('enAcctPw');
    const enAcctPwConfirmInput = document.getElementById('enAcctPwConfirm');
    const enPwMatchMsg = document.getElementById('enPwMatchMessage');

    if (enAcctPwInput && enAcctPwConfirmInput && enPwMatchMsg) {

        function checkEnAcctPasswordMatch() {
            const pw = enAcctPwInput.value;
            const confirmPw = enAcctPwConfirmInput.value;

            if (confirmPw === '') {
                enPwMatchMsg.textContent = '';
                return;
            }

            const numPattern = /^\d{4}$/;
            if (pw.length > 0 && !numPattern.test(pw)) {
                enPwMatchMsg.textContent = '비밀번호는 숫자 4자리여야 합니다.';
                enPwMatchMsg.style.color = '#E53935';
                return;
            }

            if (pw === confirmPw) {
                enPwMatchMsg.textContent = '비밀번호가 일치합니다.';
                enPwMatchMsg.style.color = '#4A6FA5';
            } else {
                enPwMatchMsg.textContent = '비밀번호가 일치하지 않습니다.';
                enPwMatchMsg.style.color = '#E53935';
            }
        }
        enAcctPwInput.addEventListener('input', checkEnAcctPasswordMatch);
        enAcctPwConfirmInput.addEventListener('input', checkEnAcctPasswordMatch);
    }

    ////////////////////////////////////////////////////////////////////////////
    // 9️⃣ [수정됨] '외화 계좌 개설 2단계' - 폼 제출(완료) 시 유효성 검사 (en_account_open_2 전용)
    ////////////////////////////////////////////////////////////////////////////

    const enAccountForm = document.getElementById('enAccountOpenForm');

    if (enAccountForm) {
        enAccountForm.addEventListener('submit', function(e) {

            // 2. 검증에 사용할 요소들 선택
            const pw = enAcctPwInput ? enAcctPwInput.value : "";
            const pwConfirm = enAcctPwConfirmInput ? enAcctPwConfirmInput.value : "";

            // [추가] '거래 목적'과 '자금 출처' select 요소 선택
            // (HTML의 id="cddPurpose", id="cddSource"를 사용합니다)
            const purposeSelect_en = document.getElementById('cddPurpose');
            const sourceSelect_en = document.getElementById('cddSource');

            // 검사 1: 휴대폰 인증 여부
            if (isPhoneVerified === false) {
                e.preventDefault();
                alert('휴대폰 인증을 완료해주세요.');
                document.querySelector('.open2-verify-section').scrollIntoView({ behavior: 'smooth' });
                return;
            }

            // [추가] 검사 2: 거래 목적 선택 여부
            if (purposeSelect_en && purposeSelect_en.value === "") {
                e.preventDefault();
                alert('거래 목적을 선택해주세요.');
                purposeSelect_en.focus(); // 해당 select로 포커스 이동
                return;
            }

            // [추가] 검사 3: 자금 출처 선택 여부
            if (sourceSelect_en && sourceSelect_en.value === "") {
                e.preventDefault();
                alert('자금 출처를 선택해주세요.');
                sourceSelect_en.focus();
                return;
            }

            // 검사 4: 비밀번호 입력 여부 (4자리)
            if (pw.length < 4 || !/^\d{4}$/.test(pw)) {
                e.preventDefault();
                alert('계좌 비밀번호 4자리를 정확히 입력해주세요.');
                if (enAcctPwInput) enAcctPwInput.focus();
                return;
            }

            // 검사 5: 비밀번호 일치 여부
            if (pw !== pwConfirm) {
                e.preventDefault();
                alert('계좌 비밀번호가 일치하지 않습니다.');
                if (enAcctPwConfirmInput) enAcctPwConfirmInput.focus();
                return;
            }
        });
    }


});

function setQuestion(text) {
    document.getElementById('chatInput').value = text;

}