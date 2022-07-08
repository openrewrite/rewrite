/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.copybook.impl;

import java.io.File;

import org.openrewrite.cobol.internal.grammar.CobolPreprocessorParser;
import org.openrewrite.cobol.internal.params.CobolParserParams;
import org.openrewrite.cobol.internal.preprocessor.sub.copybook.CobolWordCopyBookFinder;
import org.openrewrite.cobol.internal.util.FilenameUtils;

public class CobolWordCopyBookFinderImpl implements CobolWordCopyBookFinder {

    @Override
    public File findCopyBook(final CobolParserParams params, final CobolPreprocessorParser.CobolWordContext ctx) {
        if (params.getCopyBookFiles() != null) {
            for (final File copyBookFile : params.getCopyBookFiles()) {
                if (isMatchingCopyBook(copyBookFile, params, ctx)) {
                    return copyBookFile;
                }
            }
        }

        if (params.getCopyBookDirectories() != null) {
            for (final File copyBookDirectory : params.getCopyBookDirectories()) {
                final File validCopyBook = findCopyBookInDirectory(copyBookDirectory, params, ctx);

                if (validCopyBook != null) {
                    return validCopyBook;
                }
            }
        }

        return null;
    }

    protected File findCopyBookInDirectory(final File copyBooksDirectory, final CobolParserParams params,
                                           final CobolPreprocessorParser.CobolWordContext ctx) {
        for (final File copyBookCandidate : copyBooksDirectory.listFiles()) {
            if (isMatchingCopyBook(copyBookCandidate, params, ctx)) {
                return copyBookCandidate;
            }
        }

        return null;
    }

    protected boolean isMatchingCopyBook(final File copyBookCandidate, final CobolParserParams params,
                                         final CobolPreprocessorParser.CobolWordContext ctx) {
        final String copyBookIdentifier = ctx.getText();

        if (params.getCopyBookExtensions() != null) {
            for (final String copyBookExtension : params.getCopyBookExtensions()) {
                if (isMatchingCopyBookWithExtension(copyBookCandidate, copyBookIdentifier, copyBookExtension)) {
                    return true;
                }
            }

            return false;
        } else {
            return isMatchingCopyBookWithoutExtension(copyBookCandidate, copyBookIdentifier);
        }
    }

    protected boolean isMatchingCopyBookWithExtension(final File copyBookCandidate, final String copyBookIdentifier,
                                                      final String copyBookExtension) {
        final String copyBookFilename = copyBookExtension == null || copyBookExtension.isEmpty() ? copyBookIdentifier
                : copyBookIdentifier + "." + copyBookExtension;
        final String copyBookCandidateName = copyBookCandidate.getName();
        final boolean result = copyBookFilename.equalsIgnoreCase(copyBookCandidateName);
        return result;
    }

    protected boolean isMatchingCopyBookWithoutExtension(final File copyBookCandidate,
                                                         final String copyBookIdentifier) {
        final String copyBookCandidateBaseName = FilenameUtils.getBaseName(copyBookCandidate.getName());
        final boolean result = copyBookCandidateBaseName.equalsIgnoreCase(copyBookIdentifier);
        return result;
    }
}
