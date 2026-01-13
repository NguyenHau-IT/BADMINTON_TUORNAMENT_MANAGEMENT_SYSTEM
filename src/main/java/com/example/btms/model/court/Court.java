package com.example.btms.model.court;

import java.util.Objects;

/**
 * Model đại diện cho một sân cầu lông
 */
public class Court {
    private final String name;
    private final String pin;
    private final String displayName;
    private boolean isActive;
    private String currentMatch;

    public Court(String name, String pin) {
        this.name = name;
        this.pin = pin;
        this.displayName = String.format("Sân %s (PIN: %s)", name, pin);
        this.isActive = false;
        this.currentMatch = "";
    }

    public Court(String name, String pin, String displayName) {
        this.name = name;
        this.pin = pin;
        this.displayName = displayName;
        this.isActive = false;
        this.currentMatch = "";
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPin() {
        return pin;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getCurrentMatch() {
        return currentMatch;
    }

    // Setters
    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void setCurrentMatch(String match) {
        this.currentMatch = match;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Court court = (Court) obj;
        return Objects.equals(pin, court.pin) && Objects.equals(name, court.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pin);
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Lấy số sân từ displayName
     * Ví dụ: "Sân 1 (PIN: 1234)" → 1
     * "Sân 01" → 1
     * Nếu không tìm được số → -1
     */
    public int getCourtNumber() {
        if (displayName == null || displayName.isEmpty()) {
            return -1;
        }

        // Tìm các chữ số trong displayName
        StringBuilder numbers = new StringBuilder();
        for (char c : displayName.toCharArray()) {
            if (Character.isDigit(c)) {
                numbers.append(c);
            } else if (!numbers.isEmpty()) {
                // Dừng ở ký tự đầu tiên không phải số sau khi tìm được số
                break;
            }
        }

        if (numbers.isEmpty()) {
            return -1;
        }

        try {
            return Integer.parseInt(numbers.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
