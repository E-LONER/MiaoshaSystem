package com.loner.controller;

import com.loner.rabbitmq.MQsender;
import com.loner.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
public class TestController {

    @Autowired
    private MQsender mQsender;

//    @RequestMapping("/mq")
//    @ResponseBody
//    public Result<String> mgSender(){
//        mQsender.send("hello world");
//        return Result.success("hello world");
//    }
}
