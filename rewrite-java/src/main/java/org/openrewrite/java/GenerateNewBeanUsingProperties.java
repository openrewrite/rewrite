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

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

public class GenerateNewBeanUsingProperties {
    public static class Scoped extends JavaIsoRefactorVisitor {
        /**
         * The block to insert the new bean into.
         */
        private final J.Block<? extends J> scope;

        /**
         * Statement that this new bean should precede or {@code null}
         * if it should be the first statement in the block.
         */
        @Nullable
        private final J before;

        private final JavaType.FullyQualified beanType;

        /**
         * If {@code null}, defaults to a lower camel cased form of the bean type's simple class name
         */
        @Nullable
        private final String beanName;

        /**
         * An alternating array of string property names and {@link J} property values
         */
        private final Object[] properties;

        public Scoped(J.Block<? extends J> scope, @Nullable J before,
                      String beanType, @Nullable String beanName,
                      Object... properties) {
            this.scope = scope;
            this.before = before;
            this.beanType = JavaType.Class.build(beanType);
            this.beanName = beanName;
            this.properties = properties;
            setCursoringOn();
        }

        @Override
        public boolean isIdempotent() {
            return false;
        }

        @Override
        public J.Block<J> visitBlock(J.Block<J> block) {
            J.Block<J> b = super.visitBlock(block);

            if (b.isScope(scope)) {
                List<J> statements = new ArrayList<>(b.getStatements());

                if (before == null) {
                    statements.addAll(0, buildBeanStatements());
                } else {
                    for (int i = 0; i < statements.size(); i++) {
                        J statement = statements.get(i);
                        if (statement.isScope(before)) {
                            statements.addAll(i, buildBeanStatements());
                            break;
                        }
                    }
                }

                b = b.withStatements(statements);
            }

            return b;
        }

        private List<J> buildBeanStatements() {
            String name = beanName == null ? StringUtils.uncapitalize(beanType.getClassName()) :
                    beanName;

            StringBuilder snippet = new StringBuilder(beanType.getClassName() + " " + name + " = new " +
                    beanType.getClassName() + "();\n");

            if (properties.length % 2 != 0) {
                throw new IllegalStateException("Properties must an array of key/value property pairs.");
            }

            for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i += 2) {
                snippet.append(name).append(".set").append(StringUtils.capitalize(properties[i].toString())).append("(")
                        .append(((J) properties[i + 1]).printTrimmed()).append(");\n");
            }

            List<J> beanStatements = getTreeBuilder().buildSnippet(getCursor(), snippet.toString());
            andThen(new AutoFormat(beanStatements.toArray(new J[0])));
            return beanStatements;
        }
    }
}
