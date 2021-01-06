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
package org.openrewrite.xml;

import org.openrewrite.Tree;
import org.openrewrite.refactor.Formatter;
import org.openrewrite.xml.tree.Xml;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;

public class AutoFormat extends XmlRefactorVisitor {
    private final Xml.Tag[] scope;

    public AutoFormat(Xml.Tag... scope) {
        this.scope = scope;
        setCursoringOn();
    }

    @Override
    public Xml reduce(Xml r1, Xml r2) {
        Xml x = super.reduce(r1, r2);
        if (r2 != null && r2.getPrefix().startsWith("|")) {
            x = x.withPrefix(r2.getPrefix().substring(1));
        }
        return x;
    }

    @Override
    public Xml visitTree(Tree tree) {
        Xml x = super.visitTree(tree);

        String prefix = tree.getPrefix();
        if (prefix.contains("\n") && stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
            int indentMultiple = (int) getCursor().getPathAsStream().filter(Xml.Tag.class::isInstance).count() - 1;
            Formatter.Result wholeSourceIndent = formatter.wholeSourceIndent();
            String shiftedPrefix = "|" + prefix.substring(0, prefix.lastIndexOf('\n') + 1) + range(0, indentMultiple * wholeSourceIndent.getIndentToUse())
                    .mapToObj(n -> wholeSourceIndent.isIndentedWithSpaces() ? " " : "\t")
                    .collect(Collectors.joining(""));

            if (!shiftedPrefix.equals(prefix)) {
                x = x.withPrefix(shiftedPrefix);
            }
        }

        return x;
    }
}
