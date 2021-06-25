package com.loner.utils;

import com.alibaba.fastjson.JSON;
import org.thymeleaf.util.StringUtils;

/**
 * 将收到的数据类型转换为特定类型
 */
public class ObjectConverter {

    //输出String值
    public static <T>String toString(T value){
        if (value ==null)
            return null;
        Class<?> classType=value.getClass();
        if (classType ==int.class || classType ==Integer.class){
            return ""+value;
        }else if(classType ==long.class || classType ==Long.class){
            return ""+value;
        }else if (classType ==String.class){
            return (String)value;
        }else {
            return JSON.toJSONString(value);
        }
    }

    //String转其他类型
    public static <T>T StringTo(String str,Class<T> classType){
        if (str == null || str.length() <= 0|| classType==null){
            return null;
        }else if(classType ==int.class || classType ==Integer.class){
            return (T) Integer.valueOf(str);
        }else if (classType ==long.class || classType ==Long.class){
            return (T) Long.valueOf(str);
        }else if (classType == String.class){
            return (T)str;
        }else {
            return (T) JSON.toJavaObject(JSON.parseObject(str),classType);
        }
    }

}
