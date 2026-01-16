package org.example.campusaudit.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.campusaudit.service.ItemsService;
import org.example.campusaudit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/audit")
@Slf4j
public class InternalAuditController {

    @Autowired
    private ItemsService itemsService;

    /**
     * 接收来自 Item 系统的审核请求
     */
    @PostMapping("/process")
    public Result<String> receiveAuditTask(
            @RequestParam String itemId) {

        // 2. 参数基本校验
        if (itemId == null) {
            return Result.error(400,"参数不能为空");
        }

        log.info("接收到内部审核请求: itemId={}", itemId);

        // 3. 调用 Service 开启异步任务
        return itemsService.processAudit(itemId);
    }
}
