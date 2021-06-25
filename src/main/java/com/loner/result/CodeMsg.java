package com.loner.result;

public class CodeMsg {
    private int code;
    private String msg;

    public CodeMsg(int code,String msg){
        this.code=code;
        this.msg=msg;
    }

    //通用异常5001xx
    public static CodeMsg serverErr=new CodeMsg(500110,"系统出错！");
    public static CodeMsg BIND_Err=new CodeMsg(500111,"参数校验异常:%s");
    public static CodeMsg PATH_ERR=new CodeMsg(500112,"地址错误");
    public static CodeMsg Access_ERR=new CodeMsg(500112,"请求过于频繁，请稍后重试！");
    //登录模块异常5002xx
    public static CodeMsg loginPasswordEmpty= new CodeMsg(500210,"密码不能为空！");
    public static CodeMsg loginPhoneNumEmpty=new CodeMsg(500211,"手机号不能为空！");
    public static CodeMsg loginPhoneNumError=new CodeMsg(500212,"手机号格式错误！");
    public static CodeMsg loginUserNotExit=new CodeMsg(500213,"用户不存在！");
    public static CodeMsg loginPasswordError=new CodeMsg(500214,"密码错误！");
    //商品模块5003xx

    //订单模块5004xx

    //秒杀模块5005xx
    public static CodeMsg miaoshaStockError=new CodeMsg(500510,"秒杀库存不足！");
    public static CodeMsg miaoshaRepeatError=new CodeMsg(500511,"不能重复秒杀！");
    public static CodeMsg miaoshaUserNotLogin=new CodeMsg(500512,"用户未登录！");
    public static CodeMsg miaoshaFail=new CodeMsg(500513,"秒杀失败！");
    //参数填充创建CodeMsg对象
    public CodeMsg fillArgs(Object... args){
        int code=this.code;
        String msg=String.format(this.msg,args);
        return new CodeMsg(code,msg);
    }

    public int getCode() {
        return code;
    }



    public String getMsg() {
        return msg;
    }


}
