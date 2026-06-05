package org.javaup.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.javaup.dto.VoucherReconcileLogDto;
import org.javaup.entity.VoucherReconcileLog;
import org.javaup.enums.LogType;
import org.javaup.rabbitmq.message.SeckillVoucherMessage;
import org.javaup.mapper.VoucherReconcileLogMapper;
import org.javaup.message.MessageExtend;
import org.javaup.service.IVoucherReconcileLogService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VoucherReconcileLogServiceImpl extends ServiceImpl<VoucherReconcileLogMapper, VoucherReconcileLog>
        implements IVoucherReconcileLogService {
    
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReconcileLog(final Integer logType, final Integer businessType, final String detail, final MessageExtend<SeckillVoucherMessage> message) {
        SeckillVoucherMessage messageBody = message.getMessageBody();
        VoucherReconcileLogDto voucherReconcileLogDto = new VoucherReconcileLogDto();
        voucherReconcileLogDto.setOrderId(messageBody.getOrderId());
        voucherReconcileLogDto.setUserId(messageBody.getUserId());
        voucherReconcileLogDto.setVoucherId(messageBody.getVoucherId());
        voucherReconcileLogDto.setMessageId(message.getUuid());
        voucherReconcileLogDto.setDetail(detail);
        voucherReconcileLogDto.setBeforeQty(messageBody.getBeforeQty());
        voucherReconcileLogDto.setChangeQty(messageBody.getChangeQty());
        voucherReconcileLogDto.setAfterQty(messageBody.getAfterQty());
        voucherReconcileLogDto.setTraceId(messageBody.getTraceId());
        voucherReconcileLogDto.setLogType(logType);
        voucherReconcileLogDto.setBusinessType(businessType);
        if (voucherReconcileLogDto.getLogType().equals(LogType.RESTORE.getCode())) {
            voucherReconcileLogDto.setBeforeQty(messageBody.getAfterQty());
            voucherReconcileLogDto.setAfterQty(messageBody.getBeforeQty());
        }
        return saveReconcileLog(voucherReconcileLogDto);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReconcileLog(final Integer logType, final Integer businessType, final String detail, final Long traceId, final MessageExtend<SeckillVoucherMessage> message) {
        SeckillVoucherMessage messageBody = message.getMessageBody();
        VoucherReconcileLogDto voucherReconcileLogDto = new VoucherReconcileLogDto();
        voucherReconcileLogDto.setOrderId(messageBody.getOrderId());
        voucherReconcileLogDto.setUserId(messageBody.getUserId());
        voucherReconcileLogDto.setVoucherId(messageBody.getVoucherId());
        voucherReconcileLogDto.setMessageId(message.getUuid());
        voucherReconcileLogDto.setDetail(detail);
        voucherReconcileLogDto.setBeforeQty(messageBody.getBeforeQty());
        voucherReconcileLogDto.setChangeQty(messageBody.getChangeQty());
        voucherReconcileLogDto.setAfterQty(messageBody.getAfterQty());
        voucherReconcileLogDto.setTraceId(traceId);
        voucherReconcileLogDto.setLogType(logType);
        voucherReconcileLogDto.setBusinessType(businessType);
        if (voucherReconcileLogDto.getLogType().equals(LogType.RESTORE.getCode())) {
            voucherReconcileLogDto.setBeforeQty(messageBody.getAfterQty());
            voucherReconcileLogDto.setAfterQty(messageBody.getBeforeQty());
        }
        return saveReconcileLog(voucherReconcileLogDto);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReconcileLog(VoucherReconcileLogDto voucherReconcileLogDto) {
        VoucherReconcileLog logEntity = new VoucherReconcileLog();
        logEntity.setId(snowflakeIdGenerator.nextId())
                .setOrderId(voucherReconcileLogDto.getOrderId())
                .setUserId(voucherReconcileLogDto.getUserId())
                .setVoucherId(voucherReconcileLogDto.getVoucherId())
                .setMessageId(voucherReconcileLogDto.getMessageId())
                .setBusinessType(voucherReconcileLogDto.getBusinessType())
                .setDetail(voucherReconcileLogDto.getDetail())
                .setTraceId(voucherReconcileLogDto.getTraceId())
                .setLogType(voucherReconcileLogDto.getLogType())
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now())
                .setBeforeQty(voucherReconcileLogDto.getBeforeQty())
                .setChangeQty(voucherReconcileLogDto.getChangeQty())
                .setAfterQty(voucherReconcileLogDto.getAfterQty());
        return save(logEntity);
    }
}