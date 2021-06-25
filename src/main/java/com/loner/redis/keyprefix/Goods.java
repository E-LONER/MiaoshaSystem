package com.loner.redis.keyprefix;

public class Goods extends KeyUtilsAbstractImpl {

    //默认redis键的有效期,两天
    private static final int DEFAULT_EXPIRE_TIME=10;

    private Goods(String prefix, String key, int expiretime) {
        super(prefix, key, expiretime);
    }

    //根据GOODS在key的id来获取他的页面缓存
    public static Goods getById(String key){
        return new Goods("Goods",key,DEFAULT_EXPIRE_TIME);
    }

    //获取库存加载
    public static Goods getGoodsStock(String key){
        return new Goods("GOODSSTOCK",key,60*60);
    }


    public static int getExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}
