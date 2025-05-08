package com.example;

public class EMA {

    /**
     * 平滑因子
     */
    private float alpha;

    /**
     * 前一时间步的EMA值
     */
    private float previousEMA;

    public EMA(int period) {
        // 计算alpha值
        this.alpha = 2.0f / (period + 1);
        // 初始EMA值设为0
        this.previousEMA = 0;
    }
 
    public float calculateEMA(float currentPrice) {
        if (previousEMA == 0f) {
            previousEMA = currentPrice;
        }else {
            float currentEMA = alpha * currentPrice + (1 - alpha) * previousEMA;
            // 更新前一时间步的EMA值
            previousEMA = currentEMA;
        }
        return previousEMA;
    }
 
    public static void main(String[] args) {
        // 示例：使用9天期的EMA计算价格序列
        EMA ema = new EMA(9);
        // 示例价格序列
        float[] prices = {100, 102, 101, 104, 103, 105, 106, 108, 107, 109};
        for (float price : prices) {
            double emaValue = ema.calculateEMA(price);
            System.out.println("EMA: " + emaValue);
        }
    }
}