package com.example.manger;

import com.example.adapter.CommonMessageObserverAdapter;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CoapObservationManager {

    private static final Logger log = LoggerFactory.getLogger(CoapObservationManager.class);

    // 数据结构: resourcePath -> deviceId -> ObserveRelation
    private final ConcurrentMap<String, ConcurrentMap<String, ObserveRelation>> observers =
            new ConcurrentHashMap<>();


    // 资源注册
    public void registerResource(String resourcePath, CoapResource resource) {
        ConcurrentMap<String, ObserveRelation> map = new ConcurrentHashMap<>();
        observers.put(resourcePath, map);
        resource.setObservable(true);
    }

    // 添加观察者
    public void addObserver(String resourcePath, ObserveRelation relation, String sn) {
        ConcurrentMap<String, ObserveRelation> resourceObservers = observers.get(resourcePath);
        if (resourceObservers != null) {
            resourceObservers.put(sn, relation);
            // 设置超时清理
            scheduleCleanup(relation, resourcePath);
        }
    }

    /*
    * 获得某个观察者
    * */
    public ObserveRelation getObserverBySn(String resourcePath, String sn) {
        ConcurrentMap<String, ObserveRelation> resourceObservers = observers.get(resourcePath);
        if (resourceObservers == null) {
            throw new RuntimeException();
        }
        Optional<ObserveRelation> relation = Optional.ofNullable(resourceObservers.get(sn));
        return relation.orElse(null);
    }

    // 发送所有订阅者通知
    public void notifyAllObservers(String resourcePath, String payload) {
        ConcurrentMap<String, ObserveRelation> resourceObservers = observers.get(resourcePath);
        if (resourceObservers == null) {
            return;
        }
        for (Map.Entry<String, ObserveRelation> entry : resourceObservers.entrySet()) {
            String key = entry.getKey();
            ObserveRelation relation = entry.getValue();
            if (!isRelationValid(relation)) {
                resourceObservers.remove(key);
                continue;
            }
            try {
                Response response = new Response(CoAP.ResponseCode.CONTENT);
                response.setPayload(payload);
                response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
                relation.getExchange().sendResponse(response);
            } catch (Exception e) {
                log.warn("Failed to send observation for resource: {}", resourcePath, e);
                resourceObservers.remove(key);
            }
            // 处理逻辑
        }
    }


    // 发送单个设备消息
    public Boolean notifySingleObservers(String resourcePath, String payload, String sn) {
        ConcurrentMap<String, ObserveRelation> resourceObservers = observers.get(resourcePath);
        if (resourceObservers == null || resourceObservers.get(sn) == null) {
            return false;
        }
        ObserveRelation relation = resourceObservers.get(sn);
        try {
            Response response = new Response(CoAP.ResponseCode.CONTENT);
            response.setPayload(payload);
            response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
            relation.getExchange().sendResponse(response);
            response.addMessageObserver(new CommonMessageObserverAdapter(relation.getExchange(),response));
        } catch (Exception e) {
            log.warn("Failed to send observation for resource: {}", resourcePath, e);
            resourceObservers.remove(sn);
        }
        // 处理逻辑
        return true;
    }

    // 检查关系是否有效
    private boolean isRelationValid(ObserveRelation relation) {
        return relation != null &&
                !relation.isCanceled() &&
                relation.isEstablished() &&
                relation.getExchange() != null;
    }

    // 超时清理
    private void scheduleCleanup(ObserveRelation relation, String resourcePath) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            if (isRelationValid(relation)) {
                ConcurrentMap<String, ObserveRelation> resourceObservers = observers.get(resourcePath);
                if (resourceObservers != null) {
                    resourceObservers.remove(relation);
                }
            }
        }, 1, TimeUnit.HOURS);
    }

    /**
     * 生成客户端ID
     */
    private String getClientId(CoapExchange exchange) {
        return exchange.getSourceSocketAddress().getAddress() + ":" + exchange.getSourcePort();
    }

    /**
     * 启动批量清理
     */
    private void startBatchCleanup() {

//        // 慢速清理：每30秒检查所有观察者
//        cleanupScheduler.scheduleAtFixedRate(this::fullBatchCleanup, 30, 30, TimeUnit.SECONDS);
//
//        // 心跳超时检查：每1分钟检查一次
//        cleanupScheduler.scheduleAtFixedRate(this::checkHeartbeatTimeout, 60, 60, TimeUnit.SECONDS);

        log.info("Batch cleanup scheduler started");
    }


}
