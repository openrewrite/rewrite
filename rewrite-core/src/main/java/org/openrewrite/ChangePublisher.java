package org.openrewrite;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

public interface ChangePublisher {
    void publish(Collection<Change<?>> changes);

    default void refactorAndPublish(Collection<Refactor<?, ?>> refactor) {
        publish(refactor.stream().map(Refactor::fix).collect(toList()));
    }
}
