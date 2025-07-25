package com.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author erlong.zhou
 * @Date 2025/7/25 10:11
 */
public class $LNAddressMonitor {
    public static String LNAssetId = "0f8b9bb57522a824746b2ce364ae606ad433bc36db66ab86756e0e156a1ed34d";
    /**
     * address、balance
     */
    public static Map<String, Integer> initLNAddressBalanceMap = new HashMap<>(36);//读取file

    public static void main(String[] args) {
        Map<String, String> addressOwner = new HashMap<>();
        initAddressOwner(addressOwner);
        while (true) {
            try {
                String responseHolders = LNFiPostHolders(LNAssetId);
                JsonObject jsonObjectHolders = JsonParser.parseString(responseHolders).getAsJsonObject();
                JsonArray asJsonArray = jsonObjectHolders.get("data").getAsJsonObject().get("data").getAsJsonArray();
                for (int i = 0; i < asJsonArray.size(); i++) {
                    JsonObject asJsonObject = asJsonArray.get(i).getAsJsonObject();
                    int balance = asJsonObject.get("balance").getAsInt();
                    String owner = asJsonObject.get("owner").getAsString();
//                    System.out.println("owner:" + owner + ", balance:" + balance);
                    if (addressOwner.containsValue(owner)) {
                        Start.readStr("有监听地址出进入LN的top25");
                        System.out.println("owner:" + owner + ", balance:" + balance);
                    }
                }
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main2(String[] args) {
        String path = "E:\\init$LNAddressBalanceMap.txt";
        Map<String, String> addressOwner = new HashMap<>();
        initAddressOwner(addressOwner);

        //地址初始余额写入文件
//        for (Map.Entry<String, String> entry : addressOwner.entrySet()) {
//            try {
//                String owner = entry.getValue();
//                String responseBalance = LNFiPostBalance(LNAssetId, owner);
//                JsonObject jsonObject = JsonParser.parseString(responseBalance).getAsJsonObject();
//                int balance = jsonObject.get("data").getAsJsonObject().get("data").getAsJsonObject().get("balance").getAsInt();
//                System.out.println("address:" + entry.getKey() + ", owner:" + owner + ", balance:" + balance);
//                initLNAddressBalanceMap.put(entry.getKey(), balance);
//
//                Thread.sleep(4000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        writeMapToFile(initLNAddressBalanceMap, path);
//        System.out.println("文件写入完毕");


        //读取初始余额历史记录
        Map<String, Integer> stringStringMap = readMapFromFile(path);
        initLNAddressBalanceMap.putAll(stringStringMap);
        System.out.println("文件读取完毕");
        for (Map.Entry<String, Integer> stringIntegerEntry : initLNAddressBalanceMap.entrySet()) {
            System.out.println("address：" + stringIntegerEntry.getKey() + ", balance:" + stringIntegerEntry.getValue());
        }
        //查询第一页（余额最多，一页前25名）owner及余额
        Map<String, Integer> Top25OwnerBalanceMap = new HashMap<>();
        String responseHolders = LNFiPostHolders(LNAssetId);
        JsonObject jsonObjectHolders = JsonParser.parseString(responseHolders).getAsJsonObject();
        JsonArray asJsonArray = jsonObjectHolders.get("data").getAsJsonObject().get("data").getAsJsonArray();
        for (int i = 0; i < asJsonArray.size(); i++) {
            JsonObject asJsonObject = asJsonArray.get(i).getAsJsonObject();
            int balance = asJsonObject.get("balance").getAsInt();
            String owner = asJsonObject.get("owner").getAsString();
            System.out.println("owner:" + owner + ", balance:" + balance);
            Top25OwnerBalanceMap.put(owner, balance);
        }

        //top25名，有哪几个监听地址不在
        for (Map.Entry<String, Integer> monitorEntry : initLNAddressBalanceMap.entrySet()) {
            String owner = addressOwner.get(monitorEntry.getKey());
            if (!Top25OwnerBalanceMap.containsKey(owner)) {
                System.out.println("此监听地址不在top,address==" + monitorEntry.getKey());
            }
        }
        //top25中，有哪几个不在监听范围
        for (Map.Entry<String, Integer> top25Entry : Top25OwnerBalanceMap.entrySet()) {
            String top25Owner = top25Entry.getKey();
            if (!addressOwner.containsValue(top25Owner)) {
                System.out.println("此top地址不在监听范围,Owner==" + top25Owner + ",balance==" + top25Entry.getValue());
            }
        }

        while (true) {
            try {
                for (Map.Entry<String, String> entry : addressOwner.entrySet()) {
                    String owner = entry.getValue();
                    String responseBalance = LNFiPostBalance(LNAssetId, owner);
                    JsonObject jsonObject = JsonParser.parseString(responseBalance).getAsJsonObject();
                    int balanceLatest = jsonObject.get("data").getAsJsonObject().get("data").getAsJsonObject().get("balance").getAsInt();
                    if (balanceLatest != initLNAddressBalanceMap.get(entry.getKey())) {
                        Start.readStr("有LNFI address 出现余额变动");
                        System.out.println("有LNFI address 出现余额变动，balanceLatest:" + balanceLatest + ",initBalance:" + initLNAddressBalanceMap.get(entry.getKey()) + ",变化量:::::" + (balanceLatest - initLNAddressBalanceMap.get(entry.getKey())));
                    }
                    Thread.sleep(10000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void initAddressOwner(Map<String, String> addressOwner) {
        addressOwner.put("npub1nfxvt3tvzetxggvxdclcgtc2yvs802pq6sw4rh3dq9e6p9u2ylvskeqmxx", "9a4cc5c56c16566421866e3f842f0a232077a820d41d51de2d0173a0978a27d9"); //【邀请】
        addressOwner.put("npub1g9u0gheks0v702wn7kalaxqpgsnz5d778hugff9suhjxsyrm2utslzvlap", "4178f45f3683d9e7a9d3f5bbfe980144262a37de3df884a4b0e5e468107b5717"); //【被邀请】
        addressOwner.put("npub1ft2sexwl0gqwn6dxjjg5fgfzmlcmn92sg7smprwjmm97m5skkxvqtrgu0s", "4ad50c99df7a00e9e9a6949144a122dff1b9955047a1b08dd2decbedd216b198");
        addressOwner.put("npub14cysrvn9g4l294597ey2smwq2kcpr7fdnwpfu83fmqdn664dprhsrtshrt", "ae0901b265457ea2d685f648a86dc055b011f92d9b829e1e29d81b3d6aad08ef");
        addressOwner.put("npub1qzpg7whyj399qldkuyh2pgpaf7j5vkqakwgslrq6t33lue6jqn9qztegwl", "00828f3ae4944a507db6e12ea0a03d4fa546581db3910f8c1a5c63fe675204ca");
        addressOwner.put("npub1sudsyek9hdpqutn5whheyuvnqc24uf2se6mpdrxznd285e8jegzqhfnpg9", "871b0266c5bb420e2e7475ef92719306155e2550ceb6168cc29b547a64f2ca04");
        addressOwner.put("npub1cawr90g3qs2z8nxhs2jyaflq72eqzcqv8wwc0vg5e3cahjq9c0csch6r8a", "c75c32bd11041423ccd782a44ea7e0f2b201600c3b9d87b114cc71dbc805c3f1");
        addressOwner.put("npub18qs788xjrqsne6u5srfq6qf0639wpvkfjc3fq5su780eft7rhu4s5emxgk", "3821e39cd218213ceb9480d20d012fd44ae0b2c9962290521cf1df94afc3bf2b");
        addressOwner.put("npub1xksrhra2alwguw7sjz3yw48tw6540rrefzg92sgluezfvg0hnaws0kaqrq", "35a03b8faaefdc8e3bd090a24754eb76a9578c79489055411fe6449621f79f5d");
        addressOwner.put("npub15wrf7lccf6rf87zs0606tuftt85u2t7u2ce8gf96nl5fk6dlwkeq7p4aec", "a3869f7f184e8693f8507e9fa5f12b59e9c52fdc56327424ba9fe89b69bf75b2");
        addressOwner.put("npub1cx59cn83w0mk6rd5rsazg77dnydtyjdmvp0t4waqrtkuadplwp3q7jjatn", "c1a85c4cf173f76d0db41c3a247bcd991ab249bb605ebabba01aedceb43f7062");
        addressOwner.put("npub1zc8rfhl46hlew97n636w3uw0xd9tu0gn0vaz0dr6702505agvpnqnacm54", "160e34dff5d5ff9717d3d474e8f1cf334abe3d137b3a27b47af3d547d3a86066");//【邀请】【被邀请】
        addressOwner.put("npub1t6ppywdpw5p4axsa3z0uw2zzkmpm6mz7kfqzwl6cr25kdpfqdursddpynl", "5e821239a175035e9a1d889fc72842b6c3bd6c5eb240277f581aa96685206f07");
        addressOwner.put("npub1nwn9frcdyxd30teut8eana0nwx5y60e6xyws4dtq3sgwnsfgvpfszvxkx6", "9ba6548f0d219b17af3c59f3d9f5f371a84d3f3a311d0ab5608c10e9c1286053");

        addressOwner.put("npub1zvd42act24e4v0n7ggkrcmk9fxnvtye9tjzwtar3m57cy8gp4yesfnxp7j", "131b55770b5573563e7e422c3c6ec549a6c593255c84e5f471dd3d821d01a933");//【前10】
        addressOwner.put("npub1glsqeuxvz5rz65d4r3nr6rtz9pjgv0hkz5m6x6v3f4pe8q6tld6qdtul5q", "47e00cf0cc15062d51b51c663d0d622864863ef61537a369914d4393834bfb74");
        addressOwner.put("npub14a7n6r0efcxdtmc95swn4gtf7t7jrhtg7sucjrs89d4ddwkd4vrqvps4d3", "af7d3d0df94e0cd5ef05a41d3aa169f2fd21dd68f439890e072b6ad6bacdab06");
        addressOwner.put("npub1zp9534n8t2lsxe99fcq3p9d92egxymd3s83wwqlak8elz2at72jqgepsmv", "104b48d6675abf0364a54e011095a55650626db181e2e703fdb1f3f12babf2a4");
        addressOwner.put("npub1u8397qe0dxrr5r7ky205vvsxq8kd2rpxu3s2r2nlwu495r0wwusq09wrx2", "e1e25f032f69863a0fd6229f46320601ecd50c26e460a1aa7f772a5a0dee7720");
        addressOwner.put("npub1lunct0arkcdxlra5j6ty90a6yfsncgw42xa3y05zjz2skgnfr8fsts4vcy", "ff2785bfa3b61a6f8fb4969642bfba22613c21d551bb123e8290950b226919d3");
        addressOwner.put("npub1lews0wtjzyvqs64yrc32fw2qckj9fy8vl5ac0pmlcn5dfk975c2ssm4y6c", "fe5d07b9721118086aa41e22a4b940c5a45490ecfd3b87877fc4e8d4d8bea615");
        addressOwner.put("npub177eseyjk9skvz55kcwmqjncchx20pesrhemqm3thuz5tjnjff0eqjuralg", "f7b30c92562c2cc15296c3b6094f18b994f0e603be760dc577e0a8b94e494bf2");
        addressOwner.put("npub1tzcu85x7pzsm6zf8qlhrjkduuaslx9xxgwd7tjywmvwcyzuztnhqmzqt6d", "58b1c3d0de08a1bd092707ee3959bce761f314c6439be5c88edb1d820b825cee");
        addressOwner.put("npub1vgcpsx87nyqslcdh8j2vgy9k45hnzhddkcryuwav2dcqvt72aejqaw29dm", "62301818fe99010fe1b73c94c410b6ad2f315dadb6064e3bac5370062fcaee64");
    }

    public static void writeMapToFile(Map<String, Integer> map, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue());
                writer.newLine(); // 换行
            }
            System.out.println("Map 数据成功写入文件: " + filePath);
        } catch (IOException e) {
            System.err.println("写入文件时出错: " + e.getMessage());
        }
    }

    public static Map<String, Integer> readMapFromFile(String filePath) {
        Map<String, Integer> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 按冒号分割键和值
                String[] parts = line.split(": ", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim(); // 键
                    Integer value = Integer.parseInt(parts[1].trim()); // 值
                    map.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件时出错: " + e.getMessage());
        }
        return map;
    }

    /**
     * 0f8b9bb57522a824746b2ce364ae606ad433bc36db66ab86756e0e156a1ed34d【treat】
     * 475a642ed13bc44af6490c8571404988d7b514386cf8f3a603d04a0d2fa9f8f5【nostr】
     * a9b7c7367b9ad647651ff710a6b2ff9fa5c0d186b6eb2ff541eb4a98f6bbdd4b【burger】
     *
     * @param assetId
     * @return
     */
    public static String LNFiPostHolders(String assetId) {
        try {
            URL url = new URL("https://market-api.lnfi.network/assets/api/getHolders");

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //post添加请求参数
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true); // 允许写入数据到服务器

            // 获取输出流，用于写入JSON数据
            try (OutputStream os = con.getOutputStream()) {
                // JSON数据字符串
                String jsonInputString = "{\"assetId\":\"" + assetId + "\",\"owner\":\"\",\"page\":1,\"count\":25}";
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
        return "";
    }

    /**
     * 0f8b9bb57522a824746b2ce364ae606ad433bc36db66ab86756e0e156a1ed34d【treat】
     * 475a642ed13bc44af6490c8571404988d7b514386cf8f3a603d04a0d2fa9f8f5【nostr】
     * a9b7c7367b9ad647651ff710a6b2ff9fa5c0d186b6eb2ff541eb4a98f6bbdd4b【burger】
     *
     * @param owner
     * @return
     */
    public static String LNFiPostBalance(String assetId, String owner) {
        try {
            URL url = new URL("https://market-api.lnfi.network/assets/api/getHolder");

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //post添加请求参数
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true); // 允许写入数据到服务器

            // 获取输出流，用于写入JSON数据
            try (OutputStream os = con.getOutputStream()) {
                // JSON数据字符串
                String jsonInputString = "{\"assetId\":\"" + assetId + "\",\"owner\":\"" + owner + "\",\"page\":1,\"count\":25}";
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
        return "";
    }
}
