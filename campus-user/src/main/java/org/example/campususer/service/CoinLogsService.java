package org.example.campususer.service;

import org.example.campususer.dto.CoinLogVO;
import org.example.campususer.dto.PageResponse;
import org.example.campususer.entity.CoinLogs;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 31830
* @description 针对表【coin_logs(资金流水表)】的数据库操作Service
* @createDate 2026-01-06 17:52:19
*/
public interface CoinLogsService extends IService<CoinLogs> {

    /**
     * 充值（Mock 模式）
     * @param userId 用户ID
     * @param amount 充值金额（单位：元，1元 = 100赏币）
     */
    void recharge(String userId, Integer amount);

    /**
     * 提现（Mock 模式）
     * @param userId 用户ID
     * @param coinAmount 提现赏币数量
     */
    void withdraw(String userId, Integer coinAmount);

    /**
     * 分页查询用户的流水记录
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页响应
     */
    PageResponse<CoinLogVO> getCoinLogs(String userId, int page, int size);
}
