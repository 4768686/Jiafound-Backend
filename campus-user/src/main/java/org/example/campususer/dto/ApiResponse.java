package org.example.campususer.dto;

import lombok.Data;

/**
 * 统一 API 响应 DTO
 * @param <T> 数据类型
 */
@Data
public class ApiResponse<T> {

    /**
     * 响应码
     * 200: 成功
     * 400: 客户端错误
     * 401: 未授权
     * 403: 无权限
     * 500: 服务器错误
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 私有构造函数
     */
    private ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("操作成功");
        response.setData(data);
        return response;
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /**
     * 成功响应（仅消息）
     */
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        return response;
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 失败响应（默认400）
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(400, message);
    }

    /**
     * 未授权响应（401）
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(401, message);
    }

    /**
     * 无权限响应（403）
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return error(403, message);
    }

    /**
     * 服务器错误响应（500）
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return error(500, message);
    }
}
