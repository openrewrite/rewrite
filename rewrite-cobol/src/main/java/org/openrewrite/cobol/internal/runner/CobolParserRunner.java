/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.runner;

import org.openrewrite.cobol.internal.Program;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;

import java.io.File;
import java.io.IOException;

public interface CobolParserRunner {

	Program analyzeCode(String cobolCode, String compilationUnitName, CobolParserParams params) throws IOException;

	Program analyzeFile(File cobolFile, CobolParserParams params) throws IOException;

	Program analyzeFile(File cobolFile, CobolPreprocessor.CobolSourceFormatEnum format) throws IOException;
}
