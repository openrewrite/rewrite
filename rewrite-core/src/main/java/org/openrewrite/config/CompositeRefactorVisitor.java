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
import org.openrewrite.Refactor;
import org.openrewrite.SourceFile;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;

import java.awt.*;
import java.util.List;

public class CompositeRefactorVisitor extends SourceVisitor<Tree> {
    private String name;
    private final List<SourceVisitor<? extends Tree>> delegates;

    public CompositeRefactorVisitor(String name, List<SourceVisitor<? extends Tree>> delegates) {
        this.name = name;
        this.delegates = delegates;

        for (SourceVisitor<? extends Tree> delegate : delegates) {
            //noinspection unchecked
            andThen((SourceVisitor<Tree>) delegate);
        }
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("name", name);
    }

    CompositeRefactorVisitor setName(String name) {
        this.name = name;
        return this;
    }

    public Class<?> getVisitorType() {
        return delegates.stream().findAny()
                .map(d -> d instanceof CompositeRefactorVisitor ?
                        ((CompositeRefactorVisitor) d).getVisitorType() :
                        d.getClass())
                .orElse(null);
    }

    public String getName() {
        return name;
    }

    @Override
    public Tree visitTree(Tree tree) {
        if (tree instanceof SourceFile) {
            Refactor<Tree> refactor = new Refactor<>(tree);
            return refactor.visit(delegates).fix().getFixed();
        }

        return super.visitTree(tree);
    }

    void extendsFrom(CompositeRefactorVisitor delegate) {
        delegates.add(0, delegate);
        andThen().add(0, delegate);
    }

    @Override
    public Tree defaultTo(Tree t) {
        return delegates.stream().findAny()
                .map(d -> d instanceof CompositeRefactorVisitor ?
                        ((CompositeRefactorVisitor) d).defaultTo(t) :
                        d.defaultTo(t))
                .orElse(null);
    }
}
