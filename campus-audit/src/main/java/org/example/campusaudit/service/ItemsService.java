package org.example.campusaudit.service;

import org.example.campusaudit.dto.AuditDecisionDTO;
import org.example.campusaudit.dto.AuditPageResult;
import org.example.campusaudit.entity.Items;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.campusaudit.utils.Result;

/**
* @author 31830
* @description 针对表【items(物品启事表)】的数据库操作Service
* @createDate 2026-01-08 10:56:38
*/
public interface ItemsService extends IService<Items> {
    Result<String> processAudit(String itemId);
    AuditPageResult getPendingAuditList(int page, int pageSize);
    Result<String> manualAudit(AuditDecisionDTO auditDecisionDTO);
    int setItemStatus(String itemId, String status);
}
