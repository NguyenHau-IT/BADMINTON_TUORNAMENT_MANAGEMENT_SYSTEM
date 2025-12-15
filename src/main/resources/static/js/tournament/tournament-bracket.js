/**
 * Tournament Bracket Table View
 * 
 * Hiển thị sơ đồ thi đấu dạng bảng
 * Tích hợp với dữ liệu từ app (SO_DO_CA_NHAN, SO_DO_DOI)
 * 
 * @author BTMS Team
 * @version 5.0 - Table Only View
 */

class TournamentBracket {
    constructor() {
        this.tableContainer = document.getElementById('bracketTableContainer');
        this.tableBody = document.getElementById('bracketTableBody');
        this.categoryTabsContainer = document.getElementById('bracketCategoryTabs');
        this.emptyContainer = document.getElementById('bracketEmpty');
        this.loadingContainer = document.getElementById('bracketLoading');
        
        this.bracketData = null;
        this.allCategories = [];
        this.currentCategoryId = null;
        this.tournamentId = null;
    }
    
    showLoading() {
        if (this.loadingContainer) this.loadingContainer.style.display = 'block';
        if (this.tableContainer) this.tableContainer.style.display = 'none';
        if (this.emptyContainer) this.emptyContainer.style.display = 'none';
    }
    
    hideLoading() {
        if (this.loadingContainer) this.loadingContainer.style.display = 'none';
    }
    
    showEmpty() {
        this.hideLoading();
        if (this.tableContainer) this.tableContainer.style.display = 'none';
        if (this.emptyContainer) this.emptyContainer.style.display = 'block';
    }
    
    showTable() {
        this.hideLoading();
        if (this.tableContainer) this.tableContainer.style.display = 'block';
        if (this.emptyContainer) this.emptyContainer.style.display = 'none';
    }
    
    async loadBracket(tournamentId) {
        try {
            this.showLoading();
            this.tournamentId = tournamentId;
            
            await this.loadCategories(tournamentId);
            
            if (this.allCategories.length > 0) {
                const firstCat = this.allCategories[0];
                await this.loadCategoryBracket(firstCat.id, firstCat.isTeam);
            } else {
                this.showEmpty();
            }
        } catch (error) {
            console.error('Error loading bracket:', error);
            this.showEmpty();
        }
    }
    
    async loadCategories(tournamentId) {
        try {
            const response = await fetch(`/api/tournaments/${tournamentId}/bracket/categories`);
            if (!response.ok) throw new Error('Failed to load categories');
            
            this.allCategories = await response.json();
            this.renderCategoryTabs();
        } catch (error) {
            console.error('Error loading categories:', error);
            this.allCategories = [];
        }
    }
    
    async loadCategoryBracket(categoryId, isTeam = false) {
        try {
            this.showLoading();
            this.currentCategoryId = categoryId;
            
            const endpoint = isTeam ? 'bracket-team' : 'bracket';
            const url = `/api/tournaments/${this.tournamentId}/${endpoint}?categoryId=${categoryId}`;
            
            const response = await fetch(url);
            if (!response.ok) throw new Error('Failed to load bracket data');
            
            this.bracketData = await response.json();
            this.updateActiveTab(categoryId);
            this.renderTable();
        } catch (error) {
            console.error('Error loading category bracket:', error);
            this.showEmpty();
        }
    }
    
    renderCategoryTabs() {
        if (!this.categoryTabsContainer) return;
        
        this.categoryTabsContainer.innerHTML = '';
        if (this.allCategories.length === 0) return;
        
        this.allCategories.forEach((cat, index) => {
            const tab = document.createElement('button');
            tab.className = 'bracket-category-tab' + (index === 0 ? ' active' : '');
            tab.dataset.categoryId = cat.id;
            tab.dataset.isTeam = cat.isTeam;
            
            const icon = cat.isTeam ? 'bi-people-fill' : 'bi-person-fill';
            tab.innerHTML = `<i class="bi ${icon}"></i> <span>${cat.name}</span>`;
            
            tab.addEventListener('click', () => this.loadCategoryBracket(cat.id, cat.isTeam));
            this.categoryTabsContainer.appendChild(tab);
        });
    }
    
