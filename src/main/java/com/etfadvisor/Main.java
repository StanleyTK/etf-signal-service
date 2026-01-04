package com.etfadvisor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


public class Main {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("ETF Advisor - Local Testing");
        System.out.println("=".repeat(80));
        System.out.println();
        
        try {
            Config config = new Config();
            System.out.println("Tickers: " + config.getTickers());
            System.out.println();
            
            IndicatorCalculator indicatorCalc = new IndicatorCalculator(config);
            BuyScoreCalculator buyScoreCalc = new BuyScoreCalculator(config);
            
            LocalDate runDate = LocalDate.now();
            int minDaysNeeded = 220;
            
            System.out.println("Fetching historical data and calculating signals...");
            System.out.println("-".repeat(80));
            System.out.println();
            
            // Process each ticker
            for (String ticker : config.getTickers()) {
                try {
                    System.out.println("Processing: " + ticker);
                    
                    // Fetch historical data
                    List<Double> priceData = YahooFinanceHelper.fetchHistoricalData(ticker, minDaysNeeded, runDate);
                    
                    if (priceData == null || priceData.size() < minDaysNeeded) {
                        System.out.println("  ERROR: Insufficient data (" + 
                            (priceData != null ? priceData.size() : 0) + " days)");
                        System.out.println();
                        continue;
                    }
                    
                    // Calculate indicators
                    IndicatorCalculator.Indicators indicators = indicatorCalc.calculateAll(priceData);
                    
                    // Calculate Buy Score
                    BuyScoreCalculator.BuyScoreResult scoreResult = buyScoreCalc.calculate(indicators);
                    
                    // Print results
                    System.out.println("  Price: $" + String.format("%.2f", indicators.getCloseToday()));
                    if (indicators.getSma200() != null) {
                        System.out.println("  SMA 200: $" + String.format("%.2f", indicators.getSma200()));
                    }
                    System.out.println("  Drawdown: " + String.format("%.2f%%", indicators.getDrawdown6m() * 100));
                    System.out.println("  Z-Score: " + String.format("%.2f", indicators.getZscore()));
                    System.out.println("  Buy Score: " + scoreResult.getBuyScore());
                    System.out.println("  Tier: " + scoreResult.getTier());
                    System.out.println();
                    
                } catch (Exception e) {
                    System.err.println("  ERROR: " + e.getMessage());
                    e.printStackTrace();
                    System.out.println();
                }
            }
            
            System.out.println("=".repeat(80));
            System.out.println("Testing complete!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
