package com.loner.rabbitmq;

import com.loner.domain.MiaoshaOrder;
import com.loner.domain.Order;
import com.loner.redis.RedisService;
import com.loner.service.GoodsService;
import com.loner.service.MiaoshaService;
import com.loner.service.OrderService;
import com.loner.service.UserService;
import com.loner.utils.ObjectConverter;
import com.loner.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQreceiver {

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private MiaoshaService miaoshaService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisService redisService;

    private static Logger log= LoggerFactory.getLogger(MQsender.class);

    @RabbitListener(queues = MQconfig.Miaosha_QUEUE_NAME)
    public void receive(String msgs){
        log.info("MQ收到信息："+msgs);
        MiaoshaMsg msg= ObjectConverter.StringTo(msgs,MiaoshaMsg.class);
        long userId=msg.getUserId();
        long goodsId=msg.getGoodsId();

        //查询库存
        GoodsVo goods=goodsService.getGoodsDetail(goodsId);
        if(goods.getStockCount()<=0){
//                log.info("用户："+userCookie +" 在 ："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"秒杀失败，库存不足！");
                return ;
            }
        //判断是否秒杀成功
        MiaoshaOrder order=orderService.getOrderByUidGid(userId,goodsId);
        if(order !=null){
            return;
        }

        Order orderInfo=miaoshaService.miaosha(userId,goods);
            if (orderInfo != null){
                return;
            }

    }
}
