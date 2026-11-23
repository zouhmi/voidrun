package com.voidrun;

/** Lifecycle of a single benchmark run. */
public enum RunState {
    IDLE,
    LOADING,
    WARMUP,
    RUNNING,
    FINISHED
}
