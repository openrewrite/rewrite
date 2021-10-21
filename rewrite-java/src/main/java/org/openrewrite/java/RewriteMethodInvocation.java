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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class RewriteMethodInvocation extends Recipe {
	
	final private Predicate<MethodInvocation> checkMethodInvocation;
	final private Transformer transformer;

	@Override
	public String getDisplayName() {
		return "Rewritre method invocation";
	}
	
	@Override
	protected TreeVisitor<?, ExecutionContext> getVisitor() {
		return new JavaVisitor<ExecutionContext>() {

			@Override
			public J visitMethodInvocation(MethodInvocation method, ExecutionContext p) {
				MethodInvocation m = (MethodInvocation) super.visitMethodInvocation(method, p);
				if (checkMethodInvocation.test(m)) {
					return transformer.transform(this, m, this::addImport);
				}
				return m;
			}
			
			private void addImport(String fqName) {
		        AddImport<ExecutionContext> op = new AddImport<>(fqName, null, false);
		        if (!getAfterVisit().contains(op)) {
		            doAfterVisit(op);
		        }
			}

		};
	}

	public static Predicate<MethodInvocation> methodInvocationMatcher(String signature) {
		MethodMatcher methodMatcher = new MethodMatcher(signature);
		return mi -> methodMatcher.matches(mi);
	}
	
	public interface Transformer {
		
		J transform(JavaVisitor<ExecutionContext> visitor, MethodInvocation currentInvocation, Consumer<String> addImport);
		
	}
	
	public static RewriteMethodInvocation renameMethodInvocation(Predicate<MethodInvocation> matcher, String newName, String newType) {
		JavaType type = JavaType.buildType(newType);
		return new RewriteMethodInvocation(matcher, (v, m, a) -> {
			return m
					.withName(m.getName().withName(newName))
					.withSelect(m.getSelect().withType(type))
					.withType(m.getType().withDeclaringType(TypeUtils.asFullyQualified(type)));
		});
	}

}
