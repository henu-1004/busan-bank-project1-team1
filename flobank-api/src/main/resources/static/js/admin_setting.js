document.addEventListener("DOMContentLoaded", () => {
  const filterForm = document.getElementById("qnaFilterForm");
  const filterSelect = document.getElementById("qna-status-filter");

  if (filterForm && filterSelect) {
    filterSelect.addEventListener("change", () => {
      // 새 상태로 필터링할 때는 첫 페이지부터 다시 조회
      const qnaPageInput = filterForm.querySelector('input[name="qnaPage"]');
      if (qnaPageInput) {
        qnaPageInput.value = 1;
      }
      const action = filterForm.getAttribute("action") || window.location.pathname;
      filterForm.setAttribute("action", `${action}#qna-list`);
      filterForm.submit();
    });
  }

  const modal = document.getElementById("qnaModal");
  const modalClose = modal?.querySelector(".qna-modal-close");
  const modalCancel = modal?.querySelector(".qna-btn-cancel");
  const modalStatus = document.getElementById("qnaModalStatusBadge");
  const modalWriter = document.getElementById("qnaModalWriter");
  const modalDate = document.getElementById("qnaModalDate");
  const modalTitle = document.getElementById("qnaModalTitle");
  const modalQuestion = document.getElementById("qnaModalQuestion");
  const modalAnswer = document.getElementById("qnaModalAnswer");

  const escapeHtml = (text = "") =>
    text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  const setStatusBadge = (reply, draft, status) => {
    if (!modalStatus) return;

    const normalizedStatus = (status || "").toLowerCase();
    const hasReply =
      normalizedStatus === "answered" ||
      normalizedStatus === "safe" ||
      (reply && reply.trim().length > 0);
    const hasDraft =
      !hasReply &&
      (normalizedStatus === "draft" || (draft && draft.trim().length > 0));

    modalStatus.classList.remove("pending", "ai-draft", "complete");
    if (hasReply) {
      modalStatus.classList.add("complete");
      modalStatus.textContent = "답변 완료";
    } else if (hasDraft) {
      modalStatus.classList.add("ai-draft");
      modalStatus.textContent = "AI 초안";
    } else {
      modalStatus.classList.add("pending");
      modalStatus.textContent = "답변 대기중";
    }
  };

  const openModal = (trigger) => {
    if (!modal || !modalStatus || !modalQuestion || !modalAnswer || !modalTitle) return;

    const title = trigger.dataset.title || "문의 제목";
    const content = trigger.dataset.content || "등록된 내용이 없습니다.";
    const draft = trigger.dataset.draft || "";
    const reply = trigger.dataset.reply || "";
    const writer = trigger.dataset.writer || "";
    const date = trigger.dataset.date || "";
    const status = trigger.dataset.status || "";

    setStatusBadge(reply, draft, status);

    if (modalWriter) modalWriter.textContent = writer;
    if (modalDate) modalDate.textContent = date;
    modalTitle.textContent = title;
    modalQuestion.innerHTML = escapeHtml(content).replace(/\n/g, "<br />");

    const answerText = reply?.trim().length ? reply : draft;
    modalAnswer.value = answerText || "";
    modal.style.display = "block";
    modal.classList.add("open");
  };

  const closeModal = () => {
    if (!modal) return;
    modal.style.display = "none";
    modal.classList.remove("open");
  };

  document.querySelectorAll(".qna-btn-view").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.preventDefault();
      openModal(btn);
    });
  });

  modalClose?.addEventListener("click", closeModal);
  modalCancel?.addEventListener("click", closeModal);
  modal?.addEventListener("click", (e) => {
    if (e.target === modal) closeModal();
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && modal?.classList.contains("open")) {
      closeModal();
    }
  });
});
