/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.Tree;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

public class AutodetectGeneralFormatStyle extends XmlVisitor<LineEndingsCount> {

    /**
     * Makes a best-effort attempt to determine whether windows-style (CRLF) line endings or unix-style (LF) are
     * more common in the supplied AST.
     */
    public static GeneralFormatStyle autodetectGeneralFormatStyle(Xml.Document x) {
        LineEndingsCount count = new LineEndingsCount();
        new AutodetectGeneralFormatStyle().visit(x, count);
        if(count.lf >= count.crlf) {
            return new GeneralFormatStyle(false);
        } else {
            return new GeneralFormatStyle(true);
        }
    }

    @Override
    public @Nullable Xml visit(@Nullable Tree tree, LineEndingsCount count) {
        if(tree instanceof Xml) {
            String s = ((Xml) tree).getPrefix();
            for (int i = 0; i < s.length(); i++) {
                char current = s.charAt(i);
                char next = '\0';
                if(i < s.length() - 1) {
                    next = s.charAt(i + 1);
                }
                if (current == '\r' && next == '\n') {
                    count.crlf++;
                    i++; // skip the \n
                } else if (current == '\n') {
                    count.lf++;
                }
            }
        }
        return super.visit(tree, count);
    }
}

class LineEndingsCount {
    public int crlf = 0;
    public int lf = 0;
}
