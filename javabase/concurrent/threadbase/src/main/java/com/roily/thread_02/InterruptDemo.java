package com.roily.thread_02;

/**
 * 线程终端
 *
 *  Thread.sleep(1000);被中断会出现中断异常
 */
public class InterruptDemo {

    public static void main(String[] args) {

        //sleepThread睡眠1000ms
        Thread sleepThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        //busyThread一直执行死循环
        Thread busyThread = new Thread(() -> {
            while (true) ;
        });

        sleepThread.start();
        busyThread.start();

        sleepThread.interrupt();
        busyThread.interrupt();

        while (sleepThread.isInterrupted()) ;
        System.out.println("sleepThread isInterrupted: " + sleepThread.isInterrupted());
        System.out.println("busyThread isInterrupted: " + busyThread.isInterrupted());
    }
}