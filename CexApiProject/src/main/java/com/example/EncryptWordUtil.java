package com.example;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description TODO
 * @Author erlong.zhou
 * @Date 2022/10/20 17:32
 */
public class EncryptWordUtil {
    public final static EncryptWordUtil instance = new EncryptWordUtil();
    final static Pattern pattern = Pattern.compile("\\d*=[a-z]*");

    public static EncryptWordUtil getInstance() {
        return instance;
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("输入aes：");
        AES_KEY = scanner.next();
        /////////////Nostr私钥加密解密
        boolean isEncrypt = true;
        if (isEncrypt) {
            String encryptedResult = encrypt("bU6JTbokCFsMdRrd");
            System.out.println("加密结果："+encryptedResult);
        }else {
            String decryptResult = decrypt("Xb/gZyNgi7kn+rluWkQe5qpbtbZ3sal+CQ6LhjwNy6M=");
            System.out.println("解密结果："+decryptResult);
        }

        String text = "ability abandon accident angry arrow base buyer repeat miracle";
//        String encryptText = testEncrypt(text);
//        System.out.println("加密后的数据：" + encryptText);
        String testdecrypt = "";
        while (!testdecrypt.equals(text)){
            testdecrypt = testdecrypt("nsec1dhr4ly2u3lfk3jh8uf3ddqtuydkxgqkc4fnla3vku9lf6cygmwpsrxt9yl");
            System.out.println("解密:"+testdecrypt);
        }
        System.out.println("最终解密:"+testdecrypt);


    }
    public static String testEncrypt(String text) throws Exception {
        System.out.println("开始文本:"+text);
        Scanner scanner = new Scanner(System.in);
        System.out.print("输入aes：");
        AES_KEY = scanner.next();
//        EncryptWordUtil.AES_KEY = EncryptWordUtil.readTxt("D:/path/test.txt").trim();
        System.out.print("输入：");
        int moveNum = scanner.nextInt();
        return EncryptWordUtil.totalEncrypt(text, moveNum);
    }

