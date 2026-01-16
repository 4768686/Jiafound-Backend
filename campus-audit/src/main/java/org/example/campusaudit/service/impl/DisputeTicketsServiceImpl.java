package org.example.campusaudit.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.lang.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.campusaudit.dto.*;
import org.example.campusaudit.entity.DisputeTickets;
import org.example.campusaudit.entity.ViewDisputeDetail;
import org.example.campusaudit.mapper.ViewDisputeDetailMapper;
import org.example.campusaudit.sercurity.UserContext;
import org.example.campusaudit.service.DisputeTicketsService;
import org.example.campusaudit.mapper.DisputeTicketsMapper;
import org.example.campusaudit.service.ItemsService;
import org.example.campusaudit.utils.AzureBlobUtils;
import org.example.campusaudit.utils.PageResult;
import org.example.campusaudit.utils.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
* @author 31830
* @description 针对表【dispute_tickets(纠纷裁决工单表)】的数据库操作Service实现
* @createDate 2026-01-06 17:49:39
*/
@Service
@Slf4j
public class DisputeTicketsServiceImpl extends ServiceImpl<DisputeTicketsMapper, DisputeTickets>
    implements DisputeTicketsService{

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ItemsService itemsService;

    // 注入属性值
    @Value("${services.user.url}")
    private String userServerUrl;

    @Value("${services.item.url}")
    private String itemServerUrl;

    @Value("${services.claim.url}")
    private String claimServerUrl;

    @Autowired
    private AzureBlobUtils azureBlobUtils;

    @Autowired
    private DisputeTicketsMapper disputesMapper;

    @Autowired
    private ViewDisputeDetailMapper viewDisputeDetailMapper;

    @Override
    public PageResult<DisputeTicketDTO> getAdminDisputePool(String status, int pageNum) {
        // 1. 分页查询本地纠纷表 (MyBatis-Plus)
        Page<DisputeTickets> page = new Page<>(pageNum, 10);
        LambdaQueryWrapper<DisputeTickets> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(DisputeTickets::getStatus, status);
        }
        this.page(page, wrapper);

        // 如果没数据，直接返回空的分页对象
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }

        // 2. 批量获取用户名称 (跨服务 HTTP 调用)
        List<String> userIds = page.getRecords().stream()
                .map(DisputeTickets::getInitiatorId).distinct().toList();

        String userUrl =  userServerUrl + "/internal/v1/user/names/batch-query";

        UserBatchQueryDTO queryDTO = new UserBatchQueryDTO();
        queryDTO.setUserIds(userIds);

        // 使用 HttpEntity 包装 DTO
        HttpEntity<UserBatchQueryDTO> requestEntity = new HttpEntity<>(queryDTO);

        // 定义复杂的泛型类型引用：Result<Map<String, String>>
        ParameterizedTypeReference<Result<Map<String, String>>> responseType =
                new ParameterizedTypeReference<>() {};

        Map<String, String> nameMap = new HashMap<>();
        try {
            // 发起远程调用
            ResponseEntity<Result<Map<String, String>>> responseEntity = restTemplate.exchange(
                    userUrl, HttpMethod.POST, requestEntity, responseType
            );
            Result<Map<String, String>> userResult = responseEntity.getBody();
            if (userResult != null && userResult.getData() != null) {
                nameMap = userResult.getData();
            }
        } catch (Exception e) {
            // 打印错误日志，但不让整个接口崩溃
            log.error("远程调用用户服务失败:", e);
        }

        // 组装 VO 列表
        Map<String, String> finalNameMap = nameMap;
        List<DisputeTicketDTO> voList = page.getRecords().stream().map(entity -> {
            DisputeTicketDTO vo = new DisputeTicketDTO();
            BeanUtils.copyProperties(entity, vo);
            vo.setInitiatorName(finalNameMap.getOrDefault(entity.getInitiatorId(), "未知用户"));
            vo.setIsOvertime(entity.getCreateTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .isBefore(LocalDateTime.now().minusDays(1)));
            return vo;
        }).toList();

        // 返回分页数据给 Controller，由 Controller 包装 Result
        return new PageResult<>(voList, page.getTotal());
    }

    @Override
    public PageResult<MyDisputeListDTO> getMyDisputes(int pageNum, int pageSize) {
        String currentUserId = UserContext.getUserId();

        // 1. 在物理表 DisputeTickets 上进行分页查询，因为它有 create_time 和 initiator_id
        Page<DisputeTickets> entityPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DisputeTickets> entityWrapper = new LambdaQueryWrapper<>();

        // 注意：这里需要查询 (我是发起人) OR (我是被投诉人)
        // 被投诉人逻辑在物理表不好写，所以我们依然需要先从视图拿到符合条件的 ticket_id 集合
        // 或者直接在 SQL 里写 Join。为了保护 2GB 内存，我们采用以下联合查询逻辑：

        List<ViewDisputeDetail> involvedDisputes = viewDisputeDetailMapper.selectList(
                new LambdaQueryWrapper<ViewDisputeDetail>()
                        .eq(ViewDisputeDetail::getInitiatorId, currentUserId)
                        .or()
                        .eq(ViewDisputeDetail::getRespondentId, currentUserId)
                        .select(ViewDisputeDetail::getTicketId) // 只查 ID 节省内存
        );

        if (involvedDisputes.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }

        List<String> ticketIds = involvedDisputes.stream().map(ViewDisputeDetail::getTicketId).toList();

        // 2. 回到物理表根据 ticketIds 分页并排序
        entityWrapper.in(DisputeTickets::getTicketId, ticketIds)
                .orderByDesc(DisputeTickets::getCreateTime);
        disputesMapper.selectPage(entityPage, entityWrapper);

        // 3. 转换为 DTO 并填充视图中的身份信息
        List<MyDisputeListDTO> dtoList = entityPage.getRecords().stream().map(entity -> {
            MyDisputeListDTO dto = new MyDisputeListDTO();
            BeanUtils.copyProperties(entity, dto);

            // 从视图中找到对应的身份细节（这里建议把视图数据转为 Map 提高查找效率）
            // 判定关系：如果物理表的 initiatorId == 当前用户，则是我发起的
            if (currentUserId.equals(entity.getInitiatorId())) {
                dto.setRelationType("Initiated by Me");
            } else {
                dto.setRelationType("Involving Me");
            }

            // 截断理由
            String reason = entity.getReason();
            dto.setReasonSummary(StringUtils.hasText(reason) && reason.length() > 20
                    ? reason.substring(0, 20) + "..." : reason);

            return dto;
        }).toList();

        return new PageResult<>(dtoList, entityPage.getTotal());
    }

    @Override
    public DisputeDetailDTO getDisputeDetail(String ticketId) {
        // 1. 获取物理表数据
        DisputeTickets ticket = this.getById(ticketId);
        if (ticket == null) return null;

        // 2. 获取视图数据（主要为了拿到 respondent_id）
        ViewDisputeDetail view = viewDisputeDetailMapper.selectOne(
                new LambdaQueryWrapper<ViewDisputeDetail>().eq(ViewDisputeDetail::getTicketId, ticketId)
        );

        DisputeDetailDTO detail = new DisputeDetailDTO();
        BeanUtils.copyProperties(ticket, detail);

        if (view != null) {
            detail.setRespondentId(view.getRespondentId());
            detail.setInitiatorRole(view.getInitiatorRole());
        }

        // 3. 解析证据 JSON [重点优化：减少 2GB 内存消耗]
        if (ticket.getEvidenceData() != null) {
            try {
                // 1. 确保将 Object 转换为 String
                String jsonStr = JSONUtil.toJsonStr(ticket.getEvidenceData());

                // 2. 使用正确的 TypeReference 解析方法
                Map<String, DisputeDetailDTO.EvidenceDetail> evidenceMap = JSONUtil.toBean(jsonStr,
                        new TypeReference<>() {
                        },
                        true
                );

                // 3. 根据 ID 分类
                if (evidenceMap != null) {
                    detail.setInitiatorEvidence(evidenceMap.get(ticket.getInitiatorId()));
                    if (view != null) {
                        detail.setRespondentEvidence(evidenceMap.get(view.getRespondentId()));
                    }
                }
            } catch (Exception e) {
                log.error("Evidence JSON parsing failed: {}", e.getMessage());
            }
        }

        return detail;
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> adminRuling(RulingRequestDTO dto) {
        // 1. 获取工单并更新状态（逻辑保持不变）
        DisputeTickets ticket = this.getById(dto.getTicketId());
        if (ticket == null || !"Reviewing".equals(ticket.getStatus())) {
            return Result.error(400, "工单状态错误或不存在");
        }

        // 2. 获取视图详情，用于准确定位被投诉人
        ViewDisputeDetail view = viewDisputeDetailMapper.selectOne(
                new LambdaQueryWrapper<ViewDisputeDetail>().eq(ViewDisputeDetail::getTicketId, dto.getTicketId())
        );

        // 3. 执行物理表更新
        ticket.setStatus("Closed");
        ticket.setRulingResult(dto.getRulingResult());
        ticket.setUpdateTime(new Date());
        this.updateById(ticket);

        // 4. 执行信用分扣除逻辑
        handleCreditDeduction(ticket, view, dto.getDeductCredit());

        Map<String, Object> data = new HashMap<>();
        data.put("status", "Closed");
        return Result.success(data);
    }

    private void handleCreditDeduction(DisputeTickets ticket, ViewDisputeDetail view, int amount) {
        // 判定谁是败诉方
        String loserId;
        if ("InitiatorWin".equalsIgnoreCase(ticket.getRulingResult())) {
            // 发起人赢，扣被投诉人（Respondent）的分
            loserId = view.getRespondentId();
        } else {
            // 被投诉人赢（或者发起人无理取闹），扣发起人（Initiator）的分
            loserId = view.getInitiatorId();
        }

        // 构造远程调用请求
        DeductCoinPunishRequest request = DeductCoinPunishRequest.builder()
                .userId(loserId)
                .amount(amount)
                .reason("Dispute Ruling Punishment: " + ticket.getTicketId())
                .relatedItemId(view.getItemId())
                .build();

        // 发起远程调用 (端口 8085)
        String url = "http://localhost:8085/internal/v1/user/coin/deduct-punish";
        try {
            log.info("发起信用分扣除远程调用: {}", request);
            restTemplate.postForEntity(url, request, Result.class);
        } catch (Exception e) {
            // 在 2GB 服务器上，如果跨服务调用超时，记录日志但不中断事务
            log.error("信用分扣除远程调用失败, ticketId: {}, error: {}", ticket.getTicketId(), e.getMessage());
        }
    }

    /**
     * 批量上传图片并返回 URL 列表
     *
     */
    @Override
    public List<String> uploadImages(MultipartFile[] files) {
        List<String> imageUrls = new ArrayList<>();

        if (files == null || files.length == 0) {
            return imageUrls;
        }

        // 限制上传数量，防止 2GB 服务器因 I/O 过载崩溃
        if (files.length > 9) {
            throw new RuntimeException("单次上传图片不能超过 9 张");
        }

        for (MultipartFile file : files) {
            // 基础校验：空文件跳过
            if (file.isEmpty()) continue;

            try {
                // 调用你提供的 uploadFile 函数
                String url = azureBlobUtils.uploadFile(file);
                imageUrls.add(url);
                log.info("图片上传成功: {}", url);
            } catch (IOException e) {
                log.error("图片上传失败: {}, 文件名: {}", e.getMessage(), file.getOriginalFilename());
                // 根据业务决定：是直接抛出异常还是记录后继续上传下一张
                // 建议：如果一张失败，可能网络有问题，直接中断
                throw new RuntimeException("文件服务响应异常，请重试");
            }
        }

        return imageUrls;
    }

    @Override
    @Transactional
    public Result<String> submitEvidence(String ticketId, EvidenceRequestDTO requestDTO) {
        String userId = UserContext.getUserId();
        // 1. 只构建内容部分，不要再包一层 userId
        Map<String, Object> content = new HashMap<>();
        content.put("image_urls", requestDTO.getImageUrls());
        content.put("comments", requestDTO.getComments());

        // 2. 转换为 JSON 字符串
        String jsonContent = JSONUtil.toJsonStr(content);

        // 3. 执行更新
        // SQL 的 JSON_SET 会负责把这个 content 挂在 userId 这个键下面
        int rows = disputesMapper.updateEvidenceJson(ticketId, userId, jsonContent);

        return rows > 0 ? Result.success("提交成功") : Result.error(400, "单据不存在");
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> applyDispute(DisputeApplyDTO dto) {
        String userId = UserContext.getUserId();

        // 1. 生成工单 ID 和 截止时间
        String ticketId = IdUtil.simpleUUID();
        DateTime deadline = DateUtil.offsetHour(new Date(), 48); // 举例：48小时后

        // 2. 构造初始证据 JSON
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(userId, dto.getEvidenceData());
        String initialJson = JSONUtil.toJsonStr(wrapper);

        // 3. 插入数据库
        DisputeTickets ticket = new DisputeTickets();
        ticket.setTicketId(ticketId);
        ticket.setClaimId(dto.getClaimId());
        ticket.setReason(dto.getReason());
        ticket.setEvidenceData(initialJson); // 存入初始 JSON
        ticket.setDeadline(deadline);
        ticket.setStatus("Reviewing");
        ticket.setInitiatorId(userId);
        ticket.setCreateTime(new Date());
        ticket.setUpdateTime(new Date());

        disputesMapper.insert(ticket);

        // 4. 利用视图 Mapper 快速找到 item_id 并更新状态
        ViewDisputeDetail viewDetail = viewDisputeDetailMapper.selectOne(
                new LambdaQueryWrapper<ViewDisputeDetail>()
                        .eq(ViewDisputeDetail::getTicketId, ticketId)
        );

        if (viewDetail != null && viewDetail.getItemId() != null) {
            // 更新物品状态为 UNDER_DISPUTE (假设状态码为 4 或字符串 "UNDER_DISPUTE")
            // 这通过 Feign 或直接 Service 调用实现
            itemsService.setItemStatus(viewDetail.getItemId(), "UNDER_DISPUTE");
            log.info("关联物品状态已更新为维权中: itemId={}", viewDetail.getItemId());
        } else {
            log.warn("未能通过视图找到关联物品，ticketId={}", ticketId);
        }

        // TODO 修改认领单状态为 UNDER_DISPUTE

        // 4. 返回文档要求的格式
        Map<String, Object> res = new HashMap<>();
        res.put("ticket_id", ticketId);
        res.put("deadline", deadline);
        return Result.success(res);
    }

    @Override
    public Result<String> revokeDispute(String ticketId){
        DisputeTickets ticket = this.getById(ticketId);
        if(ticket == null){
            return Result.error(400,"纠纷工单不存在");
        }
        if(!ticket.getStatus().equals("Reviewing")){
            return Result.error(400,"仅能撤销审核中的纠纷工单");
        }
        if(!ticket.getInitiatorId().equals(UserContext.getUserId())){
            return Result.error(403,"无权撤销他人纠纷工单");
        }
        ticket.setStatus("Revoked");
        ticket.setUpdateTime(new Date());
        this.updateById(ticket);
        return Result.success("纠纷工单已撤销");
    }
}




