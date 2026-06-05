package org.javaup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.javaup.dto.GetVoucherOrderRouterDto;
import org.javaup.entity.VoucherOrderRouter;

public interface IVoucherOrderRouterService extends IService<VoucherOrderRouter> {
    
    Long get(GetVoucherOrderRouterDto getVoucherOrderRouterDto);
}
