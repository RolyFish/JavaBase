package com.roily.deadlock;

/**
 * descripte: 死锁
 *
 * @author: RoilyFish
 * @date: 2022/3/13
 */
public class DeadLock {


    private static String resource_a = "A";
    private static String resource_b = "B";

    public static void main(String[] args) {
        deadLock2();
    }

    public static void deadLock2() {
        Thread threadA = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (resource_a) {
                    System.out.println("get resource a");
                    try {
                        Thread.sleep(3000);
                        synchronized (resource_b) {
                            System.out.println("get resource b");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Thread threadB = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (resource_b) {
                    System.out.println("get resource b");
                    synchronized (resource_a) {
                        System.out.println("get resource a");
                    }
                }
            }
        });
        threadA.start();
        threadB.start();

    }


}
