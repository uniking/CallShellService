package org.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FileTransferClient {

    private String uuid = "";
    private String cmdUrl = "";
    private String uploadUrl = "";
    private String downloadUrl = "";
    private String pwdUrl = "";
    private String clearUrl = "";

    public FileTransferClient(String ip, int port){
        this.cmdUrl = "http://"+ip+":"+port+"/cmd";
        this.uploadUrl = "http://"+ip+":"+port+"/upload";
        this.downloadUrl = "http://"+ip+":"+port+"/download";
        this.pwdUrl = "http://"+ip+":"+port+"/pwd";
        this.clearUrl = "http://"+ip+":"+port+"/clear";

        uuid = UUID.randomUUID().toString();
    }

    /*
    要操作文件, 文件必须在以uuid名字为目录的路径下, 如 uuid/hello.txt
     */
    public int execCmd(String cmd, boolean haveFile) {
        try {
            System.out.println("to downloadFile");
            URL url = new URL(cmdUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-UUID", uuid);
            connection.setRequestProperty("X-CMD", cmd);
            if(haveFile){
                connection.setRequestProperty("X-FILE", "HF");
            }else{
                connection.setRequestProperty("X-FILE", "NF");
            }

            // 获取响应代码
            int responseCode = connection.getResponseCode();

            connection.disconnect();
            return responseCode;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /*
    获取当前文件处理的目录
     */
    public String getFileDirectory() {
        String fdir = "";
        try {
            URL url = new URL(pwdUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-UUID", uuid);

            // 获取响应代码
            int responseCode = connection.getResponseCode();
            if(responseCode == 200){
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[128];
                int len = inputStream.read(buffer);
                System.out.println("read "+len);
                fdir = new String(buffer, 0, len, StandardCharsets.UTF_8);
                inputStream.close();
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fdir;
    }

    public void clearFileDirectory() {
        try {
            URL url = new URL(clearUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-UUID", uuid);

            // 获取响应代码
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(String fileName){
        uploadFile(fileName, new File(fileName).getName());
    }

    /*
    文件被上传到uuid为名字的目录下
     */
    public void uploadFile(String fileName, String webName) {
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setChunkedStreamingMode(0);

            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("X-UUID", uuid);
            connection.setRequestProperty("X-File-Name", webName);

            // 打开输出流
            OutputStream outputStream = connection.getOutputStream();
            FileInputStream fileInputStream = new FileInputStream(fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            outputStream.close();

            // 获取响应代码
            int responseCode = connection.getResponseCode();
            System.out.println("Upload Response Code: " + responseCode);

            // 关闭连接
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    返回错误码, 主要关注处理成功和处理失败的码
     */
    public int downloadFile(String fileName, String newFilePath) {
        try {
            System.out.println("to downloadFile");
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-UUID", uuid);
            connection.setRequestProperty("X-File-Name", fileName);

            // 获取响应代码
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                System.out.println("Download Response Code: " + responseCode);

                // 读取响应内容并保存为文件
                Utils.createFilePath(newFilePath);
                InputStream inputStream = connection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(newFilePath);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.close();
                inputStream.close();

                // 关闭连接
                connection.disconnect();
                return responseCode;
            } else {
                System.out.println("responseCode=" + responseCode + ", " + connection.getResponseMessage());
                // 关闭连接
                connection.disconnect();
                return responseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
