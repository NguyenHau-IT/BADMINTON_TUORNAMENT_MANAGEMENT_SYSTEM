package com.example.btms.web.dto;

/**
 * DTO chứa thống kê tổng quan của giải đấu
 */
public class OverviewStatsDTO {
    private int totalContents; // Tổng số nội dung thi đấu
    private int totalPlayersInTournament; // Tổng số VĐV tham gia giải
    private int totalClubs; // Tổng số câu lạc bộ
    private String tournamentName; // Tên giải đấu
    private String tournamentStatus; // Trạng thái giải đấu

    // Constructor
    public OverviewStatsDTO() {
    }

    public OverviewStatsDTO(int totalContents, int totalPlayersInTournament,
            int totalClubs, String tournamentName, String tournamentStatus) {
        this.totalContents = totalContents;
        this.totalPlayersInTournament = totalPlayersInTournament;
        this.totalClubs = totalClubs;
        this.tournamentName = tournamentName;
        this.tournamentStatus = tournamentStatus;
    }

    // Getters and Setters
    public int getTotalContents() {
        return totalContents;
    }

    public void setTotalContents(int totalContents) {
        this.totalContents = totalContents;
    }

    public int getTotalPlayersInTournament() {
        return totalPlayersInTournament;
    }

    public void setTotalPlayersInTournament(int totalPlayersInTournament) {
        this.totalPlayersInTournament = totalPlayersInTournament;
    }

    public int getTotalClubs() {
        return totalClubs;
    }

    public void setTotalClubs(int totalClubs) {
        this.totalClubs = totalClubs;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String getTournamentStatus() {
        return tournamentStatus;
    }

    public void setTournamentStatus(String tournamentStatus) {
        this.tournamentStatus = tournamentStatus;
    }

    @Override
    public String toString() {
        return "OverviewStatsDTO{" +
                "totalContents=" + totalContents +
                ", totalPlayersInTournament=" + totalPlayersInTournament +
                ", totalClubs=" + totalClubs +
                ", tournamentName='" + tournamentName + '\'' +
                ", tournamentStatus='" + tournamentStatus + '\'' +
                '}';
    }
}