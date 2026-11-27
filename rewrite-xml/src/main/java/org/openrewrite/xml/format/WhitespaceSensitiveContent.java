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
package org.openrewrite.xml.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.Locale;

/// Content of elements whose whitespace is part of what they hold, such as the text of an HTML `<pre>` or the
/// code in a `<script>`, which formatting must leave as it is.
final class WhitespaceSensitiveContent {
    private static final String[] ELEMENTS = {"pre", "textarea", "script", "style"};
    private static final String[] HTML_EXTENSIONS = {".html", ".htm", ".xhtml", ".jsp", ".jspf"};

    private WhitespaceSensitiveContent() {
    }

    static boolean contains(@Nullable Tree tree, Cursor parent) {
        if (!(tree instanceof Content) && !(tree instanceof Xml.Tag.Closing) || !isHtml(parent)) {
            return false;
        }
        return parent.getPathAsStream().anyMatch(t -> t instanceof Xml.Tag && isWhitespaceSensitive((Xml.Tag) t));
    }

    static boolean isWhitespaceSensitive(Xml.Tag tag, Cursor cursor) {
        return isWhitespaceSensitive(tag) && isHtml(cursor);
    }

    // Outside HTML these names carry no whitespace semantics, such as an Android resource's <style>
    private static boolean isHtml(Cursor cursor) {
        Xml.Document document = cursor.firstEnclosing(Xml.Document.class);
        if (document == null) {
            return false;
        }
        String path = document.getSourcePath().toString().toLowerCase(Locale.ROOT);
        for (String extension : HTML_EXTENSIONS) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWhitespaceSensitive(Xml.Tag tag) {
        for (String element : ELEMENTS) {
            if (element.equalsIgnoreCase(tag.getName())) {
                return true;
            }
        }
        return false;
    }
}
