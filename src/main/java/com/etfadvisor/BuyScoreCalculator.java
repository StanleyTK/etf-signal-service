package com.etfadvisor;

public class BuyScoreCalculator {
    
    private final Config config;
    public BuyScoreCalculator(Config config) {
        this.config = config;
    }
    
    /**
     * Pasted from backtest_historical.ipynb
     */
    public BuyScoreResult calculate(IndicatorCalculator.Indicators indicators) {
        double closeToday = indicators.getCloseToday();
        Double sma200 = indicators.getSma200();
        double drawdown6m = indicators.getDrawdown6m();
        double zscore = indicators.getZscore();
        
        double trendScore = (sma200 != null && closeToday > sma200) ? 1.0 : 0.3;
        double dd = Math.abs(Math.min(drawdown6m, 0));
        double drawdownScore = Math.max(0, Math.min(1, dd / config.getDrawdownMax()));
        double z = Math.abs(Math.min(zscore, 0));
        double zscoreScore = Math.max(0, Math.min(1, z / config.getZscoreMax()));
        
        // Combine with weights: 25% trend, 45% drawdown, 30% z-score
        double baseScore = 100 * (0.25 * trendScore + 0.45 * drawdownScore + 0.30 * zscoreScore);
        int buyScore = (int) Math.max(0, Math.min(100, baseScore));
        
        String tier;
        if (buyScore >= config.getTierStrongBuy()) {
            tier = "STRONG_BUY";
        } else if (buyScore >= config.getTierBuy()) {
            tier = "BUY";
        } else if (buyScore >= config.getTierDcaOnly()) {
            tier = "DCA_ONLY";
        } else {
            tier = "WAIT";
        }
        
        return new BuyScoreResult(buyScore, tier, trendScore, drawdownScore, zscoreScore);
    }
    

    public static class BuyScoreResult {
        private final int buyScore;
        private final String tier;
        private final double trendScore;
        private final double drawdownScore;
        private final double zscoreScore;
        
        public BuyScoreResult(int buyScore, String tier, double trendScore, 
                            double drawdownScore, double zscoreScore) {
            this.buyScore = buyScore;
            this.tier = tier;
            this.trendScore = trendScore;
            this.drawdownScore = drawdownScore;
            this.zscoreScore = zscoreScore;
        }
        
        public int getBuyScore() { return buyScore; }
        public String getTier() { return tier; }
        public double getTrendScore() { return trendScore; }
        public double getDrawdownScore() { return drawdownScore; }
        public double getZscoreScore() { return zscoreScore; }
    }
}

