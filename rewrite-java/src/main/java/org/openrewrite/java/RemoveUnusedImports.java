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
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.Style;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.style.ImportLayoutStyle.isPackageAlwaysFolded;
import static org.openrewrite.java.tree.TypeUtils.fullyQualifiedNamesAreEqual;
import static org.openrewrite.java.tree.TypeUtils.toFullyQualifiedName;

/**
 * This recipe will remove any imports for types that are not referenced within the compilation unit. This recipe
 * is aware of the import layout style and will correctly handle unfolding of wildcard imports if the import counts
 * drop below the configured values.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveUnusedImports extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unused imports";
    }

    @Override
    public String getDescription() {
        return "Remove imports for types that are not referenced. As a precaution against incorrect changes no imports " +
                "will be removed from any source where unknown types are referenced. The most common cause of unknown " +
                "types is the use of annotation processors not supported by OpenRewrite, such as lombok.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-S1128");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new NoMissingTypes(), new RemoveUnusedImportsVisitor());
    }

    private static class RemoveUnusedImportsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            ImportLayoutStyle layoutStyle = Optional.ofNullable(Style.from(ImportLayoutStyle.class, cu))
                    .orElse(IntelliJ.importLayout());
            String sourcePackage = cu.getPackageDeclaration() == null ? "" :
                    cu.getPackageDeclaration().getExpression().printTrimmed(getCursor()).replaceAll("\\s", "");
            Map<String, TreeSet<String>> methodsAndFieldsByTypeName = new HashMap<>();
            Map<String, Set<JavaType.FullyQualified>> typesByPackage = new HashMap<>();

            for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                if (method.hasFlags(Flag.Static)) {
                    methodsAndFieldsByTypeName.computeIfAbsent(method.getDeclaringType().getFullyQualifiedName(), t -> new TreeSet<>())
                            .add(method.getName());
                }
            }

            for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getOwner());
                if (fq != null) {
                    methodsAndFieldsByTypeName.computeIfAbsent(fq.getFullyQualifiedName(), f -> new TreeSet<>())
                            .add(variable.getName());
                }
            }

            for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                if (javaType instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) javaType;
                    typesByPackage.computeIfAbsent(parameterized.getType().getPackageName(), f -> new HashSet<>())
                            .add(parameterized.getType());
                    for (JavaType typeParameter : parameterized.getTypeParameters()) {
                        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(typeParameter);
                        if (fq != null) {
                            typesByPackage.computeIfAbsent(
                                    fq.getOwningClass() == null ?
                                            fq.getPackageName() :
                                            toFullyQualifiedName(fq.getOwningClass().getFullyQualifiedName()),
                                    f -> new HashSet<>()).add(fq);
                        }
                    }
                } else if (javaType instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified fq = (JavaType.FullyQualified) javaType;
                    typesByPackage.computeIfAbsent(
                            fq.getOwningClass() == null ?
                                    fq.getPackageName() :
                                    toFullyQualifiedName(fq.getOwningClass().getFullyQualifiedName()),
                            f -> new HashSet<>()).add(fq);
                }
            }

            boolean changed = false;

            // the key is a list because a star import may get replaced with multiple unfolded imports
            List<ImportUsage> importUsage = new ArrayList<>(cu.getPadding().getImports().size());
            for (JRightPadded<J.Import> anImport : cu.getPadding().getImports()) {
                // assume initially that all imports are unused
                ImportUsage singleUsage = new ImportUsage();
                singleUsage.imports.add(anImport);
                importUsage.add(singleUsage);
            }

            // Collects all annotation imports and translates them to full name qualifiers using the imports collection
            final Set<String> annotationImports = new HashSet<>();
            final Set<J.Annotation> annotations = findAllAnnotations(cu);

            if (isNotEmpty(annotations)) {
                for (J.Annotation annotation : annotations) {
                    if (isNotEmpty(annotation.getArguments())) {
                        for (Expression expression : annotation.getArguments()) {
                            if (expression instanceof J.NewArray) {
                                J.NewArray newArray = (J.NewArray) expression;
                                for (Expression initializer : newArray.getInitializer()) {
                                    String className = null;
                                    if (initializer instanceof J.FieldAccess) {
                                        J.FieldAccess fieldAccess = (J.FieldAccess) initializer;
                                        className = fieldAccess.getTarget().print(getCursor());
                                    } else if (initializer instanceof J.Literal) {
                                        J.Literal literal = (J.Literal) initializer;
                                        className = literal.getValueSource();
                                    }
                                    if (className != null) {
                                        final String member = JavaType.ShallowClass.build(className).getClassName();
                                        final String packageName = JavaType.ShallowClass.build(className).getPackageName().isEmpty() ? getPackageName(importUsage, member) : JavaType.ShallowClass.build(className).getPackageName();
                                        if (!packageName.isEmpty()) {
                                            try {
                                                final J.Import importToAdd = new J.Import(randomId(),
                                                        Space.EMPTY,
                                                        Markers.EMPTY,
                                                        new JLeftPadded<>(Space.SINGLE_SPACE, Boolean.FALSE, Markers.EMPTY),
                                                        TypeTree.build(packageName + "." + member).withPrefix(Space.SINGLE_SPACE),
                                                        null);

                                                annotationImports.add(importToAdd.toString());
                                            } catch (Exception e) {
                                                throw new RecipeException("Cannot create import for class: {}", className, e);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // whenever an import statement is found to be used and not already in use it should be marked true
            Set<String> checkedImports = new HashSet<>();
            Set<String> usedWildcardImports = new HashSet<>();
            Set<String> usedStaticWildcardImports = new HashSet<>();
            for (ImportUsage anImport : importUsage) {
                J.Import elem = anImport.imports.get(0).getElement();
                J.FieldAccess qualid = elem.getQualid();
                J.Identifier name = qualid.getName();

                if (checkedImports.contains(elem.toString())) {
                    anImport.used = false;
                    changed = true;
                } else if (annotationImports.contains(elem.toString())) {
                    anImport.used = true;
                } else if (elem.isStatic()) {
                    String outerType = elem.getTypeName();
                    SortedSet<String> methodsAndFields = methodsAndFieldsByTypeName.get(outerType);

                    // some class names are not handled properly by `getTypeName()`
                    // see https://github.com/openrewrite/rewrite/issues/1698 for more detail
                    String target = qualid.getTarget().toString();
                    String modifiedTarget = methodsAndFieldsByTypeName.keySet().stream()
                            .filter(fqn -> fullyQualifiedNamesAreEqual(target, fqn))
                            .findFirst()
                            .orElse(target);
                    SortedSet<String> targetMethodsAndFields = methodsAndFieldsByTypeName.get(modifiedTarget);

                    Set<JavaType.FullyQualified> staticClasses = null;
                    for (JavaType.FullyQualified maybeStatic : typesByPackage.getOrDefault(target, emptySet())) {
                        if (maybeStatic.getOwningClass() != null && outerType.startsWith(maybeStatic.getOwningClass().getFullyQualifiedName())) {
                            if (staticClasses == null) {
                                staticClasses = new HashSet<>();
                            }
                            staticClasses.add(maybeStatic);
                        }
                    }

                    if (methodsAndFields == null && targetMethodsAndFields == null && staticClasses == null) {
                        anImport.used = false;
                        changed = true;
                    } else if ("*".equals(qualid.getSimpleName())) {
                        if (isPackageAlwaysFolded(layoutStyle.getPackagesToFold(), elem)) {
                            anImport.used = true;
                            usedStaticWildcardImports.add(elem.getTypeName());
                        } else if (((methodsAndFields == null ? 0 : methodsAndFields.size()) +
                                (staticClasses == null ? 0 : staticClasses.size())) < layoutStyle.getNameCountToUseStarImport()) {
                            // replacing the star with a series of unfolded imports
                            anImport.imports.clear();

                            // add each unfolded import
                            if (methodsAndFields != null) {
                                for (String method : methodsAndFields) {
                                    anImport.imports.add(new JRightPadded<>(elem
                                            .withQualid(qualid.withName(name.withSimpleName(method)))
                                            .withPrefix(Space.format("\n")), Space.EMPTY, Markers.EMPTY));
                                }
                            }

                            if (staticClasses != null) {
                                for (JavaType.FullyQualified fqn : staticClasses) {
                                    anImport.imports.add(new JRightPadded<>(elem
                                            .withQualid(qualid.withName(name.withSimpleName(fqn.getClassName().contains(".") ? fqn.getClassName().substring(fqn.getClassName().lastIndexOf(".") + 1) : fqn.getClassName())))
                                            .withPrefix(Space.format("\n")), Space.EMPTY, Markers.EMPTY));
                                }
                            }

                            // move whatever the original prefix of the star import was to the first unfolded import
                            anImport.imports.set(0, anImport.imports.get(0).withElement(anImport.imports.get(0)
                                    .getElement().withPrefix(elem.getPrefix())));

                            changed = true;
                        } else {
                            usedStaticWildcardImports.add(elem.getTypeName());
                        }
                    } else if (staticClasses != null && staticClasses.stream().anyMatch(c -> elem.getTypeName().equals(c.getFullyQualifiedName())) ||
                            (methodsAndFields != null && methodsAndFields.contains(qualid.getSimpleName())) ||
                            (targetMethodsAndFields != null && targetMethodsAndFields.contains(qualid.getSimpleName()))) {
                        anImport.used = true;
                    } else {
                        anImport.used = false;
                        changed = true;
                    }
                } else {
                    String target = qualid.getTarget().toString();
                    Set<JavaType.FullyQualified> types = typesByPackage.getOrDefault(target, new HashSet<>());
                    Set<JavaType.FullyQualified> typesByFullyQualifiedClassPath = typesByPackage.getOrDefault(toFullyQualifiedName(target), new HashSet<>());
                    Set<String> topLevelTypeNames = Stream.concat(types.stream(), typesByFullyQualifiedClassPath.stream())
                            .filter(fq -> fq.getOwningClass() == null)
                            .map(JavaType.FullyQualified::getFullyQualifiedName)
                            .collect(Collectors.toSet());
                    Set<JavaType.FullyQualified> combinedTypes = Stream.concat(types.stream(), typesByFullyQualifiedClassPath.stream())
                            .filter(fq -> fq.getOwningClass() == null || !topLevelTypeNames.contains(fq.getOwningClass().getFullyQualifiedName()))
                            .collect(Collectors.toSet());
                    JavaType.FullyQualified qualidType = TypeUtils.asFullyQualified(elem.getQualid().getType());

                    // look into methods and check if a qualifier is used
                    if (cu.getClasses().stream().anyMatch(classDeclaration -> scanStatement(classDeclaration, qualid.getSimpleName()))) {
                        anImport.used = true;
                    } else if (combinedTypes.isEmpty() || sourcePackage.equals(elem.getPackageName()) && qualidType != null && !qualidType.getFullyQualifiedName().contains("$")) {
                        anImport.used = false;
                        changed = true;
                    } else if ("*".equals(elem.getQualid().getSimpleName())) {
                        if (isPackageAlwaysFolded(layoutStyle.getPackagesToFold(), elem)) {
                            anImport.used = true;
                            usedWildcardImports.add(elem.getPackageName());
                        } else if (combinedTypes.size() < layoutStyle.getClassCountToUseStarImport()) {
                            // replacing the star with a series of unfolded imports
                            anImport.imports.clear();

                            // add each unfolded import
                            combinedTypes.stream().map(JavaType.FullyQualified::getClassName).sorted().distinct().forEach(type ->
                                    anImport.imports.add(new JRightPadded<>(elem
                                            .withQualid(qualid.withName(name.withSimpleName(type.substring(type.lastIndexOf('.') + 1))))
                                            .withPrefix(Space.format("\n")), Space.EMPTY, Markers.EMPTY))
                            );

                            // move whatever the original prefix of the star import was to the first unfolded import
                            anImport.imports.set(0, anImport.imports.get(0).withElement(anImport.imports.get(0)
                                    .getElement().withPrefix(elem.getPrefix())));

                            changed = true;
                        } else {
                            usedWildcardImports.add(target);
                        }
                    } else if (combinedTypes.stream().noneMatch(c -> {
                        if ("*".equals(elem.getQualid().getSimpleName())) {
                            return elem.getPackageName().equals(c.getPackageName());
                        }
                        return fullyQualifiedNamesAreEqual(c.getFullyQualifiedName(), elem.getTypeName());
                    })) {
                        anImport.used = false;
                        changed = true;
                    }
                }
                checkedImports.add(elem.toString());
            }

            // Do not use direct imports that are imported by a wildcard import
            Set<String> ambiguousStaticImportNames = getAmbiguousStaticImportNames(cu);
            for (ImportUsage anImport : importUsage) {
                J.Import elem = anImport.imports.get(0).getElement();
                if (!"*".equals(elem.getQualid().getSimpleName())) {
                    if (elem.isStatic()) {
                        if (usedStaticWildcardImports.contains(elem.getTypeName()) &&
                                !ambiguousStaticImportNames.contains(elem.getQualid().getSimpleName())) {
                            anImport.used = false;
                            changed = true;
                        }
                    } else {
                        if (usedWildcardImports.size() == 1 && usedWildcardImports.contains(elem.getPackageName()) && !elem.getTypeName().contains("$") && !conflictsWithJavaLang(elem)) {
                            anImport.used = false;
                            changed = true;
                        }
                    }
                }
            }

            if (changed) {
                List<JRightPadded<J.Import>> imports = new ArrayList<>();
                Space lastUnusedImportSpace = null;
                for (ImportUsage anImportGroup : importUsage) {
                    if (anImportGroup.used) {
                        List<JRightPadded<J.Import>> importGroup = anImportGroup.imports;
                        for (int i = 0; i < importGroup.size(); i++) {
                            JRightPadded<J.Import> anImport = importGroup.get(i);
                            if (i == 0 && lastUnusedImportSpace != null && anImport.getElement().getPrefix().getLastWhitespace()
                                    .chars().filter(c -> c == '\n').count() <= 1) {
                                anImport = anImport.withElement(anImport.getElement().withPrefix(lastUnusedImportSpace));
                            }
                            imports.add(anImport);
                        }
                        lastUnusedImportSpace = null;
                    } else if (lastUnusedImportSpace == null) {
                        lastUnusedImportSpace = anImportGroup.imports.get(0).getElement().getPrefix();
                    }
                }

                cu = cu.getPadding().withImports(imports);
                if (cu.getImports().isEmpty() && !cu.getClasses().isEmpty()) {
                    cu = autoFormat(cu, cu.getClasses().get(0).getName(), ctx, getCursor().getParentOrThrow());
                }
            }

            return cu;
        }

        private static Set<J.Annotation> findAllAnnotations(J.CompilationUnit cu) {
            Set<J.Annotation> annotations = new HashSet<>();
            cu.getClasses().stream()
                    .map(cl -> cl.getLeadingAnnotations())
                    .flatMap(List::stream)
                    .forEach(annotations::add);
            cu.getTypesInUse().getTypesInUse().stream().forEach(type -> {
                if (type instanceof JavaType.Class) {
                    JavaType.Class aClass = (JavaType.Class) type;
                    if (aClass.getKind() == JavaType.FullyQualified.Kind.Annotation) {
                        FindAnnotations.find(cu, aClass.getFullyQualifiedName()).forEach(annotations::add);
                    }
                }
            });
            return annotations;
        }

        public static boolean scanStatements(List<Statement> statements, String simpleName) {
            if (isEmpty(statements)) {
                return false;
            }
            return statements.stream().anyMatch(statement -> scanStatement(statement, simpleName));
        }

        public static boolean scanStatement(@Nullable Statement statement, String simpleName) {
            if (statement == null || StringUtils.isBlank(simpleName)) {
                return false;
            }

            if (statement instanceof Expression) {
                Expression expression = (Expression) statement;
                return scanExpression(expression, simpleName);
            }
            if (statement instanceof J.ClassDeclaration) {
                J.ClassDeclaration classDeclaration = (J.ClassDeclaration) statement;
                return scanAnnotations(classDeclaration.getLeadingAnnotations(), simpleName) ||
                        scanStatement(classDeclaration.getBody(), simpleName);
            }
            if (statement instanceof J.Block) {
                J.Block block = (J.Block) statement;
                return scanStatements(block.getStatements(), simpleName);
            }
            if (statement instanceof J.Case) {
                J.Case aCase = (J.Case) statement;
                return scanStatements(aCase.getStatements(), simpleName);
            }
            if (statement instanceof J.If) {
                J.If ifStatement = (J.If) statement;
                return scanExpression(ifStatement.getIfCondition(), simpleName) ||
                        scanElse(ifStatement.getElsePart(), simpleName) ||
                        scanThen(ifStatement.getThenPart(), simpleName);
            }
            if (statement instanceof J.Switch) {
                J.Switch switchStatement = (J.Switch) statement;
                return scanStatements(switchStatement.getCases().getStatements(), simpleName) ||
                        scanExpression(switchStatement.getSelector(), simpleName);
            }
            if (statement instanceof J.Label) {
                J.Label label = (J.Label) statement;
                return label.getLabel().getSimpleName().equals(simpleName) ||
                        scanStatement(label.getStatement(), simpleName);
            }
            if (statement instanceof J.Synchronized) {
                J.Synchronized synchronizedStatement = (J.Synchronized) statement;
                return scanStatement(synchronizedStatement.getBody(), simpleName);
            }
            if (statement instanceof Loop) {
                Loop loop = (Loop) statement;
                return scanStatement(loop.getBody(), simpleName);
            }
            if (statement instanceof J.Try) {
                J.Try jTry = (J.Try) statement;
                return scanStatement(jTry.getBody(), simpleName) ||
                        scanCatches(jTry.getCatches(), simpleName) ||
                        scanFinally(jTry.getFinally(), simpleName);
            }
            if (statement instanceof J.Throw) {
                J.Throw aThrow = (J.Throw) statement;
                return scanExpression(aThrow.getException(), simpleName);
            }
            if (statement instanceof J.Return) {
                J.Return jReturn = (J.Return) statement;
                if (jReturn.getExpression() instanceof J.MethodInvocation) {
                    J.MethodInvocation methodInvocation = (J.MethodInvocation) jReturn.getExpression();
                    return scanStatement(methodInvocation, simpleName);
                }
            }
            if (statement instanceof J.VariableDeclarations) {
                J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) statement;
                return scanTypeTree(variableDeclarations.getTypeExpression(), simpleName) ||
                        variableDeclarations.getVariables().stream()
                                .anyMatch(namedVariable -> scanExpression(namedVariable.getInitializer(), simpleName));
            }
            if (statement instanceof J.MethodDeclaration) {
                J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) statement;
                return scanStatements(methodDeclaration.getParameters(), simpleName) ||
                        scanStatement(methodDeclaration.getBody(), simpleName);
            }
            return false;
        }

        private static boolean scanExpressions(@Nullable List<Expression> initializer, String simpleName) {
            if (isEmpty(initializer)) {
                return false;
            }
            return initializer.stream().anyMatch(expression -> scanExpression(expression, simpleName));
        }

        private static boolean scanExpression(@Nullable Expression expression, String simpleName) {
            if (expression == null || StringUtils.isBlank(simpleName)) {
                return false;
            }
            if (expression instanceof TypeTree) {
                TypeTree typeTree = (TypeTree) expression;
                return scanTypeTree(typeTree, simpleName);
            }
            if (expression instanceof J.NewArray) {
                J.NewArray newArray = (J.NewArray) expression;
                if (newArray.getType() instanceof JavaType.Array) {
                    JavaType.Array arrayType = (JavaType.Array) newArray.getType();
                    if (arrayType.getElemType() instanceof JavaType.Class) {
                        JavaType.Class classType = (JavaType.Class) arrayType.getElemType();
                        return simpleName.equals(classType.getClassName()) ||
                                scanExpressions(newArray.getInitializer(), simpleName);
                    }

                }
                return scanExpressions(newArray.getInitializer(), simpleName);
            }
            if (expression instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) expression;
                return scanExpression(assignment.getVariable(), simpleName) ||
                        scanExpression(assignment.getAssignment(), simpleName);
            }
            if (expression instanceof J.Unary) {
                J.Unary unary = (J.Unary) expression;
                return scanExpression(unary.getExpression(), simpleName);
            }
            if (expression instanceof J.Binary) {
                J.Binary binary = (J.Binary) expression;
                return scanExpression(binary.getRight(), simpleName) ||
                        scanExpression(binary.getLeft(), simpleName);
            }
            if (expression instanceof J.Ternary) {
                J.Ternary ternary = (J.Ternary) expression;
                return scanExpression(ternary.getCondition(), simpleName) ||
                        scanExpression(ternary.getTruePart(), simpleName) ||
                        scanExpression(ternary.getFalsePart(), simpleName);
            }
            if (expression instanceof J.Annotation) {
                J.Annotation annotation = (J.Annotation) expression;
                return scanAnnotation(annotation, simpleName);
            }
            if (expression instanceof J.Lambda) {
                J.Lambda lambda = (J.Lambda) expression;
                if (lambda.getBody() instanceof J.Block) {
                    J.Block jBlock = (J.Block) lambda.getBody();
                    return scanStatement(jBlock, simpleName);
                }
            }
            if (expression instanceof J.Literal) {
                J.Literal literal = (J.Literal) expression;
                return !StringUtils.isBlank(literal.getValueSource()) && literal.getValueSource().equals(simpleName);
            }
            if (expression instanceof J.MethodInvocation) {
                J.MethodInvocation invocation = (J.MethodInvocation) expression;
                return scanExpression(invocation.getSelect(), simpleName) ||
                        scanArguments(invocation.getArguments(), simpleName);
            }
            if (expression instanceof MethodCall) {
                MethodCall methodCall = (MethodCall) expression;
                return scanArguments(methodCall.getArguments(), simpleName);
            }
            if (expression instanceof J.AssignmentOperation) {
                J.AssignmentOperation assignmentOperation = (J.AssignmentOperation) expression;
                return scanExpression(assignmentOperation.getVariable(), simpleName) ||
                        scanExpression(assignmentOperation.getAssignment(), simpleName);
            }
            return false;
        }

        private static boolean scanTypeTree(@Nullable TypeTree typeExpression, String simpleName) {
            if (typeExpression == null) {
                return false;
            }

            if (typeExpression instanceof J.Identifier) {
                J.Identifier identifier = (J.Identifier) typeExpression;
                return identifier.getSimpleName().equals(simpleName) ||
                        scanAnnotations(identifier.getAnnotations(), simpleName);
            }
            if (typeExpression instanceof J.ArrayType) {
                J.ArrayType arrayType = (J.ArrayType) typeExpression;
                return scanAnnotations(arrayType.getAnnotations(), simpleName) ||
                        scanTypeTree(arrayType.getElementType(), simpleName);
            }
            if (typeExpression instanceof J.FieldAccess) {
                J.FieldAccess fieldAccess = (J.FieldAccess) typeExpression;
                return simpleName.equals(fieldAccess.getSimpleName()) ||
                        scanExpression(fieldAccess.getTarget(), simpleName);
            }
            if (typeExpression instanceof J.AnnotatedType) {
                J.AnnotatedType annotatedType = (J.AnnotatedType) typeExpression;
                return scanAnnotations(annotatedType.getAnnotations(), simpleName);
            }
            if (typeExpression instanceof J.ParameterizedType) {
                J.ParameterizedType parameterizedType = (J.ParameterizedType) typeExpression;
                if (isEmpty(parameterizedType.getTypeParameters())) {
                    return false;
                }
                return scanTypeParameters(parameterizedType.getTypeParameters(), simpleName);
            }
            return false;
        }

        private static boolean scanThen(Statement thenPart, String simpleName) {
            return scanStatement(thenPart, simpleName);
        }

        private static boolean scanElse(@Nullable J.If.Else elsePart, String simpleName) {
            if (elsePart == null) {
                return false;
            }
            return scanStatement(elsePart.getBody(), simpleName);
        }

        private static boolean scanFinally(@Nullable J.Block aFinally, String simpleName) {
            if (aFinally == null || isEmpty(aFinally.getStatements())) {
                return false;
            }
            return scanStatements(aFinally.getStatements(), simpleName);
        }

        // It seems that scanning inside annotation of classes is somehow limited not to recognize everything
        private static boolean scanAnnotations(@Nullable List<J.Annotation> annotations, String simpleName) {
            if (isEmpty(annotations)) {
                return false;
            }
            return annotations.stream().anyMatch(annotation -> scanAnnotation(annotation, simpleName));
        }

        private static boolean scanAnnotation(J.Annotation annotation, String simpleName) {
            if (annotation.getType() instanceof JavaType.Class) {
                JavaType.Class aClass = (JavaType.Class) annotation.getType();
                return simpleName.equals(aClass.getClassName()) || scanArguments(annotation.getArguments(), simpleName);
            }
            return scanArguments(annotation.getArguments(), simpleName);
        }

        private static boolean scanTypeParameters(@Nullable List<Expression> typeParameters, String simpleName) {
            if (typeParameters == null || typeParameters.isEmpty()) {
                return false;
            }
            return typeParameters.stream().anyMatch(typeParameter -> scanExpression(typeParameter, simpleName));
        }

        private static boolean scanArguments(@Nullable List<Expression> arguments, String simpleName) {
            if (isEmpty(arguments)) {
                return false;
            }
            return arguments.stream().anyMatch(argument -> scanExpression(argument, simpleName));
        }

        private static boolean scanCatches(List<J.Try.Catch> catches, String simpleName) {
            return catches.stream().anyMatch(aCatch -> scanCatch(aCatch, simpleName));
        }

        private static boolean scanCatch(J.Try.Catch aCatch, String simpleName) {
            return scanStatement(aCatch.getBody(), simpleName);
        }

        private static String getPackageName(List<ImportUsage> importUsage, String member) {
            for (ImportUsage anImport : importUsage) {
                J.Import elem = anImport.imports.get(0).getElement();
                if (member.equals(elem.getClassName())) {
                    return elem.getPackageName();
                }
            }
            return "";
        }

        private static Set<String> getAmbiguousStaticImportNames(J.CompilationUnit cu) {
            Set<String> typesWithWildcardImport = new HashSet<>();
            for (J.Import elem : cu.getImports()) {
                if ("*".equals(elem.getQualid().getSimpleName())) {
                    typesWithWildcardImport.add(elem.getTypeName());
                }
            }
            Set<JavaType.FullyQualified> qualifiedTypes = new HashSet<>();
            for (JavaType.Variable variable : cu.getTypesInUse().getVariables()) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getOwner());
                if (fq != null && typesWithWildcardImport.contains(fq.getFullyQualifiedName())) {
                    qualifiedTypes.add(fq);
                }
            }
            Set<String> seen = new HashSet<>();
            Set<String> ambiguous = new HashSet<>();
            for (JavaType.FullyQualified fq : qualifiedTypes) {
                for (JavaType.Variable member : fq.getMembers()) {
                    if (!seen.add(member.getName())) {
                        ambiguous.add(member.getName());
                    }
                }
            }
            return ambiguous;
        }

        private static final Set<String> JAVA_LANG_CLASS_NAMES = new HashSet<>(Arrays.asList(
                "AbstractMethodError",
                "Appendable",
                "ArithmeticException",
                "ArrayIndexOutOfBoundsException",
                "ArrayStoreException",
                "AssertionError",
                "AutoCloseable",
                "Boolean",
                "BootstrapMethodError",
                "Byte",
                "Character",
                "CharSequence",
                "Class",
                "ClassCastException",
                "ClassCircularityError",
                "ClassFormatError",
                "ClassLoader",
                "ClassNotFoundException",
                "ClassValue",
                "Cloneable",
                "CloneNotSupportedException",
                "Comparable",
                "Deprecated",
                "Double",
                "Enum",
                "EnumConstantNotPresentException",
                "Error",
                "Exception",
                "ExceptionInInitializerError",
                "Float",
                "FunctionalInterface",
                "IllegalAccessError",
                "IllegalAccessException",
                "IllegalArgumentException",
                "IllegalCallerException",
                "IllegalMonitorStateException",
                "IllegalStateException",
                "IllegalThreadStateException",
                "IncompatibleClassChangeError",
                "IndexOutOfBoundsException",
                "InheritableThreadLocal",
                "InstantiationError",
                "InstantiationException",
                "Integer",
                "InternalError",
                "InterruptedException",
                "Iterable",
                "LayerInstantiationException",
                "LinkageError",
                "Long",
                "MatchException",
                "Math",
                "Module",
                "ModuleLayer",
                "NegativeArraySizeException",
                "NoClassDefFoundError",
                "NoSuchFieldError",
                "NoSuchFieldException",
                "NoSuchMethodError",
                "NoSuchMethodException",
                "NullPointerException",
                "Number",
                "NumberFormatException",
                "Object",
                "OutOfMemoryError",
                "Override",
                "Package",
                "Process",
                "ProcessBuilder",
                "ProcessHandle",
                "Readable",
                "Record",
                "ReflectiveOperationException",
                "Runnable",
                "Runtime",
                "RuntimeException",
                "RuntimePermission",
                "SafeVarargs",
                "ScopedValue",
                "SecurityException",
                "SecurityManager",
                "Short",
                "StackOverflowError",
                "StackTraceElement",
                "StackWalker",
                "StrictMath",
                "String",
                "StringBuffer",
                "StringBuilder",
                "StringIndexOutOfBoundsException",
                "StringTemplate",
                "SuppressWarnings",
                "System",
                "Thread",
                "ThreadDeath",
                "ThreadGroup",
                "ThreadLocal",
                "Throwable",
                "TypeNotPresentException",
                "UnknownError",
                "UnsatisfiedLinkError",
                "UnsupportedClassVersionError",
                "UnsupportedOperationException",
                "VerifyError",
                "VirtualMachineError",
                "Void",
                "WrongThreadException"
        ));

        private static boolean conflictsWithJavaLang(J.Import elem) {
            return JAVA_LANG_CLASS_NAMES.contains(elem.getClassName());
        }

        private static boolean isEmpty(@Nullable Collection<?> collection) {
            return collection == null || collection.isEmpty();
        }

        private static boolean isNotEmpty(@Nullable Collection<?> collection) {
            return !isEmpty(collection);
        }
    }

    private static class ImportUsage {
        final List<JRightPadded<J.Import>> imports = new ArrayList<>();
        boolean used = true;
    }
}
