//package com.example;
//import cn.hutool.json.JSONUtil;
//import lombok.Data;
//import org.eclipse.californium.core.CoapResource;
//import org.eclipse.californium.core.coap.CoAP;
//import org.eclipse.californium.core.coap.MediaTypeRegistry;
//import org.eclipse.californium.core.coap.Response;
//import org.eclipse.californium.core.observe.ObserveRelation;
//import org.eclipse.californium.core.server.resources.CoapExchange;
//import org.eclipse.californium.core.server.resources.ObservableResource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * 基于ObservableResource实现的可观察资源
// * 支持按设备SN进行消息推送
// */
//public class DeviceObservableResource extends CoapResource implements ObservableResource {
//
//    private static final Logger logger = LoggerFactory.getLogger(DeviceObservableResource.class);
//
//    // 存储观察者关系：SN -> 观察者列表
//    private final Map<String, Set<ObserveRelation>> deviceObservers = new ConcurrentHashMap<>();
//
//    // 存储设备的最新状态
//    private final Map<String, DeviceStatus> deviceStatusMap = new ConcurrentHashMap<>();
//
//    // 用于定时更新和清理
//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//
//    // 观察序号生成器
//    private final AtomicInteger observeSequence = new AtomicInteger(0);
//
//    // 默认心跳间隔（秒）
//    private static final int DEFAULT_HEARTBEAT_INTERVAL = 30;
//
//    public DeviceObservableResource() {
//        this("devices");
//    }
//
//    public DeviceObservableResource(String name) {
//        super(name);
//
//        setObserveType(CoAP.Type.CON);
//
//        // 设置为可观察资源
//        setObservable(true);
//
//        // 设置观察类型
//        getAttributes().setObservable();
//
//        // 添加资源描述
//        getAttributes().addResourceType("observe-device");
//        getAttributes().addInterfaceDescription("core#a");
//        getAttributes().addContentType(MediaTypeRegistry.APPLICATION_JSON);
//
//        // 启动定时任务
//        startScheduledTasks();
//
//        logger.info("设备可观察资源初始化完成: {}", name);
//    }
//
//    @Override
//    public void handleGET(CoapExchange exchange) {
//        try {
//            // 获取设备SN
//            String deviceSn = extractDeviceSnFromRequest(exchange);
//
//            if (deviceSn == null || deviceSn.isEmpty()) {
//                // 返回所有设备状态
//                respondWithAllDevices(exchange);
//                return;
//            }
//
//            // 获取指定设备的状态
//            DeviceStatus status = deviceStatusMap.get(deviceSn);
//
//            if (status == null) {
//                // 设备不存在
//                exchange.respond(CoAP.ResponseCode.NOT_FOUND,
//                        String.format("设备 %s 未注册", deviceSn));
//                return;
//            }
//
//            // 构建响应
//            Response response = new Response(CoAP.ResponseCode.CONTENT);
//            response.setPayload(JSONUtil.toJsonStr(status));
//            response.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_JSON);
//
//            // 检查是否包含Observe选项
//            if (exchange.getRequestOptions().hasObserve()) {
//                // 这是一个观察请求
//                ObserveRelation relation = exchange.advanced().getRelation();
//
//                if (relation != null) {
//                    // 添加观察关系
//                    addDeviceObserver(deviceSn, relation);
//
//                    // 设置观察序号
//                    response.getOptions().setObserve(getNextObserveNumber());
//
//                    logger.info("设备 {} 添加观察者，总数: {}",
//                            deviceSn, getObserverCount(deviceSn));
//                }
//            }
//
//            exchange.respond(response);
//
//        } catch (Exception e) {
//            logger.error("处理GET请求失败", e);
//            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @Override
//    public void handlePOST(CoapExchange exchange) {
//        try {
//            // POST用于设备注册或发送消息
//            String payload = exchange.getRequestText();
//
//            if (exchange.getRequestOptions().getUriPath().contains("register")) {
//                // 设备注册
//                registerDevice(exchange, payload);
//            } else if (exchange.getRequestOptions().getUriPath().contains("send")) {
//                // 发送消息给设备
//                sendMessageToDevice(exchange, payload);
//            } else {
//                // 默认处理：更新设备状态
//                updateDeviceStatus(exchange, payload);
//            }
//
//        } catch (Exception e) {
//            logger.error("处理POST请求失败", e);
//            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    /**
//     * 添加观察者关系（ObservableResource接口要求）
//     */
//    @Override
//    public void addObserveRelation(ObserveRelation relation) {
//        // 这里由handleGET方法具体实现
//        // 这个方法通常不需要实现，因为我们在handleGET中已经处理了
//    }
//
//    /**
//     * 移除观察者关系（ObservableResource接口要求）
//     */
//    @Override
//    public void removeObserveRelation(ObserveRelation relation) {
//        // 遍历所有设备，移除该观察者
//        deviceObservers.forEach((deviceSn, observers) -> {
//            boolean removed = observers.removeIf(obs -> obs.equals(relation));
//            if (removed) {
//                logger.info("设备 {} 移除观察者，剩余: {}",
//                        deviceSn, observers.size());
//
//                // 如果该设备没有观察者了，可以清理资源
//                if (observers.isEmpty()) {
//                    deviceObservers.remove(deviceSn);
//                    logger.info("设备 {} 已无观察者，清理", deviceSn);
//                }
//            }
//        });
//    }
//
//
//    /**
//     * 为特定设备添加观察者
//     */
//    private synchronized void addDeviceObserver(String deviceSn, ObserveRelation relation) {
//        Set<ObserveRelation> observers = deviceObservers
//                .computeIfAbsent(deviceSn, k -> ConcurrentHashMap.newKeySet());
//
//        observers.add(relation);
//        // 设置取消处理器
//        removeDeviceObserver(deviceSn, relation);
//
//        logger.info("设备 {} 添加观察者成功，当前观察者数: {}",
//                deviceSn, observers.size());
//    }
//
//    /**
//     * 移除特定设备的观察者
//     */
//    private synchronized void removeDeviceObserver(String deviceSn, ObserveRelation relation) {
//        Set<ObserveRelation> observers = deviceObservers.get(deviceSn);
//        if (observers != null) {
//            observers.remove(relation);
//            logger.info("设备 {} 移除观察者，剩余: {}", deviceSn, observers.size());
//
//            // 清理空集合
//            if (observers.isEmpty()) {
//                deviceObservers.remove(deviceSn);
//                logger.info("设备 {} 已无观察者，清理", deviceSn);
//            }
//        }
//    }
//
//    /**
//     * 设备注册
//     */
//    private void registerDevice(CoapExchange exchange, String payload) {
//        try {
//            DeviceRegistration registration = DeviceRegistration.fromJson(payload);
//            String deviceSn = registration.getDeviceSn();
//
//            // 创建设备状态
//            DeviceStatus status = new DeviceStatus(
//                    deviceSn,
//                    registration.getDeviceType(),
//                    registration.getIpAddress(),
//                    System.currentTimeMillis(),
//                    "online"
//            );
//
//            deviceStatusMap.put(deviceSn, status);
//
//            // 响应
//            Response response = new Response(CoAP.ResponseCode.CREATED);
//            response.setPayload(String.format("设备 %s 注册成功", deviceSn));
//            response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
//
//            exchange.respond(response);
//
//            logger.info("设备注册成功: {}", deviceSn);
//
//            // 通知相关观察者设备上线
//            notifyDeviceStatusChange(deviceSn, status);
//
//        } catch (Exception e) {
//            logger.error("设备注册失败", e);
//            exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "注册信息格式错误");
//        }
//    }
//
//    /**
//     * 更新设备状态
//     */
//    private void updateDeviceStatus(CoapExchange exchange, String payload) {
//        try {
//            DeviceStatusUpdate update = DeviceStatusUpdate.fromJson(payload);
//            String deviceSn = update.getDeviceSn();
//
//            DeviceStatus currentStatus = deviceStatusMap.get(deviceSn);
//            if (currentStatus == null) {
//                exchange.respond(CoAP.ResponseCode.NOT_FOUND, "设备未注册");
//                return;
//            }
//
//            // 更新状态
//            currentStatus.updateFrom(update);
//            currentStatus.setLastHeartbeat(System.currentTimeMillis());
//            currentStatus.setStatus("online");
//
//            // 响应
//            exchange.respond(CoAP.ResponseCode.CHANGED, "状态更新成功");
//
//            logger.debug("设备状态更新: {}", deviceSn);
//
//            // 通知观察者
//            notifyDeviceStatusChange(deviceSn, currentStatus);
//
//        } catch (Exception e) {
//            logger.error("更新设备状态失败", e);
//            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
//        }
//    }
//
//    /**
//     * 核心功能：发送消息给指定设备
//     * 这是你要的功能 - 收到消息后推送给对应设备
//     */
//    private void sendMessageToDevice(CoapExchange exchange, String payload) {
//        try {
//            DeviceMessage message = DeviceMessage.fromJson(payload);
//            String targetDeviceSn = message.getTargetDeviceSn();
//
//            // 检查设备是否存在
//            if (!deviceStatusMap.containsKey(targetDeviceSn)) {
//                exchange.respond(CoAP.ResponseCode.NOT_FOUND,
//                        String.format("设备 %s 不存在", targetDeviceSn));
//                return;
//            }
//
//            // 检查设备是否在线
//            DeviceStatus status = deviceStatusMap.get(targetDeviceSn);
//            if (!"online".equals(status.getStatus())) {
//                exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE,
//                        String.format("设备 %s 不在线", targetDeviceSn));
//                return;
//            }
//
//            // 立即推送消息给观察该设备的客户端
//            boolean delivered = pushMessageToDevice(targetDeviceSn, message);
//
//            if (delivered) {
//                // 保存消息到设备历史
//                status.addMessage(message);
//
//                exchange.respond(CoAP.ResponseCode.CHANGED,
//                        String.format("消息已发送到设备 %s", targetDeviceSn));
//
//                logger.info("消息发送成功到设备: {}, 内容: {}",
//                        targetDeviceSn, message.getContent());
//            } else {
//                exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE,
//                        String.format("设备 %s 无观察者，消息暂存", targetDeviceSn));
//
//                // 可以存储到消息队列，等待设备连接
//                storePendingMessage(targetDeviceSn, message);
//            }
//
//        } catch (Exception e) {
//            logger.error("发送消息失败", e);
//            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    /**
//     * 推送消息到指定设备（核心推送逻辑）
//     * @param deviceSn 设备SN
//     * @param message 消息内容
//     * @return 是否成功推送
//     */
//    public boolean pushMessageToDevice(String deviceSn, DeviceMessage message) {
//        Set<ObserveRelation> observers = deviceObservers.get(deviceSn);
//
//        if (observers == null || observers.isEmpty()) {
//            logger.warn("设备 {} 无观察者，无法推送消息", deviceSn);
//            return false;
//        }
//
//        // 构建通知消息
//        Response notification = new Response(CoAP.ResponseCode.CONTENT);
//
//        // 创建通知负载
//        Map<String, Object> notificationData = new HashMap<>();
//        notificationData.put("type", "message");
//        notificationData.put("timestamp", System.currentTimeMillis());
//        notificationData.put("deviceSn", deviceSn);
//        notificationData.put("messageId", message.getMessageId());
//        notificationData.put("content", message.getContent());
//        notificationData.put("sender", message.getSender());
//
//        String jsonPayload = toJson(notificationData);
//        notification.setPayload(jsonPayload);
//        notification.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_JSON);
//        notification.getOptions().setObserve(getNextObserveNumber());
//
//        // 发送给所有观察者
//        boolean success = false;
//        int successCount = 0;
//        int totalCount = observers.size();
//
//        Iterator<ObserveRelation> iterator = observers.iterator();
//        while (iterator.hasNext()) {
//            ObserveRelation relation = iterator.next();
//            try {
//                // 发送通知
//                relation.getExchange().sendResponse(notification);
//                successCount++;
//                success = true;
//
//                logger.debug("消息推送到设备 {} 的观察者: {}",
//                        deviceSn, relation.getExchange().getRemoteSocketAddress());
//
//            } catch (Exception e) {
//                logger.error("推送消息失败，移除无效观察者", e);
//                iterator.remove();  // 移除失败的观察者
//            }
//        }
//
//        logger.info("设备 {} 消息推送完成: 成功 {}/{}",
//                deviceSn, successCount, totalCount);
//
//        // 清理空集合
//        if (observers.isEmpty()) {
//            deviceObservers.remove(deviceSn);
//            logger.info("设备 {} 已无观察者，清理", deviceSn);
//        }
//
//        return success;
//    }
//
//    /**
//     * 通知设备状态变化
//     */
//    private void notifyDeviceStatusChange(String deviceSn, DeviceStatus status) {
//        Set<ObserveRelation> observers = deviceObservers.get(deviceSn);
//
//        if (observers == null || observers.isEmpty()) {
//            return;
//        }
//
//        // 构建状态通知
//        Response notification = new Response(CoAP.ResponseCode.CONTENT);
//
//        Map<String, Object> statusData = new HashMap<>();
//        statusData.put("type", "status");
//        statusData.put("timestamp", System.currentTimeMillis());
//        statusData.put("deviceSn", deviceSn);
//        statusData.put("status", status.getStatus());
//        statusData.put("lastHeartbeat", status.getLastHeartbeat());
//        statusData.put("data", status.getData());
//
//        notification.setPayload(toJson(statusData));
//        notification.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_JSON);
//        notification.getOptions().setObserve(getNextObserveNumber());
//
//        // 发送通知
//        observers.forEach(relation -> {
//            try {
//                relation.getExchange().sendResponse(notification);
//            } catch (Exception e) {
//                logger.error("通知状态变化失败", e);
//            }
//        });
//    }
//
//    /**
//     * 启动定时任务
//     */
//    private void startScheduledTasks() {
//        // 1. 设备心跳检查（检测离线设备）
//        scheduler.scheduleAtFixedRate(this::checkDeviceHeartbeat,
//                60, 60, TimeUnit.SECONDS);
//
//        // 2. 观察者清理（移除长时间未更新的观察者）
//        scheduler.scheduleAtFixedRate(this::cleanupObservers,
//                300, 300, TimeUnit.SECONDS);
//
//        // 3. 状态定期广播（可选）
//        scheduler.scheduleAtFixedRate(this::broadcastStatusSummary,
//                30, 30, TimeUnit.SECONDS);
//    }
//
//    /**
//     * 检查设备心跳
//     */
//    private void checkDeviceHeartbeat() {
//        long now = System.currentTimeMillis();
//        long timeout = DEFAULT_HEARTBEAT_INTERVAL * 1000 * 3;  // 3倍心跳间隔
//
//        deviceStatusMap.forEach((deviceSn, status) -> {
//            if (now - status.getLastHeartbeat() > timeout) {
//                // 设备超时，标记为离线
//                status.setStatus("offline");
//                logger.warn("设备 {} 心跳超时，标记为离线", deviceSn);
//
//                // 通知观察者设备离线
//                notifyDeviceStatusChange(deviceSn, status);
//            }
//        });
//    }
//
//    /**
//     * 清理观察者
//     */
//    private void cleanupObservers() {
//        int totalBefore = getTotalObserverCount();
//
//        deviceObservers.entrySet().removeIf(entry -> {
//            String deviceSn = entry.getKey();
//            Set<ObserveRelation> observers = entry.getValue();
//
//            // 移除无效的观察者
//            observers.removeIf(relation -> {
//                try {
//                    // 检查观察者是否仍然有效
//                    return !relation.getExchange().isComplete();
//                } catch (Exception e) {
//                    return true;  // 移除异常的观察者
//                }
//            });
//
//            // 如果设备没有观察者了，清理
//            if (observers.isEmpty()) {
//                logger.debug("清理无观察者的设备: {}", deviceSn);
//                return true;
//            }
//
//            return false;
//        });
//
//        int totalAfter = getTotalObserverCount();
//        logger.info("观察者清理完成: {} -> {} (清理了 {} 个)",
//                totalBefore, totalAfter, totalBefore - totalAfter);
//    }
//
//    /**
//     * 广播状态摘要
//     */
//    private void broadcastStatusSummary() {
//        // 可以定期广播系统状态给所有观察者
//        // 实现略...
//    }
//
//    /**
//     * 存储待处理消息
//     */
//    private void storePendingMessage(String deviceSn, DeviceMessage message) {
//        // 实现消息队列存储
//        // 可以使用数据库、Redis或内存队列
//        logger.info("设备 {} 无观察者，消息暂存: {}", deviceSn, message.getMessageId());
//    }
//
//    /**
//     * 从请求中提取设备SN
//     */
//    private String extractDeviceSnFromRequest(CoapExchange exchange) {
//        // 方式1：从URI查询参数获取
//        String query = exchange.getRequestOptions().getUriQueryString();
//        if (query != null && query.contains("sn=")) {
//            return query.substring(query.indexOf("sn=") + 3);
//        }
//
//        // 方式2：从URI路径获取
//        List<String> uriPath = exchange.getRequestOptions().getUriPath();
//        if (uriPath.size() > 1) {
//            // 假设路径格式: /devices/{sn}
//            return uriPath.get(1);
//        }
//
//        // 方式3：从负载中提取（对于POST请求）
//        return null;
//    }
//
//    /**
//     * 返回所有设备状态
//     */
//    private void respondWithAllDevices(CoapExchange exchange) {
//        Map<String, Object> responseData = new HashMap<>();
//        responseData.put("total", deviceStatusMap.size());
//        responseData.put("online",
//                deviceStatusMap.values().stream()
//                        .filter(s -> "online".equals(s.getStatus()))
//                        .count());
//        responseData.put("devices", deviceStatusMap.values());
//
//        exchange.respond(CoAP.ResponseCode.CONTENT,
//                toJson(responseData),
//                MediaTypeRegistry.APPLICATION_JSON);
//    }
//
//    /**
//     * 获取下一个观察序号
//     */
//    private int getNextObserveNumber() {
//        return observeSequence.incrementAndGet() & 0xFFFFFF;  // 限制在24位
//    }
//
//    /**
//     * 获取设备观察者数量
//     */
//    public int getObserverCount(String deviceSn) {
//        Set<ObserveRelation> observers = deviceObservers.get(deviceSn);
//        return observers != null ? observers.size() : 0;
//    }
//
//    /**
//     * 获取总观察者数量
//     */
//    public int getTotalObserverCount() {
//        return deviceObservers.values().stream()
//                .mapToInt(Set::size)
//                .sum();
//    }
//
//    /**
//     * 获取设备状态
//     */
//    public DeviceStatus getDeviceStatus(String deviceSn) {
//        return deviceStatusMap.get(deviceSn);
//    }
//
//    /**
//     * 获取所有设备SN
//     */
//    public Set<String> getAllDeviceSns() {
//        return new HashSet<>(deviceStatusMap.keySet());
//    }
//
//    /**
//     * 检查设备是否有观察者
//     */
//    public boolean hasObservers(String deviceSn) {
//        Set<ObserveRelation> observers = deviceObservers.get(deviceSn);
//        return observers != null && !observers.isEmpty();
//    }
//
//    /**
//     * JSON序列化辅助方法
//     */
//    private String toJson(Object obj) {
//        // 这里可以使用Jackson、Gson或FastJSON
//        // 简化实现，实际项目中应使用JSON库
//        if (obj instanceof Map) {
//            Map<?, ?> map = (Map<?, ?>) obj;
//            StringBuilder sb = new StringBuilder("{");
//            map.forEach((k, v) -> {
//                sb.append("\"").append(k).append("\":");
//                if (v instanceof String) {
//                    sb.append("\"").append(v).append("\"");
//                } else {
//                    sb.append(v);
//                }
//                sb.append(",");
//            });
//            if (sb.length() > 1) {
//                sb.deleteCharAt(sb.length() - 1);
//            }
//            sb.append("}");
//            return sb.toString();
//        }
//        return "{}";
//    }
//
//
//
//    // ==================== 内部数据类 ====================
//
//    /**
//     * 设备状态类
//     */
//    @Data
//    public static class DeviceStatus {
//        private String deviceSn;
//        private String deviceType;
//        private String ipAddress;
//        private long lastHeartbeat;
//        private String status;  // online, offline, error
//        private Map<String, Object> data = new HashMap<>();
//        private List<DeviceMessage> messageHistory = new ArrayList<>();
//
//        public DeviceStatus(String deviceSn, String deviceType,
//                            String ipAddress, long lastHeartbeat, String status) {
//            this.deviceSn = deviceSn;
//            this.deviceType = deviceType;
//            this.ipAddress = ipAddress;
//            this.lastHeartbeat = lastHeartbeat;
//            this.status = status;
//        }
//
//        public void updateFrom(DeviceStatusUpdate update) {
//            if (update.getData() != null) {
//                this.data.putAll(update.getData());
//            }
//        }
//
//        public void addMessage(DeviceMessage message) {
//            if (messageHistory.size() >= 100) {
//                messageHistory.remove(0);  // 限制历史记录长度
//            }
//            messageHistory.add(message);
//        }
//    }
//
//    /**
//     * 设备注册信息
//     */
//    @Data
//    public static class DeviceRegistration {
//        private String deviceSn;
//        private String deviceType;
//        private String ipAddress;
//
//        public static DeviceRegistration fromJson(String json) {
//            // 简化实现，实际应使用JSON解析
//            DeviceRegistration reg = new DeviceRegistration();
//            // 解析JSON...
//            return reg;
//        }
//    }
//
//    /**
//     * 设备状态更新
//     */
//    @Data
//    public static class DeviceStatusUpdate {
//        private String deviceSn;
//        private Map<String, Object> data;
//
//        public static DeviceStatusUpdate fromJson(String json) {
//            // 简化实现
//            return new DeviceStatusUpdate();
//        }
//    }
//
//    /**
//     * 设备消息
//     */
//    @Data
//    public static class DeviceMessage {
//        private String messageId;
//        private String targetDeviceSn;
//        private String sender;
//        private String content;
//        private long timestamp;
//        private Map<String, Object> extras;
//
//        public static DeviceMessage fromJson(String json) {
//            // 简化实现
//            DeviceMessage msg = new DeviceMessage();
//            msg.setMessageId(UUID.randomUUID().toString());
//            msg.setTimestamp(System.currentTimeMillis());
//            return msg;
//        }
//    }
//}