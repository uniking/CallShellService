package org.example;

import java.io.File;

public class Example {
    public static void test1(){
        //每处理一次文件, 需要重新new
        FileTransferClient fileTransferClient = new FileTransferClient("localhost", 8080);

        // 上传文件
        fileTransferClient.uploadFile("/home/wzl/git/FileProcessService/file1.txt");
        fileTransferClient.uploadFile("file2.txt");
        fileTransferClient.uploadFile("file3.txt");

        //执行脚本
        while(true){
            try {
                //./package.sh -i /root/tortoise/test.apk -o /root/tortoise/new.apk -c /root/tortoise/config.json
                int code = fileTransferClient.execCmd("ls -al " + fileTransferClient.getFileDirectory(), false);
                if(code != 200){
                    System.out.println("没有资源");
                }else{
                    break;
                }
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


        while (true) {
            try {
                int code = fileTransferClient.downloadFile(fileTransferClient.getFileDirectory()+"/file1.txt", "download/1.txt");
                if(code == 200 || code == 201){
                    System.out.println("下载结束");
                    break;
                }else{
                    System.out.println("正在处理中");
                }
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean android_package(String ip, int port, String apkPath, String jksPath,
                                          String configPath, String emmConfigPath, String newApkPath){
        boolean bRet = false;
        //每处理一次文件, 需要重新new
        FileTransferClient fileTransferClient = new FileTransferClient(ip, port);
        String fileDir = fileTransferClient.getFileDirectory();

        // 上传文件
        fileTransferClient.uploadFile(jksPath);
        fileTransferClient.uploadFile(emmConfigPath);
        fileTransferClient.uploadFile(apkPath);

        //修理配置文件路径, keystore, sandbox_config
        String keystore = Utils.getJsonValue(configPath, "keystore");
        if(keystore != null){
            String name = new File(keystore).getName();
            Utils.modifyJsonValue(configPath, "keystore", fileDir+"/"+name);
        }
        String sandbox_config = Utils.getJsonValue(configPath, "sandbox_config");
        if(sandbox_config != null){
            String name = new File(sandbox_config).getName();
            Utils.modifyJsonValue(configPath, "sandbox_config", fileDir+"/"+name);
        }
        fileTransferClient.uploadFile(configPath);

        String apkName = new File(apkPath).getName();
        String configName = new File(configPath).getName();

        //执行脚本
        String cmd = String.format("tortoise -i %s/%s -o %s/packaged.apk -c %s/%s", fileDir, apkName, fileDir, fileDir, configName);
        while(true){
            try {
                int code = fileTransferClient.execCmd(cmd, true);
                if(code != 200){
                    System.out.println("没有资源");
                }else{
                    break;
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //下载文件
        while (true) {
            try {
                int code = fileTransferClient.downloadFile(fileTransferClient.getFileDirectory()+"/packaged.apk", newApkPath);
                if(code == 200 || code == 201){
                    System.out.println("下载结束");
                    if(code == 200){
                        bRet = true;
                    }
                    break;
                }else{
                    System.out.println("正在处理中");
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        fileTransferClient.clearFileDirectory();
        return bRet;
    }

    /*
    ./isign_export_creds.sh ioResource/123456.p12 key 123456
    ./msm_patch.sh -c key -f ioResource/iOSSandboxSDK.ipa -o new.ipa
     */
    public static void ios_export_creds(String ip, int port, String p12, String passwd){
        //每处理一次文件, 需要重新new
        FileTransferClient fileTransferClient = new FileTransferClient(ip, port);

        // 上传文件
        fileTransferClient.uploadFile(p12);
        String p12Name = new File(p12).getName();

        //执行脚本
        while(true){
            try {
                String fileDir = fileTransferClient.getFileDirectory();
                String cmd = String.format("isign_export_creds.sh %s/%s %s %s", fileDir, p12Name, fileDir, passwd);
                int code = fileTransferClient.execCmd(cmd, true);
                if(code != 200){
                    System.out.println("没有资源");
                }else{
                    break;
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //下载文件
        while (true) {
            try {
                int code = fileTransferClient.downloadFile(fileTransferClient.getFileDirectory()+"/certificate.pem", "iosTest/key/certificate.pem");
                if(code == 200 || code == 201){
                    System.out.println("下载结束");
                    break;
                }else{
                    System.out.println("正在处理中");
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        fileTransferClient.downloadFile(fileTransferClient.getFileDirectory()+"/key.pem", "iosTest/key/key.pem");

        fileTransferClient.clearFileDirectory();
    }

    /*
    ./isign_export_creds.sh ioResource/123456.p12 key 123456
    ./msm_patch.sh -c key -f ioResource/iOSSandboxSDK.ipa -o new.ipa
     */
    public static void ios_package(String ip, int port, String ipa, String cerPath, String keyPath, String provision, String opt){
        //每处理一次文件, 需要重新new
        FileTransferClient fileTransferClient = new FileTransferClient(ip, port);

        // 上传文件
        String unpackageName = "unpackaged.ipa";
        String packageName = "packaged.ipa";
        fileTransferClient.uploadFile(cerPath, "certificate.pem");
        fileTransferClient.uploadFile(keyPath, "key.pem");
        fileTransferClient.uploadFile(provision, "isign.mobileprovision");
        fileTransferClient.uploadFile(ipa, unpackageName);

        //执行脚本
        while(true){
            try {
                String fileDir = fileTransferClient.getFileDirectory();

                String cmd = String.format("iosPackage %s -c %s -f %s/%s -o %s/%s", opt, fileDir, fileDir, unpackageName, fileDir, packageName);
                int code = fileTransferClient.execCmd(cmd, true);
                if(code != 200){
                    System.out.println("没有资源");
                }else{
                    break;
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //下载文件
        while (true) {
            try {
                int code = fileTransferClient.downloadFile(fileTransferClient.getFileDirectory()+"/"+packageName, "iosTest/packaged.ipa");
                if(code == 200 || code == 201){
                    System.out.println("下载结束");
                    break;
                }else{
                    System.out.println("正在处理中");
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        fileTransferClient.clearFileDirectory();
    }



    public static void main(String[] args) {
        Example.android_package("localhost", 8080, "tortoiseTest/test.apk",
                "tortoiseTest/android.jks",
                "tortoiseTest/config.json",
                "tortoiseTest/emm-control.json",
                "download/new.apk");

        Example.ios_export_creds("localhost", 8080, "iosTest/123456.p12", "123456");

        Example.ios_package("localhost", 8080, "iosTest/iOSSandboxSDK.ipa",
                "iosTest/key/certificate.pem",
                "iosTest/key/key.pem",
                "iosTest/embedded.mobileprovision",
                "");
    }
}
