package com.gsc.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

/**
 * 第一版本的亚马逊抢购软件
 * 产品列表为横向+纵向
 * 流程为点产品图片 ，当有一键购按钮时点击（有自营是才会有一键购按钮，并且自动选择自营产品）
 * @author guoshaocheng
 *
 */
public class ProductBuying1 implements Runnable {

	private static final Logger logger = Logger.getLogger(ProductBuying1.class);
	
	private SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMdd-HHmmss");

	private String  productPageUrl;
	private String  account;
	private String  password;
	private int     refreshInterval;
	
	@SuppressWarnings("unused")
	private int     oneClickRetryTime;
	
	private boolean isPrintHtml;
	private String  homePage;
	private String  logPath;
	
	public ProductBuying1(String productPageUrl, String account, String password,
			int refreshInterval, int oneClickRetryTime, boolean isPrintHtml,
			String homePage,String logPath) {
		this.productPageUrl = productPageUrl;
		this.account = account;
		this.password = password;
		this.refreshInterval = refreshInterval;
		this.oneClickRetryTime = oneClickRetryTime;
		this.isPrintHtml = isPrintHtml;
		this.homePage = homePage;
		this.logPath = logPath;
	}

	@Override
	public void run() {
		buyByAfterClickImage();
	}
	
	private void buyByAfterClickImage() {

		info("-----------------------productBuying start(buyByClickImage)，version 1.0----------------------");
		WebClient webClient = null;
		try {

			webClient = getWebClient();
			
			info("---------------------home page--------------------");
			HtmlPage homePage = webClient
					.getPage(this.homePage);
			printHtmlFile(homePage.asXml(), "homePage");

			DomElement accElement = homePage.getElementById("nav-link-accountList");
			if(accElement == null) {
				accElement = homePage.getElementById("nav-link-yourAccount");
			}
			info("---------------------sign in page-------------------");
			HtmlPage signInPage = (HtmlPage) accElement.click();
			printHtmlFile(signInPage.asXml(), "signInPage");

			info("---------------------after sign In Page-----------------");
			HtmlPage afterSignInPage = signIn(signInPage);
			printHtmlFile(afterSignInPage.asXml(), "afterSignInPage");
			
			if(!isSignInSuccess(afterSignInPage)) {
				if(isPrintHtml==false) {
					printHtmlFile(afterSignInPage.asXml(), "afterSignInPage",true);
				}
				error("登录失败，请查看账号密码是否正确");
				throw new Exception("登录失败，请查看账号密码是否正确");
			}
			
			info("---------------------to buy Now With 1-Click Page-----------------");
			HtmlPage buyNowWith1ClickPage = webClient.getPage(productPageUrl);
			printHtmlFile(buyNowWith1ClickPage.asXml(), "buyNowWith1ClickPage");
			
			while (!hasSelfSupportProduct(buyNowWith1ClickPage)) {
				info("-----------------未找到自营产品，休眠:" 
						+ refreshInterval + "ms--------------");
				Thread.sleep(refreshInterval);
				buyNowWith1ClickPage = webClient.getPage(productPageUrl);
				printHtmlFile(buyNowWith1ClickPage.asXml(), "buyNowWith1ClickPage");
			}
			info("-----------------------找到自营产品-----------------------------");
			
			//界面有turn on 1-Click ordering
			if(buyNowWith1ClickPage.asXml().contains("oneClickSignInLink")) {
				info("---------------------click turn on 1-Click ordering-----------------");
				buyNowWith1ClickPage = turnOn1ClickOrdering(buyNowWith1ClickPage);
				printHtmlFile(buyNowWith1ClickPage.asXml(), "afterTurnOn1ClickOrdering");
			}
			
			info("---------------------click buy Now With 1-Click-----------------");
			HtmlPage afterBuyNowWith1ClickPage = buyNowWith1Click(buyNowWith1ClickPage);
			printHtmlFile(afterBuyNowWith1ClickPage.asXml(), "afterBuyNowWith1ClickPage");
			
			if(afterBuyNowWith1ClickPage.asXml().contains("Sign in")) {
				info("---------------------sign in again-----------------");
				afterBuyNowWith1ClickPage = signInAgain(afterBuyNowWith1ClickPage);
				printHtmlFile(afterBuyNowWith1ClickPage.asXml(), "afterBuyNowWith1ClickPage-signInAgain");
			}
			
			buyEnd(afterBuyNowWith1ClickPage);
			
		} catch (FailingHttpStatusCodeException e) {
			error(CommonTools.getExceptionString(e));
		} catch (MalformedURLException e) {
			error(CommonTools.getExceptionString(e));
		} catch (IOException e) {
			error(CommonTools.getExceptionString(e));
		} catch (Exception e) {
			error(CommonTools.getExceptionString(e));
		} finally {
			webClient.close();
			info("-----------------------productBuying(buyByClickImage)，version "
					+ "1.0 end----------------------");
		}
	}
	
	private HtmlPage signInAgain(HtmlPage page) throws IOException {

		if(page.getTitleText().equalsIgnoreCase("Amazon Anmelden") ||
				page.getTitleText().equalsIgnoreCase("Amazon Sign in")){
			info("------------------重新登录---------------------------");
			return signIn(page);
		}
		
		return page;
	}
	
	private boolean isSignInSuccess(HtmlPage afterSignInPage) {

		if(afterSignInPage.asXml().contains("There was a problem")) {
			return false;
		}
		return true;
	}

