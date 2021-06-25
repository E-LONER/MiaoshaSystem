package com.loner.controller;


import com.loner.myAnnotation.LimitAccess.AccessLimit;
import com.loner.rabbitmq.MQsender;
import com.loner.rabbitmq.MiaoshaMsg;
import com.loner.redis.RedisService;
import com.loner.redis.keyprefix.Goods;
import com.loner.redis.keyprefix.Path;
import com.loner.result.CodeMsg;
import com.loner.result.Result;
import com.loner.service.GoodsService;
import com.loner.service.MiaoshaService;
import com.loner.service.OrderService;
import com.loner.service.UserService;
import com.loner.utils.MD5Util;
import com.loner.utils.UUIDUtil;
import com.loner.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
    @EnableAutoConfiguration
    @RequestMapping("/miaosha")
    public class MiaoshaController implements InitializingBean {
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

        @Autowired
        private MQsender mQsender;

        //打印日志
        private static Logger log= LoggerFactory.getLogger(LoginController.class);


        /**
         * 在系统初始化时就加载库存到redis内存
         * 方法：使用spring-boon 的InitializingBean，让miaosha类实现他的afterPropertiesSet方法
         */

        // 内存标记法优化，即减少redis访问
       private Map<Long,Boolean> stockMark=new HashMap<>();

        @Override
        public void afterPropertiesSet() throws Exception {
            List<GoodsVo> goodsList=goodsService.getGoodsInfo();
            for(GoodsVo goods:goodsList){
                redisService.set(Goods.getGoodsStock(String.valueOf(goods.getId())).getPrefix(),60*3,goods.getStockCount());
                stockMark.put(goods.getId(),true);
            }
        }
        /**
         * ******************************优化前******************************************
         * QPS:87.8/SEC
         * 平均值：73293
         * 数据库：stock_count:-132
         * linux top检测：load average：0.13左右
         *
         * 第六章优化后 QPS=114.7 sec
         * 平均值：26236
         * 样品：5000*2
         * linux top检测：load average：3.6
         * 超卖问题已经解决
         *
         * 第七章优化
         * QPS:1140.8/SEC
         *平均值：2289
         */

        /**
         * 卖超问题：
         *1. 通过在修改数据库时添加查询，只有在库存>0时才会修改数据库数据。
         *2. 通过建立synchronized进行加锁，但是否会降低并发速度
         */

        /**
         * 重复下单问题：一个账户同一个产品秒杀两次或者一个用户秒杀多个商品
         * 1. 数据库层面：对于秒杀数据库订单表建立唯一索引，针对用户id和物品id
         */

        /**
         * 可以优化的点：
         * 1. 查询库存可以放置在Redis中进行，在秒杀开始前从数据库中取出库存，然后每次查询和减库存可以在Redis中进行，等秒杀结束后刷新到数据库中
         * 2. 将订单查询判断是否重复秒杀可否放在Redis中查询，减少访问数据库，同时因为秒杀订单少，又不会消耗多少内存空间
         * 3.
         */

        /**
         *系统初始化，将库存加载到Redis缓存中
         * 订单来了，redis预减库存，库存不足，直接返回（redis是单线程），否则进入下一步
         * 请求入队，立即返回排队中（减少未知等待）
         * 请求出队，生成订单，减少库存
         * 客户端轮询，查询是否请求成功
         */

    /**
     *URL地址隐藏
     * 思路：
     *      1. 秒杀开始之前请求接口获取URL地址
     *      2. 接口改造，带上Pathvariable参数
     *      3. 添加生成地址的接口
     *      4. 后台收到请求，验证pathvariable参数
     */

    /**
     *添加图形验证码（数学公式等）
     * 作用：分散用户请求时间；防机器人
     * 思路：
     *      1. 添加图形验证码的接口
     *      2. 在获取秒杀地址验证的时候进行图形验证码
     *      3. ScriptEngine
     */
        @AccessLimit(time = 10,maxAccess = 5,login = true)
        @RequestMapping(value ="/{path}/do_miaosha",method=RequestMethod.POST)
        @ResponseBody
        public Result<Integer> do_miaosha(String userCookie,
                                          @RequestParam("goodsId")long goodsId,
                                          @PathVariable("path")String path) {
            long uId = Long.parseLong(userCookie);
            //判断用户状态
            if (userCookie == null) {
                return Result.error(CodeMsg.miaoshaUserNotLogin);
            }

            //验证path
            Boolean res=cheackPath(path,uId,goodsId);
            if (!res)
                return Result.error(CodeMsg.PATH_ERR);
            //打印日志信息
            log.info("用户：" + userCookie + " 在 ：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "开始秒杀");
            //查询是否有库存,实现1.0
//            GoodsVo goods=goodsService.getGoodsDetail(goodsId);
//            if(goods.getStockCount()<=0){
//                log.info("用户："+userCookie +" 在 ："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"秒杀失败，库存不足！");
//                return Result.error(CodeMsg.miaoshaStockError);
//            }

            //商品库存标记减少redis访问优化,如果为false则说明库存不足，直接返回库存不足错误消息
            if (stockMark.get(goodsId))
                return Result.error(CodeMsg.miaoshaStockError);

            //实现2.0，直接查询redis缓存库存是否足够
            long stock = redisService.decr(Goods.getGoodsStock(Long.toString(goodsId)).getPrefix());
            if (stock < 0) {
                log.info("用户：" + userCookie + " 在 ：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "秒杀失败，库存不足！");
                stockMark.put(goodsId,false);
                return Result.error(CodeMsg.miaoshaStockError);
            }

            //将请求加入消息队列中
            MiaoshaMsg msg = new MiaoshaMsg();
            msg.setGoodsId(goodsId);
            msg.setUserId(uId);
            mQsender.sendMiaoshaMsg(msg);
            return Result.success(0);
//
//            //判断是否秒杀成功
//            MiaoshaOrder order=orderService.getOrderByUidGid(uId,goodsId);
//            if(order !=null){
//                //打印日志信息
//                log.info("用户："+userCookie +" 在 ："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"秒杀失败，重复下单！");
//                return Result.error(CodeMsg.miaoshaRepeatError);
//            }
//
//
//            //开始秒杀，减库存加入用户订单，事务
//            Order orderInfo=miaoshaService.miaosha(uId,goods);
//            if (orderInfo != null){
//                //打印日志信息
//                log.info("用户："+userCookie +" 在 ："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"秒杀成功");
//                return Result.success(orderInfo);
//
//            }
//            //打印日志信息
//            log.info("用户："+userCookie +" 在 ："+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"秒杀失败");
//            return Result.error(CodeMsg.miaoshaFail);
//
//        }
//
        }

        /**
         * 客户端轮询订单结果
         * @param userCookie
         * @param goodsId
         * @return Result<long>
         */
        @RequestMapping(value ="/result",method=RequestMethod.GET)
        @ResponseBody
        public Result<Long> getResult(String userCookie,@RequestParam("goodsId")long goodsId) {
            if (userCookie ==null)
                return Result.error(CodeMsg.loginUserNotExit);
            long userId=Long.parseLong(userCookie);

            //查询订单结果
            long res=miaoshaService.getMiaoshaResult(userId,goodsId);
            if (res == -1 || res ==0 ) {

                return Result.success(res);
            }
            else{
                System.out.println(res);
                return Result.error(CodeMsg.miaoshaRepeatError);
            }


        }

    @RequestMapping(value ="/path",method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getPath(String userCookie,@RequestParam("goodsId")long goodsId) {
        if (userCookie ==null)
            return Result.error(CodeMsg.loginUserNotExit);
        long userId=Long.parseLong(userCookie);

        String ran= UUIDUtil.uuid();
        String str= MD5Util.md5(ran+"loner");
        redisService.set(Path.getById(""+userId+":"+goodsId).getPrefix(),Path.getExpire(),str);
        return Result.success(str);
    }

    public Boolean cheackPath(String path,long uId,long gId) {
        String str=redisService.get(Path.getById(""+uId+":"+gId).getPrefix(),String.class);
        if(str.equals(path))
            return true;
        else
            return false;
    }

}



