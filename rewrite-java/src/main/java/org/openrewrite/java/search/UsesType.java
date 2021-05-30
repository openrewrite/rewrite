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
package org.openrewrite.java.search;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

public class UsesType<P> extends JavaIsoVisitor<P> {
    @SuppressWarnings("ConstantConditions")
    private static final Marker FOUND_TYPE = new JavaSearchResult(randomId(), null, null);

    private final List<String> fullyQualifiedTypeSegments;

    public UsesType(String fullyQualifiedType) {
        Scanner scanner = new Scanner(fullyQualifiedType);
        scanner.useDelimiter("\\.");
        this.fullyQualifiedTypeSegments = new ArrayList<>();
        while (scanner.hasNext()) {
            fullyQualifiedTypeSegments.add(scanner.next());
        }
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = cu;
        Set<JavaType> types = c.getTypesInUse();
        for (JavaType type : types) {
            if (type instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified fq = (JavaType.FullyQualified) type;
                if ((c = maybeMark(c, fq)) != cu) {
                    return c;
                }
            } else if (type instanceof JavaType.Method) {
                JavaType.Method method = (JavaType.Method) type;
                if (method.hasFlags(Flag.Static)) {
                    if ((c = maybeMark(c, method.getDeclaringType())) != cu) {
                        return c;
                    }
                }
            }
        }

        for (J.Import anImport : c.getImports()) {
            if (anImport.isStatic()) {
                if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType()))) != cu) {
                    return c;
                }
            } else if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getType()))) != cu) {
                return c;
            }
        }

        return c;
    }

    private J.CompilationUnit maybeMark(J.CompilationUnit c, @Nullable JavaType.FullyQualified fq) {
        if (fq == null) {
            return c;
        }

        Scanner scanner = new Scanner(fq.getFullyQualifiedName());
        scanner.useDelimiter("\\.");
        int i = 0;
        for (; scanner.hasNext() && i < fullyQualifiedTypeSegments.size(); i++) {
            String segment = fullyQualifiedTypeSegments.get(i);
            if (segment.equals("*")) {
                break;
            }
            String test = scanner.next();
            if (!segment.equals(test)) {
                return c;
            }
        }

        return c.withMarkers(c.getMarkers().addIfAbsent(FOUND_TYPE));
    }
}
