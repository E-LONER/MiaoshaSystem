package com.loner.controller;

import com.loner.myAnnotation.LimitAccess.AccessLimit;
import com.loner.redis.RedisService;
import com.loner.redis.keyprefix.Goods;
import com.loner.redis.keyprefix.User;
import com.loner.result.Result;
import com.loner.service.GoodsService;
import com.loner.service.UserService;
import com.loner.vo.GoodsDetailVo;
import com.loner.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafView;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@EnableAutoConfiguration
@RequestMapping("/goods")
public class GoodsController {
    @Autowired
    private RedisService redisService;

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    /**
     * 秒杀商品列表获取，5000线程数
     * 2个循环数
     * QPS(吞吐率):95.8/sec
     * 平均值：35591
     *
     * 页面缓存+URL缓存优化后：QPS=921.3/SEC,平均值=163
     * @param model
     * @param userCookie
     * @return
     */

    @RequestMapping(value = "/to_glist",produces ="text/html" )
    @ResponseBody
    public String glist(Model model, HttpServletRequest request, HttpServletResponse response,
//                        @CookieValue(value = UserService.COOKIE_NAME,required = false) String cookieUser,
//                        @RequestParam(value = UserService.COOKIE_NAME,required = false) String paramUser
                        String userCookie//此处使用config包下的WebConfig类来处理获得参数
    ){
        //判断用户状态
        if (userCookie ==null)
            return "login";
        //查询页面缓存
        String goodslist_html=redisService.get(Goods.getById("").getPrefix(),String.class);
        if(!StringUtils.isEmpty(goodslist_html)){
            return goodslist_html;
        }
        //从数据库获取所有商品列表
        List<GoodsVo> goodsVoList=goodsService.getGoodsInfo();
        model.addAttribute("goodsList",goodsVoList);
        WebContext context=new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
        goodslist_html=thymeleafViewResolver.getTemplateEngine().process("goods_list", context);
        if(!StringUtils.isEmpty(goodslist_html))
            redisService.set(Goods.getById("").getPrefix(),Goods.getExpire(),goodslist_html);
        return goodslist_html;
    }

    //秒杀商品详情页(旧版本）
    @RequestMapping(value = "/to_detail2/{goodsId}",produces ="text/html")
    @ResponseBody
    public String detail2(Model model,HttpServletRequest request, HttpServletResponse response,String userCookie,@PathVariable("goodsId")long goodsId){
        model.addAttribute("user",userCookie);
        GoodsVo goodsVo =goodsService.getGoodsDetail(goodsId);
        model.addAttribute("goods",goodsVo);

        //判断秒杀状态
        long startTime=goodsVo.getStartDate().getTime();
        long endTime=goodsVo.getEndDate().getTime();
        long nowTime=System.currentTimeMillis();
        int miaoshaState;
        long remainTime;
        //如果startTime<nowTime,秒杀未开始
        if(startTime>nowTime){
            miaoshaState=0;
            remainTime=(startTime-nowTime)/1000;
        }else if (startTime<nowTime && nowTime<endTime){
            //秒杀已经开始
            miaoshaState=1;
            remainTime=0;
        }else {
            //秒杀已经结束
            miaoshaState=-1;
            remainTime=-1;
        }
        model.addAttribute("mstate",miaoshaState);
        model.addAttribute("mremaintime",remainTime);

        //URL缓存
        String html=redisService.get(Goods.getById(String.valueOf(goodsId)).getPrefix(),String.class);
        if(!StringUtils.isEmpty(html))
            return html;
        WebContext context=new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
        html=thymeleafViewResolver.getTemplateEngine().process("goods_detail",context);
        if (!StringUtils.isEmpty(html))
            redisService.set(Goods.getById(String.valueOf(goodsId)).getPrefix(),Goods.getExpire(),html);
        return html;

    }


    //秒杀商品详情页(新版本，前后端分离，页面静态化）
    @RequestMapping(value="/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail(HttpServletRequest request, HttpServletResponse response, String userCookie, @PathVariable("goodsId")long goodsId){
        GoodsVo goodsVo =goodsService.getGoodsDetail(goodsId);
        //判断秒杀状态
        long startTime=goodsVo.getStartDate().getTime();
        long endTime=goodsVo.getEndDate().getTime();
        long nowTime=System.currentTimeMillis();
        int miaoshaState;
        long remainTime;
        //如果startTime<nowTime,秒杀未开始
        if(startTime>nowTime){
            miaoshaState=0;
            remainTime=(startTime-nowTime)/1000;
        }else if (startTime<nowTime && nowTime<endTime){
            //秒杀已经开始
            miaoshaState=1;
            remainTime=0;
        }else {
            //秒杀已经结束
            miaoshaState=-1;
            remainTime=-1;
        }
        GoodsDetailVo goodsDetail=new GoodsDetailVo();
        goodsDetail.setGoodsVo(goodsVo);
        goodsDetail.setMiaoshaState(miaoshaState);
        goodsDetail.setRemainTime(remainTime);
        goodsDetail.setUserId(Long.parseLong(userCookie));
       return Result.success(goodsDetail);

    }
}
