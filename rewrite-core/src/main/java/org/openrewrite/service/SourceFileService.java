/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.service;

import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SourceFileService {
    public long computeWeight(SourceFile sourceFile, Predicate<Object> uniqueIdentity) {
        AtomicInteger n = new AtomicInteger();
        this.<AtomicInteger>newVisitor((tree, atomicInteger) -> {
            if (tree != null) {
                atomicInteger.incrementAndGet();
            }
        }).visit(sourceFile, n);
        return n.get();
    }

    public <P> TreeVisitor<?, P> newVisitor(BiConsumer<Tree, P> consumer) {
        return new TreeVisitor<Tree, P>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, P p) {
                consumer.accept(tree, p);
                return super.visit(tree, p);
            }
        };
    }
}
