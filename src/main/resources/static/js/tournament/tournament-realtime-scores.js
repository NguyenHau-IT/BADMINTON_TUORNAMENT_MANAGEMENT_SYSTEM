/**
 * Real-time Score Updates using Server-Sent Events (SSE)
 * 
 * FEATURES:
 * - Real-time score updates without polling
 * - Automatic reconnection on disconnect
 * - Smooth score transition animations
 * - Connection status indicator
 * - Fallback to polling if SSE not supported
 * 
 * SSE ENDPOINTS (Backend cần implement):
 * - GET /api/matches/{matchId}/score-stream
 * - GET /api/tournaments/{tournamentId}/live-updates
 * 
 * USAGE:
 * 1. Call: initRealtimeScoreUpdates(matchId);
 * 2. HTML elements required:
 *    - <span id="team1Score">0</span>
 *    - <span id="team2Score">0</span>
 *    - <div id="matchStatus">Đang diễn ra</div>
 * 
 * @author BTMS Team
 * @version 1.0
 */

(function() {
    'use strict';
    
    // ========== CONFIG ==========
    const CONFIG = {
        reconnectDelay: 3000,    // ms - Wait before reconnecting
        heartbeatInterval: 30000, // ms - Heartbeat check
        pollingInterval: 5000,    // ms - Fallback polling interval
        maxReconnectAttempts: 5
    };
    
    // ========== REALTIME SCORE UPDATER ==========
    class RealtimeScoreUpdater {
        constructor(matchId) {
            this.matchId = matchId;
            this.eventSource = null;
            this.reconnectAttempts = 0;
            this.isConnected = false;
            this.useFallback = false;
            this.pollingTimer = null;
            
            this.elements = {
                team1Score: document.getElementById('team1Score'),
                team2Score: document.getElementById('team2Score'),
                team1Set1: document.getElementById('team1Set1'),
                team1Set2: document.getElementById('team1Set2'),
                team1Set3: document.getElementById('team1Set3'),
                team2Set1: document.getElementById('team2Set1'),
                team2Set2: document.getElementById('team2Set2'),
                team2Set3: document.getElementById('team2Set3'),
                matchStatus: document.getElementById('matchStatus'),
                connectionStatus: document.getElementById('connectionStatus')
            };
            
            // Check SSE support
            if (!window.EventSource) {
                console.warn('⚠️ SSE not supported, using polling fallback');
                this.useFallback = true;
            }
            
            this.init();
        }
        
        /**
         * Initialize connection
         */
        init() {
            console.log('🔴 Initializing realtime score updates for match:', this.matchId);
            
            if (this.useFallback) {
                this.startPolling();
            } else {
                this.connect();
            }
        }
        
        /**
         * Connect to SSE endpoint
         */
        connect() {
            if (this.eventSource) {
                this.eventSource.close();
            }
            
            const url = `/api/matches/${this.matchId}/score-stream`;
            console.log('🔌 Connecting to SSE:', url);
            
            try {
                this.eventSource = new EventSource(url);
                
                // Connection opened
                this.eventSource.addEventListener('open', () => {
                    console.log('✅ SSE connection established');
                    this.isConnected = true;
                    this.reconnectAttempts = 0;
                    this.updateConnectionStatus('connected');
                });
                
                // Score update event
                this.eventSource.addEventListener('score-update', (event) => {
                    const data = JSON.parse(event.data);
                    console.log('📊 Score update received:', data);
                    this.updateScore(data);
                });
                
                // Set update event
                this.eventSource.addEventListener('set-update', (event) => {
                    const data = JSON.parse(event.data);
                    console.log('🎯 Set update received:', data);
                    this.updateSet(data);
                });
                
                // Match status change
                this.eventSource.addEventListener('status-change', (event) => {
                    const data = JSON.parse(event.data);
                    console.log('🏁 Status change:', data);
                    this.updateStatus(data);
                });
                
                // Heartbeat
                this.eventSource.addEventListener('heartbeat', () => {
                    console.log('💓 Heartbeat received');
                });
                
                // Error handling
                this.eventSource.addEventListener('error', (error) => {
                    console.error('❌ SSE connection error:', error);
                    this.isConnected = false;
                    this.updateConnectionStatus('disconnected');
                    
                    // Close and attempt reconnect
                    this.eventSource.close();
                    this.attemptReconnect();
                });
                
            } catch (error) {
                console.error('❌ Failed to create SSE connection:', error);
                this.useFallback = true;
                this.startPolling();
            }
        }
        
        /**
         * Attempt to reconnect
         */
        attemptReconnect() {
            if (this.reconnectAttempts >= CONFIG.maxReconnectAttempts) {
                console.warn('⚠️ Max reconnect attempts reached, falling back to polling');
                this.useFallback = true;
                this.startPolling();
                return;
            }
            
            this.reconnectAttempts++;
            this.updateConnectionStatus('reconnecting');
            
            console.log(`🔄 Reconnecting... (attempt ${this.reconnectAttempts}/${CONFIG.maxReconnectAttempts})`);
            
            setTimeout(() => {
                this.connect();
            }, CONFIG.reconnectDelay);
        }
        
        /**
         * Start polling fallback
         */
        startPolling() {
            console.log('🔄 Starting polling fallback');
            this.updateConnectionStatus('polling');
            
            // Clear any existing timer
            if (this.pollingTimer) {
                clearInterval(this.pollingTimer);
            }
            
            // Poll immediately
            this.pollScore();
            
            // Set up interval
            this.pollingTimer = setInterval(() => {
                this.pollScore();
            }, CONFIG.pollingInterval);
        }
        
        /**
         * Poll score from API
         */
        async pollScore() {
            try {
                const response = await fetch(`/api/matches/${this.matchId}/score`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                
                const data = await response.json();
                this.updateScore(data);
                this.updateSet(data);
                this.updateStatus(data);
                
            } catch (error) {
                console.error('❌ Polling error:', error);
            }
        }
        
        /**
         * Update score display
         */
        updateScore(data) {
            if (this.elements.team1Score && data.team1Score !== undefined) {
                this.animateScoreChange(this.elements.team1Score, data.team1Score);
            }
            
            if (this.elements.team2Score && data.team2Score !== undefined) {
                this.animateScoreChange(this.elements.team2Score, data.team2Score);
            }
        }
        
        /**
         * Update set scores
         */
        updateSet(data) {
            if (data.sets) {
                // Set 1
                if (this.elements.team1Set1 && data.sets[0]) {
                    this.elements.team1Set1.textContent = data.sets[0].team1 || 0;
                }
                if (this.elements.team2Set1 && data.sets[0]) {
                    this.elements.team2Set1.textContent = data.sets[0].team2 || 0;
                }
                
                // Set 2
                if (this.elements.team1Set2 && data.sets[1]) {
                    this.elements.team1Set2.textContent = data.sets[1].team1 || 0;
                }
                if (this.elements.team2Set2 && data.sets[1]) {
                    this.elements.team2Set2.textContent = data.sets[1].team2 || 0;
                }
                
                // Set 3
                if (this.elements.team1Set3 && data.sets[2]) {
                    this.elements.team1Set3.textContent = data.sets[2].team1 || 0;
                }
                if (this.elements.team2Set3 && data.sets[2]) {
                    this.elements.team2Set3.textContent = data.sets[2].team2 || 0;
                }
            }
        }
        
        /**
         * Update match status
         */
        updateStatus(data) {
            if (this.elements.matchStatus && data.status) {
                const statusMap = {
                    'scheduled': 'Chưa bắt đầu',
                    'live': 'Đang diễn ra',
                    'finished': 'Kết thúc',
                    'paused': 'Tạm dừng'
                };
                
                this.elements.matchStatus.textContent = statusMap[data.status] || data.status;
                this.elements.matchStatus.className = `match-status status-${data.status}`;
                
                // If match finished, stop updates
                if (data.status === 'finished') {
                    this.disconnect();
                }
            }
        }
        
        /**
         * Animate score change
         */
        animateScoreChange(element, newScore) {
            const oldScore = parseInt(element.textContent) || 0;
            
            if (oldScore === newScore) return;
            
            // Add flash animation
            element.classList.add('score-flash');
            
            // Update value
            element.textContent = newScore;
            
            // Remove animation class after animation completes
            setTimeout(() => {
                element.classList.remove('score-flash');
            }, 600);
        }
        
        /**
         * Update connection status indicator
         */
        updateConnectionStatus(status) {
            if (!this.elements.connectionStatus) return;
            
            const statusConfig = {
                'connected': {
                    text: 'Kết nối',
                    class: 'text-success',
                    icon: 'bi-wifi'
                },
                'disconnected': {
                    text: 'Mất kết nối',
                    class: 'text-danger',
                    icon: 'bi-wifi-off'
                },
                'reconnecting': {
                    text: 'Đang kết nối lại...',
                    class: 'text-warning',
                    icon: 'bi-arrow-repeat'
                },
                'polling': {
                    text: 'Cập nhật định kỳ',
                    class: 'text-info',
                    icon: 'bi-arrow-clockwise'
                }
            };
            
            const config = statusConfig[status];
            if (config) {
                this.elements.connectionStatus.innerHTML = `
                    <i class="bi ${config.icon} ${config.class}"></i>
                    <small class="${config.class}">${config.text}</small>
                `;
            }
        }
        
        /**
         * Disconnect and cleanup
         */
        disconnect() {
            console.log('🔌 Disconnecting realtime updates');
            
            if (this.eventSource) {
                this.eventSource.close();
                this.eventSource = null;
            }
            
            if (this.pollingTimer) {
                clearInterval(this.pollingTimer);
                this.pollingTimer = null;
            }
            
            this.isConnected = false;
        }
    }
    
    // ========== CSS ANIMATIONS (Auto-inject) ==========
    const style = document.createElement('style');
    style.textContent = `
        @keyframes scoreFlash {
            0%, 100% { 
                transform: scale(1); 
                background-color: transparent;
            }
            50% { 
                transform: scale(1.3); 
                background-color: #ffc107;
                color: white;
                border-radius: 4px;
                padding: 2px 6px;
            }
        }
        
        .score-flash {
            animation: scoreFlash 0.6s ease;
            display: inline-block;
        }
        
        .match-status {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-weight: 600;
            font-size: 0.875rem;
        }
        
        .status-live {
            background: #dc3545;
            color: white;
            animation: pulse 2s infinite;
        }
        
        .status-finished {
            background: #6c757d;
            color: white;
        }
        
        .status-scheduled {
            background: #0d6efd;
            color: white;
        }
        
        .status-paused {
            background: #ffc107;
            color: #000;
        }
        
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.7; }
        }
    `;
    document.head.appendChild(style);
    
    // ========== EXPORT TO GLOBAL ==========
    window.initRealtimeScoreUpdates = function(matchId) {
        return new RealtimeScoreUpdater(matchId);
    };
    
    // Auto-initialize if data attribute present
    document.addEventListener('DOMContentLoaded', () => {
        const matchElement = document.querySelector('[data-match-id]');
        if (matchElement) {
            const matchId = matchElement.dataset.matchId;
            console.log('🎯 Auto-initializing realtime scores for match:', matchId);
            window.initRealtimeScoreUpdates(matchId);
        }
    });
    
})();
