package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileStatus {

    public static final int FILE_NOT_PROCESS = 0;
    public static final int FILE_PROCESSING = 1;
    public static final int FILE_PROCESS_FINISH = 2;

    public static final int FILE_PROCESS_ERROR = 3;


    public String uuid = "";
    public String cmd = "";
    public int uploadFileNum = 0;
    public int workStatus = 0;//0未处理, 1处理中, 2处理完成

    public FileStatus(String uuid){
        this.uuid = uuid;
    }

    public void executeCmd() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //System.out.println(uuid + ",设置工作状态1");
                    workStatus = FILE_PROCESSING;

                    // 创建Runtime对象
                    Runtime runtime = Runtime.getRuntime();

                    // 执行cmd命令
                    System.out.println(uuid+", execute cmd, "+cmd);
                    Process process = runtime.exec(cmd);

                    // 读取命令的输出
                    InputStream inputStream = process.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    String lastLine="";
                    String line="";
                    while ((line = reader.readLine()) != null) {
                        lastLine = line;
                        //System.out.println(line);
                    }
                    System.out.println(lastLine);

                    // 等待命令执行完成
                    int exitCode = process.waitFor();

                    // 输出执行结果
                    if (exitCode == 0) {
                        workStatus = FILE_PROCESS_FINISH;
                        //System.out.println(uuid + ",执行成功");
                    } else {
                        workStatus = FILE_PROCESS_ERROR;
                        //System.out.println(uuid + ",执行失败");

                    }
                } catch (Exception e) {
                    workStatus = FILE_PROCESS_ERROR;
                    e.printStackTrace();
                }

                //System.out.println(uuid + ",释放资源锁");
                FileTransferServer.releaseResource();
            }
        }).start();
    }
}
