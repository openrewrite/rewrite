package org.openrewrite.config;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;

import java.util.List;

public class CompositeSourceVisitor extends SourceVisitor<Object> {
    private String name;
    private final List<SourceVisitor<Object>> delegates;

    public CompositeSourceVisitor(String name, List<SourceVisitor<Object>> delegates) {
        this.name = name;
        this.delegates = delegates;
        delegates.forEach(this::andThen);
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("name", name);
    }

    CompositeSourceVisitor setName(String name) {
        this.name = name;
        return this;
    }

    public Class<?> getVisitorType() {
        return delegates.stream().findAny()
                .map(Object::getClass)
                .orElse(null);
    }

    public String getName() {
        return name;
    }

    @Override
    public Object defaultTo(Tree t) {
        return delegates.stream().findAny().map(v -> v.defaultTo(t)).orElse(null);
    }
}
