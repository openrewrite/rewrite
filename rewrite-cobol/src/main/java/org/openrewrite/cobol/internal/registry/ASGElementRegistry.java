/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.registry;

import org.antlr.v4.runtime.tree.ParseTree;
import org.openrewrite.cobol.internal.ASGElement;

public interface ASGElementRegistry {

	void addASGElement(ASGElement asgElement);

	ASGElement getASGElement(ParseTree ctx);

}
