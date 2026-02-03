package com.example.order;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.TcpConfig;
import org.eclipse.californium.elements.config.UdpConfig;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UnorderedClient {

    public static void demonstrateUnordered() {
        System.out.println("=== 演示CoAP Observe的无序性 ===");

        String uri = "coap://localhost:5683/unordered";
        Configuration config = createCustomConfig();
        CoapClient client = new CoapClient(uri);
        CoapEndpoint endpoint = CoapEndpoint.builder()
                .setConfiguration(config)
                .setInetSocketAddress(new InetSocketAddress(0)) // 随机端口
                .build();
        client.setEndpoint(endpoint);
        final List<String> receivedMessages = new ArrayList<>();
        final List<Long> timestamps = new ArrayList<>();

        // 创建观察关系
        CoapObserveRelation relation = client.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String content = response.getResponseText();
                long timestamp = System.currentTimeMillis();

                synchronized (receivedMessages) {
                    receivedMessages.add(content);
                    timestamps.add(timestamp);

                    System.out.println("[客户端] 收到: " + content);

                    // 检查顺序
                    if (receivedMessages.size() > 1) {
                        long prevTime = timestamps.get(timestamps.size() - 2);
                        if (timestamp < prevTime) {
                            System.out.println("  ⚠️  时间戳逆序！");
                        }
                    }
                }
            }

            @Override
            public void onError() {
                System.err.println("[客户端] 观察错误");
            }
        });

        // 等待接收消息
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 分析结果
        System.out.println("\n=== 接收结果分析 ===");
        System.out.println("总共收到 " + receivedMessages.size() + " 条消息");

        for (int i = 0; i < receivedMessages.size(); i++) {
            System.out.printf("%2d. %s%n", i + 1, receivedMessages.get(i));
        }

        // 检查是否有乱序
        boolean hasOutOfOrder = false;
        for (int i = 1; i < timestamps.size(); i++) {
            if (timestamps.get(i) < timestamps.get(i - 1)) {
                hasOutOfOrder = true;
                long diff = timestamps.get(i - 1) - timestamps.get(i);
                System.out.printf("消息 %d 比消息 %d 早到达 %d ms%n",
                        i + 1, i, diff);
            }
        }

        if (hasOutOfOrder) {
            System.out.println("\n❌ 检测到消息乱序！");
        } else {
            System.out.println("\n✅ 消息按到达顺序有序");
        }

        relation.proactiveCancel();
        client.shutdown();
    }

    public static void main(String[] args) {
        demonstrateUnordered();
    }

    private static Configuration createCustomConfig() {
        // 创建空的配置
        Configuration config = new Configuration();

        // 必须注册配置模块
        CoapConfig.register();
        UdpConfig.register();
        TcpConfig.register();

        // 然后设置配置值...
        config.set(CoapConfig.COAP_PORT, 5683);
        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, 32);
        // ... 其他配置
        // ========== 基础配置 ==========
        // ========== 块传输配置（核心修改） ==========
        config.set(CoapConfig.MAX_MESSAGE_SIZE, 128 * 1024);           // 64KB
        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 16 * 1024);    // 64KB
        config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 10 * 1024 * 1024);
        // ========== 块传输配置强化 ==========
        config.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 300, TimeUnit.SECONDS); // 延长到5分钟
        config.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, false); // 允许更灵活的块处理
// ========== UDP 配置 ==========
        config.set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, 64 * 1024 * 1024);
        config.set(UdpConfig.UDP_SEND_BUFFER_SIZE, 64 * 1024 * 1024);
        config.set(UdpConfig.UDP_RECEIVER_THREAD_COUNT, 8);
        config.set(UdpConfig.UDP_SENDER_THREAD_COUNT, 8);
        config.set(UdpConfig.UDP_DATAGRAM_SIZE, 128 * 1024);   // 必须 >= 你期望的最大块大小
// ========== 并发连接配置 ==========
        config.set(CoapConfig.MAX_ACTIVE_PEERS, 100000);
        config.set(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, 30, TimeUnit.SECONDS);

// ========== 线程池配置 ==========
        int cores = Runtime.getRuntime().availableProcessors();
        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, Math.max(32, cores * 2));

// ========== 超时和重传配置 ==========
        config.set(CoapConfig.ACK_TIMEOUT, 1, TimeUnit.SECONDS);
        config.set(CoapConfig.ACK_INIT_RANDOM, 1.5f);
        config.set(CoapConfig.MAX_RETRANSMIT, 2);
        config.set(CoapConfig.ACK_TIMEOUT_SCALE, 2.0f);
        config.set(CoapConfig.EXCHANGE_LIFETIME, 300, TimeUnit.SECONDS);

// ========== 交换生命周期 ==========
        config.set(CoapConfig.NON_LIFETIME, 10, TimeUnit.SECONDS);
        config.set(CoapConfig.NSTART, 1);

// ========== 其他配置 ==========
        config.set(CoapConfig.MAX_LATENCY, 10, TimeUnit.SECONDS);
        return config;
    }
}