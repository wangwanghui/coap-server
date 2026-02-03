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
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * ⚡ 极致精简版 CoAP 压测服务端
// * 目标：5w并发，2核8G
// * 原则：去掉一切统计代码，只做收包→回包
// */
//@Slf4j
//public class CoapStressServer {
//
//    private static final Logger logger = LoggerFactory.getLogger(CoapStressServer.class);
//    private static final long START_TIME = System.currentTimeMillis();
//
//    public static void main(String[] args) {
//        try {
//            // 设置高性能JVM参数
//            System.setProperty("java.net.preferIPv4Stack", "true");
//            System.setProperty("coap.udp.buffer.size", "65536");
//
//            // 配置高性能参数
//            Configuration config = createOptimizedConfig();
//
//            // 创建CoAP服务器
//            CoapServer server = new CoapServer(config);
//
//            // 只添加核心资源
//            server.add(new OptimizedEchoResource());
//            //server.add(new SimpleStatusResource()); // 使用修复版状态资源
//
//            // 启动服务器
//            server.start();
//
//            logger.info("🚀 高性能CoAP服务器已启动 (端口: 5683)");
//            logger.info("📊 支持资源路径: /echo, /status");
//
//        } catch (Exception e) {
//            logger.error("服务器启动失败", e);
//            System.exit(1);
//        }
//    }
//
//    /**
//     * 创建优化配置
//     */
//    private static Configuration createOptimizedConfig() {
//        Configuration config = new Configuration();
//
//        // 线程数优化
//        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, 8);
//
//        // 网络参数优化
//        config.set(CoapConfig.COAP_PORT, 5683);
//        config.set(CoapConfig.MAX_ACTIVE_PEERS, 500000);
//        config.set(CoapConfig.MAX_MESSAGE_SIZE, 65536);
//        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
//
//        // 超时和重传优化
//        config.set(CoapConfig.ACK_TIMEOUT, 2, TimeUnit.SECONDS);
//        config.set(CoapConfig.MAX_RETRANSMIT, 1);
//        config.set(CoapConfig.EXCHANGE_LIFETIME, 10, TimeUnit.SECONDS);
//
//        // 内存优化
//        config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 65536);
//        config.set(CoapConfig.NOTIFICATION_CHECK_INTERVAL_TIME, 300, TimeUnit.SECONDS);
//
//        return config;
//    }
//
//    /**
//     * ⚡ 优化版回显资源 - 极致性能 ⚡
//     */
//    public static class OptimizedEchoResource extends CoapResource {
//        // 使用线程局部变量减少内存分配
//        private static final ThreadLocal<StringBuilder> threadLocalBuilder =
//                ThreadLocal.withInitial(() -> new StringBuilder(128));
//
//        public OptimizedEchoResource() {
//            super("echo");
//            setObservable(false);
//        }
//
//        @Override
//        public void handlePOST(CoapExchange exchange) {
//            try {
//                byte[] requestPayload = exchange.getRequestPayload();
//
//                StringBuilder sb = threadLocalBuilder.get();
//                sb.setLength(0);
//                sb.append("{\"status\":\"ok\"");
//                if (requestPayload != null && requestPayload.length > 0) {
//                    sb.append(",\"size\":").append(requestPayload.length);
//                }
//                sb.append("}");
//
//                exchange.respond(CoAP.ResponseCode.CONTENT,
//                        sb.toString(),
//                        MediaTypeRegistry.APPLICATION_JSON);
//
//            } catch (Exception e) {
//                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR,
//                        "{\"error\":\"server_error\"}",
//                        MediaTypeRegistry.APPLICATION_JSON);
//            }
//            // 🔴 完全删除统计代码
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            exchange.respond(CoAP.ResponseCode.CONTENT,
//                    "{\"method\":\"use_post\"}",
//                    MediaTypeRegistry.APPLICATION_JSON);
//        }
//    }
//
//    /**
//     * ✅ 修复版状态资源 - 返回固定信息，不依赖统计变量
//     */
//    public static class SimpleStatusResource extends CoapResource {
//        public SimpleStatusResource() {
//            super("status");
//            setObservable(false);
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            long uptime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - START_TIME);
//
//            StringBuilder response = new StringBuilder(128);
//            response.append("{\n")
//                    .append("  \"status\": \"running\",\n")
//                    .append("  \"total_requests\": 0,\n")  // 固定值，不再统计
//                    .append("  \"current_concurrent\": 0,\n") // 固定值
//                    .append("  \"peak_concurrent\": 0,\n")   // 固定值
//                    .append("  \"uptime_seconds\": ").append(uptime).append("\n")
//                    .append("}");
//
//            exchange.respond(CoAP.ResponseCode.CONTENT,
//                    response.toString(),
//                    MediaTypeRegistry.APPLICATION_JSON);
//        }
//    }
//}