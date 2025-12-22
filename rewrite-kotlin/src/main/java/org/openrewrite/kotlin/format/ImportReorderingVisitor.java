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
package org.openrewrite.kotlin.format;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.style.ImportLayoutStyle;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.Style;

import java.util.HashSet;
import java.util.List;

public class ImportReorderingVisitor<P> extends KotlinIsoVisitor<P> {

    @Override
    public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, P p) {
        List<JRightPadded<J.Import>> importList = cu.getPadding().getImports();

        ImportLayoutStyle layoutStyle = Style.from(ImportLayoutStyle.class, cu, IntelliJ::importLayout);
        List<JRightPadded<J.Import>> ordered = layoutStyle.orderImports(importList, new HashSet<>());

        if (referentialIdentical(importList, ordered)) {
            return cu;
        } else {
            return cu.getPadding().withImports(ordered);
        }
    }

    private <T> boolean referentialIdentical(List<T> l1, List<T> l2) {
        if (l1.size() != l2.size()) {
            return false;
        }

        for (int i = 0; i < l1.size(); i++) {
            if (l1.get(i) != l2.get(i)) {
                return false;
            }
        }

        return true;
    }
}
