/*
package com.example;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.TcpConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
*/
/**
 * 专为 20w 并发压测设计的 CoAP 服务端
 *//*

*/
/**
 * 极简版 CoAP 压测服务端
 * 只负责：收数据 -> 回响应
 * 不包含任何统计和监控代码
 *//*

@Slf4j
public class CoapStressServer {

    private static final AtomicLong requestCount = new AtomicLong(0);
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final long startTime = System.currentTimeMillis();

    public static void main(String[] args) {
        log.info("启动 CoAP 服务器 (Californium 3.x)...");

        try {
            // ========== 创建配置 ==========
            // 方法2：或者创建自定义配置（需要注册模块）
            Configuration config = createCustomConfig();

            // ========== 基础配置 ==========
            // ========== 块传输配置（核心修改） ==========
            config.set(CoapConfig.MAX_MESSAGE_SIZE, 64 * 1024);           // 64KB
            config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 64 * 1024);       // 64KB
            config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 10 * 1024 * 1024);
            config.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 60, TimeUnit.SECONDS);
            config.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, false);

// ========== UDP 配置 ==========
            config.set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, 64 * 1024 * 1024);
            config.set(UdpConfig.UDP_SEND_BUFFER_SIZE, 64 * 1024 * 1024);
            config.set(UdpConfig.UDP_RECEIVER_THREAD_COUNT, 8);
            config.set(UdpConfig.UDP_SENDER_THREAD_COUNT, 8);

// ========== 并发连接配置 ==========
            config.set(CoapConfig.MAX_ACTIVE_PEERS, 100000);
            config.set(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, 30, TimeUnit.SECONDS);

// ========== 线程池配置 ==========
            int cores = Runtime.getRuntime().availableProcessors();
            config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, Math.max(32, cores * 2));

// ========== 超时和重传配置 ==========
            config.set(CoapConfig.ACK_TIMEOUT, 5, TimeUnit.SECONDS);
            config.set(CoapConfig.ACK_INIT_RANDOM, 1.5f);
            config.set(CoapConfig.MAX_RETRANSMIT, 2);
            config.set(CoapConfig.ACK_TIMEOUT_SCALE, 2.0f);
            config.set(CoapConfig.EXCHANGE_LIFETIME, 30, TimeUnit.SECONDS);

// ========== 交换生命周期 ==========
            config.set(CoapConfig.NON_LIFETIME, 10, TimeUnit.SECONDS);
            config.set(CoapConfig.NSTART, 1);

// ========== 其他配置 ==========
            config.set(CoapConfig.MAX_LATENCY, 10, TimeUnit.SECONDS);

            // ========== 创建服务器 ==========
            CoapServer server = new CoapServer(config);

            // 添加资源
            server.add(new BenchmarkResource());
            server.add(new EchoResource());
            server.add(new StatsResource());

            // 启动服务器
            server.start();

            log.info("==================================================");
            log.info("CoAP 服务器已启动 (Californium 3.x)");
            log.info("端口: 5683");
            log.info("最大连接数: {}", config.get(CoapConfig.MAX_ACTIVE_PEERS));
            log.info("协议线程数: {}", config.get(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT));
            log.info("==================================================");

            // 保持运行
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("服务器关闭，总处理请求数: {}", requestCount.get());
                server.stop();
                server.destroy();
            }));

            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("服务器启动失败", e);
            System.exit(1);
        }
    }

    // ========== 如果你的版本需要手动注册配置 ==========
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

        return config;
    }

    */
/**
     * 基准测试资源
     *//*

    static class BenchmarkResource extends CoapResource {
        public BenchmarkResource() {
            super("benchmark");
            setObservable(false);
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            long start = System.nanoTime();
            requestCount.incrementAndGet();
            activeConnections.incrementAndGet();

            try {
                // 极简响应 - 不要打印日志！
                exchange.respond(CoAP.ResponseCode.CONTENT, "OK");
            } finally {
                activeConnections.decrementAndGet();
            }

            // 偶尔打印统计（每1000个请求）
            long count = requestCount.get();
            if (count % 1000 == 0) {
                long latency = (System.nanoTime() - start) / 1000; // 微秒
                log.info("处理 {} 个请求，当前延迟: {}µs", count, latency);
            }
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            requestCount.incrementAndGet();
            activeConnections.incrementAndGet();

            try {
                byte[] payload = exchange.getRequestPayload();
                // 只返回字节数，不处理内容
                String response = String.valueOf(payload.length);
                exchange.respond(CoAP.ResponseCode.CONTENT, response);
            } finally {
                activeConnections.decrementAndGet();
            }
        }
    }

    */
/**
     * 回显资源
     *//*

    static class EchoResource extends CoapResource {
        public EchoResource() {
            super("echo");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            exchange.respond("Send POST with data");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            byte[] payload = exchange.getRequestPayload();
            exchange.respond(CoAP.ResponseCode.CONTENT, payload);
        }
    }

    */
/**
     * 统计资源
     *//*

    static class StatsResource extends CoapResource {
        public StatsResource() {
            super("stats");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            long count = requestCount.get();
            long uptime = (System.currentTimeMillis() - startTime) / 1000;

            String stats = String.format(
                    "Server Status:\n" +
                            "Total Requests: %d\n" +
                            "Active Connections: %d\n" +
                            "Uptime: %d seconds\n" +
                            "QPS: %.2f",
                    count,
                    activeConnections.get(),
                    uptime,
                    uptime > 0 ? (double)count / uptime : 0
            );

            exchange.respond(CoAP.ResponseCode.CONTENT, stats);
        }
    }
}*/
