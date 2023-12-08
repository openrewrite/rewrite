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
package org.openrewrite.java.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.TypeMappings;
import org.openrewrite.java.tree.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindTypeMappings extends ScanningRecipe<FindTypeMappings.Accumulator> {
    transient TypeMappings typeMappingsPerSource = new TypeMappings(this);

    @Override
    public String getDisplayName() {
        return "Find type mappings";
    }

    @Override
    public String getDescription() {
        return "Find types mapped to J trees.";
    }

    @Data
    public static class Accumulator {
        Map<String, Map<String, Map<String, AtomicInteger>>> sourceToMappedTypeCount = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            String sourcePath = "";
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                sourcePath = cu.getSourcePath().toString();
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof TypedTree) {
                    Map<String, AtomicInteger> counts = acc.getSourceToMappedTypeCount()
                            .computeIfAbsent(sourcePath, k -> new HashMap<>())
                            .computeIfAbsent(tree.getClass().getName(), k -> new HashMap<>());
                    if (tree instanceof J.Identifier) {
                        J.Identifier i = (J.Identifier) tree;
                        if (i.getFieldType() != null) {
                            counts.computeIfAbsent(i.getFieldType().getClass().getName(), k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                    } else if (tree instanceof J.MemberReference) {
                        J.MemberReference m = (J.MemberReference) tree;
                        if (m.getVariableType() != null) {
                            counts.computeIfAbsent(m.getVariableType().getClass().getName(), k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        } else if (m.getMethodType() != null) {
                            counts.computeIfAbsent(m.getMethodType().getClass().getName(), k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                    } else if (tree instanceof J.MethodDeclaration) {
                        J.MethodDeclaration m = (J.MethodDeclaration) tree;
                        if (m.getMethodType() != null) {
                            counts.computeIfAbsent(m.getMethodType().getClass().getName(), k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                    } else if (tree instanceof J.MethodInvocation) {
                        J.MethodInvocation m = (J.MethodInvocation) tree;
                        if (m.getMethodType() != null) {
                            counts.computeIfAbsent(m.getMethodType().getClass().getName(), k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                    } else if (tree instanceof J.NewClass) {
                        J.NewClass m = (J.NewClass) tree;
                        if (m.getMethodType() != null) {
                            counts.computeIfAbsent(m.getMethodType().getClass().getName(), k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                    } else if (tree instanceof J.VariableDeclarations.NamedVariable) {
                        J.VariableDeclarations.NamedVariable n = (J.VariableDeclarations.NamedVariable) tree;
                        if (n.getVariableType() != null) {
                            counts.computeIfAbsent(n.getVariableType().getClass().getName(), k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                    }
                    counts.computeIfAbsent(((TypedTree) tree).getType() == null ?
                                    "null" :
                                    ((TypedTree) tree).getType().getClass().getName(), k -> new AtomicInteger(0))
                            .incrementAndGet();
                }
                return super.visit(tree, ctx);
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        for (Map.Entry<String, Map<String, Map<String, AtomicInteger>>> sources : acc.getSourceToMappedTypeCount().entrySet()) {
            for (Map.Entry<String, Map<String, AtomicInteger>> trees : sources.getValue().entrySet()) {
                for (Map.Entry<String, AtomicInteger> types : trees.getValue().entrySet()) {
                    typeMappingsPerSource.insertRow(ctx, new TypeMappings.Row(sources.getKey(), trees.getKey(), types.getKey(), types.getValue().get()));
                }
            }
        }

        return Collections.emptyList();
    }
}
