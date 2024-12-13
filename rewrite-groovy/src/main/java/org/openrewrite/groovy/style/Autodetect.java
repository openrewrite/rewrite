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
package org.openrewrite.groovy.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.emptySet;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.groovy.Autodetect",
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
            if (cu instanceof G.CompilationUnit) {
                javaDetector.sampleJava((G.CompilationUnit) cu);
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
