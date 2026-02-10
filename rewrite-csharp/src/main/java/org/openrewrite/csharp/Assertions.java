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
package org.openrewrite.csharp;

import org.jspecify.annotations.Nullable;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.util.function.Consumer;

public class Assertions {

    private Assertions() {
    }

    public static SourceSpecs csharp(@Nullable String before) {
        return csharp(before, s -> {
        });
    }

    public static SourceSpecs csharp(@Nullable String before, Consumer<SourceSpec<Cs.CompilationUnit>> spec) {
        SourceSpec<Cs.CompilationUnit> cs = new SourceSpec<>(
                Cs.CompilationUnit.class, null, CSharpParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        cs.path(System.nanoTime() + ".cs");
        spec.accept(cs);
        return cs;
    }

    public static SourceSpecs csharp(@Nullable String before, @Nullable String after) {
        return csharp(before, after, s -> {
        });
    }

    public static SourceSpecs csharp(@Nullable String before, @Nullable String after,
                                     Consumer<SourceSpec<Cs.CompilationUnit>> spec) {
        SourceSpec<Cs.CompilationUnit> cs = new SourceSpec<>(
                Cs.CompilationUnit.class, null, CSharpParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        cs.path(System.nanoTime() + ".cs");
        cs.after(s -> after);
        spec.accept(cs);
        return cs;
    }

    public static SourceSpecs csproj(@Nullable String before) {
        return csproj(before, s -> {
        });
    }

    public static SourceSpecs csproj(@Nullable String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(
                Xml.Document.class, null, XmlParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        xml.path("project.csproj");
        spec.accept(xml);
        return xml;
    }

    public static SourceSpecs csproj(@Nullable String before, @Nullable String after) {
        return csproj(before, after, s -> {
        });
    }

    public static SourceSpecs csproj(@Nullable String before, @Nullable String after,
                                     Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(
                Xml.Document.class, null, XmlParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        xml.path("project.csproj");
        xml.after(s -> after);
        spec.accept(xml);
        return xml;
    }
}
