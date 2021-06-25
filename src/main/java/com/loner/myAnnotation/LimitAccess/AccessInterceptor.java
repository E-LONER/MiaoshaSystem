package com.loner.myAnnotation.LimitAccess;

import com.alibaba.fastjson.JSON;
import com.loner.config.UserContext_localThread;
import com.loner.domain.MiaoshaUser;
import com.loner.redis.RedisService;
import com.loner.redis.keyprefix.AccessKey;
import com.loner.result.CodeMsg;
import com.loner.result.Result;
import com.loner.service.UserService;

import com.sun.xml.internal.ws.client.sei.MethodHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserService userService;

    @Override
    //方法调用前的拦截器，即实现请求频繁注解，写完注解主体后，记得要在config包下的WebConfig类中注册注解，才可以调用
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (handler instanceof HandlerMethod){
            MiaoshaUser user=getUser(request,response);
            UserContext_localThread.setUser(user);
            HandlerMethod handlerMethod= (HandlerMethod) handler;//创建一个handler对象
            AccessLimit accessLimit=handlerMethod.getMethodAnnotation(AccessLimit.class);//获取自己创建的注解对象
            if (accessLimit ==null)
                return true;
            boolean login = accessLimit.login();
            int maxAccess = accessLimit.maxAccess();
            int time = accessLimit.time();
            String uri_key=request.getRequestURI();
            if (login && user ==null){
                sendMsg(Result.error(CodeMsg.loginUserNotExit),response);
                return false;
            }else if (login && user !=null){
                uri_key+=":"+user.getId();
            }else{

            }
            String key=AccessKey.getById(uri_key).getPrefix();
            long access_count=redisService.get(key,Long.class);
            if (access_count==0){
                redisService.set(key,time,1);
            }else if(access_count<maxAccess){
                redisService.decr(key);
            }else {
                sendMsg(Result.error(CodeMsg.Access_ERR),response);
                return false;
            }


        }
        return true;
    }

    private void sendMsg(Result<Object> error, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream outpuStream=response.getOutputStream();
        String str= JSON.toJSONString(error);
        outpuStream.write(str.getBytes("UTF-8"));
        outpuStream.flush();
        outpuStream.close();
    }

    //从cookie中取出用户信息
    private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response) {
        String paramKeyId=request.getParameter(UserService.COOKIE_NAME);
        String cookireKeyId=getCookieValue(request,UserService.COOKIE_NAME);
        if(StringUtils.isEmpty(paramKeyId) && StringUtils.isEmpty(cookireKeyId)){
            return null;
        }
        String keyId=StringUtils.isEmpty(paramKeyId)?cookireKeyId:paramKeyId;
        String userId=userService.getByKeyId(keyId);
        MiaoshaUser miaoshaUser=new MiaoshaUser();
        miaoshaUser.setId(Long.parseLong(userId));
        return miaoshaUser;
    }

    //遍历所有cookies获得user的cookie
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies=request.getCookies();
        if(cookies == null || cookies.length <= 0){
            return null;
        }
        for(Cookie cookie:cookies){
            if (cookie.getName().equals(cookieName)){
                return cookie.getValue();
            }

        }
        return null;
    }

}
