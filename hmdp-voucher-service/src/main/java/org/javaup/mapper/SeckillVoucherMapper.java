package org.javaup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.javaup.entity.SeckillVoucher;

public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {
   
    @Update("UPDATE tb_seckill_voucher SET stock = stock + 1,update_time = NOW() WHERE voucher_id = #{voucherId}")
    Integer rollbackStock(@Param("voucherId")Long voucherId);

}
