/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.exception;

public class CobolPreprocessorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CobolPreprocessorException(final String message) {
        super(message);
    }
}
