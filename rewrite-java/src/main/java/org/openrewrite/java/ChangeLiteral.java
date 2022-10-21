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

import org.apache.commons.text.StringEscapeUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.function.Function;

public class ChangeLiteral<P> extends JavaIsoVisitor<P> {
    private final Expression scope;
    private final Function<Object, Object> transform;

    /**
     * @param scope     And expression containing a literal, including a binary expression like String concatenation, where
     *                  you want to transform the String literals participating in the concatenation.
     * @param transform The transformation to apply to each literal found in the expression scope.
     */
    public ChangeLiteral(Expression scope, Function<Object, Object> transform) {
        this.scope = scope;
        this.transform = transform;
    }

    @Override
    public J.Literal visitLiteral(J.Literal literal, P p) {
        if (getCursor().isScopeInPath(scope)) {
            Object transformed = transform.apply(literal.getValue());

            if (transformed.equals(literal.getValue()) || literal.getType() == null) {
                return literal;
            }

            String transformedSource;
            switch (literal.getType()) {
                case Boolean:
                case Byte:
                case Int:
                case Short:
                case Void:
                    transformedSource = transformed.toString();
                    break;
                case Char:
                    String escaped = StringEscapeUtils.escapeEcmaScript(transformed.toString());

                    // there are two differences between javascript escaping and character escaping
                    switch (escaped) {
                        case "\\\"":
                            transformedSource = "'\"'";
                            break;
                        case "\\/":
                            transformedSource = "'/'";
                            break;
                        default:
                            transformedSource = "'" + escaped + "'";
                    }
                    break;
                case Double:
                    transformedSource = transformed.toString() + "d";
                    break;
                case Float:
                    transformedSource = transformed.toString() + "f";
                    break;
                case Long:
                    transformedSource = transformed.toString() + "L";
                    break;
                case String:
                    transformedSource = "\"" + StringEscapeUtils.escapeJava(transformed.toString()) + "\"";
                    break;
                case Null:
                    transformedSource = "null";
                    break;
                case None:
                default:
                    transformedSource = "";
            }

            return literal.withValue(transformed).withValueSource(transformedSource);
        }

        return literal;
    }
}
