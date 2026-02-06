package com.example.event;

import com.example.manger.CoapObservationManager;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


/*
*
* 顺序发送事件
* */
@Component
@Slf4j
public class EventDrivenSenderV2 {

    @Autowired
    private  CoapObservationManager coapObservationManager;

    // 核心数据结构
    // 1. 设备消息队列
    private final Map<String, Queue<String>> deviceQueues = new ConcurrentHashMap<>();

    // 2. 正在发送中的设备
    private final Set<String> sendingDevices = ConcurrentHashMap.newKeySet();


    @Autowired
    private Executor eventHandlers;
    // 3. 事件处理器（固定线程数）

    // 4. ACK事件队列
    private final BlockingQueue<AckEvent> ackQueue = new LinkedBlockingQueue<>();

    // 5. ACK处理器（单个线程）
    private final ExecutorService ackProcessor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        startAckProcessor();
    }

    /**
     * 发送顺序消息
     */
    public void sendSequential(String sn, List<String> messages) {
        // 1. 将消息加入设备队列
        Queue<String> queue = deviceQueues.computeIfAbsent(sn,
                k -> new ConcurrentLinkedQueue<>());
        queue.addAll(messages);
        // 2. 如果设备不在发送中，触发发送
        if (sendingDevices.add(sn)) {
            triggerSend(sn);
        }
    }


    /**
     * 触发发送 - 不阻塞！
     */
    private void triggerSend(String sn) {
        eventHandlers.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sendNextMessage(sn);
                } catch (Exception e) {
                    log.error("发送异常, SN: {}", sn, e);
                    sendingDevices.remove(sn);
                }
            }
        });
    }

    /**
     * 发送下一条消息 - 立即返回，不等待ACK！
     */
    private void sendNextMessage(String sn) {
        Queue<String> queue = deviceQueues.get(sn);
        if (queue == null || queue.isEmpty()) {
            sendingDevices.remove(sn);
            return;
        }

        String message = queue.peek(); // 只看不取

        try {
            ObserveRelation relation = getRelation(sn);
            if (relation == null) {
                handleError(sn, "设备未连接");
                return;
            }
            Response response = new Response(CoAP.ResponseCode.CONTENT);
            response.setPayload(message);
            // 关键：立即发送，不等待！
            sendMessageAsync(relation, response, sn, message);

        } catch (Exception e) {
            log.error("发送失败, SN: {}", sn, e);
            handleError(sn, "发送异常");
        }
    }

    /**
     * 异步发送消息
     */
    private void sendMessageAsync(ObserveRelation relation, Response response,
                                  String sn, String message) {
        // 添加监听器
        response.addMessageObserver(new MessageObserverAdapter() {
            @Override
            public void onAcknowledgement() {
                // 不在这里处理业务逻辑！
                // 只是将事件放入队列
                ackQueue.offer(new AckEvent(sn, message, true, System.currentTimeMillis()));
            }

            @Override
            public void onTimeout() {
                ackQueue.offer(new AckEvent(sn, message, false, System.currentTimeMillis()));
            }
        });

        // 立即发送，立即返回
        relation.getExchange().sendResponse(response);
    }

    /**
     * 启动ACK处理器
     */
    private void startAckProcessor() {
        ackProcessor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 从队列取出ACK事件
                    AckEvent event = ackQueue.take();

                    // 处理ACK
                    handleAckEvent(event);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /**
     * 处理ACK事件
     */
    private void handleAckEvent(AckEvent event) {
        Queue<String> queue = deviceQueues.get(event.sn);
        if (queue == null) return;

        if (event.success) {
            // 成功：移除已发送的消息
            String headMessage = queue.peek();
            if (headMessage != null && headMessage.equals(event.message)) {
                queue.poll(); // 移除

                // 如果还有消息，发送下一条
                if (!queue.isEmpty()) {
                    triggerSend(event.sn);
                } else {
                    sendingDevices.remove(event.sn);
                }
            }
        } else {
            queue.clear();
            sendingDevices.remove(event.sn);
            log.error("消息超时, SN: {}, Message: {}", event.sn, event.message);
        }
    }

    private void handleError(String sn, String reason) {
        Queue<String> queue = deviceQueues.get(sn);
        if (queue != null) {
            queue.clear();
        }
        sendingDevices.remove(sn);
        log.error("处理错误, SN: {}, Reason: {}", sn, reason);
    }

    private static class AckEvent {
        final String sn;
        final String message;
        final boolean success;
        final long timestamp;

        AckEvent(String sn, String message, boolean success, long timestamp) {
            this.sn = sn;
            this.message = message;
            this.success = success;
            this.timestamp = timestamp;
        }
    }

    private ObserveRelation getRelation(String sn) {
        return coapObservationManager.getObserverBySn("/resource/" + sn, sn);
    }
}