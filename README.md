# etf-signal-service

trying to send daily emails to myself if now is a good time to buy S&P500 or not

sample results:

```
{
  "tier_counts": {
    "DCA_ONLY": 2,
    "WAIT": 6
  },
  "results": [
    {
      "ticker": "VOO",
      "close_today": 628.2999877929688,
      "buy_score": 28,
      "tier": "WAIT",
      "sma_200": 577.0126504516602,
      "trend_score": 1,
      "drawdown_score": 0.08584891187593813,
      "zscore_score": 0,
      "zscore": 0.3465170499477881,
      "drawdown_6m": -0.010301869425112575,
      "status": "OK"
    },
    {
      "ticker": "VTI",
      "close_today": 336.30999755859375,
      "buy_score": 28,
      "tier": "WAIT",
      "sma_200": 308.97749908447264,
      "trend_score": 1,
      "drawdown_score": 0.08753107157749296,
      "zscore_score": 0,
      "zscore": 0.33236155203776907,
      "drawdown_6m": -0.010503728589299155,
      "status": "OK"
    },
    {
      "ticker": "MGK",
      "close_today": 410.8599853515625,
      "buy_score": 40,
      "tier": "DCA_ONLY",
      "sma_200": 372.6749501037598,
      "trend_score": 1,
      "drawdown_score": 0.2940905863783076,
      "zscore_score": 0.0727064265151547,
      "zscore": -0.18176606628788675,
      "drawdown_6m": -0.03529087036539691,
      "status": "OK"
    },
    {
      "ticker": "QQQ",
      "close_today": 613.1199951171875,
      "buy_score": 40,
      "tier": "DCA_ONLY",
      "sma_200": 558.4906497192383,
      "trend_score": 1,
      "drawdown_score": 0.29688440419019063,
      "zscore_score": 0.0809080378849028,
      "zscore": -0.20227009471225701,
      "drawdown_6m": -0.035626128502822874,
      "status": "OK"
    },
    {
      "ticker": "VTV",
      "close_today": 192.80999755859375,
      "buy_score": 25,
      "tier": "WAIT",
      "sma_200": 179.25064979553224,
      "trend_score": 1,
      "drawdown_score": 0.018113050864763485,
      "zscore_score": 0,
      "zscore": 0.9796579839265223,
      "drawdown_6m": -0.002173566103771618,
      "status": "OK"
    },
    {
      "ticker": "VB",
      "close_today": 261.5400085449219,
      "buy_score": 31,
      "tier": "WAIT",
      "sma_200": 242.000400390625,
      "trend_score": 1,
      "drawdown_score": 0.1461873164060749,
      "zscore_score": 0,
      "zscore": 0.5487541852661221,
      "drawdown_6m": -0.017542477968728987,
      "status": "OK"
    },
    {
      "ticker": "VXUS",
      "close_today": 76.54000091552734,
      "buy_score": 25,
      "tier": "WAIT",
      "sma_200": 69.97069988250732,
      "trend_score": 1,
      "drawdown_score": 0,
      "zscore_score": 0,
      "zscore": 1.5549307272641069,
      "drawdown_6m": 0,
      "status": "OK"
    },
    {
      "ticker": "VEA",
      "close_today": 63.20000076293945,
      "buy_score": 25,
      "tier": "WAIT",
      "sma_200": 57.512500038146975,
      "trend_score": 1,
      "drawdown_score": 0,
      "zscore_score": 0,
      "zscore": 1.3059528806022225,
      "drawdown_6m": 0,
      "status": "OK"
    }
  ],
  "statusCode": 200,
  "run_date": "2026-01-04"
}
```