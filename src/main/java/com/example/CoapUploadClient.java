//package com.example;
//import org.eclipse.californium.core.CoapClient;
//import org.eclipse.californium.core.CoapResponse;
//import org.eclipse.californium.core.coap.CoAP;
//import org.eclipse.californium.core.coap.MediaTypeRegistry;
//import org.eclipse.californium.core.coap.Request;
//import org.eclipse.californium.elements.config.Configuration;
//import org.eclipse.californium.elements.config.UdpConfig;
//import org.eclipse.californium.core.config.CoapConfig;
//
//import java.nio.charset.StandardCharsets;
//
//public class CoapUploadClient {
//
//    private static final String SERVER_URI = "coap://127.0.0.1:5683/echo";
//    private static final int PAYLOAD_SIZE = 64 * 1024; // 64KB
//
//    public static void main(String[] args) {
//        System.out.println("📤 准备上传 64KB 数据到 " + SERVER_URI);
//
//        // 创建配置
//        Configuration config = createClientConfig();
//
//        // ✅ 正确方式：先创建 client，再 setConfiguration
//        CoapClient client = new CoapClient(SERVER_URI);
//        try {
//            // 构造 64KB 数据
//            byte[] payload = new byte[PAYLOAD_SIZE];
//            java.util.Arrays.fill(payload, (byte) 'A');
//
//            // ✅ 正确发送 POST 请求（3.x 方式）
//            Request request = new Request(CoAP.Code.POST);
//            request.setPayload(payload);
//            request.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN); // ← 正确引用
//
//            long start = System.currentTimeMillis();
//            CoapResponse response = client.advanced(request); // ← 使用 advanced()
//            long latency = System.currentTimeMillis() - start;
//
//            if (response != null && response.isSuccess()) {
//                System.out.println("✅ 上传成功！");
//                System.out.println("⏱️  耗时: " + latency + " ms");
//                System.out.println("📦 响应码: " + response.getCode());
//                System.out.println("📊 响应长度: " + response.getPayload().length + " 字节");
//
//                if (response.getPayload().length > 0) {
//                    String respStr = new String(response.getPayload(), StandardCharsets.UTF_8);
//                    if (respStr.length() > 100) {
//                        System.out.println("📋 响应内容（前100字节）: " + respStr.substring(0, 100) + "...");
//                    } else {
//                        System.out.println("📋 响应内容: " + respStr);
//                    }
//                }
//
//                if (response.getPayload().length == PAYLOAD_SIZE) {
//                    System.out.println("✅ 回显完整（64KB）！");
//                } else {
//                    System.out.println("⚠️  回显不完整");
//                }
//
//            } else {
//                System.err.println("❌ 请求失败: " + (response != null ? response.getCode() : "null"));
//            }
//
//        } catch (Exception e) {
//            System.err.println("💥 异常: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            client.shutdown(); // 手动关闭
//        }
//    }
//
//    private static Configuration createClientConfig() {
//        Configuration config = new Configuration();
//
//        // 注册模块（必须！）
//        CoapConfig.register();
//        UdpConfig.register();
//
//        // UDP 配置
//        config.set(UdpConfig.UDP_DATAGRAM_SIZE, 2048);
//        config.set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, 1024 * 1024);
//        config.set(UdpConfig.UDP_SEND_BUFFER_SIZE, 1024 * 1024);
//
//        // CoAP 配置
//        config.set(CoapConfig.MAX_MESSAGE_SIZE, 65536);
//        config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 65536);
//        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
//
//        return config;
//    }
//}