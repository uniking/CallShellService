package org.example;

import java.io.File;
import java.nio.file.Paths;

public class Example {
    public static boolean android_package(String ip, int port, String apkPath, String newApkPath, String jksPath,
                                          String configPath, String emmConfigPath){
        boolean bRet = false;
        //每处理一次文件, 需要重新new
        FileTransferClient fileTransferClient = new FileTransferClient(ip, port);
        String fileDir = fileTransferClient.getFileDirectory();

        // 上传文件
        System.out.println("upload files, "+apkPath);
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
        System.out.println("exec tortoise, "+apkPath);
        String cmd = String.format("tortoise -i %s/%s -o %s/packaged.apk -c %s/%s", fileDir, apkName, fileDir, fileDir, configName);
        while(true){
            try {
                int code = fileTransferClient.execCmd(cmd, true);
                if(code != 200){
                    ;//System.out.println("没有资源");
                }else{
                    break;
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //下载文件
        System.out.println("download file, "+apkPath);
        while (true) {
            try {
                int code = fileTransferClient.downloadFile(fileTransferClient.getFileDirectory()+"/packaged.apk", newApkPath);
                if(code == 200 || code == 201){
                    ;//System.out.println("下载结束");
                    if(code == 200){
                        bRet = true;
                    }
                    break;
                }else{
                    ;//System.out.println("正在处理中");
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("finish, "+apkPath);
        fileTransferClient.clearFileDirectory();
        return bRet;
    }

    public static boolean ios_package(String ip, int port, String ipa, String outIpa, String p12, String passwd, String provision, String opt){
        boolean bResult = false;
        //每处理一次文件, 需要重新new
        FileTransferClient fileTransferClient = new FileTransferClient(ip, port);

        // 上传文件
        System.out.println("upload files, "+ipa);
        String unpackageName = "unpackaged.ipa";
        String packageName = "packaged.ipa";
        fileTransferClient.uploadFile(p12, "cer.p12");
        fileTransferClient.uploadFile(provision, "isign.mobileprovision");
        fileTransferClient.uploadFile(ipa, unpackageName);

        String fileDir = fileTransferClient.getFileDirectory();
        //执行脚本
        System.out.println("exec isign_export_creds, "+ipa);
        while(true){
            try {
                String cmd = String.format("isign_export_creds.sh %s/cer.p12 %s %s", fileDir, fileDir, passwd);
                int code = fileTransferClient.execCmd(cmd, true);
                if(code != 200){
                    ;//System.out.println("没有资源");
                }else{
                    break;
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("exec iosPackage, "+ipa);
        while(true){
            try {
                String cmd = String.format("iosPackage %s -c %s -f %s/%s -o %s/%s", opt, fileDir, fileDir, unpackageName, fileDir, packageName);
                int code = fileTransferClient.execCmd(cmd, false);
                if(code != 200){
                    ;//System.out.println("没有资源");
                }else{
                    break;
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //下载文件
        System.out.println("download file, "+ipa);
        while (true) {
            try {
                int code = fileTransferClient.downloadFile(fileTransferClient.getFileDirectory()+"/"+packageName, outIpa);
                if(code == 200 || code == 201){
                    ;//System.out.println("下载结束");
                    if(code == 200){
                        bResult = true;
                    }
                    break;
                }else{
                    ;//System.out.println("正在处理中");
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("finish, "+ipa);

        fileTransferClient.clearFileDirectory();

        return bResult;
    }

    public static void test(String ip, int port){
        FileTransferClient fileTransferClient = new FileTransferClient(ip, port);

        fileTransferClient.uploadFile("iosTest/iOSSandboxSDK.ipa");

        fileTransferClient.clearFileDirectory();
    }


    public static void main(String[] args) {
//        Example.android_package("120.46.65.127", 8000, "tortoiseTest/test.apk", "tortoiseTest/packaged.apk",
//                "tortoiseTest/android.jks",
//                "tortoiseTest/config.json",
//                "tortoiseTest/emm-control.json");

//        Example.ios_package("120.46.65.127", 8000, "iosTest/iOSSandboxSDK.ipa", "iosTest/packaged.ipa",
//                "iosTest/123456.p12", "123456", "iosTest/embedded.mobileprovision", "");
    }
}
