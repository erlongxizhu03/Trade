package com.example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description
 * @Author erlong.zhou
 * @Date 2025/7/11 11:33
 */
public class ReciteWord {

    public static void main(String... args) throws Exception {
        //解析成map
        String filePath = "C:\\Users\\rocket.zhou\\Documents\\单词\\word1.txt"; // 文件路径
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            StringBuffer sb = new StringBuffer();

            for (String line : lines) {
                sb.append(line).append("\r\n");
            }
//            System.out.println(sb.toString());
            // 正则表达式匹配单词、音标和释义
            Pattern pattern = Pattern.compile("(\\w+)\\s+/([^/]+)/\\s*(\\w*\\.*)(.*?)(?=\\w+\\s+/)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(sb.toString());
            Map<String, String> wordMap = new LinkedHashMap<>();
            while (matcher.find()) {
                String word = matcher.group(1).trim(); // 单词
                String phonetic = matcher.group(2).trim(); // 音标
                String partOfSpeech = matcher.group(3).trim(); // 词性
                String definition = matcher.group(4).trim(); // 释义
                // 将单词和释义存入Map中
                wordMap.put(word+"_"+phonetic, partOfSpeech + definition);
            }
            // 输出结果
            int i = 1;
            for (Map.Entry<String, String> entry : wordMap.entrySet()) {
                System.out.println(i++ + ":" + entry.getKey() + "===" + entry.getValue());
            }
            Scanner scanner = null;
            System.out.println("\u001B[31m根据释义输入对应的单词:\u001B[0m");
            while (true) {
                // 随机获取一个键值对(单词和释义
                String wordPhonetic = getRandomKey(wordMap);
                String word = wordPhonetic.split("_")[0];
                String phonetic = wordPhonetic.split("_")[1];
                String randomValue = wordMap.get(wordPhonetic);
                System.out.println("释义===" + randomValue + ":");

                scanner = new Scanner(System.in);
                String input = scanner.nextLine(); // 读取一行文本

                while (input == null || !input.equals(word.toLowerCase())) {
                    System.out.println("不正确，请再次输入,音标提示, /"+phonetic+"/:");
                    scanner = new Scanner(System.in);
                    input = scanner.nextLine(); // 读取一行文本
                    if ("1".equals(input)) {
                        //1 单词展示
                        System.out.println(word);
                    }
                }
                System.out.println("正确，请输入下一个：");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getRandomKey(Map<String, String> map) {
        // 将Map的键转化为List
        List<String> keys = new ArrayList<>(map.keySet());
        // 生成随机索引
        int randomIndex = new Random().nextInt(keys.size());
        // 返回随机键
        return keys.get(randomIndex);
    }
}
