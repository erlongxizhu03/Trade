package com.example;

import com.google.gson.*;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @Description TODO
 * @Author erlong.zhou
 * @Date 2025/3/21 14:20
 */
public class Start {

    /**
     * 自己的大脑和计算机共同决策。
     * 1、元组填写价格和数量，一键下限价单（不看自然就有耐心了，有了耐心就超越多数人）
     * 2、成交后自动计算止盈止损（根据入场价即可，止损在全部限价单完成后再计算，如果小仓位的话不计算止损反正爆不了），不设置，达到价格自动成交
     * 3、
     */

    //变化率波动大提醒
    //对照时间范围，休眠10秒的倍数（300s,5分钟变化率），15分钟的变化率。
    private static int timeLen1 = 300;
    //对照时间范围，秒，1小时变化率。超过1小时的删除。
    private static int timeLen2 = 1200;
    //触发提示的变化率（0.01，波动1%会提示）, 可用前边*根k线 开盘收盘差的均值 代替，过小的波动率视为噪声不用关注
    private static float triggerRate = 0.045F;
    //触发提示的变化率, 可用前边*根k线 开盘收盘差的均值 代替
    private static float triggerRate2 = 0.01204F;
    static Map<Integer, Float> map = new HashMap<>();
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //是否发邮件控制
    static boolean isShowFram = true;
    //是否发邮件控制
    static boolean isSendEmail = false;
    static boolean outBwtween = false;
    public static void main(String[] args) throws Exception {
        //使用 Runtime.getRuntime().addShutdownHook 添加一个关闭钩子。这不会阻止JVM退出，但可以在JVM开始关闭时执行代码。
        //main方法结束项目进程还在，需要有守护线程或者定时任务。
        readStr("hello world!");
//        showMsg("hello world!");
//        exportHistoryData();
        Map<Long, String> hisDataMap = KeiBarUtil.analysisHisDataToMap("D:/file.txt");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook is running.");
            // 执行清理代码
        }));
        float ema10 = KeiBarUtil.getEma(hisDataMap, "2024-05-12 08:00:00", 10, "1d");
        float ema20 = KeiBarUtil.getEma(hisDataMap, "2024-05-12 08:00:00", 20, "1d");
        float rsi9 = KeiBarUtil.getRSI(hisDataMap, "2025-05-02 08:00:00", 9, "1d");
        float rsi14 = KeiBarUtil.getRSI(hisDataMap, "2025-05-02 08:00:00", 14, "1d");

        float ema4H10 = KeiBarUtil.getEma(hisDataMap, "2025-05-02 08:00:00", 10, "4H");
        float ema4H20 = KeiBarUtil.getEma(hisDataMap, "2025-05-02 08:00:00", 20, "4H");
        float rsi4H9 = KeiBarUtil.getRSI(hisDataMap, "2025-05-02 08:00:00", 9, "4H");
        float rsi4H14 = KeiBarUtil.getRSI(hisDataMap, "2025-05-02 08:00:00", 14, "4H");
