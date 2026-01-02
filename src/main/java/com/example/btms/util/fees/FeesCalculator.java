package com.example.btms.util.fees;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.btms.model.club.CauLacBo;
import com.example.btms.model.player.DangKiCaNhan;
import com.example.btms.model.player.VanDongVien;
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

    public static final int FEE_FIRST_EVENT = 200000;
    public static final int FEE_SECOND_AND_MORE = 100000;
    private static final Log log = new Log();

    /**
     * Tính lệ phí cho 1 VĐV dựa trên số nội dung đăng ký
     * 
     * @param eventCount số nội dung đăng ký
     * @return lệ phí
     */
    public static int calculateFeeForPlayer(int eventCount) {
        if (eventCount <= 0) {
            return 0;
        }
        if (eventCount == 1) {
            return FEE_FIRST_EVENT;
        }
        return FEE_FIRST_EVENT + (eventCount - 1) * FEE_SECOND_AND_MORE;
    }

    /**
     * Tính lệ phí theo câu lạc bộ
     * 
     * @param registrations danh sách đăng ký cá nhân
     * @param players       danh sách VĐV
     * @param clubs         danh sách CLB
     * @return Map<ID_CLB, ClubFeeInfo> - thông tin lệ phí theo CLB
     */
    public static Map<Integer, ClubFeeInfo> calculateClubFees(
            List<DangKiCaNhan> registrations,
            List<VanDongVien> players,
            List<CauLacBo> clubs) {

        log.logTs("FeesCalculator: registrations=%d, players=%d, clubs=%d",
                registrations.size(), players.size(), clubs.size());

        // Map VĐV -> số nội dung đăng ký
        Map<Integer, Integer> playerEventCount = new LinkedHashMap<>();
        for (DangKiCaNhan reg : registrations) {
            playerEventCount.merge(reg.getIdVdv(), 1, Integer::sum);
        }
        log.logTs("FeesCalculator: Unique players with registrations: %d", playerEventCount.size());

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

        int skippedCount = 0;
        for (Integer playerId : playerEventCount.keySet()) {
            int eventCount = playerEventCount.get(playerId);
            int playerFee = calculateFeeForPlayer(eventCount);
            Integer clubId = playerClub.get(playerId);

            if (clubId != null && clubId > 0) {
                ClubFeeInfo info = result.computeIfAbsent(clubId, k -> {
                    CauLacBo club = clubMap.get(k);
                    return new ClubFeeInfo(k, club != null ? club.getTenClb() : "Unknown",
                            club != null ? club.getTenNgan() : "");
                });
                info.addPlayerFee(playerId, playerFee, eventCount);
                log.logTs("FeesCalculator: Player %d (events=%d, fee=%d) added to club %d",
                        playerId, eventCount, playerFee, clubId);
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
        private int totalFee = 0;

        public ClubFeeInfo(Integer clubId, String clubName, String clubShortName) {
            this.clubId = clubId;
            this.clubName = clubName;
            this.clubShortName = clubShortName;
        }

        public void addPlayerFee(Integer playerId, int fee, int eventCount) {
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

        public int getTotalFee() {
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
        private final int fee;
        private final int eventCount;

        public PlayerFeeInfo(Integer playerId, int fee, int eventCount) {
            this.playerId = playerId;
            this.fee = fee;
            this.eventCount = eventCount;
        }

        public Integer getPlayerId() {
            return playerId;
        }

        public int getFee() {
            return fee;
        }

        public int getEventCount() {
            return eventCount;
        }
    }
}
