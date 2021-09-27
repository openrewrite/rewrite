/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.cleanup.ModifierOrder.sortModifiers;
import static org.openrewrite.java.tree.J.ClassDeclaration.Kind.Type.Interface;

public class FinalClassVisitor extends JavaIsoVisitor<ExecutionContext> {
    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext executionContext) {
        if(classDeclaration.getKind() == Interface || classDeclaration.hasModifier(J.Modifier.Type.Abstract)) {
            return classDeclaration;
        }
        J.ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, executionContext);

        if(cd.hasModifier(J.Modifier.Type.Final) || cd.getKind() != J.ClassDeclaration.Kind.Type.Class) {
            return cd;
        }

        boolean allPrivate = true;
        int constructorCount = 0;
        for(Statement s : cd.getBody().getStatements()) {
            if(s instanceof J.MethodDeclaration && ((J.MethodDeclaration)s).isConstructor()) {
                J.MethodDeclaration constructor = (J.MethodDeclaration)s;
                constructorCount++;
                if(!constructor.hasModifier(J.Modifier.Type.Private)) {
                    allPrivate = false;
                }
            }
            if(constructorCount > 0 && !allPrivate) {
                return cd;
            }
        }

        if(constructorCount > 0) {
            List<J.Modifier> modifiers = new ArrayList<>(cd.getModifiers());
            modifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY,  J.Modifier.Type.Final, emptyList()));
            modifiers = sortModifiers(modifiers);
            cd = cd.withModifiers(modifiers);

            assert getCursor().getParent() != null;
            cd = autoFormat(cd, cd.getName(), executionContext, getCursor().getParent());
        }
        return cd;
    }
}
