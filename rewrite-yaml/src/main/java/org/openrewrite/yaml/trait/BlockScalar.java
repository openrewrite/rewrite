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
package org.openrewrite.yaml.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.yaml.tree.Yaml;

/**
 * Body-aware access to a FOLDED / LITERAL {@link Yaml.Scalar}: the raw {@link Yaml.Scalar#value}
 * carries the block envelope (chomp indicator, header newline, indented body, trailing whitespace
 * bounding the next sibling), so rewriting it directly via the Lombok-generated {@code withValue}
 * clobbers the envelope.
 */
@Value
public class BlockScalar implements Trait<Yaml.Scalar> {
    Cursor cursor;

    public String getBody() {
        String value = getTree().getValue();
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

    public Yaml.Scalar withBody(String newBody) {
        return withBody(newBody, 2);
    }

    public Yaml.Scalar withBody(String newBody, int defaultIndentSpaces) {
        Yaml.Scalar scalar = getTree();
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

    public static class Matcher extends SimpleTraitMatcher<BlockScalar> {
        @Override
        protected @Nullable BlockScalar test(Cursor cursor) {
            if (cursor.getValue() instanceof Yaml.Scalar) {
                Yaml.Scalar.Style style = ((Yaml.Scalar) cursor.getValue()).getStyle();
                if (style == Yaml.Scalar.Style.FOLDED || style == Yaml.Scalar.Style.LITERAL) {
                    return new BlockScalar(cursor);
                }
            }
            return null;
        }
    }
}
