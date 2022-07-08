/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor.sub.document.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.BufferedTokenStream;

import org.openrewrite.cobol.internal.grammar.CobolPreprocessorParser;
import org.openrewrite.cobol.internal.preprocessor.sub.util.TokenUtils;

/**
 * A mapping from a replaceable to a replacement.
 */
public class CobolReplacementMapping implements Comparable<CobolReplacementMapping> {

	public CobolPreprocessorParser.ReplaceableContext replaceable;

	public CobolPreprocessorParser.ReplacementContext replacement;

	@Override
	public int compareTo(final CobolReplacementMapping o) {
		return o.replaceable.getText().length() - replaceable.getText().length();
	}

	private String extractPseudoText(final CobolPreprocessorParser.PseudoTextContext pseudoTextCtx, final BufferedTokenStream tokens) {
		final String pseudoText = TokenUtils.getTextIncludingHiddenTokens(pseudoTextCtx, tokens).trim();
		final String content = pseudoText.replaceAll("^==", "").replaceAll("==$", "").trim();
		return content;
	}

	/**
	 * Whitespace in Cobol replaceables matches line breaks. Hence, the replaceable
	 * search string has to be enhanced to a regex, which is returned by this
	 * function.
	 */
	private String getRegexFromReplaceable(final String replaceable) {
		final String result;

		if (replaceable == null) {
			result = null;
		} else {
			final String[] parts = replaceable.split("\\s+");
			final String[] regexParts = new String[parts.length];
			final String regexSeparator = "[\\r\\n\\s]+";

			for (int i = 0; i < parts.length; i++) {
				final String part = parts[i];
				regexParts[i] = Pattern.quote(part);
			}

			result = String.join(regexSeparator, regexParts);
		}

		return result;
	}

	private String getText(final CobolPreprocessorParser.ReplaceableContext ctx, final BufferedTokenStream tokens) {
		final String result;

		if (ctx.pseudoText() != null) {
			result = extractPseudoText(ctx.pseudoText(), tokens);
		} else if (ctx.charDataLine() != null) {
			result = TokenUtils.getTextIncludingHiddenTokens(ctx, tokens);
		} else if (ctx.cobolWord() != null) {
			result = ctx.getText();
		} else if (ctx.literal() != null) {
			result = ctx.literal().getText();
		} else {
			result = null;
		}

		return result;
	}

	private String getText(final CobolPreprocessorParser.ReplacementContext ctx, final BufferedTokenStream tokens) {
		final String result;

		if (ctx.pseudoText() != null) {
			result = extractPseudoText(ctx.pseudoText(), tokens);
		} else if (ctx.charDataLine() != null) {
			result = TokenUtils.getTextIncludingHiddenTokens(ctx, tokens);
		} else if (ctx.cobolWord() != null) {
			result = ctx.getText();
		} else if (ctx.literal() != null) {
			result = ctx.literal().getText();
		} else {
			result = null;
		}

		return result;
	}

	protected String replace(final String string, final BufferedTokenStream tokens) {
		final String replaceableString = getText(replaceable, tokens);
		final String replacementString = getText(replacement, tokens);

		final String result;

		if (replaceableString != null && replacementString != null) {
			// regex for the replaceable
			final String replaceableRegex = getRegexFromReplaceable(replaceableString);

			// regex for the replacement
			final String quotedReplacementRegex = Matcher.quoteReplacement(replacementString);

			// result = Pattern.compile(replaceableRegex).matcher(string).replaceAll(quotedReplacementRegex);
			result = replaceAll(Pattern.compile(replaceableRegex), string, quotedReplacementRegex);
		} else {
			result = string;
		}

		return result;
	}

	// http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/regex/Matcher.java
	static String replaceAll(Pattern p, String text, String replacement) {
		// XXX This is where the fun happens
		Matcher m = p.matcher(text);
        m.reset();
        boolean result = m.find();
        if (result) {
            StringBuffer sb = new StringBuffer();
            do {
                m.appendReplacement(sb, replacement);
                result = m.find();
            } while (result);
            m.appendTail(sb);
            return sb.toString();
        }
        return text;
    }
	@Override
	public String toString() {
		return replaceable.getText() + " -> " + replacement.getText();
	}
}
