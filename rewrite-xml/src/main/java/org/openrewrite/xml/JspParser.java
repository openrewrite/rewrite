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
package org.openrewrite.xml;

import org.jspecify.annotations.Nullable;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.Locale;

/** Parses JSP template syntax, including translation-unit fragments with multiple roots. */
public class JspParser extends XmlParser {
    @Override
    public boolean accept(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jsp") || name.endsWith(".jspf") || name.endsWith(".inc");
    }

    @Override
    protected boolean isJsp(@Nullable Path path) {
        return true;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.jsp");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends XmlParser.Builder {
        public Builder() {
            super(Xml.JspDocument.class);
        }

        @Override
        public JspParser build() {
            return new JspParser();
        }

        @Override
        public String getDslName() {
            return "jsp";
        }
    }
}
