package com.example.btms.util.fees;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.btms.model.club.CauLacBo;
import com.example.btms.model.player.DangKiCaNhan;
import com.example.btms.model.player.VanDongVien;
import com.example.btms.model.team.ChiTietDoi;
import com.example.btms.model.team.DangKiDoi;
import com.example.btms.util.log.Log;

/**
 * Tính lệ phí theo câu lạc bộ dựa trên đăng ký nội dung
 * 
 * Công thức:
 * - 1 VĐV đăng ký nội dung đầu: 200 000đ
 * - Nội dung thứ 2 trở đi: mỗi nội dung 100 000đ
 * 
 * Tổng lệ phí CLB = Tổng lệ phí của tất cả VĐV trong CLB
 */
public class FeesCalculator {

    private static final Log log = new Log();

    /**
     * Tính lệ phí cho 1 VĐV dựa trên số nội dung đăng ký
     * 
     * @param eventCount         số nội dung đăng ký
     * @param firstEventFee      lệ phí nội dung đầu tiên
     * @param subsequentEventFee lệ phí nội dung từ 2 trở đi
     * @return lệ phí
     */
    public static long calculateFeeForPlayer(int eventCount, long firstEventFee, long subsequentEventFee) {
        if (eventCount <= 0) {
            return 0;
        }
        if (eventCount == 1) {
            return firstEventFee;
        }
        return firstEventFee + (eventCount - 1) * subsequentEventFee;
    }

    /**
     * Tính lệ phí theo câu lạc bộ (từ cả đăng ký cá nhân và đăng ký đội)
     * Mỗi VĐV được tính lệ phí dựa trên tổng số nội dung = đăng ký cá nhân + tất cả
     * đội mà VĐV tham gia
     * 
     * @param registrations      danh sách đăng ký cá nhân
     * @param teamRegistrations  danh sách đăng ký đội
     * @param teamDetails        danh sách chi tiết VĐV trong đội
     * @param players            danh sách VĐV
     * @param clubs              danh sách CLB
     * @param firstEventFee      lệ phí nội dung đầu tiên (từ panel)
     * @param subsequentEventFee lệ phí nội dung từ 2 trở đi (từ panel)
     * @return Map<ID_CLB, ClubFeeInfo> - thông tin lệ phí theo CLB
     */
    public static Map<Integer, ClubFeeInfo> calculateClubFees(
            List<DangKiCaNhan> registrations,
            List<DangKiDoi> teamRegistrations,
            List<ChiTietDoi> teamDetails,
            List<VanDongVien> players,
            List<CauLacBo> clubs,
            long firstEventFee,
            long subsequentEventFee) {

        log.logTs("FeesCalculator: individual=%d, teams=%d, teamDetails=%d, players=%d, clubs=%d",
                registrations.size(), teamRegistrations.size(), teamDetails.size(), players.size(), clubs.size());

        // Map VĐV -> số nội dung đăng ký cá nhân
        Map<Integer, Integer> playerEventCount = new LinkedHashMap<>();
        for (DangKiCaNhan reg : registrations) {
            playerEventCount.merge(reg.getIdVdv(), 1, Integer::sum);
        }
        log.logTs("FeesCalculator: Players with individual registrations: %d", playerEventCount.size());

        // Map Team ID -> nội dung ID (để biết nội dung nào đội tham gia)
        Map<Integer, Integer> teamContent = new LinkedHashMap<>();
        for (DangKiDoi team : teamRegistrations) {
            teamContent.put(team.getIdTeam(), team.getIdNoiDung());
        }

        // Map VĐV -> số nội dung từ đội (tính từ ChiTietDoi)
        Map<Integer, Integer> playerTeamEventCount = new LinkedHashMap<>();
        for (ChiTietDoi detail : teamDetails) {
            if (teamContent.containsKey(detail.getIdTeam())) {
                playerTeamEventCount.merge(detail.getIdVdv(), 1, Integer::sum);
            }
        }
        log.logTs("FeesCalculator: Players with team registrations: %d", playerTeamEventCount.size());

        // Map VĐV -> tổng số nội dung (cá nhân + đội)
        Map<Integer, Integer> playerTotalEventCount = new LinkedHashMap<>();
        for (Integer playerId : playerEventCount.keySet()) {
            int totalEvents = playerEventCount.get(playerId) + playerTeamEventCount.getOrDefault(playerId, 0);
            playerTotalEventCount.put(playerId, totalEvents);
        }
        for (Integer playerId : playerTeamEventCount.keySet()) {
            if (!playerTotalEventCount.containsKey(playerId)) {
                playerTotalEventCount.put(playerId, playerTeamEventCount.get(playerId));
            }
        }
        log.logTs("FeesCalculator: Players with total registrations: %d", playerTotalEventCount.size());

        // Map VĐV -> CLB
        Map<Integer, Integer> playerClub = new LinkedHashMap<>();
        for (VanDongVien player : players) {
            playerClub.put(player.getId(), player.getIdClb());
        }

        // Map CLB -> thông tin CLB
        Map<Integer, CauLacBo> clubMap = new LinkedHashMap<>();
        for (CauLacBo club : clubs) {
            clubMap.put(club.getId(), club);
        }

        // Tính lệ phí theo CLB
        Map<Integer, ClubFeeInfo> result = new LinkedHashMap<>();

        // Xử lý lệ phí từ đăng ký cá nhân + đội (dựa trên tổng nội dung của mỗi VĐV)
        int skippedCount = 0;
        for (Integer playerId : playerTotalEventCount.keySet()) {
            int totalEventCount = playerTotalEventCount.get(playerId);
            long playerFee = calculateFeeForPlayer(totalEventCount, firstEventFee, subsequentEventFee);
            Integer clubId = playerClub.get(playerId);

            if (clubId != null && clubId > 0) {
                ClubFeeInfo info = result.computeIfAbsent(clubId, k -> {
                    CauLacBo club = clubMap.get(k);
                    return new ClubFeeInfo(k, club != null ? club.getTenClb() : "Unknown",
                            club != null ? club.getTenNgan() : "");
                });
                int individualCount = playerEventCount.getOrDefault(playerId, 0);
                int teamCount = playerTeamEventCount.getOrDefault(playerId, 0);
                info.addPlayerFee(playerId, playerFee, totalEventCount);
                log.logTs("FeesCalculator: Player %d (individual=%d, team=%d, total=%d, fee=%d) added to club %d",
                        playerId, individualCount, teamCount, totalEventCount, playerFee, clubId);
            } else {
                skippedCount++;
                log.logTs("FeesCalculator: Player %d skipped - clubId=%s", playerId, clubId);
            }
        }

        log.logTs("FeesCalculator: Result clubs=%d, skipped players=%d", result.size(), skippedCount);

        return result;
    }

