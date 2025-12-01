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
  const modalHint = document.getElementById("qnaModalHint");
  const modalSave = document.getElementById("qnaModalSave");
  const modalDelete = document.getElementById("qnaModalDelete");

  let activeTrigger = null;
  let activeRow = null;

  const escapeHtml = (text = "") =>
    text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  const normalizeStatus = (status = "") => status.toLowerCase();

  const shouldUseReply = (status, reply) => {
    const normalizedStatus = normalizeStatus(status);
    if (["answered", "safe", "complete"].includes(normalizedStatus)) return true;
    return reply && reply.trim().length > 0;
  };

  const setStatusBadge = (reply, draft, status) => {
    if (!modalStatus) return;

    const normalizedStatus = (status || "").toLowerCase();
    const hasReply =
      normalizedStatus === "answered" ||
      normalizedStatus === "safe" ||
      normalizedStatus === "complete" ||
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

  const setHintMessage = (status, reply, draft) => {
    if (!modalHint) return;
    const normalizedStatus = normalizeStatus(status);
    if (shouldUseReply(normalizedStatus, reply)) {
      modalHint.textContent = "등록된 답변을 수정할 수 있습니다.";
      return;
    }
    if (draft && draft.trim().length > 0) {
      modalHint.textContent = "AI 생성 초안입니다. 검토 후 저장하면 답변으로 등록됩니다.";
      return;
    }
    modalHint.textContent = "문의에 대한 답변을 작성해주세요.";
  };

  const applyRowStatus = (row, reply) => {
    if (!row) return;
    const badge = row.querySelector(".qna-status");
    if (!badge) return;
    const hasReply = reply && reply.trim().length > 0;
    badge.classList.remove("pending", "complete");
    if (hasReply) {
      badge.classList.add("complete");
      badge.textContent = "답변 완료";
    } else {
      badge.classList.add("pending");
      badge.textContent = "답변 대기중";
    }
  };

  const openModal = (trigger) => {
    if (!modal || !modalStatus || !modalQuestion || !modalAnswer || !modalTitle) return;

    activeTrigger = trigger;
    activeRow = trigger.closest("tr");

    const title = trigger.dataset.title || "문의 제목";
    const content = trigger.dataset.content || "등록된 내용이 없습니다.";
    const draft = trigger.dataset.draft || "";
    const reply = trigger.dataset.reply || "";
    const writer = trigger.dataset.writer || "";
    const date = trigger.dataset.date || "";
    const status = trigger.dataset.status || "";
    const qnaNo = trigger.dataset.no || "";

    setStatusBadge(reply, draft, status);
    setHintMessage(status, reply, draft);

    if (modalWriter) modalWriter.textContent = writer;
    if (modalDate) modalDate.textContent = date;
    modalTitle.textContent = title;
    modalQuestion.innerHTML = escapeHtml(content).replace(/\n/g, "<br />");

    const answerText = shouldUseReply(status, reply) ? reply : draft;
    modalAnswer.value = answerText || "";
    modalAnswer.dataset.qnaNo = qnaNo;
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

  modalSave?.addEventListener("click", async () => {
    if (!activeTrigger) return;
    const qnaNo = modalAnswer?.dataset.qnaNo;
    if (!qnaNo) return;

    const replyText = modalAnswer.value ?? "";
    modalSave.disabled = true;
    modalSave.textContent = "저장 중...";

    try {
      const res = await fetch(`/admin/api/qna/${qnaNo}/reply`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ reply: replyText }),
      });

      if (!res.ok) {
        throw new Error("failed to save");
      }

      const data = await res.json();
      const updatedReply = data.reply ?? "";
      const updatedStatus = data.status || (updatedReply.trim().length > 0 ? "answered" : "pending");

      activeTrigger.dataset.reply = updatedReply;
      activeTrigger.dataset.status = updatedStatus;
      setStatusBadge(updatedReply, activeTrigger.dataset.draft, updatedStatus);
      setHintMessage(updatedStatus, updatedReply, activeTrigger.dataset.draft);
      applyRowStatus(activeRow, updatedReply);
    } catch (err) {
      alert("답변 저장 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      modalSave.disabled = false;
      modalSave.textContent = "저장";
    }
  });

  modalDelete?.addEventListener("click", async () => {
    if (!activeTrigger || !activeRow) return;
    const qnaNo = modalAnswer?.dataset.qnaNo;
    if (!qnaNo) return;
    if (!confirm("이 문의를 삭제하시겠습니까?")) return;

    modalDelete.disabled = true;

    try {
      const res = await fetch(`/admin/api/qna/${qnaNo}`, { method: "DELETE" });
      if (!res.ok) throw new Error("delete failed");

      activeRow.remove();
      closeModal();
    } catch (err) {
      alert("삭제 중 문제가 발생했습니다. 다시 시도해주세요.");
    } finally {
      modalDelete.disabled = false;
    }
  });
});
