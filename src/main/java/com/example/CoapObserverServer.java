//package com.example;
//
//import org.eclipse.californium.core.CoapResource;
//import org.eclipse.californium.core.CoapServer;
//import org.eclipse.californium.core.coap.CoAP;
//import org.eclipse.californium.core.coap.MediaTypeRegistry;
//import org.eclipse.californium.core.server.resources.CoapExchange;
//import org.eclipse.californium.core.server.resources.ConcurrentCoapResource;
//import org.eclipse.californium.core.server.resources.Resource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Random;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * CoAP Observer 服务端
// * 提供可观察资源，每秒自动更新
// */
//public class CoapObserverServer {
//    private static final Logger logger = LoggerFactory.getLogger(CoapObserverServer.class);
//    private CoapServer server;
//
//    public CoapObserverServer(int port) {
//        // 创建服务器
//        server = new CoapServer(port);
//        // 添加资源
//        addResources();
//    }
//
//    private void addResources() {
//        // 1. 可观察的时间资源（每秒更新）
//        server.add(new ObservableTimeResource("time"));
//
//        // 2. 可观察的温度传感器资源
//        server.add(new ObservableSensorResource("temperature"));
//
//        // 3. 计数器资源（每2秒更新）
//        server.add(new ObservableCounterResource("counter"));
//
//        // 4. 普通资源（用于对比）
//        server.add(new CoapResource("hello") {
//            @Override
//            public void handleGET(CoapExchange exchange) {
//                String response = "Hello CoAP! " + new Date();
//                exchange.respond(CoAP.ResponseCode.CONTENT, response);
//            }
//        });
//
//        // 5. 可观察的JSON数据资源
//        server.add(new ObservableJsonResource("data"));
//    }
//
//    public void start() {
//        server.start();
//        logger.info("CoAP Observer 服务器已启动");
//        logger.info("监听端口: " + server.getEndpoints().get(0).getAddress().getPort());
//        logger.info("可用资源:");
//        logger.info("  coap://localhost:5683/time         - 可观察的时间资源");
//        logger.info("  coap://localhost:5683/temperature  - 可观察的温度传感器");
//        logger.info("  coap://localhost:5683/counter      - 可观察的计数器");
//        logger.info("  coap://localhost:5683/data         - 可观察的JSON数据");
//        logger.info("  coap://localhost:5683/hello        - 普通资源");
//    }
//
//    public void stop() {
//        server.stop();
//        logger.info("服务器已停止");
//    }
//
//    /**
//     * 可观察的时间资源
//     * 每秒自动更新时间
//     */
//    public static class ObservableTimeResource extends CoapResource {
//        private final Timer updateTimer = new Timer("Time-Update-Timer", true);
//        private String currentTime;
//        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//
//        public ObservableTimeResource(String name) {
//            super(name);
//            setObservable(true); // 设置为可观察
//            setObserveType(CoAP.Type.NON); // 通知使用 NON 消息
//            getAttributes().setTitle("Observable Time Resource");
//            getAttributes().addContentType(MediaTypeRegistry.TEXT_PLAIN);
//            getAttributes().setObservable();
//
//            // 初始化时间
//            currentTime = sdf.format(new Date());
//
//            // 每秒更新一次
//            updateTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    currentTime = sdf.format(new Date());
//
//                    // 如果有观察者，通知他们
//                    if (hasObservers()) {
//                        logger.debug("时间更新: {}，通知 {} 个观察者",
//                                currentTime, getObserverCount());
//
//                        // 触发变更，通知所有观察者
//                        changed();
//                    } else {
//                        logger.trace("时间更新: {}，无观察者", currentTime);
//                    }
//                }
//            }, 1000, 1000); // 1秒后开始，每秒一次
//
//            logger.info("时间资源已创建");
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            logger.info("收到GET请求，客户端: {}",
//                    exchange.getSourceAddress().getHostAddress());
//
//            // 检查是否是观察请求
//            if (exchange.getRequestOptions().hasObserve()) {
//                int observe = exchange.getRequestOptions().getObserve();
//                if (observe == 0) {
//                    logger.info("客户端 {} 开始观察此资源",
//                            exchange.getSourceAddress().getHostAddress());
//                } else {
//                    logger.info("客户端 {} 取消观察此资源",
//                            exchange.getSourceAddress().getHostAddress());
//                }
//            }
//
//            // 返回当前时间
//            exchange.respond(CoAP.ResponseCode.CONTENT,
//                    currentTime,
//                    MediaTypeRegistry.TEXT_PLAIN);
//        }
//
//        @Override
//        public void handleDELETE(CoapExchange exchange) {
//            updateTimer.cancel();
//            delete();
//            exchange.respond(CoAP.ResponseCode.DELETED);
//        }
//    }
//
//    /**
//     * 可观察的温度传感器资源
//     * 模拟温度传感器，温度随机变化
//     */
//    public static class ObservableSensorResource extends CoapResource {
//        private final Timer updateTimer = new Timer("Sensor-Update-Timer", true);
//        private double temperature = 20.0;
//        private double humidity = 50.0;
//        private final Random random = new Random();
//        private int updateCount = 0;
//
//        public ObservableSensorResource(String name) {
//            super(name);
//            setObservable(true);
//            setObserveType(CoAP.Type.NON);
//            getAttributes().setTitle("Temperature Sensor");
//            getAttributes().addContentType(MediaTypeRegistry.APPLICATION_JSON);
//            getAttributes().setObservable();
//
//            // 每3秒更新一次传感器数据
//            updateTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    // 随机变化温度
//                    temperature += (random.nextDouble() - 0.5) * 2.0;
//                    temperature = Math.max(-10.0, Math.min(50.0, temperature));
//
//                    // 随机变化湿度
//                    humidity += (random.nextDouble() - 0.5) * 10.0;
//                    humidity = Math.max(0.0, Math.min(100.0, humidity));
//
//                    updateCount++;
//
//                    // 如果有观察者，通知他们
//                    if (hasObservers()) {
//                        logger.info("传感器更新: 温度={:.1f}°C, 湿度={:.1f}%，观察者={}",
//                                temperature, humidity, getObserverCount());
//
//                        // 触发变更
//                        changed();
//                    } else {
//                        logger.trace("传感器更新但无观察者");
//                    }
//                }
//            }, 3000, 3000);
//
//            logger.info("温度传感器资源已创建");
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            // 构建JSON响应
//            String json = String.format(
//                    "{\"temperature\": %.1f, \"humidity\": %.1f, " +
//                            "\"unit\": \"celsius\", \"updates\": %d, " +
//                            "\"timestamp\": %d}",
//                    temperature, humidity, updateCount, System.currentTimeMillis()
//            );
//
//            exchange.respond(CoAP.ResponseCode.CONTENT,
//                    json,
//                    MediaTypeRegistry.APPLICATION_JSON);
//        }
//
//        @Override
//        public void handleDELETE(CoapExchange exchange) {
//            updateTimer.cancel();
//            delete();
//            exchange.respond(CoAP.ResponseCode.DELETED);
//        }
//    }
//
//    /**
//     * 可观察的计数器资源
//     * 每2秒计数一次
//     */
//    public static class ObservableCounterResource extends ConcurrentCoapResource {
//        private final Timer updateTimer = new Timer("Counter-Update-Timer", true);
//        private final AtomicInteger counter = new AtomicInteger(0);
//
//        public ObservableCounterResource(String name) {
//            super(name, 2); // 允许2个并发请求
//
//            setObservable(true);
//            setObserveType(CoAP.Type.CON); // 使用 CON 消息通知（更可靠）
//            getAttributes().setTitle("Counter Resource");
//            getAttributes().addContentType(MediaTypeRegistry.TEXT_PLAIN);
//            getAttributes().setObservable();
//
//            // 每2秒更新一次
//            updateTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    int current = counter.incrementAndGet();
//
//                    if (hasObservers()) {
//                        logger.info("计数器更新: {}，观察者={}",
//                                current, getObserverCount());
//                        changed();
//                    }
//                }
//            }, 2000, 2000);
//
//            logger.info("计数器资源已创建");
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            String response = "计数: " + counter.get();
//            exchange.respond(CoAP.ResponseCode.CONTENT,
//                    response,
//                    MediaTypeRegistry.TEXT_PLAIN);
//        }
//
//        @Override
//        public void handlePOST(CoapExchange exchange) {
//            // 重置计数器
//            counter.set(0);
//            exchange.respond(CoAP.ResponseCode.CHANGED,
//                    "计数器已重置");
//            changed(); // 通知观察者
//        }
//    }
//
//    /**
//     * 可观察的JSON数据资源
//     * 包含多种数据类型
//     */
//    public static class ObservableJsonResource extends CoapResource {
//        private final Timer updateTimer = new Timer("Data-Update-Timer", true);
//        private int dataValue = 0;
//        private boolean status = true;
//
//        public ObservableJsonResource(String name) {
//            super(name);
//            setObservable(true);
//            setObserveType(CoAP.Type.NON);
//            getAttributes().setTitle("JSON Data Resource");
//            getAttributes().addContentType(MediaTypeRegistry.APPLICATION_JSON);
//            getAttributes().setObservable();
//
//            // 每5秒更新一次
//            updateTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    dataValue++;
//                    status = !status; // 切换状态
//
//                    if (hasObservers()) {
//                        logger.info("JSON数据更新: 值={}, 状态={}，观察者={}",
//                                dataValue, status, getObserverCount());
//                        changed();
//                    }
//                }
//            }, 5000, 5000);
//
//            logger.info("JSON数据资源已创建");
//        }
//
//        @Override
//        public void handleGET(CoapExchange exchange) {
//            String json = String.format(
//                    "{\"value\": %d, \"status\": %s, " +
//                            "\"message\": \"观察者模式示例\", " +
//                            "\"timestamp\": %d}",
//                    dataValue, status, System.currentTimeMillis()
//            );
//
//            exchange.respond(CoAP.ResponseCode.CONTENT,
//                    json,
//                    MediaTypeRegistry.APPLICATION_JSON);
//        }
//    }
//
//    public static void main(String[] args) {
//        int port = 5683;
//        if (args.length > 0) {
//            try {
//                port = Integer.parseInt(args[0]);
//            } catch (NumberFormatException e) {
//                logger.warn("无效端口号，使用默认: 5683");
//            }
//        }
//
//        CoapObserverServer server = new CoapObserverServer(port);
//
//        // 添加关闭钩子
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            logger.info("正在关闭服务器...");
//            server.stop();
//        }));
//
//        try {
//            server.start();
//
//            // 保持主线程运行
//            Thread.sleep(Long.MAX_VALUE);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            logger.info("服务器被中断");
//        } catch (Exception e) {
//            logger.error("服务器启动失败", e);
//        }
//    }
//}