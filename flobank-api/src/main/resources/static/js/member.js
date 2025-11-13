document.addEventListener("DOMContentLoaded", () => {

    /* ============================================================
       1. 약관 동의 페이지 (member/terms.html) 로직
       ============================================================ */
    const agreeAll = document.getElementById('agreeAll');

    // 'agreeAll' 요소가 있는 페이지에서만 실행 (register.html 등에서는 무시됨)
    if (agreeAll) {
        // .terms-check 클래스 내부의 input 태그들 선택
        // (register.html의 form 구조와 겹치지 않도록 클래스명으로 찾는 것이 안전합니다)
        const checkboxes = document.querySelectorAll('.terms-check input');

        // 전체 동의 체크박스 이벤트
        agreeAll.addEventListener('change', () => {
            checkboxes.forEach(chk => chk.checked = agreeAll.checked);
        });

        // 개별 체크박스 클릭 시 전체 동의 상태 업데이트
        checkboxes.forEach(chk => {
            chk.addEventListener('change', () => {
                // 배열로 변환 후 모든 체크박스가 체크되었는지 확인 (.every)
                const allChecked = Array.from(checkboxes).every(c => c.checked);
                agreeAll.checked = allChecked;
            });
        });
    }


    /* ============================================================
       2. 회원가입 페이지 (member/register.html) - 아이디 중복 확인 로직
       ============================================================ */
    const idInput = document.getElementById('reg-custId');

    // 'reg-custId' 입력창이 있는 페이지에서만 실행 (terms.html 등에서는 무시됨)
    if (idInput) {
        // 중복확인 버튼(register-btn)을 주입
        const idCheckBtn = idInput.parentElement.querySelector('.register-btn');

        if (idCheckBtn) {
            idCheckBtn.addEventListener('click', function() {
                const custId = idInput.value.trim(); // 공백 제거

                // 1. 예외처리: 빈 값 입력 시
                if (!custId) {
                    alert("아이디를 입력해주세요.");
                    idInput.focus();
                    return;
                }

                // 2. 서버로 비동기 요청 (AJAX)
                fetch('/flobank/member/checkId', {
                    method: 'POST',
                    headers: {
                        // @RequestParam으로 받기 위해 폼 데이터 형식으로 전송
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: 'custId=' + encodeURIComponent(custId)
                })
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('네트워크 응답에 문제가 있습니다.');
                        }
                        return response.json(); // 서버에서 보낸 true/false (boolean) 반환
                    })
                    .then(isDuplicated => {
                        if (isDuplicated) {
                            // true: 중복됨 (이미 있음)
                            alert("이미 사용 중인 아이디입니다.");
                            idInput.value = ""; // 입력한 내용 지우기
                            idInput.focus();
                        } else {
                            // false: 사용 가능 (없음)
                            alert("사용 가능한 아이디입니다.");

                            // (선택사항) 사용 가능한 아이디는 수정 못하게 막아두기
                            // idInput.readOnly = true;
                            // idInput.style.backgroundColor = "#f0f0f0";
                        }
                    })
                    .catch(error => {
                        console.error('Error:', error);
                        alert("중복 확인 중 오류가 발생했습니다.");
                    });
            });
        }
    }

});