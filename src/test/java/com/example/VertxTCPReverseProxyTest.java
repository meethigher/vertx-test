package com.example;

import io.vertx.core.Vertx;
import org.junit.Test;

import static org.junit.Assert.*;

public class VertxTCPReverseProxyTest {

    @Test
    public void test() {
        Vertx.vertx().deployVerticle(new VertxTCPReverseProxy("0.0.0.0", 22, "10.0.0.9", 5432));
    }
}