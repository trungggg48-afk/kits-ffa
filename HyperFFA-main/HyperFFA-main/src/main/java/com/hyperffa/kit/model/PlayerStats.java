package com.hyperffa.kit.model;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private long playtimeSeconds;
    private int currentKillstreak;
    private int bestKillstreak;
    private double coins;
    private double money;
    private String tier;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
        this.playtimeSeconds = 0;
        this.currentKillstreak = 0;
        this.bestKillstreak = 0;
        this.coins = 0.0;
        this.money = 0.0;
        this.tier = "LT5";
    }

    public PlayerStats(UUID uuid, String name, int kills, int deaths, long playtimeSeconds,
                       int currentKillstreak, int bestKillstreak, double coins, double money, String tier) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.playtimeSeconds = playtimeSeconds;
        this.currentKillstreak = currentKillstreak;
        this.bestKillstreak = bestKillstreak;
        this.coins = coins;
        this.money = money;
        this.tier = (tier != null && !tier.isEmpty()) ? tier : "LT5";
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void incrementKills() {
        this.kills++;
        this.currentKillstreak++;
        if (this.currentKillstreak > this.bestKillstreak) {
            this.bestKillstreak = this.currentKillstreak;
        }
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void incrementDeaths() {
        this.deaths++;
        this.currentKillstreak = 0;
    }

    public long getPlaytimeSeconds() {
        return playtimeSeconds;
    }

    public void addPlaytimeSeconds(long seconds) {
        this.playtimeSeconds += seconds;
    }

    public void setPlaytimeSeconds(long playtimeSeconds) {
        this.playtimeSeconds = playtimeSeconds;
    }

    public long getPlaytimeHours() {
        return playtimeSeconds / 3600;
    }

    public double getKdr() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / (double) deaths;
    }

    public String getFormattedPlaytime() {
        long hours = playtimeSeconds / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    public int getCurrentKillstreak() {
        return currentKillstreak;
    }

    public void setCurrentKillstreak(int currentKillstreak) {
        this.currentKillstreak = currentKillstreak;
    }

    public int getBestKillstreak() {
        return bestKillstreak;
    }

    public void setBestKillstreak(int bestKillstreak) {
        this.bestKillstreak = bestKillstreak;
    }

    public double getCoins() {
        return coins;
    }

    public void setCoins(double coins) {
        this.coins = coins;
    }

    public void addCoins(double amount) {
        this.coins += amount;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public void addMoney(double amount) {
        this.money += amount;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }
}
