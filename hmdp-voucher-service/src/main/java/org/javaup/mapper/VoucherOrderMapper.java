package org.javaup.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.javaup.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {
    
    /**
     * 根据优惠券id和用户id删除数据
     * @param voucherId 优惠券id
     * @param userId 用户id
     * @return 删除数量
     */
    @Delete("DELETE FROM tb_voucher_order where voucher_id = #{voucherId} and user_id = #{userId}")
    Integer deleteVoucherOrder(@Param("voucherId")Long voucherId, @Param("userId")Long userId);

}
