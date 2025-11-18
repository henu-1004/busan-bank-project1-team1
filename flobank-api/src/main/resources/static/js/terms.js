/* ============================================================
   약관 수정 모달
============================================================ */

// 모달 요소
const termsModal = document.getElementById("termsModal");
const modalClose = document.querySelector(".terms-modal-close");

// 수정 버튼 클릭 이벤트
document.querySelectorAll(".terms-edit-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {

        // 🔹 클릭된 테이블 row 찾기
        const row = e.target.closest("tr");

        // 🔥 실제 데이터(row에서 가져오기)
        const category = row.children[0].innerText;
        const title = row.children[1].innerText;
        const version = row.children[2].innerText;
        const regDate = row.children[3].innerText;
        const writer = "홍길동 관리자";  // 서버 연동 시 실제 작성자로 대체

        // 🔥 모달에 값 삽입
        document.getElementById("modalCategory").value = category;
        document.getElementById("modalTitle").value = title;
        document.getElementById("modalVersion").value = version;
        document.getElementById("modalRegDate").value = regDate;
        document.getElementById("modalWriter").value = writer;
        document.getElementById("modalContent").value =
            "여기에 약관 내용이 들어갑니다."; // 실제 내용 받아오면 교체

        // 모달 열기
        termsModal.style.display = "block";
    });
});

// 모달 닫기
modalClose.addEventListener("click", () => {
    termsModal.style.display = "none";
});

// 바깥 클릭하면 닫기
window.addEventListener("click", (e) => {
    if (e.target === termsModal) {
        termsModal.style.display = "none";
    }
});


/* ============================================================
   약관 삭제
============================================================ */

document.querySelectorAll(".terms-delete-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {

        const row = e.target.closest("tr");

        // 삭제 확인 팝업
        const result = confirm("정말 삭제하시겠습니까?");

        if (result) {

            // 🔥 1) 화면에서 행 삭제
            row.remove();

            // 🔥 2) 서버 연동이 필요한 경우 (추후 사용)
            /*
            const termsId = row.dataset.id;
            fetch(`/admin/terms/delete/${termsId}`, {
                method: "DELETE"
            }).then(res => {
                if (res.ok) {
                    row.remove();
                } else {
                    alert("삭제 실패");
                }
            });
            */
        }
    });
});
