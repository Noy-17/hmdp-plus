package org.javaup.service.impl;

import org.javaup.entity.BlogComments;
import org.javaup.mapper.BlogCommentsMapper;
import org.javaup.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
