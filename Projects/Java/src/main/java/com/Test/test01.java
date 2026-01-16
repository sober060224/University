package com.Test;

class People {
    String name;

    People(String name) {
        this.name = name;
    }
}

class Student extends People {
    String ooo;

    public Student(String ooo) {
        super(ooo);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

public class test01 {
    public static void main(String[] args) {
        int[] arr2 = new int[] { 1, 2, 3 };
        int[][] arr1 = new int[][] { { 1, 2 }, { 2, 3 } };

        System.out.println("arr2:");
        for (int i : arr2)
            System.out.println(i);

        System.out.println("arr1:");
        for (int[] arr : arr1) {
            for (int i : arr) {
                System.out.printf("%d ", i);
            }
            System.out.println();
        }

        String a = "abc";
        char c = 'd';
        System.out.println(a + c);

        byte b = 10;
        b += 1;
        System.out.println(b);

        Student s = new Student("asdfasd");
        System.out.println(s.getName());

        int i = 2;
        // print prime
        while (i < 1000) {
            boolean isPrime = true;

            // 有一个除的尽就不是素数（除了1和它本身）
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