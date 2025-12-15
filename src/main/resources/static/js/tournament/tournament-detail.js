/**
 * Tournament Detail Page JavaScript
 * Handles countdown, social share, parallax, tabs, and animations
 */

// ========== GLOBAL FUNCTIONS (Must be global for onclick attributes) ==========

/**
 * Share on Facebook
 */
window.shareFacebook = function() {
    const url = encodeURIComponent(window.location.href);
    const title = document.querySelector('.hero-title')?.textContent || 'Giải đấu cầu lông';
    window.open(
        `https://www.facebook.com/sharer/sharer.php?u=${url}`,
        'facebook-share',
        'width=600,height=400'
    );
};

/**
 * Share on Twitter
 */
window.shareTwitter = function() {
    const url = encodeURIComponent(window.location.href);
    const title = document.querySelector('.hero-title')?.textContent || 'Giải đấu cầu lông';
    const text = encodeURIComponent(`${title} - Đăng ký ngay!`);
    window.open(
        `https://twitter.com/intent/tweet?url=${url}&text=${text}`,
        'twitter-share',
        'width=600,height=400'
    );
};

/**
 * Share on Zalo
 */
window.shareZalo = function() {
    const url = encodeURIComponent(window.location.href);
    window.open(
        `https://sp.zalo.me/share_inline?url=${url}`,
        'zalo-share',
        'width=600,height=400'
    );
};

/**
 * Copy link to clipboard
 */
window.copyLink = function() {
    const url = window.location.href;
    
    // Copy to clipboard
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(url).then(() => {
            showCopySuccess();
        }).catch(err => {
            console.error('Failed to copy:', err);
            // Fallback method
            fallbackCopyTextToClipboard(url);
        });
    } else {
        // Fallback for older browsers
        fallbackCopyTextToClipboard(url);
    }
};

/**
 * Fallback copy method for older browsers
 */
function fallbackCopyTextToClipboard(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
        const successful = document.execCommand('copy');
        if (successful) {
            showCopySuccess();
        }
    } catch (err) {
        console.error('Fallback copy failed:', err);
    }
    
    document.body.removeChild(textArea);
}

/**
 * Show copy success message
 */
function showCopySuccess() {
    const successDiv = document.createElement('div');
    successDiv.className = 'copy-success';
    successDiv.innerHTML = '<i class="bi bi-check-circle"></i> Đã sao chép liên kết!';
    
    const shareCard = document.querySelector('.social-share-card');
    if (shareCard) {
        shareCard.appendChild(successDiv);
        
        // Remove after 3 seconds
        setTimeout(() => {
            successDiv.remove();
        }, 3000);
    }
}

// ========== IMAGE GALLERY LIGHTBOX ==========

let currentImageIndex = 0;
let galleryImages = [];

/**
 * Open lightbox with specific image
 */
window.openLightbox = function(index) {
    // Collect all gallery images
    const galleryItems = document.querySelectorAll('.gallery-item');
    galleryImages = Array.from(galleryItems).map(item => {
        const img = item.querySelector('.gallery-image');
        const title = item.querySelector('.gallery-overlay span');
        return {
            src: img.src,
            alt: img.alt,
            title: title ? title.textContent : ''
        };
    });
    
    if (galleryImages.length === 0) return;
    
    currentImageIndex = index;
    updateLightboxImage();
    
    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('lightboxModal'));
    modal.show();
};

/**
 * Navigate to previous image
 */
window.prevImage = function() {
    currentImageIndex = (currentImageIndex - 1 + galleryImages.length) % galleryImages.length;
    updateLightboxImage();
};

/**
 * Navigate to next image
 */
window.nextImage = function() {
    currentImageIndex = (currentImageIndex + 1) % galleryImages.length;
    updateLightboxImage();
};

/**
 * Update lightbox image display
 */
function updateLightboxImage() {
    const image = galleryImages[currentImageIndex];
    const lightboxImg = document.getElementById('lightboxImage');
    const lightboxTitle = document.getElementById('lightboxTitle');
    const lightboxCounter = document.getElementById('lightboxCounter');
    
    if (lightboxImg && image) {
        lightboxImg.src = image.src;
        lightboxImg.alt = image.alt;
    }
    
    if (lightboxTitle && image) {
        lightboxTitle.textContent = image.title || 'Hình ảnh giải đấu';
    }
    
    if (lightboxCounter) {
        lightboxCounter.textContent = `${currentImageIndex + 1} / ${galleryImages.length}`;
    }
}

