package com.gsc.tool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

public class MyProperties {

	private static final Logger logger = Logger.getLogger(MyProperties.class);
	
	public static Properties pps;
	
	public static final String logPath = "D:\\panicBuying\\";
	
	public static boolean isLoadSuccess = false;
	
	static {
		logger.info("---------------加载配置文件，配置文件路径:" + logPath + "config.properties");
		pps = new Properties();
		FileInputStream propertiesFileInputStream = null;
		
		try {
			propertiesFileInputStream =
					new FileInputStream(logPath + "config.properties");
			pps.load(propertiesFileInputStream);
			Enumeration<?> pNameEnumeration = pps.propertyNames();
			while (pNameEnumeration.hasMoreElements()) {
				String strKey = (String) pNameEnumeration.nextElement();
				String strValue = pps.getProperty(strKey);
				logger.info(strKey + "=" + strValue);
			}
			isLoadSuccess = true;
		} catch (FileNotFoundException e) {
			logger.error(CommonTools.getExceptionString(e));
		} catch (IOException e) {
			logger.error(CommonTools.getExceptionString(e));
		} finally {
			if(propertiesFileInputStream != null) {
				try {
					propertiesFileInputStream.close();
				} catch (IOException e) {
					logger.error(CommonTools.getExceptionString(e));
				}
				
			}
		}
	}
}
