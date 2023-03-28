package com.roly.consumer.controller;

import com.roly.api.ProviderApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Date: 2023/03/28/17:15
 * @Description:
 */
@RestController
public class ConsumerController {

    @Resource
    ProviderApi providerApi;


    @RequestMapping(value = "/test1/{data}", method = RequestMethod.GET)
    String test1(@PathVariable(value = "data") String data) {
        return providerApi.test1(data);
    }

    @RequestMapping(value = "/test2", method = RequestMethod.POST)
    String test2(@RequestBody String data) {
        return providerApi.test2(data);
    }

}
