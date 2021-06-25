package com.loner.redis.keyprefix;

public abstract class KeyUtilsAbstractImpl implements KeyUtils{

    private String prefix;
    private int expiretime;
    private String key;

    public KeyUtilsAbstractImpl(String prefix,String key,int expiretime){
        this.expiretime=expiretime;
        this.prefix=prefix;
        this.key=key;
    }

    public String getPrefix() {
        return prefix+":"+key;
    }

}
