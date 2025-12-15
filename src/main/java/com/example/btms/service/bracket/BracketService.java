package com.example.btms.service.bracket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.example.btms.model.bracket.SoDoCaNhan;
import com.example.btms.model.bracket.SoDoDoi;
import com.example.btms.model.category.NoiDung;
import com.example.btms.model.player.VanDongVien;
import com.example.btms.repository.bracket.SoDoCaNhanRepository;
import com.example.btms.repository.bracket.SoDoDoiRepository;
import com.example.btms.repository.category.NoiDungRepository;
import com.example.btms.repository.player.VanDongVienRepository;
import com.example.btms.web.dto.BracketDTO;

/**
 * Service layer for Tournament Bracket operations
 * 
 * Handles business logic for bracket visualization:
 * - Fetches bracket data from SO_DO_CA_NHAN and SO_DO_DOI tables
 * - Converts to DTO format for API responses
 * - Determines match status based on scores
 * - Organizes matches by rounds following BWF single-elimination format
 * 
 * Bracket Structure (Single-Elimination):
 * - Round 1: 16/32/64 participants (depending on bracket size)
 * - Quarter Finals (Column 2): 8 participants
 * - Semi Finals (Column 3): 4 participants
 * - Finals (Column 4): 2 participants
 * - Champion (Column 5): 1 winner
 * 
 * @author BTMS Team
 * @version 3.0 - Full Integration with App Bracket System
 */
@Service
public class BracketService {
    
    private final DataSource dataSource;
    private SoDoCaNhanRepository soDoCaNhanRepository;
    private SoDoDoiRepository soDoDoiRepository;
    private VanDongVienRepository vanDongVienRepository;
    private NoiDungRepository noiDungRepository;
    
    // Round name mapping based on BWF tournament format
    private static final Map<Integer, String> ROUND_NAMES = Map.of(
        1, "Vòng 1",
        2, "Vòng 2", 
        3, "Tứ kết",
        4, "Bán kết",
        5, "Chung kết",
        6, "Vô địch"
    );
    
    public BracketService(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeRepositories();
    }
    
    /**
     * Initialize repositories with database connection from DataSource
     */
    private void initializeRepositories() {
        try {
            Connection connection = dataSource.getConnection();
            if (connection != null) {
                this.soDoCaNhanRepository = new SoDoCaNhanRepository(connection);
                this.soDoDoiRepository = new SoDoDoiRepository(connection);
                this.vanDongVienRepository = new VanDongVienRepository(connection);
                this.noiDungRepository = new NoiDungRepository(connection);
            }
        } catch (SQLException e) {
            // Repositories will be initialized on first use
        }
    }
    
    /**
     * Ensure repositories are initialized
     */
    private void ensureRepositories() throws SQLException {
        if (soDoCaNhanRepository == null || soDoDoiRepository == null || 
            vanDongVienRepository == null || noiDungRepository == null) {
            Connection connection = dataSource.getConnection();
            if (connection == null) {
                throw new SQLException("No database connection available");
            }
            this.soDoCaNhanRepository = new SoDoCaNhanRepository(connection);
            this.soDoDoiRepository = new SoDoDoiRepository(connection);
            this.vanDongVienRepository = new VanDongVienRepository(connection);
            this.noiDungRepository = new NoiDungRepository(connection);
        }
    }
    
