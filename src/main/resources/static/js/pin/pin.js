"use strict";

$(function () {
  let currentIndex = 0;
  let isLoggedIn = false;
  let refereeLoginModal = null;

  const $digits = $(".pin-digit");
  const $submitBtn = $("#submitBtn");
  const $errorMessage = $("#errorMessage");
  const $loading = $("#loading");
  const $form = $("#pinForm");

  // ===== Device Detection =====
  function getDeviceInfo() {
    const ua = navigator.userAgent;
    console.log("User-Agent:", ua); // Debug
    let deviceId = "";
    let deviceModel = "";

    // Detect OPPO (CPH model)
    const oppoMatch = ua.match(/CPH\d+/i);
    if (oppoMatch) {
      deviceId = oppoMatch[0];
      deviceModel = "OPPO " + deviceId;
      console.log("Detected OPPO:", deviceId);
    }

    // Detect Samsung (SM- model)
    const samsungMatch = ua.match(/SM-[A-Z0-9]+/i);
    if (samsungMatch) {
      deviceId = samsungMatch[0];
      deviceModel = "Samsung " + deviceId;
      console.log("Detected Samsung:", deviceId);
    }

    // Detect Xiaomi (Redmi, Mi, POCO)
    const xiaomiMatch = ua.match(/(Redmi|Mi|POCO)\s+([A-Z0-9\s]+)/i);
    if (xiaomiMatch) {
      deviceId = xiaomiMatch[2].trim();
      deviceModel = "Xiaomi " + xiaomiMatch[1] + " " + deviceId;
      console.log("Detected Xiaomi:", deviceId);
    }

    // Detect Vivo (V model)
    const vivoMatch = ua.match(/vivo\s+(\d+)/i) || ua.match(/V\d+/i);
    if (vivoMatch) {
      deviceId = vivoMatch[1] || vivoMatch[0];
      deviceModel = "Vivo " + deviceId;
      console.log("Detected Vivo:", deviceId);
    }

    // Detect iPhone
    const iphoneMatch = ua.match(/iPhone(\d+,\d+)/i);
    if (iphoneMatch) {
      deviceId = iphoneMatch[0];
      deviceModel = getIPhoneModel(iphoneMatch[0]);
      console.log("Detected iPhone:", deviceId);
    } else if (ua.includes("iPhone")) {
      deviceId = "iPhone";
      deviceModel = "iPhone";
      console.log("Detected iPhone (generic)");
    }

    // Detect iPad
    if (ua.includes("iPad")) {
      const ipadMatch = ua.match(/iPad(\d+,\d+)/i);
      deviceId = ipadMatch ? ipadMatch[0] : "iPad";
      deviceModel = ipadMatch ? getIPadModel(ipadMatch[0]) : "iPad";
      console.log("Detected iPad:", deviceId);
    }

    // Detect Huawei
    const huaweiMatch = ua.match(/(HW-|HUAWEI\s+)([A-Z0-9-]+)/i);
    if (huaweiMatch) {
      deviceId = huaweiMatch[2];
      deviceModel = "Huawei " + deviceId;
      console.log("Detected Huawei:", deviceId);
    }

    // Fallback: Generate fingerprint from screen + navigator
    if (!deviceId) {
      deviceId = generateFingerprint();
      deviceModel = getBrowserInfo();
      console.log("Using fingerprint:", deviceId);
    }

    console.log("Final device info:", { deviceId, deviceModel });
    return { deviceId, deviceModel };
  }

  function generateFingerprint() {
    // Generate unique ID based on device characteristics
    const screen = window.screen;
    const data = [
      screen.width,
      screen.height,
      screen.colorDepth,
      navigator.platform,
      navigator.language,
      new Date().getTimezoneOffset(),
    ].join("|");

    // Simple hash
    let hash = 0;
    for (let i = 0; i < data.length; i++) {
      const char = data.charCodeAt(i);
      hash = (hash << 5) - hash + char;
      hash = hash & hash;
    }
    return "FP" + Math.abs(hash).toString(16).toUpperCase();
  }

  function getBrowserInfo() {
    const ua = navigator.userAgent;
    let browser = "Unknown Browser";

    if (ua.includes("Edg")) browser = "Edge";
    else if (ua.includes("Chrome") && !ua.includes("Edg")) browser = "Chrome";
    else if (ua.includes("Firefox")) browser = "Firefox";
    else if (ua.includes("Safari") && !ua.includes("Chrome"))
      browser = "Safari";
    else if (ua.includes("Opera") || ua.includes("OPR")) browser = "Opera";

    return browser + " on " + navigator.platform;
  }

  function getIPhoneModel(identifier) {
    const models = {
      "iPhone13,2": "iPhone 12",
      "iPhone13,3": "iPhone 12 Pro",
      "iPhone14,2": "iPhone 13 Pro",
      "iPhone14,3": "iPhone 13 Pro Max",
      "iPhone14,5": "iPhone 13",
      "iPhone15,2": "iPhone 14 Pro",
      "iPhone15,3": "iPhone 14 Pro Max",
    };
    return models[identifier] || identifier;
  }

  function getIPadModel(identifier) {
    const models = {
      "iPad13,1": "iPad Air (4th gen)",
      "iPad13,2": "iPad Air (4th gen)",
      "iPad14,1": "iPad mini (6th gen)",
    };
    return models[identifier] || identifier;
  }

  // ===== Helpers =====
  function getPin() {
    return $digits
      .map(function () {
        return $(this).val();
      })
      .get()
      .join("");
  }

  function updateSubmitButton() {
    const pin = getPin();
    $submitBtn.prop("disabled", pin.length !== 4);
  }

  function focusIndex(i) {
    currentIndex = Math.max(0, Math.min(3, i));
    $digits.eq(currentIndex).trigger("focus");
  }

  function showError(msg) {
    $errorMessage.text(msg).removeClass("d-none");
  }

  function hideError() {
    $errorMessage.addClass("d-none");
  }

  function showLoading() {
    $loading.removeClass("d-none");
    $submitBtn.addClass("d-none");
  }

  function hideLoading() {
    $loading.addClass("d-none");
    $submitBtn.removeClass("d-none");
  }

  function clearAll() {
    $digits.each(function () {
      $(this).val("").removeClass("filled");
    });
    focusIndex(0);
    updateSubmitButton();
    hideError();
  }

  function inputDigit(d) {
    if (currentIndex < 4) {
      const $cur = $digits.eq(currentIndex);
      $cur.val(String(d)).addClass("filled");
      if (currentIndex < 3) focusIndex(currentIndex + 1);
      updateSubmitButton();
      hideError();
    }
  }

  function backspace() {
    const $cur = $digits.eq(currentIndex);
    if ($cur.val()) {
      $cur.val("").removeClass("filled");
    } else if (currentIndex > 0) {
      focusIndex(currentIndex - 1);
      $digits.eq(currentIndex).val("").removeClass("filled");
    }
    updateSubmitButton();
    hideError();
  }

  // ===== Inputs events =====
  $digits.on("input", function () {
    const $t = $(this);
    const index = $digits.index($t);
    const value = ($t.val() || "").replace(/[^0-9]/g, "");
    $t.val(value);

    if (value) {
      $t.addClass("filled");
      if (index < 3) focusIndex(index + 1);
    } else {
      $t.removeClass("filled");
    }

    updateSubmitButton();
    hideError();
  });

  $digits.on("keydown", function (e) {
    const index = $digits.index(this);

    if (e.key === "Backspace" && !$(this).val() && index > 0) {
      focusIndex(index - 1);
    } else if (e.key === "ArrowLeft" && index > 0) {
      focusIndex(index - 1);
    } else if (e.key === "ArrowRight" && index < 3) {
      focusIndex(index + 1);
    } else if (e.key === "Enter") {
      e.preventDefault();
      $form.trigger("submit");
    }
  });

  $digits.on("focus", function () {
    currentIndex = $digits.index(this);
  });

  $digits.on("paste", function (e) {
    e.preventDefault();
    const data = (e.originalEvent.clipboardData || window.clipboardData)
      .getData("text")
      .replace(/[^0-9]/g, "")
      .slice(0, 4);

    for (let i = 0; i < data.length && i < 4; i++) {
      const $d = $digits.eq(i);
      $d.val(data[i]).addClass("filled");
    }
    if (data.length > 0) {
      const nextIndex = Math.min(data.length, 3);
      focusIndex(nextIndex);
    }
    updateSubmitButton();
    hideError();
  });

  // ===== Keypad (event delegation) =====
  $("#keypad").on("click", "button", function () {
    const $btn = $(this);
    const digit = $btn.data("digit");
    const action = $btn.data("action");

    if (digit !== undefined) {
      inputDigit(digit);
    } else if (action === "clear") {
      clearAll();
    } else if (action === "backspace") {
      backspace();
    }
  });

  // ===== Submit =====
  $form.on("submit", function (e) {
    e.preventDefault();

    // Check authentication first - kiểm tra lại để đảm bảo không bị block hoặc kicked
    $.ajax({
      url: "/api/referee/check-auth",
      method: "GET",
      success: function (authData) {
        if (authData.blocked) {
          showBlockedMessage(authData.message || "Thiết bị của bạn đã bị chặn");
          return;
        }

        if (authData.kicked) {
          showKickedMessage(authData.message || "Phiên đăng nhập đã bị đóng");
          return;
        }

        if (!authData.isLoggedIn) {
          showError("Vui lòng đăng nhập trước khi nhập PIN");
          showLoginModal();
          return;
        }

        if (!authData.verified) {
          showWaitingApprovalMessage(
            authData.message || "Đang chờ quản trị viên duyệt..."
          );
          return;
        }

        // Nếu authenticated và không bị block/kicked, tiếp tục submit PIN
        submitPin();
      },
      error: function () {
        showError("Không thể xác thực. Vui lòng thử lại.");
      },
    });
  });

  function submitPin() {
    const pin = getPin();

    if (pin.length !== 4) {
      showError("Vui lòng nhập đủ 4 chữ số");
      return;
    }

    showLoading();
    hideError();

    $.ajax({
      url: `/api/court/${pin}/status`,
      method: "GET",
    })
      .done(function () {
        window.location.href = `/scoreboard/${pin}`;
      })
      .fail(function (jqXHR) {
        if (jqXHR.status === 404) {
          showError("Mã PIN không tồn tại. Vui lòng kiểm tra lại.");
        } else {
          showError("Có lỗi xảy ra. Vui lòng thử lại.");
        }
        hideLoading();
      });
  }

  // ===== Access info (URL + QR) =====
  function initializeAccessInfo() {
    const currentUrl = window.location.origin + "/pin";
    const $urlBox = $("#currentUrl .url-text");
    $urlBox.text(currentUrl);
    loadQRCode(currentUrl);
  }

  // Global copy function (đang được gọi từ HTML onclick)
  window.copyUrl = function () {
    const currentUrl = window.location.origin + "/pin";
    navigator.clipboard
      .writeText(currentUrl)
      .then(function () {
        const $btn = $("#currentUrl button");
        $btn
          .removeClass("btn-primary")
          .addClass("btn-success")
          .html('<i class="bi bi-check2"></i> Copied!');
        setTimeout(function () {
          $btn
            .removeClass("btn-success")
            .addClass("btn-primary")
            .html('<i class="bi bi-clipboard"></i> Copy');
        }, 1800);
      })
      .catch(function (err) {
        console.error("Failed to copy URL:", err);
        alert("Không thể copy URL. Vui lòng copy thủ công.");
      });
  };

  function loadQRCode(url) {
    // Tải lib QR bằng jQuery
    $.getScript("https://cdn.jsdelivr.net/npm/qrcode@1.5.3/build/qrcode.min.js")
      .done(function () {
        try {
          const $qr = $("#qrCode").empty();
          const canvas = document.createElement("canvas");
          // global QRCode do lib cung cấp
          QRCode.toCanvas(
            canvas,
            url,
            {
              width: 120,
              height: 120,
              margin: 2,
              color: { dark: "#000000", light: "#FFFFFF" },
            },
            function (error) {
              if (error) {
                console.error("QR code generation failed:", error);
                $qr.html(
                  '<div class="small text-body-secondary text-center">' +
                    url +
                    "</div>"
                );
              } else {
                $qr.append(canvas);
              }
            }
          );
        } catch (e) {
          console.error("QR code generation error:", e);
        }
      })
      .fail(function () {
        console.log("QR code library not loaded, using fallback");
      });
  }

  // ===== Referee Authentication Functions =====
  function checkRefereeAuth() {
    $.ajax({
      url: "/api/referee/check-auth",
      method: "GET",
      success: function (data) {
        // Kiểm tra nếu bị block
        if (data.blocked) {
          isLoggedIn = false;
          $digits.prop("disabled", true);
          showBlockedMessage(data.message || "Thiết bị của bạn đã bị chặn");
          return;
        }

        // Kiểm tra nếu bị kicked (đá khỏi hệ thống)
        if (data.kicked) {
          isLoggedIn = false;
          $digits.prop("disabled", true);
          showKickedMessage(data.message || "Phiên đăng nhập đã bị đóng");
          return;
        }

        isLoggedIn = data.isLoggedIn || false;
        if (isLoggedIn) {
          // Kiểm tra trạng thái verified
          if (!data.verified) {
            $digits.prop("disabled", true);
            showWaitingApprovalMessage(data.message || "Đang chờ quản trị viên duyệt...");
            return;
          }
          
          // Enable PIN inputs
          $digits.prop("disabled", false);
          $("#refereeLoginModal").modal("hide");
        } else {
          // Disable PIN inputs and show login modal
          $digits.prop("disabled", true);
          showLoginModal();
        }
      },
      error: function () {
        isLoggedIn = false;
        $digits.prop("disabled", true);
        showLoginModal();
      },
    });
  }

  function showBlockedMessage(message) {
    // Hiển thị thông báo bị chặn
    showError(message);
    // Ẩn modal login nếu đang hiển thị
    $("#refereeLoginModal").modal("hide");
    // Disable tất cả inputs
    $digits.prop("disabled", true);
    $("button").prop("disabled", true);

    // Hiển thị modal cảnh báo
    const blockedHtml = `
      <div class="modal fade" id="blockedModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-danger">
            <div class="modal-header bg-danger text-white">
              <h5 class="modal-title"><i class="bi bi-exclamation-triangle-fill me-2"></i>Truy cập bị chặn</h5>
            </div>
            <div class="modal-body text-center py-4">
              <i class="bi bi-ban text-danger" style="font-size: 4rem;"></i>
              <p class="mt-3 mb-0 fs-5">${message}</p>
              <p class="text-muted mt-2">Vui lòng liên hệ quản trị viên để được hỗ trợ.</p>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" onclick="window.location.reload()">Tải lại trang</button>
            </div>
          </div>
        </div>
      </div>
    `;

    // Xóa modal cũ nếu có
    $("#blockedModal").remove();
    // Thêm modal mới
    $("body").append(blockedHtml);
    // Hiển thị modal
    const blockedModal = new bootstrap.Modal(
      document.getElementById("blockedModal")
    );
    blockedModal.show();
  }

  function showKickedMessage(message) {
    // Hiển thị thông báo bị đá khỏi hệ thống
    showError(message);
    // Ẩn modal login nếu đang hiển thị
    $("#refereeLoginModal").modal("hide");

    // Hiển thị modal thông báo
    const kickedHtml = `
      <div class="modal fade" id="kickedModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-warning">
            <div class="modal-header bg-warning text-dark">
              <h5 class="modal-title"><i class="bi bi-exclamation-circle-fill me-2"></i>Phiên đăng nhập đã kết thúc</h5>
            </div>
            <div class="modal-body text-center py-4">
              <i class="bi bi-door-open text-warning" style="font-size: 4rem;"></i>
              <p class="mt-3 mb-0 fs-5">${message}</p>
              <p class="text-muted mt-2">Quản trị viên đã đóng phiên đăng nhập của bạn.</p>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-primary" onclick="window.location.reload()">Đăng nhập lại</button>
            </div>
          </div>
        </div>
      </div>
    `;

    // Xóa modal cũ nếu có
    $("#kickedModal").remove();
    // Thêm modal mới
    $("body").append(kickedHtml);
    // Hiển thị modal
    const kickedModal = new bootstrap.Modal(
      document.getElementById("kickedModal")
    );
    kickedModal.show();
  }

  function showWaitingApprovalMessage(message) {
    // Hiển thị thông báo chờ duyệt
    showError(message);
    // Ẩn modal login nếu đang hiển thị
    $("#refereeLoginModal").modal("hide");

    // Disable PIN inputs
    $digits.prop("disabled", true);
    $submitBtn.prop("disabled", true);

    // Hiển thị modal thông báo
    const waitingHtml = `
      <div class="modal fade" id="waitingApprovalModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-info">
            <div class="modal-header bg-info text-white">
              <h5 class="modal-title"><i class="bi bi-hourglass-split me-2"></i>Chờ phê duyệt</h5>
            </div>
            <div class="modal-body text-center py-4">
              <div class="spinner-border text-info mb-3" role="status" style="width: 3rem; height: 3rem;">
                <span class="visually-hidden">Loading...</span>
              </div>
              <p class="mt-3 mb-0 fs-5">${message}</p>
              <p class="text-muted mt-2">Quản trị viên sẽ xác nhận thiết bị của bạn trong giây lát.</p>
              <p class="text-muted"><small>Trang sẽ tự động cập nhật sau khi được duyệt.</small></p>
            </div>
            <div class="modal-footer justify-content-center">
              <button type="button" class="btn btn-outline-secondary" onclick="window.location.reload()">Làm mới</button>
            </div>
          </div>
        </div>
      </div>
    `;

    // Xóa modal cũ nếu có
    $("#waitingApprovalModal").remove();
    // Thêm modal mới
    $("body").append(waitingHtml);
    // Hiển thị modal
    const waitingModal = new bootstrap.Modal(
      document.getElementById("waitingApprovalModal")
    );
    waitingModal.show();

    // Poll every 3 seconds để check nếu đã được duyệt
    const checkInterval = setInterval(function () {
      $.ajax({
        url: "/api/referee/check-auth",
        method: "GET",
        success: function (authData) {
          if (authData.verified) {
            // Đã được duyệt, reload trang
            clearInterval(checkInterval);
            window.location.reload();
          }
        },
      });
    }, 3000);
  }

  function showLoginModal() {
    if (!refereeLoginModal) {
      refereeLoginModal = new bootstrap.Modal(
        document.getElementById("refereeLoginModal"),
        {
          backdrop: "static",
          keyboard: false,
        }
      );
    }
    refereeLoginModal.show();
  }

  function handleRefereeLogin(e) {
    e.preventDefault();

    const maTrongTai = $("#maTrongTai").val().trim();
    const matKhau = $("#matKhau").val();
    const $errorMsg = $("#loginErrorMsg");
    const $submitBtn = $('#refereeLoginForm button[type="submit"]');

    // Validation
    if (!maTrongTai || !matKhau) {
      $errorMsg.text("Vui lòng nhập đầy đủ thông tin đăng nhập").show();
      return;
    }

    // Disable submit button
    $submitBtn
      .prop("disabled", true)
      .html(
        '<span class="spinner-border spinner-border-sm me-2"></span>Đang đăng nhập...'
      );
    $errorMsg.hide();

    // Get device info
    const deviceInfo = getDeviceInfo();
    console.log("Sending device info:", deviceInfo);

    $.ajax({
      url: "/api/referee/login",
      method: "POST",
      contentType: "application/json",
      data: JSON.stringify({
        maTrongTai: maTrongTai,
        matKhau: matKhau,
        deviceId: deviceInfo.deviceId,
        deviceModel: deviceInfo.deviceModel,
      }),
      success: function (data) {
        console.log("Login response:", data);
        if (data.success) {
          isLoggedIn = true;
          // Enable PIN inputs
          $digits.prop("disabled", false);

          // Hide modal
          $("#refereeLoginModal").modal("hide");

          // Show success toast
          const toastEl = document.getElementById("loginSuccessToast");
          const toast = new bootstrap.Toast(toastEl);
          toast.show();

          // Reset form
          $("#refereeLoginForm")[0].reset();
          $errorMsg.hide();
        } else {
          $errorMsg.text(data.message || "Đăng nhập thất bại").show();
        }
      },
      error: function (xhr) {
        let errorMessage = "Đăng nhập thất bại";
        if (xhr.responseJSON && xhr.responseJSON.message) {
          errorMessage = xhr.responseJSON.message;
        }
        $errorMsg.text(errorMessage).show();
      },
      complete: function () {
        $submitBtn.prop("disabled", false).text("Đăng nhập");
      },
    });
  }

  // ===== Init =====
  // Check authentication on page load
  checkRefereeAuth();

  // Periodic check for auth status (every 5 seconds)
  setInterval(function() {
    checkRefereeAuth();
  }, 5000);

  // Bind login form submit
  $("#refereeLoginForm").on("submit", handleRefereeLogin);

  focusIndex(0);
  updateSubmitButton();
  initializeAccessInfo();
});
