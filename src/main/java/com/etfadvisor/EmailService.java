package com.etfadvisor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.util.*;

/**
 * Service for sending ETF Advisor email reports.
 */
public class EmailService {
    
    private final AmazonSimpleEmailService sesClient;
    private final Config config;
    
    public EmailService(Config config) {
        this.config = config;
        this.sesClient = AmazonSimpleEmailServiceClientBuilder.defaultClient();
    }
    
    /**
     * Send email report with past 10 trading days for each ticker.
     */
    public void sendEmailReport(String runDate, Map<String, List<Map<String, Object>>> tickerHistory, Context context) {
        try {
            // Get today's signals for summary
            List<Map<String, Object>> todaySignals = new ArrayList<>();
            for (String ticker : config.getTickers()) {
                List<Map<String, Object>> history = tickerHistory.get(ticker);
                if (history != null && !history.isEmpty()) {
                    Map<String, Object> today = history.get(history.size() - 1); // Most recent (last in list)
                    todaySignals.add(today);
                }
            }
            
            StringBuilder html = new StringBuilder();
            html.append("<html><head><style>");
            html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 1200px; margin: 0 auto; }");
            html.append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }");
            html.append("h2 { color: #34495e; margin-top: 30px; }");
            html.append(".summary-box { background: #f8f9fa; border: 2px solid #3498db; border-radius: 8px; padding: 20px; margin: 20px 0; }");
            html.append(".buy-recommendation { background: #fff; border-left: 4px solid #27ae60; padding: 15px; margin: 10px 0; border-radius: 4px; }");
            html.append(".wait-recommendation { background: #fff; border-left: 4px solid #95a5a6; padding: 15px; margin: 10px 0; border-radius: 4px; }");
            html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; font-size: 14px; }");
            html.append("th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }");
            html.append("th { background-color: #3498db; color: white; font-weight: bold; }");
            html.append("tr:nth-child(even) { background-color: #f8f9fa; }");
            html.append("tr:hover { background-color: #e8f4f8; }");
            html.append(".tier-strong-buy { color: #c0392b; font-weight: bold; }");
            html.append(".tier-buy { color: #e67e22; font-weight: bold; }");
            html.append(".tier-dca-only { color: #3498db; }");
            html.append(".tier-wait { color: #7f8c8d; }");
            html.append(".score-high { color: #27ae60; font-weight: bold; }");
            html.append(".score-medium { color: #f39c12; }");
            html.append(".score-low { color: #e74c3c; }");
            html.append("</style></head><body>");
            
            html.append("<h1>ETF Advisor — Daily Report</h1>");
            html.append("<p><strong>Date:</strong> ").append(runDate).append("</p>");
            
            // TODAY'S BUY RECOMMENDATIONS SUMMARY
            html.append("<div class=\"summary-box\">");
            html.append("<h2 style=\"margin-top: 0;\">📊 Today's Buy Recommendations</h2>");
            
            List<Map<String, Object>> strongBuy = new ArrayList<>();
            List<Map<String, Object>> buy = new ArrayList<>();
            List<Map<String, Object>> dcaOnly = new ArrayList<>();
            List<Map<String, Object>> wait = new ArrayList<>();
            
            for (Map<String, Object> signal : todaySignals) {
                String tier = (String) signal.get("tier");
                if ("STRONG_BUY".equals(tier)) {
                    strongBuy.add(signal);
                } else if ("BUY".equals(tier)) {
                    buy.add(signal);
                } else if ("DCA_ONLY".equals(tier)) {
                    dcaOnly.add(signal);
                } else {
                    wait.add(signal);
                }
            }
            
            if (!strongBuy.isEmpty()) {
                html.append("<div class=\"buy-recommendation\">");
                html.append("<strong style=\"color: #c0392b; font-size: 16px;\">🔴 STRONG_BUY</strong><br>");
                for (Map<String, Object> s : strongBuy) {
                    String ticker = (String) s.get("ticker");
                    int score = ((Number) s.get("buy_score")).intValue();
                    double price = ((Number) s.get("close")).doubleValue();
                    html.append(String.format("• %s (Score: %d, Price: $%.2f)<br>", ticker, score, price));
                }
                html.append("</div>");
            }
            
            if (!buy.isEmpty()) {
                html.append("<div class=\"buy-recommendation\">");
                html.append("<strong style=\"color: #e67e22; font-size: 16px;\">🟠 BUY</strong><br>");
                for (Map<String, Object> s : buy) {
                    String ticker = (String) s.get("ticker");
                    int score = ((Number) s.get("buy_score")).intValue();
                    double price = ((Number) s.get("close")).doubleValue();
                    html.append(String.format("• %s (Score: %d, Price: $%.2f)<br>", ticker, score, price));
                }
                html.append("</div>");
            }
            
            if (!dcaOnly.isEmpty()) {
                html.append("<div class=\"buy-recommendation\">");
                html.append("<strong style=\"color: #3498db; font-size: 16px;\">🔵 DCA_ONLY</strong><br>");
                for (Map<String, Object> s : dcaOnly) {
                    String ticker = (String) s.get("ticker");
                    int score = ((Number) s.get("buy_score")).intValue();
                    double price = ((Number) s.get("close")).doubleValue();
                    html.append(String.format("• %s (Score: %d, Price: $%.2f)<br>", ticker, score, price));
                }
                html.append("</div>");
            }
            
            if (!wait.isEmpty()) {
                html.append("<div class=\"wait-recommendation\">");
                html.append("<strong style=\"color: #7f8c8d; font-size: 16px;\">⚪ WAIT</strong><br>");
                for (Map<String, Object> s : wait) {
                    String ticker = (String) s.get("ticker");
                    int score = ((Number) s.get("buy_score")).intValue();
                    double price = ((Number) s.get("close")).doubleValue();
                    html.append(String.format("• %s (Score: %d, Price: $%.2f)<br>", ticker, score, price));
                }
                html.append("</div>");
            }
            
            html.append("</div>");
            
            html.append("<hr style=\"margin: 30px 0; border: 1px solid #ddd;\">");
            html.append("<h2>📈 Detailed Analysis (Past 10 Trading Days)</h2>");
            
            // Process each ticker
            for (String ticker : config.getTickers()) {
                List<Map<String, Object>> history = tickerHistory.get(ticker);
                if (history == null || history.isEmpty()) {
                    continue;
                }
                
                html.append("<h2>").append(ticker).append("</h2>");
                html.append("<table>");
                html.append("<tr>");
                html.append("<th>Date</th>");
                html.append("<th>Price</th>");
                html.append("<th>SMA 200</th>");
                html.append("<th>Drawdown</th>");
                html.append("<th>Z-Score</th>");
                html.append("<th>Buy Score</th>");
                html.append("<th>Tier</th>");
                html.append("</tr>");
                
                for (Map<String, Object> day : history) {
                    String date = (String) day.get("date");
                    double close = ((Number) day.get("close")).doubleValue();
                    Double sma200 = day.get("sma_200") != null ? ((Number) day.get("sma_200")).doubleValue() : null;
                    double drawdown = ((Number) day.get("drawdown_6m")).doubleValue();
                    double zscore = ((Number) day.get("zscore")).doubleValue();
                    int buyScore = ((Number) day.get("buy_score")).intValue();
                    String tier = (String) day.get("tier");
                    
                    String tierClass = "tier-" + tier.toLowerCase().replace("_", "-");
                    String scoreClass = buyScore >= 75 ? "score-high" : (buyScore >= 35 ? "score-medium" : "score-low");
                    
                    html.append("<tr>");
                    html.append("<td>").append(date).append("</td>");
                    html.append("<td>$").append(String.format("%.2f", close)).append("</td>");
                    html.append("<td>").append(sma200 != null ? "$" + String.format("%.2f", sma200) : "N/A").append("</td>");
                    html.append("<td>").append(String.format("%.2f%%", drawdown * 100)).append("</td>");
                    html.append("<td>").append(String.format("%.2f", zscore)).append("</td>");
                    html.append("<td class=\"").append(scoreClass).append("\">").append(buyScore).append("</td>");
                    html.append("<td class=\"").append(tierClass).append("\">").append(tier).append("</td>");
                    html.append("</tr>");
                }
                
                html.append("</table>");
                
                // Summary for this ticker
                int avgScore = (int) history.stream()
                    .mapToInt(d -> ((Number) d.get("buy_score")).intValue())
                    .average()
                    .orElse(0.0);
                
                Map<String, Long> tierCounts = new HashMap<>();
                for (Map<String, Object> day : history) {
                    String tier = (String) day.get("tier");
                    tierCounts.put(tier, tierCounts.getOrDefault(tier, 0L) + 1);
                }
                
                html.append("<p><strong>Average Buy Score:</strong> ").append(avgScore).append(" | ");
                html.append("<strong>Signals:</strong> ");
                String[] tierOrder = {"STRONG_BUY", "BUY", "DCA_ONLY", "WAIT"};
                List<String> tierSummary = new ArrayList<>();
                for (String tier : tierOrder) {
                    long count = tierCounts.getOrDefault(tier, 0L);
                    if (count > 0) {
                        tierSummary.add(tier + ": " + count);
                    }
                }
                html.append(String.join(", ", tierSummary));
                html.append("</p>");
            }
            
            html.append("<hr>");
            html.append("<p><em>This is an automated report from ETF Advisor.</em></p>");
            html.append("</body></html>");
            
            // Send via SES
            SendEmailRequest emailRequest = new SendEmailRequest()
                .withSource(config.getEmailFrom())
                .withDestination(new Destination().withToAddresses(config.getEmailTo()))
                .withMessage(new Message()
                    .withSubject(new Content("ETF Advisor — " + runDate))
                    .withBody(new Body().withHtml(new Content(html.toString()))));
            
            sesClient.sendEmail(emailRequest);
            
        } catch (Exception e) {
            context.getLogger().log("Error sending email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}