    /**
     * Get bracket data for a specific tournament and category
     * 
     * @param tournamentId Tournament ID (ID_GIAI)
     * @param categoryId Category ID (ID_NOI_DUNG)
     * @param isTeam true for team bracket, false for singles bracket
     * @return BracketDTO with all matches organized by rounds
     * @throws SQLException if database error occurs
     */
    public BracketDTO getBracket(Integer tournamentId, Integer categoryId, Boolean isTeam) throws SQLException {
        ensureRepositories();
        
        BracketDTO bracketDTO = new BracketDTO();
        bracketDTO.setTournamentId(tournamentId);
        bracketDTO.setCategoryId(categoryId);
        bracketDTO.setIsTeam(isTeam);
        bracketDTO.setFormat("single-elimination");
        
        // Get category name
        try {
            NoiDung noiDung = noiDungRepository.findById(categoryId).orElse(null);
            if (noiDung != null) {
                bracketDTO.setCategoryName(noiDung.getTenNoiDung());
            }
        } catch (Exception e) {
            // Category name is optional, continue without it
        }
        
        List<BracketDTO.MatchDTO> matches;
        
        if (isTeam) {
            // Fetch team bracket data from SO_DO_DOI
            List<SoDoDoi> soDoDoiList = soDoDoiRepository.list(tournamentId, categoryId);
            matches = mapTeamBracketToMatches(soDoDoiList);
        } else {
            // Fetch singles bracket data from SO_DO_CA_NHAN
            List<SoDoCaNhan> soDoCaNhanList = soDoCaNhanRepository.list(tournamentId, categoryId);
            matches = mapSinglesBracketToMatches(soDoCaNhanList);
        }
        
        // Calculate max rounds
        Integer maxRound = matches.stream()
            .map(BracketDTO.MatchDTO::getRound)
            .max(Integer::compareTo)
            .orElse(0);
        
        // Group matches into RoundDTO for tree view
        List<BracketDTO.RoundDTO> roundsList = groupMatchesIntoRounds(matches, maxRound);
        
        bracketDTO.setTotalRounds(maxRound);
        bracketDTO.setTotalMatches(matches.size());
        bracketDTO.setMatches(matches);
        bracketDTO.setRoundsList(roundsList);
        
        return bracketDTO;
    }
    
    /**
     * Group matches into rounds for tree view
     */
    private List<BracketDTO.RoundDTO> groupMatchesIntoRounds(List<BracketDTO.MatchDTO> matches, int totalRounds) {
        List<BracketDTO.RoundDTO> rounds = new ArrayList<>();
        
        if (matches == null || matches.isEmpty()) {
            return rounds;
        }
        
        // Group matches by round
        Map<Integer, List<BracketDTO.MatchDTO>> matchesByRound = matches.stream()
            .collect(Collectors.groupingBy(BracketDTO.MatchDTO::getRound));
        
        // Create RoundDTO for each round in order
        for (int round = 1; round <= totalRounds; round++) {
            BracketDTO.RoundDTO roundDTO = new BracketDTO.RoundDTO();
            roundDTO.setRound(round);
            roundDTO.setName(getRoundName(round, totalRounds));
            
            List<BracketDTO.MatchDTO> roundMatches = matchesByRound.get(round);
            if (roundMatches != null) {
                // Sort by position for consistent ordering
                roundMatches.sort((a, b) -> {
                    int posA = a.getPosition() != null ? a.getPosition() : 0;
                    int posB = b.getPosition() != null ? b.getPosition() : 0;
                    return Integer.compare(posA, posB);
                });
                roundDTO.setMatches(roundMatches);
            }
            
            rounds.add(roundDTO);
        }
        
        return rounds;
    }
    
    /**
     * Get bracket data for first available category in tournament
     * Prioritizes categories with actual data
     * 
     * @param tournamentId Tournament ID
     * @return BracketDTO for first category with data
     * @throws SQLException if database error occurs
     */
    public BracketDTO getBracketForTournament(Integer tournamentId) throws SQLException {
        ensureRepositories();
        
        // Get all categories for this tournament
        List<Map<String, Object>> categories = getAvailableCategoriesForBracket(tournamentId);
        
        if (!categories.isEmpty()) {
            Map<String, Object> firstCategory = categories.get(0);
            Integer categoryId = (Integer) firstCategory.get("id");
            Boolean isTeam = (Boolean) firstCategory.get("isTeam");
            return getBracket(tournamentId, categoryId, isTeam);
        }
        
        // No bracket data found, return empty bracket
        BracketDTO emptyBracket = new BracketDTO();
        emptyBracket.setTournamentId(tournamentId);
        emptyBracket.setFormat("single-elimination");
        emptyBracket.setTotalRounds(0);
        emptyBracket.setTotalMatches(0);
        emptyBracket.setMatches(new ArrayList<>());
        emptyBracket.setRoundsList(new ArrayList<>());
        return emptyBracket;
    }
    
