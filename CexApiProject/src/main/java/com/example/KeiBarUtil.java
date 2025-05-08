package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description TODO
 * @Author erlong.zhou
 * @Date 2025/5/7 18:12
 */
public class KeiBarUtil {

    final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final static Pattern pattern = Pattern.compile("(?!:)(\\d|[.])+(?=,)");


    /**
     * 1分钟的毫秒数
     */
    public static final long MINUTE_MILLISECOND = 60000;

    /**
     * 1小时的毫秒数
     */
    public static final long HOUR_MILLISECOND = 60 * MINUTE_MILLISECOND;

    /**
     * 1天的毫秒数
     */
    public static final long DAY_MILLISECOND = 24 * HOUR_MILLISECOND;

    /**
     * 解析历史数据到Map
     */
    public static Map<Long, String> analysisHisDataToMap(String filePath) {
        Map<Long, String> map = new HashMap<>(540000);
        try {
            Gson gson = new Gson();
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                // 处理每一行，例如打印出来
                System.out.println(line);
                // 在这里可以对line进行进一步处理
                Matcher matcher = pattern.matcher(line);
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
                map.put(ts, "{'open':"+open+",'hig':"+hig+",'low':"+low+",'close':"+close+"}");
                //解析成对象太慢
//                KBarData kBarData = gson.fromJson("{'open':"+open+",'hig':"+hig+",'low':"+low+",'close':"+close+"}", KBarData.class);
//                map.put(ts, kBarData);
            }
            System.out.println(map.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 获取ts对应的keibar
     * @param map
     * @param ts
     * @param barType
     * @return
     */
    private static String getKeiBarInfoJson(Map<Long, String> map, long ts, String barType) {
        String json = map.get(ts);
        if ("1m".equals(barType)) {
            return json;
        }else {
            long timeLenByBarType = getTimeLenByBarType(barType);
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            Float open = getValByKey(jsonObject, "open");
            Float hig = getValByKey(jsonObject, "hig");
            Float low = getValByKey(jsonObject, "low");
            Float close = getValByKey(jsonObject, "close");
            for (long i = 0; i < timeLenByBarType / MINUTE_MILLISECOND-1; i++) {
                ts += MINUTE_MILLISECOND;
                json = map.get(ts);
                if (json != null) {
                    JsonObject jsonObject1 = JsonParser.parseString(json).getAsJsonObject();
                    Float hig1 = getValByKey(jsonObject1, "hig");
                    Float low1 = getValByKey(jsonObject1, "low");
                    Float close1 = getValByKey(jsonObject1, "close");
                    hig = hig1 > hig ? hig1 : hig;
                    low = low1 < low ? low1 : low;
                    close = close1;
                }
            }
            return "{'open':"+open+",'hig':"+hig+",'low':"+low+",'close':"+close+"}";
        }
    }

    private static String getBarType(long ts) {
        String level = "1m";
        if (ts % (DAY_MILLISECOND)==0) {
            level = "1d";
        } else if (ts % (4 * HOUR_MILLISECOND) == 0) {
            level = "4H";
        } else if (ts % (HOUR_MILLISECOND) == 0) {
            level = "1H";
        } else if (ts % (30 * MINUTE_MILLISECOND) == 0) {
            level = "30m";
        } else if (ts % (15 * MINUTE_MILLISECOND) == 0) {
            level = "15m";
        } else if (ts % (5 * MINUTE_MILLISECOND) == 0) {
            level = "5m";
        }
        return level;
    }

    /**
     *
     * @param barType
     * @return
     */
    private static long getTimeLenByBarType(String barType) {
        long timeLen = 0;
        switch (barType) {
            case "1d":
                timeLen = DAY_MILLISECOND;
                break;
            case "4H":
                timeLen = 4 * HOUR_MILLISECOND;
                break;
            case "1H":
                timeLen = HOUR_MILLISECOND;
                break;
            case "30m":
                timeLen = 30 * MINUTE_MILLISECOND;
                break;
            case "15m":
                timeLen = 15 * MINUTE_MILLISECOND;
                break;
            case "5m":
                timeLen = 5 * MINUTE_MILLISECOND;
                break;
            default:
                timeLen = MINUTE_MILLISECOND;
        }
        return timeLen;
    }


    public static JsonObject getJsonObj(String json){
        return JsonParser.parseString(json).getAsJsonObject();
    }
    /**
     *
     * @param key  "open"，"hig"，low，close
     * @return
     */
    public static float getValByKey(JsonObject jsonObject, String key){
        return jsonObject.get(key).getAsFloat();
    }

    /**
     * 获取n天的ema
     * @param n
     * @param barType
     * @return
     */
    public static float getEma(Map<Long, String> hisDataMap, String date, int n, String barType){
        float res = 0;
        EMA ema = new EMA(n);
        try {
            long time = sdf.parse(date).getTime();
            long timeLenByBarType = getTimeLenByBarType(barType);
            List<Float> closeList = new ArrayList<>();
            String s = hisDataMap.get(time - n * timeLenByBarType);
            if (s == null) {
                return 0;
            }
            for (int i = 0; i < n; i++) {
                String keiBarInfoJson = getKeiBarInfoJson(hisDataMap, time-(n-i)*timeLenByBarType, barType);
                JsonObject jsonObject = JsonParser.parseString(keiBarInfoJson).getAsJsonObject();
                Float close = getValByKey(jsonObject, "close");
                closeList.add(close);
            }
            for (float price : closeList) {
                float emaValue = ema.calculateEMA(price);
                res = emaValue;
            }
            System.out.println("EMA"+n+": " + res);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return res;
    }


    /**
     * 获取n天的ema
     * @param n
     * @param barType
     * @return
     */
    public static float getRSI(Map<Long, String> hisDataMap, String date, int n, String barType){
        float rsiVal = 0;
        try {
            long time = sdf.parse(date).getTime();
            long timeLenByBarType = getTimeLenByBarType(barType);
            List<Float> closeList = new ArrayList<>();
            String s = hisDataMap.get(time - n * timeLenByBarType);
            if (s == null) {
                return 0;
            }
            for (int i = 0; i < n; i++) {
                String keiBarInfoJson = getKeiBarInfoJson(hisDataMap, time-(n-i)*timeLenByBarType, barType);
                JsonObject jsonObject = JsonParser.parseString(keiBarInfoJson).getAsJsonObject();
                Float close = getValByKey(jsonObject, "close");
                closeList.add(close);
            }
            RSI rsi = new RSI(n);
            rsiVal = rsi.calculateRSI(closeList);
            System.out.println("RSI"+n+": " + rsiVal);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return rsiVal;
    }
}
