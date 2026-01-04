package com.etfadvisor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration loaded from environment variables.
 */
public class Config {
    
    private final List<String> tickers;
    private final String emailFrom;
    private final String emailTo;
    
    // Algorithm parameters
    private final int zscoreWindow;
    private final int lookbackHighDays;
    private final int smaDays;
    
    // Buy Score parameters
    private final double drawdownMax;
    private final double zscoreMax;
    
    // Buy Score tier thresholds
    private final int tierStrongBuy;
    private final int tierBuy;
    private final int tierDcaOnly;
    
    public Config() {
        String tickersStr = System.getenv("TICKERS");
        if (tickersStr == null || tickersStr.isEmpty()) {
            // just for main.java to run locally
            tickersStr = "VOO";
        }
        this.tickers = parseTickers(tickersStr);
        
        // Email configuration - hardcoded
        this.emailFrom = "stanleykim2003@gmail.com";
        this.emailTo = "stanleykim2003@gmail.com";
        
        // Algorithm parameters (with defaults from notebook)
        this.zscoreWindow = getIntEnv("ZSCORE_WINDOW", 30);
        this.lookbackHighDays = getIntEnv("LOOKBACK_HIGH_DAYS", 126);
        this.smaDays = getIntEnv("SMA_DAYS", 200);
        
        // Buy Score parameters (with defaults from notebook)
        this.drawdownMax = getDoubleEnv("DRAWDOWN_MAX", 0.12);
        this.zscoreMax = getDoubleEnv("ZSCORE_MAX", 2.5);
        
        // Buy Score tier thresholds (with defaults from notebook)
        this.tierStrongBuy = getIntEnv("TIER_STRONG_BUY", 75);
        this.tierBuy = getIntEnv("TIER_BUY", 55);
        this.tierDcaOnly = getIntEnv("TIER_DCA_ONLY", 35);
    }
    
    private String getRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Required environment variable " + key + " is not set");
        }
        return value;
    }
    
    private int getIntEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private double getDoubleEnv(String key, double defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private List<String> parseTickers(String tickersStr) {
        return Arrays.stream(tickersStr.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    // Getters
    public List<String> getTickers() { return tickers; }
    public String getEmailFrom() { return emailFrom; }
    public String getEmailTo() { return emailTo; }
    public int getZscoreWindow() { return zscoreWindow; }
    public int getLookbackHighDays() { return lookbackHighDays; }
    public int getSmaDays() { return smaDays; }
    public double getDrawdownMax() { return drawdownMax; }
    public double getZscoreMax() { return zscoreMax; }
    public int getTierStrongBuy() { return tierStrongBuy; }
    public int getTierBuy() { return tierBuy; }
    public int getTierDcaOnly() { return tierDcaOnly; }
}

