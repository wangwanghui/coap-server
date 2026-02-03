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
//import java.util.Date;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class SimpleObserveServer2 {
//
//    public static void main(String[] args) {
//        CoapServer server = new CoapServer();
//        server.add(new SimpleObservableResource("test"));
//        server.start();
//        System.out.println("CoAP 服务端启动: coap://localhost:5683/test");
//        System.out.println("==========================================");
//    }
//}
//
//class SimpleObservableResource extends CoapResource implements ObservableResource {
//
//    private final AtomicInteger counter = new AtomicInteger(0);
//    private final AtomicInteger messageId = new AtomicInteger(1000);
//    private final AtomicInteger ackCount = new AtomicInteger(0);
//    private final AtomicInteger notificationCount = new AtomicInteger(0);
//
//    public SimpleObservableResource(String name) {
//        super(name);
//
//        // 设置为可观察，使用 CON 模式
//        setObservable(true);
//        setObserveType(CoAP.Type.CON);
//        getAttributes().setObservable();
//
//        // 每5秒更新一次
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(5000);
//                    changed(); // 触发观察通知
//                    notificationCount.incrementAndGet();
//                } catch (InterruptedException e) {
//                    break;
//                }
//            }
//        }).start();
//    }
//
//    @Override
//    public void handleGET(CoapExchange exchange) {
//        int value = counter.incrementAndGet();
//        int mid = messageId.incrementAndGet();
//
//        System.out.println("\n[收到GET请求]");
//        System.out.println("  客户端: " + exchange.getSourceAddress());
//        System.out.println("  请求MID: " + exchange.advanced().getRequest().getMID());
//        System.out.println("  请求Token: " + bytesToHex(exchange.advanced().getRequest().getToken().getBytes()));
//
//        Response response = new Response(CoAP.ResponseCode.CONTENT);
//        response.setPayload("初始值: " + value + " (" + new Date() + ")");
//        response.setType(CoAP.Type.CON); // CON 模式
//        response.setMID(mid);
//        response.getOptions().setObserve(value);
//        response.setToken(exchange.advanced().getRequest().getToken());
//
//        System.out.println("  响应MID: " + mid);
//        System.out.println("  响应Token: " + bytesToHex(response.getToken().getBytes()));
//
//        exchange.respond(response);
//
//        System.out.println("  已发送初始响应，等待客户端 ACK...");
//    }
//
//    @Override
//    public void changed() {
//        int value = counter.incrementAndGet();
//        int mid = messageId.incrementAndGet();
//
//        System.out.println("\n[发送观察通知 #" + notificationCount.get() + "]");
//        System.out.println("  值: " + value);
//        System.out.println("  通知MID: " + mid);
//        System.out.println("  时间: " + new Date());
//
//        // 注意：在实际的 Californium 实现中，通知会自动发送给所有观察者
//        // 这里我们只是打印日志，实际通知由框架处理
//
//        System.out.println("  等待客户端 ACK...");
//    }
//
//    @Override
//    public void handleResponse(ObserveRelation relation, Response response) {
//        ackCount.incrementAndGet();
//
//        System.out.println("\n[收到 ACK #" + ackCount.get() + "]");
//        System.out.println("  来自: " + relation.getExchange().getRemoteSocketAddress());
//        System.out.println("  响应对应的MID: " + response.getMID());
//        System.out.println("  Token: " + bytesToHex(response.getToken().getBytes()));
//        System.out.println("  状态码: " + response.getCode());
//        System.out.println("  累计ACK: " + ackCount.get());
//    }
//
//    private String bytesToHex(byte[] bytes) {
//        if (bytes == null) return "null";
//        StringBuilder hex = new StringBuilder();
//        for (byte b : bytes) {
//            hex.append(String.format("%02x", b));
//        }
//        return hex.toString();
//    }
//
//}
