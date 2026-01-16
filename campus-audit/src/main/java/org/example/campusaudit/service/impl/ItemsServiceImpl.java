package org.example.campusaudit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.campusaudit.dto.AuditDecisionDTO;
import org.example.campusaudit.dto.AuditItemDTO;
import org.example.campusaudit.dto.AuditPageResult;
import org.example.campusaudit.dto.UserBatchQueryDTO;
import org.example.campusaudit.dto.openai.AIAuditResult;
import org.example.campusaudit.entity.Items;
import org.example.campusaudit.service.ItemsService;
import org.example.campusaudit.mapper.ItemsMapper;
import org.example.campusaudit.utils.AICheckUtils;
import org.example.campusaudit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author 31830
* @description 针对表【items(物品启事表)】的数据库操作Service实现
* @createDate 2026-01-08 10:56:38
*/
@Service
@Slf4j
public class ItemsServiceImpl extends ServiceImpl<ItemsMapper, Items>
    implements ItemsService{
    @Autowired
    private ThreadPoolTaskExecutor auditExecutor; // 注入自定义线程池

    @Autowired
    private ItemsMapper itemsMapper;

    @Autowired
    private AICheckUtils aiCheckUtils;

    @Autowired
    private RestTemplate restTemplate;

    // 注入属性值
    @Value("${services.user.url}")
    private String userServerUrl;

    DateFormatter formatter = new DateFormatter("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result<String> processAudit(String itemId) {
        auditExecutor.execute(() -> {
            log.info("开始异步审核物品: {}", itemId);
            try {
                // 执行具体的审核逻辑（如调用 OpenAI 或内容过滤）
                AIAuditResult result = aiCheckUtils.checkContent(getItemDiscription(itemId));
                boolean isSafe = result.getSuggestion().equals("PASS");

                if (isSafe) {
                    // 审核通过：更新为已发布，后续由 Item 系统轮询入库
                    updateItemStatus(itemId, "PUBLISHED");
                    log.info("物品 {} 审核通过", itemId);
                } else {
                    // 审核不通过：转为人工审核
                    updateItemStatus(itemId, "MANUAL_REVIEW");
                    log.warn("物品 {} 内容违规，已转人工", itemId);
                }
            } catch (Exception e) {
                log.error("物品 {} 异步审核发生异常", itemId, e);
                // 发生异常时，为了保险也转为人工，防止任务“失踪”
                updateItemStatus(itemId, "MANUAL_REVIEW");
            }
        });

        return Result.success("物品 " + itemId + " 已提交审核");
    }

    @Override
    public AuditPageResult getPendingAuditList(int page, int pageSize) {
        // 1. 分页查询本地待复核物品 (MyBatis-Plus)
        Page<Items> pageConfig = new Page<>(page, pageSize);
        LambdaQueryWrapper<Items> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Items::getStatus, "MANUAL_REVIEW")
                .orderByDesc(Items::getPublishTime);

        IPage<Items> itemPage = itemsMapper.selectPage(pageConfig, queryWrapper);

        if (itemPage.getRecords().isEmpty()) {
            return new AuditPageResult(itemPage.getTotal(), new ArrayList<>());
        }

        // 2. 提取并批量查询发布者姓名 (运用封装的函数)
        List<String> publisherIds = itemPage.getRecords().stream()
                .map(Items::getUserId) // 假设数据库字段为 userId
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, String> nameMap = getUserNameMap(publisherIds);

        // 3. 组装 DTO 列表
        List<AuditItemDTO> dtoList = itemPage.getRecords().stream().map(item -> {
            AuditItemDTO dto = new AuditItemDTO();
            dto.setItemID(item.getItemId());
            // 从远程 Map 中匹配名字，找不到则显示“未知用户”
            dto.setPublisherNickname(nameMap.getOrDefault(item.getUserId(), "未知用户"));
            dto.setDescription(item.getDescription());
            dto.setSubmitTime(formatter.print(item.getPublishTime(), Locale.CHINESE));
            return dto;
        }).collect(Collectors.toList());

        return new AuditPageResult(itemPage.getTotal(), dtoList);
    }

    @Override
    public Result<String> manualAudit(AuditDecisionDTO auditDecisionDTO){
        String itemId = auditDecisionDTO.getItemID();
        String decision = auditDecisionDTO.getDecision();

        if (decision.equals("APPROVE")) {
            updateItemStatus(itemId, "PUBLISHED");
            return Result.success("物品 " + itemId + " 已人工审核通过并发布");
        } else if (decision.equals("REJECT")) {
            updateItemStatus(itemId, "REJECTED");
            return Result.success("物品 " + itemId + " 已人工审核拒绝");
        } else {
            return Result.error(400,"无效的审核决策: " + decision);
        }
    }

    @Override
    public int setItemStatus(String itemId, String status){
        Items item = itemsMapper.selectById(itemId);
        if(item == null){
            return 0;
        }
        item.setStatus(status);
        return itemsMapper.updateById(item);
    }

    /**
     * 跨服务批量查询用户 ID 对应的昵称映射
     *
     */
    private Map<String, String> getUserNameMap(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        String userUrl = userServerUrl + "/internal/v1/user/names/batch-query";
        UserBatchQueryDTO queryDTO = new UserBatchQueryDTO();
        queryDTO.setUserIds(userIds);

        // 封装请求
        HttpEntity<UserBatchQueryDTO> requestEntity = new HttpEntity<>(queryDTO);
        ParameterizedTypeReference<Result<Map<String, String>>> responseType =
                new ParameterizedTypeReference<>() {};

        try {
            ResponseEntity<Result<Map<String, String>>> responseEntity = restTemplate.exchange(
                    userUrl, HttpMethod.POST, requestEntity, responseType
            );
            Result<Map<String, String>> userResult = responseEntity.getBody();
            if (userResult != null && userResult.getData() != null) {
                return userResult.getData();
            }
        } catch (Exception e) {
            log.error("远程获取用户名称失败, ids: {}, 错误: {}", userIds, e.getMessage());
        }
        return new HashMap<>(); // 失败返回空 Map，防止后续 NPE
    }

    /**
     * 辅助方法：安全更新状态
     */
    private void updateItemStatus(String itemId, String status) {
        Items updateItem = new Items();
        updateItem.setItemId(itemId);
        updateItem.setStatus(status);
        itemsMapper.updateById(updateItem);
    }

    private String getItemDiscription(String itemId) {
        Items item = itemsMapper.selectById(itemId);
        return item != null ? item.getDescription() : "";
    }
}




