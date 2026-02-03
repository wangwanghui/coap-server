//package com.example;
//
//import org.eclipse.californium.core.CoapResource;
//import org.eclipse.californium.core.CoapServer;
//import org.eclipse.californium.core.coap.CoAP;
//import org.eclipse.californium.core.coap.Response;
//import org.eclipse.californium.core.observe.ObserveRelation;
//import org.eclipse.californium.core.server.resources.CoapExchange;
//import org.eclipse.californium.core.server.resources.ObservableResource;
//
//import java.util.Random;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * 基于 ObservableResource 的 CoAP 服务端
// * 支持 CON 模式并显示客户端 ACK
// */
//public class ObservableServer {
//
//    public static void main(String[] args) {
//        CoapServer server = new CoapServer();
//
//        // 添加可观察资源
//        server.add(new ObservableTemperatureResource("temperature"));
//        server.add(new ObservableCounterResource("counter"));
//
//        server.start();
//        System.out.println("CoAP Observable Server started on port 5683");
//        System.out.println("Resources available:");
//        System.out.println("  /temperature - 温度资源 (每分钟更新)");
//        System.out.println("  /counter - 计数器资源 (每10秒更新)");
//    }
//}
//
///**
// * 温度资源 - 每分钟更新一次
// */
//class ObservableTemperatureResource extends CoapResource implements ObservableResource {
//
//    private final Random random = new Random();
//    private float currentTemperature = 20.0f;
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//    private final AtomicInteger messageId = new AtomicInteger(1000);
//    private final ConcurrentHashMap<String, ObserveInfo> observers = new ConcurrentHashMap<>();
//
//    public ObservableTemperatureResource(String name) {
//        super(name);
//
//        // 设置为可观察资源
//        setObservable(true);
//        setObserveType(CoAP.Type.CON); // 使用 CON 模式
//        getAttributes().setObservable();
//
//        // 每60秒更新一次温度
//        scheduler.scheduleAtFixedRate(this::updateTemperature, 0, 60, TimeUnit.SECONDS);
//
//        System.out.println("Temperature resource created at: /" + name);
//    }
//
//    private void updateTemperature() {
//        // 温度在 15-30 度之间随机变化
//        currentTemperature = 15.0f + random.nextFloat() * 15.0f;
//        currentTemperature = Math.round(currentTemperature * 10) / 10.0f;
//
//        System.out.println("\n[温度更新] 新温度: " + currentTemperature + "°C");
//
//        // 通知所有观察者
//        notifyObservers();
//    }
//
//    @Override
//    public void handleGET(CoapExchange exchange) {
//        System.out.println("\n[GET请求] 来自: " + exchange.getSourceAddress() +
//                ", MID: " + exchange.advanced().getRequest().getMID());
//
//        String responseText = String.format("当前温度: %.1f°C (更新时间: %s)",
//                currentTemperature, java.time.LocalTime.now());
//
//        Response response = new Response(CoAP.ResponseCode.CONTENT);
//        response.setPayload(responseText);
//        response.setType(CoAP.Type.CON);
//        response.setMID(messageId.getAndIncrement());
//        response.getOptions().setObserve(getObserversCount());
//
//        exchange.respond(response);
//
//        // 记录观察者信息
//        String observerKey = exchange.getSourceAddress().getHostAddress() + ":" + exchange.getSourcePort();
//        observers.put(observerKey, new ObserveInfo(observerKey, System.currentTimeMillis()));
//
//        System.out.println("当前观察者数量: " + observers.size());
//    }
//
//    @Override
//    public void handlePUT(CoapExchange exchange) {
//        // 允许客户端设置温度
//        String payload = exchange.getRequestText();
//        try {
//            currentTemperature = Float.parseFloat(payload);
//            System.out.println("\n[温度设置] 新温度: " + currentTemperature + "°C");
//
//            Response response = new Response(CoAP.ResponseCode.CHANGED);
//            response.setPayload("温度已更新为: " + currentTemperature + "°C");
//            response.setType(CoAP.Type.ACK);
//            exchange.respond(response);
//
//            // 通知所有观察者
//            notifyObservers();
//        } catch (NumberFormatException e) {
//            exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "无效的温度值");
//        }
//    }
//
//    private void notifyObservers() {
//        if (observers.isEmpty()) {
//            System.out.println("没有观察者，跳过通知");
//            return;
//        }
//
//        String responseText = String.format("温度更新: %.1f°C (%s)",
//                currentTemperature, java.time.LocalTime.now());
//
//        Response notification = new Response(CoAP.ResponseCode.CONTENT);
//        notification.setPayload(responseText);
//        notification.setType(CoAP.Type.CON); // 使用 CON 模式发送通知
//        notification.setMID(messageId.getAndIncrement());
//        notification.getOptions().setObserve(getObserversCount());
//
//        System.out.println("发送通知给 " + observers.size() + " 个观察者");
//        System.out.println("通知内容: " + responseText);
//        System.out.println("通知 MID: " + notification.getMID());
//
//        // 在实际应用中，这里会调用超类的通知方法
//        // super.notifyObservers(notification);
//
//        // 这里简化处理，打印日志
//        observers.forEach((key, info) -> {
//            System.out.println("  → 发送给观察者: " + key);
//        });
//    }
//
//    @Override
//    public void handleResponse(ObserveRelation relation, Response response) {
//        // 当观察者回复 ACK 时被调用
//        System.out.println("\n[收到ACK] 来自: " + relation.getExchange().getRemoteSocketAddress() +
//                ", MID: " + response.getMID() +
//                ", 状态: " + response.getCode());
//
//        String observerKey = relation.getExchange().getRemoteSocketAddress() +
//                ":" + relation.getExchange().getSourcePort();
//
//        ObserveInfo info = observers.get(observerKey);
//        if (info != null) {
//            info.lastAckTime = System.currentTimeMillis();
//            info.ackCount++;
//            System.out.println("观察者 " + observerKey + " 已回复 " + info.ackCount + " 次 ACK");
//        }
//    }
//
//    @Override
//    public void canceled(ObserveRelation relation) {
//        String observerKey = relation.getExchange().getSourceAddress().getHostAddress() +
//                ":" + relation.getExchange().getSourcePort();
//
//        observers.remove(observerKey);
//        System.out.println("\n[观察者取消] " + observerKey);
//        System.out.println("剩余观察者数量: " + observers.size());
//    }
//
//    private static class ObserveInfo {
//        String address;
//        long subscribeTime;
//        long lastAckTime;
//        int ackCount;
//
//        ObserveInfo(String address, long subscribeTime) {
//            this.address = address;
//            this.subscribeTime = subscribeTime;
//            this.lastAckTime = subscribeTime;
//            this.ackCount = 0;
//        }
//    }
//}
//
///**
// * 计数器资源 - 每10秒更新一次
// */
//class ObservableCounterResource extends CoapResource implements ObservableResource {
//
//    private int counter = 0;
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//    private final AtomicInteger messageId = new AtomicInteger(2000);
//
//    public ObservableCounterResource(String name) {
//        super(name);
//
//        setObservable(true);
//        setObserveType(CoAP.Type.CON);
//        getAttributes().setObservable();
//
//        // 每10秒更新一次计数器
//        scheduler.scheduleAtFixedRate(this::incrementCounter, 0, 10, TimeUnit.SECONDS);
//
//        System.out.println("Counter resource created at: /" + name);
//    }
//
//    private void incrementCounter() {
//        counter++;
//        System.out.println("\n[计数器更新] 新值: " + counter);
//        changed();
//    }
//
//    @Override
//    public void handleGET(CoapExchange exchange) {
//        System.out.println("\n[GET请求] 计数器资源被访问");
//
//        String responseText = String.format("计数器: %d (更新时间: %s)",
//                counter, java.time.LocalTime.now());
//
//        Response response = new Response(CoAP.ResponseCode.CONTENT);
//        response.setPayload(responseText);
//        response.setType(CoAP.Type.CON);
//        response.setMID(messageId.getAndIncrement());
//        response.getOptions().setObserve(getObserversCount());
//
//        exchange.respond(response);
//    }
//
//    @Override
//    public void handlePOST(CoapExchange exchange) {
//        // 重置计数器
//        counter = 0;
//        System.out.println("\n[计数器重置] 值为0");
//
//        Response response = new Response(CoAP.ResponseCode.CHANGED);
//        response.setPayload("计数器已重置为0");
//        response.setType(CoAP.Type.ACK);
//        exchange.respond(response);
//
//        changed(); // 通知观察者
//    }
//}
//
///**
// * 客户端模拟器，用于测试 ACK 回复
// */
//class CoAPClientSimulator {
//    public static void main(String[] args) {
//        System.out.println("CoAP 客户端模拟器");
//        System.out.println("使用以下命令测试服务器:");
//        System.out.println();
//        System.out.println("1. 观察温度资源:");
//        System.out.println("   coap-client -m get -s 10 coap://localhost:5683/temperature");
//        System.out.println();
//        System.out.println("2. 观察计数器资源:");
//        System.out.println("   coap-client -m get -s 10 coap://localhost:5683/counter");
//        System.out.println();
//        System.out.println("3. 设置温度值:");
//        System.out.println("   coap-client -m put -e \"25.5\" coap://localhost:5683/temperature");
//        System.out.println();
//        System.out.println("4. 重置计数器:");
//        System.out.println("   coap-client -m post coap://localhost:5683/counter");
//    }
//}