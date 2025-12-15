/**
 * Tournament Search Autocomplete
 * 
 * FEATURES:
 * - Real-time search suggestions as user types
 * - Debounced AJAX requests (300ms) để tránh spam API
 * - Keyboard navigation (arrow keys, Enter, Escape)
 * - Click outside to close dropdown
 * - Cache results để giảm API calls
 * - Highlight matched text trong suggestions
 * 
 * USAGE:
 * 1. HTML: <input id="tournamentSearch" class="form-control" placeholder="Tìm kiếm...">
 * 2. Include this script: <script src="/js/tournament/tournament-search-autocomplete.js"></script>
 * 3. Call: initTournamentSearchAutocomplete('#tournamentSearch');
 * 
 * @author BTMS Team
 * @version 1.0
 */

(function() {
    'use strict';
    
    // ========== CONFIG ==========
    const CONFIG = {
        debounceDelay: 300,      // ms - Wait time before sending API request
        minSearchLength: 2,      // Minimum characters before search
        maxResults: 8,           // Maximum suggestions to show
        cacheExpiry: 5 * 60 * 1000  // 5 minutes cache
    };
    
    // ========== CACHE ==========
    const cache = new Map();
    
    /**
     * Get cached results if still valid
     */
    function getCachedResults(query) {
        const cached = cache.get(query);
        if (!cached) return null;
        
        const age = Date.now() - cached.timestamp;
        if (age > CONFIG.cacheExpiry) {
            cache.delete(query);
            return null;
        }
        
        return cached.results;
    }
    
    /**
     * Cache search results
     */
    function setCachedResults(query, results) {
        cache.set(query, {
            results: results,
            timestamp: Date.now()
        });
    }
    
    // ========== AUTOCOMPLETE CLASS ==========
    class TournamentSearchAutocomplete {
        constructor(inputSelector) {
            this.input = document.querySelector(inputSelector);
            if (!this.input) {
                console.error('❌ Tournament search input not found:', inputSelector);
                return;
            }
            
            this.dropdown = null;
            this.debounceTimer = null;
            this.currentFocus = -1;
            this.isLoading = false;
            
            this.init();
        }
        
        /**
         * Initialize autocomplete
         */
        init() {
            console.log('🔍 Initializing tournament search autocomplete');
            
            // Create dropdown container
            this.createDropdown();
            
            // Bind events
            this.input.addEventListener('input', this.handleInput.bind(this));
            this.input.addEventListener('keydown', this.handleKeydown.bind(this));
            this.input.addEventListener('focus', this.handleFocus.bind(this));
            
            // Click outside to close
            document.addEventListener('click', (e) => {
                if (!this.input.contains(e.target) && !this.dropdown.contains(e.target)) {
                    this.hideDropdown();
                }
            });
            
            console.log('✅ Autocomplete initialized');
        }
        
        /**
         * Create dropdown element
         */
        createDropdown() {
            this.dropdown = document.createElement('div');
            this.dropdown.className = 'tournament-search-dropdown';
            this.dropdown.style.cssText = `
                position: absolute;
                top: 100%;
                left: 0;
                right: 0;
                background: white;
                border: 1px solid #ddd;
                border-radius: 4px;
                box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                max-height: 400px;
                overflow-y: auto;
                z-index: 1000;
                display: none;
            `;
            
            // Position relative to input
            const parent = this.input.parentElement;
            if (parent.style.position !== 'relative' && parent.style.position !== 'absolute') {
                parent.style.position = 'relative';
            }
            
            parent.appendChild(this.dropdown);
        }
        
        /**
         * Handle input changes with debounce
         */
        handleInput(e) {
            const query = e.target.value.trim();
            
            // Clear previous timer
            if (this.debounceTimer) {
                clearTimeout(this.debounceTimer);
            }
            
            // Reset if query too short
            if (query.length < CONFIG.minSearchLength) {
                this.hideDropdown();
                return;
            }
            
            // Debounce API call
            this.debounceTimer = setTimeout(() => {
                this.search(query);
            }, CONFIG.debounceDelay);
        }
        
        /**
         * Search tournaments via API
         */
        async search(query) {
            console.log('🔍 Searching for:', query);
            
            // Check cache first
            const cached = getCachedResults(query);
            if (cached) {
                console.log('💾 Using cached results');
                this.showResults(cached, query);
                return;
            }
            
            // Show loading state
            this.showLoading();
            this.isLoading = true;
            
            try {
                const response = await fetch(`/api/tournaments/search/autocomplete?q=${encodeURIComponent(query)}&limit=${CONFIG.maxResults}`);
                
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                
                const results = await response.json();
                console.log('✅ Search results:', results);
                
                // Cache results
                setCachedResults(query, results);
                
                // Show results
                this.showResults(results, query);
                
            } catch (error) {
                console.error('❌ Search error:', error);
                this.showError('Lỗi tìm kiếm. Vui lòng thử lại.');
            } finally {
                this.isLoading = false;
            }
        }
        
        /**
         * Show loading state
         */
        showLoading() {
            this.dropdown.innerHTML = `
                <div class="p-3 text-center text-muted">
                    <span class="spinner-border spinner-border-sm me-2"></span>
                    Đang tìm kiếm...
                </div>
            `;
            this.dropdown.style.display = 'block';
        }
        
        /**
         * Show search results
         */
        showResults(results, query) {
            this.currentFocus = -1;
            
            if (!results || results.length === 0) {
                this.dropdown.innerHTML = `
                    <div class="p-3 text-center text-muted">
                        <i class="bi bi-search"></i>
                        Không tìm thấy giải đấu
                    </div>
                `;
                this.dropdown.style.display = 'block';
                return;
            }
            
            // Build result items
            const html = results.map((tournament, index) => {
                const highlighted = this.highlightMatch(tournament.tenGiai, query);
                const statusBadge = this.getStatusBadge(tournament.trangThai);
                const dateStr = tournament.ngayBatDau 
                    ? new Date(tournament.ngayBatDau).toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' })
                    : '';
                
                return `
                    <div class="search-result-item" data-index="${index}" data-id="${tournament.idGiai}">
                        <div class="d-flex align-items-center">
                            <div class="flex-shrink-0 me-3">
                                <i class="bi bi-trophy text-warning"></i>
                            </div>
                            <div class="flex-grow-1">
                                <div class="fw-semibold">${highlighted}</div>
                                <small class="text-muted">
                                    <i class="bi bi-geo-alt"></i> ${tournament.tinhThanh || 'Chưa rõ'}
                                    ${dateStr ? `<span class="ms-2"><i class="bi bi-calendar"></i> ${dateStr}</span>` : ''}
                                </small>
                            </div>
                            <div class="flex-shrink-0">
                                ${statusBadge}
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
            
            this.dropdown.innerHTML = html;
            this.dropdown.style.display = 'block';
            
            // Add click handlers
            this.dropdown.querySelectorAll('.search-result-item').forEach(item => {
                item.addEventListener('click', () => {
                    const id = item.dataset.id;
                    window.location.href = `/tournament/${id}`;
                });
                
                // Hover effect
                item.addEventListener('mouseenter', () => {
                    this.removeFocusClass();
                    item.classList.add('active');
                });
            });
        }
        
        /**
         * Highlight matched text
         */
        highlightMatch(text, query) {
            if (!text) return '';
            
            const regex = new RegExp(`(${query})`, 'gi');
            return text.replace(regex, '<mark>$1</mark>');
        }
        
        /**
         * Get status badge HTML
         */
        getStatusBadge(status) {
            const badges = {
                'ongoing': '<span class="badge bg-danger">Đang diễn ra</span>',
                'registration': '<span class="badge bg-success">Đăng ký</span>',
                'upcoming': '<span class="badge bg-primary">Sắp tới</span>',
                'completed': '<span class="badge bg-secondary">Kết thúc</span>'
            };
            
            return badges[status] || '';
        }
        
        /**
         * Show error message
         */
        showError(message) {
            this.dropdown.innerHTML = `
                <div class="p-3 text-center text-danger">
                    <i class="bi bi-exclamation-triangle"></i>
                    ${message}
                </div>
            `;
            this.dropdown.style.display = 'block';
        }
        
        /**
         * Hide dropdown
         */
        hideDropdown() {
            this.dropdown.style.display = 'none';
            this.currentFocus = -1;
        }
        
        /**
         * Handle keyboard navigation
         */
        handleKeydown(e) {
            const items = this.dropdown.querySelectorAll('.search-result-item');
            
            if (items.length === 0) return;
            
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                this.currentFocus++;
                if (this.currentFocus >= items.length) this.currentFocus = 0;
                this.addFocusClass(items);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                this.currentFocus--;
                if (this.currentFocus < 0) this.currentFocus = items.length - 1;
                this.addFocusClass(items);
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (this.currentFocus > -1 && items[this.currentFocus]) {
                    items[this.currentFocus].click();
                }
            } else if (e.key === 'Escape') {
                this.hideDropdown();
                this.input.blur();
            }
        }
        
        /**
         * Add focus class to current item
         */
        addFocusClass(items) {
            this.removeFocusClass();
            if (this.currentFocus >= 0 && this.currentFocus < items.length) {
                items[this.currentFocus].classList.add('active');
                items[this.currentFocus].scrollIntoView({ block: 'nearest' });
            }
        }
        
        /**
         * Remove focus from all items
         */
        removeFocusClass() {
            this.dropdown.querySelectorAll('.search-result-item.active').forEach(item => {
                item.classList.remove('active');
            });
        }
        
        /**
         * Handle input focus - show recent searches if available
         */
        handleFocus() {
            const query = this.input.value.trim();
            if (query.length >= CONFIG.minSearchLength) {
                const cached = getCachedResults(query);
                if (cached) {
                    this.showResults(cached, query);
                }
            }
        }
    }
    
    // ========== CSS STYLES (Auto-inject) ==========
    const style = document.createElement('style');
    style.textContent = `
        .tournament-search-dropdown .search-result-item {
            padding: 12px 16px;
            cursor: pointer;
            transition: background-color 0.2s;
            border-bottom: 1px solid #f0f0f0;
        }
        
        .tournament-search-dropdown .search-result-item:last-child {
            border-bottom: none;
        }
        
        .tournament-search-dropdown .search-result-item:hover,
        .tournament-search-dropdown .search-result-item.active {
            background-color: #f8f9fa;
        }
        
        .tournament-search-dropdown mark {
            background-color: #fff3cd;
            padding: 2px 4px;
            border-radius: 2px;
            font-weight: 600;
        }
    `;
    document.head.appendChild(style);
    
    // ========== EXPORT TO GLOBAL ==========
    window.initTournamentSearchAutocomplete = function(inputSelector) {
        return new TournamentSearchAutocomplete(inputSelector);
    };
    
})();
