/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.yaml.internal;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.yaml.tree.Yaml;

/**
 * Internal helpers for safely mutating FOLDED ({@code >}, {@code >-}, {@code >+}) and
 * LITERAL ({@code |}, {@code |-}, {@code |+}) block scalars: the {@link Yaml.Scalar#value}
 * field carries the block envelope (chomp indicator, indented body, trailing whitespace
 * bounding the next sibling), so a naïve Lombok-generated {@code withValue} replacement
 * corrupts the surrounding structure.
 *
 * <p><b>TODO:</b> promote these onto {@link Yaml.Scalar} as instance {@code getBody} /
 * {@code withBody} methods once enough downstream CLI bundles ship a {@code rewrite-yaml}
 * including them — promoting now would {@code NoSuchMethodError} customer recipes that
 * adopted the new API but load against an older bundled {@code Yaml.Scalar}.
 */
public final class BlockScalarUtils {

    private BlockScalarUtils() {
    }

    /**
     * Returns the body content of {@code scalar}, stripped of any style-specific envelope.
     * For PLAIN and quoted styles this returns {@link Yaml.Scalar#value} verbatim. For block
     * styles the body is dedented to column zero with interior line breaks normalized to
     * {@code \n} (so callers can compare or regex against it irrespective of the file's
     * CRLF/LF convention).
     */
    public static String getBody(Yaml.Scalar scalar) {
        if (!isBlockStyle(scalar)) {
            return scalar.getValue();
        }
        String value = scalar.getValue();
        int headerEnd = value.indexOf('\n');
        if (headerEnd < 0) {
            return "";
        }
        int bodyEnd = value.length();
        while (bodyEnd > headerEnd + 1 && Character.isWhitespace(value.charAt(bodyEnd - 1))) {
            bodyEnd--;
        }
        if (bodyEnd <= headerEnd + 1) {
            return "";
        }
        String bodyRegion = value.substring(headerEnd + 1, bodyEnd);
        int indent = 0;
        while (indent < bodyRegion.length() && bodyRegion.charAt(indent) == ' ') {
            indent++;
        }
        String indentStr = bodyRegion.substring(0, indent);
        String[] lines = bodyRegion.split("\r\n|\r|\n", -1);
        StringBuilder out = new StringBuilder(bodyRegion.length());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (indent > 0 && line.startsWith(indentStr)) {
                line = line.substring(indent);
            }
            if (i > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString();
    }

    /** {@link #withBody(Yaml.Scalar, String, int)} with a 2-space empty-body indent fallback. */
    public static Yaml.Scalar withBody(Yaml.Scalar scalar, String newBody) {
        return withBody(scalar, newBody, 2);
    }

    /**
     * Returns a copy of {@code scalar} with its body replaced by {@code newBody}. For PLAIN
     * and quoted styles this just sets {@link Yaml.Scalar#value} (via the Lombok-generated
     * {@code withValue}); for block styles the chomp indicator, header newline, body indent,
     * and trailing whitespace are preserved, and each line of {@code newBody} is emitted in
     * the existing value's line-ending convention. {@code defaultIndentSpaces} is used as the
     * body indent width when the existing block scalar has an empty body — pass an
     * {@code IndentsStyle#getIndentSize()} to honor the document's configured indent.
     */
    public static Yaml.Scalar withBody(Yaml.Scalar scalar, String newBody, int defaultIndentSpaces) {
        if (!isBlockStyle(scalar)) {
            return scalar.withValue(newBody);
        }
        String value = scalar.getValue();
        int headerEnd = value.indexOf('\n');
        String header = headerEnd < 0 ? value : value.substring(0, headerEnd + 1);
        String newLine = (headerEnd > 0 && value.charAt(headerEnd - 1) == '\r') ? "\r\n" : "\n";
        int bodyEnd = value.length();
        while (bodyEnd > 0 && Character.isWhitespace(value.charAt(bodyEnd - 1))) {
            bodyEnd--;
        }
        String indent;
        if (headerEnd >= 0 && headerEnd + 1 < bodyEnd) {
            int indentEnd = headerEnd + 1;
            while (indentEnd < bodyEnd && value.charAt(indentEnd) == ' ') {
                indentEnd++;
            }
            indent = value.substring(headerEnd + 1, indentEnd);
            if (indent.isEmpty()) {
                indent = StringUtils.repeat(" ", defaultIndentSpaces);
            }
        } else {
            indent = StringUtils.repeat(" ", defaultIndentSpaces);
        }
        String trailing = value.substring(bodyEnd);
        String[] lines = newBody.split("\r\n|\r|\n", -1);
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                body.append(newLine);
            }
            if (!lines[i].isEmpty()) {
                body.append(indent).append(lines[i]);
            }
        }
        return scalar.withValue(header + body + trailing);
    }

    private static boolean isBlockStyle(Yaml.Scalar scalar) {
        return scalar.getStyle() == Yaml.Scalar.Style.FOLDED ||
                scalar.getStyle() == Yaml.Scalar.Style.LITERAL;
    }
}
