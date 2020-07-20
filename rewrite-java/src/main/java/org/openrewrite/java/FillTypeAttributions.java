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
package org.openrewrite.java;

import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;

import static java.util.Arrays.stream;

public class FillTypeAttributions extends JavaRefactorVisitor {
    private final JavaType.Class[] types;

    public FillTypeAttributions(JavaType.Class[] types) {
        this.types = types;
    }

    @Override
    public J reduce(J r1, J r2) {
        J j = super.reduce(r1, r2);
        if (r2 != null && j instanceof NameTree) {
            j = ((NameTree) j).withType(((NameTree) r2).getType());
        }
        return j;
    }

    @Override
    public J visitTree(Tree tree) {
        if (tree instanceof NameTree) {
            NameTree n = (NameTree) super.visitTree(tree);

            if (n.getType() == null) {
                if (n instanceof J.Ident) {
                    J.Ident ident = (J.Ident) n;
                    return stream(types)
                            .filter(im -> im.getClassName().equals(((J.Ident) n).getSimpleName()))
                            .findAny()
                            .map(im -> ident.withType(JavaType.Class.build(im.getFullyQualifiedName())))
                            .orElse(ident);
                } else if (n instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) n;
                    return stream(types)
                            .filter(im -> im.getFullyQualifiedName().equals(fieldAccess.printTrimmed()))
                            .findAny()
                            .map(im -> fieldAccess.withType(JavaType.Class.build(im.getFullyQualifiedName())))
                            .orElse(fieldAccess);
                }
            }

            return n;
        }

        return super.visitTree(tree);
    }
}
