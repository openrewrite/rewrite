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
package org.openrewrite.yaml.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class NormalizeLineBreaksVisitor<P> extends YamlIsoVisitor<P> {
    private final GeneralFormatStyle generalFormatStyle;

    @Nullable
    private final Tree stopAfter;

    public NormalizeLineBreaksVisitor(GeneralFormatStyle generalFormatStyle, @Nullable Tree stopAfter) {
        this.generalFormatStyle = generalFormatStyle;
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable Yaml postVisit(Yaml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Yaml.Documents.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Yaml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Yaml) tree;
        }

        Yaml y = super.visit(tree, p);
        if (y != null) {
            String modifiedPrefix = normalizeNewLines(y.getPrefix(), generalFormatStyle.isUseCRLFNewLines());
            if (!y.getPrefix().equals(modifiedPrefix)) {
                y = y.withPrefix(modifiedPrefix);
            }
        }
        return y;
    }

    private static String normalizeNewLines(String text, boolean useCrlf) {
        if (!text.contains("\n")) {
            return text;
        }

        StringBuilder normalized = new StringBuilder();
        char[] charArray = text.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (useCrlf && c == '\n' && (i == 0 || text.charAt(i - 1) != '\r')) {
                normalized.append('\r').append('\n');
            } else if (useCrlf || c != '\r') {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }
}
