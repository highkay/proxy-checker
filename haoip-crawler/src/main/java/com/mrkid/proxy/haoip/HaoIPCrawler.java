package com.mrkid.proxy.haoip;

import com.mrkid.proxy.dto.Proxy;
import com.mrkid.proxy.dto.Source;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: xudong
 * Date: 03/11/2016
 * Time: 12:47 PM
 */
public class HaoIPCrawler {

    public List<Proxy> fetchProxy() throws IOException {
        Document doc = Jsoup.connect("http://haoip.cc/tiqu.htm").timeout(30000).get();

        final Elements tables = doc.select("div .row .col-xs-12");

        final List<Proxy> proxies = tables.stream().map(table -> extractProxies(table)).flatMap(l -> l.stream())
                .collect(Collectors.toList());

        return proxies;
    }

    private List<Proxy> extractProxies(Element table) {
        return Arrays.stream(table.text().split("\\s")).map(s -> {
            final String[] token = s.split(":");
            Proxy proxy = new Proxy("http", token[0], Integer.valueOf(token[1]));
            proxy.setSource(Source.HAOIP.name());

            return proxy;

        }).collect(Collectors.toList());
    }

}
