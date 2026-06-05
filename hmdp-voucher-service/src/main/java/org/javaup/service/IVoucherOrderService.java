package org.javaup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.javaup.dto.CancelVoucherOrderDto;
import org.javaup.dto.GetVoucherOrderByVoucherIdDto;
import org.javaup.dto.GetVoucherOrderDto;
import org.javaup.dto.Result;
import org.javaup.entity.VoucherOrder;
import org.javaup.rabbitmq.message.SeckillVoucherMessage;
import org.javaup.message.MessageExtend;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result<Long> seckillVoucher(Long voucherId);

    void createVoucherOrderV1(VoucherOrder voucherOrder);
    
    boolean createVoucherOrderV2(MessageExtend<SeckillVoucherMessage> message);
    
    Long getSeckillVoucherOrder(GetVoucherOrderDto getVoucherOrderDto);
    
    Boolean cancel(CancelVoucherOrderDto cancelVoucherOrderDto);
    
    boolean autoIssueVoucherToEarliestSubscriber(final Long voucherId, final Long excludeUserId);
    
    Long getSeckillVoucherOrderIdByVoucherId(GetVoucherOrderByVoucherIdDto getVoucherOrderByVoucherIdDto);
}
