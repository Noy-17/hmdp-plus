package org.javaup.rabbitmq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopSyncMessage implements Serializable {
    private Long shopId;
    private String operation;
}
