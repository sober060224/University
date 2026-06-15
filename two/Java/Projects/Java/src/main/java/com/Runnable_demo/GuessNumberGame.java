package com.Runnable_demo;

class NumberGame {
    private int targetNumber;
    private int guess;
    private boolean numberSet = false;
    private boolean guessed = false;
    private boolean gameOver = false;

    // 第一个线程设置数字
    public synchronized void setNumber(int number) {
        while (numberSet) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.targetNumber = number;
        numberSet = true;
        System.out.println("第一个线程设置要猜的数字为: " + number);
        notifyAll();
    }

    // 第二个线程猜数字
    public synchronized void guessNumber(int guess) {
        while (!numberSet || guessed) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.guess = guess;
        guessed = true;
        System.out.println("第二个线程猜测: " + guess);
        notifyAll();
    }

    // 第一个线程给出提示
    public synchronized void giveHint() {
        while (!guessed || gameOver) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (guess < targetNumber) {
            System.out.println("提示: 猜小了");
        } else if (guess > targetNumber) {
            System.out.println("提示: 猜大了");
        } else {
            System.out.println("提示: 猜对了！游戏结束");
            gameOver = true;
        }

        guessed = false;
        notifyAll();
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized int getTargetNumber() {
        return targetNumber;
    }
}

// 第一个线程：生成数字
class NumberGenerator extends Thread {
    private NumberGame game;

    public NumberGenerator(NumberGame game) {
        this.game = game;
    }

    public void run() {
        // 生成1-10的随机整数
        int number = (int) (Math.random() * 10) + 1;
        game.setNumber(number);

        while (!game.isGameOver()) {
            game.giveHint();
            try {
                Thread.sleep(100); // 短暂休眠避免过度占用CPU
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

// 第二个线程：猜数字
class NumberGuesser extends Thread {
    private NumberGame game;

    public NumberGuesser(NumberGame game) {
        this.game = game;
    }

    public void run() {
        int guess = 1;

        while (!game.isGameOver() && guess <= 10) {
            game.guessNumber(guess);

            // 等待提示
            try {
                Thread.sleep(100); // 短暂休眠避免过度占用CPU
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 如果猜对了，游戏会在giveHint中结束
            // 如果没猜对，继续猜下一个数字
            guess++;
        }
    }
}

public class GuessNumberGame {
    public static void main(String[] args) {
        NumberGame game = new NumberGame();

        NumberGenerator generator = new NumberGenerator(game);
        NumberGuesser guesser = new NumberGuesser(game);

        System.out.println("双线程猜数字游戏开始！");
        generator.start();
        guesser.start();

        try {
            generator.join();
            guesser.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("游戏结束，两个线程都已进入死亡状态");
    }
}
