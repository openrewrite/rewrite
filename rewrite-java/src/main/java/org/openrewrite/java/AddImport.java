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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
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

            // No need to add imports if the class to import is in java.lang
            if ("java.lang".equals(packageName) && StringUtils.isBlank(member)) {
                return cu;
            }
            // Nor if the classes are within the same package
            if (!"Record".equals(typeName) && cu.getPackageDeclaration() != null &&
                    packageName.equals(cu.getPackageDeclaration().getExpression().printTrimmed(getCursor()))) {
                return cu;
            }
            Optional<JavaType> typeReference = findTypeReference(cu);
            if (onlyIfReferenced && !typeReference.isPresent()) {
                return cu;
            }

            ImportStatus importStatus = checkImportsForType(cu.getImports());

            if (importStatus == ImportStatus.EXPLICITLY_IMPORTED) {
                return cu;
            }

            if (!"Record".equals(typeName) && importStatus == ImportStatus.IMPLICITLY_IMPORTED) {
                return cu;
            }

            if (importStatus == ImportStatus.IMPORT_AMBIGUITY && typeReference.isPresent()) {
                if (typeReference.get() instanceof JavaType.FullyQualified) {
                    return new FullyQualifyTypeReference<P>((JavaType.FullyQualified) typeReference.get()).visit(cu, p);
                }
                if (typeReference.get() instanceof JavaType.Method || typeReference.get() instanceof JavaType.Variable) {
                    return new FullyQualifyMemberReference<P>(typeReference.get()).visit(cu, p);
                }
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

            if (imports.isEmpty() && !cu.getClasses().isEmpty() && cu.getPackageDeclaration() == null) {
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

            ImportLayoutStyle layoutStyle = Optional.ofNullable(Style.from(ImportLayoutStyle.class, ((SourceFile) cu)))
                    .orElse(IntelliJ.importLayout());

            List<JavaType.FullyQualified> classpath = cu.getMarkers().findFirst(JavaSourceSet.class)
                    .map(JavaSourceSet::getClasspath)
                    .orElse(emptyList());

            List<JRightPadded<J.Import>> newImports = layoutStyle.addImport(cu.getPadding().getImports(), importToAdd, cu.getPackageDeclaration(), classpath);

            if (member != null && typeReference.isPresent()) {
                cu = (JavaSourceFile) new ShortenFullyQualifiedMemberReferences(typeReference.get()).visit(cu, p);
            } else if (member == null && typeReference.isPresent()) {
                cu = (JavaSourceFile) new ShortenFullyQualifiedTypeReferences((JavaType.FullyQualified) typeReference.get()).visit(cu, p);
            }

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

    private ImportStatus checkImportsForType(List<J.Import> imports) {
        for (J.Import imp : imports) {
            String ending = imp.getQualid().getSimpleName();
            if (imp.isStatic() ^ member != null) {
                continue;
            }
            if (imp.isStatic()) {
                if (imp.getTypeName().equals(fullyQualifiedName)) {
                    if (ending.equals(member)) {
                        return ImportStatus.EXPLICITLY_IMPORTED;
                    } else if ("*".equals(ending)) {
                        return ImportStatus.IMPLICITLY_IMPORTED;
                    }
                }
                if (!"*".equals(ending) && ending.equals(member)) {
                    return ImportStatus.IMPORT_AMBIGUITY;
                }
            } else {
                String impTypeName = imp.getTypeName().replace('$', '.');
                if (fullyQualifiedName.equals(impTypeName)) {
                    return ImportStatus.EXPLICITLY_IMPORTED;
                } else if ("*".equals(ending)) {
                    String prefix = impTypeName.substring(0, impTypeName.length() - 1);
                    if (fullyQualifiedName.startsWith(prefix) && !fullyQualifiedName.substring(prefix.length()).contains(".")) {
                        return ImportStatus.IMPLICITLY_IMPORTED;
                    }
                }
                if (!"*".equals(ending) && ending.equals(typeName)) {
                    return ImportStatus.IMPORT_AMBIGUITY;
                }
            }
        }
        return ImportStatus.NOT_IMPORTED;
    }

    private List<JRightPadded<J.Import>> checkCRLF(JavaSourceFile cu, List<JRightPadded<J.Import>> newImports) {
        GeneralFormatStyle generalFormatStyle = Optional.ofNullable(Style.from(GeneralFormatStyle.class, ((SourceFile) cu)))
                .orElse(autodetectGeneralFormatStyle(cu));
        if (generalFormatStyle.isUseCRLFNewLines()) {
            return ListUtils.map(newImports, rp -> rp.map(
                    i -> i.withPrefix(i.getPrefix().withWhitespace(i.getPrefix().getWhitespace()
                            .replaceAll("(?<!\r)\n", "\r\n")))
            ));
        }
        return newImports;
    }

    private Optional<JavaType> getTypeReference(NameTree t) {
        if (!(t instanceof J.FieldAccess)) {
           return Optional.ofNullable(t.getType());

        }
        if (isOfClassType(((J.FieldAccess) t).getTarget().getType(), fullyQualifiedName)) {
            return Optional.ofNullable(((J.FieldAccess) t).getTarget().getType());
        }
        return Optional.empty();
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
    private Optional<JavaType> findTypeReference(JavaSourceFile compilationUnit) {
        if (member == null) {
            //Non-static imports, we just look for field accesses.
            for (NameTree t : FindTypes.find(compilationUnit, fullyQualifiedName)) {
                if (!(t instanceof J.FieldAccess) || !((J.FieldAccess) t).isFullyQualifiedClassReference(fullyQualifiedName)) {
                    Optional<JavaType> mayBeTypeReference = getTypeReference(t);
                    if (mayBeTypeReference.isPresent()) {
                        return mayBeTypeReference;
                    }
                }
            }
            return Optional.empty();
        }

        // For static method imports, we are either looking for a specific method or a wildcard.
        for (J invocation : FindMethods.find(compilationUnit, fullyQualifiedName + " *(..)")) {
            if (invocation instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) invocation;
                if (mi.getSelect() == null &&
                        ("*".equals(member) || mi.getName().getSimpleName().equals(member))) {
                    return Optional.ofNullable(mi.getMethodType());
                }
            }
        }

        // Check whether there is static-style access of the field in question
        return Optional.ofNullable(new FindStaticFieldAccess().reduce(compilationUnit, new AtomicReference<>()).get());
    }

    private enum ImportStatus {
        NOT_IMPORTED,
        IMPLICITLY_IMPORTED,
        EXPLICITLY_IMPORTED,
        IMPORT_AMBIGUITY,
    }

    private class FindStaticFieldAccess extends JavaIsoVisitor<AtomicReference<JavaType>> {
        private boolean checkIsOfClassType(@Nullable JavaType type, String fullyQualifiedName) {
            if (isOfClassType(type, fullyQualifiedName)) {
                return true;
            }
            return type instanceof JavaType.Class && isOfClassType(((JavaType.Class) type).getOwningClass(), fullyQualifiedName);
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, AtomicReference<JavaType> fieldAccess) {
            // If the type isn't used there's no need to proceed further
            for (JavaType.Variable varType : cu.getTypesInUse().getVariables()) {
                if (checkIsOfClassType(varType.getOwner(), fullyQualifiedName)) {
                    return super.visitCompilationUnit(cu, fieldAccess);
                }
            }
            return cu;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, AtomicReference<JavaType> fieldAccess) {
            assert getCursor().getParent() != null;
            if (identifier.getSimpleName().equals(member) && identifier.getFieldType() != null &&
                    checkIsOfClassType(identifier.getFieldType().getOwner(), fullyQualifiedName) &&
                    !(getCursor().getParent().firstEnclosingOrThrow(J.class) instanceof J.FieldAccess)) {
                assert identifier.getType() != null;
                fieldAccess.set(identifier.getFieldType());
            }
            return identifier;
        }
    }

    @AllArgsConstructor
    private class ShortenFullyQualifiedTypeReferences extends ShortenFullyQualifiedReference {
        private final JavaType.FullyQualified typeToShorten;

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
            if (fieldAccess.isFullyQualifiedClassReference(typeToShorten.getFullyQualifiedName())) {
                return fieldAccess.getName().withPrefix(fieldAccess.getPrefix());
            }
            return super.visitFieldAccess(fieldAccess, p);
        }

        @Override
        public J visitIdentifier(J.Identifier identifier, P p) {
            if (isFullyQualifiedClassReference(identifier, typeToShorten.getFullyQualifiedName())) {
                return identifier.withSimpleName(typeToShorten.getClassName());
            }
            return super.visitIdentifier(identifier, p);
        }
    }

    @AllArgsConstructor
    private class ShortenFullyQualifiedMemberReferences extends ShortenFullyQualifiedReference {
        private final JavaType memberToShorten;

        @Override
        public J visitMethodInvocation(J.MethodInvocation methodInvocation, P p) {
            J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(methodInvocation, p);
            if (!(memberToShorten instanceof JavaType.Method)) {
                return mi;
            }
            JavaType.Method m = (JavaType.Method) memberToShorten;
            JavaType.FullyQualified targetType = m.getDeclaringType();

            if (isFullyQualifiedClassReference(mi.getSelect(), targetType.getFullyQualifiedName()) && mi.getSimpleName().equals(m.getName())) {
                return methodInvocation.withSelect(null);
            }
            return mi;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
            if (!(memberToShorten instanceof JavaType.Variable)) {
                return super.visitFieldAccess(fieldAccess, p);
            }
            JavaType.Variable var = (JavaType.Variable) memberToShorten;
            JavaType.FullyQualified targetType = (JavaType.FullyQualified) var.getOwner();
            if (targetType != null) {
                if (fieldAccess.getTarget() instanceof J.FieldAccess) {
                    J.FieldAccess target = (J.FieldAccess) fieldAccess.getTarget();
                    if (target.isFullyQualifiedClassReference(targetType.getFullyQualifiedName())) {
                        return fieldAccess.getName().withPrefix(fieldAccess.getPrefix());
                    }
                }
            }
            return super.visitFieldAccess(fieldAccess, p);
        }
    }

    private abstract class ShortenFullyQualifiedReference extends JavaVisitor<P> {
        @Override
        public J visitImport(J.Import _import, P p) {
            return _import;
        }

        @Override
        protected JavadocVisitor<P> getJavadocVisitor() {
            return new JavadocVisitor<P>(new JavaVisitor<>()) {
                @Override
                public Javadoc visitReference(Javadoc.Reference reference, P p) {
                    return reference;
                }
            };
        }

        protected boolean isFullyQualifiedClassReference(@Nullable Expression expr, String className) {
            if (expr instanceof J.FieldAccess) {
                return ((J.FieldAccess) expr).isFullyQualifiedClassReference(className);
            }
            if (expr instanceof J.Identifier) {
                J.Identifier id = (J.Identifier) expr;
                return id.getFieldType() == null && id.getSimpleName().equals(className);
            }
            return false;
        }
    }
}
