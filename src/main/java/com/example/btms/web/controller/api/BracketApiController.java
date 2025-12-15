package com.example.btms.web.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.btms.service.bracket.BracketService;
import com.example.btms.web.dto.BracketDTO;

import java.sql.SQLException;
import java.util.*;

/**
 * Tournament Bracket API Controller
 * 
 * Provides bracket/draw data for tournament visualization
 * Integrated with app bracket system (SO_DO_CA_NHAN, SO_DO_DOI)
 * 
 * Bracket structure follows BWF single-elimination format:
 * - Round 1: 16/32/64 participants
 * - Quarter Finals: 8 participants  
 * - Semi Finals: 4 participants
 * - Finals: 2 participants
 * - Champion: 1 winner
 * 
 * @author BTMS Team
 * @version 3.0 - Full Integration with App Bracket System
 */
@RestController
@RequestMapping("/api/tournaments")
@CrossOrigin(origins = "*")
public class BracketApiController {
    
    private final BracketService bracketService;
    
    public BracketApiController(BracketService bracketService) {
        this.bracketService = bracketService;
    }
    
    /**
     * GET /api/tournaments/{id}/bracket
     * Get bracket data for tournament - returns all categories' brackets
     * 
     * @param id Tournament ID
     * @param categoryId Category ID (optional, defaults to first available)
     * @param isTeam true for team bracket, false for singles (optional, auto-detect)
     * @return Bracket data with matches organized by rounds
     */
    @GetMapping("/{id}/bracket")
    public ResponseEntity<?> getBracket(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean isTeam) {
        try {
            BracketDTO bracketData;
            
            if (categoryId != null && isTeam != null) {
                // Specific category and type requested
                bracketData = bracketService.getBracket(id, categoryId, isTeam);
            } else {
                // Auto-detect first available bracket from database
                bracketData = bracketService.getBracketForTournament(id);
            }
            
            // If no bracket data found, return empty bracket (not mock data)
            if (bracketData.getTotalMatches() == 0) {
                return ResponseEntity.ok(createEmptyBracket(id));
            }
            
            return ResponseEntity.ok(bracketData);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Database error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load bracket",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * GET /api/tournaments/{id}/bracket/all
     * Get all categories' brackets for a tournament
     * 
     * @param id Tournament ID
     * @return List of bracket data for all categories
     */
    @GetMapping("/{id}/bracket/all")
    public ResponseEntity<?> getAllBrackets(@PathVariable Integer id) {
        try {
            List<BracketDTO> allBrackets = bracketService.getAllBracketsForTournament(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tournamentId", id);
            response.put("categories", allBrackets);
            response.put("totalCategories", allBrackets.size());
            
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Database error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load brackets",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * GET /api/tournaments/{id}/bracket/categories
     * Get list of available categories for bracket selection
     * 
     * @param id Tournament ID
     * @return List of categories with bracket data available
     */
    @GetMapping("/{id}/bracket/categories")
    public ResponseEntity<?> getBracketCategories(@PathVariable Integer id) {
        try {
            List<Map<String, Object>> categories = bracketService.getAvailableCategoriesForBracket(id);
            return ResponseEntity.ok(categories);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Database error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to load categories",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Create empty bracket response
     */
    private BracketDTO createEmptyBracket(Integer tournamentId) {
        BracketDTO bracket = new BracketDTO();
        bracket.setTournamentId(tournamentId);
        bracket.setFormat("single-elimination");
        bracket.setTotalRounds(0);
        bracket.setTotalMatches(0);
        bracket.setMatches(new ArrayList<>());
        bracket.setRoundsList(new ArrayList<>());
        bracket.setIsTeam(false);
        return bracket;
    }
}
