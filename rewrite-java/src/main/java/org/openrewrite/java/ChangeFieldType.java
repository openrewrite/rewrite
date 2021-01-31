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

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

public class ChangeFieldType<P> extends JavaIsoVisitor<P> {
    private final JavaType.Class type;
    private final String targetType;

    public ChangeFieldType(JavaType.Class type, String targetType) {
        this.type = type;
        this.targetType = targetType;
    }

    @Override
    public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable, P p) {
        JavaType.Class typeAsClass = multiVariable.getTypeAsClass();
        J.VariableDecls mv = super.visitMultiVariable(multiVariable, p);
        if (typeAsClass != null && typeAsClass.equals(type)) {
            JavaType.Class type = JavaType.Class.build(targetType);

            maybeAddImport(targetType);
            maybeRemoveImport(typeAsClass);

            mv = mv.withTypeExpr(mv.getTypeExpr() == null ?
                    null :
                    J.Ident.build(mv.getTypeExpr().getId(),
                            mv.getTypeExpr().getPrefix(),
                            Markers.EMPTY,
                            type.getClassName(),
                            type)
            );

            mv = mv.withVars(ListUtils.map(mv.getVars(), var -> {
                JavaType.Class varType = TypeUtils.asClass(var.getType());
                if (varType != null && !varType.equals(type)) {
                    return var.withType(type).withName(var.getName().withType(type));
                }
                return var;
            }));
        }

        return mv;
    }
}
