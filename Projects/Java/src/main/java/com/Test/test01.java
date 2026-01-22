Projects/Java/src/main/java/com/Test/test01.java
package com.Test;

/**
 * 测试类，包含人员类、学生类和主程序
 */
class People {
    /**
     * 姓名属性
     */
    String name;

    /**
     * 构造函数，初始化姓名
     * @param name 要设置的姓名
     */
    People(String name) {
        this.name = name;
    }
}

class Student extends People {
    /**
     * 额外属性
     */
    String ooo;

    /**
     * 学生构造函数，调用父类构造函数
     * @param ooo 要设置的额外属性
     */
    public Student(String ooo) {
        super(oo);
    }

    /**
     * 获取姓名
     * @return 返回姓名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置姓名
     * @param name 要设置的新姓名
     */
    public void setName(String name) {
        this.name = name;
    }
}

public class test01 {
    public static void main(String[] args) {
        // 声明并初始化一维整数数组
        int[] arr2 = new int[] { 1, 2, 3 };
        
        // 声明并初始化二维整数数组
        int[][] arr1 = new int[][] { { 1, 2 }, { 2, 3 } };

        // 打印一维数组
        System.out.println("arr2:");
        for (int i : arr2)
            System.out.println(i);

        // 打印二维数组
        System.out.println("arr1:");
        for (int[] arr : arr1) {
            for (int i : arr) {
                System.out.printf("%d ", i);
            }
            System.out.println();
        }

        // 字符串与字符拼接
        String a = "abc";
        char c = 'd';
        System.out.println(a + c);

        // 字节类型变量操作
        byte b = 10;
        b += 1;
        System.out.println(b);

        // 创建学生对象并获取姓名
        Student s = new Student("asdfasd");
        System.out.println(s.getName());

        // 打印素数
        int i = 2;
        // 使用while循环打印1000以内的素数
        while (i < 1000) {
            boolean isPrime = true;

            // 检查是否为素数（优化：只检查到平方根）
            for (int j = 2; j <= Math.sqrt(i); j++) {
                if (i % j == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime)
                System.out.println(i);
            i++;
        }
    }
}
