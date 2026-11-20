/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of yarn src/reporters/base-reporter.js {@code lang} and {@code stringifyLangArgs} (yarn 1.22.22): a message of
 * reporters/lang/en.js with its {@code $0}, {@code $1}, ... filled by each argument's {@code JSON.stringify}, whose
 * escapes of an ESC and of a line break are taken back. {@code os.EOL} is POSIX's, as the paths the port's messages
 * name are.
 */
final class Lang {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$(\\d+)");

    // an escaped ESC that an even number of backslashes leads: the escape's own backslash is the odd one
    private static final Pattern ESCAPED_ESC = Pattern.compile("((?:^|[^\\\\])(?:\\\\{2})*)\\\\u001[bB]");

    private static final Pattern ESCAPED_LINE_BREAK = Pattern.compile("[\\\\]r[\\\\]n|[\\\\]n");

    // 1.7.0's: the group holds a backslash that leads the escape's
    private static final Pattern ESCAPED_LINE_BREAK_UNLESS_LED = Pattern.compile("[\\\\]r[\\\\]n|([\\\\])?[\\\\]n");

    private static final String EOL = "\n";

    private Lang() {
    }

    /**
     * {@code lang}'s {@code msg.replace(/\$(\d+)/g, ...)}: one pass; digits that are no index of the arguments (past
     * them, or led by a zero) give {@code undefined}.
     */
    static String format(YarnClassicRelease release, String message, @Nullable Object... args) {
        Matcher placeholder = PLACEHOLDER.matcher(message);
        StringBuffer out = new StringBuffer();
        while (placeholder.find()) {
            String digits = placeholder.group(1);
            boolean index = digits.length() <= 9 && (digits.length() == 1 || digits.charAt(0) != '0');
            int i = index ? Integer.parseInt(digits) : -1;
            String arg = i >= 0 && i < args.length ? stringify(args[i], release) : "undefined";
            placeholder.appendReplacement(out, Matcher.quoteReplacement(arg));
        }
        placeholder.appendTail(out);
        return out.toString();
    }

    // JSON.stringify(val) || val + '', then the escapes taken back
    private static String stringify(@Nullable Object val, YarnClassicRelease release) {
        String json = val == null ? null : Js.jsonStringify(val);
        String str = json == null ? Js.string(val) : json;
        str = ESCAPED_ESC.matcher(str).replaceAll("$1\u001b");
        if (!release.breaksTheLinesOfAnArgument()) {
            return str;
        }
        if (!release.keepsAnEscapedBackslashBeforeAnN()) {
            return ESCAPED_LINE_BREAK.matcher(str).replaceAll(EOL);
        }
        Matcher lineBreak = ESCAPED_LINE_BREAK_UNLESS_LED.matcher(str);
        StringBuffer out = new StringBuffer();
        while (lineBreak.find()) {
            lineBreak.appendReplacement(out, Matcher.quoteReplacement(lineBreak.group(1) != null ?
                    lineBreak.group() : EOL));
        }
        lineBreak.appendTail(out);
        return out.toString();
    }
}
