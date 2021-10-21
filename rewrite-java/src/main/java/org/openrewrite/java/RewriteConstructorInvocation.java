/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.NewClass;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RewriteConstructorInvocation extends Recipe {
	
	final private Predicate<NewClass> checkConstructorInvocation;
	final private Transformer transformer;

	public RewriteConstructorInvocation(Predicate<NewClass> checkConstructorInvocation, Transformer transformer) {
		this.checkConstructorInvocation = checkConstructorInvocation;
		this.transformer = transformer;
	}

	@Override
	public String getDisplayName() {
		return "Rewrite constructor invocation";
	}
	
	@Override
	protected TreeVisitor<?, ExecutionContext> getVisitor() {
		return new JavaVisitor<ExecutionContext>() {

			
			@Override
			public J visitNewClass(NewClass newClass, ExecutionContext p) {
				NewClass n = (NewClass) super.visitNewClass(newClass, p);
				if (test(n)) {
					return transformer.transform(this, n, this::addImport);
				}
				return n;
			}
			
			private boolean test(NewClass n) {
				return checkConstructorInvocation.test(n);
			}

			private void addImport(String fqName) {
		        AddImport<ExecutionContext> op = new AddImport<>(fqName, null, false);
		        if (!getAfterVisit().contains(op)) {
		            doAfterVisit(op);
		        }
			}

		};
	}
	
	public static Predicate<NewClass> constructorMatcher(String typeFqName, String... parameterTypes) {
		return n -> {
			if (n.getConstructorType() != null 
					&& n.getConstructorType().getResolvedSignature() != null
					&& typeFqName.equals(n.getConstructorType().getDeclaringType().getFullyQualifiedName())) {
				String[] paramTypes = n.getConstructorType().getResolvedSignature().getParamTypes()
						.stream()
						.map(t -> TypeUtils.asFullyQualified(t).getFullyQualifiedName())
						.toArray(String[]::new);
				return Arrays.equals(parameterTypes, paramTypes);
			}
			return false;
		};
	}
	
	public static interface Transformer {
		
		J transform(JavaVisitor<ExecutionContext> visitor, NewClass n, Consumer<String> addImport);
		
	}



}
