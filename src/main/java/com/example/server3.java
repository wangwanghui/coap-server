//package com.example;
//
//import org.eclipse.californium.core.CoapResource;
//import org.eclipse.californium.core.CoapServer;
//import org.eclipse.californium.core.coap.CoAP;
//import org.eclipse.californium.core.coap.MediaTypeRegistry;
//import org.eclipse.californium.core.config.CoapConfig;
//import org.eclipse.californium.core.server.resources.CoapExchange;
//import org.eclipse.californium.elements.config.Configuration;
//import org.eclipse.californium.elements.config.UdpConfig;
//import org.eclipse.californium.elements.util.StandardCharsets;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Arrays;
//import java.util.concurrent.TimeUnit;
//
//public class server3 {
//    private static final Logger logger = LoggerFactory.getLogger(server3.class);
//
//    public static void main(String[] args) {
//        try {
//            // 🔥 30w QPS极限优化：JVM启动参数（必须在启动时设置）
//            //setJvmUltraParams();
//
//            // 创建极致优化配置
//            Configuration config = createUltraOptimizedConfig();
//
//            // 创建CoAP服务器
//            CoapServer server = new CoapServer(config);
//
//            // 只保留核心资源（减少开销）
//            server.add(new UltraEchoResource());
//
//            // 启动服务器
//            server.start();
//
//            logger.info("🚀 30w QPS极限版CoAP服务器已启动 (端口: 5683)");
//            logger.info("🔥 目标QPS: 300,000");
//            logger.info("📊 支持资源路径: /echo");
//
//        } catch (Exception e) {
//            logger.error("服务器启动失败", e);
//            System.exit(1);
//        }
//    }
//
//
//    /**
//     * 🔥 30w QPS极限配置
//     */
//    private static Configuration createUltraOptimizedConfig() {
//        Configuration config = new Configuration();
//
//        // ========== 🚀 线程池极限优化 ==========
//        int availableProcessors = Runtime.getRuntime().availableProcessors();
//        int ultraThreads = Math.max(64, availableProcessors * 8);  // 核心数×8
//        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, ultraThreads);
//
//        // UDP接收/发送线程（关键！）
//        config.set(UdpConfig.UDP_RECEIVER_THREAD_COUNT, 16);
//        config.set(UdpConfig.UDP_SENDER_THREAD_COUNT, 16);
//
//        // ========== 🌐 网络层极限优化 ==========
//        config.set(CoapConfig.COAP_PORT, 5683);
//        config.set(CoapConfig.MAX_ACTIVE_PEERS, 500000);  // 50w并发连接
//
//        // 消息大小极限
//        config.set(CoapConfig.MAX_MESSAGE_SIZE, 256 * 1024);      // 256KB
//        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 64 * 1024);   // 64KB
//        config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 10 * 1024 * 1024); // 10MB
//
//        // ========== ⚡ 超时和重传优化 ==========
//        config.set(CoapConfig.ACK_TIMEOUT, 1, TimeUnit.SECONDS);  // 1秒超时
//        config.set(CoapConfig.MAX_RETRANSMIT, 1);                 // 1次重传
//        config.set(CoapConfig.ACK_TIMEOUT_SCALE, 1.5f);
//        config.set(CoapConfig.EXCHANGE_LIFETIME, 5, TimeUnit.SECONDS); // 5秒交换生命周期
//
//        // ========== 🔄 块传输优化 ==========
//        config.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 30, TimeUnit.SECONDS);
//        config.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, false);
//
//        // ========== 🧠 内存和GC优化 ==========
//        config.set(CoapConfig.NOTIFICATION_CHECK_INTERVAL_TIME, 600, TimeUnit.SECONDS); // 10分钟
//       // config.set(CoapConfig.OBSERVATION_REUSE_ADDRESS, true);
//
//        // ========== 📊 统计和监控优化 ==========
//       // config.set(CoapConfig.HEALTH_STATUS_PRINT_LEVEL, 0);  // 关闭内部统计
//
//        logger.info("🔥 30w QPS极限配置已生效:");
//        logger.info("   协议线程数: {}", ultraThreads);
//        logger.info("   最大连接数: 500000");
//        logger.info("   最大消息大小: 256KB");
//        logger.info("   ACK超时: 1秒");
//
//        return config;
//    }
//
//    /**
//     * ⚡ 30w QPS极限版回显资源（极致性能）
//     */
//    public static class UltraEchoResource extends CoapResource {
//        // 使用字节数组常量，避免字符串拼接
//        private static final byte[] OK_RESPONSE = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
//        private static final byte[] ERROR_RESPONSE = "{\"error\":\"server_error\"}".getBytes(StandardCharsets.UTF_8);
//
//        // 线程局部变量预分配
//        private static final ThreadLocal<byte[]> responseBuffer =
//                ThreadLocal.withInitial(() -> new byte[1024]);
//
//        public UltraEchoResource() {
//            super("echo");
//            setObservable(false);
//        }
//
//        @Override
//        public void handlePOST(CoapExchange exchange) {
//            try {
//                byte[] payload = exchange.getRequestPayload();
//
//                // 🔥 极致优化：直接返回预分配响应，零内存分配
//                if (payload == null || payload.length == 0) {
//                    exchange.respond(CoAP.ResponseCode.CONTENT, OK_RESPONSE,
//                            MediaTypeRegistry.APPLICATION_JSON);
//                } else {
//                    // 如果有payload，快速构造响应（避免StringBuilder）
//                    byte[] response = responseBuffer.get();
//                    System.arraycopy(OK_RESPONSE, 0, response, 0, OK_RESPONSE.length);
//                    // 简单追加payload长度信息
//                    String sizeInfo = ",\"size\":" + payload.length + "}";
//                    byte[] sizeBytes = sizeInfo.getBytes(StandardCharsets.UTF_8);
//                    System.arraycopy(sizeBytes, 0, response, OK_RESPONSE.length - 1, sizeBytes.length);
//
//                    exchange.respond(CoAP.ResponseCode.CONTENT,
//                            Arrays.copyOf(response, OK_RESPONSE.length + sizeBytes.length - 1),
//                            MediaTypeRegistry.APPLICATION_JSON);
//                }
//            } catch (Exception e) {
//                // 极致性能：不记录错误日志
//                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR,
//                        ERROR_RESPONSE, MediaTypeRegistry.APPLICATION_JSON);
//            }
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            // 直接返回预分配响应
//            exchange.respond(CoAP.ResponseCode.CONTENT,
//                    "{\"method\":\"use_post\"}".getBytes(StandardCharsets.UTF_8),
//                    MediaTypeRegistry.APPLICATION_JSON);
//        }
//    }
//}
