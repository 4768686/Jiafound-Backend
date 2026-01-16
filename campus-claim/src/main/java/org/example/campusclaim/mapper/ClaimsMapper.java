package org.example.campusclaim.mapper;

import org.example.campusclaim.entity.Claims;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ClaimsMapper extends BaseMapper<Claims> {

    @Update("UPDATE items SET status = #{status} WHERE item_id = #{itemId}")
    void updateItemStatus(String itemId, String status);

    @Select("SELECT image_url FROM item_images WHERE item_id = #{itemId} ORDER BY sort_order ASC")
    List<String> selectItemImages(String itemId);

    @Select("SELECT i.item_id as itemID, " +
            "i.user_id as publisherID, " +
            "i.item_type as itemType, " +
            "i.reward_points as rewardPoints, " +
            "i.description as title, " +        // 用于卡片标题
            "i.description as description, " + 
            "i.location_text as location, " +   // 统一别名为 location
            "i.status, " +
            "i.publish_time as publishTime, " + // 统一别名为 publishTime
            "u.nickname as publisherNickname, " +
            "u.avatar_url as publisherAvatar " +
            "FROM items i " +
            "LEFT JOIN users u ON i.user_id = u.user_id " +
            "WHERE i.item_id = #{itemId}")
    Map<String, Object> selectItemDetail(String itemId);

    @Select("SELECT item_id as itemID, " +
            "description as title, " +
            "location_text as foundPlace, " +
            "publish_time as publishTime, " +
            "item_type as itemType " +
            "FROM items " +
            "WHERE status = 'Published' " + 
            "ORDER BY publish_time DESC")
    List<Map<String, Object>> selectHallList();

    @Select("SELECT c.claim_id, c.status, c.create_time, " +
            "i.item_id, i.description as title, i.image_url as thumb " +
            "FROM claims c " +
            "LEFT JOIN items i ON c.item_id = i.item_id " +
            "WHERE c.applicant_id = #{userId} " +
            "ORDER BY c.create_time DESC")
    List<Map<String, Object>> selectMyAppliedList(String userId);

    @Select("SELECT i.item_id, i.description as title, i.status as itemStatus, " +
            "count(c.claim_id) as applyCount " +
            "FROM items i " +
            "LEFT JOIN claims c ON i.item_id = c.item_id " +
            "WHERE i.user_id = #{userId} " +
            "GROUP BY i.item_id " +
            "ORDER BY i.publish_time DESC")
    List<Map<String, Object>> selectMyPublishedList(String userId);
}