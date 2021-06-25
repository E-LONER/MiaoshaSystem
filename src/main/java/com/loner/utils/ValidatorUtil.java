package com.loner.utils;

import org.thymeleaf.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtil {
    /**
     * Pattern与Matcher一起合作.Matcher类提供了对正则表达式的分组支持,以及对正则表达式的多次匹配支持.
     * 单独用Pattern只能使用Pattern.matches(String regex,CharSequence input)一种最基础最简单的匹配。
     */
    private static final Pattern phoneNumPattern= Pattern.compile("1\\d{10}");

    /**
     * 一种简单的手机号验证正则表达式
     */
    public static boolean isPhoneNum(String pn){
        if(StringUtils.isEmpty(pn)){
        return false;
     }
        Matcher matcher=phoneNumPattern.matcher(pn);
        return matcher.matches();
    }
}
