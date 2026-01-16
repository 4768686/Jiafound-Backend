package org.example.campusitem.service;

import org.example.campusitem.entity.Items;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.campusitem.dto.ItemPublishRequest;

public interface ItemsService extends IService<Items> {
    /**
     * 发布物品并保存
     * @param request 前端传来的发布请求
     * @return 保存后的物品ID
     */
    Object saveItemWithImages(ItemPublishRequest request);
}