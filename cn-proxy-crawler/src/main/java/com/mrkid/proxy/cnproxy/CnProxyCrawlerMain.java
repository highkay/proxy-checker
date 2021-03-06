package com.mrkid.proxy.cnproxy;

import com.mrkid.proxy.dto.Proxy;
import com.mrkid.proxy.utils.Crawl4jUtils;
import edu.uci.ics.crawler4j.crawler.CrawlController;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * User: xudong
 * Date: 30/11/2016
 * Time: 9:51 AM
 */
public class CnProxyCrawlerMain {
    public static void main(String[] args) throws Exception {
        BlockingQueue<Proxy> proxies = new LinkedBlockingQueue<>();

        CrawlController crawlController = Crawl4jUtils.newCrawlController(CnProxyCrawler.STORE_ROOT);

        crawlController.addSeed(CnProxyCrawler.SEED);

        int numberOfCrawlers = 1;
        crawlController.start(() -> new CnProxyCrawler(proxies), numberOfCrawlers);
        crawlController.waitUntilFinish();
        crawlController.shutdown();

        System.out.println("crawl cnproxy finish, proxy count:" + proxies.size());
        System.out.println(proxies);
    }
}
