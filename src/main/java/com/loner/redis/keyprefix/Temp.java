package com.loner.redis.keyprefix;

public class Temp extends KeyUtilsAbstractImpl{

    //默认redis键的有效期,两天
    private static final int DEFAULT_EXPIRE_TIME=10*3600;
    public Temp(String prefix, String key, int expiretime) {
        super(prefix, key, expiretime);
    }
    //根据GOODS在key的id来获取他的页面缓存
    public static Temp getById(String key){
        return new Temp("COUNT",key,DEFAULT_EXPIRE_TIME);
    }


    public static int getExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}


