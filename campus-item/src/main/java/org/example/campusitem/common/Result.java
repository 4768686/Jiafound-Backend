package org.example.campusitem.common;

import lombok.Data;

/**
 * 全局统一响应结果类
 */
@Data
public class Result<T> {
    private Integer code; // 状态码 200成功
    private String msg;   // 提示信息
    private T data;       // 数据体

    // --- 核心修复：手动添加 Getter (Jackson 序列化必须) ---
    public Integer getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }
    
    // 成功返回
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.msg = "成功";
        result.data = data;
        return result;
    }

    // 失败返回
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.code = 500;
        result.msg = msg;
        return result;
    }

    // 手动补全 Setter（防止Lombok失效导致报错）
    public void setCode(Integer code) { this.code = code; }
    public void setMsg(String msg) { this.msg = msg; }
    public void setData(T data) { this.data = data; }
}