package com.loner.redis.keyprefix;

import com.loner.myAnnotation.LimitAccess.AccessLimit;

public class AccessKey extends KeyUtilsAbstractImpl {

    private static final int DEFAULT_EXPIRE_TIME=10;
    public AccessKey(String prefix, String key, int expiretime) {
        super(prefix, key, expiretime);
    }

    public static AccessKey getById(String key){
        return new AccessKey("Access",key,DEFAULT_EXPIRE_TIME);
    }
    public static int getExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}
