package com.example;

// 定义一个生产玩具和对面包的生产者和消费者程序，使用线程来模拟生产和消费过程。
public class ToyAndSteamedBreadProducerSimple {

    private static int toyCount = 0; // 记录生产的玩具数量
    private static int steamedBreadCount = 0; // 记录吃掉的馒头（对面包）的数量
    private static boolean isEating = false; // 是否正在吃馒头的状态标志
    private static boolean isExit = false; // 退出状态标志，用于线程安全地停止生产过程
    private static final Object lock = new Object(); // 同步锁对象，保证操作的原子性

    public static void main(String[] args) {
        System.out.println("工人开始工作...");

        // 创建一个线程来模拟生产玩具的过程
        Thread produceThread = new Thread(() -> {
            while (toyCount < 50) { // 循环条件：总共需要生产50个玩具
                synchronized (lock) { // 同步代码块，保证操作的原子性
                    // 如果正在吃馒头，则等待直到可以继续生产
                    while (isEating) {
                        try {
                            System.out.println("生产暂停，等待吃完馒头...");
                            lock.wait(); // 释放锁并休眠，直到被notifyAll唤醒
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // 恢复中断状态
                        }
                    }
                }

                // 模拟生产和吃馒头的时间间隔（例如200毫秒）来控制生产速度。
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 恢复中断状态
                }

                synchronized (lock) { // 同步代码块，保证操作的原子性
                    toyCount++; // 生产一个玩具
                    System.out.println("生产第 " + toyCount + " 个玩具");

                    if (toyCount % 20 == 0) { // 每生产完20个玩具就通知吃馒头线程开始吃馒头。
                        isEating = true;
                        lock.notifyAll(); // 唤醒所有等待的线程，这里是吃馒头的那条线程
                    }
                }

                if (toyCount == 50) { // 如果已经生产了50个玩具就退出循环并打印完成信息。
                    System.out.println("\n已完成50个玩具的生产!");
                    isExit = true;
                    break;
                }
            }
        });

        // 创建一个线程来模拟吃馒头的过程，每吃完3个馒头（对面包）就通知生产者可以继续生产。
        Thread eatThread = new Thread(() -> {
            while (!isExit) { // 循环条件：不等于退出状态标志
                synchronized (lock) { // 同步代码块，保证操作的原子性
                    // 如果当前不是吃馒头的状态就等待直到可以使用锁（即可以开始吃馒头）。
                    while (!isEating) {
                        try {
                            lock.wait(); // 释放锁并休眠，直到被notifyAll唤醒
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // 恢复中断状态
                        }
                    }
                }

                synchronized (lock) { // 同步代码块，保证操作的原子性
                    steamedBreadCount++; // 吃掉一个馒头（对面包）
                    System.out.println("吃第 " + steamedBreadCount + " 个馒头");

                    if (steamedBreadCount == 3) { // 每吃完3个馒头就允许生产者开始继续生产和通知所有等待的线程。
                        isEating = false;
                        steamedBreadCount = 0; // 重置馒头计数器，以便可以再次吃到第1个馒头（对面包）。
                        lock.notifyAll(); // 唤醒所有等待的线程，这里是生产玩具的那条线程
                    }
                }

                try {
                    Thread.sleep(1000); // 模拟吃一个馒头的耗时时间间隔（例如1000毫秒）。
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 恢复中断状态
                }
            }
        });

        // 启动生产者和消费者线程
        produceThread.start();
        eatThread.start();

        // 等待所有任务完成（即所有的玩具都已经被生产和吃掉）。
        try {
            produceThread.join();
            eatThread.join();
            System.out.println("\n所有任务完成!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
    }
}