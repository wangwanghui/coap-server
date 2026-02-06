/*
package com.example;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.ObservableResource;

import java.util.Set;

public class ManualObserveResource extends CoapResource implements ObservableResource {

    private int temperature = 20;
    private int observeCounter = 0;

    public ManualObserveResource(String name) {
        super(name);
        setObservable(true);
        getAttributes().setObservable();

        // 定时更新线程
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10000);
                    temperature += (Math.random() > 0.5 ? 1 : -1);
                    System.out.println("[温度更新] 新温度: " + temperature);

                    // 手动推送给所有观察者
                    manuallyNotifyAllObservers();

                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        System.out.println("[GET请求] 来自: " + exchange.getSourceAddress());

        // 创建首次响应
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload("当前温度: " + temperature + "°C");
        response.getOptions().setObserve(observeCounter);
        response.setToken(exchange.advanced().getRequest().getToken());

        // 设置为CON确保可靠
        response.setType(CoAP.Type.CON);

        exchange.respond(response);
        System.out.println("  已发送首次响应，Observe序号: " + observeCounter);
    }

*
     * 手动推送方法 - 这才是changed()真正做的事


    private void manuallyNotifyAllObservers() {
        // 获取此资源的所有观察关系
        Set<Observation> observations = getObservations();

        if (observations.isEmpty()) {
            System.out.println("  无观察者，无需推送");
            return;
        }

        // 递增Observe序号
        observeCounter++;

        System.out.println("  开始手动推送，观察者数量: " + observations.size());

        for (Observation observation : observations) {
            // 为每个观察者创建独立的通知响应
            Response notification = new Response(CoAP.ResponseCode.CONTENT);
            notification.setPayload("更新温度: " + temperature + "°C");
            notification.getOptions().setObserve(observeCounter);

            // 关键：使用原始请求的Token
            notification.setToken(observation.getRequest().getToken());

            // 使用CON类型（需要ACK确认）
            notification.setType(CoAP.Type.CON);

            // 获取对应的Exchange和Endpoint
            Endpoint endpoint = observation.getExchange().getEndpoint();

            System.out.println("  发送通知给 " +
                    observation.getExchange().getSourceSocketAddress() +
                    ", Token: " + bytesToHex(notification.getToken().getBytes()) +
                    ", Observe序号: " + observeCounter);

            // 直接通过Endpoint发送，不经过handleGET
            endpoint.sendResponse(notification);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    // 主方法用于测试
    public static void main(String[] args) throws Exception {
        CoapServer server = new CoapServer();
        server.add(new ManualObserveResource("temperature"));
        server.start();

        System.out.println("手动推送服务器启动，端口5683");
        System.out.println("使用coap-client测试：coap-client -m get -s 10 coap://localhost:5683/temperature");
    }
}
*/
