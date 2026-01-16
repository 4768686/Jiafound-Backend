package org.example.campusclaim.service; // 或 .audit.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.campusclaim.entity.DisputeTicket;
import org.example.campusclaim.mapper.DisputeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
public class DisputeService extends ServiceImpl<DisputeMapper, DisputeTicket> {

    /**
     * 创建纠纷工单
     * @param claimId 认领单ID
     * @param userId 发起人ID
     * @param reason 原因
     * @return 生成的 ticketId
     */
    @Transactional(rollbackFor = Exception.class)
    public String createTicket(String claimId, String userId, String reason, String evidenceJsonData) {
        DisputeTicket ticket = new DisputeTicket();
        ticket.setTicketId(UUID.randomUUID().toString().replace("-", ""));
        ticket.setClaimId(claimId);
        ticket.setInitiatorId(userId);
        ticket.setReason(reason);
        ticket.setEvidenceData(evidenceJsonData); 
        ticket.setStatus("Reviewing");
        
        // 设置截止时间：当前时间 + 48小时
        long fortyEightHours = 48 * 60 * 60 * 1000L;
        ticket.setDeadline(new Date(System.currentTimeMillis() + fortyEightHours));
        
        ticket.setCreateTime(new Date());
        
        this.save(ticket);
        return ticket.getTicketId();
    }

    public String getActiveTicketInitiator(String claimId) {
    QueryWrapper<DisputeTicket> query = new QueryWrapper<>();
    query.eq("claim_id", claimId)
         .eq("status", "Reviewing") // 正在审核中的
         .last("LIMIT 1");
    DisputeTicket ticket = this.getOne(query);
    return ticket != null ? ticket.getInitiatorId() : null;
}

public boolean revokeTicket(String claimId, String userId) {
    QueryWrapper<DisputeTicket> query = new QueryWrapper<>();
    query.eq("claim_id", claimId)
         .eq("status", "Reviewing")
         .last("LIMIT 1");
    DisputeTicket ticket = this.getOne(query);
    
    if (ticket == null) return false;
    
    // 校验发起人：只有发起人能撤销
    if (!ticket.getInitiatorId().equals(userId)) {
        return false; 
    }
    
    ticket.setStatus("Revoked"); // 设为已撤销
    this.updateById(ticket);
    return true;
}
}