    /**
     * Lớp chứa thông tin lệ phí của 1 CLB
     */
    public static class ClubFeeInfo {
        private final Integer clubId;
        private final String clubName;
        private final String clubShortName;
        private final Map<Integer, PlayerFeeInfo> playerFees = new LinkedHashMap<>();
        private long totalFee = 0;

        public ClubFeeInfo(Integer clubId, String clubName, String clubShortName) {
            this.clubId = clubId;
            this.clubName = clubName;
            this.clubShortName = clubShortName;
        }

        public void addPlayerFee(Integer playerId, long fee, int eventCount) {
            playerFees.put(playerId, new PlayerFeeInfo(playerId, fee, eventCount));
            totalFee += fee;
        }

        public Integer getClubId() {
            return clubId;
        }

        public String getClubName() {
            return clubName;
        }

        public String getClubShortName() {
            return clubShortName;
        }

        public Map<Integer, PlayerFeeInfo> getPlayerFees() {
            return playerFees;
        }

        public long getTotalFee() {
            return totalFee;
        }

        public int getPlayerCount() {
            return playerFees.size();
        }
    }

    /**
     * Lớp chứa thông tin lệ phí của 1 VĐV
     */
    public static class PlayerFeeInfo {
        private final Integer playerId;
        private final long fee;
        private final int eventCount;

        public PlayerFeeInfo(Integer playerId, long fee, int eventCount) {
            this.playerId = playerId;
            this.fee = fee;
            this.eventCount = eventCount;
        }

        public Integer getPlayerId() {
            return playerId;
        }

        public long getFee() {
            return fee;
        }

        public int getEventCount() {
            return eventCount;
        }
    }
}
