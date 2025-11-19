document.addEventListener("DOMContentLoaded", () => {
  // 🔹 탭 버튼 활성화 전환 + 콘텐츠 전환
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

});
