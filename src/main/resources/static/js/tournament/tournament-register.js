/**
 * Tournament Registration Page JavaScript
 * Multi-step form validation and navigation
 */

document.addEventListener('DOMContentLoaded', function() {
    console.log('🎯 Tournament Registration JS loaded');
    
    // Prevent back/forward cache - force page reload
    window.addEventListener('pageshow', function(event) {
        if (event.persisted) {
            console.log('⚠️ Page loaded from bfcache, forcing reload');
            window.location.reload();
        }
    });
    
    // Check if form exists
    const registrationForm = document.getElementById('registrationForm');
    const registrationFormCard = document.querySelector('.registration-form-card');
    console.log('Form element:', registrationForm);
    console.log('Form card:', registrationFormCard);
    console.log('Form card content preview:', registrationFormCard ? registrationFormCard.innerHTML.substring(0, 200) : 'NULL');
    
    // Initialize AOS
    if (typeof AOS !== 'undefined') {
        AOS.init({
            duration: 800,
            once: true
        });
    }

    // Form validation
    function validateForm() {
        const form = document.getElementById('registrationForm');
        if (!form) return false;
        
        const requiredFields = form.querySelectorAll('[required]');
        let isValid = true;
        let firstInvalidField = null;

        requiredFields.forEach(field => {
            // Check if field is visible and enabled
            if (field.offsetParent === null) return; // Skip hidden fields
            
            let fieldValue = field.type === 'checkbox' ? field.checked : field.value.trim();
            
            if (!fieldValue) {
                isValid = false;
                field.classList.add('is-invalid');
                if (!firstInvalidField) firstInvalidField = field;
                
                // Show error message
                let errorDiv = field.parentElement.querySelector('.invalid-feedback');
                if (!errorDiv) {
                    errorDiv = document.createElement('div');
                    errorDiv.className = 'invalid-feedback';
                    errorDiv.style.display = 'block';
                    errorDiv.textContent = 'Vui lòng điền thông tin này';
                    field.parentElement.appendChild(errorDiv);
                }
            } else {
                field.classList.remove('is-invalid');
                const errorDiv = field.parentElement.querySelector('.invalid-feedback');
                if (errorDiv) errorDiv.remove();
            }
        });

        if (!isValid && firstInvalidField) {
            firstInvalidField.focus();
            alert('Vui lòng điền đầy đủ thông tin bắt buộc!');
        }

        return isValid;
    }
    
    // Clear validation errors on input
    function setupFieldValidation() {
        const allFields = document.querySelectorAll('.form-control, .form-select, .form-check-input');
        allFields.forEach(field => {
            const clearError = function() {
                field.classList.remove('is-invalid');
                const errorDiv = field.parentElement.querySelector('.invalid-feedback');
                if (errorDiv) errorDiv.remove();
            };
            
            // Remove old listeners before adding new ones
            field.removeEventListener('input', clearError);
            field.removeEventListener('change', clearError);
            field.addEventListener('input', clearError);
            field.addEventListener('change', clearError);
        });
    }

    async function submitForm(event) {
        event.preventDefault(); // Prevent default form submission
        
        if (!validateForm()) {
            return;
        }
        
        // Show loading
        const submitBtn = document.querySelector('.btn-register');
        if (!submitBtn) {
            console.error('Submit button not found');
            return;
        }
        
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Đang xử lý...';
        submitBtn.disabled = true;

        try {
            // Get tournament ID from URL
            const pathParts = window.location.pathname.split('/');
            const tournamentId = pathParts[pathParts.indexOf('tournaments') + 1];
            
            // Collect form data
            const formData = {
                hoTen: document.getElementById('fullName')?.value || '',
                email: document.getElementById('email')?.value || '',
                dienThoai: document.getElementById('phone')?.value || '',
                ngaySinh: document.getElementById('birthDate')?.value || '',
                gioiTinh: document.getElementById('gender')?.value || '',
                cccd: document.getElementById('idCard')?.value || '',
                
                noiDung: document.getElementById('category')?.value || '',
                trinhDo: document.getElementById('skillLevel')?.value || '',
                cauLacBo: document.getElementById('club')?.value || '',
                tenDongDoi: document.getElementById('partnerName')?.value || '',
                emailDongDoi: document.getElementById('partnerEmail')?.value || '',
                
                phuongThucThanhToan: document.getElementById('paymentMethod')?.value || 'cash',
                dongYDieuKhoan: document.getElementById('agreeTerms')?.checked || false,
                xacNhanSucKhoe: document.getElementById('healthConfirm')?.checked || false,
                ghiChu: document.getElementById('notes')?.value || ''
            };
            
            console.log('📤 Submitting registration:', formData);
            
            // Call API
            const response = await fetch(`/api/tournaments/${tournamentId}/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });
            
            const result = await response.json();
            console.log('📥 Registration response:', result);
            
            if (result.success) {
                // Show success message
                showSuccessMessage(result);
            } else {
                // Show error message
                showErrorMessage(result.message || 'Đăng ký thất bại. Vui lòng thử lại.');
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
            }
            
        } catch (error) {
            console.error('❌ Registration error:', error);
            showErrorMessage('Đã xảy ra lỗi. Vui lòng kiểm tra kết nối và thử lại.');
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        }
    }
    
    function showSuccessMessage(result) {
        const formCard = document.querySelector('.registration-form-card');
        if (!formCard) {
            alert('Đăng ký thành công! Mã: ' + result.registrationCode);
            return;
        }
        
        formCard.innerHTML = `
            <div class="success-message text-center py-5">
                <i class="bi bi-check-circle-fill text-success" style="font-size: 72px;"></i>
                <h2 class="mt-3 mb-3">Đăng ký thành công!</h2>
                <p class="lead">
                    ${result.message}<br>
                    ${result.registrationCode ? `<strong>Mã đăng ký: ${result.registrationCode}</strong><br>` : ''}
                    ${result.confirmationEmail ? `Email xác nhận đã được gửi đến: ${result.confirmationEmail}` : ''}
                </p>
                ${result.paymentInstructions ? `
                    <div class="alert alert-info mt-3 text-start">
                        <h6><i class="bi bi-info-circle"></i> Hướng dẫn thanh toán:</h6>
                        <pre style="white-space: pre-wrap;">${result.paymentInstructions}</pre>
                    </div>
                ` : ''}
                <a href="/tournaments" class="btn btn-primary btn-lg mt-4">
                    <i class="bi bi-arrow-left"></i> Quay lại danh sách giải đấu
                </a>
            </div>
        `;
        
        // Scroll to top
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    
    function showErrorMessage(message) {
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-danger alert-dismissible fade show mt-3';
        alertDiv.innerHTML = `
            <i class="bi bi-exclamation-triangle"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        const formCard = document.querySelector('.registration-form-card');
        const form = document.getElementById('registrationForm');
        
        if (formCard && form) {
            // Remove any existing alerts
            const existingAlerts = formCard.querySelectorAll('.alert-danger');
            existingAlerts.forEach(alert => alert.remove());
            
            // Insert new alert before the form
            formCard.insertBefore(alertDiv, form);
        } else {
            alert(message);
        }
        
        // Auto dismiss after 5 seconds
        setTimeout(() => {
            alertDiv.remove();
        }, 5000);
        
        // Scroll to top to show error
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // Category change handler
    const categorySelect = document.getElementById('category');
    if (categorySelect) {
        categorySelect.addEventListener('change', function() {
            const partnerSection = document.getElementById('partnerSection');
            const selectedCategory = this.value;
            
            // Show partner section for doubles categories (men-doubles, women-doubles, mixed-doubles)
            if (selectedCategory && selectedCategory.includes('doubles')) {
                partnerSection.style.display = 'block';
            } else {
                partnerSection.style.display = 'none';
            }
        });
    }

    // Add partner button
    window.addPartner = function() {
        document.getElementById('partnerFields').style.display = 'block';
        document.getElementById('addPartnerBtn').style.display = 'none';
    };

    window.removePartner = function() {
        document.getElementById('partnerFields').style.display = 'none';
        document.getElementById('addPartnerBtn').style.display = 'block';
        
        // Clear partner fields
        document.querySelectorAll('#partnerFields input').forEach(input => {
            input.value = '';
        });
    };

    // Form field animations
    const formControls = document.querySelectorAll('.form-control, .form-select');
    formControls.forEach(control => {
        control.addEventListener('focus', function() {
            this.parentElement.querySelector('.form-label')?.classList.add('text-primary');
        });

        control.addEventListener('blur', function() {
            this.parentElement.querySelector('.form-label')?.classList.remove('text-primary');
        });
    });

    // Setup field validation handlers
    setupFieldValidation();
    
    // Attach form submission handler ONLY if form exists
    if (registrationForm) {
        console.log('✅ Form found, attaching submit handler');
        registrationForm.addEventListener('submit', submitForm);
    } else {
        console.warn('❌ Form NOT found! Check if HTML rendered correctly');
    }
    
    // Also attach to submit button directly as backup
    const submitBtn = document.querySelector('.btn-register');
    if (submitBtn) {
        console.log('✅ Submit button found');
        submitBtn.addEventListener('click', function(e) {
            e.preventDefault();
            submitForm(e);
        });
    } else {
        console.warn('❌ Submit button NOT found');
    }
    
    // Debug: Check form card content
    if (registrationFormCard) {
        console.log('Form card innerHTML length:', registrationFormCard.innerHTML.length);
        console.log('Form card has form?', registrationFormCard.querySelector('#registrationForm') !== null);
    }
});
