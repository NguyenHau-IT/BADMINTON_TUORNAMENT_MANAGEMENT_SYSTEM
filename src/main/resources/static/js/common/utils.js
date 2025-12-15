/**
 * BTMS - Utility Functions
 * Reusable helper functions
 */

const BTMSUtils = {
    /**
     * Debounce function calls
     */
    debounce: function(func, wait = 300) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    /**
     * Throttle function calls
     */
    throttle: function(func, limit = 300) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },
    
    /**
     * Deep clone object
     */
    deepClone: function(obj) {
        return JSON.parse(JSON.stringify(obj));
    },
    
    /**
     * Check if element is in viewport
     */
    isInViewport: function(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    },
    
    /**
     * Get query parameter
     */
    getQueryParam: function(param) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(param);
    },
    
    /**
     * Set query parameter
     */
    setQueryParam: function(param, value) {
        const url = new URL(window.location);
        url.searchParams.set(param, value);
        window.history.pushState({}, '', url);
    },
    
    /**
     * Remove query parameter
     */
    removeQueryParam: function(param) {
        const url = new URL(window.location);
        url.searchParams.delete(param);
        window.history.pushState({}, '', url);
    },
    
    /**
     * Copy to clipboard
     */
    copyToClipboard: async function(text) {
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch (err) {
            console.error('Failed to copy:', err);
            return false;
        }
    },
    
    /**
     * Generate random ID
     */
    generateId: function(prefix = 'id') {
        return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    },
    
    /**
     * Format currency (VND)
     */
    formatCurrency: function(amount) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    },
    
    /**
     * Format relative time
     */
    formatRelativeTime: function(date) {
        const now = new Date();
        const diff = now - new Date(date);
        const seconds = Math.floor(diff / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);
        
        if (days > 7) {
            return BTMS.formatDate(date);
        } else if (days > 0) {
            return `${days} ngày trước`;
        } else if (hours > 0) {
            return `${hours} giờ trước`;
        } else if (minutes > 0) {
            return `${minutes} phút trước`;
        } else {
            return 'Vừa xong';
        }
    },
    
    /**
     * Validate email
     */
    isValidEmail: function(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    },
    
    /**
     * Validate phone number (Vietnam)
     */
    isValidPhone: function(phone) {
        const re = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
        return re.test(phone);
    },
    
    /**
     * Local storage helpers
     */
    storage: {
        set: function(key, value) {
            try {
                localStorage.setItem(key, JSON.stringify(value));
                return true;
            } catch (e) {
                console.error('localStorage set error:', e);
                return false;
            }
        },
        
        get: function(key, defaultValue = null) {
            try {
                const item = localStorage.getItem(key);
                return item ? JSON.parse(item) : defaultValue;
            } catch (e) {
                console.error('localStorage get error:', e);
                return defaultValue;
            }
        },
        
        remove: function(key) {
            try {
                localStorage.removeItem(key);
                return true;
            } catch (e) {
                console.error('localStorage remove error:', e);
                return false;
            }
        },
        
        clear: function() {
            try {
                localStorage.clear();
                return true;
            } catch (e) {
                console.error('localStorage clear error:', e);
                return false;
            }
        }
    },

    // ========== LOGGING UTILITY ==========
    /**
     * Production-safe logger that can be toggled on/off
     * Set BTMS_DEBUG = true in console or localStorage to enable debug logs
     * 
     * Usage:
     *   BTMSUtils.log.debug('Message', data);
     *   BTMSUtils.log.info('Message');
     *   BTMSUtils.log.warn('Warning');
     *   BTMSUtils.log.error('Error', error);
     */
    log: {
        // Check if debug mode is enabled
        isDebug: function() {
            if (typeof window !== 'undefined') {
                return window.BTMS_DEBUG === true || 
                       localStorage.getItem('BTMS_DEBUG') === 'true' ||
                       window.location.hostname === 'localhost' ||
                       window.location.hostname === '127.0.0.1';
            }
            return false;
        },

        debug: function(...args) {
            if (this.isDebug()) {
                console.log('[BTMS]', ...args);
            }
        },

        info: function(...args) {
            if (this.isDebug()) {
                console.info('[BTMS ℹ️]', ...args);
            }
        },

        warn: function(...args) {
            // Warnings always show
            console.warn('[BTMS ⚠️]', ...args);
        },

        error: function(...args) {
            // Errors always show
            console.error('[BTMS ❌]', ...args);
        },

        // Enable debug mode
        enableDebug: function() {
            window.BTMS_DEBUG = true;
            localStorage.setItem('BTMS_DEBUG', 'true');
            console.log('[BTMS] Debug mode ENABLED');
        },

        // Disable debug mode
        disableDebug: function() {
            window.BTMS_DEBUG = false;
            localStorage.removeItem('BTMS_DEBUG');
            console.log('[BTMS] Debug mode DISABLED');
        }
    }
};

// Expose to global scope
window.BTMSUtils = BTMSUtils;

// Create shorthand for logging
window.btmsLog = BTMSUtils.log;
