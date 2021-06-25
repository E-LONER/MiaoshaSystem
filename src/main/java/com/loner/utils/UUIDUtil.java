package com.loner.utils;

import java.util.UUID;

/*
 生成session ID，用于唯一标识一个用户session信息
 UUID由以下几部分的组合：
    （1）当前日期和时间，UUID的第一个部分与时间有关，如果你在生成一个UUID之后，过几秒又生成一个UUID，则第一个部分不同，其余相同。
    （2）时钟序列。
    （3）全局唯一的IEEE机器识别号，如果有网卡，从网卡MAC地址获得，没有网卡以其他方式获得。
 */
public class UUIDUtil {
    public static String uuid(){
        //随机生成session ID，用于标识用户；生成得uuid中间带横杠，所以需要把横杠去掉
        return UUID.randomUUID().toString().replace("-","");
    }
}
