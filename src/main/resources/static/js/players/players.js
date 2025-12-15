/**
 * Players Module JavaScript
 * BTMS - Badminton Tournament Management System
 */

(function() {
    'use strict';

    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', function() {
        initPlayersModule();
    });

    function initPlayersModule() {
        initAOS();
        initFilterForm();
        initPlayerCards();
        initGalleryLightbox();
        initStatsAnimation();
        initProfileTabs();
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
     * Handle filter form functionality
     */
    function initFilterForm() {
        const filterForm = document.querySelector('.filter-form');
        if (!filterForm) return;

        // Real-time search with debounce
        const searchInput = filterForm.querySelector('input[name="search"]');
        if (searchInput) {
            let debounceTimer;
            searchInput.addEventListener('input', function() {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    // Auto-submit after typing stops
                    // filterForm.submit();
                }, 500);
            });
        }

        // Auto-submit on select change
        const selects = filterForm.querySelectorAll('select');
        selects.forEach(select => {
            select.addEventListener('change', function() {
                // Uncomment to auto-submit
                // filterForm.submit();
            });
        });

        // Clear filters button
        const clearBtn = document.querySelector('.btn-clear-filters');
        if (clearBtn) {
            clearBtn.addEventListener('click', function() {
                filterForm.reset();
                window.location.href = '/players';
            });
        }
    }

    /**
     * Add hover effects and animations to player cards
     */
    function initPlayerCards() {
        const playerCards = document.querySelectorAll('.player-card');
        
        playerCards.forEach(card => {
            // Parallax effect on hover
            card.addEventListener('mousemove', function(e) {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;
                
                const rotateX = (y - centerY) / 20;
                const rotateY = (centerX - x) / 20;
                
                card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-8px)`;
            });
            
            card.addEventListener('mouseleave', function() {
                card.style.transform = '';
            });
        });

        // Top player cards special effect
        const topPlayerCards = document.querySelectorAll('.top-player-card');
        topPlayerCards.forEach((card, index) => {
            if (index < 3) {
                card.classList.add('spotlight');
            }
        });
    }

    /**
     * Initialize gallery lightbox for player detail page
     */
    function initGalleryLightbox() {
        const galleryItems = document.querySelectorAll('.gallery-item');
        if (galleryItems.length === 0) return;

        // Create lightbox container
        const lightbox = document.createElement('div');
        lightbox.className = 'gallery-lightbox';
        lightbox.innerHTML = `
            <button class="lightbox-close"><i class="bi bi-x-lg"></i></button>
            <button class="lightbox-prev"><i class="bi bi-chevron-left"></i></button>
            <button class="lightbox-next"><i class="bi bi-chevron-right"></i></button>
            <div class="lightbox-content">
                <img src="" alt="">
            </div>
        `;
        document.body.appendChild(lightbox);

        let currentIndex = 0;
        const images = Array.from(galleryItems).map(item => item.querySelector('img').src);

        // Open lightbox
        galleryItems.forEach((item, index) => {
            item.addEventListener('click', function() {
                currentIndex = index;
                showImage(currentIndex);
                lightbox.classList.add('active');
                document.body.style.overflow = 'hidden';
            });
        });

        // Close lightbox
        lightbox.querySelector('.lightbox-close').addEventListener('click', closeLightbox);
        lightbox.addEventListener('click', function(e) {
            if (e.target === lightbox) closeLightbox();
        });

        // Navigate
        lightbox.querySelector('.lightbox-prev').addEventListener('click', function() {
            currentIndex = (currentIndex - 1 + images.length) % images.length;
            showImage(currentIndex);
        });

        lightbox.querySelector('.lightbox-next').addEventListener('click', function() {
            currentIndex = (currentIndex + 1) % images.length;
            showImage(currentIndex);
        });

        // Keyboard navigation
        document.addEventListener('keydown', function(e) {
            if (!lightbox.classList.contains('active')) return;
            
            if (e.key === 'Escape') closeLightbox();
            if (e.key === 'ArrowLeft') {
                currentIndex = (currentIndex - 1 + images.length) % images.length;
                showImage(currentIndex);
            }
            if (e.key === 'ArrowRight') {
                currentIndex = (currentIndex + 1) % images.length;
                showImage(currentIndex);
            }
        });

        function showImage(index) {
            lightbox.querySelector('.lightbox-content img').src = images[index];
        }

        function closeLightbox() {
            lightbox.classList.remove('active');
            document.body.style.overflow = '';
        }
    }

    /**
     * Animate statistics counters
     */
    function initStatsAnimation() {
        const statCards = document.querySelectorAll('.stat-card .stat-value');
        
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const target = entry.target;
                    const value = target.textContent;
                    
                    // Check if it's a number or percentage
                    if (value.includes('%')) {
                        const num = parseFloat(value);
                        animateValue(target, 0, num, 1500, '%');
                    } else {
                        const num = parseInt(value.replace(/,/g, ''));
                        if (!isNaN(num)) {
                            animateValue(target, 0, num, 1500);
                        }
                    }
                    
                    observer.unobserve(target);
                }
            });
        }, { threshold: 0.5 });

        statCards.forEach(card => observer.observe(card));
    }

    /**
     * Animate numeric value from start to end
     */
    function animateValue(element, start, end, duration, suffix = '') {
        const startTime = performance.now();
        
        function update(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            // Easing function (ease-out)
            const easeOut = 1 - Math.pow(1 - progress, 3);
            const current = start + (end - start) * easeOut;
            
            if (suffix === '%') {
                element.textContent = current.toFixed(1) + suffix;
            } else {
                element.textContent = Math.floor(current).toLocaleString();
            }
            
            if (progress < 1) {
                requestAnimationFrame(update);
            }
        }
        
        requestAnimationFrame(update);
    }

    /**
     * Initialize profile tabs (if any)
     */
    function initProfileTabs() {
        const tabs = document.querySelectorAll('.profile-tab');
        const tabContents = document.querySelectorAll('.tab-content');
        
        if (tabs.length === 0) return;

        tabs.forEach(tab => {
            tab.addEventListener('click', function() {
                const targetId = this.dataset.tab;
                
                // Update active tab
                tabs.forEach(t => t.classList.remove('active'));
                this.classList.add('active');
                
                // Show corresponding content
                tabContents.forEach(content => {
                    if (content.id === targetId) {
                        content.classList.add('active');
                    } else {
                        content.classList.remove('active');
                    }
                });
            });
        });
    }

    // Add CSS for lightbox and animations
    const style = document.createElement('style');
    style.textContent = `
        .gallery-lightbox {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0,0,0,0.95);
            z-index: 9999;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            visibility: hidden;
            transition: all 0.3s ease;
        }
        
        .gallery-lightbox.active {
            opacity: 1;
            visibility: visible;
        }
        
        .lightbox-content {
            max-width: 90%;
            max-height: 90%;
        }
        
        .lightbox-content img {
            max-width: 100%;
            max-height: 90vh;
            object-fit: contain;
            border-radius: 8px;
        }
        
        .lightbox-close,
        .lightbox-prev,
        .lightbox-next {
            position: absolute;
            background: rgba(255,255,255,0.1);
            border: none;
            color: white;
            width: 50px;
            height: 50px;
            border-radius: 50%;
            cursor: pointer;
            font-size: 1.5rem;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
        }
        
        .lightbox-close:hover,
        .lightbox-prev:hover,
        .lightbox-next:hover {
            background: rgba(255,255,255,0.2);
        }
        
        .lightbox-close {
            top: 20px;
            right: 20px;
        }
        
        .lightbox-prev {
            left: 20px;
            top: 50%;
            transform: translateY(-50%);
        }
        
        .lightbox-next {
            right: 20px;
            top: 50%;
            transform: translateY(-50%);
        }
        
        .player-card {
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }
        
        .top-player-card.spotlight::before {
            content: '';
            position: absolute;
            top: -2px;
            left: -2px;
            right: -2px;
            bottom: -2px;
            background: linear-gradient(45deg, #FFD700, #FFA500, #FF6B35, #667eea, #764ba2);
            background-size: 400%;
            border-radius: 22px;
            z-index: -1;
            animation: gradientBorder 3s ease infinite;
        }
        
        @keyframes gradientBorder {
            0%, 100% { background-position: 0% 50%; }
            50% { background-position: 100% 50%; }
        }
        
        .profile-tab {
            padding: 12px 24px;
            background: transparent;
            border: none;
            color: #718096;
            font-weight: 600;
            cursor: pointer;
            border-bottom: 3px solid transparent;
            transition: all 0.3s ease;
        }
        
        .profile-tab:hover {
            color: #667eea;
        }
        
        .profile-tab.active {
            color: #667eea;
            border-bottom-color: #667eea;
        }
        
        .tab-content {
            display: none;
        }
        
        .tab-content.active {
            display: block;
            animation: fadeIn 0.3s ease;
        }
        
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
    `;
    document.head.appendChild(style);

})();
