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
package org.openrewrite.yaml.style;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.FindIndentYamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class Autodetect {
    public static IndentsStyle tabsAndIndents(Yaml yaml, IndentsStyle orElse) {
        FindIndentYamlVisitor<Void> findIndent = new FindIndentYamlVisitor<>(0);

        //noinspection ConstantConditions
        findIndent.visit(yaml, null);

        return findIndent.nonZeroIndents() > 0 ?
                new IndentsStyle(findIndent.getMostCommonIndent()) :
                orElse;
    }

    public static GeneralFormatStyle generalFormat(Yaml yaml) {
        FindLineFormatJavaVisitor<Void> findLineFormat = new FindLineFormatJavaVisitor<>();

        //noinspection ConstantConditions
        findLineFormat.visit(yaml, null);

        return new GeneralFormatStyle(!findLineFormat.isIndentedWithLFNewLines());
    }

    private static class FindLineFormatJavaVisitor<P> extends YamlIsoVisitor<P> {
        private int linesWithCRLFNewLines;
        private int linesWithLFNewLines;

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        @Override
        public @Nullable Yaml visit(@Nullable Tree tree, P p) {
            Yaml y = super.visit(tree, p);
            if (y != null) {
                if (y.getPrefix().startsWith("\r\n")) {
                    linesWithCRLFNewLines++;
                } else if (y.getPrefix().startsWith("\n")) {
                    linesWithLFNewLines++;
                }
            }
            return y;
        }
    }
}
