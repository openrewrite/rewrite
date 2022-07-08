/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.document.impl;

import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;

import org.openrewrite.cobol.internal.grammar.CobolPreprocessorParser;

/**
 * A replacement context that defines, which replaceables should be replaced by
 * which replacements.
 */
public class CobolDocumentContext {

    private CobolReplacementMapping[] currentReplaceableReplacements;

    private StringBuffer outputBuffer = new StringBuffer();

    public String read() {
        return outputBuffer.toString();
    }

    /**
     * Replaces replaceables with replacements (COPY ... REPLACING ...).
     */
    public void replaceReplaceablesByReplacements(final BufferedTokenStream tokens) {
        if (currentReplaceableReplacements != null) {
            Arrays.sort(currentReplaceableReplacements);

            for (final CobolReplacementMapping replaceableReplacement : currentReplaceableReplacements) {
                final String currentOutput = outputBuffer.toString();
                final String replacedOutput = replaceableReplacement.replace(currentOutput, tokens);

                outputBuffer = new StringBuffer();
                outputBuffer.append(replacedOutput);
            }
        }
    }

    public void storeReplaceablesAndReplacements(final List<CobolPreprocessorParser.ReplaceClauseContext> replaceClauses) {
        if (replaceClauses == null) {
            currentReplaceableReplacements = null;
        } else {
            final int length = replaceClauses.size();
            currentReplaceableReplacements = new CobolReplacementMapping[length];

            int i = 0;

            for (final CobolPreprocessorParser.ReplaceClauseContext replaceClause : replaceClauses) {
                final CobolReplacementMapping replaceableReplacement = new CobolReplacementMapping();

                replaceableReplacement.replaceable = replaceClause.replaceable();
                replaceableReplacement.replacement = replaceClause.replacement();

                currentReplaceableReplacements[i] = replaceableReplacement;
                i++;
            }
        }
    }

    public void write(final String text) {
        outputBuffer.append(text);
    }
}
