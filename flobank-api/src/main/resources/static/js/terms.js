document.addEventListener("DOMContentLoaded", () => {

    /* ============================================================
       요소 선택
    ============================================================ */
    const termsModal = document.getElementById("termsModal");
    const modalClose = document.querySelector(".terms-modal-close");
    const modalCancel = document.querySelector(".terms-modal-cancel");
    const saveBtn = document.querySelector(".terms-modal-save");

    const editTitle = document.getElementById("modalTitle");
    const editContent = document.getElementById("modalContent");

    /* ============================================================
       카테고리 라벨 변환
    ============================================================ */
    const categoryLabel = (cate) => {
        const mapper = {
            1: "회원가입",
            2: "환전하기",
            3: "외화송금",
            4: "외환예금",
            5: "원화통장개설",
            6: "외화통장개설",
        };
        return mapper[cate] || "기타";
    };

    /* ============================================================
       모달 닫기
    ============================================================ */
    const closeModal = () => {
        termsModal.style.display = "none";
    };

    modalClose.addEventListener("click", closeModal);
    modalCancel.addEventListener("click", closeModal);

    window.addEventListener("click", (e) => {
        if (e.target === termsModal) closeModal();
    });

    /* ============================================================
       수정 버튼 클릭 → 모달 열기 + 데이터 로딩
    ============================================================ */
    document.querySelectorAll(".terms-edit-btn").forEach((btn) => {
        btn.addEventListener("click", async (e) => {
            e.stopPropagation();

            const cate = btn.dataset.cate;
            const order = btn.dataset.order;

            if (!cate || !order) {
                alert("선택한 약관 정보가 올바르지 않습니다.");
                return;
            }

            try {
                const res = await fetch(`/flobank/admin/terms/detail?cate=${cate}&order=${order}`);

                if (!res.ok) {
                    throw new Error("약관 정보를 불러오지 못했습니다.");
                }

                const data = await res.json();

                // HTML input에 값 채우기
                editTitle.value = data.title;
                editContent.value = data.content;

                document.getElementById("modalCategory").value = categoryLabel(cate);
                document.getElementById("modalVersion").value = "v" + data.version;
                document.getElementById("modalRegDate").value = data.regDy;
                document.getElementById("modalWriter").value = data.adminId;
                document.getElementById("modalVerMemo").value = data.verMemo ?? "";



                // 저장 버튼에 정보 저장
                saveBtn.dataset.cate = cate;
                saveBtn.dataset.order = order;
                saveBtn.dataset.version = data.version;

                // 모달 오픈
                termsModal.style.display = "block";

            } catch (err) {
                alert(err.message);
            }
        });
    });

    /* ============================================================
       저장하기 버튼 → 새 버전 생성(update)
    ============================================================ */
    saveBtn.addEventListener("click", async () => {

        const cate = saveBtn.dataset.cate;
        const order = saveBtn.dataset.order;
        const currentVersion = saveBtn.dataset.version;

        if (!cate || !order) {
            alert("수정할 약관을 먼저 선택해주세요.");
            return;
        }

        const title = editTitle.value.trim();
        const content = editContent.value.trim();

        const verMemo = document.getElementById("modalVerMemo").value.trim();

        if (!title || !content) {
            alert("제목과 내용은 비울 수 없습니다.");
            return;
        }

        const formData = new URLSearchParams();
        formData.append("cate", cate);
        formData.append("order", order);
        formData.append("title", title);
        formData.append("content", content);
        formData.append("currentVersion", currentVersion);
        formData.append("verMemo", verMemo);

        // 버튼 상태 변경
        saveBtn.disabled = true;
        const originalLabel = saveBtn.textContent;
        saveBtn.textContent = "저장 중...";

        try {
            const res = await fetch(`/flobank/admin/terms/update`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData,
            });

            const data = await res.json();

            if (data.status === "OK") {
                alert("약관이 수정되었습니다.");
                location.reload();
            } else {
                throw new Error(data.message || "수정에 실패했습니다.");
            }
        } catch (err) {
            alert(err.message);
        } finally {
            saveBtn.disabled = false;
            saveBtn.textContent = originalLabel;
        }
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
