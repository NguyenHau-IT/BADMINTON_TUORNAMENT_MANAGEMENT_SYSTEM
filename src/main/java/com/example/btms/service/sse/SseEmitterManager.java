package com.example.btms.service.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE Emitter Manager - Manages Server-Sent Events connections
 * 
 * Features:
 * - Connection pooling by tournament/match
 * - Auto cleanup of dead connections
 * - Heartbeat mechanism
 * - Thread-safe operations
 * 
 * Performance: ConcurrentHashMap for O(1) lookups
 * Security: Timeout and connection limits
 * Maintainability: Centralized SSE management
 * 
 * @author BTMS Team
 * @version 1.0
 */
@Service
public class SseEmitterManager {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterManager.class);

    // Configuration
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private static final long HEARTBEAT_INTERVAL = 15000L; // 15 seconds
    private static final int MAX_CONNECTIONS_PER_RESOURCE = 1000;

    // Connection pools - organized by resource type
    private final Map<String, Set<SseEmitter>> tournamentEmitters = new ConcurrentHashMap<>();
    private final Map<String, Set<SseEmitter>> matchEmitters = new ConcurrentHashMap<>();

    // Heartbeat scheduler
    private final ScheduledExecutorService heartbeatScheduler;

    public SseEmitterManager() {
        this.heartbeatScheduler = Executors.newScheduledThreadPool(1);
        startHeartbeat();
    }

    /**
     * Create and register SSE emitter for tournament updates
     */
    public SseEmitter createTournamentEmitter(Integer tournamentId) {
        String key = "tournament-" + tournamentId;

        // Check connection limit
        Set<SseEmitter> emitters = tournamentEmitters.computeIfAbsent(key,
                k -> new CopyOnWriteArraySet<>());

        if (emitters.size() >= MAX_CONNECTIONS_PER_RESOURCE) {
            logger.warn("Max connections reached for tournament {}", tournamentId);
            throw new IllegalStateException("Too many active connections");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Setup callbacks
        emitter.onCompletion(() -> removeEmitter(key, emitter, tournamentEmitters));
        emitter.onTimeout(() -> removeEmitter(key, emitter, tournamentEmitters));
        emitter.onError(e -> {
            logger.debug("SSE error for tournament {}: {}", tournamentId, e.getMessage());
            removeEmitter(key, emitter, tournamentEmitters);
        });

        emitters.add(emitter);
        logger.info("SSE client connected to tournament {} (total: {})", tournamentId, emitters.size());

        // Send initial connection message as JSON
        try {
            String jsonData = String.format(
                    "{\"tournamentId\":%d,\"message\":\"Connected\",\"timestamp\":%d}",
                    tournamentId, System.currentTimeMillis());
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(jsonData));
        } catch (IOException e) {
            logger.warn("Failed to send initial message: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * Create and register SSE emitter for match updates
     */
    public SseEmitter createMatchEmitter(Integer matchId) {
        String key = "match-" + matchId;

        Set<SseEmitter> emitters = matchEmitters.computeIfAbsent(key,
                k -> new CopyOnWriteArraySet<>());

        if (emitters.size() >= MAX_CONNECTIONS_PER_RESOURCE) {
            logger.warn("Max connections reached for match {}", matchId);
            throw new IllegalStateException("Too many active connections");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> removeEmitter(key, emitter, matchEmitters));
        emitter.onTimeout(() -> removeEmitter(key, emitter, matchEmitters));
        emitter.onError(e -> {
            logger.debug("SSE error for match {}: {}", matchId, e.getMessage());
            removeEmitter(key, emitter, matchEmitters);
        });

        emitters.add(emitter);
        logger.info("SSE client connected to match {} (total: {})", matchId, emitters.size());

        // Send initial connection message as JSON
        try {
            String jsonData = String.format(
                    "{\"matchId\":%d,\"message\":\"Connected\",\"timestamp\":%d}",
                    matchId, System.currentTimeMillis());
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(jsonData));
        } catch (IOException e) {
            logger.warn("Failed to send initial message: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * Broadcast update to all tournament subscribers
     */
    public void broadcastTournamentUpdate(Integer tournamentId, String eventName, Object data) {
        String key = "tournament-" + tournamentId;
        broadcastToEmitters(tournamentEmitters.get(key), eventName, data, key);
    }

    /**
     * Broadcast update to all match subscribers
     */
    public void broadcastMatchUpdate(Integer matchId, String eventName, Object data) {
        String key = "match-" + matchId;
        broadcastToEmitters(matchEmitters.get(key), eventName, data, key);
    }

    /**
     * Internal broadcast method
     */
    private void broadcastToEmitters(Set<SseEmitter> emitters, String eventName,
            Object data, String resourceKey) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        logger.debug("Broadcasting {} to {} clients on {}", eventName, emitters.size(), resourceKey);

        // Copy to avoid ConcurrentModificationException
        Set<SseEmitter> deadEmitters = new CopyOnWriteArraySet<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                logger.debug("Failed to send to client: {}", e.getMessage());
                deadEmitters.add(emitter);
            } catch (Exception e) {
                logger.error("Unexpected error broadcasting: {}", e.getMessage(), e);
                deadEmitters.add(emitter);
            }
        }

        // Cleanup dead connections
        emitters.removeAll(deadEmitters);
        deadEmitters.forEach(emitter -> emitter.complete());
    }

    /**
     * Remove emitter from pool
     */
    private void removeEmitter(String key, SseEmitter emitter,
            Map<String, Set<SseEmitter>> emitterMap) {
        Set<SseEmitter> emitters = emitterMap.get(key);
        if (emitters != null) {
            emitters.remove(emitter);
            logger.debug("SSE client disconnected from {} (remaining: {})", key, emitters.size());

            // Cleanup empty sets
            if (emitters.isEmpty()) {
                emitterMap.remove(key);
            }
        }
    }

    /**
     * Start heartbeat mechanism to keep connections alive
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat();
            } catch (Exception e) {
                logger.error("Heartbeat error: {}", e.getMessage(), e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        logger.info("SSE heartbeat started (interval: {}ms)", HEARTBEAT_INTERVAL);
    }

    /**
     * Send heartbeat to all connected clients
     */
    private void sendHeartbeat() {
        int totalClients = 0;

        // Tournament emitters
        for (Set<SseEmitter> emitters : tournamentEmitters.values()) {
            totalClients += emitters.size();
            broadcastToEmitters(emitters, "heartbeat",
                    Map.of("timestamp", System.currentTimeMillis()), "");
        }

        // Match emitters
        for (Set<SseEmitter> emitters : matchEmitters.values()) {
            totalClients += emitters.size();
            broadcastToEmitters(emitters, "heartbeat",
                    Map.of("timestamp", System.currentTimeMillis()), "");
        }

        if (totalClients > 0) {
            logger.trace("Heartbeat sent to {} clients", totalClients);
        }
    }

    /**
     * Send score update to all clients watching a match
     * This method is called from Scoreboard app when admin updates scores
     * 
     * @param matchId   Match ID
     * @param scoreData Score data as JSON string
     */
    public void sendScoreUpdate(Integer matchId, String scoreData) {
        String key = "match-" + matchId;
        Set<SseEmitter> emitters = matchEmitters.get(key);

        if (emitters != null && !emitters.isEmpty()) {
            logger.info("Broadcasting score update to {} clients for match {}", emitters.size(), matchId);
            broadcastToEmitters(emitters, "score-update", scoreData, key);
        } else {
            logger.debug("No clients connected to match {}, skipping broadcast", matchId);
        }
    }

    /**
     * Send tournament update to all clients watching a tournament
     * 
     * @param tournamentId Tournament ID
     * @param eventType    Event type (tournament-update, registration-update, etc.)
     * @param data         Event data as JSON string
     */
    public void sendTournamentUpdate(Integer tournamentId, String eventType, String data) {
        String key = "tournament-" + tournamentId;
        Set<SseEmitter> emitters = tournamentEmitters.get(key);

        if (emitters != null && !emitters.isEmpty()) {
            logger.info("Broadcasting {} to {} clients for tournament {}",
                    eventType, emitters.size(), tournamentId);
            broadcastToEmitters(emitters, eventType, data, key);
        }
    }

    /**
     * Get connection statistics
     */
    public Map<String, Object> getStats() {
        int tournamentConnections = tournamentEmitters.values().stream()
                .mapToInt(Set::size)
                .sum();

        int matchConnections = matchEmitters.values().stream()
                .mapToInt(Set::size)
                .sum();

        return Map.of(
                "tournamentConnections", tournamentConnections,
                "matchConnections", matchConnections,
                "totalConnections", tournamentConnections + matchConnections,
                "tournamentChannels", tournamentEmitters.size(),
                "matchChannels", matchEmitters.size());
    }

    /**
     * Get detailed SSE connections list
     */
    public Map<String, Object> getConnections() {
        Map<String, Object> connections = new HashMap<>();

        // Tournament connections details
        Map<String, Object> tournamentDetails = new HashMap<>();
        for (Map.Entry<String, Set<SseEmitter>> entry : tournamentEmitters.entrySet()) {
            String key = entry.getKey();
            Set<SseEmitter> emitters = entry.getValue();

            Map<String, Object> connectionInfo = new HashMap<>();
            connectionInfo.put("count", emitters.size());
            connectionInfo.put("maxConnections", MAX_CONNECTIONS_PER_RESOURCE);
            connectionInfo.put("type", "tournament");
            connectionInfo.put("resourceId", key.replace("tournament-", ""));
            connectionInfo.put("endpoint", "/api/sse/tournaments/" + key.replace("tournament-", "") + "/subscribe");
            connectionInfo.put("status", emitters.size() > 0 ? "active" : "inactive");
            connectionInfo.put("lastUpdate", System.currentTimeMillis());

            tournamentDetails.put(key, connectionInfo);
        }

        // Match connections details
        Map<String, Object> matchDetails = new HashMap<>();
        for (Map.Entry<String, Set<SseEmitter>> entry : matchEmitters.entrySet()) {
            String key = entry.getKey();
            Set<SseEmitter> emitters = entry.getValue();

            Map<String, Object> connectionInfo = new HashMap<>();
            connectionInfo.put("count", emitters.size());
            connectionInfo.put("maxConnections", MAX_CONNECTIONS_PER_RESOURCE);
            connectionInfo.put("type", "match");
            connectionInfo.put("resourceId", key.replace("match-", ""));
            connectionInfo.put("endpoint", "/api/sse/matches/" + key.replace("match-", "") + "/subscribe");
            connectionInfo.put("status", emitters.size() > 0 ? "active" : "inactive");
            connectionInfo.put("lastUpdate", System.currentTimeMillis());

            matchDetails.put(key, connectionInfo);
        }

        // Summary statistics
        int totalTournamentConnections = tournamentEmitters.values().stream()
                .mapToInt(Set::size)
                .sum();
        int totalMatchConnections = matchEmitters.values().stream()
                .mapToInt(Set::size)
                .sum();

        Map<String, Object> summary = Map.of(
                "totalConnections", totalTournamentConnections + totalMatchConnections,
                "tournamentConnections", totalTournamentConnections,
                "matchConnections", totalMatchConnections,
                "tournamentChannels", tournamentEmitters.size(),
                "matchChannels", matchEmitters.size(),
                "heartbeatInterval", HEARTBEAT_INTERVAL,
                "sseTimeout", SSE_TIMEOUT,
                "maxConnectionsPerResource", MAX_CONNECTIONS_PER_RESOURCE,
                "timestamp", System.currentTimeMillis());

        connections.put("summary", summary);
        connections.put("tournaments", tournamentDetails);
        connections.put("matches", matchDetails);

        return connections;
    }

    /**
     * Cleanup on shutdown
     */
    public void shutdown() {
        logger.info("Shutting down SSE manager...");
        heartbeatScheduler.shutdown();

        // Complete all emitters
        tournamentEmitters.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
        matchEmitters.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));

        tournamentEmitters.clear();
        matchEmitters.clear();

        logger.info("SSE manager shutdown complete");
    }
}
