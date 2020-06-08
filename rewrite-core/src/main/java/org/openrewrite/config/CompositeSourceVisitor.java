/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
