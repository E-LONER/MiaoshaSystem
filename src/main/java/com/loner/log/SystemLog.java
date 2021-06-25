package com.loner.log;

import org.springframework.validation.ObjectError;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SystemLog {
    // 生成异常信息
    public static  void convException(List<ObjectError> errors){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringBuffer buffer = sw.getBuffer();
        File file =new File("C:/Users/E-loner/Desktop/Exeception.txt");
        PrintWriter writer = null;
        FileWriter fileWrite = null;
        try {
            fileWrite = new FileWriter(String.valueOf(file), true);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        writer = new PrintWriter(fileWrite);
        writer.append(System.getProperty("line.separator") + new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(new Date()));
        writer.append(System.getProperty("line.separator"));
        writer.append("*****************************************************************\n" + errors.toString() + "\n*****************************************************************");
        writer.append(System.getProperty("line.separator"));
        writer.flush();
        writer.close();

    }
}
