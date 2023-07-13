package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class FileTransferServer {
    public static final String VROOT = "fpsTmp";
    static Map<String, FileStatus> fileStatusMap = new HashMap<>();
    static ResourceLock resourceLock = new ResourceLock(1);
    static String currentDirectory;
    static String fileDirectory;

    public static void releaseResource(){
        resourceLock.release();
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = exchange.getRequestHeaders().getFirst("X-UUID");

            try {
                // 创建目录
                File rootDir = new File(VROOT);
                if (!rootDir.exists()) {
                    rootDir.mkdirs();
                }
                File directory = new File(Paths.get(VROOT, uuid).toString());
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // 获取文件内容并保存到目录
                String fileName = exchange.getRequestHeaders().getFirst("X-File-Name");
                System.out.println(uuid + ", upload, "+fileName);
                Path filePath = directory.toPath().resolve(fileName);
                try (InputStream inputStream = exchange.getRequestBody()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }

                // 响应客户端
                Utils.sendResponse(exchange, 200, "File uploaded successfully");

                //更新fileStatus
                getFileStatus(uuid).uploadFileNum++;
            } finally {
                // 释放资源锁
            }
        }
    }

    static class CmdHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = exchange.getRequestHeaders().getFirst("X-UUID");
            String cmd = exchange.getRequestHeaders().getFirst("X-CMD");
            String haveFile = exchange.getRequestHeaders().getFirst("X-FILE");

            //判断是否已经上传文件
            if(haveFile == null && haveFile.equals("HF")){
                File dir = new File(uuid);
                if(! dir.exists()){
                    exchange.sendResponseHeaders(201, 0);
                    return;
                }
            }

            //准备FileStatus
            FileStatus fileStatus = getFileStatus(uuid);

            //根据状态执行
            switch (fileStatus.workStatus){
                case FileStatus.FILE_NOT_PROCESS:
                    {
                        String realCmd = CmdMap.getInstance().replaceCmd(cmd, uuid);
                        if(realCmd == null){
                            exchange.sendResponseHeaders(205, 0);
                            return;
                        }

                        if(resourceLock.acquire()){
                            fileStatus.cmd = realCmd;
                            fileStatus.executeCmd();
                            exchange.sendResponseHeaders(200, 0);
                            return;
                        }else{
                            exchange.sendResponseHeaders(204, 0);
                            return;
                        }
                    }
                case FileStatus.FILE_PROCESSING:
                    exchange.sendResponseHeaders(201, 0);
                    break;
                case FileStatus.FILE_PROCESS_FINISH:
                    exchange.sendResponseHeaders(200, 0);
                    break;
                default:
                    exchange.sendResponseHeaders(205, 0);
                    break;
            }
        }
    }

    static class PwdHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = exchange.getRequestHeaders().getFirst("X-UUID");
            Utils.sendResponse(exchange, 200, Paths.get(fileDirectory, uuid).toString());
        }
    }

    static class ClearHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = exchange.getRequestHeaders().getFirst("X-UUID");
            if(uuid != null && uuid.length() != 0){
                String dir = Paths.get(fileDirectory, uuid).toString();
                Utils.deleteDirectory(new File(dir));
            }
            Utils.sendResponse(exchange, 200);
        }
    }

    /*
    下载文件,
    200 处理文件成功
    201 处理文件失败
    202 正在处理文件, 或其他问题
     */
    static class DownloadHandler implements HttpHandler {
        public static long getFileSize(String filePath) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                return file.length();
            } else {
                return -1; // 文件不存在或不是一个有效的文件
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            try {
                String uuid = exchange.getRequestHeaders().getFirst("X-UUID");

                switch (fileStatusMap.get(uuid).workStatus){
                    case FileStatus.FILE_NOT_PROCESS:
                        Utils.sendResponse(exchange, 202, "not exec cmd");
                        break;
                    case FileStatus.FILE_PROCESSING:
                        Utils.sendResponse(exchange, 202, "pless wait, file is processing");
                        break;
                    case FileStatus.FILE_PROCESS_ERROR:
                        Utils.sendResponse(exchange, 201, "file process error");
                        break;
                    case FileStatus.FILE_PROCESS_FINISH:
                    {
                        String fileName = exchange.getRequestHeaders().getFirst("X-File-Name");
                        System.out.println(uuid+", download, "+fileName);
                        if(new File(fileName).exists()){
                            long fileSize = getFileSize(fileName);
                            exchange.sendResponseHeaders(200, fileSize);

                            // 设置响应头
                            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"compressed.zip\"");

                            // 将压缩文件的内容直接写入响应流
                            try (InputStream fileInputStream = new FileInputStream(fileName);
                                 OutputStream outputStream = exchange.getResponseBody())
                            {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fileInputStream.read(buffer)) != -1)
                                {
                                    //System.out.println("send data, "+bytesRead);
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                            }
                        }else{
                            Utils.sendResponse(exchange, 202, "file no exist");
                        }

                    }
                        break;
                    default:
                        Utils.sendResponse(exchange, 202, "unknow file status");
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            finally {
                // 释放资源锁
            }
        }
    }

    //参数 指定一个执行的shell脚本,
    public static void main(String[] args) {
        int port = 8080; // 服务器端口

        if(args.length != 1){
            System.out.println("set a listen port");
            return;
        }
        if(args.length == 1){
            try {
                port = Integer.parseInt(args[0]);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        currentDirectory = System.getProperty("user.dir");
        fileDirectory = Paths.get(currentDirectory, FileTransferServer.VROOT).toString();
        //清理文件目录
        Utils.deleteDirectory(new File(fileDirectory));

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/upload", new UploadHandler());
            server.createContext("/download", new DownloadHandler());
            server.createContext("/cmd", new CmdHandler());
            server.createContext("/pwd", new PwdHandler());
            server.createContext("/clear", new PwdHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileStatus getFileStatus(String uuid){
        FileStatus fileStatus = fileStatusMap.get(uuid);
        if(fileStatus == null){
            fileStatus = new FileStatus(uuid);
            fileStatusMap.put(uuid, fileStatus);
        }

        return fileStatus;
    }
}