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
package org.openrewrite.java.search;

import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Acts as a sort of bloom filter for the presence of an import for a particular type in a {@link org.openrewrite.java.tree.J.CompilationUnit},
 * i.e. it may falsely report the presence of an import, but would never negatively report when the type is in present.
 */
@Incubating(since = "7.4.0")
public class MaybeUsesImport<P> extends JavaIsoVisitor<P> {
    private final List<String> fullyQualifiedTypeSegments;

    public MaybeUsesImport(String fullyQualifiedType) {
        Scanner scanner = new Scanner(fullyQualifiedType);
        scanner.useDelimiter("\\.");
        this.fullyQualifiedTypeSegments = new ArrayList<>();
        while (scanner.hasNext()) {
            fullyQualifiedTypeSegments.add(scanner.next());
        }
    }

    @Override
    public J.Import visitImport(J.Import _import, P p) {
        J.Import i = super.visitImport(_import, p);
        if (matchesType(i)) {
            i = SearchResult.found(i);
        }
        return i;
    }

    private boolean matchesType(J.Import i) {
        Expression prior = null;
        for (String segment : fullyQualifiedTypeSegments) {
            for (Expression expr = i.getQualid(); expr != prior; ) {
                if (expr instanceof J.Identifier) {
                    // this can only be the first segment
                    prior = expr;
                    if (!((J.Identifier) expr).getSimpleName().equals(segment)) {
                        return false;
                    }
                } else if (expr instanceof J.FieldAccess) {
                    J.FieldAccess fa = (J.FieldAccess) expr;
                    if (fa.getTarget() == prior) {
                        String simpleName = fa.getSimpleName();
                        if (!"*".equals(segment) && !simpleName.equals(segment) && !"*".equals(simpleName)) {
                            return false;
                        }
                        prior = fa;
                        continue;
                    }
                    expr = fa.getTarget();
                }
            }
        }

        for (Expression expr = i.getQualid(); expr != prior; ) {
            if (!(expr instanceof J.FieldAccess)) {
                return false; // don't think this can ever happen
            }

            J.FieldAccess fa = (J.FieldAccess) expr;
            if (Character.isLowerCase(fa.getSimpleName().charAt(0))) {
                return fa == i.getQualid() && i.isStatic();
            }
            expr = fa.getTarget();
        }

        return true;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
        return annotation;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        return classDecl;
    }
}
