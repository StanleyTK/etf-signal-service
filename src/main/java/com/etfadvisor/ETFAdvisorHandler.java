package com.etfadvisor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AWS Lambda handler for ETF Advisor.
 * Runs daily analysis on hardcoded ETFs and returns signals.
 */
public class ETFAdvisorHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Config config = new Config();
            context.getLogger().log("Starting ETF Advisor analysis");
            
            LocalDate runDate = LocalDate.now();
            String runDateStr = runDate.format(DateTimeFormatter.ISO_DATE);
            context.getLogger().log("Processing date: " + runDateStr);
            
            IndicatorCalculator indicatorCalc = new IndicatorCalculator(config);
            BuyScoreCalculator buyScoreCalc = new BuyScoreCalculator(config);
            
            List<Map<String, Object>> results = new ArrayList<>();
            int minDaysNeeded = 220;
            
            // Process each ticker
            for (String ticker : config.getTickers()) {
                try {
                    context.getLogger().log("Processing ticker: " + ticker);
                    
                    // Fetch historical data (at least 220 days)
                    List<Double> priceData = YahooFinanceHelper.fetchHistoricalData(ticker, minDaysNeeded, runDate);
                    
                    if (priceData == null || priceData.size() < minDaysNeeded) {
                        context.getLogger().log("Insufficient data for " + ticker + ": " + 
                            (priceData != null ? priceData.size() : 0) + " days");
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("ticker", ticker);
                        errorResult.put("status", "INSUFFICIENT_DATA");
                        errorResult.put("error", "Less than " + minDaysNeeded + " days of data");
                        results.add(errorResult);
                        continue;
                    }
                    
                    // Calculate indicators
                    IndicatorCalculator.Indicators indicators = indicatorCalc.calculateAll(priceData);
                    
                    // Calculate Buy Score
                    BuyScoreCalculator.BuyScoreResult scoreResult = buyScoreCalc.calculate(indicators);
                    
                    // Build result
                    Map<String, Object> result = new HashMap<>();
                    result.put("ticker", ticker);
                    result.put("status", "OK");
                    result.put("close_today", indicators.getCloseToday());
                    result.put("sma_200", indicators.getSma200());
                    result.put("drawdown_6m", indicators.getDrawdown6m());
                    result.put("zscore", indicators.getZscore());
                    result.put("buy_score", scoreResult.getBuyScore());
                    result.put("tier", scoreResult.getTier());
                    result.put("trend_score", scoreResult.getTrendScore());
                    result.put("drawdown_score", scoreResult.getDrawdownScore());
                    result.put("zscore_score", scoreResult.getZscoreScore());
                    
                    results.add(result);
                    
                    context.getLogger().log(String.format("%s: %s (Score=%d, Price=%.2f)", 
                        ticker, scoreResult.getTier(), scoreResult.getBuyScore(), indicators.getCloseToday()));
                    
                } catch (Exception e) {
                    context.getLogger().log("Error processing " + ticker + ": " + e.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("ticker", ticker);
                    errorResult.put("status", "ERROR");
                    errorResult.put("error", e.getMessage());
                    results.add(errorResult);
                }
            }
            
            // Calculate summary
            Map<String, Integer> tierCounts = new HashMap<>();
            
            for (Map<String, Object> r : results) {
                if ("OK".equals(r.get("status"))) {
                    String tier = (String) r.get("tier");
                    tierCounts.put(tier, tierCounts.getOrDefault(tier, 0) + 1);
                }
            }
            
            // Build response
            response.put("statusCode", 200);
            response.put("run_date", runDateStr);
            response.put("tier_counts", tierCounts);
            response.put("results", results);
            
        } catch (Exception e) {
            context.getLogger().log("Lambda handler error: " + e.getMessage());
            response.put("statusCode", 500);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
}