    /**
     * Get all brackets for all categories in a tournament
     * 
     * @param tournamentId Tournament ID
     * @return List of BracketDTO for all categories
     * @throws SQLException if database error occurs
     */
    public List<BracketDTO> getAllBracketsForTournament(Integer tournamentId) throws SQLException {
        ensureRepositories();
        
        List<BracketDTO> allBrackets = new ArrayList<>();
        List<Map<String, Object>> categories = getAvailableCategoriesForBracket(tournamentId);
        
        for (Map<String, Object> category : categories) {
            Integer categoryId = (Integer) category.get("id");
            Boolean isTeam = (Boolean) category.get("isTeam");
            BracketDTO bracket = getBracket(tournamentId, categoryId, isTeam);
            allBrackets.add(bracket);
        }
        
        return allBrackets;
    }
    
    /**
     * Get list of categories that have bracket data for a tournament
     * 
     * @param tournamentId Tournament ID
     * @return List of category info maps
     * @throws SQLException if database error occurs
     */
    public List<Map<String, Object>> getAvailableCategoriesForBracket(Integer tournamentId) throws SQLException {
        List<Map<String, Object>> categories = new ArrayList<>();
        
        // Query to find categories that have bracket data for this tournament
        // NOI_DUNG table doesn't have ID_GIAI, so we find categories via SO_DO tables
        String sql = """
            SELECT DISTINCT nd.ID as ID_NOI_DUNG, nd.TEN_NOI_DUNG, nd.TEAM,
                   CASE WHEN sd.cnt > 0 THEN 1 ELSE 0 END as hasSingles,
                   CASE WHEN sdd.cnt > 0 THEN 1 ELSE 0 END as hasTeam
            FROM NOI_DUNG nd
            LEFT JOIN (
                SELECT ID_NOI_DUNG, COUNT(*) as cnt 
                FROM SO_DO_CA_NHAN 
                WHERE ID_GIAI = ? 
                GROUP BY ID_NOI_DUNG
            ) sd ON nd.ID = sd.ID_NOI_DUNG
            LEFT JOIN (
                SELECT ID_NOI_DUNG, COUNT(*) as cnt 
                FROM SO_DO_DOI 
                WHERE ID_GIAI = ? 
                GROUP BY ID_NOI_DUNG
            ) sdd ON nd.ID = sdd.ID_NOI_DUNG
            WHERE (sd.cnt > 0 OR sdd.cnt > 0)
            ORDER BY nd.ID
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tournamentId);
            stmt.setInt(2, tournamentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> category = new HashMap<>();
                    category.put("id", rs.getInt("ID_NOI_DUNG"));
                    category.put("name", rs.getString("TEN_NOI_DUNG"));
                    category.put("isTeam", rs.getBoolean("TEAM"));
                    category.put("hasSinglesData", rs.getInt("hasSingles") > 0);
                    category.put("hasTeamData", rs.getInt("hasTeam") > 0);
                    categories.add(category);
                }
            }
        }
        
        return categories;
    }
    
    /**
     * Map team bracket data (SoDoDoi) to match DTOs
     * Groups entries by position to create match pairs
     */
    private List<BracketDTO.MatchDTO> mapTeamBracketToMatches(List<SoDoDoi> soDoDoiList) {
        List<BracketDTO.MatchDTO> matches = new ArrayList<>();
        
        if (soDoDoiList == null || soDoDoiList.isEmpty()) {
            return matches;
        }
        
        // Group by column (round) and position
        Map<Integer, Map<Integer, List<SoDoDoi>>> byRoundAndPosition = new HashMap<>();
        
        for (SoDoDoi sodo : soDoDoiList) {
            int round = sodo.getSoDo() != null ? sodo.getSoDo() : 1;
            int position = sodo.getViTri() != null ? sodo.getViTri() : 1;
            // Position starts from 1, so (position-1)/2 groups pairs: (1,2)->0, (3,4)->1, etc.
            int matchIndex = (position - 1) / 2;
            
            byRoundAndPosition
                .computeIfAbsent(round, k -> new HashMap<>())
                .computeIfAbsent(matchIndex, k -> new ArrayList<>())
                .add(sodo);
        }
        
        int matchId = 1;
        for (Integer round : byRoundAndPosition.keySet().stream().sorted().collect(Collectors.toList())) {
            Map<Integer, List<SoDoDoi>> roundMatches = byRoundAndPosition.get(round);
            
            for (Integer matchPos : roundMatches.keySet().stream().sorted().collect(Collectors.toList())) {
                List<SoDoDoi> pair = roundMatches.get(matchPos);
                
                BracketDTO.MatchDTO match = new BracketDTO.MatchDTO();
                match.setId(matchId++);
                match.setRound(round);
                match.setPosition(matchPos);
                
                // First entry in pair
                if (!pair.isEmpty()) {
                    SoDoDoi first = pair.get(0);
                    match.setPlayer1Name(first.getTenTeam() != null ? first.getTenTeam() : "TBD");
                    match.setPlayer1Id(first.getIdClb());
                    match.setPlayer1Score(first.getDiem());
                    match.setScheduledTime(first.getThoiGian());
                    match.setMatchId(first.getIdTranDau());
                    match.setPosX(first.getToaDoX());
                    match.setPosY(first.getToaDoY());
                }
                
                // Second entry in pair (if exists)
                if (pair.size() > 1) {
                    SoDoDoi second = pair.get(1);
                    match.setPlayer2Name(second.getTenTeam() != null ? second.getTenTeam() : "TBD");
                    match.setPlayer2Id(second.getIdClb());
                    match.setPlayer2Score(second.getDiem());
                } else {
                    match.setPlayer2Name("BYE");
                }
                
                // Determine status and winner
                determineMatchStatusAndWinner(match);
                
                matches.add(match);
            }
        }
        
        return matches;
    }
    
    /**
     * Map singles bracket data (SoDoCaNhan) to match DTOs
     * Groups entries by position to create match pairs
     */
    private List<BracketDTO.MatchDTO> mapSinglesBracketToMatches(List<SoDoCaNhan> soDoCaNhanList) {
        List<BracketDTO.MatchDTO> matches = new ArrayList<>();
        
        if (soDoCaNhanList == null || soDoCaNhanList.isEmpty()) {
            return matches;
        }
        
        // Pre-fetch all player names for efficiency
        Map<Integer, String> playerNames = new HashMap<>();
        for (SoDoCaNhan sodo : soDoCaNhanList) {
            Integer idVdv = sodo.getIdVdv();
            if (idVdv != null && !playerNames.containsKey(idVdv)) {
                try {
                    VanDongVien vdv = vanDongVienRepository.findById(idVdv);
                    if (vdv != null && vdv.getHoTen() != null) {
                        playerNames.put(idVdv, vdv.getHoTen());
                    } else {
                        playerNames.put(idVdv, "VĐV #" + idVdv);
                    }
                } catch (Exception e) {
                    playerNames.put(idVdv, "VĐV #" + idVdv);
                }
            }
        }
        
        // Group by column (round) and match position
        Map<Integer, Map<Integer, List<SoDoCaNhan>>> byRoundAndMatch = new HashMap<>();
        
        for (SoDoCaNhan sodo : soDoCaNhanList) {
            int round = sodo.getSoDo() != null ? sodo.getSoDo() : 1;
            int position = sodo.getViTri() != null ? sodo.getViTri() : 1;
            // Position starts from 1, so (position-1)/2 groups pairs: (1,2)->0, (3,4)->1, etc.
            int matchIndex = (position - 1) / 2;
            
            byRoundAndMatch
                .computeIfAbsent(round, k -> new HashMap<>())
                .computeIfAbsent(matchIndex, k -> new ArrayList<>())
                .add(sodo);
        }
        
        int matchId = 1;
        for (Integer round : byRoundAndMatch.keySet().stream().sorted().collect(Collectors.toList())) {
            Map<Integer, List<SoDoCaNhan>> roundMatches = byRoundAndMatch.get(round);
            
            for (Integer matchPos : roundMatches.keySet().stream().sorted().collect(Collectors.toList())) {
                List<SoDoCaNhan> pair = roundMatches.get(matchPos);
                
                // Sort by position to ensure consistent ordering
                pair.sort((a, b) -> {
                    int posA = a.getViTri() != null ? a.getViTri() : 0;
                    int posB = b.getViTri() != null ? b.getViTri() : 0;
                    return Integer.compare(posA, posB);
                });
                
                BracketDTO.MatchDTO match = new BracketDTO.MatchDTO();
                match.setId(matchId++);
                match.setRound(round);
                match.setPosition(matchPos);
                
                // First player
                if (!pair.isEmpty()) {
                    SoDoCaNhan first = pair.get(0);
                    match.setPlayer1Id(first.getIdVdv());
                    match.setPlayer1Name(playerNames.getOrDefault(first.getIdVdv(), "TBD"));
                    match.setPlayer1Score(first.getDiem());
                    match.setScheduledTime(first.getThoiGian());
                    match.setMatchId(first.getIdTranDau());
                    match.setPosX(first.getToaDoX());
                    match.setPosY(first.getToaDoY());
                }
                
                // Second player (if exists)
                if (pair.size() > 1) {
                    SoDoCaNhan second = pair.get(1);
                    match.setPlayer2Id(second.getIdVdv());
                    match.setPlayer2Name(playerNames.getOrDefault(second.getIdVdv(), "TBD"));
                    match.setPlayer2Score(second.getDiem());
                } else {
                    match.setPlayer2Name("BYE");
                }
                
                // Determine status and winner
                determineMatchStatusAndWinner(match);
                
                matches.add(match);
            }
        }
        
        return matches;
    }
    
    /**
     * Determine match status and winner based on scores
     */
    private void determineMatchStatusAndWinner(BracketDTO.MatchDTO match) {
        Integer score1 = match.getPlayer1Score();
        Integer score2 = match.getPlayer2Score();
        
        // Check for BYE
        if ("BYE".equals(match.getPlayer2Name())) {
            match.setStatus("bye");
            match.setWinner(match.getPlayer1Id());
            return;
        }
        
        // Determine status based on scores
        if (score1 != null && score2 != null && score1 > 0 && score2 > 0) {
            // Both scores exist - match is completed or in progress
            // In badminton, typically games go to 21 or 30 (tiebreak)
            if (score1 >= 21 || score2 >= 21) {
                match.setStatus("completed");
                if (score1 > score2) {
                    match.setWinner(match.getPlayer1Id());
                } else if (score2 > score1) {
                    match.setWinner(match.getPlayer2Id());
                }
            } else {
                match.setStatus("live");
            }
        } else if (score1 != null && score1 > 0) {
            // Only first score exists
            match.setStatus("live");
        } else if (match.getScheduledTime() != null) {
            match.setStatus("scheduled");
        } else {
            match.setStatus("pending");
        }
    }
    
    /**
     * Get round name based on round number and total rounds
     */
    public String getRoundName(int round, int totalRounds) {
        if (totalRounds <= 0) return "Vòng " + round;
        
        int fromFinal = totalRounds - round;
        
        switch (fromFinal) {
            case 0: return "Vô địch";
            case 1: return "Chung kết";
            case 2: return "Bán kết";
            case 3: return "Tứ kết";
            default: return "Vòng " + round;
        }
    }
    
    /**
     * Check if bracket exists for tournament and category
     */
    public boolean hasBracket(Integer tournamentId, Integer categoryId, Boolean isTeam) throws SQLException {
        ensureRepositories();
        
        if (isTeam) {
            List<SoDoDoi> list = soDoDoiRepository.list(tournamentId, categoryId);
            return !list.isEmpty();
        } else {
            List<SoDoCaNhan> list = soDoCaNhanRepository.list(tournamentId, categoryId);
            return !list.isEmpty();
        }
    }
}
