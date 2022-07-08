/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal;

import org.openrewrite.cobol.CompilationUnit;
import org.openrewrite.cobol.internal.registry.ASGElementRegistry;
import org.openrewrite.cobol.tree.Cobol;

import java.util.List;

public interface Program extends ASGElement {

	ASGElementRegistry getASGElementRegistry();

	CompilationUnit getCompilationUnit();

	CompilationUnit getCompilationUnit(String name);

	List<CompilationUnit> getCompilationUnits();

	void registerCompilationUnit(CompilationUnit compilationUnit);
}
