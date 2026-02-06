package com.example.adapter;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class CommonMessageObserverAdapter extends MessageObserverAdapter {

    private Response response;

    private Exchange exchange;

    private CoapExchange coapExchange;

    private String sn;


    public CommonMessageObserverAdapter(Exchange exchange, Response response) {
               this.exchange = exchange;
               this.response = response;
    }

    public CommonMessageObserverAdapter(String sn) {
        this.exchange = exchange;
        this.response = response;
    }

    @Override
    public void onAcknowledgement() {
        System.out.println("\n[服务器] ✓ GET响应收到ACK");
        //是否有序有序
         response.getToken().toString();
        System.out.println("  对应MID: " + response.getMID());
    }

    @Override
    public void onRetransmission() {
        System.out.println("\n[服务器] ↺ GET响应重试发送");
        System.out.println("  对应MID: " + response.getMID());
    }

    @Override
    public void onTimeout() {
        System.out.println("\n[服务器] ✗ GET响应ACK超时");
        System.out.println("  对应MID: " + response.getMID());

    }

    public CommonMessageObserverAdapter getAdapter(){

        return this;
    }


}