    updateActiveTab(categoryId) {
        if (!this.categoryTabsContainer) return;
        
        this.categoryTabsContainer.querySelectorAll('.bracket-category-tab').forEach(tab => {
            tab.classList.toggle('active', tab.dataset.categoryId == categoryId);
        });
    }
    
    renderTable() {
        if (!this.tableBody) {
            this.showEmpty();
            return;
        }
        
        const matches = this.bracketData?.matches || [];
        if (matches.length === 0) {
            this.showEmpty();
            return;
        }
        
        this.tableBody.innerHTML = '';
        
        // Sort by round then position
        const sorted = [...matches].sort((a, b) => {
            if (a.round !== b.round) return a.round - b.round;
            return a.position - b.position;
        });
        
        sorted.forEach(match => {
            const row = this.createTableRow(match);
            this.tableBody.appendChild(row);
        });
        
        this.showTable();
    }
    
    createTableRow(match) {
        const row = document.createElement('tr');
        row.className = this.getRowClass(match);
        
        const roundName = this.getRoundName(match.round, this.bracketData.totalRounds);
        const dateTime = this.formatDateTime(match.scheduledTime);
        const score = this.formatScore(match);
        const status = this.getStatusBadge(match.status);
        
        const p1Win = match.winner === match.player1Id;
        const p2Win = match.winner === match.player2Id;
        
        row.innerHTML = `
            <td><span class="round-badge">${roundName}</span></td>
            <td>${dateTime.date}</td>
            <td>${dateTime.time}</td>
            <td class="text-end ${p1Win ? 'fw-bold text-success' : ''}">${this.escapeHtml(match.player1Name || 'TBD')}</td>
            <td class="text-center">${score}</td>
            <td class="${p2Win ? 'fw-bold text-success' : ''}">${this.escapeHtml(match.player2Name || 'TBD')}</td>
            <td class="text-center">${status}</td>
        `;
        
        return row;
    }
    
    getRowClass(match) {
        switch (match.status) {
            case 'live': return 'table-warning';
            case 'completed': return '';
            case 'bye': return 'text-muted';
            default: return '';
        }
    }
    
    getRoundName(round, totalRounds) {
        if (!totalRounds || totalRounds <= 0) return `Vòng ${round}`;
        
        // fromFinal = số vòng từ vòng chung kết (0 = chung kết)
        const fromFinal = totalRounds - round;
        
        switch (fromFinal) {
            case 0: return 'Chung kết';
            case 1: return 'Bán kết';
            case 2: return 'Tứ kết';
            case 3: return 'Vòng 16';
            case 4: return 'Vòng 32';
            default: return `Vòng ${round}`;
        }
    }
    
    formatDateTime(dateTimeStr) {
        if (!dateTimeStr) return { date: '-', time: '-' };
        
        try {
            const dt = new Date(dateTimeStr);
            return {
                date: dt.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
                time: dt.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
            };
        } catch {
            return { date: '-', time: '-' };
        }
    }
    
    formatScore(match) {
        if (match.status === 'bye') return '<span class="text-muted">BYE</span>';
        if (match.status === 'upcoming') return '<span class="text-muted">vs</span>';
        
        const s1 = match.player1Score ?? '-';
        const s2 = match.player2Score ?? '-';
        
        if (match.status === 'live') {
            return `<span class="text-danger fw-bold">${s1} - ${s2}</span>`;
        }
        
        return `<strong>${s1}</strong> - <strong>${s2}</strong>`;
    }
    
    getStatusBadge(status) {
        const badges = {
            'upcoming': '<span class="badge bg-secondary">Chưa đấu</span>',
            'scheduled': '<span class="badge bg-info">Đã lên lịch</span>',
            'live': '<span class="badge bg-danger">LIVE</span>',
            'completed': '<span class="badge bg-success">Hoàn thành</span>',
            'bye': '<span class="badge bg-light text-dark">BYE</span>'
        };
        return badges[status] || badges['upcoming'];
    }
    
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

window.TournamentBracket = TournamentBracket;
