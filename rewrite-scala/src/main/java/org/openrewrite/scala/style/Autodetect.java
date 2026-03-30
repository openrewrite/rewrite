/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.scala.tree.S;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.emptySet;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(@JsonProperty("id") UUID id, @JsonProperty("styles") Collection<Style> styles) {
        super(id,
                "org.openrewrite.scala.Autodetect",
                "Auto-detected",
                "Automatically detect styles from a repository's existing code.",
                emptySet(), styles);
    }

    public static Detector detector() {
        return new Detector();
    }

    public static class Detector {
        org.openrewrite.java.style.Autodetect.Detector javaDetector = new org.openrewrite.java.style.Autodetect.Detector();

        public void sample(SourceFile cu) {
            if (cu instanceof S.CompilationUnit) {
                javaDetector.sampleJava((S.CompilationUnit) cu);
            }
        }

        public Autodetect build() {
            return new Autodetect(Tree.randomId(), Arrays.asList(
                    javaDetector.getTabsAndIndentsStyle(),
                    javaDetector.getImportLayoutStyle(),
                    javaDetector.getSpacesStyle(),
                    javaDetector.getWrappingAndBracesStyle(),
                    javaDetector.getFormatStyle()));
        }
    }
}
