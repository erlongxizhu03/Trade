package com.example.english;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
class Pronunciation {
    String pronunciation;
    String audio;
    String region;
    // Getters and Setters
}
class PosItem {
    List<Pronunciation> pronunciations;
    // Getters and Setters
}
class Record {
    String word;
    List<PosItem> pos_items;
    // Getters and Setters
}
/**
 * @Description
 * @Author erlong.zhou
 * @Date 2025/7/23 14:48
 */
public class Soundmark {

    public static void main(String[] args) {
        String returnValue = getSoundmark("He was glad for one thing: the rope was off his neck. That had given them an unfair advantage;");
        System.out.println(returnValue.trim());
    }

    private static String getSoundmark(String searchWord) {
        String jsonDbFilePath = System.getProperty("user.dir")+"/src/main/resources/cam_dict.refined.json";
        String region = "us";
        List<Record> jsonDatabase = loadJsonDatabase(jsonDbFilePath);
        String returnValue = searchWordsInDatabase(jsonDatabase, searchWord, region);
        return returnValue;
    }

    // 加载 JSON 数据库
    private static List<Record> loadJsonDatabase(String filePath) {
        List<Record> records = new ArrayList<>();
        Gson gson = new Gson();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Record record = gson.fromJson(line, Record.class);
                records.add(record);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return records;
    }
    // 在 JSON 数据库中检索 word
    private static String searchWordsInDatabase(List<Record> database, String searchWord, String region) {
        StringBuilder phonetics = new StringBuilder();
        String[] words = searchWord.replaceAll("[,.?!;:]", " ").split("\\s+");
        for (String word : words) {
            String trimmedWord = word.trim().toLowerCase();
            if (!trimmedWord.isEmpty()) {
                String result = searchInJsonDatabase(database, trimmedWord, region);
                if ("not exist".equals(result)) {
                    phonetics.append(trimmedWord).append("* ");
                } else {
                    phonetics.append(result).append(" ");
                }
            }
        }
        return phonetics.toString();
    }
    // 检索单个单词的发音信息
    private static String searchInJsonDatabase(List<Record> database, String searchWord, String region) {
        for (Record record : database) {
            if (record.word.equals(searchWord)) {
                for (PosItem posItem : record.pos_items) {
                    for (Pronunciation pronunciation : posItem.pronunciations) {
                        if (pronunciation.region.equals(region)) {
                            return pronunciation.pronunciation;
                        }
                    }
                }
            }
        }
        return "not exist";
    }

}
