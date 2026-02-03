//package com.example;
//
//import org.eclipse.californium.core.CoapServer;
//import org.eclipse.californium.core.coap.CoAP;
//import org.eclipse.californium.core.config.CoapConfig;
//import org.eclipse.californium.core.server.resources.CoapExchange;
//import org.eclipse.californium.core.server.resources.ConcurrentCoapResource;
//import org.eclipse.californium.elements.config.Configuration;
//import org.eclipse.californium.elements.config.TcpConfig;
//import org.eclipse.californium.elements.config.UdpConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * 使用 ConcurrentCoapResource 的简单实现
// */
//public class SimpleObservableServer {
//    private static final Logger logger = LoggerFactory.getLogger(SimpleObservableServer.class);
//
//    public static void main(String[] args) {
//        Configuration config = createCustomConfig();
//        CoapServer server = new CoapServer(config);
//        // ========== 创建服务器 ==========
//        // 添加资源
//        server.add(new CoapStressServer.BenchmarkResource());
//        server.add(new CoapStressServer.EchoResource());
//        server.add(new CoapStressServer.StatsResource());
//        server.add(new DeviceObservableResource());
//        logger.error("==================================================");
//        logger.info("CoAP 服务器已启动 (Californium 3.x)");
//        logger.info("端口: 5683");
//        System.out.println(config.get(CoapConfig.MAX_ACTIVE_PEERS));
//        System.out.println(config.get(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT));
//        logger.info("==================================================");
//        System.out.println(config.get(CoapConfig.PREFERRED_BLOCK_SIZE));
//        System.out.println(config.get(CoapConfig.BLOCKWISE_STATUS_LIFETIME, TimeUnit.SECONDS));
//        logger.info("✅ UDP_DATAGRAM_SIZE: {}", config.get(UdpConfig.UDP_DATAGRAM_SIZE));
//        // 启动服务器
////        server.add(new SimpleObservableResource("time"));
////        server.add(new SimpleObservableResource("data", 2)); // 2个线程处理
//        server.start();
//    }
//
//    /**
//     * 使用 ConcurrentCoapResource
//     */
//    static class SimpleObservableResource extends ConcurrentCoapResource {
//        private final AtomicInteger counter = new AtomicInteger(0);
//        private final Timer timer = new Timer(true);
//        private int observerCount = 0;
//
//        public SimpleObservableResource(String name) {
//            this(name, 1);
//        }
//
//        public SimpleObservableResource(String name, int threads) {
//            super(name, threads);
//            setObservable(true);
//            setObserveType(CoAP.Type.CON);
//            startUpdateTask();
//        }
//
//        private void startUpdateTask() {
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    int value = counter.incrementAndGet();
//
//                    // 这里不能直接获取观察者数量
//                    // 但我们可以通过其他方式跟踪
//                    logger.info("资源 '{}' 值更新为: {}", getName(), value);
//
//                    // 通知观察者
//                    changed();
//                }
//            }, 1000, 2000);
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            String response = String.format("值: %s, 名称: %s",
//                    UUID.randomUUID().toString(), getName());
//            exchange.respond(CoAP.ResponseCode.CONTENT, response);
//        }
//
//        // 重写方法来跟踪观察者
//        @Override
//        public void addObserveRelation(org.eclipse.californium.core.observe.ObserveRelation relation) {
//            super.addObserveRelation(relation);
//            observerCount++;
//            logger.info("资源 '{}' 增加观察者，当前: {}", getName(), observerCount);
//        }
//
//        @Override
//        public void removeObserveRelation(org.eclipse.californium.core.observe.ObserveRelation relation) {
//            super.removeObserveRelation(relation);
//            observerCount--;
//            logger.info("资源 '{}' 减少观察者，当前: {}", getName(), observerCount);
//        }
//
//        public boolean hasObservers() {
//            return observerCount > 0;
//        }
//    }
//
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
//        config.set(CoapConfig.ACK_TIMEOUT, 2, TimeUnit.SECONDS);
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
//}