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
package org.openrewrite.java.internal;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.format.BlankLinesVisitor;
import org.openrewrite.java.style.BlankLinesStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.Style;

public class FormatFirstClassPrefix<P> extends JavaIsoVisitor<P> {
    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
        if (classDecl != cu.getClasses().get(0)) {
            return classDecl;
        }

        // Only adjust blank lines in the class prefix. Using autoFormat here would run
        // the full formatting pipeline including NormalizeTabsOrSpaces, which can convert
        // tabs to spaces (or vice versa) in annotation parameters and the class prefix.
        BlankLinesStyle style = Style.from(BlankLinesStyle.class, cu, IntelliJ::blankLines);
        Space prefix = classDecl.getPrefix();

        if (cu.getImports().isEmpty()) {
            if (cu.getPackageDeclaration() == null) {
                if (!prefix.getWhitespace().isEmpty()) {
                    classDecl = classDecl.withPrefix(prefix.withWhitespace(""));
                }
            } else {
                String whitespace = BlankLinesVisitor.minimumLines(prefix.getWhitespace(),
                        style.getMinimum().getAfterPackage());
                if (!whitespace.equals(prefix.getWhitespace())) {
                    classDecl = classDecl.withPrefix(prefix.withWhitespace(whitespace));
                }
            }
        } else {
            int min = style.getMinimum().getAfterImports();
            int max = Math.max(style.getKeepMaximum().getInDeclarations(), min);
            String whitespace = BlankLinesVisitor.minimumLines(prefix.getWhitespace(), min);
            whitespace = keepMaximumLines(whitespace, max);
            if (!whitespace.equals(prefix.getWhitespace())) {
                classDecl = classDecl.withPrefix(prefix.withWhitespace(whitespace));
            }
        }
        return classDecl;
    }

    private static String keepMaximumLines(String whitespace, int max) {
        long newLineCount = whitespace.chars().filter(c -> c == '\n').count();
        if (newLineCount - 1 > max) {
            int startWhitespaceAtIndex = 0;
            for (int i = 0; i < newLineCount - max; i++, startWhitespaceAtIndex++) {
                startWhitespaceAtIndex = whitespace.indexOf('\n', startWhitespaceAtIndex);
            }
            startWhitespaceAtIndex--;
            return whitespace.substring(startWhitespaceAtIndex);
        }
        return whitespace;
    }
}
