/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.copybook;

import java.io.File;

import org.openrewrite.cobol.internal.grammar.CobolPreprocessorParser;
import org.openrewrite.cobol.internal.params.CobolParserParams;

public interface LiteralCopyBookFinder {

    File findCopyBook(CobolParserParams params, CobolPreprocessorParser.LiteralContext ctx);
}
