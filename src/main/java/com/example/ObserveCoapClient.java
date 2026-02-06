package com.example;


import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.UdpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ObserveCoapClient {


    private static final Configuration config = new Configuration();
    static {
        CoapConfig.register();
        UdpConfig.register();

        // ✅ 彻底解决UDP大小限制
        config.set(CoapConfig.MAX_MESSAGE_SIZE, 128 * 1024);           // 128KB
        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 16 * 1024);        // 16KB块大小

        // ✅ 关键修复：大幅提高UDP数据报大小
        config.set(UdpConfig.UDP_DATAGRAM_SIZE, 128 * 1024);            // 64KB！直接设置最大值

        // 超时配置
        config.set(CoapConfig.ACK_TIMEOUT, 1, TimeUnit.SECONDS);      // 30秒
        config.set(CoapConfig.MAX_RETRANSMIT, 1);
        config.set(CoapConfig.EXCHANGE_LIFETIME, 2, TimeUnit.SECONDS); // 5分钟
        //config.set(CoapConfig.DEFAULT_BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER,false);
    }
    private static final Logger logger = LoggerFactory.getLogger(ObserveCoapClient.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    public static void main(String[] args) throws InterruptedException {
        String serverUri = "coap://localhost:5683";
        String resource = "temperature"; // 默认订阅时间资源

        if (args.length > 0) {
            serverUri = args[0];
        }
        if (args.length > 1) {
            resource = args[1];
        }

        String uri = serverUri + "/" + resource;
        logger.info("开始订阅: {}", uri);
        logger.info("按 Ctrl+C 停止");

        CoapClient client = new CoapClient(uri);
        CoapEndpoint endpoint = CoapEndpoint.builder()
                .setConfiguration(config)
                .setInetSocketAddress(new InetSocketAddress(0)) // 随机端口
                .build();
        client.setEndpoint(endpoint);
        // 观察处理器
        CoapHandler handler = new CoapHandler() {
            private int count = 0;

            @Override
            public void onLoad(CoapResponse response) {
                count++;
                String time = sdf.format(new Date());
                String content = response.getResponseText();

                if (response.isSuccess()) {
                    System.out.printf("[%s] #%d: %s\n", time, count, content);
                } else {
                    System.err.printf("[%s] 错误: %s\n", time, response.getCode());
                }
            }

            @Override
            public void onError() {
                System.err.println("[" + sdf.format(new Date()) + "] 观察错误");
            }
        };

        // 开始观察
        CoapObserveRelation relation = client.observe(handler);

        // 等待用户中断
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 停止观察
        relation.proactiveCancel();
        client.shutdown();
        logger.info("订阅已停止");
    }


}
