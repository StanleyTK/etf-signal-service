package com.etfadvisor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for fetching stock data from Yahoo Finance.
 * Can be used by both Lambda handler and local testing.
 */
public class YahooFinanceHelper {
    
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Fetches historical daily price data from Yahoo Finance API.
     * @param ticker Stock ticker symbol (e.g., "VOO", "AAPL")
     * @param daysNeeded Minimum number of trading days needed (default 220)
     * @param endDate End date for data (null = today)
     * @return List of daily close prices (oldest first, most recent last)
     */
    public static java.util.List<Double> fetchHistoricalData(String ticker, int daysNeeded, LocalDate endDate) 
            throws IOException, InterruptedException {
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        // Fetch enough data (add buffer for weekends/holidays)
        int periodDays = (int) (daysNeeded * 1.5);
        LocalDate startDate = endDate.minusDays(periodDays);
        
        // Convert to Unix timestamps (seconds)
        long period1 = startDate.toEpochDay() * 86400L;
        long period2 = (endDate.toEpochDay() + 1) * 86400L; // Add one day to include end date
        
        String url = String.format(
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d&includePrePost=false",
            ticker, period1, period2
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .build();
        
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (httpResponse.statusCode() != 200) {
            throw new IOException("Failed to fetch data for " + ticker + ": HTTP " + httpResponse.statusCode() + 
                " - " + httpResponse.body());
        }
        
        String responseBody = httpResponse.body();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new IOException("Empty response from Yahoo Finance for " + ticker);
        }
        
        // Parse JSON response to extract close prices
        return parseHistoricalResponse(responseBody, endDate);
    }
    
    /**
     * Parses Yahoo Finance historical data response to extract close prices using Jackson.
     */
    private static List<Double> parseHistoricalResponse(String responseBody, LocalDate endDate) throws IOException {
        List<Double> closes = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode chartNode = rootNode.path("chart");
            JsonNode resultNode = chartNode.path("result");
            
            if (!resultNode.isArray() || resultNode.size() == 0) {
                throw new IOException("No result data in Yahoo Finance response");
            }
            
            JsonNode firstResult = resultNode.get(0);
            JsonNode indicators = firstResult.path("indicators");
            JsonNode quote = indicators.path("quote");
            
            if (!quote.isArray() || quote.size() == 0) {
                throw new IOException("No quote data in Yahoo Finance response");
            }
            
            JsonNode timestampsNode = firstResult.path("timestamp");
            JsonNode closesNode = quote.get(0).path("close");
            
            if (!timestampsNode.isArray() || !closesNode.isArray()) {
                throw new IOException("Invalid data format: timestamps or closes not arrays");
            }
            
            // Match timestamps with closes and filter by endDate
            int minLength = Math.min(timestampsNode.size(), closesNode.size());
            for (int i = 0; i < minLength; i++) {
                JsonNode timestampNode = timestampsNode.get(i);
                JsonNode closeNode = closesNode.get(i);
                
                if (timestampNode.isNull() || closeNode.isNull()) {
                    continue;
                }
                
                try {
                    long timestamp = timestampNode.asLong();
                    LocalDate date = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                    
                    // Only include data up to endDate
                    if (!date.isAfter(endDate)) {
                        double close = closeNode.asDouble();
                        if (close > 0 && !Double.isNaN(close) && !Double.isInfinite(close)) {
                            closes.add(close);
                        }
                    }
                } catch (Exception e) {
                    // Skip invalid entries
                    continue;
                }
            }
            
            if (closes.isEmpty()) {
                throw new IOException("No valid price data found in response");
            }
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IOException("Failed to parse JSON response: " + e.getMessage() + 
                " (Response length: " + (responseBody != null ? responseBody.length() : 0) + ")", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to parse historical data: " + e.getMessage(), e);
        }
        
        return closes;
    }
    
    /**
     * Fetches today's stock data from Yahoo Finance API
     * @param ticker Stock ticker symbol (e.g., "VOO", "AAPL")
     * @return Map containing stock data (price, change, etc.)
     */
    public static Map<String, Object> fetchStockData(String ticker) throws IOException, InterruptedException {
        // Yahoo Finance API endpoint for quote data
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker + "?interval=1d&range=1d";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .build();
        
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (httpResponse.statusCode() != 200) {
            throw new IOException("Failed to fetch data: HTTP " + httpResponse.statusCode());
        }
        
        // Parse JSON response
        String responseBody = httpResponse.body();
        Map<String, Object> stockData = parseYahooFinanceResponse(responseBody);
        stockData.put("date", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        
        return stockData;
    }
    
    /**
     * Parses Yahoo Finance API response to extract stock data
     * @param responseBody JSON response string from Yahoo Finance
     * @return Map containing parsed stock data
     */
    private static Map<String, Object> parseYahooFinanceResponse(String responseBody) {
        Map<String, Object> stockData = new HashMap<>();
        
        try {
            // Find regularMarketPrice in the JSON
            int priceIndex = responseBody.indexOf("\"regularMarketPrice\":");
            if (priceIndex != -1) {
                int start = responseBody.indexOf(":", priceIndex) + 1;
                int end = responseBody.indexOf(",", start);
                if (end == -1) end = responseBody.indexOf("}", start);
                String priceStr = responseBody.substring(start, end).trim();
                double price = Double.parseDouble(priceStr);
                stockData.put("price", price);
            }
            
            // Find previousClose
            int prevCloseIndex = responseBody.indexOf("\"previousClose\":");
            if (prevCloseIndex != -1) {
                int start = responseBody.indexOf(":", prevCloseIndex) + 1;
                int end = responseBody.indexOf(",", start);
                if (end == -1) end = responseBody.indexOf("}", start);
                String prevCloseStr = responseBody.substring(start, end).trim();
                double prevClose = Double.parseDouble(prevCloseStr);
                stockData.put("previousClose", prevClose);
                
                // Calculate change
                if (stockData.containsKey("price")) {
                    double price = (Double) stockData.get("price");
                    double change = price - prevClose;
                    double changePercent = (change / prevClose) * 100;
                    stockData.put("change", change);
                    stockData.put("changePercent", changePercent);
                }
            }
            
        } catch (Exception e) {
            // If parsing fails, return raw response
            stockData.put("rawResponse", responseBody);
            stockData.put("parseError", e.getMessage());
        }
        
        return stockData;
    }
    
    /**
     * Fetches stock data for multiple tickers
     * @param tickers Array of ticker symbols
     * @return Map with ticker as key and stock data as value
     */
    public static Map<String, Map<String, Object>> fetchMultipleStocks(String[] tickers) {
        Map<String, Map<String, Object>> results = new HashMap<>();
        
        for (String ticker : tickers) {
            try {
                Map<String, Object> data = fetchStockData(ticker);
                results.put(ticker, data);
            } catch (Exception e) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", e.getMessage());
                results.put(ticker, errorData);
            }
        }
        
        return results;
    }
}

