package org.example.campusaudit.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.campusaudit.dto.AuditPageResult;
import org.example.campusaudit.sercurity.AdminOnly;
import org.example.campusaudit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.campusaudit.service.ItemsService;
import org.example.campusaudit.dto.AuditDecisionDTO;

@RestController
@RequestMapping("/api/v1/admin/audit")
@Slf4j
public class AuditController {
    @Autowired
    private ItemsService itemsService;

    @GetMapping("/pending-list")
    @AdminOnly
    public Result<AuditPageResult> getPendingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(itemsService.getPendingAuditList(page, pageSize));
    }

    @PutMapping("/action")
    @AdminOnly
    public Result<String> manualAudit(@RequestBody AuditDecisionDTO auditDecisionDTO) {
        return itemsService.manualAudit(auditDecisionDTO);
    }
}
