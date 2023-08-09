/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.format;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.style.GeneralFormatStyle;

public class AutodetectGeneralFormatStyle extends JavaIsoVisitor<LineEndingsCount> {

    AutodetectJavadocVisitor javadocVisitor = new AutodetectJavadocVisitor();

    /**
     * Makes a best-effort attempt to determine whether windows-style (CRLF) line endings or unix-style (LF) are
     * more common in the supplied AST.
     */
    public static GeneralFormatStyle autodetectGeneralFormatStyle(JavaSourceFile j) {
        LineEndingsCount count = new LineEndingsCount();
        new AutodetectGeneralFormatStyle().visit(j, count);
        if(count.lf >= count.crlf) {
            return new GeneralFormatStyle(false);
        } else {
            return new GeneralFormatStyle(true);
        }
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, LineEndingsCount count) {
        processString(space.getWhitespace(), count);
        for (Comment comment : space.getComments()) {
            if (comment instanceof TextComment) {
                processString(((TextComment) comment).getText(), count);
            } else if (comment instanceof Javadoc) {
                javadocVisitor.visit((Javadoc) comment, count);
            }
        }
        return space;
    }

    private static void processString(String s, LineEndingsCount count) {
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

    private class AutodetectJavadocVisitor extends org.openrewrite.java.JavadocVisitor<LineEndingsCount> {
        public AutodetectJavadocVisitor() {
            super(AutodetectGeneralFormatStyle.this);
        }

        @Override
        public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, LineEndingsCount lineEndingsCount) {
            processString(lineBreak.getMargin(), lineEndingsCount);
            return lineBreak;
        }
    }
}

class LineEndingsCount {
    public int crlf = 0;
    public int lf = 0;
}
