package org.uiuc.ise.yicheng.purity.analysis.agent;

import org.objectweb.asm.Opcodes;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class Config {
    public static int ASM_VERSION = Opcodes.ASM9;
    public static String MID_SEPARATOR = "#";

    public static synchronized String version() {
        String version = System.getProperty("purity.analysis.version");
        if (version == null) {
            version = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
            System.setProperty("purity.analysis.version", version);
        }
        return version;
    }

    public static String workingDirectory() {
        return System.getProperty("user.home") + "/AgentLogs/PurityAnalysis/" + version();
    }
}
