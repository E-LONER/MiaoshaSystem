package com.loner.service;

import com.loner.dao.GoodsDao;
import com.loner.domain.Goods;
import com.loner.domain.MiaoshaGoods;
import com.loner.vo.GoodsVo;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {
    @Autowired
    private GoodsDao goodsDao;

    //获取所有商品（包括普通商品+秒杀商品)
    public List<GoodsVo> getGoodsInfo(){
        return goodsDao.getGoodsInfo();
    }

    //获取商品的秒杀详情页信息（包括时间等）
    public GoodsVo getGoodsDetail(long goodsId) {
        return goodsDao.getGoodsDetailByGoodsId(goodsId);
    }
    //在减库存是否加入判断条件，当库存量大于0时在再减
    public int reduceStock(GoodsVo goodsVo) {
        MiaoshaGoods vo=new MiaoshaGoods();
        vo.setGoodsId(goodsVo.getId());
        return goodsDao.reduce(vo);
    }
}
