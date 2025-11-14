// 로그인 필요 공통 이벤트 함수 (전역)
window.goKoAccountOpenMain = function () {
    const hasLoginFlag = document.cookie
        .split(";")
        .map(v => v.trim())
        .some(v => v.startsWith("loginYn=Y"));

    if (!hasLoginFlag) {
        alert("로그인 후 이용 부탁드립니다.");
        return;
    }
    window.location.href = "/flobank/mypage/account_open_main";
};

document.addEventListener("DOMContentLoaded", () => {
  /** ============================
   * ✅ 1. 네비게이션 & Mega 메뉴
   * ============================ */
  const nav = document.querySelector(".nav-menu");
  const menuItems = document.querySelectorAll(".menu-item");
  const megaMenu = document.querySelector(".mega-menu");

  if (nav && menuItems.length && megaMenu) {
    // hover 시 전체 메뉴 열림
    menuItems.forEach((item) => {
      item.addEventListener("mouseenter", () => {
        megaMenu.classList.add("show");
        menuItems.forEach((i) => i.classList.remove("active"));
        item.classList.add("active");
      });
    });

    const wrapper = document.querySelector("header");
    let isInside = false;

    wrapper.addEventListener("mouseenter", () => {
      isInside = true;
    });

    wrapper.addEventListener("mouseleave", (e) => {
      const to = e.relatedTarget;
      if (!wrapper.contains(to)) {
        isInside = false;
        megaMenu.classList.remove("show");
        menuItems.forEach((i) => i.classList.remove("active"));
      }
    });

    window.addEventListener("scroll", () => {
      megaMenu.classList.remove("show");
      menuItems.forEach((i) => i.classList.remove("active"));
    });
  }

  /** ============================
   * ✅ 2. 검색 모달
   * ============================ */
  const searchTrigger = document.querySelector(".search-trigger");
  const searchModal = document.getElementById("searchModal");
  const closeButton = searchModal?.querySelector(".search-top-sheet__close");
  const searchForm = searchModal?.querySelector(".search-top-sheet__form");
  const searchInput = document.getElementById("globalSearch");

  if (searchTrigger && searchModal) {
    const openModal = () => {
      searchModal.classList.add("open");
      searchModal.setAttribute("aria-hidden", "false");
      document.body.classList.add("modal-open");
      setTimeout(() => searchInput?.focus(), 150);
    };

    const closeModal = () => {
      searchModal.classList.remove("open");
      searchModal.setAttribute("aria-hidden", "true");
      document.body.classList.remove("modal-open");
      searchTrigger.focus();
    };

    searchTrigger.addEventListener("click", (e) => {
      e.preventDefault();
      openModal();
    });

    closeButton?.addEventListener("click", closeModal);
    searchForm?.addEventListener("submit", (e) => e.preventDefault());
    searchModal.addEventListener("click", (e) => {
      if (e.target === searchModal) closeModal();
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && searchModal.classList.contains("open"))
        closeModal();
    });
  }

  /** ============================
   * ✅ 3. 슬라이드 배너
   * ============================ */
  const slideWrapper = document.querySelector(".slides");
  const slides = document.querySelectorAll(".slide");
  const dots = document.querySelectorAll(".dot");
  const prevBtn = document.querySelector(".prev");
  const nextBtn = document.querySelector(".next");

  if (slideWrapper && slides.length) {
    slideWrapper.style.width = `${slides.length * 100}%`;
    slides.forEach(
      (slide) => (slide.style.flex = `0 0 ${100 / slides.length}%`)
    );

    let current = 0;
    let slideInterval;
    const intervalTime = 3000;

    function showSlide(index) {
      slideWrapper.style.transition = "transform 0.8s ease-in-out";
      slideWrapper.style.transform = `translateX(-${
        index * (100 / slides.length)
      }%)`;
      dots.forEach((dot) => dot.classList.remove("active"));
      dots[index].classList.add("active");
    }

    function nextSlide() {
      current = (current + 1) % slides.length;
      showSlide(current);
    }

    function prevSlide() {
      current = (current - 1 + slides.length) % slides.length;
      showSlide(current);
    }

    function startAutoSlide() {
      slideInterval = setInterval(nextSlide, intervalTime);
    }

    function stopAutoSlide() {
      clearInterval(slideInterval);
    }

    slideWrapper.addEventListener("mouseenter", stopAutoSlide);
    slideWrapper.addEventListener("mouseleave", startAutoSlide);
    nextBtn?.addEventListener("click", () => {
      nextSlide();
      stopAutoSlide();
      startAutoSlide();
    });
    prevBtn?.addEventListener("click", () => {
      prevSlide();
      stopAutoSlide();
      startAutoSlide();
    });
    dots.forEach((dot, index) => {
      dot.addEventListener("click", () => {
        current = index;
        showSlide(current);
        stopAutoSlide();
        startAutoSlide();
      });
    });

    showSlide(current);
    startAutoSlide();
  }

  /** ============================
   * ✅ 4. 언어 선택 드롭다운
   * ============================ */
  const langToggle = document.querySelector(".language-toggle");
  const langMenu = document.querySelector(".language-menu");

  if (langToggle && langMenu) {
    langToggle.addEventListener("click", (e) => {
      e.preventDefault();
      langMenu.classList.toggle("show"); 
    });

    document.addEventListener("click", (e) => {
      if (!e.target.closest(".language-dropdown")) {
        langMenu.classList.remove("show"); 
      }
    });

    langMenu.querySelectorAll("li").forEach((item) => {
      item.addEventListener("click", () => {
        alert(`${item.textContent}으로 언어가 변경됩니다.`);
        langMenu.classList.remove("show");
      });
    });
  }
});
