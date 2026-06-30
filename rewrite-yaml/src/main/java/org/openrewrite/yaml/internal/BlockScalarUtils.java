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
 * LITERAL ({@code |}, {@code |-}, {@code |+}) block scalars in the YAML LST.
 *
 * <p>The trap this exists to avoid: for block scalars, {@link Yaml.Scalar#getValue()}
 * carries three concatenated things — the chomp/indent indicator(s), the indented body,
 * AND the trailing whitespace that bounds the block from the next sibling mapping entry.
 * Naïvely replacing {@code value} via {@link Yaml.Scalar#withValue(String)} clobbers the
 * block envelope, drops the chomp indicator, and lets the printer glue the next sibling
 * key onto the same line.
 *
 * <p>Use {@link #getBody(Yaml.Scalar)} and {@link #withBody(Yaml.Scalar, String)} when a
 * recipe needs to set or transform a property's value and the existing scalar might be a
 * block scalar.
 *
 * <p><b>TODO:</b> these helpers should eventually move onto {@link Yaml.Scalar} itself as
 * instance methods ({@code getBody()}, {@code withBody(String)}) so they're discoverable
 * to recipe authors. Promoting them is deliberately deferred to avoid a forwards-compatibility
 * trap: built-in and customer recipes compiled against new {@code Yaml.Scalar} methods would
 * {@code NoSuchMethodError} when loaded under Moderne CLI versions that bundle an older
 * {@code rewrite-yaml}. Once a long-enough adoption window has passed for those CLI bundles
 * to roll forward, inline this logic into {@code Yaml.Scalar} and have consumers call the
 * instance methods directly.
 */
public final class BlockScalarUtils {

    private BlockScalarUtils() {
    }

    /**
     * Returns the body content of {@code scalar}, stripped of any style-specific envelope.
     *
     * <p>For PLAIN and quoted scalars this is identical to {@link Yaml.Scalar#getValue()}.
     * For FOLDED and LITERAL scalars this is the body dedented to column zero, joined with
     * single {@code \n}s, with the chomp indicator, header newline, indent, and trailing
     * whitespace stripped.
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
        if (indent == 0) {
            return bodyRegion;
        }
        String indentStr = bodyRegion.substring(0, indent);
        String[] lines = bodyRegion.split("\n", -1);
        StringBuilder out = new StringBuilder(bodyRegion.length());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith(indentStr)) {
                line = line.substring(indent);
            }
            if (i > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString();
    }

    /**
     * Returns a copy of {@code scalar} with its body replaced by {@code newBody}, defaulting
     * the empty-body indent fallback to 2 spaces.
     */
    public static Yaml.Scalar withBody(Yaml.Scalar scalar, String newBody) {
        return withBody(scalar, newBody, 2);
    }

    /**
     * Returns a copy of {@code scalar} with its body replaced by {@code newBody}.
     *
     * <p>For PLAIN and quoted styles this is equivalent to {@link Yaml.Scalar#withValue(String)}.
     * For FOLDED and LITERAL scalars the block envelope is preserved: the chomp indicator,
     * header newline, and trailing whitespace that bounds the block from the next sibling are
     * kept intact, and each line of {@code newBody} is re-indented to the block body's column.
     *
     * <p>If the original block scalar had an empty body and the body indent cannot be
     * recovered from the existing value, {@code defaultIndentSpaces} is used as the indent
     * width. Recipes that honor the document's configured {@code IndentsStyle} should pass
     * its {@code getIndentSize()}; {@code 2} matches the YAML convention default.
     */
    public static Yaml.Scalar withBody(Yaml.Scalar scalar, String newBody, int defaultIndentSpaces) {
        if (!isBlockStyle(scalar)) {
            return scalar.withValue(newBody);
        }
        String value = scalar.getValue();
        int headerEnd = value.indexOf('\n');
        String header = headerEnd < 0 ? value : value.substring(0, headerEnd + 1);
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
        if (trailing.isEmpty()) {
            trailing = "\n";
        }
        String[] lines = newBody.split("\n", -1);
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                body.append('\n');
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