// Keyboard navigation for lightbox
document.addEventListener('keydown', function(e) {
    const modal = document.getElementById('lightboxModal');
    if (modal && modal.classList.contains('show')) {
        if (e.key === 'ArrowLeft') {
            prevImage();
        } else if (e.key === 'ArrowRight') {
            nextImage();
        } else if (e.key === 'Escape') {
            bootstrap.Modal.getInstance(modal)?.hide();
        }
    }
});

// ========== DOM READY INITIALIZATION ==========

document.addEventListener('DOMContentLoaded', function() {
    // Initialize countdown timer
    initCountdownTimer();
    
    // Initialize parallax effect
    initParallaxEffect();
    // Initialize AOS
    AOS.init({
        duration: 800,
        once: true
    });

    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Interactive tabs with URL hash and analytics
    initInteractiveTabs();
    
    // Initialize related tournaments carousel
    initRelatedTournamentsCarousel();

    // Registration button click tracking
    const registerBtn = document.querySelector('a[href*="/register"]');
    if (registerBtn) {
        registerBtn.addEventListener('click', function(e) {
            const tournamentId = this.href.split('/').slice(-2)[0];
            console.log('Registration started for tournament:', tournamentId);
            // In production: send to analytics
        });
    }

    // Info card animations on scroll
    const infoCards = document.querySelectorAll('.info-card');
    if ('IntersectionObserver' in window) {
        const cardObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.style.opacity = '0';
                    entry.target.style.transform = 'translateY(20px)';
                    setTimeout(() => {
                        entry.target.style.transition = 'all 0.5s ease';
                        entry.target.style.opacity = '1';
                        entry.target.style.transform = 'translateY(0)';
                    }, 100);
                    cardObserver.unobserve(entry.target);
                }
            });
        });

        infoCards.forEach(card => cardObserver.observe(card));
    }

});

// ========== COUNTDOWN TIMER ==========

/**
 * Initialize countdown timer
 */
function initCountdownTimer() {
    const countdownElement = document.getElementById('countdownTimer');
    if (!countdownElement) return;

    const deadline = countdownElement.dataset.deadline;
    if (!deadline) return;

    // Parse deadline (format: yyyy-MM-ddTHH:mm:ss)
    const deadlineDate = new Date(deadline);
    
    // Update countdown every second
    const countdownInterval = setInterval(() => {
        const now = new Date().getTime();
        const distance = deadlineDate - now;

        // Check if expired
        if (distance < 0) {
            clearInterval(countdownInterval);
            countdownElement.innerHTML = '<div class="countdown-label text-center"><i class="bi bi-exclamation-circle"></i> Đã hết hạn đăng ký</div>';
            return;
        }

        // Calculate time units
        const days = Math.floor(distance / (1000 * 60 * 60 * 24));
        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((distance % (1000 * 60)) / 1000);

        // Update DOM
        updateCountdownValue('days', days);
        updateCountdownValue('hours', hours);
        updateCountdownValue('minutes', minutes);
        updateCountdownValue('seconds', seconds);
    }, 1000);
}

/**
 * Update countdown value with animation
 */
function updateCountdownValue(elementId, value) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const formattedValue = value.toString().padStart(2, '0');
    
    // Only update if value changed (prevents unnecessary DOM updates)
    if (element.textContent !== formattedValue) {
        element.textContent = formattedValue;
        
        // Add flash animation
        element.style.animation = 'none';
        setTimeout(() => {
            element.style.animation = 'flash 0.3s ease';
        }, 10);
    }
}

// ========== PARALLAX EFFECT ==========

/**
 * Initialize parallax scrolling effect
 */
function initParallaxEffect() {
    const parallaxBg = document.querySelector('.parallax-bg');
    if (!parallaxBg) return;

    let ticking = false;

    window.addEventListener('scroll', () => {
        if (!ticking) {
            window.requestAnimationFrame(() => {
                const scrolled = window.pageYOffset;
                const heroHeight = document.querySelector('.hero-section')?.offsetHeight || 600;
                
                // Only apply parallax if within hero section
                if (scrolled <= heroHeight) {
                    const parallaxSpeed = 0.5;
                    parallaxBg.style.transform = `translate3d(0, ${scrolled * parallaxSpeed}px, 0)`;
                }
                
                ticking = false;
            });
            
            ticking = true;
        }
    });
}

// ========== INTERACTIVE TABS ==========

/**
 * Initialize interactive tabs with URL hash sync and smooth transitions
 */
