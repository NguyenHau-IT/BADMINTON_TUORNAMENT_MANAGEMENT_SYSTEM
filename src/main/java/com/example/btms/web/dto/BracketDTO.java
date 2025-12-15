package com.example.btms.web.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Tournament Bracket Data
 * 
 * USE CASE: API response for tournament bracket visualization
 * Combines data from SO_DO_CA_NHAN and SO_DO_DOI tables
 * 
 * @author BTMS Team
 * @version 2.0 - Enhanced with RoundDTO for tree view
 */
public class BracketDTO {
    
    private Integer tournamentId;
    private Integer categoryId;
    private String categoryName;
    private String format;            // "single-elimination", "double-elimination", etc.
    private Integer totalRounds;      // Total number of rounds
    private Integer totalMatches;
    private Boolean isTeam;           // true = team bracket, false = singles bracket
    private List<MatchDTO> matches;   // Flat list of all matches
    
    @JsonProperty("rounds")
    private List<RoundDTO> roundsList;    // Matches grouped by rounds for tree view
    
    // ===== NESTED CLASS for Round =====
    
    /**
     * Inner class for each round in the bracket
     */
    public static class RoundDTO {
        private Integer round;        // Round number (1, 2, 3...)
        private String name;          // Round name (Vòng 1, Tứ kết, Bán kết...)
        private List<MatchDTO> matches;
        
        public RoundDTO() {
            this.matches = new ArrayList<>();
        }
        
        public RoundDTO(Integer round, String name) {
            this.round = round;
            this.name = name;
            this.matches = new ArrayList<>();
        }
        
        public Integer getRound() {
            return round;
        }
        
        public void setRound(Integer round) {
            this.round = round;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<MatchDTO> getMatches() {
            return matches;
        }
        
        public void setMatches(List<MatchDTO> matches) {
            this.matches = matches;
        }
        
        public void addMatch(MatchDTO match) {
            this.matches.add(match);
        }
    }
    
    // ===== NESTED CLASS for Match =====
    
    /**
     * Inner class for each match in the bracket
     */
    public static class MatchDTO {
        private Integer id;               // VI_TRI from database
        private Integer round;            // SO_DO from database
        private Integer position;         // VI_TRI - position in bracket
        
        // Player/Team Info
        private Integer player1Id;        // ID_VDV or ID_CLB
        private String player1Name;       // Player name or team name
        private Integer player1Score;     // DIEM
        
        private Integer player2Id;
        private String player2Name;
        private Integer player2Score;
        
        // Match Info
        private String status;            // "scheduled", "live", "completed", "bye"
        private Integer winner;           // player1Id or player2Id, null if not completed
        private LocalDateTime scheduledTime; // THOI_GIAN
        private String matchId;           // ID_TRAN_DAU (UUID)
        
        // Position Info (for rendering)
        private Integer posX;             // TOA_DO_X
        private Integer posY;             // TOA_DO_Y
        
        // Constructors
        public MatchDTO() {}
        
        public MatchDTO(Integer id, Integer round, Integer position,
                       Integer player1Id, String player1Name, Integer player1Score,
                       Integer player2Id, String player2Name, Integer player2Score,
                       String status, Integer winner, LocalDateTime scheduledTime,
                       String matchId, Integer posX, Integer posY) {
            this.id = id;
            this.round = round;
            this.position = position;
            this.player1Id = player1Id;
            this.player1Name = player1Name;
            this.player1Score = player1Score;
            this.player2Id = player2Id;
            this.player2Name = player2Name;
            this.player2Score = player2Score;
            this.status = status;
            this.winner = winner;
            this.scheduledTime = scheduledTime;
            this.matchId = matchId;
            this.posX = posX;
            this.posY = posY;
        }
        
        // Getters and Setters
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public Integer getRound() {
            return round;
        }
        
        public void setRound(Integer round) {
            this.round = round;
        }
        
        public Integer getPosition() {
            return position;
        }
        
        public void setPosition(Integer position) {
            this.position = position;
        }
        
        public Integer getPlayer1Id() {
            return player1Id;
        }
        
        public void setPlayer1Id(Integer player1Id) {
            this.player1Id = player1Id;
        }
        
        public String getPlayer1Name() {
            return player1Name;
        }
        
        public void setPlayer1Name(String player1Name) {
            this.player1Name = player1Name;
        }
        
        public Integer getPlayer1Score() {
            return player1Score;
        }
        
        public void setPlayer1Score(Integer player1Score) {
            this.player1Score = player1Score;
        }
        
        public Integer getPlayer2Id() {
            return player2Id;
        }
        
        public void setPlayer2Id(Integer player2Id) {
            this.player2Id = player2Id;
        }
        
        public String getPlayer2Name() {
            return player2Name;
        }
        
        public void setPlayer2Name(String player2Name) {
            this.player2Name = player2Name;
        }
        
        public Integer getPlayer2Score() {
            return player2Score;
        }
        
        public void setPlayer2Score(Integer player2Score) {
            this.player2Score = player2Score;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public Integer getWinner() {
            return winner;
        }
        
        public void setWinner(Integer winner) {
            this.winner = winner;
        }
        
        public LocalDateTime getScheduledTime() {
            return scheduledTime;
        }
        
        public void setScheduledTime(LocalDateTime scheduledTime) {
            this.scheduledTime = scheduledTime;
        }
        
        public String getMatchId() {
            return matchId;
        }
        
        public void setMatchId(String matchId) {
            this.matchId = matchId;
        }
        
        public Integer getPosX() {
            return posX;
        }
        
        public void setPosX(Integer posX) {
            this.posX = posX;
        }
        
        public Integer getPosY() {
            return posY;
        }
        
        public void setPosY(Integer posY) {
            this.posY = posY;
        }
    }
    
    // Main DTO Constructors
    public BracketDTO() {
        this.matches = new ArrayList<>();
        this.roundsList = new ArrayList<>();
    }
    
    public BracketDTO(Integer tournamentId, Integer categoryId, String categoryName,
                     String format, Integer totalRounds, Integer totalMatches,
                     Boolean isTeam, List<MatchDTO> matches, List<RoundDTO> rounds) {
        this.tournamentId = tournamentId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.format = format;
        this.totalRounds = totalRounds;
        this.totalMatches = totalMatches;
        this.isTeam = isTeam;
        this.matches = matches != null ? matches : new ArrayList<>();
        this.roundsList = rounds != null ? rounds : new ArrayList<>();
    }
    
    // Main DTO Getters and Setters
    public Integer getTournamentId() {
        return tournamentId;
    }
    
    public void setTournamentId(Integer tournamentId) {
        this.tournamentId = tournamentId;
    }
    
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public Integer getTotalRounds() {
        return totalRounds;
    }
    
    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }
    
    public Integer getTotalMatches() {
        return totalMatches;
    }
    
    public void setTotalMatches(Integer totalMatches) {
        this.totalMatches = totalMatches;
    }
    
    public Boolean getIsTeam() {
        return isTeam;
    }
    
    public void setIsTeam(Boolean isTeam) {
        this.isTeam = isTeam;
    }
    
    public List<MatchDTO> getMatches() {
        return matches;
    }
    
    public void setMatches(List<MatchDTO> matches) {
        this.matches = matches != null ? matches : new ArrayList<>();
    }
    
    public List<RoundDTO> getRoundsList() {
        return roundsList;
    }
    
    public void setRoundsList(List<RoundDTO> rounds) {
        this.roundsList = rounds != null ? rounds : new ArrayList<>();
    }
}
