package com.etfadvisor;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates technical indicators: SMA, drawdown, z-score.
 */
public class IndicatorCalculator {
    
    private final Config config;
    
    public IndicatorCalculator(Config config) {
        this.config = config;
    }
    
    /**
     * Calculate all indicators from price data.
     * @param priceData List of daily close prices (most recent last)
     * @return Map with indicators: close_today, sma_200, drawdown_6m, zscore
     */
    public Indicators calculateAll(List<Double> priceData) {
        if (priceData == null || priceData.isEmpty()) {
            throw new IllegalArgumentException("Price data is empty");
        }
        
        double closeToday = priceData.get(priceData.size() - 1);
        Double sma200 = calculateSMA(priceData, config.getSmaDays());
        double drawdown6m = calculateDrawdown(priceData, config.getLookbackHighDays());
        double zscore = calculateZScore(priceData, config.getZscoreWindow());
        
        return new Indicators(closeToday, sma200, drawdown6m, zscore);
    }
    
    /**
     * Calculate Simple Moving Average.
     */
    private Double calculateSMA(List<Double> prices, int window) {
        if (prices.size() < window) {
            return null;
        }
        
        double sum = 0.0;
        for (int i = prices.size() - window; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / window;
    }
    
    /**
     * Calculate drawdown from high.
     */
    private double calculateDrawdown(List<Double> prices, int lookbackDays) {
        int actualLookback = Math.min(lookbackDays, prices.size());
        double closeToday = prices.get(prices.size() - 1);
        
        double maxClose = closeToday;
        for (int i = prices.size() - actualLookback; i < prices.size(); i++) {
            maxClose = Math.max(maxClose, prices.get(i));
        }
        
        return (closeToday / maxClose) - 1.0;
    }
    
    /**
     * Calculate z-score of current close vs rolling mean/std.
     */
    private double calculateZScore(List<Double> prices, int window) {
        int actualWindow = Math.min(window, prices.size());
        double closeToday = prices.get(prices.size() - 1);
        
        // Calculate mean
        double sum = 0.0;
        for (int i = prices.size() - actualWindow; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        double mean = sum / actualWindow;
        
        // Calculate standard deviation
        double variance = 0.0;
        for (int i = prices.size() - actualWindow; i < prices.size(); i++) {
            double diff = prices.get(i) - mean;
            variance += diff * diff;
        }
        variance /= actualWindow;
        double std = Math.sqrt(variance);
        
        if (std == 0) {
            return 0.0;
        }
        
        return (closeToday - mean) / std;
    }
    
    /**
     * Helper class to hold indicator values.
     */
    public static class Indicators {
        private final double closeToday;
        private final Double sma200;
        private final double drawdown6m;
        private final double zscore;
        
        public Indicators(double closeToday, Double sma200, double drawdown6m, double zscore) {
            this.closeToday = closeToday;
            this.sma200 = sma200;
            this.drawdown6m = drawdown6m;
            this.zscore = zscore;
        }
        
        public double getCloseToday() { return closeToday; }
        public Double getSma200() { return sma200; }
        public double getDrawdown6m() { return drawdown6m; }
        public double getZscore() { return zscore; }
    }
}