function initInteractiveTabs() {
    const tabs = document.querySelectorAll('.nav-tabs .nav-link');
    const tabContents = document.querySelectorAll('.tab-pane');
    
    if (tabs.length === 0) return;

    // Activate tab from URL hash on page load
    const hash = window.location.hash;
    if (hash) {
        const targetTab = document.querySelector(`.nav-tabs .nav-link[href="${hash}"]`);
        if (targetTab) {
            const bsTab = new bootstrap.Tab(targetTab);
            bsTab.show();
        }
    }

    // Tab change event listeners
    tabs.forEach(tab => {
        tab.addEventListener('shown.bs.tab', function(e) {
            const tabName = e.target.textContent.trim();
            const tabId = e.target.getAttribute('href');
            
            // Update URL hash without scrolling
            history.replaceState(null, null, tabId);
            
            // Analytics tracking
            console.log('Tab changed to:', tabName, tabId);
            
            // Add entrance animation to content
            const activePane = document.querySelector(tabId);
            if (activePane) {
                activePane.style.animation = 'none';
                setTimeout(() => {
                    activePane.style.animation = 'fadeInUp 0.5s ease';
                }, 10);
            }
        });

        // Smooth scroll to tabs when clicked from deep link
        tab.addEventListener('click', function(e) {
            const tabsSection = document.querySelector('.content-section');
            if (tabsSection && window.scrollY > tabsSection.offsetTop) {
                setTimeout(() => {
                    tabsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }, 100);
            }
        });
    });

    // Handle browser back/forward with hash
    window.addEventListener('hashchange', function() {
        const hash = window.location.hash;
        if (hash) {
            const targetTab = document.querySelector(`.nav-tabs .nav-link[href="${hash}"]`);
            if (targetTab && !targetTab.classList.contains('active')) {
                const bsTab = new bootstrap.Tab(targetTab);
                bsTab.show();
            }
        }
    });
}

// ========== RELATED TOURNAMENTS CAROUSEL ==========

/**
 * Initialize Swiper carousel for related tournaments
 */
function initRelatedTournamentsCarousel() {
    const swiperElement = document.querySelector('.relatedTournamentsSwiper');
    if (!swiperElement) return;

    const swiper = new Swiper('.relatedTournamentsSwiper', {
        slidesPerView: 1,
        spaceBetween: 20,
        loop: false,
        autoplay: {
            delay: 5000,
            disableOnInteraction: false,
            pauseOnMouseEnter: true
        },
        pagination: {
            el: '.swiper-pagination',
            clickable: true,
            dynamicBullets: true
        },
        navigation: {
            nextEl: '.swiper-button-next-custom',
            prevEl: '.swiper-button-prev-custom'
        },
        breakpoints: {
            640: {
                slidesPerView: 2,
                spaceBetween: 20
            },
            992: {
                slidesPerView: 3,
                spaceBetween: 30
            }
        }
    });

    // Analytics tracking for carousel interactions
    swiper.on('slideChange', function () {
        console.log('Related tournament slide changed to:', swiper.activeIndex);
    });
}

// ========== FLASH ANIMATION (for countdown updates) ==========

// Add CSS animation dynamically
const style = document.createElement('style');
style.textContent = `
    @keyframes flash {
        0%, 100% {
            opacity: 1;
        }
        50% {
            opacity: 0.6;
            transform: scale(1.1);
        }
    }
`;
document.head.appendChild(style);

// ========== ENHANCED TAB NAVIGATION & GALLERY ==========

/**
 * Enhanced Tab Navigation with smooth transitions and URL hash support
 */
class TournamentDetailEnhanced {
    constructor() {
        this.currentTab = 'overview';
        this.currentImageIndex = 0;
        this.galleryImages = [];
        
        this.initGalleryLightbox();
        this.initScrollAnimations();
    }
    
    /**
     * Gallery Lightbox Component
     */
    initGalleryLightbox() {
        // Collect all gallery images
        const galleryItems = document.querySelectorAll('.gallery-item');
        this.galleryImages = Array.from(galleryItems).map(item => {
            const img = item.querySelector('.gallery-image');
            return {
                url: img?.src,
                alt: img?.alt || '',
                title: item.querySelector('.gallery-overlay span')?.textContent || ''
            };
        });
        
        if (this.galleryImages.length === 0) return;
        
        // Create lightbox HTML
        this.createLightboxDOM();
        
        // Attach click handlers
        galleryItems.forEach((item, index) => {
            item.addEventListener('click', () => this.openLightbox(index));
        });
        
        // Keyboard navigation
        document.addEventListener('keydown', (e) => {
            if (!this.isLightboxOpen()) return;
            
            switch(e.key) {
                case 'Escape':
                    this.closeLightbox();
                    break;
                case 'ArrowLeft':
                    this.previousImage();
                    break;
                case 'ArrowRight':
                    this.nextImage();
                    break;
            }
        });
    }
    
