package com.example.resource;

import com.example.adapter.CommonMessageObserverAdapter;
import com.example.dto.DeviceObserveRelationDTO;
import com.example.manger.CoapObservationManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ObservableResource extends CoapResource{
    private int temperature = 20;
    private AtomicInteger notifyCount = new AtomicInteger();

   private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CoapObservationManager observationManager;


    public ObservableResource(String name) {
        super(name);
        setObservable(true);
        //setObserveType(CoAP.Type.CON);
        getAttributes().setObservable();
        observationManager.registerResource(getURI(), this);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        System.out.println("\n[GET请求] 来自: " + exchange.getSourceAddress());
        System.out.println("  MID: " + exchange.advanced().getRequest().getMID());
        System.out.println("  Token: " + bytesToHex(exchange.advanced().getRequest().getToken().getBytes()));
        byte[] body = exchange.getRequestPayload();
        try {
            DeviceObserveRelationDTO dto = objectMapper.readValue(body, DeviceObserveRelationDTO.class);
        if (exchange.getRequestOptions().hasObserve()) {
            ObserveRelation relation = exchange.advanced().getRelation();
            observationManager.addObserver(getURI(), relation, dto.getSn());
            // 为这个响应添加ACK监控
            response.addMessageObserver(new CommonMessageObserverAdapter(relation.getExchange(),response));
        }
        response.setPayload("温度: " + temperature + "°C");
        response.setType(CoAP.Type.NON);
        response.getOptions().setObserve(notifyCount.get());
        exchange.respond(response);
        System.out.println("  已发送响应，MID: " + response.getMID());
        } catch (IOException e) {
            response.setPayload("请求格式错误".getBytes());
            exchange.respond(response);
        }
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
