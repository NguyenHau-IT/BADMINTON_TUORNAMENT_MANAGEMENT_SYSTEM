/**
 * Rankings Module JavaScript
 * BTMS - Badminton Tournament Management System
 */

(function() {
    'use strict';

    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', function() {
        initRankingsModule();
    });

    function initRankingsModule() {
        initAOS();
        initTableHover();
        initRankAnimations();
        initStickyHeader();
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
     * Add hover effects to ranking tables
     */
    function initTableHover() {
        const tableRows = document.querySelectorAll('.rankings-table tbody tr');
        
        tableRows.forEach(row => {
            row.addEventListener('mouseenter', function() {
                this.style.transform = 'scale(1.01)';
                this.style.boxShadow = '0 4px 15px rgba(102, 126, 234, 0.15)';
            });
            
            row.addEventListener('mouseleave', function() {
                this.style.transform = 'scale(1)';
                this.style.boxShadow = 'none';
            });
        });
    }

    /**
     * Animate rank badges on scroll
     */
    function initRankAnimations() {
        const rankBadges = document.querySelectorAll('.rank-badge.top-1, .rank-badge.top-2, .rank-badge.top-3');
        
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('animate-pop');
                }
            });
        }, { threshold: 0.5 });

        rankBadges.forEach(badge => observer.observe(badge));
    }

    /**
     * Make category tabs sticky on scroll
     */
    function initStickyHeader() {
        const filterBar = document.querySelector('.filter-bar');
        if (!filterBar) return;

        const filterBarTop = filterBar.offsetTop;
        const header = document.querySelector('.main-header');
        const headerHeight = header ? header.offsetHeight : 70;

        window.addEventListener('scroll', function() {
            if (window.pageYOffset > filterBarTop - headerHeight) {
                filterBar.classList.add('sticky');
                filterBar.style.top = headerHeight + 'px';
            } else {
                filterBar.classList.remove('sticky');
                filterBar.style.top = '';
            }
        });
    }

    /**
     * Format large numbers with comma separators
     */
    function formatNumber(num) {
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }

    /**
     * Animate counter from 0 to target value
     */
    function animateCounter(element, target, duration = 2000) {
        let start = 0;
        const step = target / (duration / 16);
        
        const timer = setInterval(() => {
            start += step;
            if (start >= target) {
                element.textContent = formatNumber(target);
                clearInterval(timer);
            } else {
                element.textContent = formatNumber(Math.floor(start));
            }
        }, 16);
    }

    // Animate stat numbers when visible
    const statNumbers = document.querySelectorAll('.stat-number[data-target]');
    if (statNumbers.length > 0) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const target = parseInt(entry.target.dataset.target);
                    animateCounter(entry.target, target);
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.5 });

        statNumbers.forEach(num => observer.observe(num));
    }

    // Add CSS for animations
    const style = document.createElement('style');
    style.textContent = `
        .animate-pop {
            animation: popIn 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
        }
        
        @keyframes popIn {
            0% { transform: scale(0.5); opacity: 0; }
            100% { transform: scale(1); opacity: 1; }
        }
        
        .filter-bar.sticky {
            position: fixed;
            left: 0;
            right: 0;
            z-index: 100;
            background: white;
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            padding: 15px 20px;
            border-radius: 0;
            animation: slideDown 0.3s ease;
        }
        
        @keyframes slideDown {
            from { transform: translateY(-100%); }
            to { transform: translateY(0); }
        }
        
        .rankings-table tbody tr {
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }
    `;
    document.head.appendChild(style);

})();
