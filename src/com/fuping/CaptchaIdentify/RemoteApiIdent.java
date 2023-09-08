package com.fuping.CaptchaIdentify;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

import static cn.hutool.core.util.StrUtil.isEmptyIfStr;
import static com.fuping.CommonUtils.Utils.*;
import static com.fuping.PrintLog.PrintLog.print_error;
import static com.fuping.PrintLog.PrintLog.print_info;

public class RemoteApiIdent {
    public static String imageToBase64(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            FileInputStream imageInputStream = new FileInputStream(imageFile);
            byte[] imageBytes = new byte[(int) imageFile.length()];
            imageInputStream.read(imageBytes);
            imageInputStream.close();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null; // 处理异常，返回null或其他错误信息
        }
    }

    public static String remoteIdentCommon(String Url, String base64Image,  String ExpectedStatus, String ExpectedKeywords){
        // 创建 HTTP 请求对象
        HttpRequest request = HttpUtil.createPost(Url);
        // 设置请求体，这里将 Base64 图片数据作为请求体
        request.body(base64Image);
        //设置超市时间等参数
        request.timeout(5000);//超时，毫秒

        try{
            // 发送 POST 请求并获取响应
            HttpResponse response = request.execute();
            // 获取响应状态码
            int statusCode = response.getStatus();
            String responseBody = response.body();

            //当前 ExpectedStatus 不为空时, 判断响应状态码是否包含关键字正则
            if (!isEmptyIfStr(ExpectedStatus) && !containsMatchingSubString(String.valueOf(statusCode), ExpectedStatus)) {
                print_error(String.format("异常状态: [%s] <--> [%s]", ExpectedStatus, statusCode));
                return null;
            }
            //当前 ExpectedKeywords 不为空时, 判断响应体是否包含关键字正则
            if (!isEmptyIfStr(ExpectedKeywords) && !containsMatchingSubString(responseBody, ExpectedKeywords)) {
                print_error(String.format("异常内容: [%s] <--> [%s]", ExpectedKeywords, responseBody));
                return null;
            }
            print_info(String.format("Status:[%s] Content: [%s]", statusCode, responseBody));
            return responseBody;
        } catch (Exception exception){
            exception.printStackTrace();
            return null;
        }
    }
    public static String IndentCaptcha(String imagePath, String remoteApi,
                                       String expectedStatus, String expectedKeywords,
                                       String extractRegex, String expectedLength){

        //从绝地路径提取
        imagePath = getFileStrAbsolutePath(imagePath);

        //转base64处理
        String base64Image = imageToBase64(imagePath);
        if (isEmptyIfStr(base64Image)) {
            print_error(String.format("转换失败: 图片[%s] <--> Base64格式失败!!!",  imagePath));
            return null;
        }

        //开始进行识别
        String remoteIdentData = remoteIdentCommon(remoteApi, base64Image, expectedStatus,  expectedKeywords);
        if (isEmptyIfStr(remoteIdentData)) {
            print_error(String.format("接口错误: 接口[%s] <--> 图片[%s]!!!", imagePath, remoteApi));
            return null;
        }

        //提取响应中的验证码
        String captchaResult = regexExtract(remoteIdentData, extractRegex);
        if (isEmptyIfStr(captchaResult)) {
            print_error(String.format("提取错误: 结果[%s] <--> 正则[%s]", remoteIdentData, extractRegex));
            return null;
        }

        //当前 captchaResult 不为空时, 判断验证码长度是否正确
        if (isNumber(expectedLength) && Integer.parseInt(expectedLength) !=  captchaResult.length()) {
            print_error(String.format("识别错误: 结果[%s] <--> 长度[%s] <--> 期望长度:[%s]",captchaResult, captchaResult.length(), expectedLength));
            return null;
        }

        //全部排除过后,验证码格式正确
        return captchaResult;
    }

    public static void main(String[] args) {
        //输入图片地址 图片格式转换
        String imagePath = "tmp/yzm2.jpg";
        String remoteApi = "http://127.0.0.1:5000/base64ocr"; // POST 请求的 URL
        String result = IndentCaptcha(imagePath, remoteApi, "200", null, null, "4");
        print_info(String.format("result:%s", result));
    }
}
