package com.smartparking.service;

public class LoginSystem {

    //账户类型
    public enum AccountType{Admin, Staff, Visitor}

    /*
    登录
    选择账户类型、输入账户、密码，对文件验证
     */
    public static boolean login(AccountType  accountType){
        return false;
    }

    /*
    注册
    对管理员开放所有类型账户的注册
    对工作人员、访客开放访客账户注册
     */
    public static boolean register(AccountType accountType){
        return false;
    }

    /*
    生成验证码
    将字母和数字存入数组，用随机数生成获取对应索引值，作为获得验证码的方法
    验证码为4位，同时存在字母和数字
    不区分大小写
     */
    private String getVerificationCode(){
        return null;
    }



}
