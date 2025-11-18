// -------------------------------------------------------------
// 사용자 정보 변경 모달 (userInfoModal)
// -------------------------------------------------------------
document.querySelector('.profile-footer a').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('userInfoModal').style.display = 'flex';
});

document.querySelector('.close-btn').addEventListener('click', () => {
    document.getElementById('userInfoModal').style.display = 'none';
});

window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        document.getElementById('userInfoModal').style.display = 'none';
    }
});


document.addEventListener("DOMContentLoaded", function () {
    // -------------------------------------------------------------
    // 환전 상세 내역 모달 (exchangeModal)
    // -------------------------------------------------------------
    const exchangeModal = document.getElementById("exchangeModal");
    const closeExchangeBtn = exchangeModal.querySelector(".close-btn");

    document.querySelectorAll(".exchange-link").forEach(btn => {
        btn.addEventListener("click", () => {
            exchangeModal.style.display = "flex";
        });
    });

    closeExchangeBtn.addEventListener("click", () => {
        exchangeModal.style.display = "none";
    });

    window.addEventListener("click", e => {
        if (e.target === exchangeModal) {
            exchangeModal.style.display = "none";
        }
    });

    // -------------------------------------------------------------
    // 계좌 거래내역 모달 (accountModal)
    // -------------------------------------------------------------
    const accountModal = document.getElementById("accountModal");
    const closeAccountBtn = accountModal.querySelector(".close-btn");

    document.querySelectorAll(".account-table td:nth-child(2)").forEach(cell => {
        cell.addEventListener("click", () => {
            accountModal.style.display = "flex";
        });
    });

    closeAccountBtn.addEventListener("click", () => {
        accountModal.style.display = "none";
    });

    window.addEventListener("click", (e) => {
        if (e.target === accountModal) {
            accountModal.style.display = "none";
        }
    });

    // -------------------------------------------------------------
    // 예금 상세정보 모달 (depositModal)
    // -------------------------------------------------------------
    const depositModal = document.getElementById("depositModal");
    // ... (기존 예금 상세정보 모달 로직 전체) ...
    const closeDepositBtn = depositModal.querySelector(".close-btn");
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
            // 값 원복 (input의 값을 span의 값으로)
            row.querySelector('.acct-name-input').value = row.querySelector('.acct-name-display').textContent;
        }

        accountListTable.addEventListener('click', async function(e) {
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
                const acctType = button.dataset.acctType;
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
                            acctType: acctType // "KRW" 또는 "FRGN"
                        })
                    });

                    if (!response.ok) {
                        throw new Error('서버 통신에 실패했습니다.');
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
});