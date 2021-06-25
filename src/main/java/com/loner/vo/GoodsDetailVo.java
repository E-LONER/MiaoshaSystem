package com.loner.vo;

public class GoodsDetailVo {
    private int miaoshaState;
    private long remainTime;
    private GoodsVo goodsVo;
    private long userId;

    public int getMiaoshaState() {
        return miaoshaState;
    }

    public void setMiaoshaState(int miaoshaState) {
        this.miaoshaState = miaoshaState;
    }

    public long getRemainTime() {
        return remainTime;
    }

    public void setRemainTime(long remainTime) {
        this.remainTime = remainTime;
    }

    public GoodsVo getGoodsVo() {
        return goodsVo;
    }

    public void setGoodsVo(GoodsVo goodsVo) {
        this.goodsVo = goodsVo;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
