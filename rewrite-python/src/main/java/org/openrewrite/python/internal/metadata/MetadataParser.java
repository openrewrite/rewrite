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
package org.openrewrite.python.internal.metadata;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RFC 5322-style parser for Python core metadata (METADATA / PKG-INFO). Parsing stops at the
 * first blank line; the body (package description) is ignored.
 */
public class MetadataParser {

    private MetadataParser() {
    }

    public static CoreMetadata parse(byte[] utf8) {
        String metadataVersion = "";
        String name = "";
        String version = "";
        String requiresPython = null;
        List<String> requiresDist = new ArrayList<>();
        List<String> providesExtra = new ArrayList<>();
        List<String> dynamic = new ArrayList<>();

        for (String[] header : headers(new String(utf8, StandardCharsets.UTF_8))) {
            String field = header[0];
            String value = header[1];
            switch (field) {
                case "metadata-version":
                    metadataVersion = value;
                    break;
                case "name":
                    name = value;
                    break;
                case "version":
                    version = value;
                    break;
                case "requires-dist":
                    requiresDist.add(value);
                    break;
                case "requires-python":
                    requiresPython = value;
                    break;
                case "provides-extra":
                    providesExtra.add(value);
                    break;
                case "dynamic":
                    for (String part : value.split(",")) {
                        if (!part.trim().isEmpty()) {
                            dynamic.add(part.trim().toLowerCase(Locale.ROOT));
                        }
                    }
                    break;
            }
        }
        return new CoreMetadata(metadataVersion, name, version, requiresDist, requiresPython, providesExtra, dynamic);
    }

    private static List<String[]> headers(String content) {
        List<String[]> headers = new ArrayList<>();
        String currentField = null;
        StringBuilder currentValue = null;
        for (String line : content.split("\n", -1)) {
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (line.isEmpty()) {
                break;
            }
            if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                if (currentValue != null) {
                    currentValue.append(' ').append(line.trim());
                }
                continue;
            }
            flush(headers, currentField, currentValue);
            currentField = null;
            currentValue = null;
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            currentField = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            currentValue = new StringBuilder(line.substring(colon + 1).trim());
        }
        flush(headers, currentField, currentValue);
        return headers;
    }

    private static void flush(List<String[]> headers, @Nullable String field, @Nullable StringBuilder value) {
        if (field != null && value != null) {
            headers.add(new String[]{field, value.toString()});
        }
    }
}
