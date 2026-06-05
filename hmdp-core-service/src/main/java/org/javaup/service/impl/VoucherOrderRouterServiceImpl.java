package org.javaup.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.GetVoucherOrderRouterDto;
import org.javaup.entity.VoucherOrderRouter;
import org.javaup.mapper.VoucherOrderRouterMapper;
import org.javaup.service.IVoucherOrderRouterService;
import org.javaup.utils.UserHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class VoucherOrderRouterServiceImpl extends ServiceImpl<VoucherOrderRouterMapper, VoucherOrderRouter> implements IVoucherOrderRouterService {
    
    @Override
    public Long get(GetVoucherOrderRouterDto getVoucherOrderRouterDto) {
        VoucherOrderRouter voucherOrderRouter = lambdaQuery()
                .eq(VoucherOrderRouter::getUserId,  UserHolder.getUser().getId())
                .eq(VoucherOrderRouter::getVoucherId, getVoucherOrderRouterDto.getVoucherId())
                .one();
        if (Objects.nonNull(voucherOrderRouter)) {
            return voucherOrderRouter.getOrderId();
        }
        return null;
    }
}