//        if (true) {
//            return;
//        }
        Date parse = KeiBarUtil.sdf.parse("2024-05-05 08:00:00");
        long time = parse.getTime();
        String s = hisDataMap.get(time);
        float hPrice = 0;//计算高价，根据当前close
        float lPrice = 0;//计算低价，根据当前close
        float stopLossPrice = 0;//计算止损价，加格加满时，当止损价>0时,优先判断止损价
        float baoCangPrice = 0;//计算爆仓价，有仓位时。
        int totalJettonCount = 0;//总筹码，总筹码正负对应得出，大于或小于止损价，执行止损
        Map<Float, Integer> entryPricesMap = new HashMap<>();
        while (s != null) {
            JsonObject jsonObj = KeiBarUtil.getJsonObj(s);
            float close = KeiBarUtil.getValByKey(jsonObj, "close");
            if (totalJettonCount == 0 && time % KeiBarUtil.DAY_MILLISECOND==0) {
                float open = KeiBarUtil.getValByKey(jsonObj, "open");
                //空仓节点
                hPrice = open + 600;
                lPrice = open - 600;
            }
            if (stopLossPrice > 0) {
                //有止损价，减仓
                if (close >= hPrice) {
                    if (totalJettonCount >0) {
                        //止盈 -totalJettonCount
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，止盈筹码："+totalJettonCount+",平仓价格："+close+",入场均价："+avgEntryPrice);
                        totalJettonCount = 0;
                        entryPricesMap.clear();
                        //空仓节点
                        hPrice = close + 600;
                        lPrice = close - 1000;
                        stopLossPrice = 0;
                    }else if (totalJettonCount <0) {
                        //止损
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，止损筹码："+totalJettonCount+",平仓价格："+close+",入场均价："+avgEntryPrice);
                        totalJettonCount = 0;
                        entryPricesMap.clear();
                        //空仓节点
                        hPrice = close + 600;
                        lPrice = close - 1000;
                        stopLossPrice = 0;
                    }
                }
                if (close <= lPrice){
                    if (totalJettonCount <0) {
                        //止盈 -totalJettonCount
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，止盈筹码："+totalJettonCount+",平仓价格："+close+",入场均价："+avgEntryPrice);
                        totalJettonCount = 0;
                        entryPricesMap.clear();
                        //空仓节点
                        hPrice = close + 1000;
                        lPrice = close - 600;
                        stopLossPrice = 0;
                    }else if (totalJettonCount >0) {
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，止损筹码："+totalJettonCount+",平仓价格："+close+",入场均价："+avgEntryPrice);
                        totalJettonCount = 0;
                        entryPricesMap.clear();
                        //空仓节点
                        hPrice = close + 1000;
                        lPrice = close - 600;
                        stopLossPrice = 0;
                    }
                }
            }else {
                //没有止损价，加格或盈利
                if (close >= hPrice) {
                    if (totalJettonCount >0) {
                        //止盈 -totalJettonCount
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，止盈筹码："+totalJettonCount+",平仓价格："+close+",入场均价："+avgEntryPrice);
                        totalJettonCount = 0;
                        entryPricesMap.clear();
                        //空仓节点
                        hPrice = close + 600;
                        lPrice = close - 600;
                    }else if (totalJettonCount <=0) {
                        //操作加格或止损，并计算入场价、爆仓价。
                        int addJettonCount = getAddJettonCount(totalJettonCount, 1);

                        totalJettonCount = totalJettonCount + addJettonCount;
                        if (totalJettonCount == 10) {
                            //计算止损
                            stopLossPrice = close - 600;
                        }else if (totalJettonCount == -10){
                            stopLossPrice = close + 600;;
                        }
                        entryPricesMap.put(close, addJettonCount);
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，仓位操作："+addJettonCount+",总筹码："+totalJettonCount+",入场均价："+avgEntryPrice);
                        //操作后修改节点，加格价、止盈价、止损价
                        if (totalJettonCount > 0) {
                            //longOrder
                            hPrice = avgEntryPrice + 1000;//止盈价
                            lPrice = close - 600;//加格价
                        }else {
                            //shortOrder
                            hPrice = close + 600;//加格价
                            lPrice = avgEntryPrice - 1000;//止盈价
                        }
//                        int addOrWinOrLose = isAddOrWinOrLose(avgEntryPrice, stopLossPrice, totalJettonCount, 1);
//                        hPrice = close + 1000;
//                        lPrice = close - 1000;
                    }
                }
                if (close <= lPrice){
                    if (totalJettonCount <0) {
                        //止盈 -totalJettonCount
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，止盈筹码："+totalJettonCount+",平仓价格："+close+",入场均价："+avgEntryPrice);
                        totalJettonCount = 0;
                        entryPricesMap.clear();
                        //空仓节点
                        hPrice = close + 600;
                        lPrice = close - 600;
                    }else if (totalJettonCount >=0) {
                        //操作加格或止损，并计算入场价、爆仓价。
                        int addJettonCount = getAddJettonCount(totalJettonCount, 1);

                        totalJettonCount = totalJettonCount + addJettonCount;
                        if (totalJettonCount == 10) {
                            //计算止损
                            stopLossPrice = close - 600;
                        }else if (totalJettonCount == -10){
                            stopLossPrice = close + 600;;
                        }
                        entryPricesMap.put(close, addJettonCount);
                        float avgEntryPrice = getAvgEntryPrice(entryPricesMap);
                        System.out.println(KeiBarUtil.sdf.format(time)+"，仓位操作："+addJettonCount+",总筹码："+totalJettonCount+",入场均价："+avgEntryPrice);
                        //操作后修改节点，加格价、止盈价、止损价
                        if (totalJettonCount > 0) {
                            //longOrder
                            hPrice = avgEntryPrice + 1000;//止盈价
                            lPrice = close - 600;//加格价
                        }else {
                            //shortOrder
                            hPrice = close + 600;//加格价
                            lPrice = avgEntryPrice - 1000;//止盈价
                        }

                    }
                }
            }
            time += KeiBarUtil.MINUTE_MILLISECOND;
            s = hisDataMap.get(time);
        }
        Font font = new Font("微软雅黑", Font.PLAIN, 12);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);
        UIManager.put("OptionPane.inputFont", font);
        //必要条件(管住手，交易是等待的艺术，尽量不开单)：
        //1.没达指标不开单（否则容易被咬住）【指标2-3个：RSI/MACD/5分钟内波动率/斐波那契布林带】
        //2.第一手仓位控制==本金大小（永远不要全仓all in）
        //3.开第一单后，第二单至少间隔的点数（不低于1000点，降低交易频率，毛刺大一点）
        //4.没走出形态时该认错认错，可以亏小钱
        //5.盈利300点就走（剥头皮，合约不格局，盈亏比5：3）
        //平仓后+-1000点不开仓。
        int i = 0;
        NumberFormat percentInstance = NumberFormat.getPercentInstance();
        percentInstance.setMinimumFractionDigits(2);

        while (true) {
            i++;
//            String s = LNFiPostGetPublic();
//            System.out.println(s);
            try {
                int startTime = (int) (System.currentTimeMillis()/1000);
                String response = lNFiGet();
                if ("".equals(response)) {
                    continue;
                }
//              System.out.println(response.toString());
                int endTime = (int) (System.currentTimeMillis()/1000);
                System.out.println("耗时"+(endTime - startTime)+"秒");
                JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

                float price = jsonObject.get("data").getAsJsonArray().get(0).getAsJsonObject().get("index_price").getAsFloat();
                float tag_price = jsonObject.get("data").getAsJsonArray().get(0).getAsJsonObject().get("tag_price").getAsFloat();

                String ctime = formatter.format(new Date());

                System.out.printf(i +"、"+ctime + "， price=====%s, tag_price=====%s\n", String.format("%.2f", price), String.format("%.2f", tag_price));
                int systemTime = (int) (System.currentTimeMillis() / 1000);
                int key = systemTime - systemTime % 10;
                map.put(key, price);
//                System.out.println(ctime + ",map.size == "+ map.size());
                int preKey1 = key - timeLen1;
                Float preI = map.get(preKey1);
                if (preI == null) {
                    preI = map.get(preKey1 - 10);
                }

                if (preI != null) {
                    float changeRate = (price - preI) / preI;
                    if (Math.abs(changeRate) > Math.abs(triggerRate)) {
                        System.out.println(ctime + "， 剧烈变化：" + timeLen1 / 60 + "分钟内涨跌：" + percentInstance.format(changeRate));
                        String type = changeRate > 0 ? "空" : "多";
                        readStr("BTC剧烈波动，可以" + type + "，当前价格：" + String.format("%.2f", tag_price) + "分钟内涨跌：" + percentInstance.format(changeRate));
                        showMsg("BTC剧烈波动，可以" + type + "，当前价格：" + String.format("%.2f", tag_price) + "分钟内涨跌：" + percentInstance.format(changeRate));
                    } else {
                        System.out.println(ctime + " " + timeLen1 / 60 + "分钟内涨跌：" + percentInstance.format(changeRate));
                    }
//                    System.out.println("5分钟前的价：" + preKey1 + ", " + preI + ",变化率：" + changeRate);
                } else {
                    System.out.println("preI==" + preI + ",key==" + key + ",preKey1==" + preKey1 + ",dicLen:" + map.size());
                }
                if (i % 1000 == 0) {
                    //超过timeLen2的删掉
                    int deledKey2 = key-timeLen2;
                    for (Integer keyItem : map.keySet()) {
                        if (keyItem < deledKey2){
                            map.remove(keyItem); //
                        }
                    }
                }
                //急跌买入
                //CD时间不开单
                //抢收盘价
                float lowerPrice = 94100f;
                float highPrice = 95464;
                if (!outBwtween && tag_price < lowerPrice) {
                    System.out.println("牌来了，可以开多");
                    readStr("牌来了，可以开long：" + String.format("%.2f", tag_price));
                    showMsg("牌来了，可以开long：" + String.format("%.2f", tag_price));
                    outBwtween = true;
                }

                if (!outBwtween && tag_price > highPrice) {
                    System.out.println("牌来了，可以平多");
                    readStr("牌来了，可以开short：" + String.format("%.2f", tag_price));
                    showMsg("牌来了，可以开short：" + String.format("%.2f", tag_price));
                    outBwtween = true;
                }
                if (tag_price >= lowerPrice && tag_price <= highPrice){
                    outBwtween = false;
                }
                Thread.sleep(10000);
                System.out.println("新一轮开始" + i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 平均入场价
     * @param inPriceMap
     */
    private static float getAvgEntryPrice(Map<Float, Integer> inPriceMap) {
        float totalCost = 0;
        float totalNum = 0;
        for (Map.Entry<Float, Integer> entry : inPriceMap.entrySet()) {
            Float price = entry.getKey();
            Integer num = Math.abs(entry.getValue());
            totalCost += price* num;
            totalNum += num;
        }
        return totalCost/totalNum;
    }

    /**
     * 判断是加格1、还是盈利2，或者止损走人-1。
     * @param avgEntryPrice
     * @param totalJettonCount
     * @param isHig
     * @return 加格1、还是盈利2，或者止损走人-1。
     */
    public static int isAddOrWinOrLose(float avgEntryPrice, float stopLossPrice, int totalJettonCount, int isHig){
        //止损也要勾头时候
        if (stopLossPrice >0) {

        }else {
            if (isHig > 0) {

            }else if(isHig < 0){

            }
        }
        return 0;
    }

    /**
     * 加格
     * @param totalJettonCount
     * @param isHig
     * @return
     */
    public static int getAddJettonCount(int totalJettonCount, int isHig){
        int res = 0;
        switch (totalJettonCount) {
            case 0:
                if (isHig == 1){
                    res = -1;
                }else {
                    res = 1;
                }
                break;
            case 1:
                //1个多单
                if (isHig == 1){
                    res = -totalJettonCount;//平完
                }else {
                    res = 2;//第二手
                }
                break;
            case 3:
                //3个多单
                if (isHig == 1){
                    res = -totalJettonCount;//平完
                }else {
                    res = 3;//第三手
                }
                break;
            case 6:
                //6个多单
                if (isHig == 1){
                    res = -totalJettonCount;//平完
                }else {
                    res = 4;//第四手
                }
                break;
            case -1:
                //1个空单
                if (isHig == 1){
                    res = -2;//第二手
                }else {
                    res = -totalJettonCount;//平完
                }
                break;
            case -3:
                //1个空单
                if (isHig == 1){
                    res = -3;//第三手
                }else {
                    res = -totalJettonCount;//平完
                }
                break;
            case -6:
                //1个空单
                if (isHig == 1){
                    res = -4;//第四手
                }else {
                    res = -totalJettonCount;//平完
                }
                break;
            default:
                break;
        }

        return res;
    }

    /**
     * 解析历史数据
     */
    private static void analysisHisDataToDB(String filePath) {

        String url = "jdbc:mysql://127.0.0.1:3306/trade?allowMultiQueries=true&amp&characterEncoding=utf8"; // 注意替换your_database_name为你的数据库名
        String username = "root"; // 替换为你的数据库用户名
        String password = "123456"; // 替换为你的数据库密码
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 建立连接
            conn = DriverManager.getConnection(url, username, password);
            System.out.println("成功连接到数据库！");

            // 使用连接...（例如，执行查询等）
            String sql = "INSERT INTO `history_info`(`time`, `tsDate`, `level`, `open`, `hig`, `low`, `close`) VALUES (?, ?, ?, ?, ?, ?, ?) on DUPLICATE KEY update `open` = ?, `hig` = ?, `low` = ?, `close` = ?";
            pstmt = conn.prepareStatement(sql);

            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                // 处理每一行，例如打印出来
//                System.out.println(line);
                // 在这里可以对line进行进一步处理
                Matcher matcher = KeiBarUtil.pattern.matcher(line);
                boolean found = matcher.find();
                int i = 0;
                long ts = 0;
                float open = 0;
                float hig = 0;
                float low = 0;
                float close = 0;
                while (found) {
                    String group = matcher.group();
                    switch (i) {
                        case 0:
                            ts = Long.parseLong(group);
                            break;
                        case 1:
                            open = Float.parseFloat(group);
                            break;
                        case 2:
                            hig = Float.parseFloat(group);
                            break;
                        case 3:
                            low = Float.parseFloat(group);
                            break;
                        case 4:
                            close = Float.parseFloat(group);
                            break;
                        default:
                            break;
                    }
//                    System.out.println("找到匹配: " + group); // 输出匹配的部分
                    found = matcher.find(); // 继续查找下一个匹配项
                    i++;
                }

                String level = "1m";
                if (ts % (3600 * 24 *1000)==0) {
                    level = "1d";
                } else if (ts % (3600 * 4 * 1000) == 0) {
                    level = "4H";
                } else if (ts % (3600 * 1 * 1000) == 0) {
                    level = "1H";
                } else if (ts % (1800 * 1000) == 0) {
                    level = "30m";
                } else if (ts % (900 * 1000) == 0) {
                    level = "15m";
                } else if (ts % (300 * 1000) == 0) {
                    level = "5m";
                }

                List<Object> params = new ArrayList<>();
                //insert参数
                params.add(ts);
                params.add(KeiBarUtil.sdf.format(ts));
                params.add(level);
                params.add(open);
                params.add(hig);
                params.add(low);
                params.add(close);
                //update参数
                params.add(open);
                params.add(hig);
                params.add(low);
                params.add(close);
                for (int p = 0; p < params.size(); p++) {
                    pstmt.setObject(p + 1, params.get(p));
                }
                pstmt.addBatch();//批量执行
            }
            int[] ints = pstmt.executeBatch();//批量入库
            pstmt.clearBatch();
//                int result = pstmt.executeUpdate();//单个入库
            System.out.println("入库结果"+ints);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭连接
            try {
                pstmt.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 导出历史数据到文件
     */
    private static void exportHistoryData() {
        long beforeTime = 0;
        long afterTime = 0;
        String limit = "100";
        String bar = "1m";//15m/30m/1H/2H/4H
        int pages = 5256*4;//pages个100分钟，一年是5256个100分钟
        try {
            beforeTime = KeiBarUtil.sdf.parse("2020-04-10 08:00:00").getTime();
            afterTime = KeiBarUtil.sdf.parse("2024-05-04 08:00:01").getTime();
            System.out.println(beforeTime+","+afterTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String filePath = "D:/file2020-2024.txt";

        try (PrintStream out = new PrintStream(new FileOutputStream(filePath))) {
            // 可以继续添加更多的println调用
            for (long s = pages; s > 0; s--) {
                long afterTimeItem = afterTime - s *  60 * 100*1000;
                System.out.println(KeiBarUtil.sdf.format(new Date(afterTimeItem)));
//                out.println(KeiBarUtil.sdf.format(new Date(afterTimeItem)));
                String historyData = getHistoryData(String.valueOf(beforeTime), String.valueOf(afterTimeItem), limit, bar);//600000是1m100条 即100分钟,一页数据对应分钟查询一次
//            System.out.println(historyData);
                JsonArray data = null;
                try {
                    JsonObject jsonHisData = JsonParser.parseString(historyData).getAsJsonObject();
                    data = jsonHisData.get("data").getAsJsonArray();
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.sleep(1000);
                    System.out.println("异常afterTimeItem："+afterTimeItem);
//                    out.println("异常afterTimeItem："+afterTimeItem);
                    s++;
                    continue;
                }
                for (int i = data.size()-1; i >=0; i--) {
                    JsonElement datum = data.get(i);
                    String ts = datum.getAsJsonArray().get(0).getAsString();
                    String open = datum.getAsJsonArray().get(1).getAsString();
                    String hig = datum.getAsJsonArray().get(2).getAsString();
                    String low = datum.getAsJsonArray().get(3).getAsString();
                    String close = datum.getAsJsonArray().get(4).getAsString();
                    String isConfirm = datum.getAsJsonArray().get(5).getAsString();
//                    System.out.printf(data.size()*(pages-s)+(data.size()-i)+":"+ts+", "+KeiBarUtil.sdf.format(Long.parseLong(ts))+"：open:%s,hig:%s,low:%s,close:%s,isConfirm:%s",open,hig,low,close, isConfirm+"\n");
                    out.println(data.size()*(pages-s)+(data.size()-i)+":"+ts+", "+KeiBarUtil.sdf.format(Long.parseLong(ts))+"：open:"+open+",hig:"+hig+",low:"+low+",close:"+close+",isConfirm:"+isConfirm);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getHistoryData(String before, String after, String limit,String bar){
        //公共数据：获取指数历史K线数据

        BufferedReader br = null;
        try {

            //&after=1744243200000&before=1744416000000&bar=5m&limit=100  bar默认1m
            URL url = new URL("https://www.okx.com/api/v5/market/history-index-candles?instId=BTC-USD&before="+before+"&after="+after+"&limit="+limit+"&bar="+bar);
//            URL url = new URL("https://test-futures-api.ln.exchange/napi/common/public_all_index_tag_price");
//            System.out.println("开始请求");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            //get请求，参数直接在url后
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

//            String urlParameters = "instId=BTC-USD&after=1744243200000&before=1744416000000&bar=5m&limit=100";
//            try (OutputStream os = con.getOutputStream()) {
//                byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
//                os.write(input, 0, input.length);
//            }
            con.setConnectTimeout(5000); // 设置连接超时时间为5000毫秒
            con.setReadTimeout(5000); // 设置读取超时时间为5000毫秒
            con.connect(); // 尝试连接
            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Response Code : " + responseCode);
                return "";
            }
            System.out.println("Response Code : " + responseCode);
            br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
//            System.out.println("请求结束");
            br.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

        /** 字符串文本阅读
         * @param str 要读的文字字符串
         */
    public static void readStr(String str){
        try {
            //写死，jar包所在服务器绝对路径也得有对应py
            String[] command = {"python", "src/main/python/sayText.py", str};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            if (isSendEmail) {
                String[] commandEmail = {"python", "src/main/python/sendEmail.py", str};
                Runtime.getRuntime().exec(commandEmail);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showMsg(String msg) {
        if (!isShowFram) {
            return;
        }
//        Font font = new Font("微软雅黑", Font.PLAIN, 12);
//        UIManager.put("OptionPane.messageFont", font);
        // 设置定时器来自动关闭对话框
        Timer timer = new Timer(4000, new ActionListener() { // 3000毫秒后执行
            @Override
            public void actionPerformed(ActionEvent e) {
                // 关闭弹出框
                JOptionPane.getRootFrame().dispose();
            }
        });
        timer.setRepeats(false); // 只执行一次
        timer.start();
        JOptionPane.showMessageDialog(null, msg, "提示", JOptionPane.INFORMATION_MESSAGE);
//        JDialog dialog = new JDialog();
//        JOptionPane pane = new JOptionPane(msg);
//        pane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
//        dialog.setContentPane(pane);
//        dialog.setModal(true);
//        dialog.setTitle("提示");
//        dialog.setAlwaysOnTop(true); // 确保弹窗始终在最顶层
//        dialog.pack();
//        dialog.setLocationRelativeTo(null); // 居中显示
//        dialog.setVisible(true);
    }


    public static String lNFiGet() {
        BufferedReader br = null;
        try {
            URL url = new URL("https://test-futures-api.ln.exchange/napi/market/public_all_index_tag_price?");
//            URL url = new URL("https://test-futures-api.ln.exchange/napi/common/public_all_index_tag_price");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            //get请求，参数直接在url后
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

//            String urlParameters = "param1=" + URLEncoder.encode("value1", "UTF-8") + "&param2=" + URLEncoder.encode("value2", "UTF-8");
//            try (OutputStream os = con.getOutputStream()) {
//                byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
//                os.write(input, 0, input.length);
//            }
            con.setConnectTimeout(5000); // 设置连接超时时间为5000毫秒，默认超时时间是无限的
            con.setReadTimeout(5000); // 设置读取超时时间为5000毫秒，默认超时时间是无限的
            con.connect(); // 尝试连接
            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Response Code : " + responseCode);
                return "";
            }
            br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String LNFiPostGetPublic() {
        try {
            URL url = new URL("https://test-futures-api.ln.exchange/napi/common/public_info");
//            URL url = new URL("https://test-futures-api.ln.exchange/napi/common/public_info");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            //get请求，参数直接在url后
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

//            String urlParameters = "param1=" + URLEncoder.encode("value1", "UTF-8") + "&param2=" + URLEncoder.encode("value2", "UTF-8");
//            try (OutputStream os = con.getOutputStream()) {
//                byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
//                os.write(input, 0, input.length);
//            }

            int responseCode = con.getResponseCode();
//            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    public static String LNFiPostDepth() {
        try {
            URL url = new URL("https://test-futures-api.ln.exchange/open/v1/depth");

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //post添加请求参数
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true); // 允许写入数据到服务器

            // 获取输出流，用于写入JSON数据
            try (OutputStream os = con.getOutputStream()) {
                // JSON数据字符串
                String jsonInputString = "{\"contractName\":\"TREAT-BTC-USDT\",\"limit\":\"100\"}";
                // 将JSON字符串写入输出流
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void LNFiPostCreatUser() {
        try {
            URL url = new URL("https://test-futures-api.ln.exchange/napi/user/create_user");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //post添加请求参数
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true); // 允许写入数据到服务器

            // 获取输出流，用于写入JSON数据
            try (OutputStream os = con.getOutputStream()) {
                // JSON数据字符串
                String jsonInputString = "{\"nostrAddress\":\"npub13e289kz5gyr8zt8u5prjf429753nls7qyxckpfsz4z47u2fh6mlqsvveyq\"," +
                        "\"broker\":1000," +
                        "\"ethAddress\":\"0x77835b816de80349b94cabdb67fcc940bc08bf1d\"," +
                        "\"time\":\"" + System.currentTimeMillis() + "\"}";
                // 将JSON字符串写入输出流
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void LNFiPostOrder() {
        try {
            URL url = new URL("https://test-futures-api.ln.exchange/napi/order/order_create");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //post添加请求参数
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true); // 允许写入数据到服务器

            // 获取输出流，用于写入JSON数据
            try (OutputStream os = con.getOutputStream()) {
                // JSON数据字符串
                String jsonInputString = "{'side':'buy'," +
                        "'open':'OPEN'," +
                        "'isConditionOrder':false," +
                        "'triggerPrice':0," +
                        "'kind':10," +
                        "'source':1," +
                        "'type':1," +
                        "'triggerType':1," +
                        "'expiredTime':30," +
                        "'volume':1," +
                        "'price':60000," +
                        "'ctime':1710843099430," +
                        "'contractName':'E-BTC-USDT'," +
                        "'timeInForce':2}";
                // 将JSON字符串写入输出流
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
