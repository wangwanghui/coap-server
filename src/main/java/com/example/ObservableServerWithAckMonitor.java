package com.example;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.TcpConfig;
import org.eclipse.californium.elements.config.UdpConfig;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 监控 ACK 的 Observable 服务端
 */
public class ObservableServerWithAckMonitor {

    private static Configuration createCustomConfig() {
        // 创建空的配置
        Configuration config = new Configuration();

        // 必须注册配置模块
        CoapConfig.register();
        UdpConfig.register();
        TcpConfig.register();

        // 然后设置配置值...
       // config.set(CoapConfig.COAP_PORT, 5683);
        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, 32);
        // ... 其他配置
        // ========== 基础配置 ==========
        // ========== 块传输配置（核心修改） ==========
        config.set(CoapConfig.MAX_MESSAGE_SIZE, 128 * 1024);           // 64KB
        config.set(CoapConfig.PREFERRED_BLOCK_SIZE, 16 * 1024);    // 64KB
        config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 10 * 1024 * 1024);
        // ========== 块传输配置强化 ==========
        config.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 300, TimeUnit.SECONDS); // 延长到5分钟
        config.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, false); // 允许更灵活的块处理
// ========== UDP 配置 ==========
        config.set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, 64 * 1024 * 1024);
        config.set(UdpConfig.UDP_SEND_BUFFER_SIZE, 64 * 1024 * 1024);
        config.set(UdpConfig.UDP_RECEIVER_THREAD_COUNT, 8);
        config.set(UdpConfig.UDP_SENDER_THREAD_COUNT, 8);
        config.set(UdpConfig.UDP_DATAGRAM_SIZE, 128 * 1024);   // 必须 >= 你期望的最大块大小
// ========== 并发连接配置 ==========
        config.set(CoapConfig.MAX_ACTIVE_PEERS, 100000);
        config.set(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, 30, TimeUnit.SECONDS);

// ========== 线程池配置 ==========
        int cores = Runtime.getRuntime().availableProcessors();
        config.set(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT, Math.max(32, cores * 2));

// ========== 超时和重传配置 ==========
        config.set(CoapConfig.ACK_TIMEOUT, 1, TimeUnit.SECONDS);
        config.set(CoapConfig.ACK_INIT_RANDOM, 1.5f);
        config.set(CoapConfig.MAX_RETRANSMIT, 2);
        config.set(CoapConfig.ACK_TIMEOUT_SCALE, 2.0f);
        config.set(CoapConfig.EXCHANGE_LIFETIME, 300, TimeUnit.SECONDS);

// ========== 交换生命周期 ==========
        config.set(CoapConfig.NON_LIFETIME, 10, TimeUnit.SECONDS);
        config.set(CoapConfig.NSTART, 1);

// ========== 其他配置 ==========
        config.set(CoapConfig.MAX_LATENCY, 10, TimeUnit.SECONDS);
        return config;
    }

    public static void main(String[] args) {
        Configuration config = createCustomConfig();

        // ========== 创建服务器 ==========
        CoapServer server = new CoapServer(config);
        server.add(new ObservableResource("temperature"));
        server.start();
        System.out.println("服务器启动，端口 5683");
    }
}

class ObservableResource extends CoapResource {

    private int temperature = 20;
    private AtomicInteger notifyCount = new AtomicInteger();

    public ObservableResource(String name) {
        super(name);
        setObservable(true);
        setObserveType(CoAP.Type.CON);
        getAttributes().setObservable();

        // 模拟温度变化
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // 10秒更新一次
                    temperature += (Math.random() > 0.5 ? 1 : -1);
                    changed();
                    System.out.println("\n[温度更新] 新温度: " + temperature + "°C");
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        System.out.println("\n[GET请求] 来自: " + exchange.getSourceAddress());
        System.out.println("  MID: " + exchange.advanced().getRequest().getMID());
        System.out.println("  Token: " + bytesToHex(exchange.advanced().getRequest().getToken().getBytes()));

        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload("温度: " + temperature + "°C");
        response.setType(CoAP.Type.CON);
        response.getOptions().setObserve(notifyCount.get());
        // 为这个响应添加ACK监控
        response.addMessageObserver(new MessageObserverAdapterTest(response));
        exchange.respond(response);
        System.out.println("  已发送响应，MID: " + response.getMID());
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}