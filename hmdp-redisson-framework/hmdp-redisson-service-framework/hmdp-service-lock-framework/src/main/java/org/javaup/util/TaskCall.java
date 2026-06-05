package org.javaup.util;

@FunctionalInterface
public interface TaskCall<V> {

    /**
     * 执行任务
     * @return 结果
     * */
    V call();
}
