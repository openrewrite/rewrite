/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin;

import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.style.ImportLayoutStyle;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.GeneralFormatStyle;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

/**
 * A Kotlin refactoring visitor that can be used to add imports to a given compilation unit.
 * This visitor can also be configured to only add the import if the imported class / member is referenced within the
 * compilation unit.
 * <p>
 * The {@link AddImport#fullyQualifiedName} must be supplied and represents a fully qualified class name.
 * <p>
 * The {@link AddImport#member} is an optional member within the imported type. It can be set to "*"
 * to represent a wildcard import.
 * <p>
 * The import can optionally also be configured with an alias.
 * <p>
 * The {@link AddImport#onlyIfReferenced} is a flag (defaulted to {@code true}) to indicate if the import should only be added
 * if there is a reference to the imported class / member.
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddImport<P> extends KotlinIsoVisitor<P> {

    private static final Set<String> IMPLICITLY_IMPORTED_PACKAGES = unmodifiableSet(new HashSet<>(Arrays.asList(
            "kotlin",
            "kotlin.annotation",
            "kotlin.collections",
            "kotlin.comparisons",
            "kotlin.io",
            "kotlin.ranges",
            "kotlin.sequences",
            "kotlin.text",
            "kotlin.math",
            "java.lang"
    )));

    @Nullable
    private final String packageName;

    private final String typeName;

    @EqualsAndHashCode.Include
    private final String fullyQualifiedName;

    @EqualsAndHashCode.Include
    @Nullable
    private final String member;

    @EqualsAndHashCode.Include
    @Nullable
    private final String alias;

    @EqualsAndHashCode.Include
    private final boolean onlyIfReferenced;

    public AddImport(@Nullable String packageName, String typeName, @Nullable String member, @Nullable String alias, boolean onlyIfReferenced) {
        this.packageName = packageName == null || packageName.isEmpty() ? null : packageName;
        this.typeName = typeName.replace('.', '$');
        this.fullyQualifiedName = packageName == null ? typeName : packageName + "." + typeName;
        this.member = member;
        this.alias = alias;
        this.onlyIfReferenced = onlyIfReferenced;
    }

    @Override
    public @Nullable J preVisit(J tree, P p) {
        stopAfterPreVisit();
        J j = tree;
        if (packageName != null && tree instanceof K.CompilationUnit) {
            K.CompilationUnit cu = (K.CompilationUnit) tree;
            if (JavaType.Primitive.fromKeyword(fullyQualifiedName) != null) {
                return cu;
            }

            if (alias == null) {
                // No need to add imports if the class to import is implicitly imported, or if the classes are within the same package
                if ((IMPLICITLY_IMPORTED_PACKAGES.contains(packageName) && StringUtils.isBlank(member)) ||
                    (cu.getPackageDeclaration() != null && packageName.equals(cu.getPackageDeclaration().getExpression().printTrimmed(getCursor())))) {
                    return cu;
                }
            }

            if (onlyIfReferenced && !hasReference(cu)) {
                return cu;
            }

            if (cu.getImports().stream().anyMatch(i -> {
                String ending = i.getQualid().getSimpleName();

                String alias1 = Optional.ofNullable(i.getAlias()).map(J.Identifier::getSimpleName).orElse("");
                String alias2 = alias != null ? alias : "";
                if (!alias1.equals(alias2)) {
                    return false;
                }

                if (member == null) {
                    return i.getPackageName().equals(packageName) &&
                           (ending.equals(typeName) || "*".equals(ending));
                }
                return i.getPackageName().equals(fullyQualifiedName) &&
                       (ending.equals(member) || "*".equals(ending));
            })) {
                return cu;
            }

            J.Import importToAdd = new J.Import(randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new JLeftPadded<>(Space.EMPTY, member != null, Markers.EMPTY),
                    TypeTree.build(fullyQualifiedName +
                                   (member == null ? "" : "." + member), '`').withPrefix(Space.SINGLE_SPACE),
                    alias != null ? new JLeftPadded<>(
                            Space.SINGLE_SPACE,
                            new J.Identifier(
                                    randomId(),
                                    Space.SINGLE_SPACE,
                                    Markers.EMPTY,
                                    emptyList(),
                                    alias,
                                    null,
                                    null
                            ),
                            Markers.EMPTY
                    ) : null
            );

            List<JRightPadded<J.Import>> imports = new ArrayList<>(cu.getPadding().getImports());

            if (imports.isEmpty() && !cu.getClasses().isEmpty()) {
                if (cu.getPackageDeclaration() == null) {
                    // leave javadocs on the class and move other comments up to the import
                    // (which could include license headers and the like)
                    String whitespace = "";
                    if (!cu.getAnnotations().isEmpty()) {
                        // The 1st import added after annotation needs to be in a new line
                        whitespace = "\n\n";
                    }
                    Space firstStatementPrefix = cu.getStatements().get(0).getPrefix();
                    importToAdd = importToAdd.withPrefix(firstStatementPrefix
                            .withComments(ListUtils.map(firstStatementPrefix.getComments(), comment -> comment instanceof Javadoc ? null : comment))
                            .withWhitespace(whitespace));

                    cu = cu.withStatements(ListUtils.mapFirst(cu.getStatements(), stmt ->
                            stmt.withComments(ListUtils.map(stmt.getComments(), comment -> comment instanceof Javadoc ? comment : null))
                    ));
                }
            }

            ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                    .orElse(IntelliJ.importLayout());

            List<JavaType.FullyQualified> classpath = cu.getMarkers().findFirst(JavaSourceSet.class)
                    .map(JavaSourceSet::getClasspath)
                    .orElse(emptyList());

            List<JRightPadded<J.Import>> newImports = layoutStyle.addImport(cu.getPadding().getImports(), importToAdd, cu.getPackageDeclaration(), classpath);

            // ImportLayoutStyle::addImport adds always `\n` as newlines. Checking if we need to fix them
            GeneralFormatStyle generalFormatStyle = Optional.ofNullable(cu.getStyle(GeneralFormatStyle.class))
              .orElse(autodetectGeneralFormatStyle(cu));
            newImports = checkCRLF(newImports, generalFormatStyle);

            cu = cu.getPadding().withImports(newImports);

            // make sure first statement has a prefix if necessary
            if (((K.CompilationUnit) tree).getImports().isEmpty() || ((K.CompilationUnit) tree).getPackageDeclaration() == null) {
                cu = cu.withStatements(ListUtils.mapFirst(cu.getStatements(), stmt ->
                  stmt.getPrefix().isEmpty() ? stmt.withPrefix(stmt.getPrefix().withWhitespace(generalFormatStyle.isUseCRLFNewLines() ? "\r\n\r\n" : "\n\n")) : stmt));
            }

            j = cu;
        }
        return j;
    }

    // TODO refactor ImportLayoutStyle so that this method can be removed
    private List<JRightPadded<J.Import>> checkCRLF(List<JRightPadded<J.Import>> newImports, GeneralFormatStyle generalFormatStyle) {
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
                JavaType.Class classType = JavaType.ShallowClass.build(fullyQualifiedName);
                boolean foundReference = false;
                boolean usingAlias = false;
                if (t instanceof J.ParameterizedType) {
                    J.ParameterizedType pt = (J.ParameterizedType) t;
                    if (pt.getClazz() instanceof J.Identifier) {
                        String nameInSource = ((J.Identifier) pt.getClazz()).getSimpleName();
                        if (alias != null) {
                            if ( nameInSource.equals(alias)) {
                                usingAlias = true;
                            }
                        } else if (nameInSource.equals(classType.getClassName())) {
                            foundReference = true;
                        }
                    }
                } else if (t instanceof J.Identifier) {
                    String nameInSource = ((J.Identifier) t).getSimpleName();
                    if (alias != null) {
                        if ( nameInSource.equals(alias)) {
                            usingAlias = true;
                        }
                    } else if (nameInSource.equals(classType.getClassName())) {
                        foundReference = true;
                    }
                } else {
                    foundReference = true;
                }

                if (usingAlias) {
                    return true;
                }

                if (foundReference && (!(t instanceof J.FieldAccess) || !((J.FieldAccess) t).isFullyQualifiedClassReference(fullyQualifiedName)) &&
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
        new AddImport<P>.FindStaticFieldAccess().visit(compilationUnit, hasStaticFieldAccess);
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
