/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.line.writer;

import java.util.List;

import org.openrewrite.cobol.internal.StringWithOriginalPositions;
import org.openrewrite.cobol.internal.preprocessor.sub.CobolLine;

public interface CobolLineWriter {

    StringWithOriginalPositions serialize(String originalCode, List<CobolLine> lines);
}
