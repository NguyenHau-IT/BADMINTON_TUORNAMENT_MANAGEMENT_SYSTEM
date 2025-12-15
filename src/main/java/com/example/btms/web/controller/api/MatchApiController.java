package com.example.btms.web.controller.api;

import com.example.btms.model.match.ChiTietTranDau;
import com.example.btms.model.match.ChiTietVan;
import com.example.btms.service.match.ChiTietTranDauService;
import com.example.btms.service.match.ChiTietVanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * REST API Controller cho Match (Trận đấu)
 * 
 * Base URL: /api/matches
 * 
 * Provides endpoints for:
 * - Real-time score updates (SSE)
 * - Polling fallback for score retrieval
 * - Match details
 * 
 * @author BTMS Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MatchApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(MatchApiController.class);
    
    @Autowired(required = false)
    private ChiTietTranDauService matchService;
    
    @Autowired(required = false)
    private ChiTietVanService setService;
    
    // Store active SSE connections
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    
    // Heartbeat scheduler
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
    
    public MatchApiController() {
        // Start heartbeat task every 30 seconds
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeats, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * GET /api/matches/{matchId}/score-stream
     * Server-Sent Events endpoint for real-time score updates
     * 
     * Events:
     * - connected: Initial connection confirmation
     * - score-update: Score changes (team1Score, team2Score, currentSet)
     * - set-update: Set completion (set1, set2, set3)
     * - status-change: Match status changes (ongoing, completed, paused)
     * - heartbeat: Keep-alive ping every 30 seconds
     * 
     * @param matchId Match ID (String UUID)
     * @return SseEmitter for event streaming
     */
    @GetMapping(value = "/{matchId}/score-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamScores(@PathVariable String matchId) {
        logger.info("SSE connection request for match: {}", matchId);
        
        // Create emitter with 5 minute timeout
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes
        
        // Add to active connections
        activeEmitters.put(matchId, emitter);
        
        // Handle completion/timeout
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for match: {}", matchId);
            activeEmitters.remove(matchId);
        });
        
        emitter.onTimeout(() -> {
            logger.warn("SSE connection timeout for match: {}", matchId);
            activeEmitters.remove(matchId);
            emitter.complete();
        });
        
        emitter.onError((e) -> {
            logger.error("SSE connection error for match {}: {}", matchId, e.getMessage());
            activeEmitters.remove(matchId);
        });
        
        // Send initial connection confirmation
        try {
            Map<String, Object> connectionData = new HashMap<>();
            connectionData.put("matchId", matchId);
            connectionData.put("connected", true);
            connectionData.put("timestamp", System.currentTimeMillis());
            
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(connectionData));
            
            // Send current score immediately
            sendCurrentScore(emitter, matchId);
            
        } catch (IOException e) {
            logger.error("Failed to send initial data for match {}: {}", matchId, e.getMessage());
            emitter.completeWithError(e);
            activeEmitters.remove(matchId);
        }
        
        return emitter;
    }
    
    /**
     * GET /api/matches/{matchId}/score
     * Polling fallback endpoint for score retrieval
     * 
     * Returns current match score and status
     * 
     * Response:
     * {
     *   "matchId": "123e4567-e89b-12d3-a456-426614174000",
     *   "status": "ongoing",
     *   "team1Score": 21,
     *   "team2Score": 18,
     *   "currentSet": 2,
     *   "sets": [
     *     { "setNumber": 1, "team1Score": 21, "team2Score": 19 },
     *     { "setNumber": 2, "team1Score": 18, "team2Score": 21 }
     *   ],
     *   "lastUpdated": 1700123456789
     * }
     * 
     * @param matchId Match ID
     * @return ResponseEntity with match score data
     */
    @GetMapping("/{matchId}/score")
    public ResponseEntity<Map<String, Object>> getScore(@PathVariable String matchId) {
        logger.debug("GET /api/matches/{}/score", matchId);
        
        try {
            Map<String, Object> response = buildScoreResponse(matchId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting score for match {}: {}", matchId, e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve score: " + e.getMessage());
            error.put("matchId", matchId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * POST /api/matches/{matchId}/score
     * Update match score (for admin/scorekeeper use)
     * 
     * Request Body:
     * {
     *   "team1Score": 21,
     *   "team2Score": 18,
     *   "setNumber": 2,
     *   "status": "ongoing"
     * }
     * 
     * Broadcasts update to all SSE subscribers
     * 
     * @param matchId Match ID
     * @param scoreUpdate Score update data
     * @return ResponseEntity with success status
     */
    @PostMapping("/{matchId}/score")
    public ResponseEntity<Map<String, Object>> updateScore(
            @PathVariable String matchId,
            @RequestBody Map<String, Object> scoreUpdate) {
        
        logger.info("Score update for match {}: {}", matchId, scoreUpdate);
        
        try {
            // Broadcast to all SSE subscribers
            broadcastScoreUpdate(matchId, scoreUpdate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("matchId", matchId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to update score for match {}: {}", matchId, e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Build score response from database
     */
    private Map<String, Object> buildScoreResponse(String matchId) {
        Map<String, Object> response = new HashMap<>();
        response.put("matchId", matchId);
        response.put("lastUpdated", System.currentTimeMillis());
        
        // Check if services are available
        if (matchService == null || setService == null) {
            logger.warn("Match services not available, returning mock data");
            response.put("status", "unavailable");
            response.put("message", "Match service not configured");
            return response;
        }
        
        try {
            // Get match details
            ChiTietTranDau match = matchService.get(matchId);
            
            if (match != null) {
                // Determine status
                String status = determineMatchStatus(match);
                response.put("status", status);
                
                // Get sets data
                List<ChiTietVan> sets = setService.listByMatch(matchId);
                response.put("totalSets", sets.size());
                
                // Build sets array
                java.util.List<Map<String, Object>> setsData = new java.util.ArrayList<>();
                int team1TotalScore = 0;
                int team2TotalScore = 0;
                
                for (ChiTietVan set : sets) {
                    Map<String, Object> setData = new HashMap<>();
                    setData.put("setNumber", set.getSetNo());
                    setData.put("team1Score", set.getTongDiem1());
                    setData.put("team2Score", set.getTongDiem2());
                    setsData.add(setData);
                    
                    // Count sets won
                    if (set.getTongDiem1() > set.getTongDiem2()) {
                        team1TotalScore++;
                    } else if (set.getTongDiem2() > set.getTongDiem1()) {
                        team2TotalScore++;
                    }
                }
                
                response.put("sets", setsData);
                response.put("team1SetsWon", team1TotalScore);
                response.put("team2SetsWon", team2TotalScore);
                response.put("currentSet", sets.isEmpty() ? 1 : sets.size());
                
                // Current set scores (last set if ongoing)
                if (!sets.isEmpty()) {
                    ChiTietVan currentSet = sets.get(sets.size() - 1);
                    response.put("team1Score", currentSet.getTongDiem1());
                    response.put("team2Score", currentSet.getTongDiem2());
                } else {
                    response.put("team1Score", 0);
                    response.put("team2Score", 0);
                }
                
            } else {
                response.put("status", "not-found");
                response.put("message", "Match not found");
            }
            
        } catch (Exception e) {
            logger.error("Error building score response: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Determine match status from database
     */
    private String determineMatchStatus(ChiTietTranDau match) {
        LocalDateTime now = LocalDateTime.now();
        
        if (match.getKetThuc() != null && now.isAfter(match.getKetThuc())) {
            return "completed";
        } else if (match.getBatDau() != null && now.isAfter(match.getBatDau()) && now.isBefore(match.getKetThuc())) {
            return "ongoing";
        } else {
            return "upcoming";
        }
    }
    
    /**
     * Send current score to a specific emitter
     */
    private void sendCurrentScore(SseEmitter emitter, String matchId) throws IOException {
        Map<String, Object> scoreData = buildScoreResponse(matchId);
        
        emitter.send(SseEmitter.event()
            .name("score-update")
            .data(scoreData));
    }
    
    /**
     * Broadcast score update to all subscribers
     */
    private void broadcastScoreUpdate(String matchId, Map<String, Object> scoreUpdate) {
        SseEmitter emitter = activeEmitters.get(matchId);
        
        if (emitter != null) {
            try {
                scoreUpdate.put("timestamp", System.currentTimeMillis());
                
                emitter.send(SseEmitter.event()
                    .name("score-update")
                    .data(scoreUpdate));
                
                logger.info("Broadcasted score update for match: {}", matchId);
                
            } catch (IOException e) {
                logger.error("Failed to broadcast score for match {}: {}", matchId, e.getMessage());
                activeEmitters.remove(matchId);
                emitter.completeWithError(e);
            }
        }
    }
    
    /**
     * Send heartbeat to all active connections
     */
    private void sendHeartbeats() {
        activeEmitters.forEach((matchId, emitter) -> {
            try {
                Map<String, Object> heartbeat = new HashMap<>();
                heartbeat.put("timestamp", System.currentTimeMillis());
                
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(heartbeat));
                
            } catch (IOException e) {
                logger.warn("Heartbeat failed for match {}, removing connection", matchId);
                activeEmitters.remove(matchId);
                emitter.completeWithError(e);
            }
        });
    }
}
