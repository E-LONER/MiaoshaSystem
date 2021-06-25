package com.loner.service;

import com.loner.dao.OrderDao;
import com.loner.domain.MiaoshaOrder;
import com.loner.domain.Order;
import com.loner.redis.RedisService;
import com.loner.redis.keyprefix.OrderKey;
import com.loner.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.Date;

@Service
public class OrderService {
    @Autowired
    private OrderDao orderDao;

    @Autowired
    private RedisService redis;

    public MiaoshaOrder getOrderByUidGid(long uId, long gId){
        MiaoshaOrder order=redis.get(OrderKey.getById(String.valueOf(uId)+":"+String.valueOf(gId)).getPrefix(),MiaoshaOrder.class);
        return order;
    }

    public Order getOrderByOrderId(long orderId){
        return orderDao.getOrderByOrderId(orderId);
    }

    @Transactional
    public Order createOrder(long uId, GoodsVo goods) {
        Order order=new Order();
        order.setCreateDate(new Date());
        order.setDeliveryAddrId(0L);
        order.setGoodsCount(1);
        order.setGoodsId(goods.getId());
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsPrice(goods.getMiaoshaPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setUserId(uId);
        order.setGoods(goods);

        long res=orderDao.insertOrder(order);

        MiaoshaOrder miaoshaOrder=new MiaoshaOrder();
        miaoshaOrder.setGoodsId(goods.getId());
        miaoshaOrder.setUserId(uId);
        miaoshaOrder.setOrderId(order.getId());

        orderDao.insertMiaoshaOrder(miaoshaOrder);
        //加入Redis缓存，利用ORDER:uID:gID组合
        String prefix=String.valueOf(uId)+":"+String.valueOf(goods.getId());
        redis.set(OrderKey.getById(prefix).getPrefix(),OrderKey.getExpire(),miaoshaOrder);
        return order;
    }

    /**
     *  标记秒杀失败结果，缓存存放10min
     *  结果：100101
     */
    public void setGoodsOver(Long uId,Long gId) {
        String prefix=String.valueOf(uId)+":"+String.valueOf(gId);
        redis.set(OrderKey.getById(prefix).getPrefix(),600,100101);
    }
}
