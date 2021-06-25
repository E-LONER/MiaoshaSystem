package com.loner.utils;

import org.springframework.util.DigestUtils;

public class MD5Util {
    private static final String salt = "1a2b3c4d";
    //md5校验器
    public static String md5(String salt){
        return DigestUtils.md5DigestAsHex(salt.getBytes());
    }

    public static String uPasswordToDBpassword(String password,String salt){
        String str=salt.charAt(0)+salt.charAt(2)+password+salt.charAt(4)+salt.charAt(3)+salt.charAt(2);
        return md5(str);
    }

    public static String inputPassToFormPass(String inputPass) {
        String str = ""+salt.charAt(0)+salt.charAt(2) + inputPass +salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }
    public static String inputPassToDbPass(String inputPass, String saltDB) {
        String formPass = inputPassToFormPass(inputPass);
        String dbPass = uPasswordToDBpassword(formPass, saltDB);
        return dbPass;
    }

//    public static String formPassToDBPass(String formPass, String salt) {
//        String str = ""+salt.charAt(0)+salt.charAt(2) + formPass +salt.charAt(5) + salt.charAt(4);
//        return md5(str);
//    }
}
