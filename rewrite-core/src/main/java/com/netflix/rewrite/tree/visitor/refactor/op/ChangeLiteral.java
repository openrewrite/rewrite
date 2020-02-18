/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.tree.Expression;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import com.netflix.rewrite.tree.visitor.refactor.ScopedRefactorVisitor;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class ChangeLiteral extends ScopedRefactorVisitor {
    private final Function<Object, Object> transform;

    public ChangeLiteral(UUID scope, Function<Object, Object> transform) {
        super(scope);
        this.transform = transform;
    }

    @Override
    public String getRuleName() {
        return "core.ChangeLiteral";
    }

    @Override
    public List<AstTransform> visitExpression(Expression expr) {
        return nested.visit(expr);
    }

    private RefactorVisitor nested = new RefactorVisitor() {
        @Override
        public List<AstTransform> visitLiteral(Tr.Literal literal) {
            if(!isInScope(literal)) {
                return emptyList();
            }

            var transformed = transform.apply(literal.getValue());

            if(transformed == literal.getValue() || literal.getType() == null) {
                return emptyList();
            }

            String transformedSource;
            switch(literal.getType()) {
                case Boolean:
                case Byte:
                case Int:
                case Short:
                case Void:
                    transformedSource = transformed.toString();
                    break;
                case Char:
                    var escaped = StringEscapeUtils.escapeJavaScript(transformed.toString());

                    // there are two differences between javascript escaping and character escaping
                    switch(escaped) {
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
                case Wildcard:
                    transformedSource = "*";
                    break;
                case Null:
                    transformedSource = "null";
                    break;
                case None:
                default:
                    transformedSource = "";
            }

            return transform(literal, l -> l.withValue(transformed).withValueSource(transformedSource));
        }
    };
}
