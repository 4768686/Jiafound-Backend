package org.example.campusaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor // 自动生成全参构造函数，解决报错
@NoArgsConstructor  // 建议保留无参构造函数，防止某些框架（如 Jackson）反序列化失败
public class AuditPageResult {
    private long total;
    private List<AuditItemDTO> list;
}
