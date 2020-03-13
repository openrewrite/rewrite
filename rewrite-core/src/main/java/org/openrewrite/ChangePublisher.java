package org.openrewrite;

public interface ChangePublisher {
    boolean publish(Change<?> change);
}
