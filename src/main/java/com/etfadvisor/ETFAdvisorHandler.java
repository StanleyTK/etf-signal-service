package com.etfadvisor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AWS Lambda handler for ETF Advisor.
 * Runs daily analysis on ETFs and sends email report with past 10 trading days.
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
            
            // Store results for past 10 trading days for each ticker
            Map<String, List<Map<String, Object>>> tickerHistory = new HashMap<>();
            int minDaysNeeded = 220;
            int daysToAnalyze = 10; // Past 10 trading days
            
            // Process each ticker
            for (String ticker : config.getTickers()) {
                try {
                    context.getLogger().log("Processing ticker: " + ticker);
                    
                    // Fetch historical data (need enough for past 10 days + calculations)
                    List<Double> priceData = YahooFinanceHelper.fetchHistoricalData(ticker, minDaysNeeded + daysToAnalyze, runDate);
                    
                    if (priceData == null || priceData.size() < minDaysNeeded) {
                        context.getLogger().log("Insufficient data for " + ticker + ": " + 
                            (priceData != null ? priceData.size() : 0) + " days");
                        continue;
                    }
                    
                    List<Map<String, Object>> tickerResults = new ArrayList<>();
                    
                    // Calculate signals for past 10 trading days
                    for (int dayOffset = 0; dayOffset < daysToAnalyze && dayOffset < priceData.size() - minDaysNeeded; dayOffset++) {
                        int endIndex = priceData.size() - dayOffset;
                        int startIndex = Math.max(0, endIndex - minDaysNeeded);
                        
                        List<Double> dayData = new ArrayList<>();
                        for (int i = startIndex; i < endIndex; i++) {
                            dayData.add(priceData.get(i));
                        }
                        
                        if (dayData.size() < minDaysNeeded) {
                            continue;
                        }
                        
                        try {
                            // Calculate indicators for this day
                            IndicatorCalculator.Indicators indicators = indicatorCalc.calculateAll(dayData);
                            
                            // Calculate Buy Score
                            BuyScoreCalculator.BuyScoreResult scoreResult = buyScoreCalc.calculate(indicators);
                            
                            // Estimate date (approximate - trading days only)
                            LocalDate dayDate = runDate.minusDays(dayOffset);
                            
                            Map<String, Object> dayResult = new HashMap<>();
                            dayResult.put("ticker", ticker);
                            dayResult.put("date", dayDate.format(DateTimeFormatter.ISO_DATE));
                            dayResult.put("close", indicators.getCloseToday());
                            dayResult.put("sma_200", indicators.getSma200());
                            dayResult.put("drawdown_6m", indicators.getDrawdown6m());
                            dayResult.put("zscore", indicators.getZscore());
                            dayResult.put("buy_score", scoreResult.getBuyScore());
                            dayResult.put("tier", scoreResult.getTier());
                            
                            tickerResults.add(dayResult);
                            
                        } catch (Exception e) {
                            context.getLogger().log("Error processing day offset " + dayOffset + " for " + ticker + ": " + e.getMessage());
                        }
                    }
                    
                    // Reverse to show oldest first
                    Collections.reverse(tickerResults);
                    tickerHistory.put(ticker, tickerResults);
                    
                    context.getLogger().log(String.format("%s: Processed %d days", ticker, tickerResults.size()));
                    
                } catch (Exception e) {
                    context.getLogger().log("Error processing " + ticker + ": " + e.getMessage());
                }
            }
            
            // Send email report if email is configured
            if (config.getEmailFrom() != null && config.getEmailTo() != null && 
                !config.getEmailFrom().isEmpty() && !config.getEmailTo().isEmpty()) {
                EmailService emailService = new EmailService(config);
                emailService.sendEmailReport(runDateStr, tickerHistory, context);
                context.getLogger().log("Email report sent successfully");
            } else {
                context.getLogger().log("Email not configured, skipping email send");
            }
            
            // Build response
            response.put("statusCode", 200);
            response.put("run_date", runDateStr);
            response.put("ticker_history", tickerHistory);
            
        } catch (Exception e) {
            context.getLogger().log("Lambda handler error: " + e.getMessage());
            response.put("statusCode", 500);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
}
