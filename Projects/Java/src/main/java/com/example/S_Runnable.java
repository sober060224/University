package com.example;

class MyRunnable implements Runnable {
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(Thread.currentThread().getName() + "正在运行：" + i);
            if (i == 2) {
                System.out.print("线程礼让：");
                Thread.yield();
            }
        }
    }
}

public class S_Runnable {
    public static void main(String[] args) {
        Test.S_Runnable_test02();
    }
}