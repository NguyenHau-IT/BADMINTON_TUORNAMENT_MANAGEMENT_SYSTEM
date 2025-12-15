/**
 * Tournament Live Filters (No Page Reload)
 * 
 * FEATURES:
 * - Client-side filtering without full page reload
 * - Multiple filter criteria: status, province, category, date range
 * - Smooth animations when filtering
 * - URL parameters sync (bookmarkable)
 * - Filter count badge
 * - Reset filters button
 * 
 * USAGE:
 * 1. Call: initTournamentLiveFilters();
 * 2. Works with tournament cards having data attributes:
 *    - data-status="ongoing"
 *    - data-province="TP. Hồ Chí Minh"
 *    - data-category="singles"
 *    - data-start-date="2024-01-15"
 * 
 * @author BTMS Team
 * @version 1.0
 */

(function() {
    'use strict';
    
    // ========== FILTER STATE ==========
    const filterState = {
        status: [],
        province: '',
        category: '',
        startDate: '',
        endDate: '',
        keyword: ''
    };
    
    // ========== DOM ELEMENTS ==========
    let elements = {};
    
    /**
     * Initialize live filters
     */
    function initTournamentLiveFilters() {
        console.log('🎛️ Initializing tournament live filters');
        
        // Get DOM elements
        elements = {
            statusFilter: document.getElementById('statusFilter'),
            provinceFilter: document.getElementById('provinceFilter'),
            categoryFilter: document.getElementById('categoryFilter'),
            startDateFilter: document.getElementById('startDateFilter'),
            endDateFilter: document.getElementById('endDateFilter'),
            keywordFilter: document.getElementById('keywordFilter'),
            resetBtn: document.getElementById('resetFiltersBtn'),
            filterCountBadge: document.getElementById('filterCountBadge'),
            tournamentCards: document.querySelectorAll('.tournament-card'),
            resultsContainer: document.getElementById('tournamentsContainer'),
            noResultsMessage: document.getElementById('noResultsMessage')
        };
        
        // Check required elements
        if (!elements.tournamentCards || elements.tournamentCards.length === 0) {
            console.warn('⚠️ No tournament cards found');
            return;
        }
        
        // Load filters from URL
        loadFiltersFromURL();
        
        // Bind events
        bindFilterEvents();
        
        // Initial filter application
        applyFilters();
        
        console.log('✅ Live filters initialized');
    }
    
    /**
     * Bind filter change events
     */
    function bindFilterEvents() {
        // Status (multi-select)
        if (elements.statusFilter) {
            elements.statusFilter.addEventListener('change', (e) => {
                filterState.status = Array.from(e.target.selectedOptions).map(opt => opt.value);
                applyFilters();
                updateURL();
            });
        }
        
        // Province
        if (elements.provinceFilter) {
            elements.provinceFilter.addEventListener('change', (e) => {
                filterState.province = e.target.value;
                applyFilters();
                updateURL();
            });
        }
        
        // Category
        if (elements.categoryFilter) {
            elements.categoryFilter.addEventListener('change', (e) => {
                filterState.category = e.target.value;
                applyFilters();
                updateURL();
            });
        }
        
        // Date range
        if (elements.startDateFilter) {
            elements.startDateFilter.addEventListener('change', (e) => {
                filterState.startDate = e.target.value;
                applyFilters();
                updateURL();
            });
        }
        
        if (elements.endDateFilter) {
            elements.endDateFilter.addEventListener('change', (e) => {
                filterState.endDate = e.target.value;
                applyFilters();
                updateURL();
            });
        }
        
        // Keyword search
        if (elements.keywordFilter) {
            let debounceTimer;
            elements.keywordFilter.addEventListener('input', (e) => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    filterState.keyword = e.target.value.toLowerCase().trim();
                    applyFilters();
                    updateURL();
                }, 300);
            });
        }
        
        // Reset button
        if (elements.resetBtn) {
            elements.resetBtn.addEventListener('click', resetFilters);
        }
    }
    
    /**
     * Apply all filters to tournament cards
     */
    function applyFilters() {
        let visibleCount = 0;
        let activeFilterCount = 0;
        
        // Count active filters
        if (filterState.status.length > 0) activeFilterCount++;
        if (filterState.province) activeFilterCount++;
        if (filterState.category) activeFilterCount++;
        if (filterState.startDate || filterState.endDate) activeFilterCount++;
        if (filterState.keyword) activeFilterCount++;
        
        // Update filter count badge
        if (elements.filterCountBadge) {
            if (activeFilterCount > 0) {
                elements.filterCountBadge.textContent = activeFilterCount;
                elements.filterCountBadge.style.display = 'inline-block';
            } else {
                elements.filterCountBadge.style.display = 'none';
            }
        }
        
        // Filter each card
        elements.tournamentCards.forEach(card => {
            const show = shouldShowCard(card);
            
            if (show) {
                visibleCount++;
                fadeIn(card);
            } else {
                fadeOut(card);
            }
        });
        
        // Show/hide no results message
        if (elements.noResultsMessage) {
            if (visibleCount === 0) {
                elements.noResultsMessage.style.display = 'block';
                fadeIn(elements.noResultsMessage);
            } else {
                elements.noResultsMessage.style.display = 'none';
            }
        }
        
        console.log(`🔍 Filters applied: ${visibleCount}/${elements.tournamentCards.length} visible`);
    }
    
    /**
     * Check if card should be shown based on filters
     */
    function shouldShowCard(card) {
        // Status filter
        if (filterState.status.length > 0) {
            const cardStatus = card.dataset.status;
            if (!filterState.status.includes(cardStatus)) {
                return false;
            }
        }
        
        // Province filter
        if (filterState.province) {
            const cardProvince = card.dataset.province || '';
            if (cardProvince !== filterState.province) {
                return false;
            }
        }
        
        // Category filter
        if (filterState.category) {
            const cardCategory = card.dataset.category || '';
            if (cardCategory !== filterState.category) {
                return false;
            }
        }
        
        // Date range filter
        if (filterState.startDate || filterState.endDate) {
            const cardDate = card.dataset.startDate;
            if (!cardDate) return false;
            
            const cardDateObj = new Date(cardDate);
            
            if (filterState.startDate) {
                const filterStartDate = new Date(filterState.startDate);
                if (cardDateObj < filterStartDate) return false;
            }
            
            if (filterState.endDate) {
                const filterEndDate = new Date(filterState.endDate);
                if (cardDateObj > filterEndDate) return false;
            }
        }
        
        // Keyword filter
        if (filterState.keyword) {
            const cardText = (
                card.dataset.name + ' ' + 
                card.dataset.province + ' ' +
                card.dataset.description
            ).toLowerCase();
            
            if (!cardText.includes(filterState.keyword)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Reset all filters
     */
    function resetFilters() {
        console.log('🔄 Resetting filters');
        
        // Reset state
        filterState.status = [];
        filterState.province = '';
        filterState.category = '';
        filterState.startDate = '';
        filterState.endDate = '';
        filterState.keyword = '';
        
        // Reset form elements
        if (elements.statusFilter) {
            Array.from(elements.statusFilter.options).forEach(opt => opt.selected = false);
        }
        if (elements.provinceFilter) elements.provinceFilter.value = '';
        if (elements.categoryFilter) elements.categoryFilter.value = '';
        if (elements.startDateFilter) elements.startDateFilter.value = '';
        if (elements.endDateFilter) elements.endDateFilter.value = '';
        if (elements.keywordFilter) elements.keywordFilter.value = '';
        
        // Apply filters (show all)
        applyFilters();
        
        // Update URL
        updateURL();
    }
    
    /**
     * Load filters from URL parameters
     */
    function loadFiltersFromURL() {
        const params = new URLSearchParams(window.location.search);
        
        // Status
        const statuses = params.get('status');
        if (statuses) {
            filterState.status = statuses.split(',');
            if (elements.statusFilter) {
                filterState.status.forEach(status => {
                    const option = Array.from(elements.statusFilter.options).find(opt => opt.value === status);
                    if (option) option.selected = true;
                });
            }
        }
        
        // Province
        const province = params.get('province');
        if (province) {
            filterState.province = province;
            if (elements.provinceFilter) elements.provinceFilter.value = province;
        }
        
        // Category
        const category = params.get('category');
        if (category) {
            filterState.category = category;
            if (elements.categoryFilter) elements.categoryFilter.value = category;
        }
        
        // Date range
        const startDate = params.get('startDate');
        if (startDate) {
            filterState.startDate = startDate;
            if (elements.startDateFilter) elements.startDateFilter.value = startDate;
        }
        
        const endDate = params.get('endDate');
        if (endDate) {
            filterState.endDate = endDate;
            if (elements.endDateFilter) elements.endDateFilter.value = endDate;
        }
        
        // Keyword
        const keyword = params.get('q');
        if (keyword) {
            filterState.keyword = keyword.toLowerCase();
            if (elements.keywordFilter) elements.keywordFilter.value = keyword;
        }
    }
    
    /**
     * Update URL with current filters (bookmarkable)
     */
    function updateURL() {
        const params = new URLSearchParams();
        
        if (filterState.status.length > 0) {
            params.set('status', filterState.status.join(','));
        }
        if (filterState.province) params.set('province', filterState.province);
        if (filterState.category) params.set('category', filterState.category);
        if (filterState.startDate) params.set('startDate', filterState.startDate);
        if (filterState.endDate) params.set('endDate', filterState.endDate);
        if (filterState.keyword) params.set('q', filterState.keyword);
        
        const newURL = params.toString() 
            ? `${window.location.pathname}?${params.toString()}`
            : window.location.pathname;
        
        window.history.replaceState({}, '', newURL);
    }
    
    /**
     * Fade in animation
     */
    function fadeIn(element) {
        element.style.display = 'block';
        element.style.opacity = '0';
        element.style.transform = 'translateY(20px)';
        
        requestAnimationFrame(() => {
            element.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
            element.style.opacity = '1';
            element.style.transform = 'translateY(0)';
        });
    }
    
    /**
     * Fade out animation
     */
    function fadeOut(element) {
        element.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        element.style.opacity = '0';
        element.style.transform = 'translateY(-20px)';
        
        setTimeout(() => {
            element.style.display = 'none';
        }, 300);
    }
    
    // ========== EXPORT TO GLOBAL ==========
    window.initTournamentLiveFilters = initTournamentLiveFilters;
    
})();
