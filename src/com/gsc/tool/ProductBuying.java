package com.gsc.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

/**
 * 第零版本的亚马逊抢购软件
 * 产品页面为竖屏
 * 流程为点new、点购物车、一键购物车
 * @author guoshaocheng
 *
 */
public class ProductBuying implements Runnable {

	private static final Logger logger = Logger.getLogger(ProductBuying.class);

	private String  productPageUrl;
	private String  account;
	private String  password;
	private int     refreshInterval;
	private int     oneClickRetryTime;
	private boolean isPrintHtml;
	private String  homePage;
	private String  logPath;
	
	public ProductBuying(String productPageUrl, String account, String password,
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
//		buyBy1Click();
		buyByBasket();
	}
	
	private void buyByBasket() {

		info("-----------------------productBuying start(buyByBasket)----------------------");
		WebClient webClient = null;
		try {

			webClient = getWebClient();
			
			info("---------------------home page--------------------");
			HtmlPage homePage = webClient
					.getPage(this.homePage);
			printHtmlFile(homePage.asXml(), "homePage");

			DomElement accElement = homePage
					.getElementById("nav-link-yourAccount");
			info("---------------------sign in page-------------------");
			HtmlPage signInPage = (HtmlPage) accElement.click();
			printHtmlFile(signInPage.asXml(), "signInPage");

			info("---------------------after sign In Page-----------------");
			HtmlPage afterSignInPage = signIn(signInPage);
			printHtmlFile(afterSignInPage.asXml(), "afterSignInPage");
			
			info("---------------------to offer list Page-----------------");
			HtmlPage offerListingPage = webClient
					.getPage(productPageUrl);
			HtmlPage afterClickAddToBasket = clickAddToBasket(offerListingPage);
			if(afterClickAddToBasket != null)
				printHtmlFile(afterClickAddToBasket.asXml(), "afterClickAddToBasket");
			
			while(afterClickAddToBasket == null) {
				info("-----------------未找到自营产品，休眠:" 
						+ refreshInterval + "ms--------------");
				Thread.sleep(refreshInterval);
				try {
					offerListingPage = webClient.getPage(productPageUrl);
					afterClickAddToBasket = clickAddToBasket(offerListingPage);
				} catch (FailingHttpStatusCodeException e) {
					error(CommonTools.getExceptionString(e));
					Thread.sleep(2*refreshInterval);
					webClient.close();
					webClient = getWebClient();
				}
				if(afterClickAddToBasket != null)
					printHtmlFile(afterClickAddToBasket.asXml(), "afterClickAddToBasket");
			}
			
			
			info("---------------------Click Shopping-Basket-----------------");
			HtmlPage afterClickShoppingBasket = clickShoopingBasket(afterClickAddToBasket);
			printHtmlFile(afterClickShoppingBasket.asXml(), "afterClickShoppingBasket");
			
			info("---------------------Click 1-click-----------------");
			HtmlPage after1Click = buy1Click1(afterClickShoppingBasket);
			printHtmlFile(after1Click.asXml(), "after1Click");
			
			buyEnd(after1Click);
			
//			info("---------------------Click Proceed To Checkout-----------------");
//			HtmlPage afterClickProceedToCheckoutPage = 
//					clickProceedToCheckout(afterClickAddToBasket);
//			printHtmlFile(afterClickProceedToCheckoutPage.asXml(), 
//					"afterClickProceedToCheckoutPage1");
//			
//			afterClickProceedToCheckoutPage = signInAgain(afterClickProceedToCheckoutPage);
//			printHtmlFile(afterClickProceedToCheckoutPage.asXml(), 
//					"afterClickProceedToCheckoutPage2");
//			
//			info("---------------------Click Deliver To This Address-----------------");
//			HtmlPage afterClickDeliverToThiAddress = 
//					clickDeliverToThisAddress(afterClickProceedToCheckoutPage);
//			printHtmlFile(afterClickDeliverToThiAddress.asXml(), 
//					"afterClickDeliverToThiAddress");
//			
//			info("---------------------Choose your delivery options-----------------");
//			HtmlPage afterContinuePage1 = continueClick(afterClickDeliverToThiAddress);
//			printHtmlFile(afterContinuePage1.asXml(), "afterContinuePage1");
//			
//			info("---------------------Select a payment method-----------------");
//			HtmlPage afterContinuePage2 = continueClick(afterContinuePage1);
//			printHtmlFile(afterContinuePage2.asXml(), "afterContinuePage2");
			
//			buyEnd(afterBuyNow);

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
		}
		info("-----------------------productBuying(buyByBasket) end----------------------");
	}
	
