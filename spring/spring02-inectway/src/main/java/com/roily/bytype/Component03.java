package com.roily.bytype;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author rolyfish
 * @Date: 2022/10/09/15:54
 * @Description:
 */
@Component
public class Component03 {

    //默认按照类型注入
    @Resource
    Component01 component01;

    /**
     * 通过构造器注入，可以省略@AutoWrite
     * @param component01
     */
    // public Component02(Component01 component01) {
    //     this.component01 = component01;
    // }

}