	private boolean hasSelfSupportProduct(HtmlPage buyNowWith1ClickPage) {

		if(buyNowWith1ClickPage.asXml().contains("oneClickSignInLink") ||
				buyNowWith1ClickPage.asXml().contains("one-click-button") ||
				buyNowWith1ClickPage.asXml().contains("oneClickBuyButton")) {
			return true;
		}
		return false;
	}

	private HtmlPage buyNowWith1Click(HtmlPage buyNowWith1ClickPage) throws Exception {

		DomNodeList<DomElement> inputList = buyNowWith1ClickPage.getElementsByTagName("input");
		for (DomElement inputElement : inputList) {
			if(inputElement.asXml().contains("oneClickBuyButton") ||
					inputElement.asXml().contains("one-click-button")) {
				try {
					HtmlPage afterbuyNowWith1Click = inputElement.click();
					return afterbuyNowWith1Click;
				} catch (IOException e) {
					e.printStackTrace();
					throw new Exception("buy Now With 1-Click 异常",e);
				}
			}
		}
		throw new Exception("未找到buy Now With 1-Click");
	}

	private HtmlPage turnOn1ClickOrdering(HtmlPage offerListingPage) throws Exception {
		
		DomNodeList<DomElement> aList = offerListingPage.getElementsByTagName("a");
		for (DomElement aElement : aList) {
			if(aElement.asXml().contains("oneClickSignInLink")) {
				try {
					HtmlPage afterTurnOn1ClickOrdering = aElement.click();
					return afterTurnOn1ClickOrdering;
				} catch (IOException e) {
					e.printStackTrace();
					throw new Exception("turn on 1-Click ordering 异常",e);
				}
			}
		}
		throw new Exception("未找到turn on 1-Click ordering");
	}

	private WebClient getWebClient() {
		
		WebClient webClient = new WebClient();
		
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getCookieManager().setCookiesEnabled(true);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		
		return webClient;
	}



	
	private void buyEnd(HtmlPage endPage) throws Exception {
		
		if(endPage.asXml().contains("Your 1-Click order has been placed.")) {
			//发送成功邮件邮件
			info("--------------------恭喜，订购成功----------------");
			sendMail("恭喜，订购成功,产品url：" + productPageUrl, 
					"恭喜，订购成功,account:" + account);
		} else {
			//发送失败邮件
			info("--------------------购买失败，请检查订单状态----------------");
			sendMail("购买失败，请检查订单状态,产品url：" + productPageUrl, 
					"购买失败，请检查订单状态,account:" + account);
			throw new Exception("购买失败，请检查订单状态");
		}
	}
	

	private HtmlPage signIn(HtmlPage signInPage) throws IOException {

		info("sign in invoke start");
		HtmlInput emailInput = (HtmlInput) signInPage
				.getElementById("ap_email");
		HtmlInput passwordInput = (HtmlInput) signInPage
				.getElementById("ap_password");
		HtmlInput signInSubmitInput = (HtmlSubmitInput) signInPage
				.getElementById("signInSubmit");
		emailInput.setValueAttribute(account);
		passwordInput.setValueAttribute(password);
		HtmlPage afterSignInPage = (HtmlPage) signInSubmitInput.click();
		info("sign in invoke end");
		afterSignInPage.getWebResponse();
		return afterSignInPage;
	}

	private void printHtmlFile(String content, String fileName) throws IOException{

		printHtmlFile(content,fileName,isPrintHtml);
	}
	
	private void printHtmlFile(String content, String fileName,boolean isPrintHtmlFile) throws IOException{

		if(isPrintHtmlFile) {
			File file = null;
			FileWriter writer = null;
			try {
				file = new File(logPath + "/" + Thread.currentThread().getId());
				if (!file.exists())
					file.mkdirs();
				
				file = new File(logPath + "/"  + Thread.currentThread().getId()  + 
						"/" + yyyyMMddHHmmss.format(new Date()) + "___" + fileName + ".html");
				if (!file.exists())
					file.createNewFile();
				writer = new FileWriter(file);
				writer.write(content);
			} catch (IOException e) {
				logger.error(CommonTools.getExceptionString(e));
				throw e;
			} finally {
				if (writer!=null) {
					try {
						writer.close();
					} catch (IOException e) {
						logger.error(CommonTools.getExceptionString(e));
						throw e;
					}
				}
			}
		}
	}
	
	private void sendMail(String mailContent, String subject) throws Exception {
		
		String sendEmailAccount = MyProperties.pps.getProperty("sendEmailAccount");
		String sendEmailPasswor = MyProperties.pps.getProperty("SendEmailPasswor");
		String recEmailAccount = MyProperties.pps.getProperty("recEmailAccount");
		String smtpServiceAddress = MyProperties.pps.getProperty("smtpServiceAddress");
		MyEmail.sendMail(sendEmailAccount, sendEmailPasswor, recEmailAccount,
				smtpServiceAddress, mailContent, subject);
	}
	
	private void info(String str) {
		logger.info(str + "\r\n配置信息：account=" + account 
				+ ",password=" + password + ",url=" + productPageUrl);
	}
	
	private void error(String str) {
		logger.error(str + "\r\n配置信息：account=" + account 
				+ ",password=" + password + ",url=" + productPageUrl);
	}

}
