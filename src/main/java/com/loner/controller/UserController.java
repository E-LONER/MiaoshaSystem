package com.loner.controller;

import com.loner.domain.UserInfo;
import com.loner.result.Result;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
@RequestMapping("/user")
public class UserController {
    @RequestMapping("/getuserinfo")
    @ResponseBody
    /**
     * QPS=930.7/sec
     * 平均值：114
     */
    public Result<String>  userinfo(Model model,String user){
        return Result.success(user);
    }
}
