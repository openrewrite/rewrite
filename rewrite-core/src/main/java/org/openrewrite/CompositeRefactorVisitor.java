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
package org.openrewrite;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class CompositeRefactorVisitor implements RefactorVisitor<Tree> {
    private final String name;
    private final List<RefactorVisitor<? extends Tree>> delegates;

    public CompositeRefactorVisitor(String name, List<RefactorVisitor<? extends Tree>> delegates) {
        this.name = name;
        this.delegates = delegates;
    }

    @Override
    public Validated validate() {
        return delegates.stream()
                .map(RefactorVisitor::validate)
                .reduce(Validated.none(), (validation, acc) -> acc.and(validation));
    }

    @Override
    public Tree visit(Tree tree) {
        return tree;
    }

    @Override
    public Collection<Tree> generate() {
        return delegates.stream()
                .flatMap(d -> d.generate().stream())
                .collect(toList());
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("name", name);
    }

    public String getName() {
        return name;
    }

    public void extendsFrom(CompositeRefactorVisitor delegate) {
        delegates.add(0, delegate);
    }

    @Override
    public Tree defaultTo(Tree t) {
        return delegates.stream()
                .map(d -> d instanceof CompositeRefactorVisitor ?
                        ((CompositeRefactorVisitor) d).defaultTo(t) :
                        d.defaultTo(t))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    private final List<RefactorVisitor<? extends Tree>> andThen = new ArrayList<>();

    @Override
    public List<RefactorVisitor<? extends Tree>> andThen() {
        return andThen;
    }

    @Override
    public void next() {
        andThen.clear();
        delegates.forEach(RefactorVisitor::next);
        andThen.addAll(delegates);
    }
}
