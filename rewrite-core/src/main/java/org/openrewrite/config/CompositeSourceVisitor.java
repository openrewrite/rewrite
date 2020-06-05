package org.openrewrite.config;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;

import java.util.List;

public class CompositeSourceVisitor<T> extends SourceVisitor<T> {
    private final String name;
    private final List<SourceVisitor<T>> delegates;

    public CompositeSourceVisitor(String name, List<SourceVisitor<T>> delegates) {
        this.name = name;
        this.delegates = delegates;
        delegates.forEach(this::andThen);
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("name", name);
    }

    public List<SourceVisitor<T>> getDelegates() {
        return delegates;
    }

    @Override
    public T defaultTo(Tree t) {
        return delegates.stream().findAny().map(v -> v.defaultTo(t)).orElse(null);
    }
}
