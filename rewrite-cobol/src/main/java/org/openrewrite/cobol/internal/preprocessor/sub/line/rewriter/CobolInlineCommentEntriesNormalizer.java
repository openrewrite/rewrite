/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.line.rewriter;

import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;

/**
 * Preprocessor, which normalizes inline comment entries.
 */
public interface CobolInlineCommentEntriesNormalizer extends CobolLineRewriter {

    CobolLine processLine(CobolLine line);
}
