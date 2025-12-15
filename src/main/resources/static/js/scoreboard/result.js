/**
 * Result Page JavaScript - Mobile Optimized
 * Handles user interactions and enhancements for match result display
 */

document.addEventListener("DOMContentLoaded", function () {
  // Auto-refresh prevention for result page
  window.addEventListener("beforeunload", function (e) {
    // Can add confirmation dialog if needed
    // e.preventDefault();
    // e.returnValue = '';
  });

  // Mobile touch enhancements
  if ("ontouchstart" in window) {
    const actionButtons = document.querySelectorAll(".btn-action");

    actionButtons.forEach((btn) => {
      // Touch start effect
      btn.addEventListener(
        "touchstart",
        function () {
          this.style.transform = "scale(0.95)";
          this.style.transition = "transform 0.1s ease";
        },
        { passive: true }
      );

      // Touch end effect
      btn.addEventListener(
        "touchend",
        function () {
          this.style.transform = "";
          this.style.transition = "all 0.3s ease";
        },
        { passive: true }
      );

      // Cancel touch effect if touch is cancelled
      btn.addEventListener(
        "touchcancel",
        function () {
          this.style.transform = "";
          this.style.transition = "all 0.3s ease";
        },
        { passive: true }
      );
    });
  }

  // Keyboard navigation
  document.addEventListener("keydown", function (e) {
    switch (e.key) {
      case "Escape":
      case "Backspace":
        e.preventDefault();
        goBackToScoreboard();
        break;
      case "n":
      case "N":
        if (e.ctrlKey || e.metaKey) {
          e.preventDefault();
          goToNewMatch();
        }
        break;
    }
  });

  // Helper function to go back to scoreboard
  function goBackToScoreboard() {
    const backBtn = document.querySelector(".btn-back");
    if (backBtn && backBtn.href) {
      window.location.href = backBtn.href;
    }
  }

  // Helper function to start new match
  function goToNewMatch() {
    const newBtn = document.querySelector(".btn-new");
    if (newBtn && newBtn.href) {
      window.location.href = newBtn.href;
    }
  }

  // Add click handlers for better accessibility
  const backButton = document.querySelector(".btn-back");
  const newButton = document.querySelector(".btn-new");

  if (backButton) {
    backButton.addEventListener("click", function (e) {
      // Add loading state
      this.style.opacity = "0.7";
      this.innerHTML =
        '<i class="bi bi-arrow-left"></i> <span>Đang tải...</span>';
    });
  }

  if (newButton) {
    newButton.addEventListener("click", function (e) {
      // Add loading state
      this.style.opacity = "0.7";
      this.innerHTML =
        '<i class="bi bi-plus-circle"></i> <span>Đang tải...</span>';
    });
  }

  // Enhanced animation triggers
  const resultContainer = document.querySelector(".result-container");
  if (resultContainer) {
    // Trigger animation on load
    resultContainer.style.opacity = "0";
    resultContainer.style.transform = "translateY(30px)";

    setTimeout(() => {
      resultContainer.style.opacity = "1";
      resultContainer.style.transform = "translateY(0)";
      resultContainer.style.transition = "all 0.6s ease-out";
    }, 100);
  }

  // Add ripple effect for buttons on desktop
  if (!("ontouchstart" in window)) {
    document.querySelectorAll(".btn-action").forEach((btn) => {
      btn.addEventListener("click", function (e) {
        const ripple = document.createElement("span");
        const rect = this.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;

        ripple.style.width = ripple.style.height = size + "px";
        ripple.style.left = x + "px";
        ripple.style.top = y + "px";
        ripple.style.position = "absolute";
        ripple.style.borderRadius = "50%";
        ripple.style.backgroundColor = "rgba(255, 255, 255, 0.5)";
        ripple.style.transform = "scale(0)";
        ripple.style.animation = "ripple 0.6s linear";
        ripple.style.pointerEvents = "none";

        this.appendChild(ripple);

        setTimeout(() => {
          ripple.remove();
        }, 600);
      });
    });

    // Add ripple animation CSS
    const style = document.createElement("style");
    style.textContent = `
            .btn-action {
                position: relative;
                overflow: hidden;
            }
            
            @keyframes ripple {
                to {
                    transform: scale(4);
                    opacity: 0;
                }
            }
        `;
    document.head.appendChild(style);
  }

  console.log("Result page interactions initialized");
});

// Export functions for potential external use
window.ResultPage = {
  goBackToScoreboard: function () {
    const backBtn = document.querySelector(".btn-back");
    if (backBtn && backBtn.href) {
      window.location.href = backBtn.href;
    }
  },

  goToNewMatch: function () {
    const newBtn = document.querySelector(".btn-new");
    if (newBtn && newBtn.href) {
      window.location.href = newBtn.href;
    }
  },
};
