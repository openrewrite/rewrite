/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.kotlin.tree.K;

/**
 * Visitor mixin composed with {@code RemoveAnnotationVisitor} via the
 * {@code TreeVisitorAdapter} SPI mechanism (registered at
 * {@code META-INF/services/org.openrewrite.java.RemoveAnnotationVisitor}).
 *
 * <p>Reason: file-scope annotations like {@code @file:OptIn(...)} live on
 * {@code K.CompilationUnit.annotations} — Kotlin-specific structure that
 * the upstream {@code JavaIsoVisitor}-based {@code RemoveAnnotationVisitor}
 * never sees. Its declaration-level cleanup hooks (visitClassDeclaration /
 * visitMethodDeclaration / visitVariableDeclarations) only fire for J nodes,
 * leaving stranded blank lines where a file-scope annotation used to sit.
 *
 * <p>This mixin overrides the K-specific {@code visitCompilationUnit}.
 * It calls super to drive the normal Kotlin traversal — which routes each
 * file-level {@code J.Annotation} back through the proxy to the base's
 * removal logic — and then, if the annotation list shrank, runs
 * {@code NormalizeFormatVisitor} + {@code BlankLinesVisitor} on the result
 * to absorb stranded whitespace without paying for full auto-format.
 */
public class RemoveAnnotationKotlinMixin extends KotlinIsoVisitor<ExecutionContext> {

    @Override
    public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
        K.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
        if (c.getAnnotations().size() < cu.getAnnotations().size()) {
            AutoFormatService service = c.service(AutoFormatService.class);
            c = (K.CompilationUnit) service.normalizeFormatVisitor(null).visitNonNull(c, ctx);
            c = (K.CompilationUnit) service.blankLinesVisitor(c, null).visitNonNull(c, ctx);
        }
        return c;
    }
}
