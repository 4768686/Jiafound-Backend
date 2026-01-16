package org.example.campusaudit.service;

import org.example.campusaudit.dto.*;
import org.example.campusaudit.entity.DisputeTickets;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.campusaudit.utils.PageResult;
import org.example.campusaudit.utils.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
* @author 31830
* @description 针对表【dispute_tickets(纠纷裁决工单表)】的数据库操作Service
* @createDate 2026-01-06 17:49:39
*/
public interface DisputeTicketsService extends IService<DisputeTickets> {
    PageResult<DisputeTicketDTO> getAdminDisputePool(String status, int pageNum);
    DisputeDetailDTO getDisputeDetail(String ticketId);
    PageResult<MyDisputeListDTO> getMyDisputes(int pageNum, int pageSize);
    List<String> uploadImages(MultipartFile[] files);
    Result<String> submitEvidence(String ticketId, EvidenceRequestDTO requestDTO);
    Result<Map<String, Object>> applyDispute(DisputeApplyDTO dto);
    Result<String> revokeDispute(String ticketId);
    Result<Map<String, Object>> adminRuling(RulingRequestDTO dto);
}