    private static String testdecrypt(String encryptedData) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("输入：");
        int moveNum = scanner.nextInt();
        return EncryptWordUtil.totaldecrypt(encryptedData, moveNum);
    }

    public static String totaldecrypt(String encryptedData, int moveNum) throws Exception {
        String decryptedData = decrypt(encryptedData);
        System.out.println("解密后的数据：" + decryptedData);
        String restoreNums = restoreNums(decryptedData, moveNum);
        System.out.println("restoreNums:"+restoreNums);
        String numsToText = numsToText(restoreNums);
        return numsToText;
    }

    public static String totalEncrypt(String data, int moveNum) throws Exception {
        String textToNums = textToNums(data);
        System.out.println("textToNums:"+textToNums);
        String updatedNums = updateNums(textToNums, moveNum);
        System.out.println("updatedNums:"+updatedNums);
        String encryptedData = encrypt(updatedNums);
        return encryptedData;
    }

    private static String updateNums(String nums, int moveNum) {
        StringBuffer res = new StringBuffer();
        String[] numArr = nums.split(" ");
        for (int i = 0; i < numArr.length; i++) {
            int num = Integer.parseInt(numArr[i]);
            if (num > 0) {
                int updateNum = (num + moveNum) % 2048;
                res.append(updateNum).append(" ");
            }
        }
        return res.deleteCharAt(res.length() - 1).toString();
    }

    private static String restoreNums(String nums, int moveNum) {
        StringBuffer res = new StringBuffer();
        String[] numArr = nums.split(" ");
        for (int i = 0; i < numArr.length; i++) {
            int num = Integer.parseInt(numArr[i]);
            int restoreNum = num - moveNum;
            if (restoreNum < 0) {
                restoreNum += 2048;
            }
            res.append(restoreNum).append(" ");
        }
        return res.deleteCharAt(res.length() - 1).toString();
    }

    private static String textToNums(String data) {
        String trim = readTxt("C:\\Users\\rocket.zhou\\Documents/zjc.txt").trim();
        // 创建Matcher对象并将其与目标字符串关联起来
        Matcher matcher = pattern.matcher(trim);
        Map<String, Integer> map = new HashMap<>();
        // 查找所有匹配项
        while (matcher.find()) {
            // 输出每次匹配到的位置索引
//            System.out.println("startIndex: " + matcher.start()+", endIndex: " + matcher.end());
            String matchedText = matcher.group();
//            System.out.println("匹配的文本为：" + matchedText);
            String[] split = matchedText.split("=");
            map.put(split[1], Integer.parseInt(split[0]));
        }
        StringBuffer stringBuffer = new StringBuffer();
        String[] words = data.split(" ");
        for (int i = 0; i < words.length; i++) {
            stringBuffer.append(map.getOrDefault(words[i], -1)).append(" ");
        }
        return stringBuffer.deleteCharAt(stringBuffer.length() - 1).toString();
    }

    private static String numsToText(String updatedNums) {
        String trim = readTxt("C:\\Users\\rocket.zhou\\Documents/zjc.txt").trim();
        // 创建Matcher对象并将其与目标字符串关联起来
        Matcher matcher = pattern.matcher(trim);
        Map<String, String> map = new HashMap<>();
        // 查找所有匹配项
        while (matcher.find()) {
            // 输出每次匹配到的位置索引
//            System.out.println("startIndex: " + matcher.start()+", endIndex: " + matcher.end());
            String matchedText = matcher.group();
//            System.out.println("匹配的文本为：" + matchedText);
            String[] split = matchedText.split("=");
            map.put(split[0], split[1]);
        }
        StringBuffer stringBuffer = new StringBuffer();
        String[] words = updatedNums.split(" ");
        for (int i = 0; i < words.length; i++) {
            stringBuffer.append(map.getOrDefault(words[i], "null")).append(" ");
        }
        return stringBuffer.deleteCharAt(stringBuffer.length() - 1).toString();
    }

    private static final String AES_ALGORITHM = "AES";
    // AES加密模式为CBC，填充方式为PKCS5Padding
    //CBC模式：特点：串行处理，同样的明文每次生成的密文不一样；ECB模式：特点：每段之间互不依赖，同样的明文总是生成同样的密文。
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding"; // "AES/CBC/PKCS5Padding"
    // AES密钥为16位
    public static String AES_KEY = "";
    // AES初始化向量为16位
    private static final String AES_IV = "aaaabbbbccccdddd";

    /**
     * AES加密
     *
     * @param data 待加密的数据
     * @return 加密后的数据，使用Base64编码
     */
    private static String encrypt(String data) throws Exception {
        // 将AES密钥转换为SecretKeySpec对象
        SecretKeySpec secretKeySpec = new SecretKeySpec(AES_KEY.getBytes(), AES_ALGORITHM);
        // 将AES初始化向量转换为IvParameterSpec对象
//        IvParameterSpec ivParameterSpec = new IvParameterSpec(AES_IV.getBytes());
        // 根据加密算法获取加密器
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        // 初始化加密器，设置加密模式、密钥和初始化向量
//        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        // 加密数据
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        // 对加密后的数据使用Base64编码
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * AES解密
     *
     * @param encryptedData 加密后的数据，使用Base64编码
     * @return 解密后的数据
     */
    private static String decrypt(String encryptedData) throws Exception {
        // 将AES密钥转换为SecretKeySpec对象
        SecretKeySpec secretKeySpec = new SecretKeySpec(AES_KEY.getBytes(), AES_ALGORITHM);
        // 将AES初始化向量转换为IvParameterSpec对象
//        IvParameterSpec ivParameterSpec = new IvParameterSpec(AES_IV.getBytes());
        // 根据加密算法获取解密器
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        // 初始化解密器，设置解密模式、密钥和初始化向量
//        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        // 对加密后的数据使用Base64解码
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        // 解密数据
        byte[] decryptedData = cipher.doFinal(decodedData);
        // 返回解密后的数据
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    public static String readTxt(String filePath) {
        try {
            // 创建 FileReader 对象并传入文件路径作为参数
            FileReader reader = new FileReader(new File(filePath));

            // 创建 BufferedReader 对象，将其包装到 FileReader 上
            BufferedReader bufferedReader = new BufferedReader(reader);

            // 逐行读取文件内容
            StringBuffer str = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                str.append(line);
            }

            // 关闭流
            bufferedReader.close();
            reader.close();
            return str.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
