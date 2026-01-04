package com.etfadvisor;

import java.util.Map;


public class Main {
    
    public static void main(String[] args) {
        try {
            Map<String, Object> data = YahooFinanceHelper.fetchStockData("VOO");
            printStockData("VOO", data);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        String[] tickers = {"VOO", "VTI", "AAPL"};
        Map<String, Map<String, Object>> results = YahooFinanceHelper.fetchMultipleStocks(tickers);
        
        for (String ticker : tickers) {
            Map<String, Object> data = results.get(ticker);
            if (data.containsKey("error")) {
                System.err.println(ticker + ": ERROR - " + data.get("error"));
            } else {
                printStockData(ticker, data);
            }
            System.out.println();
        }
        
        System.out.println("=".repeat(60));
        System.out.println("Testing complete!");
    }

    private static void printStockData(String ticker, Map<String, Object> data) {
        System.out.println("Ticker: " + ticker);
        if (data.containsKey("price")) {
            System.out.println("  Price: $" + data.get("price"));
        }
        if (data.containsKey("previousClose")) {
            System.out.println("  Previous Close: $" + data.get("previousClose"));
        }
        if (data.containsKey("change")) {
            double change = (Double) data.get("change");
            double changePercent = (Double) data.get("changePercent");
            String sign = change >= 0 ? "+" : "";
            System.out.println("  Change: " + sign + "$" + String.format("%.2f", change) + 
                             " (" + sign + String.format("%.2f", changePercent) + "%)");
        }
        if (data.containsKey("date")) {
            System.out.println("  Date: " + data.get("date"));
        }
    }
}

