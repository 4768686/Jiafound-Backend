package org.example.campusitem.controller;

import org.example.campusitem.dto.ItemListItemDTO;
import org.example.campusitem.dto.ItemPublishRequest;
import org.example.campusitem.entity.ItemImages;
import org.example.campusitem.entity.Items;
import org.example.campusitem.common.Result;
import org.example.campusitem.service.ItemImagesService;
import org.example.campusitem.service.ItemsService;
import org.example.campusitem.utils.AIImageDescriptionUtils;
import org.example.campusitem.utils.AzureBlobUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin // 必须添加此注解以允许前端跨域请求
@RestController
@RequestMapping("/item")
public class ItemsController {

    @Autowired
    private ItemsService itemsService;

    @Autowired
    private AIImageDescriptionUtils aiImageUtils;

    @Autowired
    private ItemImagesService itemImagesService;

    @Autowired
    private AzureBlobUtils azureBlobUtils; // 注入工具类

    @Autowired
    private RestTemplate restTemplate; // 注入RestTemplate用于跨服务调用

    @Value("${campus.user.service.url:http://localhost:8085}")
    private String userServiceUrl; // 用户服务地址
    /**
     * 发布物品信息（核心逻辑）
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishItem(@RequestBody ItemPublishRequest request) {
        try {
            // 1. 如果是寻物启事且开启悬赏，先冻结赏币
            if ("LOST".equalsIgnoreCase(request.getItemType()) && 
                Boolean.TRUE.equals(request.getRewardEnabled()) && 
                request.getRewardPoints() != null && 
                request.getRewardPoints().compareTo(BigDecimal.ZERO) > 0) {
                
                // 调用 user 服务的冻结赏币接口
                boolean freezeSuccess = freezeCoinForReward(
                    request.getUserId(), 
                    request.getRewardPoints(), 
                    null // itemId 在保存后才有，先传null，后续可优化
                );
                
                if (!freezeSuccess) {
                    return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "赏币冻结失败，可能余额不足"
                    ));
                }
            }
            
            // 2. 保存物品信息（包含匹配逻辑）
            Object matchResult = itemsService.saveItemWithImages(request);
        
            if (matchResult != null) {
                return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "match", true,
                    "message", "AI 为您找到了可能的匹配物品！",
                    "data", matchResult
                ));
            } else {
                String msg = "LOST".equals(request.getItemType()) ? 
                             "暂无匹配物品，已为您加入失物信息库" : "未找到失主，已加入待取物品库";
                return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "match", false,
                    "message", msg
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                "code", 500,
                "message", "发布失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 跨服务调用：冻结赏币
     * @return 是否成功
     */
    private boolean freezeCoinForReward(String userId, BigDecimal amount, String itemId) {
        try {
            String url = userServiceUrl + "/internal/v1/user/coin/freeze";
            
            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("amount", amount);
            requestBody.put("itemId", itemId != null ? itemId : "pending");
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发起HTTP调用
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            // 判断响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object code = response.getBody().get("code");
                return code != null && (code.equals(200) || code.equals("200"));
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * AI 识图：根据图片生成描述
     */
    @PostMapping("/upload-and-analyze")
    public Result<Map<String, String>> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.fail("文件不能为空");
            }
    
            // 1. 先进行 AI 识图（基于本地传入的文件流）
            // 这里的 generateDescriptionFromImage 已经支持 MultipartFile
            String aiDescription = aiImageUtils.generateDescriptionFromImage(file);
    
            // 2. 将同一个文件流上传到 Azure 云端
            // 使用你提供的 AzureBlobUtils 工具类
            String cloudUrl = azureBlobUtils.uploadFile(file);
    
            // 3. 封装结果返回给前端
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("url", cloudUrl);           // 云端生成的 URL
            resultMap.put("description", aiDescription); // AI 生成的语义描述
            
            System.out.println("AI分析完成: " + aiDescription);
            System.out.println("云端上传完成: " + cloudUrl);
    
            return Result.success(resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("处理失败: " + e.getMessage());
        }
    }


    // 1. 获取物品大厅 (已有的接口，确保逻辑正确)
    @GetMapping("/hall")
    public Result<List<ItemListItemDTO>> getHall(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        QueryWrapper<Items> query = new QueryWrapper<>();
        if (!"ALL".equalsIgnoreCase(type)) {
            // 使用 equalsIgnoreCase 或者在逻辑中映射：
            // 前端传 LOST -> 匹配数据库的 Lost 或 LOST
            query.and(wrapper -> wrapper.eq("item_type", type).or().eq("item_type", "Lost".equalsIgnoreCase(type) ? "Lost" : "Found"));
        }
        // 只展示已发布的物品
        query.eq("status", "Published").orderByDesc("publish_time");
        
        List<Items> items = itemsService.list(query);
        return Result.success(convertToDTOList(items));
    }

    // 2. 查询个人发布记录
    // 从请求头 Authorization 获取 userId (微服务网关通常会解析 Token 并注入 Header)
    @GetMapping("/my-list")
    public Result<Map<String, Object>> getMyList(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        // 如果没有网关层注入，暂时 Mock 一个 ID 进行测试
        if (userId == null) userId = "u_publisher_001"; 

        QueryWrapper<Items> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("publish_time");
        
        List<Items> items = itemsService.list(query);
        List<ItemListItemDTO> list = convertToDTOList(items);
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", list.size());
        data.put("list", list);
        return Result.success(data);
    }

    // 物品匹配通知 (模拟)
    @GetMapping("/match-notifications")
    public Result<List<Map<String, Object>>> getNotifications() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> notify = new HashMap<>();
        notify.put("notificationId", "NOTIFY_001");
        notify.put("content", "您发布的【黑色耳机】可能有匹配项");
        notify.put("similarity", 0.92);
        list.add(notify);
        return Result.success(list);
    }

    // 辅助转换方法
    private List<ItemListItemDTO> convertToDTOList(List<Items> items) {
        List<ItemListItemDTO> dtoList = new ArrayList<>();
        for (Items item : items) {
            ItemListItemDTO dto = new ItemListItemDTO();
            dto.setItemID(item.getItemId());
            dto.setTitle(item.getDescription());
            dto.setType(item.getItemType());
            dto.setStatus(item.getStatus());
            dto.setLocation(item.getLocationText());
            dto.setPublishTime(item.getPublishTime() != null ? item.getPublishTime().toString() : "");
            dto.setRewardPoints(item.getRewardPoints());
            dto.setLatitude(item.getLatitude() != null ? item.getLatitude().doubleValue() : 0);
            dto.setLongitude(item.getLongitude() != null ? item.getLongitude().doubleValue() : 0);

            // --- 核心修复：查询该物品关联的所有图片 ---
            List<ItemImages> imgs = itemImagesService.list(
                new QueryWrapper<ItemImages>().eq("item_id", item.getItemId())
            );
            // 提取图片 URL 并存入 DTO
            List<String> imageUrls = imgs.stream()
                                        .map(ItemImages::getImageUrl)
                                        .collect(Collectors.toList());
            
            // 如果没有图片，给一个默认图防止前端报错
            if (imageUrls.isEmpty()) {
                imageUrls.add("https://via.placeholder.com/150?text=No+Image");
            }
            dto.setImages(imageUrls);
            
            dtoList.add(dto);
        }
        return dtoList;
    }
}