	private WebClient getWebClient() {
		
		WebClient webClient = new WebClient();
		
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getCookieManager().setCookiesEnabled(true);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		
		return webClient;
	}

	private HtmlPage buy1Click1(HtmlPage afterClickShoppingBasket) throws Exception {
		
		if(afterClickShoppingBasket.asXml().contains("to turn on 1-Click ordering.")){
			DomNodeList<DomElement> aList = afterClickShoppingBasket.getElementsByTagName("a");
			for (DomElement aElement : aList) {
				if(aElement.getTextContent().contains("Sign in")) {
					try {
						HtmlPage signAgainPage = aElement.click();
						printHtmlFile(signAgainPage.asXml(), "signAgainPage");
						afterClickShoppingBasket = signInAgain(signAgainPage);
						printHtmlFile(afterClickShoppingBasket.asXml(), 
								"afterClickShoppingBasket-signInAgain");
						break;
					} catch (IOException e) {
						throw new Exception("click sign in again 异常",e);
					}
				}
			}
		}
		
		return afterClickShoppingBasket.getElementById("oneClickCartButton").click();
	}

	private HtmlPage clickShoopingBasket(HtmlPage afterClickAddToBasket) throws Exception {

		DomElement basketA = afterClickAddToBasket.getElementById("nav-cart");
		try {
			return basketA.click();
		} catch (IOException e) {
			throw new Exception("click Shooping Basket 异常",e);
		}
	}

	@SuppressWarnings("unused")
	private HtmlPage clickDeliverToThisAddress(
			HtmlPage deliverToThiAddressPage) throws Exception {

		DomNodeList<DomElement> aList = 
				deliverToThiAddressPage.getElementsByTagName("a");
		for (DomElement a : aList) {
			if(a.getTextContent().contains("Deliver to this address")) {
				return a.click();
			}
		}
		throw new Exception("未找到 Deliver To This Address按钮");
	}

	@SuppressWarnings("unused")
	private HtmlPage clickProceedToCheckout(HtmlPage proceedToCheckoutPage) throws Exception {
		
		info("----------------------click Proceed To Checkout-------------------------");
		try {
			return proceedToCheckoutPage.getElementById("hlb-ptc-btn-native").click();
		} catch (IOException e) {
			throw new Exception("点击Proceed To Checkout异常",e);
		}
	}
	
