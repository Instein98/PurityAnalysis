package org.uiuc.ise.yicheng.purity.analysis.agent.utils;

import org.uiuc.ise.yicheng.purity.analysis.agent.Config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FileUtil {

    public static void prepare(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
    }

    public static void write(String path, byte[] bytes) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    prepare(path);
                    Files.write(Paths.get(path), bytes);
                } catch (Throwable t){
                    t.printStackTrace();
                }
            }
        }).start();
    }

    public static void dumpMap(Map<String, Boolean> map, String relativePath){
        File f = new File(Config.workingDirectory() + "/" + relativePath);
        try (FileWriter fr = new FileWriter(f); BufferedWriter bw = new BufferedWriter(fr)) {

            fr.write("");

            for (String methodId: map.keySet()){
                bw.write(methodId + ": " + map.get(methodId) + "\n");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
