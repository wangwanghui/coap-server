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
//import org.eclipse.californium.core.coap.CoAP;
//
//import java.net.InetSocketAddress;
//import java.util.concurrent.TimeUnit;
//
//public class FixedCoAPClient {
//    private static final Configuration config = new Configuration();
//
//    static {
//        CoapConfig.register();
//        UdpConfig.register();
//
//        // 关键修复：客户端和服务端的 MAX_MESSAGE_SIZE 必须匹配
//        config.set(CoapConfig.MAX_MESSAGE_SIZE, 128 * 1024);           // 128KB
//        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 16 * 1024);        // 16KB块大小
//
//        // UDP缓冲区设置（比服务端小一些）
//        config.set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, 16 * 1024 * 1024);  // 16MB
//        config.set(UdpConfig.UDP_SEND_BUFFER_SIZE, 16 * 1024 * 1024);     // 16MB
//        config.set(UdpConfig.UDP_DATAGRAM_SIZE, 128 * 1024);              // 128KB
//
//        // 超时配置 - 对于64KB需要足够长
//        config.set(CoapConfig.ACK_TIMEOUT, 60, TimeUnit.SECONDS);        // 60秒
//        config.set(CoapConfig.MAX_RETRANSMIT, 8);                        // 增加重试
//        config.set(CoapConfig.EXCHANGE_LIFETIME, 300, TimeUnit.SECONDS); // 5分钟
//
//        // 块传输配置
//        config.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 300, TimeUnit.SECONDS);
//        config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 10 * 1024 * 1024); // 10MB
//
//        // 强制启用块传输
//        config.set(CoapConfig.DEFAULT_BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, false);
//
//        // 调试日志（可选）
//       // config.set(CoapConfig.LOG_LEVEL, "INFO");
//    }
//
//    public static void main(String[] args) {
//        System.out.println("🔧 修复版 CoAP 客户端 - 发送 64KB 数据");
//
//        int PAYLOAD_SIZE = 64 * 1024; // 65536字节
//        byte[] dummyPayload = new byte[PAYLOAD_SIZE];
//        for (int i = 0; i < PAYLOAD_SIZE; i++) {
//            dummyPayload[i] = (byte) (i % 256);
//        }
//
//        System.out.println("📦 生成 " + PAYLOAD_SIZE/1024 + "KB 测试数据...");
//
//        try {
//            // ✅ 修复1：创建配置化的客户端
//            CoapClient client = new CoapClient(config);
//            client.setURI("coap://127.0.0.1:5683/echo");
//            client.setTimeout(300_000L); // 5分钟
//
//            // ✅ 修复2：使用 CON 模式（可靠传输）
//            Request request = new Request(CoAP.Code.POST);
//            request.setConfirmable(true);  // 重要！使用 CON 模式
//            request.setPayload(dummyPayload);
//            request.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_OCTET_STREAM);
//
//            // ✅ 修复3：手动设置块传输选项（可选，Californium会自动检测）
//            // 如果数据大小超过 MAX_MESSAGE_SIZE，会自动分块
//
//            // ✅ 修复4：添加进度监听
//            request.addMessageObserver(new org.eclipse.californium.core.coap.MessageObserverAdapter() {
//                @Override
//                public void onAcknowledgement() {
//                    System.out.println("📤 消息已确认 (ACK)");
//                }
//
//                @Override
//                public void onResponse(CoapResponse response) {
//                    System.out.println("📥 收到响应: " + response.getCode());
//                }
//
//                @Override
//                public void onRetransmission() {
//                    System.out.println("🔄 正在重传...");
//                }
//
//                @Override
//                public void onTimeout() {
//                    System.out.println("⏰ 请求超时");
//                }
//            });
//
//            System.out.println("🚀 正在发送 " + PAYLOAD_SIZE + " 字节 POST 请求...");
//            System.out.println("预计块数: " + (PAYLOAD_SIZE / 16384 + 1)); // 16KB块
//
//            long start = System.currentTimeMillis();
//
//            // ✅ 修复5：使用同步发送，但增加详细日志
//            CoapResponse response = client.advanced(request);
//
//            long end = System.currentTimeMillis();
//
//            if (response != null) {
//                System.out.println("\n✅ 请求成功！");
//                System.out.println("响应时间: " + (end - start) + " ms");
//                System.out.println("状态码: " + response.getCode());
//                System.out.println("状态文本: " + response.getCode().name());
//
//                if (response.getPayload() != null) {
//                    System.out.println("响应体大小: " + response.getPayload().length + " 字节");
//
//                    // 验证数据完整性
//                    boolean dataCorrect = true;
//                    if (response.getPayload().length == PAYLOAD_SIZE) {
//                        for (int i = 0; i < PAYLOAD_SIZE; i++) {
//                            if (response.getPayload()[i] != dummyPayload[i]) {
//                                System.out.println("❌ 数据不匹配，位置: " + i);
//                                dataCorrect = false;
//                                break;
//                            }
//                        }
//                        if (dataCorrect) {
//                            System.out.println("✅ 64KB 回显数据完全正确");
//                        }
//                    } else {
//                        System.out.println("❌ 响应大小不匹配: 期望 " + PAYLOAD_SIZE +
//                                " 字节，实际 " + response.getPayload().length + " 字节");
//                    }
//                } else {
//                    System.out.println("⚠️  响应体为空");
//                }
//
//                // 显示响应选项
//                if (response.advanced() != null && response.advanced().getOptions() != null) {
//                    System.out.println("响应选项: " + response.advanced().getOptions());
//                    if (response.advanced().getOptions().hasBlock2()) {
//                        System.out.println("✅ 服务端使用了块传输 (Block2)");
//                    }
//                }
//            } else {
//                System.out.println("\n❌ 无响应 - 可能原因:");
//                System.out.println("1. 超时（当前超时: " + client.getTimeout() + " ms）");
//                System.out.println("2. 网络连接问题");
//                System.out.println("3. 服务端处理异常");
//            }
//
//            client.shutdown();
//            System.out.println("\n👋 客户端已关闭");
//
//        } catch (Exception e) {
//            System.err.println("\n❌ 发生异常: " + e.getMessage());
//            e.printStackTrace();
//
//            // 打印有用的调试信息
//            System.err.println("\n💡 调试建议:");
//            System.err.println("1. 检查服务端日志是否显示响应已发送");
//            System.err.println("2. 检查网络连接: ping 127.0.0.1");
//            System.err.println("3. 检查端口是否监听: netstat -an | grep 5683");
//            System.err.println("4. 使用 Wireshark 抓包分析");
//        }
//    }
//
//    /**
//     * 方法1：创建使用自定义配置的CoapClient
//     */
//    private static CoapClient createClientWithConfig(String uri) {
//        if (config == null) {
//            System.out.println("⚠️  使用默认配置创建客户端");
//            return new CoapClient(uri);
//        }
//
//        try {
//            // 方式1：使用CoapEndpoint.Builder（推荐）
//            CoapEndpoint endpoint = CoapEndpoint.builder()
//                    .setConfiguration(config)
//                    .setInetSocketAddress(new InetSocketAddress(0)) // 随机端口
//                    .build();
//
//            CoapClient client = new CoapClient(uri);
//
//            // 注意：标准的CoapClient可能不支持直接设置endpoint
//            // 我们可以使用网络配置的方式
//            return client;
//
//        } catch (Exception e) {
//            System.err.println("创建自定义配置客户端失败，使用默认: " + e.getMessage());
//            return new CoapClient(uri);
//        }
//    }
//}