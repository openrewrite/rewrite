/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Incubating(since = "7.20.0")
public class PotentiallyDeadCode extends Recipe {
    @Override
    public String getDisplayName() {
        return "List potentially dead code by declared method";
    }

    @Override
    public String getDescription() {
        return "Method definitions that are defined in this project and aren't used.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Set<JavaType.Method> declared = new HashSet<>();
        Set<JavaType.Method> used = new HashSet<>();

        GitProvenance gitProvenance = null;

        for (SourceFile sourceFile : before) {
            if (sourceFile instanceof JavaSourceFile) {
                gitProvenance = sourceFile.getMarkers().findFirst(GitProvenance.class).orElse(gitProvenance);

                JavaSourceFile cu = (JavaSourceFile) sourceFile;
                declared.addAll(cu.getTypesInUse().getDeclaredMethods()
                        .stream()
                        .filter(m -> !m.hasFlags(Flag.Private) && !m.hasFlags(Flag.Protected))
                        .filter(m -> {
                            // because a concrete implementation of an interface may not be used directly,
                            // but rather the interface method is used
                            for (JavaType.FullyQualified ann : m.getAnnotations()) {
                                if (ann.getFullyQualifiedName().equals("java.lang.Override")) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(Collectors.toList()));
                used.addAll(cu.getTypesInUse().getUsedMethods());
            }
        }

        declared.removeAll(used);

        Path sourcePath = Paths.get("" +
                "dead-code" +
                (gitProvenance == null ? "" :
                        "-" + gitProvenance.getRepositoryName()) +
                ".csv");

        return ListUtils.concatAll(before, new PlainTextParser()
                .parse(declared.stream()
                        .map(d -> d.getDeclaringType().getFullyQualifiedName() + "," +
                                d.getName() + "(" +
                                d.getParameterTypes().stream()
                                        .map(pt -> pt instanceof JavaType.FullyQualified ? ((JavaType.FullyQualified) pt).getFullyQualifiedName() :
                                                pt.toString())
                                        .collect(Collectors.joining(",")) +
                                ")")
                        .collect(Collectors.joining("\n", "declaring type,method\n", "")))
                .stream()
                .map(pt -> pt.withSourcePath(sourcePath))
                .collect(Collectors.toList()));
    }
}
