package com.loner.service;

import com.loner.Exceptions.GlobalException;
import com.loner.dao.UserInfoDao;
import com.loner.domain.UserInfo;
import com.loner.redis.RedisService;
import com.loner.redis.keyprefix.User;
import com.loner.result.CodeMsg;
import com.loner.result.Result;
import com.loner.utils.MD5Util;
import com.loner.utils.UUIDUtil;
import com.loner.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;



@Service
public class UserService {
    //用户COOKIE名
    public static final String COOKIE_NAME="user";

    @Autowired
    private UserInfoDao userInfoDao;

    @Autowired
    RedisService redisService;

    public UserInfo getById(long id){
        return userInfoDao.getById(id);
    }

    public String getByKeyId(String keyId) {
        if(keyId ==null)
            return null;
        return redisService.get(keyId,String.class);
    }

    //登录方法

    public Result<?> login(HttpServletResponse response, LoginVo loginVo) {
        String password=loginVo.getPassword();
        String phoneNum=loginVo.getPhoneNum();
        //判断手机号是否存在
        UserInfo userInfo=getById(Long.parseLong(phoneNum));
        if(userInfo==null)
            throw new GlobalException(CodeMsg.loginUserNotExit);
        //密码验证
        String dbPassword=userInfo.getPassword();
        String dbSalt=userInfo.getSalt();
        String md5CalculatePassword= MD5Util.uPasswordToDBpassword(password,dbSalt);
        if (!md5CalculatePassword.equals(dbPassword))
            return Result.error(CodeMsg.loginPasswordError);
        //生成cookie
        String token= UUIDUtil.uuid();
        addCookie(response,phoneNum,token);
        return Result.success(token);
    }

    private void addCookie(HttpServletResponse response,String user,String cookieStr){
        Cookie cookie=new Cookie(COOKIE_NAME,cookieStr);
        //设置cookie的有效期，与redis键有效期保持一致
        cookie.setMaxAge(User.getById(cookieStr).getExpire());
        redisService.set(cookieStr,User.getExpire(),user);
        //网站根目录，是相对于应用服务器存放应用的文件夹的根目录而言的(比如tomcat下面的webapp)，
        //因此cookie.setPath("/");之后，可以在webapp文件夹下的所有应用共享cookie，
        cookie.setPath("/");
        //将cookie返回给客户端
        response.addCookie(cookie);
    }

}
