package com.loner.controller;

import com.loner.myAnnotation.LimitAccess.AccessLimit;
import com.loner.redis.RedisService;
import com.loner.result.Result;
import com.loner.service.UserService;
import com.loner.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.Date;


@Controller
//@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@EnableAutoConfiguration
@RequestMapping("/login")
public class LoginController {
    //打印日志
    private static Logger log= LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    RedisService redisService;

    //向客户端返回login页面
    @RequestMapping("/tologin")
    public String to_login(){
        return "login";
    }

    //执行login操作

    @RequestMapping("/dologin")
    @ResponseBody
    //@valid用于校验变量使用，除了自身已经定义好的可以直接使用外，还可以自定义一些校验方法，https://blog.csdn.net/weixin_38118016/article/details/80977207
    public Result<?> dologin(HttpServletResponse response, @Valid LoginVo loginVo){
        //打印日志信息
        log.info(loginVo.getPhoneNum() + "用户在" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"尝试登录");
        //登录实现
        Result<?> result=userService.login(response,loginVo);
        return result;

    }

//    public static void main(String[] args) {
//        SpringApplication.run(LoginController.class,args);
//
//
//    }
}
