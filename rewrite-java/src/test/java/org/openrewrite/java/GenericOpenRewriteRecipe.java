/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Fabian Krüger
 */
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class GenericOpenRewriteRecipe<V extends TreeVisitor<?, ExecutionContext>> extends Recipe {

    private final V visitor;

    public GenericOpenRewriteRecipe(V visitor) {
        this.visitor = visitor;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return visitor;
    }

	@Override
	public String getDisplayName() {
		return visitor!=null ? visitor.getClass().getSimpleName() : "???";
	}
}
