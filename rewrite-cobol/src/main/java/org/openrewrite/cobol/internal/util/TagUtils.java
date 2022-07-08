/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.util;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.cobol.internal.preprocessor.CobolPreprocessor;

import java.util.List;

public class TagUtils {

	public static String getUntaggedText(final List<TerminalNode> terminalNodes, final String... tags) {
		final StringBuffer sb = new StringBuffer();

		for (final TerminalNode terminalNode : terminalNodes) {
			final String rawLineText = terminalNode.getText();
			String untaggedText = rawLineText;

			for (final String tag : tags) {
				untaggedText = untaggedText.replace(tag, "");
			}

			final String execLineTextCleaned = untaggedText.trim() + CobolPreprocessor.WS;

			sb.append(execLineTextCleaned);
		}

		return sb.toString().trim();
	}

}
