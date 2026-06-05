package org.javaup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.javaup.entity.SeckillVoucher;
import org.javaup.model.SeckillVoucherFullModel;

public interface ISeckillVoucherService extends IService<SeckillVoucher> {
    
    SeckillVoucherFullModel queryByVoucherId(Long voucherId);
    
    void loadVoucherStock(Long voucherId);
    
    boolean rollbackStock(Long voucherId);
}
