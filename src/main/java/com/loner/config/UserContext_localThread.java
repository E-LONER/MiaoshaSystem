package com.loner.config;

import com.loner.domain.MiaoshaUser;

//threadlocal而是一个线程内部的存储类，可以在指定线程内存储数据，数据存储以后，只有指定线程可以得到存储数据
public class UserContext_localThread {
    private static ThreadLocal<MiaoshaUser> userThreadLocal=new ThreadLocal<>();

    public static void setUser(MiaoshaUser user){
        userThreadLocal.set(user);
    }

    public static MiaoshaUser getUser(){
        return userThreadLocal.get();
    }
}
