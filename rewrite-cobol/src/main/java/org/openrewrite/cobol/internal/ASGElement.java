/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal;

import org.antlr.v4.codegen.model.ModelElement;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
 * an element of the ASG that corresponds to an element in the AST.
 */
public interface ASGElement extends ModelElement {

	List<ASGElement> getChildren();

	ParserRuleContext getCtx();

	ASGElement getParent();

	Program getProgram();
}
