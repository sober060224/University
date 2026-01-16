package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

class Ticket {
    private int count = 1;

    public void sale() {
        while (true) {
            synchronized (this) {
                if (count > 200) {
                    System.out.println("票已经卖完了");
                    break;
                } else
                    System.out.println(Thread.currentThread().getName() + "卖的第" + count++ + "张票");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class SaleWindows extends Thread {
    private Ticket ticket;

    public SaleWindows(String name, Ticket ticket) {
        super(name);
        this.ticket = ticket;
    }

    @Override
    public void run() {
        super.run();
        ticket.sale();
    }
}

public class Test {
    static void test01() {
        MyRunnable a = new MyRunnable();
        Thread AThread = new Thread(a);

        new Thread(() -> {
            System.out.println("lambda线程");
        }).start();

        AThread.start();
    }

    static void test02() {
        Thread t1 = new Thread(new MyRunnable(), "线程A");
        Thread t2 = new Thread(new MyRunnable(), "线程B");
        t1.setPriority(Thread.MAX_PRIORITY);
        t2.setPriority(Thread.MIN_PRIORITY);
        t1.start();
        t2.start();
    }

    static void test03() {
        Thread a = new Thread(new MyRunnable());
        Thread b = new Thread(new MyRunnable());

        a.start();
        b.start();
        for (int i = 0; i < 20; i++) {
            if (i == 10) {
                try {
                    b.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(Thread.currentThread().getName() + "运行：" + i);
        }
    }

    static void test04() {
        Thread a = new Thread(new MyRunnable());
        Thread b = new Thread(new MyRunnable());
        a.start();
        b.start();
    }

    static void FILE_IO_test01(String name, String type) {
        try {
            Reader fr = new FileReader("introduction.txt");
            char ch[] = new char[1024];
            StringBuffer sbf = new StringBuffer();
            int length = fr.read(ch);
            while ((length != -1)) {
                sbf.append(ch);
                length = fr.read();
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void FILE_IO_test02() {
        try {
            Reader fr = new FileReader("introduction.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null)
                System.out.println(line);

            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void FILE_IO_test03() {
        try {
            Writer wt = new FileWriter("introduction.txt");
            BufferedWriter bwt = new BufferedWriter(wt);
            String information = "Hello Java I/O!";
            bwt.write(information);
            bwt.flush();
            bwt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void FILE_IO_test04() {
        try {
            InputStream is = new FileInputStream("introduction.txt");
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);

            System.out.println(dis.read());
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void FILE_IO_test05() {
        try (
                BufferedInputStream bis = new BufferedInputStream(
                        new FileInputStream("D:\\AAA_Software\\文件\\照片\\证件\\295_413证件照.png"));
                BufferedOutputStream bos = new BufferedOutputStream(
                        new FileOutputStream("D:\\AAA_Software\\文件\\照片\\证件\\copy.png"));) {
            byte[] buffer = new byte[8172];
            int len;
            long currentTime = System.currentTimeMillis();

            while ((len = bis.read(buffer)) != -1)
                bos.write(buffer, 0, len);
            System.out.println("copy success");

            long endTime = System.currentTimeMillis();
            System.out.println("time:" + (endTime - currentTime) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // try {
        // BufferedInputStream bis = new BufferedInputStream(
        // new FileInputStream("D:\\AAA_Software\\文件\\照片\\证件\\295_413证件照.png"));
        // BufferedOutputStream bos = new BufferedOutputStream(
        // new FileOutputStream("D:\\AAA_Software\\文件\\照片\\证件\\copy.png"));
        // byte[] buffer = new byte[8192];
        // int bytesRead;
        // while ((bytesRead = bis.read(buffer)) != -1)
        // bos.write(buffer, 0, bytesRead);
        // System.out.println("文件复制成功");
        // bis.close();
        // bos.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
    }

    static void S_Runnable_test01() {
        Ticket ticket = new Ticket();

        SaleWindows w1 = new SaleWindows("windows1", ticket), w2 = new SaleWindows("windows2", ticket),
                w3 = new SaleWindows("windows3", ticket);

        w1.start();
        w2.start();
        w3.start();
    }

    static void S_Runnable_test02() {

    }
}