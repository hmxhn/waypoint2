package com.xd.waypoint.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TXTReader {

    // 根据文件路径读取文本内容
    public static String readTxtFile(String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            // 创建 BufferedReader 对象
            bufferedReader = new BufferedReader(new FileReader(filePath));
            String line;
            // 逐行读取文本内容
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 关闭 BufferedReader
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 返回读取的文本内容
        return stringBuilder.toString();
    }
}
