package com.etfadvisor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;


public class ETFAdvisorHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        Map<String, Object> response = new HashMap<>();
        try {
            String ticker = event.containsKey("ticker") 
                ? (String) event.get("ticker") 
                : "VOO";
            
            context.getLogger().log("Fetching data for ticker: " + ticker);
            
            Map<String, Object> stockData = YahooFinanceHelper.fetchStockData(ticker);
            
            response.put("statusCode", 200);
            response.put("ticker", ticker);
            response.put("data", stockData);
            response.put("message", "Successfully fetched stock data");
            
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            response.put("statusCode", 500);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
}
