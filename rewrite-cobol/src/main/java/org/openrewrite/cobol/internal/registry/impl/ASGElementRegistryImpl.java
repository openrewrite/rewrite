/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.registry.impl;

import org.antlr.v4.runtime.tree.ParseTree;
import org.openrewrite.cobol.internal.ASGElement;
import org.openrewrite.cobol.internal.registry.ASGElementRegistry;

import java.util.HashMap;
import java.util.Map;

public class ASGElementRegistryImpl implements ASGElementRegistry {

	final protected Map<ParseTree, ASGElement> asgElements = new HashMap<ParseTree, ASGElement>();

	@Override
	public void addASGElement(final ASGElement asgElement) {
		assert asgElement != null;

		final ParseTree ctx = asgElement.getCtx();

		assert ctx != null;
		assert asgElements.get(ctx) == null;

		asgElements.put(ctx, asgElement);
	}

	@Override
	public ASGElement getASGElement(final ParseTree ctx) {
		final ASGElement result = asgElements.get(ctx);
		return result;
	}
}
