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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.GeneralFormatStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

/**
 * A Java refactoring visitor that can be used to add an import (or static import) to a given compilation unit.
 * This visitor can also be configured to only add the import if the imported class/method are referenced within the
 * compilation unit.
 * <p>
 * The {@link AddImport#typeName} must be supplied and together with the optional {@link AddImport#packageName}
 * represents the fully qualified class name.
 * <p>
 * The {@link AddImport#member} is an optional method within the imported type. The staticMethod can be set to "*"
 * to represent a static wildcard import.
 * <p>
 * The {@link AddImport#onlyIfReferenced} is a flag (defaulted to true) to indicate if the import should only be added
 * if there is a reference to the imported class/method.
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddImport<P> extends JavaIsoVisitor<P> {

    @Nullable
    private final String packageName;

    private final String typeName;

    @EqualsAndHashCode.Include
    private final String fullyQualifiedName;

    @EqualsAndHashCode.Include
    @Nullable
    private final String member;

    @EqualsAndHashCode.Include
    private final boolean onlyIfReferenced;

    @EqualsAndHashCode.Include
    @Nullable
    private final String alias;

    public AddImport(String type, @Nullable String member, boolean onlyIfReferenced) {
        int lastDotIdx = type.lastIndexOf('.');
        this.packageName = lastDotIdx != -1 ? type.substring(0, lastDotIdx) : null;
        this.typeName = lastDotIdx != -1 ? type.substring(lastDotIdx + 1) : type;
        this.fullyQualifiedName = type;
        this.member = member;
        this.onlyIfReferenced = onlyIfReferenced;
        alias = null;
    }

    public AddImport(@Nullable String packageName, String typeName, @Nullable String member, @Nullable String alias, boolean onlyIfReferenced) {
        this.packageName = packageName;
        this.typeName = typeName.replace('.', '$');
        this.fullyQualifiedName = packageName == null ? typeName : packageName + "." + typeName;
        this.member = member;
        this.onlyIfReferenced = onlyIfReferenced;
        this.alias = alias;
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        stopAfterPreVisit();
        J j = tree;
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            if (packageName == null || JavaType.Primitive.fromKeyword(fullyQualifiedName) != null) {
                return cu;
            }

            // No need to add imports if the class to import is in java.lang, or if the classes are within the same package
            if (("java.lang".equals(packageName) && StringUtils.isBlank(member)) || (cu.getPackageDeclaration() != null &&
                                                                                     packageName.equals(cu.getPackageDeclaration().getExpression().printTrimmed(getCursor())))) {
                return cu;
            }

            if (onlyIfReferenced && !hasReference(cu)) {
                return cu;
            }

            if (cu.getImports().stream().anyMatch(i -> {
                String ending = i.getQualid().getSimpleName();
                if (member == null) {
                    return !i.isStatic() && i.getPackageName().equals(packageName) &&
                           (ending.equals(typeName) || "*".equals(ending));
                }
                return i.isStatic() && i.getTypeName().equals(fullyQualifiedName) &&
                       (ending.equals(member) || "*".equals(ending));
            })) {
                return cu;
            }

            J.Import importToAdd = new J.Import(randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new JLeftPadded<>(member == null ? Space.EMPTY : Space.SINGLE_SPACE,
                            member != null, Markers.EMPTY),
                    TypeTree.build(fullyQualifiedName +
                                   (member == null ? "" : "." + member)).withPrefix(Space.SINGLE_SPACE),
                    null);

            List<JRightPadded<J.Import>> imports = new ArrayList<>(cu.getPadding().getImports());

            if (imports.isEmpty() && !cu.getClasses().isEmpty()) {
                if (cu.getPackageDeclaration() == null) {
                    // leave javadocs on the class and move other comments up to the import
                    // (which could include license headers and the like)
                    Space firstClassPrefix = cu.getClasses().get(0).getPrefix();
                    importToAdd = importToAdd.withPrefix(firstClassPrefix
                            .withComments(ListUtils.map(firstClassPrefix.getComments(), comment -> comment instanceof Javadoc ? null : comment))
                            .withWhitespace(""));

                    cu = cu.withClasses(ListUtils.mapFirst(cu.getClasses(), clazz ->
                            clazz.withComments(ListUtils.map(clazz.getComments(), comment -> comment instanceof Javadoc ? comment : null))
                    ));
                }
            }

            ImportLayoutStyle layoutStyle = Optional.ofNullable(((SourceFile) cu).getStyle(ImportLayoutStyle.class))
                    .orElse(IntelliJ.importLayout());

            List<JavaType.FullyQualified> classpath = cu.getMarkers().findFirst(JavaSourceSet.class)
                    .map(JavaSourceSet::getClasspath)
                    .orElse(Collections.emptyList());

            List<JRightPadded<J.Import>> newImports = layoutStyle.addImport(cu.getPadding().getImports(), importToAdd, cu.getPackageDeclaration(), classpath);

            // ImportLayoutStyle::addImport adds always `\n` as newlines. Checking if we need to fix them
            newImports = checkCRLF(cu, newImports);

            cu = cu.getPadding().withImports(newImports);

            JavaSourceFile c = cu;
            cu = cu.withClasses(ListUtils.mapFirst(cu.getClasses(), clazz -> {
                J.ClassDeclaration cl = autoFormat(clazz, clazz.getName(), p, new Cursor(null, c));
                return clazz.withPrefix(clazz.getPrefix().withWhitespace(cl.getPrefix().getWhitespace()));
            }));

            j = cu;
        }
        return j;
    }

    private List<JRightPadded<J.Import>> checkCRLF(JavaSourceFile cu, List<JRightPadded<J.Import>> newImports) {
        GeneralFormatStyle generalFormatStyle = Optional.ofNullable(((SourceFile) cu).getStyle(GeneralFormatStyle.class))
                .orElse(autodetectGeneralFormatStyle(cu));
        if (generalFormatStyle.isUseCRLFNewLines()) {
            return ListUtils.map(newImports, rp -> rp.map(
                    i -> i.withPrefix(i.getPrefix().withWhitespace(i.getPrefix().getWhitespace()
                            .replaceAll("(?<!\r)\n", "\r\n")))
            ));
        }
        return newImports;
    }

    private boolean isTypeReference(NameTree t) {
        boolean isTypRef = true;
        if (t instanceof J.FieldAccess) {
            isTypRef = isOfClassType(((J.FieldAccess) t).getTarget().getType(), fullyQualifiedName);
        }
        return isTypRef;
    }

    /**
     * Returns true if there is at least one matching references for this associated import.
     * An import is considered a match if:
     * It is non-static and has a field reference
     * It is static, the static method is a wildcard, and there is at least on method invocation on the given import type.
     * It is static, the static method is explicitly defined, and there is at least on method invocation matching the type and method.
     *
     * @param compilationUnit The compilation passed to the visitCompilationUnit
     * @return true if the import is referenced by the class either explicitly or through a method reference.
     */
    //Note that using anyMatch when a stream is empty ends up returning true, which is not the behavior needed here!
    private boolean hasReference(JavaSourceFile compilationUnit) {
        if (member == null) {
            //Non-static imports, we just look for field accesses.
            for (NameTree t : FindTypes.find(compilationUnit, fullyQualifiedName)) {
                if ((!(t instanceof J.FieldAccess) || !((J.FieldAccess) t).isFullyQualifiedClassReference(fullyQualifiedName)) &&
                    isTypeReference(t)) {
                    return true;
                }
            }
            return false;
        }

        // For static method imports, we are either looking for a specific method or a wildcard.
        for (J invocation : FindMethods.find(compilationUnit, fullyQualifiedName + " *(..)")) {
            if (invocation instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) invocation;
                if (mi.getSelect() == null &&
                    ("*".equals(member) || mi.getName().getSimpleName().equals(member))) {
                    return true;
                }
            }
        }

        // Check whether there is static-style access of the field in question
        AtomicReference<Boolean> hasStaticFieldAccess = new AtomicReference<>(false);
        new FindStaticFieldAccess().visit(compilationUnit, hasStaticFieldAccess);
        return hasStaticFieldAccess.get();
    }

    private class FindStaticFieldAccess extends JavaIsoVisitor<AtomicReference<Boolean>> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, AtomicReference<Boolean> found) {
            // If the type isn't used there's no need to proceed further
            for (JavaType.Variable varType : cu.getTypesInUse().getVariables()) {
                if (varType.getName().equals(member) && isOfClassType(varType.getType(), fullyQualifiedName)) {
                    return super.visitCompilationUnit(cu, found);
                }
            }
            return cu;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, AtomicReference<Boolean> found) {
            assert getCursor().getParent() != null;
            if (identifier.getSimpleName().equals(member) && isOfClassType(identifier.getType(), fullyQualifiedName) &&
                !(getCursor().getParent().firstEnclosingOrThrow(J.class) instanceof J.FieldAccess)) {
                found.set(true);
            }
            return identifier;
        }
    }
}
