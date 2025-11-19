// [수정] 모든 코드를 DOMContentLoaded 이벤트 리스너 안으로 이동시킵니다.
document.addEventListener("DOMContentLoaded", function () {

    // -------------------------------------------------------------
    // 사용자 정보 변경 모달 (userInfoModal)
    // -------------------------------------------------------------
    const editProfileBtn = document.getElementById('editProfileBtn');
    const userInfoModal = document.getElementById('userInfoModal');

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
    const accountModal = document.getElementById("accountModal");
    if (accountModal) {
        const closeAccountBtn = accountModal.querySelector(".close-btn");

        // [수정] 외화 '상세확인' 버튼도 포함하도록 셀렉터 변경
        document.querySelectorAll(".account-table .account-link").forEach(cell => {
            cell.addEventListener("click", () => {
                accountModal.style.display = "flex";
                // TODO: 실제 계좌번호(cell.innerText)로 fetch API 호출
            });
        });

        if (closeAccountBtn) {
            closeAccountBtn.addEventListener("click", () => {
                accountModal.style.display = "none";
            });
        }

        window.addEventListener("click", (e) => {
            if (e.target === accountModal) {
                accountModal.style.display = "none";
            }
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
}); // <-- 메인 DOMContentLoaded 닫기