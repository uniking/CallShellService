package org.example;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CmdMap {
    static CmdMap self;
    static Map<String, String> realCmd = new HashMap<>(8);

    public static CmdMap getInstance(){
        if(self == null){
            self = new CmdMap();
            realCmd.put("ls", "ls");
            realCmd.put("zip", "zip");

            realCmd.put("tortoise", "./package.sh");
            realCmd.put("isign_export_creds.sh", "./isign_export_creds.sh");
            realCmd.put("iosPackage", "./msm_patch.sh");
        }

        return self;
    }

    public String replaceCmd(String cmd, String uuid){
        String vcmd = cmd.split(" ")[0];
        String rcmd = realCmd.get(vcmd);
        if(rcmd == null){
            return null;
        }

        //替换命令
        String tcmd = cmd.replaceFirst(vcmd, rcmd);

        //替换根目录
        String currentDirectory = System.getProperty("user.dir");
        currentDirectory = Paths.get(currentDirectory, FileTransferServer.VROOT).toString();

        //替换文件路径
        //tcmd = tcmd.replace(uuid, Paths.get(currentDirectory, uuid).toString());

        //安全处理
        tcmd = tcmd.split(";")[0];

        return tcmd;
    }

    public String replaceFilepath(String filePath, String uuid){
        return filePath.replace(uuid, Paths.get(FileTransferServer.VROOT, uuid).toString());
    }
}
