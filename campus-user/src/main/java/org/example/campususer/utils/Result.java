package org.example.campususer.utils; // 根据所在模块修改包名

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private Integer code;    // 状态码 (如 200:成功, 500:系统异常)
    private String msg;      // 提示消息
    private T data;          // 数据体

    // 成功返回 - 带数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 成功返回 - 不带数据
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 失败返回
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}