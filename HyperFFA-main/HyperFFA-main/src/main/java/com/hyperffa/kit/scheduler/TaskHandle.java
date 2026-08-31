package com.hyperffa.kit.scheduler;

@FunctionalInterface
public interface TaskHandle {
    void cancel();
}
