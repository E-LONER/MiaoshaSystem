package com.loner.service;

import com.alibaba.fastjson.parser.JSONToken;
import com.loner.domain.MiaoshaOrder;
import com.loner.domain.Order;
import com.loner.redis.RedisService;
import com.loner.redis.keyprefix.OrderKey;
import com.loner.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiaoshaService {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    RedisService redisService;

    //秒杀
    @Transactional//学习事务管理：https://www.jianshu.com/p/5687e2a38fbc
    public Order miaosha(long uId, GoodsVo goods) {
        //先下订单再减库存？
        int res=goodsService.reduceStock(goods);
        if (res==1)
            return orderService.createOrder(uId,goods);
        else{
            orderService.setGoodsOver(uId,goods.getId());
            return null;
        }

    }

    /**
     * 查询订单状况
     * @param userId
     * @param goodsId
     * @return 订单Id；-1是秒杀失败；0是排队中；
     */
    public long getMiaoshaResult(long userId, long goodsId) {
        MiaoshaOrder order=orderService.getOrderByUidGid(userId,goodsId);
        if(order != null){
            return order.getOrderId();
        }else {
            String prefix=String.valueOf(userId)+":"+String.valueOf(goodsId);
            long resCode=redisService.get(OrderKey.getById(prefix).getPrefix(),Long.class);
            if (resCode ==100101)
                return -1;
            else
                return 0;
        }
    }
}
