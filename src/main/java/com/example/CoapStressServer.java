//package com.example;
//
//import lombok.extern.slf4j.Slf4j;
//import org.eclipse.californium.core.CoapResource;
//import org.eclipse.californium.core.CoapServer;
//import org.eclipse.californium.core.coap.CoAP;
//import org.eclipse.californium.core.coap.MediaTypeRegistry;
//import org.eclipse.californium.core.config.CoapConfig;
//import org.eclipse.californium.core.server.resources.CoapExchange;
//import org.eclipse.californium.elements.config.Configuration;
//import org.eclipse.californium.elements.config.SystemConfig;
//import org.eclipse.californium.elements.config.TcpConfig;
//import org.eclipse.californium.elements.config.UdpConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;
////*
//// * 专为 20w 并发压测设计的 CoAP 服务端
////
////
////*
//// * 极简版 CoAP 压测服务端
//// * 只负责：收数据 -> 回响应
//// * 不包含任何统计和监控代码
//
//
//
//public class CoapStressServer {
//    private static final Logger log = LoggerFactory.getLogger(CoapStressServer.class);
//    private static final AtomicLong requestCount = new AtomicLong(0);
//    private static final AtomicInteger activeConnections = new AtomicInteger(0);
//    private static final long startTime = System.currentTimeMillis();
//
//    public static void main(String[] args) {
//        System.out.println("启动 CoAP 服务器 (Californium 3.x)...");
//
//        try {
//            //CoapConfig.register();
//            // ========== 创建配置 ==========
//            // 方法2：或者创建自定义配置（需要注册模块）
//            Configuration config = createCustomConfig();
//
//            // ========== 创建服务器 ==========
//            CoapServer server = new CoapServer(config);
//
//            // 添加资源
//            server.add(new BenchmarkResource());
//            server.add(new EchoResource());
//            server.add(new StatsResource());
//            log.error("==================================================");
//            log.info("CoAP 服务器已启动 (Californium 3.x)");
//            log.info("端口: 5683");
//            System.out.println(config.get(CoapConfig.MAX_ACTIVE_PEERS));
//            System.out.println(config.get(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT));
//            log.info("==================================================");
//            System.out.println(config.get(CoapConfig.PREFERRED_BLOCK_SIZE));
//            System.out.println(config.get(CoapConfig.BLOCKWISE_STATUS_LIFETIME, TimeUnit.SECONDS));
//            log.info("✅ UDP_DATAGRAM_SIZE: {}", config.get(UdpConfig.UDP_DATAGRAM_SIZE));
//            // 启动服务器
//            server.start();
//
//        } catch (Exception e) {
//            log.error("服务器启动失败", e);
//            System.exit(1);
//        }
//    }
//
//    // ========== 如果你的版本需要手动注册配置 ==========
//    private static Configuration createCustomConfig() {
//        // 创建空的配置
//        Configuration config = new Configuration();
//
//        // 必须注册配置模块
//        CoapConfig.register();
//        UdpConfig.register();
//        TcpConfig.register();
//
//        // 然后设置配置值...
//        config.set(CoapConfig.COAP_PORT, 5683);
//        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, 32);
//        // ... 其他配置
//        // ========== 基础配置 ==========
//        // ========== 块传输配置（核心修改） ==========
//        config.set(CoapConfig.MAX_MESSAGE_SIZE, 128 * 1024);           // 64KB
//        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 16 * 1024);    // 64KB
//        config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 10 * 1024 * 1024);
//        // ========== 块传输配置强化 ==========
//        config.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 300, TimeUnit.SECONDS); // 延长到5分钟
//        config.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, false); // 允许更灵活的块处理
//// ========== UDP 配置 ==========
//        config.set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, 64 * 1024 * 1024);
//        config.set(UdpConfig.UDP_SEND_BUFFER_SIZE, 64 * 1024 * 1024);
//        config.set(UdpConfig.UDP_RECEIVER_THREAD_COUNT, 8);
//        config.set(UdpConfig.UDP_SENDER_THREAD_COUNT, 8);
//        config.set(UdpConfig.UDP_DATAGRAM_SIZE, 128 * 1024);   // 必须 >= 你期望的最大块大小
//// ========== 并发连接配置 ==========
//        config.set(CoapConfig.MAX_ACTIVE_PEERS, 100000);
//        config.set(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, 30, TimeUnit.SECONDS);
//
//// ========== 线程池配置 ==========
//        int cores = Runtime.getRuntime().availableProcessors();
//        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, Math.max(32, cores * 2));
//
//// ========== 超时和重传配置 ==========
//        config.set(CoapConfig.ACK_TIMEOUT, 30, TimeUnit.SECONDS);
//        config.set(CoapConfig.ACK_INIT_RANDOM, 1.5f);
//        config.set(CoapConfig.MAX_RETRANSMIT, 2);
//        config.set(CoapConfig.ACK_TIMEOUT_SCALE, 2.0f);
//        config.set(CoapConfig.EXCHANGE_LIFETIME, 300, TimeUnit.SECONDS);
//
//// ========== 交换生命周期 ==========
//        config.set(CoapConfig.NON_LIFETIME, 10, TimeUnit.SECONDS);
//        config.set(CoapConfig.NSTART, 1);
//
//// ========== 其他配置 ==========
//        config.set(CoapConfig.MAX_LATENCY, 10, TimeUnit.SECONDS);
//        return config;
//    }
//
//
//
//    static class BenchmarkResource extends CoapResource {
//        public BenchmarkResource() {
//            super("benchmark");
//            setObservable(false);
//            log.info("BenchmarkResource资源初始化完成: {}", "benchmark");
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            long start = System.nanoTime();
//            requestCount.incrementAndGet();
//            activeConnections.incrementAndGet();
//
//            try {
//                // 极简响应 - 不要打印日志！
//                exchange.respond(CoAP.ResponseCode.CONTENT, "OK");
//            } finally {
//                activeConnections.decrementAndGet();
//            }
//
//            // 偶尔打印统计（每1000个请求）
//            long count = requestCount.get();
//            if (count % 1000 == 0) {
//                long latency = (System.nanoTime() - start) / 1000; // 微秒
//                log.info("处理 {} 个请求，当前延迟: {}µs", count, latency);
//            }
//        }
//
//        @Override
//        public void handlePOST(CoapExchange exchange) {
//            requestCount.incrementAndGet();
//            activeConnections.incrementAndGet();
//
//            try {
//                byte[] payload = exchange.getRequestPayload();
//                // 添加调试日志
//                log.info("收到POST请求，payload长度: {}字节", payload.length);
//                // 只返回字节数，不处理内容
//                String response = String.valueOf(payload.length);
//                exchange.respond(CoAP.ResponseCode.CONTENT, response);
//            } finally {
//                activeConnections.decrementAndGet();
//            }
//        }
//    }
//
//
//
//
//    static class EchoResource extends CoapResource {
//        public EchoResource() {
//            super("echo");
//            log.info("echo资源初始化完成: {}", "echo");
//        }
//
//        @Override
//        public void handlePOST(CoapExchange exchange) {
//            byte[] payload = exchange.getRequestPayload();
//
//            // ✅ 详细调试信息
//            System.out.println("=== 服务端收到请求 ===");
//            System.out.println("请求方法: " + exchange.getRequestCode());
//            System.out.println("Payload长度: " + payload.length + "字节");
//            System.out.println("请求来源: " + exchange.getSourceSocketAddress());
//            System.out.println("请求选项: " + exchange.getRequestOptions());
//
//            try {
//                // 检查是否使用了块传输
//                if (exchange.getRequestOptions().hasBlock1()) {
//                    System.out.println("✅ 检测到块传输，当前块: " +
//                            exchange.getRequestOptions().getBlock1().getNum());
//                }
//
//                exchange.respond(CoAP.ResponseCode.CONTENT, payload);
//                System.out.println("✅ 响应已发送，长度: " + payload.length + "字节");
//
//            } catch (Exception e) {
//                System.err.println("❌ 发送响应失败: " + e.getMessage());
//                e.printStackTrace();
//                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
//            }
//        }
//    }
//
//
//
//    static class StatsResource extends CoapResource {
//        public StatsResource() {
//            super("stats");
//            log.info("stats资源初始化完成: {}", "stats");
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            long count = requestCount.get();
//            long uptime = (System.currentTimeMillis() - startTime) / 1000;
//
//            String stats = String.format(
//                    "Server Status:\n" +
//                            "Total Requests: %d\n" +
//                            "Active Connections: %d\n" +
//                            "Uptime: %d seconds\n" +
//                            "QPS: %.2f",
//                    count,
//                    activeConnections.get(),
//                    uptime,
//                    uptime > 0 ? (double)count / uptime : 0
//            );
//
//            exchange.respond(CoAP.ResponseCode.CONTENT, stats);
//        }
//    }
//}
