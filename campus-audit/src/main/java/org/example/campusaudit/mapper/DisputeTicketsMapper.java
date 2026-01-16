package org.example.campusaudit.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.campusaudit.entity.DisputeTickets;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author 31830
* @description 针对表【dispute_tickets(纠纷裁决工单表)】的数据库操作Mapper
* @createDate 2026-01-06 17:49:39
* @Entity org.example.campusaudit.entity.DisputeTickets
*/
public interface DisputeTicketsMapper extends BaseMapper<DisputeTickets> {
    /**
     * 使用 MySQL JSON_SET 增量更新证据
     * 逻辑：如果 user_id 存在则覆盖其下的内容，不存在则追加新的 user_id 键值对
     */
    @Update("UPDATE dispute_tickets SET evidence_data = " +
            "JSON_SET(IFNULL(evidence_data, '{}'), " +
            "CONCAT('$.\"', #{userId}, '\"'), " +
            "CAST(#{jsonContent} AS JSON)) " +
            "WHERE ticket_id = #{ticketId}")
    int updateEvidenceJson(@Param("ticketId") String ticketId,
                           @Param("userId") String userId,
                           @Param("jsonContent") String jsonContent);
}
