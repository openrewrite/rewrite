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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;

import java.util.*;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RemoveImport<P> extends JavaIsoVisitor<P> {
    @EqualsAndHashCode.Include
    private final String type;

    private final String owner;

    public RemoveImport(String type) {
        this.type = type;
        this.owner = type.substring(0, Math.max(0, type.lastIndexOf('.')));
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {

        ImportLayoutStyle importLayoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                .orElse(IntelliJ.importLayout());

        boolean typeUsed = false;
        Set<String> otherTypesInPackageUsed = new TreeSet<>();

        Set<String> methodsAndFieldsUsed = new HashSet<>();
        Set<String> otherMethodsAndFieldsInTypeUsed = new TreeSet<>();

        for (JavaType javaType : cu.getTypesInUse()) {
            if (javaType instanceof JavaType.Variable) {
                JavaType.Variable variable = (JavaType.Variable) javaType;
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getType());
                if (fq != null) {
                    if (fq.getFullyQualifiedName().equals(type) || fq.getFullyQualifiedName().equals(owner)) {
                        methodsAndFieldsUsed.add(variable.getName());
                    }
                }
            } else if (javaType instanceof JavaType.Method) {
                JavaType.Method method = (JavaType.Method) javaType;
                if (method.hasFlags(Flag.Static)) {
                    if (method.getDeclaringType().getFullyQualifiedName().equals(type)) {
                        methodsAndFieldsUsed.add(method.getName());
                    }
                }
            } else if (javaType instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) javaType;
                if (fullyQualified.getFullyQualifiedName().equals(type)) {
                    typeUsed = true;
                } else if (fullyQualified.getFullyQualifiedName().equals(owner) || fullyQualified.getPackageName().equals(owner)) {
                    otherTypesInPackageUsed.add(fullyQualified.getClassName());
                }
            }
        }

        J.CompilationUnit c = cu;

        boolean keepImport = typeUsed;
        c = c.withImports(ListUtils.flatMap(c.getImports(), impoort -> {
            String typeName = impoort.getTypeName();
            if (impoort.isStatic()) {
                String imported = impoort.getQualid().getSimpleName();
                if ((typeName + "." + imported).equals(type) && !methodsAndFieldsUsed.contains(imported)) {
                    // e.g. remove java.util.Collections.emptySet when type is java.util.Collections.emptySet
                    return null;
                } else if (typeName.equals(type) && "*".equals(imported)) {
                    if (methodsAndFieldsUsed.isEmpty() && otherMethodsAndFieldsInTypeUsed.size() < importLayoutStyle.getClassCountToUseStarImport()) {
                        if (otherMethodsAndFieldsInTypeUsed.isEmpty()) {
                            return null;
                        } else {
                            return unfoldStarImport(impoort, otherMethodsAndFieldsInTypeUsed);
                        }
                    }
                } else if (typeName.equals(type) && !methodsAndFieldsUsed.contains(imported)) {
                    // e.g. remove java.util.Collections.emptySet when type is java.util.Collections
                    return null;
                }
            } else if (!keepImport && typeName.equals(type)) {
                return null;
            } else if (!keepImport && impoort.getPackageName().equals(owner) &&
                    "*".equals(impoort.getClassName()) &&
                    otherTypesInPackageUsed.size() < importLayoutStyle.getNameCountToUseStarImport()) {
                if (otherTypesInPackageUsed.isEmpty()) {
                    return null;
                } else {
                    return unfoldStarImport(impoort, otherTypesInPackageUsed);
                }
            }
            return impoort;
        }));

        if (c != cu) {
            if (!c.getImports().isEmpty()) {
                c = autoFormat(c, c.getImports().get(c.getImports().size() - 1), p, getCursor());
            } else if (!c.getClasses().isEmpty()) {
                c = autoFormat(c, c.getClasses().get(0).getName(), p, getCursor());
            }
        }

        return c;
    }

    private Object unfoldStarImport(J.Import starImport, Set<String> otherImportsUsed) {
        List<J.Import> unfoldedImports = new ArrayList<>(otherImportsUsed.size());
        int i = 0;
        for (String other : otherImportsUsed) {
            J.Import unfolded = starImport.withQualid(starImport.getQualid().withName(starImport
                    .getQualid().getName().withName(other)));
            unfoldedImports.add(i++ == 0 ? unfolded : unfolded.withPrefix(Space.format("\n")));
        }
        return unfoldedImports;
    }
}
