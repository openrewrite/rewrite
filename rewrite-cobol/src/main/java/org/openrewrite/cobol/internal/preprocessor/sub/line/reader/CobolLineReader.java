/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.line.reader;

import java.util.List;

import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;

/**
 * Preprocessor, which analyzes and processes line indicators.
 */
public interface CobolLineReader {

    CobolLine parseLine(String line, int lineNumber, CobolParserParams params);

    List<CobolLine> processLines(String lines, CobolParserParams params);
}
