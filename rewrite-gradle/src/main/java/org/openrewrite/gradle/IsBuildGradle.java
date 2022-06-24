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
package org.openrewrite.gradle;

import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.JavaSourceFile;

public class IsBuildGradle<P> extends JavaIsoVisitor<P> {
    @Override
    public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, P p) {
        SourceFile sourceFile = (SourceFile) cu;
        if (sourceFile.getSourcePath().toString().endsWith(".gradle")) {
            return sourceFile.withMarkers(sourceFile.getMarkers().searchResult());
        }
        return super.visitJavaSourceFile(cu, p);
    }
}
