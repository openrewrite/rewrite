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

import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

public abstract class Recipe {
    public static final TreeProcessor<?, ExecutionContext> NOOP = new TreeProcessor<Tree, ExecutionContext>() {
        @Override
        Tree visitInternal(Tree tree, ExecutionContext ctx) {
            return tree;
        }
    };

    @Nullable
    private Recipe next;

    private final Supplier<TreeProcessor<?, ExecutionContext>> processor;

    protected Recipe(Supplier<TreeProcessor<?, ExecutionContext>> processor) {
        this.processor = processor;
    }

    protected Recipe() {
        this.processor = () -> NOOP;
    }

    protected void doNext(Recipe recipe) {
        Recipe head = this;
        for (Recipe tail = next; tail != null; tail = tail.next) {
            // FIXME implement me!
        }
        head.next = recipe;
    }

    protected List<SourceFile> visit(List<SourceFile> sourceFiles, ExecutionContext execution) {
        List<SourceFile> acc = sourceFiles;
        List<SourceFile> temp = acc;
        for (int i = 0; i < execution.getMaxCycles(); i++) {
            // if this recipe isn't valid we just skip it and proceed to next
            if (validate().isValid()) {
                temp = ListUtils.map(temp, s -> {
                    try {
                        return (SourceFile) processor.get().visit(s, execution);
                    } catch(Throwable t) {
                        execution.getOnError().accept(t);
                        return s;
                    }
                });
            }
            if (next != null) {
                temp = next.visit(temp, execution);
            }
            if (temp == acc) {
                break;
            }
            acc = temp;
        }
        return acc;
    }

    public final List<Result> run(List<SourceFile> sourceFiles) {
        return run(sourceFiles, ExecutionContext.builder().build());
    }

    public final List<Result> run(List<SourceFile> sourceFiles, ExecutionContext context) {
        List<SourceFile> after = visit(sourceFiles, context);

        if (after == sourceFiles) {
            return emptyList();
        }

        // FIXME compute list difference between sourceFiles and after and generate Result
        return emptyList();
    }

    public Validated validate() {
        return Validated.none();
    }

    public String getName() {
        return getClass().getName();
    }
}
