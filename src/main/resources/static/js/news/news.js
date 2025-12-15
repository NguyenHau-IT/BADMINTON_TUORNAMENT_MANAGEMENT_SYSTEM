/**
 * News Module JavaScript
 * BTMS - Badminton Tournament Management System
 */

(function() {
    'use strict';

    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', function() {
        initNewsModule();
    });

    function initNewsModule() {
        initAOS();
        initImageLazyLoad();
        initNewsletterForm();
        initShareButtons();
        initReadingProgress();
        initStickyWidgets();
    }

    /**
     * Initialize AOS (Animate On Scroll) library
     */
    function initAOS() {
        if (typeof AOS !== 'undefined') {
            AOS.init({
                duration: 600,
                easing: 'ease-out',
                once: true,
                offset: 50
            });
        }
    }

    /**
     * Lazy load images for better performance
     */
    function initImageLazyLoad() {
        const images = document.querySelectorAll('img[data-src]');
        
        const imageObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src;
                    img.removeAttribute('data-src');
                    imageObserver.unobserve(img);
                }
            });
        });

        images.forEach(img => imageObserver.observe(img));
    }

    /**
     * Handle newsletter form submission
     */
    function initNewsletterForm() {
        const newsletterForms = document.querySelectorAll('.newsletter-form');
        
        newsletterForms.forEach(form => {
            form.addEventListener('submit', function(e) {
                e.preventDefault();
                
                const emailInput = form.querySelector('input[type="email"]');
                const email = emailInput.value.trim();
                
                if (!isValidEmail(email)) {
                    showNotification('Vui lòng nhập email hợp lệ', 'error');
                    return;
                }
                
                // Simulate subscription
                const submitBtn = form.querySelector('button[type="submit"]');
                const originalText = submitBtn.innerHTML;
                submitBtn.innerHTML = '<i class="bi bi-arrow-repeat spin"></i> Đang xử lý...';
                submitBtn.disabled = true;
                
                setTimeout(() => {
                    showNotification('Đăng ký thành công! Cảm ơn bạn đã quan tâm.', 'success');
                    emailInput.value = '';
                    submitBtn.innerHTML = originalText;
                    submitBtn.disabled = false;
                }, 1500);
            });
        });
    }

    /**
     * Validate email format
     */
    function isValidEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    /**
     * Show notification toast
     */
    function showNotification(message, type = 'info') {
        // Remove existing notifications
        const existing = document.querySelector('.news-notification');
        if (existing) existing.remove();
        
        const notification = document.createElement('div');
        notification.className = `news-notification ${type}`;
        notification.innerHTML = `
            <i class="bi ${type === 'success' ? 'bi-check-circle' : type === 'error' ? 'bi-exclamation-circle' : 'bi-info-circle'}"></i>
            <span>${message}</span>
        `;
        
        document.body.appendChild(notification);
        
        // Trigger animation
        setTimeout(() => notification.classList.add('show'), 10);
        
        // Auto remove
        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    }

    /**
     * Initialize share buttons functionality
     */
    function initShareButtons() {
        // Share on Facebook
        window.shareOnFacebook = function() {
            const url = encodeURIComponent(window.location.href);
            window.open(`https://www.facebook.com/sharer/sharer.php?u=${url}`, '_blank', 'width=600,height=400');
        };

        // Share on Twitter
        window.shareOnTwitter = function() {
            const url = encodeURIComponent(window.location.href);
            const title = encodeURIComponent(document.title);
            window.open(`https://twitter.com/intent/tweet?url=${url}&text=${title}`, '_blank', 'width=600,height=400');
        };

        // Share on LinkedIn
        window.shareOnLinkedIn = function() {
            const url = encodeURIComponent(window.location.href);
            window.open(`https://www.linkedin.com/shareArticle?mini=true&url=${url}`, '_blank', 'width=600,height=400');
        };

        // Copy link to clipboard
        window.copyLink = function() {
            navigator.clipboard.writeText(window.location.href).then(() => {
                showNotification('Đã sao chép liên kết!', 'success');
            }).catch(() => {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = window.location.href;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                showNotification('Đã sao chép liên kết!', 'success');
            });
        };
    }

    /**
     * Show reading progress bar for articles
     */
    function initReadingProgress() {
        const articleBody = document.querySelector('.article-body');
        if (!articleBody) return;
        
        // Create progress bar
        const progressBar = document.createElement('div');
        progressBar.className = 'reading-progress';
        progressBar.innerHTML = '<div class="reading-progress-bar"></div>';
        document.body.appendChild(progressBar);
        
        const progressBarInner = progressBar.querySelector('.reading-progress-bar');
        
        window.addEventListener('scroll', function() {
            const articleTop = articleBody.offsetTop;
            const articleHeight = articleBody.offsetHeight;
            const windowHeight = window.innerHeight;
            const scrollTop = window.pageYOffset;
            
            const progress = Math.min(100, Math.max(0, 
                ((scrollTop - articleTop + windowHeight) / articleHeight) * 100
            ));
            
            progressBarInner.style.width = progress + '%';
        });
    }

    /**
     * Make sidebar widgets sticky
     */
    function initStickyWidgets() {
        const sidebar = document.querySelector('.sticky-sidebar');
        if (!sidebar) return;
        
        const header = document.querySelector('.main-header');
        const headerHeight = header ? header.offsetHeight : 70;
        
        sidebar.style.top = (headerHeight + 20) + 'px';
    }

    // Add CSS for notifications and animations
    const style = document.createElement('style');
    style.textContent = `
        .news-notification {
            position: fixed;
            bottom: 20px;
            right: 20px;
            padding: 15px 25px;
            background: white;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.15);
            display: flex;
            align-items: center;
            gap: 12px;
            z-index: 9999;
            transform: translateX(120%);
            transition: transform 0.3s ease;
        }
        
        .news-notification.show {
            transform: translateX(0);
        }
        
        .news-notification.success {
            border-left: 4px solid #10b981;
        }
        
        .news-notification.success i {
            color: #10b981;
        }
        
        .news-notification.error {
            border-left: 4px solid #ef4444;
        }
        
        .news-notification.error i {
            color: #ef4444;
        }
        
        .news-notification.info {
            border-left: 4px solid #667eea;
        }
        
        .news-notification.info i {
            color: #667eea;
        }
        
        .news-notification i {
            font-size: 1.3rem;
        }
        
        .news-notification span {
            font-weight: 500;
            color: #2d3748;
        }
        
        .spin {
            animation: spin 1s linear infinite;
        }
        
        @keyframes spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
        }
        
        .reading-progress {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            height: 4px;
            background: rgba(102, 126, 234, 0.1);
            z-index: 9999;
        }
        
        .reading-progress-bar {
            height: 100%;
            background: linear-gradient(90deg, #667eea, #764ba2);
            width: 0;
            transition: width 0.1s ease;
        }
    `;
    document.head.appendChild(style);

})();
