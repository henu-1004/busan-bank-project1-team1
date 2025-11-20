document.addEventListener("DOMContentLoaded", () => {
  console.log("✅ Q&A JS Loaded");

  /* ==========================
     🔹 Q&A 상태 필터
  ========================== */
  const filterSelect = document.getElementById("qna-status-filter");
  const rows = document.querySelectorAll(".qna-table tbody tr");

  if (filterSelect) {
    filterSelect.addEventListener("change", () => {
      const selected = filterSelect.value;

      rows.forEach((row) => {
        const status = row.getAttribute("data-status");

        if (selected === "all" || status === selected) {
          row.style.display = "";
        } else {
          row.style.display = "none";
        }
      });
    });
  }

  /* ==========================
     🔹 Q&A 모달 관련
  ========================== */
  const modal = document.getElementById("qnaModal");
  const modalClose = document.querySelector(".qna-modal-close");
  const cancelBtn = document.querySelector(".qna-btn-cancel");
  const approveBtn = document.getElementById("approveBtn");

  const questionText = document.getElementById("modal-question-text");
  const aiAnswer = document.getElementById("modal-ai-answer");

  const approveButtons = document.querySelectorAll(".qna-btn-approve");

  // ✅ 승인 버튼 클릭 → 모달 열기
  approveButtons.forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const row = btn.closest("tr");
      const question = row.children[2].textContent.trim();

      // 임시로 AI 초안 내용 지정 (백엔드 연동 전)
      const aiDraft =
        "AI가 생성한 예시 답변입니다.\n환전 가능한 통화는 USD, JPY, EUR, CNH 등이며, 지점별 취급 통화가 상이할 수 있습니다.";

      questionText.textContent = question;
      aiAnswer.textContent = aiDraft;

      modal.style.display = "block";
      modal.dataset.currentRow = row.rowIndex; // 현재 행 기억
    });
  });

  // ✅ 모달 닫기 공통 함수
  const closeModal = () => {
    modal.style.display = "none";
    aiAnswer.textContent = "";
  };

  if (modalClose) modalClose.addEventListener("click", closeModal);
  if (cancelBtn) cancelBtn.addEventListener("click", closeModal);

  window.addEventListener("click", (e) => {
    if (e.target === modal) closeModal();
  });

  // ✅ 승인 버튼 클릭 → 상태 변경 + 모달 닫기
  if (approveBtn) {
    approveBtn.addEventListener("click", () => {
      const rowIndex = modal.dataset.currentRow;
      const table = document.querySelector(".qna-table");
      const row = table.rows[rowIndex];
      const statusCell = row.querySelector(".qna-status");
      const actionCell = row.lastElementChild;

      // 상태 변경
      row.dataset.status = "complete";
      statusCell.textContent = "답변 완료";
      statusCell.className = "qna-status complete";

      // 버튼 교체
      actionCell.innerHTML = `<button class="qna-btn-view">보기</button>`;

      closeModal();
    });
  }
});
