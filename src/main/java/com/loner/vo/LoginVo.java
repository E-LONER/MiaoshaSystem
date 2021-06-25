package com.loner.vo;

import com.alibaba.druid.support.json.JSONUtils;
import com.loner.validator.IsPhoneNum.IsPhoneNum;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/*
用于检验用户登录时，手机号和密码是否符合规范，
 */
public class LoginVo {

    @NotNull
    @Length(min=5)
    private String password;
    @NotNull
    @IsPhoneNum
    private String phoneNum;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    @Override
    public String toString() {
        return "LoginVo{" +
                "password='" + password + '\'' +
                ", phoneNum='" + phoneNum + '\'' +
                '}';
    }
}
