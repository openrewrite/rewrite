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
package org.openrewrite.java.service;

import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.service.SourceFileService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class JavaSourceFileService extends SourceFileService {
    @Override
    public long computeWeight(SourceFile sourceFile, Predicate<Object> uniqueIdentity) {
        AtomicInteger n = new AtomicInteger();
        new JavaVisitor<AtomicInteger>() {
            final JavaTypeVisitor<AtomicInteger> typeVisitor = new JavaTypeVisitor<AtomicInteger>() {
                @Override
                public JavaType visit(@Nullable JavaType javaType, AtomicInteger n) {
                    if (javaType != null && uniqueIdentity.test(javaType)) {
                        n.incrementAndGet();
                        return super.visit(javaType, n);
                    }
                    //noinspection ConstantConditions
                    return javaType;
                }
            };

            final JavadocVisitor<AtomicInteger> javadocVisitor = new JavadocVisitor<AtomicInteger>(this) {
                @Override
                public @Nullable Javadoc visit(@Nullable Tree tree, AtomicInteger n) {
                    if (tree != null) {
                        n.incrementAndGet();
                    }
                    return super.visit(tree, n);
                }
            };

            @Override
            public @Nullable J visit(@Nullable Tree tree, AtomicInteger n) {
                if (tree != null) {
                    n.incrementAndGet();
                }
                return super.visit(tree, n);
            }

            @Override
            public JavaType visitType(@Nullable JavaType javaType, AtomicInteger n) {
                return typeVisitor.visit(javaType, n);
            }

            @Override
            protected JavadocVisitor<AtomicInteger> getJavadocVisitor() {
                return javadocVisitor;
            }
        }.visit(sourceFile, n);
        return n.get();
    }

    @Override
    public <P> TreeVisitor<?, P> newVisitor(BiConsumer<Tree, P> consumer) {
        return new JavaVisitor<P>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, P p) {
                consumer.accept(tree, p);
                return super.visit(tree, p);
            }
        };
    }

}
