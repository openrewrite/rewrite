/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.java;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.J.MethodDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class ChangeMethodReturnTypeRecipe extends Recipe {
	
	final private Predicate<MethodDeclaration> methodCheck;
	final private String returnTypeExpression;
	final private String[] imports;

	@Override
	public String getDisplayName() {
		return "Change method return type";
	}

	@Override
	protected TreeVisitor<?, ExecutionContext> getVisitor() {
		return new JavaIsoVisitor<ExecutionContext>() {

			@Override
			public MethodDeclaration visitMethodDeclaration(MethodDeclaration method, ExecutionContext p) {
				MethodDeclaration m = super.visitMethodDeclaration(method, p);
				if (methodCheck.test(m)) {
					@Nullable
					TypeTree returnType = m.getReturnTypeExpression();

					if (returnType instanceof J.Identifier) {
						JavaType t = returnType.getType();
						maybeRemoveImport(TypeUtils.asFullyQualified(t));
					} else {
						Set<String> foundTypes = find(returnType);
						for (String removeFqName : foundTypes) {
							maybeRemoveImport(removeFqName);
						}
					}
					
					for (String i : imports) {
						maybeAddImport(i);
					}
					
					
//					return m.withReturnTypeExpression(returnType.withTemplate(tb.build(), returnType.getCoordinates().replace()));
					return m.withReturnTypeExpression(TypeTree.build(returnTypeExpression).withPrefix(Space.build(" ", new ArrayList<>())));
				}
				return m;
			}
			
		};
	}

//	private Builder template(String returnTypeExpression) {
//		return JavaTemplate.builder(() -> getVisitor().getCursor(), returnTypeExpression);
//	}

	public static Set<String> find(J j) {
        JavaIsoVisitor<Set<String>> findVisitor = new JavaIsoVisitor<Set<String>>() {

            @Override
            public <N extends NameTree> N visitTypeName(N name, Set<String> ns) {
                N n = super.visitTypeName(name, ns);
                JavaType.FullyQualified asClass = TypeUtils.asFullyQualified(n.getType());
                if (asClass != null &&
                        getCursor().firstEnclosing(J.Import.class) == null) {
                    ns.add(asClass.getFullyQualifiedName());
                }
                return n;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<String> ns) {
                J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ns);
                JavaType.FullyQualified targetClass = TypeUtils.asFullyQualified(fa.getTarget().getType());
                if (targetClass != null &&
                        fa.getName().getSimpleName().equals("class")) {
                    ns.add(targetClass.getFullyQualifiedName());
                }
                return fa;
            }
        };

        Set<String> ts = new HashSet<>();
        findVisitor.visit(j, ts);
        return ts;
    }


}
