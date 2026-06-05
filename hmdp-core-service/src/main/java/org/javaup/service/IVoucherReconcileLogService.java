package org.javaup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.javaup.dto.VoucherReconcileLogDto;
import org.javaup.entity.VoucherReconcileLog;
import org.javaup.rabbitmq.message.SeckillVoucherMessage;
import org.javaup.message.MessageExtend;

public interface IVoucherReconcileLogService extends IService<VoucherReconcileLog> {
    
    boolean saveReconcileLog(Integer logType,
                             Integer businessType,
                             String detail,
                             MessageExtend<SeckillVoucherMessage> message);
    
    boolean saveReconcileLog(Integer logType,
                             Integer businessType,
                             String detail,
                             Long traceId,
                             MessageExtend<SeckillVoucherMessage> message);
    
    
    boolean saveReconcileLog(VoucherReconcileLogDto voucherReconcileLogDto);
}