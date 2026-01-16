package org.example.campusaudit.controller;

import jakarta.validation.Valid;
import org.example.campusaudit.dto.*;
import org.example.campusaudit.sercurity.AdminOnly;
import org.example.campusaudit.service.DisputeTicketsService;
import org.example.campusaudit.utils.PageResult;
import org.example.campusaudit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/disputes") // 对应文档中的请求路径
public class DisputeTicketsController {

    @Autowired
    private DisputeTicketsService disputeTicketsService;

    /**
     * 管理端：查询纠纷审核大厅列表
     * * @param status 纠纷状态（可选参数，如 Reviewing）
     * @param page   当前页码（默认为 1）
     * @return 统一封装的 Result 对象，包含分页数据
     */
    @GetMapping("/admin/all")
    @AdminOnly
    public Result<PageResult<DisputeTicketDTO>> getAllDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page) {

        // 1. 调用 Service 层执行聚合查询逻辑
        // 注意：Controller 层不写具体的业务逻辑，只负责分发任务
        PageResult<DisputeTicketDTO> data = disputeTicketsService.getAdminDisputePool(status, page);

        // 2. 将 Service 返回的裸数据（PageResult）包装进 Result 模版中
        // 这样返回给前端的 JSON 就会包含 code: 200, msg: "操作成功" 等字段
        return Result.success(data);
    }

    @GetMapping("/my-disputes")
    public Result<PageResult<MyDisputeListDTO>> getMyDisputes(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<MyDisputeListDTO> data = disputeTicketsService.getMyDisputes(pageNum, pageSize);
        return Result.success(data);
    }

    @PostMapping("/upload-images")
    public Result<List<String>> handleImagesUpload(@RequestParam("files") MultipartFile[] files) {
        // 1. 调用 Service 批量上传
        List<String> urls = disputeTicketsService.uploadImages(files);

        // 2. 返回给前端 URL 列表，前端再随表单提交
        return Result.success(urls);
    }

    @PostMapping("/tickets/{ticket_id}/evidence")
    public Result<String> submitEvidence(
            @PathVariable("ticket_id") String ticketId,
            @RequestBody EvidenceRequestDTO requestDTO) {
        return disputeTicketsService.submitEvidence(ticketId, requestDTO);
    }

    @PutMapping("/apply")
    public Result<Map<String, Object>> applyDispute(@RequestBody DisputeApplyDTO dto) {
        return disputeTicketsService.applyDispute(dto);
    }

    @PutMapping("/tickets/{ticket_id}/revoke")
    public Result<String> revokeDispute(@PathVariable("ticket_id") String ticketId) {
        return disputeTicketsService.revokeDispute(ticketId);
    }

    @GetMapping("/tickets/{ticketId}")
    public Result<DisputeDetailDTO> getDetail(@PathVariable String ticketId) {
        DisputeDetailDTO detail = disputeTicketsService.getDisputeDetail(ticketId);
        return detail != null ? Result.success(detail) : Result.error(404, "工单不存在");
    }

    @AdminOnly
    @PostMapping("/admin/disputes/ruling")
    public Result<Map<String, Object>> adminRuling(@Valid @RequestBody RulingRequestDTO dto) {
        return disputeTicketsService.adminRuling(dto);
    }
}
