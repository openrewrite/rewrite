package org.openrewrite.java;

@SuppressWarnings("unused")
public interface LoggingHandler {

    default void onError(String message) {}

    default void onWarn(String message) {}
}
