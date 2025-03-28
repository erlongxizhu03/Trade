package com.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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
    //触发提示的变化率（0.01，波动1%会提示）, 可用前边*根k线 开盘收盘差的均值 代替
    private static float triggerRate = 0.004F;
    //触发提示的变化率, 可用前边*根k线 开盘收盘差的均值 代替
    private static float triggerRate2 = 0.01204F;
    static Map<Integer, Float> map = new HashMap<>();
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //是否发邮件控制
    static boolean isSendEmail = false;

    public static void main(String[] args) {
        readStr("hello world!");
        showMsg("hello world!");


        Font font = new Font("微软雅黑", Font.PLAIN, 12);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);
        UIManager.put("OptionPane.inputFont", font);
        //必要条件(管住手，交易是等待的艺术，尽量不开单)：
        //1.没达指标不开单（否则容易被咬住）【指标2-3个：RSI/MACD/5分钟内波动率/斐波那契布林带】
        //2.第一手仓位控制==本金大小（千万不能all in）
        //3.开第一单后，第二单至少间隔的点数（不低于500点，降低交易频率，毛刺大一点）
        //4.没走出形态时该认错认错，可以亏小钱
        //5.盈利300点就走（剥头皮，合约不格局）
        int i = 0;
        NumberFormat percentInstance = NumberFormat.getPercentInstance();
        percentInstance.setMinimumFractionDigits(2);
        while (true) {
            i++;
            try {
                String response = lNFiGet();
//              System.out.println(response.toString());
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
                int deledKey2 = key-timeLen2; //超过timeLen2的删掉
                map.remove(deledKey2); //
                //急跌买入
                //CD时间不开单
                //抢收盘价
                if (tag_price < 86024f) {
                    System.out.println("牌来了，可以开多");
                    readStr("牌来了，可以开long：" + String.format("%.2f", tag_price));
                    showMsg("牌来了，可以开long：" + String.format("%.2f", tag_price));
                }

                if (tag_price > 89464) {
                    System.out.println("牌来了，可以平多");
                    readStr("牌来了，可以开short：" + String.format("%.2f", tag_price));
                    showMsg("牌来了，可以开short：" + String.format("%.2f", tag_price));
                }
                Thread.sleep(4000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
//        JOptionPane.showMessageDialog(null, msg, "提示", JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = new JDialog();
        JOptionPane pane = new JOptionPane(msg);
        pane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        dialog.setContentPane(pane);
        dialog.setModal(true);
        dialog.setTitle("提示");
        dialog.setAlwaysOnTop(true); // 确保弹窗始终在最顶层
        dialog.pack();
        dialog.setLocationRelativeTo(null); // 居中显示
        dialog.setVisible(true);
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
