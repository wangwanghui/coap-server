//package com.example;
//
//import org.eclipse.californium.core.CoapClient;
//import org.eclipse.californium.core.CoapResponse;
//import org.eclipse.californium.core.coap.MediaTypeRegistry;
//import org.eclipse.californium.core.config.CoapConfig;
//import org.eclipse.californium.core.network.CoapEndpoint;
//import org.eclipse.californium.elements.config.Configuration;
//import org.eclipse.californium.elements.config.UdpConfig;
//import org.eclipse.californium.core.coap.Request;
//
//import java.net.InetSocketAddress;
//import java.util.concurrent.TimeUnit;
//
//public class SimpleCoAPClient {
//    private static final    Configuration config = new Configuration();
//    static {
//        CoapConfig.register();
//        UdpConfig.register();
//
//        // ✅ 彻底解决UDP大小限制
//        config.set(CoapConfig.MAX_MESSAGE_SIZE, 128 * 1024);           // 128KB
//        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 16 * 1024);        // 16KB块大小
//
//        // ✅ 关键修复：大幅提高UDP数据报大小
//        config.set(UdpConfig.UDP_DATAGRAM_SIZE, 128 * 1024);            // 64KB！直接设置最大值
//
//        // 超时配置
//        config.set(CoapConfig.ACK_TIMEOUT, 30, TimeUnit.SECONDS);      // 30秒
//        config.set(CoapConfig.MAX_RETRANSMIT, 4);
//        config.set(CoapConfig.EXCHANGE_LIFETIME, 300, TimeUnit.SECONDS); // 5分钟
//        //config.set(CoapConfig.DEFAULT_BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER,false);
//    }
//
//    public static void main(String[] args) {
//        System.out.println("测试 CoAP 客户端 - 发送 64KB 数据（强制分块传输）...");
//        System.out.println(CoapConfig.DEFAULT_BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER);
//        int PAYLOAD_SIZE = 64 * 1024; // 65536字节
//        byte[] dummyPayload = new byte[PAYLOAD_SIZE];
//        for (int i = 0; i < PAYLOAD_SIZE; i++) {
//            dummyPayload[i] = (byte) (i % 256);
//        }
//
//        try {
//            CoapClient client = new CoapClient("coap://127.0.0.1:5683/echo");
//            client.setTimeout(300_000L); // 5分钟
//            // 方式1：使用CoapEndpoint.Builder（推荐）
//            CoapEndpoint endpoint = CoapEndpoint.builder()
//                    .setConfiguration(config)
//                    .setInetSocketAddress(new InetSocketAddress(0)) // 随机端口
//                    .build();
//            client.setEndpoint(endpoint);
//            Request request = Request.newPost();
//            request.setURI("coap://127.0.0.1:5683/echo");
//            request.setPayload(dummyPayload);
//            request.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_OCTET_STREAM);
//
//            // ✅ 强制分块传输（避免自动检测问题）
//            // 不设置Block2选项，让Californium自动处理
//
//            long start = System.currentTimeMillis();
//            System.out.println("正在发送 " + PAYLOAD_SIZE + " 字节 POST 请求...");
//
//            CoapResponse response = client.advanced(request);
//            long end = System.currentTimeMillis();
//
//            if (response != null) {
//                System.out.println("✅ 成功！");
//                System.out.println("响应时间: " + (end - start) + " ms");
//                System.out.println("状态码: " + response.getCode());
//                if (response.getPayload() != null) {
//                    System.out.println("响应体大小: " + response.getPayload().length + " 字节");
//                    if (response.getPayload().length == PAYLOAD_SIZE) {
//                        System.out.println("✅ 64KB 回显数据大小正确");
//                    }
//                }
//            } else {
//                System.out.println("❌ 无响应");
//            }
//
//            client.shutdown();
//
//        } catch (Exception e) {
//            System.err.println("❌ 发生异常: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}