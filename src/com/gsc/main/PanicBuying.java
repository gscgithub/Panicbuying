package com.gsc.main;


import org.apache.log4j.Logger;

import com.gsc.tool.MyProperties;
import com.gsc.tool.ProductBuying1;

public class PanicBuying {

	private static final Logger logger = Logger.getLogger(PanicBuying.class);
	
	public static void main(String[] args) throws Exception {

		logger.info("-----------------------------PanicBuying 程序启动---------------------");
		logger.info("-----------------------------------加载配置---------------------");
		
		String productPageUrl = MyProperties.pps.getProperty("productPage");
		String[] accounts  = MyProperties.pps.getProperty("account").split(",");
		String[] passwords = MyProperties.pps.getProperty("password").split(",");
		int count = 1;
		for (int i =0; i<accounts.length; i++) {
			logger.info("启动抢购线程" + count + ",productPageUrl：" + productPageUrl +
					"account:" + accounts[i] + ",password:" + passwords[i]);
			count++;
			int refreshInterval = Integer.valueOf(MyProperties.pps.getProperty("refreshInterval"));
			int oneClickRetryTime = Integer.valueOf(MyProperties.pps.getProperty("oneClickRetryTime"));
			boolean isPrintHtml = Boolean.valueOf(MyProperties.pps.getProperty("isPrintHtml"));
			String homePage = MyProperties.pps.getProperty("homePage");
			ProductBuying1 productBuying = new ProductBuying1(productPageUrl,accounts[i],passwords[i],refreshInterval,oneClickRetryTime
					,isPrintHtml,homePage,MyProperties.logPath);
			new Thread(productBuying).start();
		}
	}
	
}
