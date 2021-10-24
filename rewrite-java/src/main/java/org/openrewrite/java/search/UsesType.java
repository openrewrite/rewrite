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
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UsesType<P> extends JavaIsoVisitor<P> {
    private final String fullyQualifiedType;
    private final List<String> fullyQualifiedTypeSegments;

    public UsesType(String fullyQualifiedType) {
        this.fullyQualifiedType = fullyQualifiedType;
        Scanner scanner = new Scanner(fullyQualifiedType);
        scanner.useDelimiter("\\.");
        this.fullyQualifiedTypeSegments = new ArrayList<>();
        while (scanner.hasNext()) {
            fullyQualifiedTypeSegments.add(scanner.next());
        }
    }

    @Override
    public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, P p) {
        JavaSourceFile c = cu;

        for (JavaType.Method method : c.getTypesInUse().getUsedMethods()) {
            if (method.hasFlags(Flag.Static)) {
                if ((c = maybeMark(c, method.getDeclaringType())) != cu) {
                    return c;
                }
            }
        }

        for (JavaType type : c.getTypesInUse().getTypesInUse()) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            if ((c = maybeMark(c, fq)) != cu) {
                return c;
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

    private JavaSourceFile maybeMark(JavaSourceFile c, @Nullable JavaType.FullyQualified fq) {
        if (fq == null) {
            return c;
        }

        if (TypeUtils.isAssignableTo(fullyQualifiedType, fq)) {
            return c.withMarkers(c.getMarkers().searchResult());
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

        return c.withMarkers(c.getMarkers().searchResult());
    }
}
