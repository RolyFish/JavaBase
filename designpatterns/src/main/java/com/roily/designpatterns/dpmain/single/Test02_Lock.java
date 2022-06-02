package com.roily.designpatterns.dpmain.single;

import org.junit.Test;

/**
 * @version 1.0.0
 * @Description
 * @ClassName Test01.java
 * @author: RoilyFish
 * @date: 2022/6/2 11:09
 */
public class Test02_Lock {

    /**
     * 多次获取同一个实例
     */
    @Test
    public void test01(){
        System.out.println(Singleton02_Lock.getInstance().hashCode());
        System.out.println(Singleton02_Lock.getInstance().hashCode());
        System.out.println(Singleton02_Lock.getInstance().hashCode());
        System.out.println(Singleton02_Lock.getInstance().hashCode());
    }

    /**
     * lock  测试多线程情况下会创建多个不同实例
     */
    @Test
    public void test02(){

        for (int i = 0; i < 100; i++) {
            new Thread(()->{
                System.out.println(Singleton02_Lock.getInstance().hashCode());
            }).start();
        }
        //保证线程执行结束
        while (true);
    }

}
