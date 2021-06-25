package com.loner.redis.keyprefix;

public class OrderKey extends KeyUtilsAbstractImpl{

    //默认redis键的有效期,两天
    private static final int DEFAULT_EXPIRE_TIME=3*60;//2分钟
    private OrderKey(String prefix, String key) {
        super(prefix,key, DEFAULT_EXPIRE_TIME);
    }
    //利用private构造函数创建对象，防止被人获取，封装性
    public static OrderKey getById(String key){return new OrderKey("ORDER",key);}

    public static int getExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}
