package com.loner.controller;

import com.loner.domain.Order;
import com.loner.result.CodeMsg;
import com.loner.result.Result;
import com.loner.service.GoodsService;
import com.loner.service.MiaoshaService;
import com.loner.service.OrderService;
import com.loner.service.UserService;
import com.loner.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private MiaoshaService miaoshaService;

    @Autowired
    private OrderService orderService;

    //打印日志
    private static Logger log= LoggerFactory.getLogger(LoginController.class);

    @RequestMapping("/getOrderInfo")
    @ResponseBody
    public Result<Order> getOrderInfo(String userCookie,@RequestParam("orderId")long orderId){
        long uId=Long.parseLong(userCookie);
        //判断用户状态
        if (userCookie ==null)
            return Result.error(CodeMsg.miaoshaUserNotLogin);

        /**
         * 个人认为可以不用查询是否已经下过订单了，应该后面已经建立唯一索引保证
         */
        Order order=orderService.getOrderByOrderId(orderId);
        GoodsVo goods=goodsService.getGoodsDetail(order.getGoodsId());
        //建立唯一索引保证用户只能秒杀一个商品
        //create unique index uid_g_id on order_info (user_id,goods_id);
        order.setGoods(goods);
        return Result.success(order);
    }
}
