package com.roily.designpatterns.dpmain.proxy.notstaticproxy;

import org.junit.Test;

/**
 * @version 1.0.0
 * @Description 我想买房子
 * @ClassName Client.java
 * @author: RoilyFish
 * @date: 2022/6/8 23:00
 */
public class Client {

    /**
     * 直接找房东买：两个人都会不签合同，房东信息容易泄露
     */
    @Test
    public void test01(){
        HouseOwner houseOwner = new HouseOwner();
        houseOwner.sellHouse();
    }

    /**
     * 经纪人，就比较专业。
     */
    @Test
    public void test2(){
        //经纪人
        HouseProxy houseProxy = new HouseProxy();
        houseProxy.sellHouse();
    }



}
