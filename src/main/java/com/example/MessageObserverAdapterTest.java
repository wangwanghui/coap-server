package com.example;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Response;

public class MessageObserverAdapterTest extends MessageObserverAdapter {

    public Response response;

    public MessageObserverAdapterTest(Response response){
        this.response = response;
    }

    @Override
    public void onAcknowledgement() {
        System.out.println("\n[服务器] ✓ GET响应收到ACK");
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


}
