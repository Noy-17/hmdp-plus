package org.javaup.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_user_behavior")
public class UserBehavior implements Serializable {
    @TableId
    private Long id;
    private Long userId;
    private String eventType;
    private Long targetId;
    private String targetType;
    private Long eventTimestamp;
    private LocalDateTime createTime;
}
