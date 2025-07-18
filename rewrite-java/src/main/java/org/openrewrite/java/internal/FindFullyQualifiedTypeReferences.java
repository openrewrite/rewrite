/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds fully qualified type references in the AST that may not be captured in TypesInUse
 * when the types are not on the classpath (and thus marked as Unknown).
 */
public class FindFullyQualifiedTypeReferences extends JavaIsoVisitor<Integer> {
    private final Set<String> fullyQualifiedReferences = new HashSet<>();
    
    public static Set<String> find(JavaSourceFile cu) {
        FindFullyQualifiedTypeReferences visitor = new FindFullyQualifiedTypeReferences();
        visitor.visit(cu, 0);
        return visitor.fullyQualifiedReferences;
    }
    
    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer p) {
        // Check if this is a fully qualified type reference (e.g., com.example.MyClass)
        if (couldBeFullyQualifiedTypeReference(fieldAccess)) {
            String fqn = extractFullyQualifiedName(fieldAccess);
            if (fqn.contains(".") && Character.isUpperCase(fieldAccess.getSimpleName().charAt(0))) {
                // Likely a type reference - class names typically start with uppercase
                fullyQualifiedReferences.add(fqn);
            }
        }
        return super.visitFieldAccess(fieldAccess, p);
    }
    
    private boolean couldBeFullyQualifiedTypeReference(J.FieldAccess fieldAccess) {
        // Check if we're in a context where a type is expected
        Object parent = getCursor().getParent() != null ? getCursor().getParent().getValue() : null;
        return parent instanceof J.VariableDeclarations ||
               parent instanceof J.ParameterizedType ||
               parent instanceof J.NewClass ||
               parent instanceof J.TypeCast ||
               parent instanceof J.InstanceOf ||
               parent instanceof J.ClassDeclaration ||
               parent instanceof J.MethodDeclaration;
    }
    
    private String extractFullyQualifiedName(J.FieldAccess fieldAccess) {
        StringBuilder fqn = new StringBuilder(fieldAccess.getSimpleName());
        Expression target = fieldAccess.getTarget();
        while (target instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) target;
            fqn.insert(0, fa.getSimpleName() + ".");
            target = fa.getTarget();
        }
        if (target instanceof J.Identifier) {
            fqn.insert(0, ((J.Identifier) target).getSimpleName() + ".");
        }
        return fqn.toString();
    }
}