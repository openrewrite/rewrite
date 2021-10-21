/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveImplementsVisitor extends JavaIsoVisitor<ExecutionContext> {
	
	private final String[] fqNames;
	private final J.ClassDeclaration scope;
	
	public RemoveImplementsVisitor(J.ClassDeclaration scope, String[] fqNames) {
		this.scope = scope;
		this.fqNames = fqNames;
	}

	@Override
	public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
		if (scope.isScope(classDecl)) {
			
			List<TypeTree> types = classDecl.getImplements();
			List<TypeTree> newTypes = new ArrayList<>();
			if (types != null) {
				for (TypeTree tr : types) {
                    JavaType type = tr.getType();
					if (type instanceof JavaType.FullyQualified) {
						JavaType.FullyQualified fqType = (JavaType.FullyQualified) type;
                        String typeFqName = fqType.getFullyQualifiedName();
						if (Arrays.asList(fqNames).contains(typeFqName)) {
							continue;
						}
					}
					newTypes.add(tr);
				}
				
				for (String fqName : fqNames) {
					maybeRemoveImport(fqName);
				}
				
				if (newTypes.size() != types.size()) {
					if (newTypes.isEmpty()) {
						classDecl = classDecl.withImplements(null);
					} else {
						classDecl = classDecl.withImplements(newTypes);
					}
				}
			}
		}

		return classDecl;
	}
	
	

}
