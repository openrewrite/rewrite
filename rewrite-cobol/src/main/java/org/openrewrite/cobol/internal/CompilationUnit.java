/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal;

import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.cobol.internal.ASGElement;
import org.openrewrite.cobol.internal.NamedElement;
import org.openrewrite.cobol.internal.grammar.CobolParser;
import org.openrewrite.cobol.tree.Cobol;

import java.util.List;

public interface CompilationUnit extends ASGElement, NamedElement {

	Cobol.ProgramUnit addProgramUnit(CobolParser.ProgramUnitContext ctx);

	List<String> getLines();

	Cobol.ProgramUnit getProgramUnit();

	List<Cobol.ProgramUnit> getProgramUnits();

	CommonTokenStream getTokens();

	int incrementFillerCounter();

	void setLines(List<String> lines);
}