	private HtmlPage clickAddToBasket(HtmlPage offerListingPage) throws Exception {

		info("---------------------click Add To Basket-------------------------");
		printHtmlFile(offerListingPage.asXml(), "offerListingPage");
		
		DomNodeList<DomElement> divList = offerListingPage
				.getElementsByTagName("div");
		
		for (DomElement div : divList) {
			//产品列表
			if(div.getAttribute("class")!=null
					&& div.getAttribute("class").equals("a-row a-spacing-mini olpOffer")) {
				DomNodeList<HtmlElement> imgElements = div.getElementsByTagName("img");
				//查找自营产品
				for (HtmlElement imgElement : imgElements) {
					
					if(imgElement.getAttribute("alt")!=null && 
							imgElement.getAttribute("alt").equals("Amazon.de")){
						
						info("-----------------------找到自营产品-----------------------------");
//						info("--------------自营产品div--------------\r\n" + div.asXml() +
//								"\r\n--------------自营产品div 结束----------------------");
						//点击 Add To Basket
						info("click Add To Basket");
						DomNodeList<HtmlElement> inputList = div.getElementsByTagName("input");
						for (HtmlElement input : inputList) {
							if(input.getAttribute("name") !=null &&
									input.getAttribute("name").equals("submit.addToCart")) {
								return input.click();
							}
						}
						throw new Exception("未找到Add To Basket按钮");
					}
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private void buyBy1Click() {
		
		info("-----------------------productBuying start(buyBy1Click)----------------------");
		WebClient webClient = null;
		try {

			webClient = new WebClient();
//			webClient.getOptions().setCssEnabled(false);
// 			webClient.getOptions().setJavaScriptEnabled(false);
			webClient.getCookieManager().setCookiesEnabled(true);
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			
			info("---------------------home page--------------------");
			HtmlPage homePage = webClient
					.getPage(this.homePage);
			printHtmlFile(homePage.asXml(), "homePage");

			DomElement accElement = homePage
					.getElementById("nav-link-yourAccount");
			info("---------------------sign in page-------------------");
			HtmlPage signInPage = (HtmlPage) accElement.click();
			printHtmlFile(signInPage.asXml(), "signInPage");

			info("---------------------after sign In Page-----------------");
			HtmlPage afterSignInPage = signIn(signInPage);
			printHtmlFile(afterSignInPage.asXml(), "afterSignInPage");
			
			info("---------------------to offer list Page-----------------");
			HtmlPage offerListingPage = webClient
					.getPage(productPageUrl);
			webClient.waitForBackgroundJavaScript(100);
			info("---------------------click 1-click-----------------");
			HtmlPage after1ClickPage = buy1Click(offerListingPage, 1);
			while(after1ClickPage == null) {
				info("-----------------未找到自营产品，休眠:" 
						+ refreshInterval + "--------------");
				Thread.sleep(refreshInterval);
				offerListingPage = webClient.getPage(productPageUrl);
				after1ClickPage = buy1Click(offerListingPage, 1);
			}
			printHtmlFile(after1ClickPage.asXml(), "after1ClickPage");
			after1ClickPage = signInAgain(after1ClickPage);
			
			info("---------------------continue click------------------------");
			HtmlPage afterContinueClickPage = continueClick(after1ClickPage);
			printHtmlFile(afterContinueClickPage.asXml(), "afterContinueClickPage");
			afterContinueClickPage = signInAgain(afterContinueClickPage);
			
			info("---------------------buy now click--------------------------");
			HtmlPage afterBuyNow = buyNow(afterContinueClickPage);
			printHtmlFile(afterBuyNow.asXml(), "afterBuyNow");
			
			buyEnd(afterBuyNow);

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
		}
		info("-----------------------productBuying(buyBy1Click) end----------------------");
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
	
	private HtmlPage buyNow(HtmlPage buyPage) throws Exception {
		
		DomNodeList<DomElement> inputList = buyPage.getElementsByTagName("input");
		for (DomElement inputElement : inputList) {
			if(inputElement.getAttribute("value")!=null &&
					inputElement.getAttribute("value").equals("Buy now")){
				info("-------------------click Buy now-----------------------");
				return (HtmlPage)inputElement.click();
			}
		}
		//发送失败邮件
		info("--------------------购买失败，请检查订单状态----------------");
		sendMail("没有 Buy now按钮！！！请检查订单状态,产品url：" + productPageUrl, 
				"没有 Buy now按钮！！！请检查订单状态,account:" + account);
		throw new Exception("------------------没有 Buy now按钮！！！请检查订单状态---------------------");
	}
	
	private HtmlPage continueClick(HtmlPage continuePage) throws Exception {
		
		DomNodeList<DomElement> continueElements = continuePage.getElementsByTagName("input");
		for (DomElement continueElement : continueElements) {
			if(continueElement.getAttribute("value")!=null &&
					continueElement.getAttribute("value").equalsIgnoreCase("Continue")) {
				return (HtmlPage) continueElement.click();
			}
		} 
		
		throw new Exception("----------------------------没有continue按钮！！！----------------------");
	}

	/**
	 * 在订单页面，点击1-Click ordering，递归调用。 可能页面没有这个按钮，需要点击一次'sign in to turn on
	 * 1-Click ordering'按钮。
	 * 
	 * @param offerListingPage
	 * @param count
	 *            递归次数
	 * @return
	 * @throws Exception 
	 */
	private HtmlPage buy1Click(HtmlPage offerListingPage,int count) throws Exception {

		if(count > oneClickRetryTime) {
			throw new Exception("------------------------buy 1-Click 超过次数限制:" + oneClickRetryTime
					+ "---------------------------");
		}
		info("buy 1-Click invoke time:" + count++);
		info("---------------------offer listing page-------------------------");
		printHtmlFile(offerListingPage.asXml(), "offerListingPage");
		
		DomNodeList<DomElement> divList = offerListingPage
				.getElementsByTagName("div");
		
		for (DomElement div : divList) {
			//产品列表
			if(div.getAttribute("class")!=null
					&& div.getAttribute("class").equals("a-row a-spacing-mini olpOffer")) {
				DomNodeList<HtmlElement> imgElements = div.getElementsByTagName("img");
				//查找自营产品
				for (HtmlElement imgElement : imgElements) {
					
					if(imgElement.getAttribute("alt")!=null && 
							imgElement.getAttribute("alt").equals("Amazon.de")){
						
						info("-----------------------找到自营产品-----------------------------");
						
						//点击一建购买
						if(div.asText().contains("Buy with 1-Click")) {
							
							info("click 1-Click ordering");
							HtmlForm form = (HtmlForm)div.getElementsByTagName("form").get(1);
							DomNodeList<HtmlElement> inputList = form.getElementsByTagName("input");
							for (HtmlElement input : inputList) {
								if(input.getAttribute("type")!=null && 
										input.getAttribute("type").equals("submit")) {
									return (HtmlPage) input.click();
								}
							}
							
						} else if(div.asText().contains("to turn on 1-Click ordering")) {
							
							info("sign in to turn on 1-Click ordering");
							//查找sign in页面（产品列表没有一建购买选项，需要去重新登录）
							DomNodeList<HtmlElement> pElements = div.getElementsByTagName("P");
							for (HtmlElement pElement : pElements) {
								if(pElement.getAttribute("class")!=null &&
										pElement.getAttribute("class").equals("a-spacing-none a-text-center olpSignIn")) {
									
									//sign页面超链接，可能是Sign页面，也可能是有了1-Click按钮的产品列表页面
									info("-------------------- signInfor1Click page------------------------");
									HtmlPage signInfor1Click = (HtmlPage)pElement.getElementsByTagName("a").get(0).click();
									printHtmlFile(signInfor1Click.asXml(), "signInfor1Click");
									
									signInfor1Click = signInAgain(signInfor1Click); 
									return buy1Click(signInfor1Click, count);
								}
							}
						} else {
							info("--------------没有 sign in to turn on 1-Click ordering 按钮！！！"
									+ "--------------------------");
							info("--------------------刷新页面----------------------");
							buy1Click((HtmlPage)offerListingPage.refresh(), count);
						}
						break;
					}
				}
			}
		
		}
		return null;
	}
	
	private HtmlPage signInAgain(HtmlPage page) throws IOException {

		if(page.getTitleText().equalsIgnoreCase("Amazon Anmelden") ||
				page.getTitleText().equalsIgnoreCase("Amazon Sign in")){
			info("------------------重新登录---------------------------");
			return signIn(page);
		}
		
		return page;
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

		if(isPrintHtml) {
			File file = null;
			FileWriter writer = null;
			try {
				file = new File(logPath + "/" + Thread.currentThread().getId());
				if (!file.exists())
					file.mkdirs();
				file = new File(logPath + "/"  + Thread.currentThread().getId()  + 
						"/" + System.currentTimeMillis() + "___" + fileName + ".html");
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
