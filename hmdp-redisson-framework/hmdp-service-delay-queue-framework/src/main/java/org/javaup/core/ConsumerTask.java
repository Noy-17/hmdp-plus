package org.javaup.core;

public interface ConsumerTask {
    
    void execute(String content);
  
    String topic();
}
