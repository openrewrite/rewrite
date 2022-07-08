/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.preprocessor;

import org.openrewrite.cobol.internal.StringWithOriginalPositions;
import org.openrewrite.cobol.internal.params.CobolParserParams;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public interface CobolPreprocessor {

    public enum CobolSourceFormatEnum {

        /**
         * Fixed format, standard ANSI / IBM reference. Each line 80 chars.<br />
         * <br />
         * 1-6: sequence area<br />
         * 7: indicator field<br />
         * 8-12: area A<br />
         * 13-72: area B<br />
         * 73-80: comments<br />
         */
        FIXED("(.{0,6})(?:" + INDICATOR_FIELD + "(.{0,4})(.{0,61})(.*))?", true),

        /**
         * HP Tandem format.<br />
         * <br />
         * 1: indicator field<br />
         * 2-5: optional area A<br />
         * 6-132: optional area B<br />
         */
        TANDEM("()(?:" + INDICATOR_FIELD + "(.{0,4})(.*)())?", false),

        /**
         * Variable format.<br />
         * <br />
         * 1-6: sequence area<br />
         * 7: indicator field<br />
         * 8-12: optional area A<br />
         * 13-*: optional area B<br />
         */
        VARIABLE("(.{0,6})(?:" + INDICATOR_FIELD + "(.{0,4})(.*)())?", true);

        private final boolean commentEntryMultiLine;

        private final Pattern pattern;

        private final String regex;

        CobolSourceFormatEnum(final String regex, final boolean commentEntryMultiLine) {
            this.regex = regex;
            pattern = Pattern.compile(regex);
            this.commentEntryMultiLine = commentEntryMultiLine;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String getRegex() {
            return regex;
        }

        public boolean isCommentEntryMultiLine() {
            return commentEntryMultiLine;
        }
    }

    final static String CHAR_ASTERISK = "*";

    final static String CHAR_D = "D";

    final static String CHAR_D_ = "d";

    final static String CHAR_DOLLAR_SIGN = "$";

    final static String CHAR_MINUS = "-";

    final static String CHAR_SLASH = "/";

    final static String COMMENT_ENTRY_TAG = "*>CE";

    final static String COMMENT_TAG = "*>";

    final static String EXEC_CICS_TAG = "*>EXECCICS";

    final static String EXEC_END_TAG = "}";

    final static String EXEC_SQL_TAG = "*>EXECSQL";

    final static String EXEC_SQLIMS_TAG = "*>EXECSQLIMS";

    final static String INDICATOR_FIELD = "([ABCdD$\\t\\-/*# ])";

    final static String NEWLINE = "\n";

    final static String WS = " ";

    StringWithOriginalPositions processWithOriginalPositions(File cobolFile, CobolParserParams params) throws IOException;

    default String process(File cobolFile, CobolParserParams params) throws IOException {
        return processWithOriginalPositions(cobolFile, params).preprocessedText;
    }

    StringWithOriginalPositions processWithOriginalPositions(String cobolCode, CobolParserParams params);

    default String process(String cobolCode, CobolParserParams params) throws IOException {
        return processWithOriginalPositions(cobolCode, params).preprocessedText;
    }
}
