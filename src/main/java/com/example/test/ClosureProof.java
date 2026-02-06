//package com.example.test;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//public class ClosureProof {
//    private static Set<String> removed = new HashSet<>();
//
//    ExecutorService executor = Executors.newFixedThreadPool(12);
//
//    public static void main(String[] args) throws Exception {
//        ExecutorService executor = Executors.newFixedThreadPool(10);
//        ClosureProof closureProof = new ClosureProof();
//        // 模拟你的场景
//        for (int i = 0; i < 200; i++) {
//            final int id = i;
//            executor.submit(() -> {
//                closureProof.processDevice("SN" + id);
//            });
//        }
//
//        executor.shutdown();
//        executor.awaitTermination(1, TimeUnit.SECONDS);
//
//        System.out.println("移除的设备: " + removed);
//        System.out.println("是否有重复移除? " + (removed.size() != 10));
//    }
//
//    public Boolean processDevice(String deviceSn) {
//        // 每个调用有自己的deviceSn变量
//       // final String deviceSn = sn;
//        final boolean result = false;
//        executor.submit(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    if (deviceSn.equals("SN145")) {
//                        throw new RuntimeException("模拟SN2异常");
//                    }
//                    System.out.println("成功处理: " + deviceSn);
//                } catch (Exception e) {
//                    System.out.println("异常处理: " + deviceSn);
//                    removed.add(deviceSn);
//                }
//            }
//        });
//        return result;
//    }
//}