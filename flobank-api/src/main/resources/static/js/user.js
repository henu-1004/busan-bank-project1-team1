// [수정] 모든 코드를 DOMContentLoaded 이벤트 리스너 안으로 이동시킵니다.
document.addEventListener("DOMContentLoaded", function () {

    // -------------------------------------------------------------
    // 사용자 정보 변경 모달 (userInfoModal)
    // -------------------------------------------------------------
    const editProfileBtn = document.getElementById('btn-user-info-modal');
    const userInfoModal = document.getElementById('userInfoModal');
    const btnWithdraw = document.getElementById("btn-withdraw");

    if (editProfileBtn && userInfoModal) {
        const closeUserInfoBtn = userInfoModal.querySelector(".close-btn");

        editProfileBtn.addEventListener('click', (e) => {
            e.preventDefault();
            userInfoModal.style.display = 'flex';
        });

        if (closeUserInfoBtn) { // 닫기 버튼이 있는지 확인
            closeUserInfoBtn.addEventListener('click', () => {
                userInfoModal.style.display = 'none';
            });
        }

        window.addEventListener('click', (e) => {
            if (e.target === userInfoModal) {
                userInfoModal.style.display = 'none';
            }
        });

        if (btnWithdraw) {
            btnWithdraw.addEventListener("click", function() {
                // 1. 단순 확인 (한 번만 묻기)
                if (confirm("정말 탈퇴하시겠습니까?")) {

                    // 2. 작별 인사 알림창
                    alert("그동안 FLOBANK를 이용해주셔서 감사합니다.");

                    // 3. 확인 버튼을 누르면 서버의 탈퇴 처리 URL로 이동
                    // (아래 컨트롤러를 만들어야 404 에러가 안 뜹니다!)
                    location.href = "/mypage/withdraw";
                }
            });
        }

    }

    // -------------------------------------------------------------
    // 환전 상세 내역 모달 (exchangeModal)
    // -------------------------------------------------------------
    const exchangeModal = document.getElementById("exchangeModal");
    if (exchangeModal) {
        const closeExchangeBtn = exchangeModal.querySelector(".close-btn");

        document.querySelectorAll(".exchange-link").forEach(btn => {
            btn.addEventListener("click", () => {
                exchangeModal.style.display = "flex";
                // TODO: 실제 데이터 fetch
            });
        });

        if (closeExchangeBtn) {
            closeExchangeBtn.addEventListener("click", () => {
                exchangeModal.style.display = "none";
            });
        }

        window.addEventListener("click", e => {
            if (e.target === exchangeModal) {
                exchangeModal.style.display = "none";
            }
        });
    }

    // -------------------------------------------------------------
    // 계좌 거래내역 모달 (accountModal)
    // -------------------------------------------------------------
    // -------------------------------------------------------------
    // 계좌 거래내역 모달 (accountModal)
    // -------------------------------------------------------------
    const accountModal = document.getElementById("accountModal");

    if (accountModal) {
        const closeAccountBtn = accountModal.querySelector(".close-btn");

        // HTML에서 비워둔 칸들 (JS로 채울 예정)
        const modalAccName = document.getElementById("acc-name");
        const modalAccNum = document.getElementById("acc-number");
        const modalAccDate = document.getElementById("acc-date");
        const historyTbody = document.getElementById("history-tbody");

        // 숫자 포맷터 (1,000원)
        const formatter = new Intl.NumberFormat('ko-KR');

        // [핵심] .account-link 클래스를 가진 모든 버튼에 클릭 이벤트 부여
        document.querySelectorAll(".account-link").forEach(btn => {
            btn.addEventListener("click", async function() {

                // 1. 클릭한 버튼의 텍스트(계좌번호)를 가져옴
                const acctNo = this.textContent.trim();

                // 2. 모달 열기 & 로딩 표시
                historyTbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:20px;">내역을 불러오는 중...</td></tr>';
                accountModal.style.display = "flex";

                try {
                    // 3. 서버로 계좌번호 전송 (AJAX)
                    const response = await fetch('/flobank/mypage/transactionHistory', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ acctNo: acctNo })
                    });

                    const result = await response.json();

                    if (result.status === 'success') {
                        const acc = result.data.account;     // 계좌 기본정보
                        const histList = result.data.history; // 거래내역 리스트 (잔액 계산됨)

                        // 4. 상단 정보 채우기
                        modalAccName.textContent = acc.acctName;
                        modalAccNum.textContent = acc.acctNo;
                        // 날짜가 있으면 앞 10자리만 자르기 (2024-06-15)
                        modalAccDate.textContent = acc.acctRegDt ? acc.acctRegDt.substring(0, 10) : '-';

                        // 5. 하단 리스트 채우기
                        historyTbody.innerHTML = ""; // 초기화

                        if (histList.length === 0) {
                            // colspan="6"으로 변경
                            historyTbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 20px;">거래 내역이 없습니다.</td></tr>';
                        } else {
                            histList.forEach(h => {
                                const tr = document.createElement("tr");

                                // 1. 스타일링 설정
                                let typeStr = "기타";
                                let typeColor = "#333";
                                let amountColor = "#333";
                                let amountPrefix = "";

                                // 2. 거래 전 잔액 계산을 위한 변수
                                let beforeBalance = 0;

                                if (h.tranType === 1) {
                                    // [입금]
                                    typeStr = "입금";
                                    typeColor = "blue";
                                    amountColor = "blue";
                                    amountPrefix = "+";
                                    beforeBalance = h.tranBalance - h.tranAmount;

                                } else if (h.tranType === 2) {
                                    // [출금]
                                    typeStr = "출금";
                                    typeColor = "red";
                                    amountColor = "red";
                                    amountPrefix = "-";
                                    beforeBalance = h.tranBalance + h.tranAmount;
                                }

                                // 3. HTML 조립
                                tr.innerHTML = `
                                    <td>${h.tranDt}</td>
                                    <td style="color: ${typeColor}; font-weight:bold;">${typeStr}</td>
                                    
                                    <td style="text-align:right; padding-right:20px; color: #666;">
                                        ${formatter.format(beforeBalance)}원
                                    </td>

                                    <td style="color: ${amountColor}; text-align:right; padding-right:20px;">
                                        ${amountPrefix}${formatter.format(h.tranAmount)}원
                                    </td>
                                    <td style="text-align:right; padding-right:20px; font-weight:bold;">
                                        ${formatter.format(h.tranBalance)}원
                                    </td>
                                    <td>${h.tranMemo || ''}</td>
                                `;
                                historyTbody.appendChild(tr);
                            });
                        }
                    } else {
                        // 성공 응답이 아닐 경우 (status != success)
                        alert("데이터를 불러오지 못했습니다.");
                        accountModal.style.display = "none";
                    }

                } catch (err) {
                    // try 블록에서 에러 발생 시
                    console.error(err);
                    historyTbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:red;">오류가 발생했습니다.</td></tr>';
                }
            }); // click 이벤트 끝
        }); // forEach 끝

        // 닫기 버튼 (X)
        if (closeAccountBtn) {
            closeAccountBtn.addEventListener("click", () => accountModal.style.display = "none");
        }

        // 배경 클릭 닫기
        window.addEventListener("click", (e) => {
            if (e.target === accountModal) accountModal.style.display = "none";
        });
    }


    // -------------------------------------------------------------
    // 예금 상세정보 모달 (depositModal)
    // -------------------------------------------------------------
    const depositModal = document.getElementById("depositModal");
    if (depositModal) {
        const closeDepositBtn = depositModal.querySelector(".close-btn");
        const depositDetailTable = depositModal.querySelector(".detail-table tbody");
        const depositHistoryTbody = document.getElementById("depositHistory");

        // --- 더미 데이터 (예시용) ---
        const deposits = {
            1: {
                name: "플로달러예금", type: "거치식", account: "112-2188-1931-00", balance: "$3,200",
                startDate: "2024.10.03", endDate: "2029.10.03 (D-159)", currency: "USD",
                rateBase: "가입 당시 환율 1,350.25원/USD", minWithdraw: "$100 이상",
                earlyRate: "연 1.2% (중도해지 시)", autoRenew: "가능 (3개월 주기)",
                interestPayment: "만기 일시지급",
                history: [
                    ["2025.10.15 13:56:23", "$200 입금", "$3,200"],
                    ["2025.09.12 09:22:11", "$150 입금", "$3,000"],
                ],
            },
            2: {
                name: "글로벌유로예금", type: "자유적립식", account: "112-5566-1823-00", balance: "€1,800",
                startDate: "2023.06.22", endDate: "2027.06.22 (D-98)", currency: "EUR",
                rateBase: "가입 당시 환율 1,470.42원/EUR", monthlyDeposit: "€300",
                maxDepositCount: "12회 중 5회 납입", extraDeposit: "가능 (남은 2회)",
                partialWithdraw: "가능 (남은 1회)", autoRenew: "불가능",
                history: [
                    ["2025.10.10 15:20:33", "€300 입금", "€1,800"],
                    ["2025.08.05 10:00:00", "€200 입금", "€1,500"],
                ],
            },
        };

        // --- 예금 이름 클릭 시 모달 열기 ---
        document.querySelectorAll(".deposit-name").forEach(link => {
            link.addEventListener("click", e => {
                e.preventDefault();
                const id = link.dataset.id;
                const data = deposits[id];
                if (!data) return; // 데이터 없으면 중단

                // (테이블 갱신 로직 ...)
                let html = `
                    <tr><th>예금이름</th><td>${data.name}</td></tr>
                    <tr><th>예금유형</th><td>${data.type}</td></tr>
                    <tr><th>예금계좌</th><td>${data.account}</td></tr>
                    <tr><th>잔액</th><td>${data.balance}</td></tr>
                    <tr><th>가입일</th><td>${data.startDate}</td></tr>
                    <tr><th>만기일</th><td>${data.endDate}</td></tr>
                    <tr><th>가입 통화</th><td>${data.currency}</td></tr>
                    <tr><th>환율 적용 기준</th><td>${data.rateBase}</td></tr>
                `;
                if (data.type === "거치식") {
                    html += `
                    <tr><th>최소출금금액</th><td>${data.minWithdraw}</td></tr>
                    <tr><th>중도해지이율</th><td>${data.earlyRate}</td></tr>
                    <tr><th>자동 연장</th><td>${data.autoRenew}</td></tr>
                    <tr><th>이자 지급 방식</th><td>${data.interestPayment}</td></tr>
                    `;
                } else if (data.type === "자유적립식") {
                    html += `
                    <tr><th>월 납입금액</th><td>${data.monthlyDeposit}</td></tr>
                    <tr><th>납입횟수</th><td>${data.maxDepositCount}</td></tr>
                    <tr><th>추가납입</th><td>${data.extraDeposit}</td></tr>
                    <tr><th>자동 연장</th><td>${data.autoRenew}</td></tr>
                    <tr><th>일부 출금</th><td>${data.partialWithdraw}</td></tr>
                    `;
                }
                depositDetailTable.innerHTML = html;

                // (거래내역 갱신 로직 ...)
                depositHistoryTbody.innerHTML = "";
                data.history.forEach(row => {
                    const tr = document.createElement("tr");
                    row.forEach(cell => {
                        const td = document.createElement("td");
                        td.textContent = cell;
                        tr.appendChild(td);
                    });
                    depositHistoryTbody.appendChild(tr);
                });

                depositModal.style.display = "flex";
            });
        });

        if (closeDepositBtn) {
            closeDepositBtn.addEventListener("click", () => {
                depositModal.style.display = "none";
            });
        }
        window.addEventListener("click", (e) => {
            if (e.target === depositModal) {
                depositModal.style.display = "none";
            }
        });
    }

    // -------------------------------------------------------------
    // [신규] 계좌 별명 수정 (AJAX)
    // -------------------------------------------------------------
    const accountListTable = document.getElementById("accountListTable");

    if (accountListTable) {

        // 1. 상태를 원래대로 되돌리는 함수
        function resetNameEdit(row) {
            row.querySelector('.acct-name-display').style.display = 'inline';
            row.querySelector('.js-edit-name').style.display = 'inline-block';
            row.querySelector('.acct-name-input').style.display = 'none';
            row.querySelector('.js-save-name').style.display = 'none';
            row.querySelector('.js-cancel-name').style.display = 'none';
            row.querySelector('.acct-name-error').style.display = 'none';
            // 값 원복 (input의 값을 span의 텍스트로)
            row.querySelector('.acct-name-input').value = row.querySelector('.acct-name-display').textContent;
        }

        accountListTable.addEventListener('click', async function(e) {
            // 클릭된 요소에서 가장 가까운 버튼(연필, 확인, 취소)을 찾음
            const button = e.target.closest('button.edit-btn');
            if (!button) return; // 버튼이 아니면 종료

            const row = button.closest('tr');
            const nameDisplay = row.querySelector('.acct-name-display');
            const nameInput = row.querySelector('.acct-name-input');
            const editBtn = row.querySelector('.js-edit-name');
            const saveBtn = row.querySelector('.js-save-name');
            const cancelBtn = row.querySelector('.js-cancel-name');
            const errorDiv = row.querySelector('.acct-name-error');

            // 2. [연필] 수정 버튼 클릭 시
            if (button.classList.contains('js-edit-name')) {
                // 다른 수정 중인 항목이 있다면 초기화
                document.querySelectorAll('#accountListTable .acct-name-wrapper').forEach(wrapper => {
                    const parentRow = wrapper.closest('tr');
                    if (parentRow !== row) {
                        resetNameEdit(parentRow);
                    }
                });

                nameDisplay.style.display = 'none';
                editBtn.style.display = 'none';
                nameInput.style.display = 'inline-block';
                saveBtn.style.display = 'inline-block';
                cancelBtn.style.display = 'inline-block';
                nameInput.focus();
            }

            // 3. [X] 취소 버튼 클릭 시
            if (button.classList.contains('js-cancel-name')) {
                resetNameEdit(row);
            }

            // 4. [V] 저장 버튼 클릭 시 (AJAX)
            if (button.classList.contains('js-save-name')) {
                const acctNo = button.dataset.acctNo;
                const acctType = button.dataset.acctType; // "KRW" or "FRGN"
                const newName = nameInput.value.trim();

                errorDiv.style.display = 'none'; // 에러 메시지 초기화

                // 4-1. 유효성 검사
                if (!newName || newName.length < 1 || newName.length > 20) {
                    errorDiv.textContent = '별명은 1자 이상 20자 이하로 입력하세요.';
                    errorDiv.style.display = 'block';
                    return;
                }

                // 4-2. AJAX 요청
                try {
                    const response = await fetch('/flobank/mypage/updateAcctName', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            acctNo: acctNo,
                            acctName: newName,
                            acctType: acctType
                        })
                    });

                    if (!response.ok) {
                        const errorData = await response.json();
                        throw new Error(errorData.message || '서버 통신에 실패했습니다.');
                    }

                    const result = await response.json();

                    if (result.status === 'success') {
                        // 4-3. 성공 시: UI 업데이트
                        nameDisplay.textContent = newName;
                        resetNameEdit(row);
                    } else {
                        throw new Error(result.message || '저장에 실패했습니다.');
                    }

                } catch (err) {
                    errorDiv.textContent = err.message;
                    errorDiv.style.display = 'block';
                }
            }
        });
    }

    // -------------------------------------------------------------
    // [비밀번호 변경 로직] 현재 비밀번호 확인 -> 새 비밀번호 표시
    // -------------------------------------------------------------
    const btnCheckPw = document.getElementById("btn-check-pw");
    const currentPwInput = document.getElementById("currentPw");
    const pwCheckMsg = document.getElementById("pw-check-msg");
    const step2Section = document.getElementById("step2-new-pw");

    if (btnCheckPw) {
        btnCheckPw.addEventListener("click", async function() {
            const inputPw = currentPwInput.value;

            if (!inputPw) {
                alert("현재 비밀번호를 입력해주세요.");
                return;
            }

            try {
                // 1. 서버에 비밀번호 검증 요청 (AJAX)
                const response = await fetch('/flobank/mypage/checkPw', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ password: inputPw })
                });

                const result = await response.json();

                if (result.status === 'success') {
                    // 2. 일치 시: 성공 메시지 + 입력창 잠금 + 새 비밀번호 창 열기
                    pwCheckMsg.style.display = 'block';
                    pwCheckMsg.style.color = 'blue';
                    pwCheckMsg.textContent = "비밀번호가 확인되었습니다.";

                    currentPwInput.readOnly = true;     // 수정 못하게 막음
                    btnCheckPw.disabled = true;         // 버튼 비활성화
                    step2Section.style.display = 'block'; // 2단계 열기

                } else {
                    // 3. 불일치 시: 에러 메시지
                    pwCheckMsg.style.display = 'block';
                    pwCheckMsg.style.color = 'red';
                    pwCheckMsg.textContent = "비밀번호가 일치하지 않습니다.";
                    currentPwInput.value = ""; // 비번 초기화
                    currentPwInput.focus();
                }

            } catch (error) {
                console.error("비밀번호 확인 중 오류:", error);
                alert("서버 오류가 발생했습니다.");
            }
        });
    }

    // (추가) 새 비밀번호와 새 비밀번호 확인 일치 여부 실시간 체크
    const newPw = document.getElementById("newPw");
    const confirmNewPw = document.getElementById("confirmNewPw");
    const newPwMsg = document.getElementById("new-pw-msg");

    function checkNewPwMatch() {
        const p1 = newPw.value;
        const p2 = confirmNewPw.value;

        if (p1 && p2) {
            newPwMsg.style.display = 'block';
            if (p1 === p2) {
                newPwMsg.style.color = 'blue';
                newPwMsg.textContent = "비밀번호가 일치합니다.";
            } else {
                newPwMsg.style.color = 'red';
                newPwMsg.textContent = "비밀번호가 서로 다릅니다.";
            }
        } else {
            newPwMsg.style.display = 'none';
        }
    }

    if(newPw && confirmNewPw) {
        newPw.addEventListener("keyup", checkNewPwMatch);
        confirmNewPw.addEventListener("keyup", checkNewPwMatch);
    }


    const btnSearchZip = document.getElementById("btn-search-zip");

    if (btnSearchZip) {
        btnSearchZip.addEventListener("click", function() {
            new daum.Postcode({
                oncomplete: function(data) {
                    // 1. 도로명/지번 주소 선택 로직
                    var addr = ''; // 주소 변수

                    if (data.userSelectedType === 'R') { // 도로명 주소 선택 시
                        addr = data.roadAddress;
                    } else { // 지번 주소 선택 시
                        addr = data.jibunAddress;
                    }

                    // 2. 우편번호와 기본주소 입력
                    document.getElementById('zipcode').value = data.zonecode;
                    document.getElementById('addr1').value = addr;

                    // 3. 상세주소 입력칸으로 포커스 이동 및 초기화
                    const addr2Input = document.getElementById('addr2');
                    addr2Input.value = '';
                    addr2Input.focus();
                }
            }).open();
        });
    }

    const btnSave = document.querySelector('.btn-save'); // 저장 버튼

    if (btnSave) {
        btnSave.addEventListener('click', async function() {

            // 1. 보낼 데이터 준비 (기본 정보)
            const updateData = {
                email: document.getElementById('email').value,
                hp: document.getElementById('hp').value,
                zipcode: document.getElementById('zipcode').value,
                addr1: document.getElementById('addr1').value,
                addr2: document.getElementById('addr2').value
            };

            // 2. 비밀번호 변경 로직 확인
            // (step2 영역이 보이고, 새 비밀번호가 입력되었을 때만 비번 변경 요청)
            const step2Div = document.getElementById('step2-new-pw');
            const newPwVal = document.getElementById('newPw').value;
            const confirmPwVal = document.getElementById('confirmNewPw').value;

            if (step2Div.style.display !== 'none' && newPwVal.trim() !== "") {
                // 유효성 검사
                if (newPwVal !== confirmPwVal) {
                    alert("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
                    return;
                }
                // 비밀번호 데이터에 추가
                updateData.newPassword = newPwVal;
            }

            // 3. 서버로 전송 (AJAX)
            try {
                const response = await fetch('/flobank/mypage/updateUserInfo', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(updateData)
                });

                const result = await response.json();

                if (result.status === 'success') {
                    alert("회원 정보가 성공적으로 수정되었습니다.");
                    location.reload(); // 화면 새로고침해서 변경된 정보 반영
                } else {
                    alert("수정 실패: " + result.message);
                }

            } catch (error) {
                console.error("정보 수정 중 오류:", error);
                alert("서버 통신 오류가 발생했습니다.");
            }
        });
    }


}); // <-- 메인 DOMContentLoaded 닫기