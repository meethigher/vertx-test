package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VertxTCPReverseProxy extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(VertxTCPReverseProxy.class);

    private final String sourceHost;

    private final int sourcePort;

    private final String targetHost;

    private final int targetPort;

    public VertxTCPReverseProxy(String sourceHost, int sourcePort, String targetHost, int targetPort) {
        this.sourceHost = sourceHost;
        this.sourcePort = sourcePort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    @Override
    public void start() throws Exception {
        NetServer netServer = vertx.createNetServer();
        NetClient netClient = vertx.createNetClient();
        netServer.connectHandler(sourceSocket -> {
                    netClient.connect(targetPort, targetHost)
                            .onSuccess(targetSocket -> {
                                log.info("{} connected, proxy to {}", sourceSocket.remoteAddress().toString(), targetSocket.remoteAddress().toString());
                                sourceSocket.handler(buffer -> {
                                    targetSocket.write(buffer)
                                            .onSuccess(t -> {
                                                log.info("source-->target: {}", buffer);
                                            })
                                            .onFailure(e -> {
                                                log.error("source-->target error", e);
                                            });
                                }).closeHandler(t -> {
                                    log.warn("source closed");
                                }).exceptionHandler(e -> {
                                    log.error("source error", e);
                                });
                                targetSocket.handler(buffer -> {
                                    sourceSocket.write(buffer)
                                            .onSuccess(t -> {
                                                log.info("target-->source: {}", buffer);
                                            })
                                            .onFailure(e -> {
                                                log.error("target-->source error", e);
                                            });
                                }).closeHandler(t -> {
                                    log.warn("target closed");
                                }).exceptionHandler(e -> {
                                    log.error("target error", e);
                                });
                            })
                            .onFailure(e -> {
                                log.error("error", e);
                                sourceSocket.close();
                            });
                }).exceptionHandler(e -> log.error("connect failed", e)).listen(sourcePort, sourceHost)
                .onSuccess(r -> log.info("proxy server started {}:{} <--> {}:{}", sourceHost, sourcePort, targetHost, targetPort))
                .onFailure(e -> log.error("proxy server started error", e));
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new VertxTCPReverseProxy("127.0.0.1", 22, "10.0.0.9", 5432));
    }

}
