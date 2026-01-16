package org.example.campususer.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.example.campususer.dto.CoinLogVO;
import org.example.campususer.dto.PageResponse;
import org.example.campususer.entity.CoinLogs;
import org.example.campususer.entity.Users;
import org.example.campususer.mapper.CoinLogsMapper;
import org.example.campususer.mapper.UsersMapper;
import org.example.campususer.service.CoinLogsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
* @author 31830
* @description 针对表coin_logs(资金流水)的数据库操作Service实现
* @createDate 2026-01-06 17:52:19
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class CoinLogsServiceImpl extends ServiceImpl<CoinLogsMapper, CoinLogs>
    implements CoinLogsService{

    private final UsersMapper usersMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(String userId, Integer amount) {
        // 1. 查询用户
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 计算赏币数量（1元 = 100赏币）
        BigDecimal coinAmount = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100));
        
        // 3. 更新用户余额
        BigDecimal newBalance = user.getCoinBalance().add(coinAmount);
        user.setCoinBalance(newBalance);
        user.setUpdateTime(new Date());
        usersMapper.updateById(user);

        // 4. 插入流水记录
        CoinLogs coinLog = new CoinLogs();
        coinLog.setLogId(UUID.randomUUID().toString().replace("-", ""));
        coinLog.setUserId(userId);
        coinLog.setAmount(coinAmount);
        coinLog.setType("RECHARGE");
        coinLog.setCreateTime(new Date());
        this.save(coinLog);

        log.info("充值成功: userId={}, 充值金额={}元, 获得赏币={}, 新余额={}", userId, amount, coinAmount, newBalance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String userId, Integer coinAmount) {
        // 1. 查询用户
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 检查余额是否充足
        BigDecimal withdrawAmount = BigDecimal.valueOf(coinAmount);
        if (user.getCoinBalance().compareTo(withdrawAmount) < 0) {
            throw new RuntimeException("余额不足，当前余额： " + user.getCoinBalance() + " 赏币");
        }

        // 3. 更新用户余额
        BigDecimal newBalance = user.getCoinBalance().subtract(withdrawAmount);
        user.setCoinBalance(newBalance);
        user.setUpdateTime(new Date());
        usersMapper.updateById(user);

        // 4. 插入流水记录
        CoinLogs coinLog = new CoinLogs();
        coinLog.setLogId(UUID.randomUUID().toString().replace("-", ""));
        coinLog.setUserId(userId);
        coinLog.setAmount(withdrawAmount.negate()); // 负数表示支出
        coinLog.setType("WITHDRAW");
        coinLog.setCreateTime(new Date());
        this.save(coinLog);

        log.info("提现成功: userId={}, coinAmount={}, newBalance={}", userId, coinAmount, newBalance);
    }

    @Override
    public PageResponse<CoinLogVO> getCoinLogs(String userId, int page, int size) {
        // 1. 构建查询条件
        LambdaQueryWrapper<CoinLogs> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoinLogs::getUserId, userId)
                .orderByDesc(CoinLogs::getCreateTime);

        // 2. 分页查询
        Page<CoinLogs> pageRequest = new Page<>(page, size);
        Page<CoinLogs> pageResult = this.page(pageRequest, queryWrapper);

        // 3. 转换VO
        List<CoinLogVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 构建分页响应
        return PageResponse.of(voList, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    /**
     * 将CoinLogs 实体转换为CoinLogVO
     */
    private CoinLogVO convertToVO(CoinLogs coinLog) {
        CoinLogVO vo = new CoinLogVO();
        vo.setLogId(coinLog.getLogId());
        vo.setType(coinLog.getType());
        vo.setAmount(coinLog.getAmount() != null ? coinLog.getAmount() : BigDecimal.ZERO);
        vo.setRelatedItemId(coinLog.getRelatedItemId());
        
        if (coinLog.getCreateTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            vo.setCreateTime(sdf.format(coinLog.getCreateTime()));
        }
        
        return vo;
    }
}




