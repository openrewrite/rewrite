/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.util;

import org.antlr.v4.runtime.tree.ParseTree;
import org.openrewrite.cobol.internal.ASGElement;
import org.openrewrite.cobol.internal.registry.ASGElementRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ANTLRUtils {

	public static <T extends ASGElement> Collection<T> findAncestors(final Class<? extends ASGElement> type,
			final ParseTree from, final ASGElementRegistry asgElementRegistry) {
		ParseTree currentCtx = from;

		final Collection<T> result = new ArrayList<T>();

		while (currentCtx != null) {
			final T parent = findParent(type, currentCtx, asgElementRegistry);

			if (parent != null) {
				currentCtx = parent.getCtx();
				result.add(parent);
			} else {
				currentCtx = null;
			}
		}

		return result;
	}

	public static List<ASGElement> findASGElementChildren(final ParseTree from,
														  final ASGElementRegistry asgElementRegistry) {
		return findChildren(ASGElement.class, from, asgElementRegistry);
	}

	@SuppressWarnings("unchecked")
	public static <T extends ASGElement> List<T> findChildren(final Class<? extends ASGElement> type,
			final ParseTree ctx, final ASGElementRegistry asgElementRegistry) {
		final List<ParseTree> children = findChildren(ctx);
		final List<T> result = new ArrayList<T>();

		for (final ParseTree currentChild : children) {
			final ASGElement asgElement = asgElementRegistry.getASGElement(currentChild);

			if (asgElement != null && type.isAssignableFrom(asgElement.getClass())) {
				result.add((T) asgElement);
			}
		}

		return result;
	}

	public static List<ParseTree> findChildren(final ParseTree ctx) {
		final List<ParseTree> result = new ArrayList<ParseTree>();
		final int n = ctx.getChildCount();

		for (int i = 0; i < n; i++) {
			final ParseTree currentChild = ctx.getChild(i);
			result.add(currentChild);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T extends ASGElement> T findParent(final Class<? extends ASGElement> type, final ParseTree from,
			final ASGElementRegistry asgElementRegistry) {
		T result = null;

		ParseTree currentCtx = from;

		while (result == null && currentCtx != null) {
			currentCtx = currentCtx.getParent();

			final ASGElement asgElement = asgElementRegistry.getASGElement(currentCtx);

			if (asgElement != null && type.isAssignableFrom(asgElement.getClass())) {
				result = (T) asgElement;
			}
		}

		return result;
	}

}
