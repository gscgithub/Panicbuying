package com.gsc.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CommonTools {

	public static String getExceptionString(Exception e) {
		
		StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw =  new StringWriter();
            pw =  new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
      }
	return sw.toString();
	}
	
	public static void main(String[] args) {
		System.out.println(CommonTools.getExceptionString(new Exception("haha")));
	}
}
