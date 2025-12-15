package com.example.btms.web.controller.api;

import com.example.btms.service.sse.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE Controller - Server-Sent Events endpoints for real-time updates
 * 
 * Endpoints:
 * - GET /api/sse/tournaments/{id}/subscribe - Subscribe to tournament updates
 * - GET /api/sse/matches/{id}/subscribe - Subscribe to match score updates
 * - GET /api/sse/stats - Get SSE connection statistics
 * 
 * Security: Public endpoints (can add auth later)
 * Performance: Non-blocking SSE with connection pooling
 * 
 * @author BTMS Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/sse")
@CrossOrigin(origins = "*")
public class SseController {

    private static final Logger logger = LoggerFactory.getLogger(SseController.class);

    private final SseEmitterManager sseEmitterManager;

    public SseController(SseEmitterManager sseEmitterManager) {
        this.sseEmitterManager = sseEmitterManager;
    }

    /**
     * GET /api/sse/tournaments/{id}/subscribe
     * Subscribe to real-time updates for a tournament
     * 
     * Events:
     * - connected: Initial connection confirmation
     * - heartbeat: Keep-alive ping
     * - tournament-update: General tournament updates
     * - match-update: Match score updates
     * - registration-update: New registrations
     * 
     * @param id Tournament ID
     * @return SSE emitter for event streaming
     */
    @GetMapping(value = "/tournaments/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeTournament(@PathVariable Integer id) {
        logger.info("SSE subscription request for tournament {}", id);

        try {
            return sseEmitterManager.createTournamentEmitter(id);
        } catch (IllegalStateException e) {
            logger.warn("Failed to create tournament emitter: {}", e.getMessage());
            // Return completed emitter to prevent client hanging
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }
    }

    /**
     * GET /api/sse/matches/{id}/subscribe
     * Subscribe to real-time score updates for a match
     * 
     * Events:
     * - connected: Initial connection confirmation
     * - heartbeat: Keep-alive ping
     * - score-update: Live score changes
     * - set-complete: Set completion
     * - match-complete: Match completion
     * - player-update: Player status changes
     * 
     * @param id Match ID
     * @return SSE emitter for event streaming
     */
    @GetMapping(value = "/matches/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeMatch(@PathVariable Integer id) {
        logger.info("SSE subscription request for match {}", id);

        try {
            return sseEmitterManager.createMatchEmitter(id);
        } catch (IllegalStateException e) {
            logger.warn("Failed to create match emitter: {}", e.getMessage());
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }
    }

    /**
     * GET /api/sse/stats
     * Get SSE connection statistics
     * 
     * For monitoring and debugging
     * 
     * @return Connection stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(sseEmitterManager.getStats());
    }

    /**
     * GET /api/sse/connections
     * Get detailed SSE connections list
     * 
     * Shows all active SSE connections with detailed information:
     * - Tournament connections by ID
     * - Match connections by ID
     * - Connection counts and status
     * - Endpoints and configuration
     * 
     * @return Detailed connections info
     */
    @GetMapping("/connections")
    public ResponseEntity<?> getConnections() {
        return ResponseEntity.ok(sseEmitterManager.getConnections());
    }

    /**
     * POST /api/sse/matches/{id}/score-update
     * Broadcast score update to all clients watching this match
     * 
     * This endpoint is called by Scoreboard app when admin updates scores
     * All connected web clients will receive the update in real-time
     * 
     * @param id        Match ID
     * @param scoreData Score update payload
     * @return Success response
     */
    @PostMapping("/matches/{id}/score-update")
    public ResponseEntity<?> broadcastScoreUpdate(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> scoreData) {

        logger.info("Broadcasting score update for match {}: {}", id, scoreData);

        try {
            // Convert to JSON string
            String jsonData = convertToJson(scoreData);
            sseEmitterManager.sendScoreUpdate(id, jsonData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Score update broadcasted",
                    "matchId", id));
        } catch (Exception e) {
            logger.error("Failed to broadcast score update: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * POST /api/sse/tournaments/{id}/update
     * Broadcast tournament update to all clients watching this tournament
     * 
     * @param id         Tournament ID
     * @param updateData Update payload with eventType and data
     * @return Success response
     */
    @PostMapping("/tournaments/{id}/update")
    public ResponseEntity<?> broadcastTournamentUpdate(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> updateData) {

        String eventType = (String) updateData.getOrDefault("eventType", "tournament-update");
        logger.info("Broadcasting {} for tournament {}", eventType, id);

        try {
            String jsonData = convertToJson(updateData.get("data"));
            sseEmitterManager.sendTournamentUpdate(id, eventType, jsonData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tournament update broadcasted",
                    "tournamentId", id));
        } catch (Exception e) {
            logger.error("Failed to broadcast tournament update: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Simple JSON converter (can use Jackson ObjectMapper for complex cases)
     */
    private String convertToJson(Object data) {
        if (data instanceof String) {
            return (String) data;
        }
        // For Map objects, build JSON manually (or use ObjectMapper)
        // This is a simplified version
        return data.toString().replace("=", ":");
    }
}
