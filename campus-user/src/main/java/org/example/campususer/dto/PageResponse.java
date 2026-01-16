package org.example.campususer.dto;

import lombok.Data;
import java.util.List;

/**
 * 分页响应 DTO
 * @param <T> 数据类型
 */
@Data
public class PageResponse<T> {

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 构造分页响应
     */
    public static <T> PageResponse<T> of(List<T> list, Long total, Integer page, Integer pageSize) {
        PageResponse<T> response = new PageResponse<>();
        response.setList(list);
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        
        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / pageSize);
        response.setTotalPages(totalPages);
        
        // 判断是否有下一页/上一页
        response.setHasNext(page < totalPages);
        response.setHasPrevious(page > 1);
        
        return response;
    }
}
