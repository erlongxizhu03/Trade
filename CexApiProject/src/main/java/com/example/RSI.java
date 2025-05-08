package com.example;

import java.util.ArrayList;
import java.util.List;

public class RSI {
    private static int PERIOD = 14;

    public RSI(int period) {
        PERIOD = period;
    }

    /**
     * 计算累计涨跌幅、计算平均涨幅与平均跌幅、计算rs(平均涨幅/平均跌幅)、计算rsi (100 - (100 / (1 + RS))
     * @param prices
     * @return
     */
    public Float calculateRSI(List<Float> prices) {
        if (prices.size() < PERIOD) {
            throw new IllegalArgumentException("Not enough data to calculate RSI.");
        }

        float gain = 0f;
        float loss = 0f;

        for (int i = 1; i < PERIOD; i++) {
            float change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gain += change;
            } else {
                loss -= change; // Loss is represented as a positive number
            }
        }

        gain /= PERIOD;
        loss /= PERIOD;

        if (loss == 0) {
            return 100f; // If there's no loss, RSI is 100
        }

        float rs = gain / loss;
        return 100 - (100 / (1 + rs));
    }

    public static void main(String[] args) {
        List<Float> prices = new ArrayList<>();
        // 示例收盘价数据
        prices.add(44.34f);
        prices.add(44.90f);
        prices.add(45.00f);
        prices.add(43.75f);
        prices.add(44.45f);
        prices.add(45.65f);
        prices.add(46.45f);
        prices.add(46.00f);
        prices.add(44.00f);
        prices.add(43.85f);
        prices.add(45.50f);
        prices.add(46.20f);
        prices.add(47.00f);
        prices.add(48.00f); // 14个收盘价样本

        double rsi = new RSI(14).calculateRSI(prices);
        System.out.println("The RSI is: " + rsi);
    }
}