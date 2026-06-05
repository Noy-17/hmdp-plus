package org.javaup.service.impl;

import org.javaup.entity.ShopType;
import org.javaup.mapper.ShopTypeMapper;
import org.javaup.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

}
