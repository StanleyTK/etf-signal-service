package com.etfadvisor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for fetching stock data from Yahoo Finance.
 * Can be used by both Lambda handler and local testing.
 */
public class YahooFinanceHelper {
    
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
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

