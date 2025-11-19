document.addEventListener("DOMContentLoaded", () => {

    /* ============================================================
       약관 수정 모달
    ============================================================ */

    const termsModal = document.getElementById("termsModal");
    const modalClose = document.querySelector(".terms-modal-close");

    // 수정 버튼 이벤트
    document.querySelectorAll(".terms-edit-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {

            const cate = btn.dataset.cate;
            const order = btn.dataset.order;

            const res = await fetch(`/flobank/admin/terms/detail?cate=${cate}&order=${order}`);
            const data = await res.json();

            document.getElementById("modalCategory").value = cate;
            document.getElementById("modalTitle").value = data.title;
            document.getElementById("modalVersion").value = data.version;
            document.getElementById("modalRegDate").value = data.regDy;
            document.getElementById("modalWriter").value = data.adminId;
            document.getElementById("modalContent").value = data.content;

            const saveBtn = document.querySelector(".terms-modal-save");
            saveBtn.dataset.cate = cate;
            saveBtn.dataset.order = order;
            saveBtn.dataset.version = data.version;

            termsModal.style.display = "block";
        });
    });

    modalClose.addEventListener("click", () => {
        termsModal.style.display = "none";
    });

    window.addEventListener("click", (e) => {
        if (e.target === termsModal) {
            termsModal.style.display = "none";
        }
    });


    /* ============================================================
       저장하기 (JS 수정 필수 부분)
    ============================================================ */
    document.querySelector(".terms-modal-save").addEventListener("click", async () => {

        const saveBtn = document.querySelector(".terms-modal-save");

        const cate = saveBtn.dataset.cate;
        const order = saveBtn.dataset.order;
        const currentVersion = saveBtn.dataset.version;

        const title = document.getElementById("modalTitle").value;
        const content = document.getElementById("modalContent").value;

        const formData = new URLSearchParams();
        formData.append("cate", cate);
        formData.append("order", order);
        formData.append("title", title);
        formData.append("content", content);
        formData.append("currentVersion", currentVersion);

        const res = await fetch("/flobank/admin/terms/update", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: formData,
        });

        const data = await res.json();

        if (data.status === "OK") {
            alert("약관이 수정되었습니다.");
            location.reload();
        } else {
            alert("수정 실패: " + data.message);
        }
    });


    /* ============================================================
       약관 삭제
    ============================================================ */
    document.querySelectorAll(".terms-delete-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {

            const cate = btn.dataset.cate;
            const order = btn.dataset.order;

            if (!confirm("정말 삭제하시겠습니까?")) return;

            const res = await fetch(`/flobank/admin/terms/delete?cate=${cate}&order=${order}`);

            if (res.ok) {
                alert("삭제되었습니다.");
                location.reload();
            } else {
                alert("삭제 실패");
            }
        });
    });


    /* ============================================================
       스크롤 위치 저장 & 복원
    ============================================================ */
    window.addEventListener("beforeunload", () => {
        localStorage.setItem("terms_scroll", String(window.scrollY));
    });

    window.addEventListener("load", () => {
        const saved = localStorage.getItem("terms_scroll");
        if (!saved) return;

        setTimeout(() => {
            window.scrollTo({
                top: parseInt(saved),
                behavior: "instant",
            });
            localStorage.removeItem("terms_scroll");
        }, 50);
    });

});
