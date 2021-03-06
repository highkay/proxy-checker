package com.mrkid.proxy.checker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrkid.proxy.dto.Proxy;
import io.reactivex.Flowable;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * User: xudong
 * Date: 31/10/2016
 * Time: 3:39 PM
 */
@Component
public class ProxyChecker {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CloseableHttpAsyncClient httpclient;

    private static final Logger logger = LoggerFactory.getLogger(ProxyChecker.class);

    public Flowable<String> getProxyResponse(String originIp,
                                             String proxyCheckerUrl, Proxy proxy) {
        final HttpPost request = new HttpPost(proxyCheckerUrl + "?originIp=" + originIp);
        try {
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(proxy), ContentType.APPLICATION_JSON));
        } catch (JsonProcessingException e) {
        }

        return toFlowable(asyncCheck(proxyCheckerUrl, proxy, request));
    }

    public Flowable<String> generalGet(String targetUrl, Proxy proxy) {
        final HttpGet request = new HttpGet(targetUrl);
        return toFlowable(asyncCheck(targetUrl, proxy, request));
    }

    private CompletableFuture<String> asyncCheck(String targetUrl, Proxy proxy, HttpRequestBase request) {

        CompletableFuture<String> promise = new CompletableFuture<>();

        HttpContext httpContext = HttpClientContext.create();

        logger.info("check proxy: " + proxy + " for url " + targetUrl);

        if (proxy.getSchema().equalsIgnoreCase("socks5") || proxy.getSchema().equalsIgnoreCase("socks4")) {
            httpContext.setAttribute("socks.address", new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        } else if (proxy.getSchema().equalsIgnoreCase("http") || proxy.getSchema().equalsIgnoreCase("https")) {
            RequestConfig config = RequestConfig.custom()
                    .setProxy(new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getSchema().toLowerCase()))
                    .build();

            request.setConfig(config);
        }

        httpclient.execute(request, httpContext, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    final String message = "status code is " + httpResponse.getStatusLine
                            ().getStatusCode();
                    logger.error(message);
                    promise.completeExceptionally(new RuntimeException(message));
                } else {
                    try {
                        promise.complete(IOUtils.toString(httpResponse.getEntity().getContent(), "utf-8"));
                    } catch (IOException e) {
                        logger.error("unable to parse check response of " + httpResponse.getEntity(), e);

                        promise.completeExceptionally(e);
                    }
                }
            }

            @Override
            public void failed(Exception e) {
                logger.error("failed to visit " + targetUrl + " through " + proxy + " caused by " + e.getMessage(), e);
                promise.completeExceptionally(e);
            }

            @Override
            public void cancelled() {
                promise.cancel(false);
            }
        });

        return promise;
    }

    private <T> Flowable<T> toFlowable(CompletableFuture<T> future) {
        return Flowable.<T>defer(() -> emitter ->
                future.whenComplete((result, error) -> {
                    if (error != null) {
                        emitter.onError(error);
                    } else {
                        logger.info("check result: " + result);

                        emitter.onNext(result);
                        emitter.onComplete();
                    }
                })).onExceptionResumeNext(Flowable.empty());
    }
}
