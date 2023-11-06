/*
 * Copyright 2020 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;

public class RemoveTrailingWhitespaceVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @JsonCreator
    public RemoveTrailingWhitespaceVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    public RemoveTrailingWhitespaceVisitor() {
        this(null);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        String eof = cu.getEof().getWhitespace();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < eof.length(); i++) {
            char c = eof.charAt(i);
            if (c == '\n' || c == '\r') {
                builder.appendCodePoint(c);
            }
        }
        eof = builder.toString();
        J.CompilationUnit c = super.visitCompilationUnit(cu, p);
        return c.withEof(c.getEof().withWhitespace(eof));
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        Space s = space;
        String whitespace = s.getWhitespace();
        int lastNewline = whitespace.lastIndexOf('\n');
        // Skip import prefixes, leave those up to OrderImports which better understands that domain
        if (lastNewline > 0 && loc != Space.Location.IMPORT_PREFIX) {
            StringBuilder ws = new StringBuilder();
            for (int i = 0; i < whitespace.length(); i++) {
                char c = whitespace.charAt(i);
                if (i >= lastNewline) {
                    ws.append(c);
                } else if (c == ',' || c == '\r' || c == '\n') {
                    ws.append(c);
                }
            }
            s = s.withWhitespace(ws.toString());
        }
        return s;
    }

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }
}
