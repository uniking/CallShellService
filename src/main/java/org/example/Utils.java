package org.example;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {

    public static void sendResponse(HttpExchange exchange, int code){
        try {
            // 响应客户端
            exchange.sendResponseHeaders(code, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendResponse(HttpExchange exchange, int code, String msg){
        try {
            // 响应客户端
            exchange.sendResponseHeaders(code, msg.length());
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(msg.getBytes());
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }

        directory.delete();
    }

    public static boolean createFilePath(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            return parentDir.mkdirs();
        }

        return true;
    }
    public static String getJsonValue(String filePath, String key) {
        try {
            // 读取 JSON 文件内容
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));

            // 创建 JSON 对象
            JSONObject jsonObject = new JSONObject(jsonString);

            // 获取指定键的值
            Object value = jsonObject.get(key);

            // 返回值（转换为字符串）
            return value.toString();
        } catch (Exception e) {
            ;//e.printStackTrace();
        }

        return null; // 如果发生错误，返回 null
    }


    public static void modifyJsonValue(String filePath, String key, Object newValue) {
        try {
            // 读取JSON文件
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonBuilder.append(line);
            }
            br.close();

            // 解析JSON
            String jsonContent = jsonBuilder.toString();
            JSONObject jsonObject = new JSONObject(jsonContent);

            // 修改指定键的值
            jsonObject.put(key, newValue);

            // 保存回文件
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(jsonObject.toString());
            fileWriter.close();

            //System.out.println("JSON文件修改成功！");
        } catch (FileNotFoundException e) {
            //System.out.println("文件未找到：" + filePath);
            e.printStackTrace();
        } catch (IOException e) {
            //System.out.println("文件读写错误：" + filePath);
            e.printStackTrace();
        } catch (JSONException e) {
            //System.out.println("JSON解析错误：" + filePath);
            e.printStackTrace();
        }
    }

    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return file.length();
        } else {
            return -1; // 文件不存在或不是一个有效的文件
        }
    }
}