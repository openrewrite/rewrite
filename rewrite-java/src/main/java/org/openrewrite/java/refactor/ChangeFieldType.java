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
package org.openrewrite.java.refactor;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.stream.Collectors.toList;

public class ChangeFieldType extends JavaRefactorVisitor {
    private final J.VariableDecls scope;
    private final String targetType;

    public ChangeFieldType(J.VariableDecls scope, String targetType) {
        this.scope = scope;
        this.targetType = targetType;
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("to", targetType);
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable) {
        JavaType.Class originalType = multiVariable.getTypeAsClass();
        J.VariableDecls mv = refactor(multiVariable, super::visitMultiVariable);
        if (scope.isScope(multiVariable) && originalType != null && !originalType.getFullyQualifiedName().equals(targetType)) {
            JavaType.Class type = JavaType.Class.build(targetType);

            maybeAddImport(targetType);
            maybeRemoveImport(originalType);

            mv = mv.withTypeExpr(mv.getTypeExpr() == null ? null : J.Ident.build(mv.getTypeExpr().getId(),
                    type.getClassName(), type, mv.getTypeExpr().getFormatting()))
                    .withVars(mv.getVars().stream().map(var -> {
                        JavaType.Class varType = TypeUtils.asClass(var.getType());
                        if (varType != null && !varType.equals(type)) {
                            return var.withType(type).withName(var.getName().withType(type));
                        }
                        return var;
                    }).collect(toList()));
        }
        return mv;
    }
}