    /**
     * Create Lightbox DOM structure
     */
    createLightboxDOM() {
        const lightboxHTML = `
            <div id="galleryLightbox" class="gallery-lightbox" style="display: none;">
                <div class="lightbox-overlay" onclick="tournamentDetailEnhanced.closeLightbox()"></div>
                <div class="lightbox-content">
                    <button class="lightbox-close" onclick="tournamentDetailEnhanced.closeLightbox()" aria-label="Đóng">
                        <i class="bi bi-x-lg"></i>
                    </button>
                    
                    <button class="lightbox-nav lightbox-prev" onclick="tournamentDetailEnhanced.previousImage()" aria-label="Ảnh trước">
                        <i class="bi bi-chevron-left"></i>
                    </button>
                    
                    <div class="lightbox-image-container">
                        <img id="lightboxImage" src="" alt="" class="lightbox-image">
                        <div class="lightbox-loader">
                            <div class="spinner-border text-light" role="status">
                                <span class="visually-hidden">Loading...</span>
                            </div>
                        </div>
                    </div>
                    
                    <button class="lightbox-nav lightbox-next" onclick="tournamentDetailEnhanced.nextImage()" aria-label="Ảnh sau">
                        <i class="bi bi-chevron-right"></i>
                    </button>
                    
                    <div class="lightbox-info">
                        <h5 id="lightboxTitle"></h5>
                        <p id="lightboxCounter"></p>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', lightboxHTML);
    }
    
    /**
     * Open lightbox at specific image index
     */
    openLightbox(index) {
        if (this.galleryImages.length === 0) return;
        
        this.currentImageIndex = index;
        const lightbox = document.getElementById('galleryLightbox');
        const img = document.getElementById('lightboxImage');
        const loader = lightbox.querySelector('.lightbox-loader');
        
        // Show lightbox
        lightbox.style.display = 'flex';
        document.body.style.overflow = 'hidden';
        
        // Show loader
        loader.style.display = 'flex';
        img.style.opacity = '0';
        
        // Load image
        const imageData = this.galleryImages[index];
        img.onload = () => {
            loader.style.display = 'none';
            img.style.opacity = '1';
        };
        
        img.src = imageData.url;
        img.alt = imageData.alt;
        
        // Update info
        document.getElementById('lightboxTitle').textContent = imageData.title;
        document.getElementById('lightboxCounter').textContent = 
            `${index + 1} / ${this.galleryImages.length}`;
        
        // Update navigation visibility
        this.updateLightboxNav();
        
        // Trigger animation
        setTimeout(() => {
            lightbox.classList.add('active');
        }, 10);
    }
    
    /**
     * Close lightbox
     */
    closeLightbox() {
        const lightbox = document.getElementById('galleryLightbox');
        lightbox.classList.remove('active');
        
        setTimeout(() => {
            lightbox.style.display = 'none';
            document.body.style.overflow = '';
        }, 300);
    }
    
    /**
     * Navigate to previous image
     */
    previousImage() {
        if (this.currentImageIndex > 0) {
            this.openLightbox(this.currentImageIndex - 1);
        }
    }
    
    /**
     * Navigate to next image
     */
    nextImage() {
        if (this.currentImageIndex < this.galleryImages.length - 1) {
            this.openLightbox(this.currentImageIndex + 1);
        }
    }
    
    /**
     * Update navigation button visibility
     */
    updateLightboxNav() {
        const prevBtn = document.querySelector('.lightbox-prev');
        const nextBtn = document.querySelector('.lightbox-next');
        
        if (prevBtn && nextBtn) {
            prevBtn.style.display = this.currentImageIndex === 0 ? 'none' : 'flex';
            nextBtn.style.display = 
                this.currentImageIndex === this.galleryImages.length - 1 ? 'none' : 'flex';
        }
    }
    
    /**
     * Check if lightbox is open
     */
    isLightboxOpen() {
        const lightbox = document.getElementById('galleryLightbox');
        return lightbox && lightbox.style.display !== 'none';
    }
    
    /**
     * Initialize scroll animations
     */
    initScrollAnimations() {
        // Parallax effect for hero section
        const heroBackground = document.querySelector('.parallax-bg');
        if (!heroBackground) return;
        
        window.addEventListener('scroll', () => {
            const scrolled = window.pageYOffset;
            const parallaxSpeed = 0.5;
            heroBackground.style.transform = `translateY(${scrolled * parallaxSpeed}px)`;
        });
    }
}

// Initialize enhanced features when DOM is ready
let tournamentDetailEnhanced;
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        tournamentDetailEnhanced = new TournamentDetailEnhanced();
    });
} else {
    tournamentDetailEnhanced = new TournamentDetailEnhanced();
}

// Expose openLightbox globally for onclick handlers
window.openLightbox = function(index) {
    if (tournamentDetailEnhanced) {
        tournamentDetailEnhanced.openLightbox(index);
    }
};
