package org.example.campusitem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.campusitem.dto.ItemPublishRequest;
import org.example.campusitem.entity.ItemImages;
import org.example.campusitem.entity.Items;
import org.example.campusitem.mapper.ItemsMapper;
import org.example.campusitem.service.ItemImagesService;
import org.example.campusitem.service.ItemsService;
import org.example.campusitem.utils.EmbeddingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
/**
* @author 31830
* @description 针对表【items(物品表)】的数据库操作Service实现
* @createDate 2026-01-06 17:51:52
*/
@Service
public class ItemsServiceImpl extends ServiceImpl<ItemsMapper, Items> implements ItemsService {

    @Autowired
    private ItemImagesService itemImagesService;

    @Autowired
    private EmbeddingUtils embeddingUtils;

    /**
     * 物品匹配或增加 Redis 向量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object saveItemWithImages(ItemPublishRequest request) {
        // 1. 生成唯一 ID
        String itemId = UUID.randomUUID().toString().replace("-", "");

        // 2. 构造并保存数据库实体
        Items item = new Items();
        item.setItemId(itemId);
        item.setUserId(request.getUserId());
        item.setItemType(request.getItemType());
        item.setDescription(request.getDescription());
        item.setLocationText(request.getLocationText());
        item.setLatitude(request.getLatitude());
        item.setLongitude(request.getLongitude());
        item.setRewardPoints(request.getRewardPoints());
        item.setStatus("Published");
        this.save(item);

        // 3. 保存图片关联
        if (request.getImageUrl() != null) {
            ItemImages image = new ItemImages();
            image.setItemId(itemId);
            image.setImageUrl(request.getImageUrl());
            itemImagesService.save(image);
        }

        // 4. AI 向量化处理
        List<Double> currentEmbedding = embeddingUtils.getEmbedding(request.getDescription());
        
        // 5. 相似度匹配 (KNN 搜索)
        // 如果是寻找失物(LOST)，就去匹配招领(FOUND)；反之亦然
        // 注意：searchSimilar 内部需要根据业务需求过滤 itemType，目前简单实现全局匹配
        List<String> matchedIds = embeddingUtils.searchSimilar(request.getDescription(), 3);

        // 6. 将当前物品存入 Redis 向量库，供以后他人匹配
        if (currentEmbedding != null) {
            embeddingUtils.saveVector(itemId, request.getDescription(), currentEmbedding);
        }

        // 7. 处理匹配结果逻辑
        if (!matchedIds.isEmpty()) {
            // 这里返回匹配到的第一个物品（最相似的）作为结果给用户
            // 实际业务中可以根据 score 阈值判断是否真的是同一个物品
            String matchedId = matchedIds.get(0);
            Items matchedItem = this.getById(matchedId);

            if (matchedItem != null) {
                // 1. 查询该物品对应的图片
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItemImages> imgWrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                imgWrapper.eq(ItemImages::getItemId, matchedId)
                        .last("LIMIT 1"); // 只取第一张图
                
                ItemImages imgEntity = itemImagesService.getOne(imgWrapper);

                // 2. 使用 Map 封装返回数据，不修改 Items 实体
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("itemId", matchedItem.getItemId());
                result.put("description", matchedItem.getDescription());
                result.put("publishTime", matchedItem.getPublishTime());
                
                // 重点：把图片地址塞进去
                if (imgEntity != null) {
                    result.put("imageUrl", imgEntity.getImageUrl());
                } else {
                    result.put("imageUrl", ""); // 或者默认图
                }

                return result; // 返回给 Controller 的是 Map 对象
            }
        }

        return null; // 无匹配结果
    }
}