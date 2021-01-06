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

import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaPrinter;
import org.openrewrite.java.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class JavaTemplate {

    private static final Logger logger = LoggerFactory.getLogger(JavaTemplate.class);

    private final JavaParser parser;
    private final String code;
    private final int parameterCount;
    private final Set<String> imports;
    private final boolean autoFormat;
    private final String parameterMarker;

    private JavaTemplate(JavaParser parser, String code, Set<String> imports, boolean autoFormat, String parameterMarker) {
        this.parser = parser;
        this.code = code;
        this.parameterCount = StringUtils.countOccurrences(code, parameterMarker);
        this.imports = imports;
        this.autoFormat = autoFormat;
        this.parameterMarker = parameterMarker;
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    public <J2 extends J> List<J2> generate(@NonNull Cursor insertionScope, Object... parameters) {

        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        J.CompilationUnit compilationUnit = insertionScope.firstEnclosing(J.CompilationUnit.class);
        //Collect any class declarations in the compilation unit, we must also collect any arbitrary nested classes
        Set<J.ClassDecl> localClasses = extractClassesFromCompilationUnit(compilationUnit);
        //Collect types references in any parameters that are Trees.
        Set<JavaType> parameterTypes = extractTypesFromParameters(parameters);

        //TODO Can possibly scope "extractTypesFromParameters to JUST method types, as that is the only type
        //that is acted upon below. Leaving this for now, because if a parameter is from some other AST (not
        //related to the current compilation unit) we may want to add some additional imports.
        Set<String> allImports = new HashSet<>(imports);

        //Compute any local methods that need to be stubbed from the extracted types. This maps the ID of the class's
        //body elements to a list of methods that need to be stubbed.
        Map<UUID, List<JavaType.Method>> localMethods = new HashMap<>();
        for (JavaType type : parameterTypes) {
            if (type instanceof JavaType.Method) {
                JavaType.Method method = (JavaType.Method) type;
                if (method.getDeclaringType() == null) {
                    continue;
                }
                //Keep track of any methods that are declared on a local class (those will need to have a stub
                //generated for them)
                localClasses.stream()
                        .filter(c -> c.getType() != null && c.getType().equals(method.getDeclaringType()))
                        .findFirst()
                        .ifPresent(
                                classDeclaration -> localMethods.computeIfAbsent(
                                        classDeclaration.getBody().getId(), c -> new ArrayList<>()).add(method)
                        );
            }
        }

        //TODO Extract any variables that need to be defined in scope. Might be able to do some of this work in PrintSnippet.

        //Substitute parameters into the template.
        String printedInsertionScope = substituteParameters(parameters);

        //Collect a list of IDs that represent the path from the insertion point back to the compiliation unit. This
        //list is used in the PrintSnippet to figure out where to traverse the tree as it prints.
        List<UUID> pathIds = new ArrayList<>();
        Cursor index = insertionScope;
        while (index != null && !(index.getTree() instanceof J.CompilationUnit)) {
            pathIds.add(index.getTree().getId());
            index = index.getParent();
        }

        printedInsertionScope = new PrintSnippet(allImports, pathIds, insertionScope.getTree().getId(), localMethods)
                .visit(compilationUnit, printedInsertionScope);

        if (logger.isDebugEnabled()) {
            logger.debug("Building code snippet using synthetic class:\n=============\n{}\n=============", printedInsertionScope);
        }

        parser.reset();
        J.CompilationUnit cu = parser
                .parse(printedInsertionScope)
                .iterator().next();

//        if (autoFormat) {
//            // TODO format the new tree
//        }

//        //TODO extract just the desired snippet elements
//        ExtractTemplatedCode extractTemplatedCode = new ExtractTemplatedCode();
//        extractTemplatedCode.visit(cu, null);
//
//        //noinspection unchecked
//        return (List<J2>) extractTemplatedCode.templated;
        return null;
    }

    /**
     * This method extracts ALL classes (and nested classes) from a compilation unit
     *
     * @param cu The original compilation unit
     * @return A set of classes declared in the compilation unit.
     */
    private static Set<J.ClassDecl> extractClassesFromCompilationUnit(J.CompilationUnit cu) {
        HashSet<J.ClassDecl> extracted = new HashSet<>();
        for (J.ClassDecl classDecl : cu.getClasses()) {
            extracted.add(classDecl);
            collectClasses(classDecl, extracted);
        }
        return extracted;
    }
    private static void collectClasses(J.ClassDecl classDecl, HashSet<J.ClassDecl> extracted) {
        extracted.add(classDecl);
        if (classDecl.getBody().getStatements() != null) {
            classDecl.getBody().getStatements().stream()
                    .filter(s -> s.getElem() instanceof J.ClassDecl)
                    .map(s -> (J.ClassDecl) s.getElem())
                    .forEach(nested -> collectClasses(nested, extracted));
        }
    }

    /**
     * This method extracts JavaTypes from any parameters that are themselves Trees. This extracts both
     * JavaType.FullyQualified and JavaType.Method from the parameters.
     *
     * @param parameters Parameters passed into the template.
     * @return A list of JavaTypes found within the parameters.
     */
    private static Set<JavaType> extractTypesFromParameters(Object[] parameters) {
        Set<JavaType> extracted = new HashSet<>();

        JavaIsoProcessor<Set<JavaType>> typeExtractor = new JavaIsoProcessor<Set<JavaType>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<JavaType> ctx) {
                if (method.getType() != null) {
                    ctx.add(method.getType());
                } else {
                    logger.warn("There is an invocation to a method [" + method.getSimpleName()
                            + "] within the original insertion scope that does not have type information.");
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, Set<JavaType> ctx) {
                if (newClass.getType() != null) {
                    ctx.add(newClass.getType());
                }
                return super.visitNewClass(newClass, ctx);
            }

            @Override
            public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, Set<JavaType> ctx) {
                if (variable.getType() != null) {
                    ctx.add(variable.getType());
                }
                return super.visitVariable(variable, ctx);
            }
        };

        for (Object parameter : parameters) {
            if (parameter instanceof J) {
                typeExtractor.visit((J) parameter, extracted);
            }
        }

        return extracted;
    }

    /**
     * A custom JavaPrinter that will print the snippet from the template and then insert that snippet into the
     * insertion point from the original AST. This printer will only navigate the tree along the insertion scope's
     * path. It also prints any class declarations that are reachable from the compiliation unit (including nested
     * classes) and generate method stubs for any methods that are referenced from parameters passed into the generate
     * method.
     */
    private static class PrintSnippet extends JavaPrinter<String> {
        private final Set<String> imports;
        private final List<UUID> pathIds;
        private final Map<UUID, List<JavaType.Method>> localMethodStubs;

        /**
         * @param imports          Additional imports that should be added to the generated source.
         * @param pathIds          A list of path ID from the insertion point back to the compilation unit.
         * @param insertionPoint   The ID of element that IS the insertion point.
         * @param localMethodStubs A Map of element IDs (J.Block) to a list of method stubs that should be generated in that block
         */
        PrintSnippet(Set<String> imports, List<UUID> pathIds, UUID insertionPoint, Map<UUID, List<JavaType.Method>> localMethodStubs) {
            super(
                    new TreePrinter<J, String>() {
                        @Override
                        public String doLast(Tree tree, String printed, String s) {
                            if (tree.getId().equals(insertionPoint)) {
                                return s;
                            }
                            return printed;
                        }
                    }
            );
            this.imports = imports;
            this.pathIds = pathIds;
            this.localMethodStubs = localMethodStubs;
        }

        @Override
        public String visitCompilationUnit(J.CompilationUnit cu, String acc) {

            //Print all original imports from the compliation unit
            String originalImports = super.visit(cu.getImports(), ";", acc);

            StringBuilder output = new StringBuilder(originalImports.length() + acc.length() + 1024);
            output.append(originalImports);
            if (!cu.getImports().isEmpty()) {
                output.append(";");
            }

            output.append("\n\n//Additional Imports\n");
            for (String _import : imports) {
                output.append(_import);
            }

            //Visit the classes of the compilation unit.
            return output.append(visit(cu.getClasses(), acc)).append(visit(cu.getEof())).toString();
        }

        @Override
        public String visitBlock(J.Block block, String context) {
            StringBuilder acc = new StringBuilder();

            if (block.getStatic() != null) {
                acc.append("static ").append(visit(block.getStatic()));
            }
            acc.append("{\n");
            if (pathIds.contains(block.getId())) {
                acc.append(visitStatements(block.getStatements(), context));
            }

            //For Class blocks, generate any stubbed out methods
            List<JavaType.Method> methods = localMethodStubs.get(block.getId());
            if (methods != null) {
                acc.append("\n\n//Stubbed Methods\n").append(methods.stream().map(JavaTemplate::stubMethod)
                        .collect(Collectors.joining("\n", "", "\n")));
            }
            //Process any nested classes.
            for (JRightPadded<Statement> paddedStat : block.getStatements()) {
                if (paddedStat.getElem() instanceof J.ClassDecl) {
                    acc.append(visitClassDecl((J.ClassDecl) paddedStat.getElem(), context));
                }
            }
            acc.append(visit(block.getEnd())).append("}\n");

            return fmt(block, acc.toString());
        }

        private String visitStatements(List<JRightPadded<Statement>> statements, String context) {
            if (statements != null && statements.stream().anyMatch(s -> pathIds.contains(s.getElem().getId()))) {
                StringBuilder acc = new StringBuilder();
                for (JRightPadded<Statement> paddedStat : statements) {
                    acc.append(visitStatement(paddedStat, context));
                    if (pathIds.contains(paddedStat.getElem().getId())) {
                        break;
                    }
                }
                return acc.toString();
            } else {
                return context;
            }
        }

        private String visitStatement(JRightPadded<Statement> paddedStat, String context) {
            Statement statement = paddedStat.getElem();
            StringBuilder output = new StringBuilder();
            if (pathIds.contains(statement.getId())) {
                output.append(visit(paddedStat.getElem(), context) + visit(paddedStat.getAfter()));
            } else if (statement instanceof J.VariableDecls) {
                J.VariableDecls variableDecls = (J.VariableDecls) statement;
                output
                        .append(variableDecls.getTypeExpr().printTrimmed())
                        .append(variableDecls.getDimensionsBeforeName().stream().map(d -> "[]").reduce("", (r1,r2) -> r1+r2))
                        .append(" ")
                        .append(variableDecls.getVars().stream().map(v -> v.getElem().getSimpleName()).collect(Collectors.joining(",")));
            } else {
                return "";
            }

            while (true) {
                if (statement instanceof J.Assert ||
                        statement instanceof J.Assign ||
                        statement instanceof J.AssignOp ||
                        statement instanceof J.Break ||
                        statement instanceof J.Continue ||
                        statement instanceof J.DoWhileLoop ||
                        statement instanceof J.Empty ||
                        statement instanceof J.MethodInvocation ||
                        statement instanceof J.NewClass ||
                        statement instanceof J.Return ||
                        statement instanceof J.Throw ||
                        statement instanceof J.Unary ||
                        statement instanceof J.VariableDecls ||
                        statement instanceof J.MethodDecl && ((J.MethodDecl) statement).isAbstract()
                ) {

                    return output.append(";").toString();
                }

                if (statement instanceof J.Label) {
                    statement = ((J.Label) statement).getStatement();
                    continue;
                }

                return output.toString();
            }
        }
    }

    /**
     * Replace the parameter markers in the template with the parameters passed into the generate method.
     * Parameters that are Java Tree's will be correctly printed into the string. The parameters are not named and
     * this relies purely on ordinal position of the parameter.
     *
     * @param parameters A list of parameters
     * @return The final snippet to be generated.
     */
    private String substituteParameters(Object... parameters) {

        String codeInstance = code;
        for (Object parameter : parameters) {
            String value;
            if (parameter instanceof Tree) {
                value = ((Tree) parameter).printTrimmed();
            } else {
                value = parameter.toString();
            }
            codeInstance = StringUtils.replaceFirst(codeInstance, parameterMarker, value);
        }
        return codeInstance;
    }

    /**
     * This generates a String representation of a method declaration with an empty body based on the supplied JavaType.Method.
     * These stubs can be used to ensure that type attribution succeeds for methods included in snippets built into AST elements.
     *
     * @param method The method type for which a stub will be generated.
     * @return A snippet representing as method declaration of code that can be included as source in a synthetically generated class.
     */
    private static String stubMethod(JavaType.Method method) {
        StringBuilder methodStub = new StringBuilder(128);

        if (!method.getFlags().isEmpty()) {
            methodStub.append(method.getFlags().stream().map(Flag::getKeyword).collect(joining(" "))).append(" ");
        }

        JavaType returnType = method.getResolvedSignature() == null ? null : method.getResolvedSignature().getReturnType();

        if (returnType instanceof JavaType.FullyQualified
                || returnType instanceof JavaType.Array
                || returnType instanceof JavaType.Primitive) {

            methodStub.append(getTypeDeclaration(returnType)).append(" ");
        } else {
            methodStub.append("void ");
        }

        methodStub.append(method.getName()).append("(");
        int argIndex = 0;
        for (JavaType parameter : method.getResolvedSignature().getParamTypes()) {
            if (argIndex > 0) {
                methodStub.append(", ");
            }
            methodStub.append(getTypeDeclaration(parameter));
            methodStub.append(" arg").append(argIndex);
            argIndex++;
        }
        methodStub.append(") {\n");
        if (returnType instanceof JavaType.FullyQualified || returnType instanceof JavaType.Array) {
            methodStub.append("  return null;\n");
        } else if (returnType instanceof JavaType.Primitive) {
            methodStub.append("  return ").append(((JavaType.Primitive) returnType).getDefaultValue()).append(";\n");
        }
        methodStub.append("}\n");
        return methodStub.toString();
    }

    /**
     * Given a JaveType, generate the code declaration for that type. This method supports full-qualified, array, and
     * primitive declarations.
     *
     * @param type The JavaType that will be converted to a type declaration code snippet
     * @return code declaration for the type.
     */
    private static String getTypeDeclaration(@Nullable JavaType type) {

        if (type instanceof JavaType.Class) {
            JavaType.Class clazz = (JavaType.Class) type;
            StringBuilder typeDeclaration = new StringBuilder(50);
            typeDeclaration.append(clazz.getFullyQualifiedName());
            if (clazz.getTypeParameters() != null && !clazz.getTypeParameters().isEmpty()) {
                typeDeclaration
                        .append("<")
                        .append(clazz.getTypeParameters().stream().map(JavaTemplate::getTypeDeclaration).collect(joining(",")))
                        .append(">");
            }
            return typeDeclaration.toString();

        }
        if (type instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.Array) {
            JavaType elementType = ((JavaType.Array) type).getElemType();
            if (elementType instanceof JavaType.FullyQualified) {
                return ((JavaType.FullyQualified) elementType).getFullyQualifiedName() + "[]";
            } else if (elementType instanceof JavaType.Primitive) {
                return ((JavaType.Primitive) elementType).getKeyword() + "[]";
            } else {
                throw new IllegalArgumentException("Cannot resolve the array type declaration.");
            }
        } else if (type instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type).getKeyword();
        } else {
            throw new IllegalArgumentException("Cannot resolve type declaration.");
        }
    }

    private static class ExtractTemplatedCode extends JavaProcessor<ExecutionContext> {
        private long templateDepth = -1;
        private final List<J> templated = new ArrayList<>();

        public ExtractTemplatedCode() {
            setCursoringOn();
        }

        @Override
        public J visitEach(J tree, ExecutionContext context) {
            Comment startToken = findMarker(tree, "<<<<START>>>>");
            if (startToken != null) {
                templateDepth = getCursor().getPathAsStream().count();

                List<Comment> comments = new ArrayList<>(tree.getPrefix().getComments());
                comments.remove(startToken);

                templated.add(tree.withPrefix(tree.getPrefix().withComments(comments)));
            } else if (!templated.isEmpty() && getCursor().getPathAsStream().count() == templateDepth) {
                templated.add(tree);
            } else if (findMarker(tree, "<<<<STOP>>>>") != null) {
                return tree;
            }

            return super.visitEach(tree, context);
        }

        private Comment findMarker(J tree, String marker) {
            return tree.getPrefix().getComments().stream()
                    .filter(c -> Comment.Style.BLOCK.equals(c.getStyle()))
                    .filter(c -> c.getText().equals(marker))
                    .findAny()
                    .orElse(null);
        }
    }

    public static class Builder {

        private final String code;
        private JavaParser javaParser = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build();
        private final Set<String> imports = new HashSet<>();

        private boolean autoFormat = true;
        private String parameterMarker = "#{}";

        Builder(String code) {
            this.code = code;
        }

        /**
         * A list of fully-qualified types that will be added when generating/compiling snippets
         *
         * <PRE>
         * Examples:
         * <p>
         * java.util.Collections
         * java.util.Date
         * </PRE>
         */
        public Builder imports(@NonNull String... fullyQualifiedTypeNames) {
            for (String typeName : fullyQualifiedTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("import " + typeName + ";\n");
            }
            return this;
        }

        /**
         * A list of fully-qualified member type names that will be statically imported when generating/compiling snippets.
         *
         * <PRE>
         * Examples:
         * <p>
         * java.util.Collections.emptyList
         * java.util.Collections.*
         * </PRE>
         */
        public Builder staticImports(@NonNull String... fullyQualifiedMemberTypeNames) {
            for (String typeName : fullyQualifiedMemberTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("static import " + typeName + ";\n");
            }
            return this;
        }

        /**
         * A list of JavaTypes that will be imported/statically imported when generating/compiling snippets.
         * <P><P>
         * FullyQualified types will be added as an import and Method types will be statically imported
         */
        public Builder imports(@NonNull JavaType... types) {
            for (JavaType type : types) {
                imports.add(javaTypeToImport(type));
            }
            return this;
        }

        public Builder javaParser(JavaParser javaParser) {
            this.javaParser = javaParser;
            return this;
        }

        public Builder autoFormat(boolean autoFormat) {
            this.autoFormat = autoFormat;
            return this;
        }

        /**
         * Define an alternate marker to denote where a parameter should be inserted into the template. If not specified, the
         * default format for parameter marker is "#{}"
         */
        public Builder parameterMarker(@NonNull String parameterMarker) {
            this.parameterMarker = parameterMarker;
            return this;
        }

        public JavaTemplate build() {
            if (javaParser == null) {
                javaParser = JavaParser.fromJavaVersion()
                        .logCompilationWarningsAndErrors(false)
                        .build();
            }
            return new JavaTemplate(javaParser, code, imports, autoFormat, parameterMarker);
        }
    }

    /**
     * Convert a JavaType into a string import statement of the form:
     * <P><PRE>
     * FullyQualfied Types : import {}fully-qualified-name};
     * Method Types        : static import {fully-qualified-name}.{methodName};
     * </PRE>
     */
    private static String javaTypeToImport(JavaType type) {
        StringBuilder anImport = new StringBuilder(256);
        if (type instanceof JavaType.FullyQualified) {
            anImport.append("import ").append(((JavaType.FullyQualified) type).getFullyQualifiedName()).append(";\n");
        } else if (type instanceof JavaType.Method) {
            JavaType.Method method = (JavaType.Method) type;
            anImport.append("static import ").append(method.getDeclaringType().getFullyQualifiedName())
                    .append(".").append(method.getName()).append(";\n");
        } else {
            throw new IllegalArgumentException("Unsupported import type.");
        }
        return anImport.toString();
    }


}
