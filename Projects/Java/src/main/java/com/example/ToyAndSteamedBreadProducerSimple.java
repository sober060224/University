package com.example;

public class ToyAndSteamedBreadProducerSimple {

    private static int toyCount = 0;
    private static int steamedBreadCount = 0;
    private static boolean isEating = false;
    private static boolean isExit = false;
    private static final Object lock = new Object();

    public static void main(String[] args) {
        System.out.println("工人开始工作...");

        // 生产玩具的线程
        Thread produceThread = new Thread(() -> {
            while (toyCount < 50) {
                synchronized (lock) {
                    // 如果正在吃馒头，则等待
                    while (isEating) {
                        try {
                            System.out.println("生产暂停，等待吃完馒头...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                // 生产一个玩具
                toyCount++;
                System.out.println("生产第 " + toyCount + " 个玩具");

                // 如果生产到20个，开始吃馒头
                if (toyCount % 20 == 0) {
                    synchronized (lock) {
                        System.out.println("\n已生产20个玩具,开始吃馒头...");
                        isEating = true;
                        lock.notifyAll(); // 通知吃馒头线程
                    }
                }

                // 模拟生产时间
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 如果生产完50个，结束
                if (toyCount == 50) {
                    System.out.println("\n已完成50个玩具的生产!");
                    isExit = true;
                    break;
                }
            }
        });

        // 吃馒头的线程
        Thread eatThread = new Thread(() -> {
            while (!isExit) {
                synchronized (lock) {
                    // 如果不是吃馒头状态，则等待
                    while (!isEating) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                // 吃馒头
                steamedBreadCount++;
                System.out.println("吃第 " + steamedBreadCount + " 个馒头");

                // 如果吃完3个馒头，恢复生产
                if (steamedBreadCount == 3) {
                    synchronized (lock) {
                        System.out.println("\n已吃完3个馒头，恢复生产玩具...\n");
                        isEating = false;
                        steamedBreadCount = 0; // 重置馒头计数
                        lock.notifyAll(); // 通知生产线程
                    }
                }

                // 模拟吃馒头时间
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 如果已经生产完50个玩具，结束吃馒头线程
                if (toyCount >= 50) {
                    break;
                }
            }
        });

        // 启动线程
        produceThread.start();
        eatThread.start();

        // 等待线程结束
        try {
            produceThread.join();
            eatThread.join();
            System.out.println("\n所有任务完成!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}