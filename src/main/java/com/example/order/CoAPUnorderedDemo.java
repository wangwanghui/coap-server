package com.example.order;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.TcpConfig;
import org.eclipse.californium.elements.config.UdpConfig;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CoAPUnorderedDemo {

    static class UnorderedResource extends CoapResource {
        private AtomicInteger sequence = new AtomicInteger(0);
        private ExecutorService executor = Executors.newFixedThreadPool(3);
        private Random random = new Random();

        public UnorderedResource(String name) {
            super(name);
            setObservable(true);
            setObserveType(CoAP.Type.CON);
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            // 初始响应
            exchange.respond(CoAP.ResponseCode.CONTENT,
                    "初始值: 0, 观察开始",
                    MediaTypeRegistry.TEXT_PLAIN);

            // 启动多个线程模拟并发更新
            for (int i = 0; i < 5; i++) {
                final int threadId = i;
                executor.execute(() -> {
                    try {
                        // 随机延迟，模拟网络延迟
                        int delay = random.nextInt(1000) + 100;
                        Thread.sleep(delay);

                        int seq = sequence.incrementAndGet();
                        String value = "线程" + threadId + "更新: 顺序" + seq;

                        System.out.println("[服务端] 发送: " + value);

                        // 通知所有观察者
                        // 注意：这是异步的，不保证顺序！
                        this.changed();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        @Override
        public void changed() {
            int currentSeq = sequence.get();
            String payload = "当前序列: " + currentSeq +
                    " 时间戳: " + System.currentTimeMillis();
            // 广播给所有观察者
            super.changed();
        }
    }

    public static void startServer() {
        Configuration config = createCustomConfig();
        CoapServer server = new CoapServer(config);

        server.add(new UnorderedResource("unordered"));
        server.start();
        System.out.println("无序观察服务器已启动");
    }

    public static void main(String[] args) throws InterruptedException {
        startServer();
        Thread.sleep(30000);
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