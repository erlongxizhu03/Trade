package com.example;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

/**
 * @Description TODO
 * @Author erlong.zhou
 * @Date 2025/3/21 14:20
 */
public class Start {
    final static Pattern pattern = Pattern.compile("x(\\d)");

    //变化率波动大提醒
    //对照时间范围，休眠10秒的倍数（300s,5分钟变化率），15分钟的变化率。
    private static int timeLen1 = 300;
    //对照时间范围，秒，1小时变化率。超过1小时的删除。
    private static int timeLen2 = 1200;
    //触发提示的变化率, 可用前边*根k线 开盘收盘差的均值 代替
    private static float triggerRate = 0.01F;
    //触发提示的变化率, 可用前边*根k线 开盘收盘差的均值 代替
    private static float triggerRate2 = 0.01204F;
    static Map<Integer, Float> map = new HashMap<>();
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        Font font = new Font("微软雅黑", Font.PLAIN, 12);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);
        UIManager.put("OptionPane.inputFont", font);

        while (true) {
            try {
                String response = lNFiGet();
//              System.out.println(response.toString());
                JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

                float price = jsonObject.get("data").getAsJsonArray().get(0).getAsJsonObject().get("index_price").getAsFloat();
                float tag_price = jsonObject.get("data").getAsJsonArray().get(0).getAsJsonObject().get("tag_price").getAsFloat();

                String ctime = formatter.format(new Date());
                System.out.printf(ctime + "， price=====%s, tag_price=====%s\n", String.format("%.2f", price), String.format("%.2f", tag_price));
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
                        System.out.println(ctime + "， 剧烈变化：" + timeLen1 / 60 + "分钟内涨跌" + String.format("%.4f", changeRate));
                        String type = changeRate > 0 ? "空" : "多";
                        showMsg("BTC剧烈波动，可以" + type + "，当前价格：" + String.format("%.2f", tag_price));
                    } else {
                        System.out.println(ctime + " " + timeLen1 / 60 + "分钟内涨跌" + String.format("%.4f", changeRate));
                    }
//                    System.out.println("5分钟前的价：" + preKey1 + ", " + preI + ",变化率：" + changeRate);
                } else {
                    System.out.println(",preI==" + preI + ",key==" + key + ",preKey1==" + preKey1 + ",dicLen:" + map.size());
                }

                //急跌买入
                //CD时间不开单
                //抢收盘价
                if (tag_price < 86024f) {
                    System.out.println("牌来了，可以开多");
                    showMsg("牌来了，可以开long：" + String.format("%.2f", tag_price));
                }

                if (tag_price > 87464) {
                    System.out.println("牌来了，可以平多");
                    showMsg("牌来了，可以开short：" + String.format("%.2f", tag_price));
                }
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void showMsg(String msg) {
        JOptionPane.showMessageDialog(null, msg, "提示", JOptionPane.INFORMATION_MESSAGE);
//        Font font = new Font("微软雅黑", Font.PLAIN, 12);
//        UIManager.put("OptionPane.messageFont", font);
    }

    public static void textToSound() {
        try {
//            boolean bool = VoiceManager.getInstance().contains("kevin");
//            boolean male = VoiceManager.getInstance().contains("male");
//            boolean female = VoiceManager.getInstance().contains("female");
//            boolean bool16 = VoiceManager.getInstance().contains("kevin16");
            Voice[] voices = VoiceManager.getInstance().getVoices();
            Voice voice = VoiceManager.getInstance().getVoice("kevin16");
            String text = "Hello, welcome to the world of text to speech conversion!";
            if (voice != null) {
                voice.allocate();
                voice.speak(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String lNFiGet() {
        try {
            URL url = new URL("https://test-futures-api.ln.exchange/napi/market/public_all_index_tag_price?");
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
