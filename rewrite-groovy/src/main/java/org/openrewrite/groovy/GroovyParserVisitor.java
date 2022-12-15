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
package org.openrewrite.groovy;

import groovyjarjarasm.asm.Opcodes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.groovy.marker.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

/**
 * See the <a href="https://groovy-lang.org/syntax.html">language syntax reference</a>.
 */
public class GroovyParserVisitor {
    private final Path sourcePath;
    @Nullable
    private final FileAttributes fileAttributes;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final GroovyTypeMapping typeMapping;
    private final ExecutionContext ctx;

    private int cursor = 0;

    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    public GroovyParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source, JavaTypeCache typeCache, ExecutionContext ctx) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
        this.typeMapping = new GroovyTypeMapping(typeCache);
        this.ctx = ctx;
    }

    public G.CompilationUnit visit(SourceUnit unit, ModuleNode ast) throws GroovyParsingException {
        NavigableMap<LineColumn, List<ASTNode>> sortedByPosition = new TreeMap<>();
        for (org.codehaus.groovy.ast.stmt.Statement s : ast.getStatementBlock().getStatements()) {
            if (!isSynthetic(s)) {
                sortedByPosition.computeIfAbsent(pos(s), i -> new ArrayList<>()).add(s);
            }
        }
        String shebang = null;
        if (source.startsWith("#!")) {
            int i = 0;
            while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                i++;
            }
            shebang = source.substring(0, i);
            cursor += i;
        }
        JRightPadded<J.Package> pkg = null;
        if (ast.getPackage() != null) {
            Space prefix = whitespace();
            cursor += "package".length();
            pkg = JRightPadded.build(new J.Package(randomId(), prefix, Markers.EMPTY,
                    typeTree(null), emptyList()));
        }

        for (ImportNode anImport : ast.getImports()) {
            sortedByPosition.computeIfAbsent(pos(anImport), i -> new ArrayList<>()).add(anImport);
        }
        for (ImportNode anImport : ast.getStarImports()) {
            sortedByPosition.computeIfAbsent(pos(anImport), i -> new ArrayList<>()).add(anImport);
        }
        for (ImportNode anImport : ast.getStaticImports().values()) {
            sortedByPosition.computeIfAbsent(pos(anImport), i -> new ArrayList<>()).add(anImport);
        }
        for (ImportNode anImport : ast.getStaticStarImports().values()) {
            sortedByPosition.computeIfAbsent(pos(anImport), i -> new ArrayList<>()).add(anImport);
        }

        for (ClassNode aClass : ast.getClasses()) {
            if (aClass.getSuperClass() == null || !"groovy.lang.Script".equals(aClass.getSuperClass().getName())) {
                sortedByPosition.computeIfAbsent(pos(aClass), i -> new ArrayList<>()).add(aClass);
            }
        }

        for (MethodNode method : ast.getMethods()) {
            sortedByPosition.computeIfAbsent(pos(method), i -> new ArrayList<>()).add(method);
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(sortedByPosition.size());
        for (List<ASTNode> values : sortedByPosition.values()) {
            try {
                for (ASTNode value : values) {
                    if (value instanceof InnerClassNode) {
                        // Inner classes will be visited as part of visiting their containing class
                        continue;
                    }
                    statements.add(convertTopLevelStatement(unit, value));
                }
            } catch (Throwable t) {
                if (t instanceof StringIndexOutOfBoundsException) {
                    throw new GroovyParsingException("Failed to parse " + sourcePath + ", cursor position likely inaccurate.", t);
                }
                throw new GroovyParsingException(
                        "Failed to parse " + sourcePath + " at cursor position " + cursor +
                        ". The next 10 characters in the original source are `" +
                        source.substring(cursor, Math.min(source.length(), cursor + 10)) + "`", t);
            }
        }

        return new G.CompilationUnit(
                randomId(),
                shebang,
                Space.EMPTY,
                Markers.EMPTY,
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                pkg,
                statements,
                format(source.substring(cursor))
        );
    }

    @RequiredArgsConstructor
    private class RewriteGroovyClassVisitor extends ClassCodeVisitorSupport {
        @Getter
        private final SourceUnit sourceUnit;

        private final Queue<Object> queue = new LinkedList<>();

        @Override
        public void visitClass(ClassNode clazz) {
            Space fmt = whitespace();
            List<J.Annotation> leadingAnnotations;
            if (clazz.getAnnotations().isEmpty()) {
                leadingAnnotations = emptyList();
            } else {
                leadingAnnotations = new ArrayList<>(clazz.getAnnotations().size());
                for (AnnotationNode annotation : clazz.getAnnotations()) {
                    visitAnnotation(annotation);
                    leadingAnnotations.add(pollQueue());
                }
            }
            List<J.Modifier> modifiers = visitModifiers(clazz.getModifiers());

            Space kindPrefix = whitespace();
            J.ClassDeclaration.Kind.Type kindType = null;
            if (source.startsWith("class", cursor)) {
                kindType = J.ClassDeclaration.Kind.Type.Class;
                cursor += "class".length();
            } else if (source.startsWith("interface", cursor)) {
                kindType = J.ClassDeclaration.Kind.Type.Interface;
                cursor += "interface".length();
            } else if (source.startsWith("@interface", cursor)) {
                kindType = J.ClassDeclaration.Kind.Type.Annotation;
                cursor += "@interface".length();
            }
            assert kindType != null;
            J.ClassDeclaration.Kind kind = new J.ClassDeclaration.Kind(randomId(), kindPrefix, Markers.EMPTY, emptyList(), kindType);
            Space namePrefix = whitespace();
            String simpleName = name();
            J.Identifier name = new J.Identifier(randomId(), namePrefix, Markers.EMPTY, simpleName, typeMapping.type(clazz), null);
            JContainer<J.TypeParameter> typeParameterContainer = null;
            if (clazz.isUsingGenerics() && clazz.getGenericsTypes() != null) {
                typeParameterContainer = visitTypeParameters(clazz.getGenericsTypes());
            }

            JLeftPadded<TypeTree> extendings = null;
            if (clazz.getSuperClass().getLineNumber() >= 0) {
                extendings = padLeft(sourceBefore("extends"), visitTypeTree(clazz.getSuperClass()));
            }

            JContainer<TypeTree> implementings = null;
            if (clazz.getInterfaces().length > 0) {
                Space implPrefix;
                if (kindType == J.ClassDeclaration.Kind.Type.Interface || kindType == J.ClassDeclaration.Kind.Type.Annotation) {
                    implPrefix = sourceBefore("extends");
                } else {
                    implPrefix = sourceBefore("implements");
                }
                List<JRightPadded<TypeTree>> implTypes = new ArrayList<>(clazz.getInterfaces().length);
                ClassNode[] interfaces = clazz.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    ClassNode anInterface = interfaces[i];
                    // Any annotation @interface is listed as extending java.lang.annotation.Annotation, although it doesn't appear in source
                    if (kindType == J.ClassDeclaration.Kind.Type.Annotation && "java.lang.annotation.Annotation".equals(anInterface.getName())) {
                        continue;
                    }
                    implTypes.add(JRightPadded.build(visitTypeTree(anInterface))
                            .withAfter(i == interfaces.length - 1 ? EMPTY : sourceBefore(",")));
                }
                // Can be empty for an annotation @interface which only implements Annotation
                if (implTypes.size() > 0) {
                    implementings = JContainer.build(implPrefix, implTypes, Markers.EMPTY);
                }
            }

            queue.add(new J.ClassDeclaration(randomId(), fmt, Markers.EMPTY,
                    leadingAnnotations,
                    modifiers,
                    kind,
                    name,
                    typeParameterContainer,
                    null,
                    extendings,
                    implementings,
                    visitClassBlock(clazz),
                    TypeUtils.asFullyQualified(typeMapping.type(clazz))));
        }

        J.Block visitClassBlock(ClassNode clazz) {
            NavigableMap<LineColumn, List<ASTNode>> sortedByPosition = new TreeMap<>();
            for (MethodNode method : clazz.getMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                sortedByPosition.computeIfAbsent(pos(method), i -> new ArrayList<>()).add(method);
            }
            /*
              In certain circumstances the same AST node may appear in multiple places.
              class A {
                  def a = new Object() {
                      // this anonymous class is both part of the initializing expression for the variable "a"
                      // And appears in the list of inner classes of "A"
                  }
              }
              So keep track of inner classes that are part of field initializers so that they don't get parsed twice
             */
            Set<InnerClassNode> fieldInitializers = new HashSet<>();
            for (FieldNode field : clazz.getFields()) {
                if (!appearsInSource(field)) {
                    continue;
                }
                if (field.hasInitialExpression() && field.getInitialExpression() instanceof ConstructorCallExpression) {
                    ConstructorCallExpression cce = (ConstructorCallExpression) field.getInitialExpression();
                    if (cce.isUsingAnonymousInnerClass() && cce.getType() instanceof InnerClassNode) {
                        fieldInitializers.add((InnerClassNode) cce.getType());
                    }
                }
                sortedByPosition.computeIfAbsent(pos(field), i -> new ArrayList<>()).add(field);
            }
            Iterator<InnerClassNode> innerClassIterator = clazz.getInnerClasses();
            while (innerClassIterator.hasNext()) {
                InnerClassNode icn = innerClassIterator.next();
                if (icn.isSynthetic() || fieldInitializers.contains(icn)) {
                    continue;
                }
                sortedByPosition.computeIfAbsent(pos(icn), i -> new ArrayList<>()).add(icn);
            }

            return new J.Block(randomId(), sourceBefore("{"), Markers.EMPTY,
                    JRightPadded.build(false),
                    sortedByPosition.values().stream()
                            .flatMap(asts -> asts.stream()
                                    .map(ast -> {
                                        if (ast instanceof FieldNode) {
                                            visitField((FieldNode) ast);
                                        } else if (ast instanceof MethodNode) {
                                            visitMethod((MethodNode) ast);
                                        } else if (ast instanceof ClassNode) {
                                            visitClass((ClassNode) ast);
                                        }
                                        Statement stat = pollQueue();
                                        return maybeSemicolon(stat);
                                    }))
                            .collect(Collectors.toList()),
                    sourceBefore("}"));
        }

        @Override
        public void visitField(FieldNode field) {
            if ((field.getModifiers() & Opcodes.ACC_ENUM) != 0) {
                visitEnumField(field);
            } else {
                visitVariableField(field);
            }
        }

        private void visitEnumField(FieldNode fieldNode) {
            // Requires refactoring visitClass to use a similar pattern as Java11ParserVisitor.
            // Currently, each field is visited one at a time, so we cannot construct the EnumValueSet.
            throw new UnsupportedOperationException("enum fields are not implemented.");
        }

        private void visitVariableField(FieldNode field) {
            RewriteGroovyVisitor visitor = new RewriteGroovyVisitor(field, this);

            List<J.Annotation> annotations = field.getAnnotations().stream()
                    .map(a -> {
                        visitAnnotation(a);
                        return (J.Annotation) pollQueue();
                    })
                    .collect(Collectors.toList());

            List<J.Modifier> modifiers = visitModifiers(field.getModifiers());
            TypeTree typeExpr = visitTypeTree(field.getOriginType());

            J.Identifier name = new J.Identifier(randomId(), sourceBefore(field.getName()), Markers.EMPTY,
                    field.getName(), typeMapping.type(field.getOriginType()), typeMapping.variableType(field));

            J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    name.getPrefix(),
                    Markers.EMPTY,
                    name.withPrefix(EMPTY),
                    emptyList(),
                    null,
                    typeMapping.variableType(field)
            );

            if (field.getInitialExpression() != null) {
                Space beforeAssign = sourceBefore("=");
                Expression initializer = visitor.visit(field.getInitialExpression());
                namedVariable = namedVariable.getPadding().withInitializer(padLeft(beforeAssign, initializer));
            }

            J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    annotations,
                    modifiers,
                    typeExpr,
                    null,
                    emptyList(),
                    singletonList(JRightPadded.build(namedVariable))
            );

            queue.add(variableDeclarations);
        }

        @Override
        protected void visitAnnotation(AnnotationNode annotation) {
            RewriteGroovyVisitor bodyVisitor = new RewriteGroovyVisitor(annotation, this);

            String lastArgKey = annotation.getMembers().keySet().stream().reduce("", (k1, k2) -> k2);
            Space prefix = sourceBefore("@");
            NameTree annotationType = visitTypeTree(annotation.getClassNode());
            JContainer<Expression> arguments = null;
            if (!annotation.getMembers().isEmpty()) {
                // This doesn't handle the case where an annotation has empty arguments like @Foo(), but that is rare
                arguments = JContainer.build(
                        sourceBefore("("),
                        annotation.getMembers().entrySet().stream()
                                .map(arg -> {
                                    Space argPrefix;
                                    if ("value".equals(arg.getKey())) {
                                        // Determine whether the value is implicit or explicit
                                        int saveCursor = cursor;
                                        argPrefix = whitespace();
                                        if (!source.startsWith("value", cursor)) {
                                            return new JRightPadded<Expression>(
                                                    ((Expression) bodyVisitor.visit(arg.getValue())).withPrefix(argPrefix),
                                                    arg.getKey().equals(lastArgKey) ? sourceBefore(")") : sourceBefore(","),
                                                    Markers.EMPTY);
                                        }
                                        cursor = saveCursor;
                                    }
                                    argPrefix = sourceBefore(arg.getKey());
                                    J.Identifier argName = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, arg.getKey(), null, null);
                                    J.Assignment assign = new J.Assignment(randomId(), argPrefix, Markers.EMPTY,
                                            argName, padLeft(sourceBefore("="), bodyVisitor.visit(arg.getValue())),
                                            null);
                                    return JRightPadded.build((Expression) assign)
                                            .withAfter(arg.getKey().equals(lastArgKey) ? sourceBefore(")") : sourceBefore(","));
                                })
                                .collect(Collectors.toList()),
                        Markers.EMPTY
                );
            }
            queue.add(new J.Annotation(randomId(), prefix, Markers.EMPTY, annotationType, arguments));
        }

        @Override
        public void visitMethod(MethodNode method) {
            Space fmt = whitespace();

            List<J.Annotation> annotations = method.getAnnotations().stream()
                    .map(a -> {
                        visitAnnotation(a);
                        return (J.Annotation) pollQueue();
                    })
                    .collect(Collectors.toList());

            List<J.Modifier> modifiers = visitModifiers(method.getModifiers());

            TypeTree returnType = visitTypeTree(method.getReturnType());

            J.Identifier name = new J.Identifier(randomId(),
                    sourceBefore(method.getName()),
                    Markers.EMPTY,
                    method.getName(),
                    null, null);

            RewriteGroovyVisitor bodyVisitor = new RewriteGroovyVisitor(method, this);

            // Parameter has no visit implementation, so we've got to do this by hand
            Space beforeParen = sourceBefore("(");
            List<JRightPadded<Statement>> params = new ArrayList<>(method.getParameters().length);
            Parameter[] unparsedParams = method.getParameters();
            for (int i = 0; i < unparsedParams.length; i++) {
                Parameter param = unparsedParams[i];

                List<J.Annotation> paramAnnotations = param.getAnnotations().stream()
                        .map(a -> {
                            visitAnnotation(a);
                            return (J.Annotation) pollQueue();
                        })
                        .collect(Collectors.toList());

                TypeTree paramType;
                if (param.isDynamicTyped()) {
                    paramType = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, "", JavaType.ShallowClass.build("java.lang.Object"), null);
                } else {
                    paramType = visitTypeTree(param.getOriginType());
                }
                JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                        new J.VariableDeclarations.NamedVariable(randomId(), EMPTY, Markers.EMPTY,
                                new J.Identifier(randomId(), whitespace(), Markers.EMPTY, param.getName(), null, null),
                                emptyList(), null, null)
                );
                cursor += param.getName().length();

                org.codehaus.groovy.ast.expr.Expression defaultValue = param.getDefaultValue();
                if (defaultValue != null) {
                    paramName = paramName.withElement(paramName.getElement().getPadding()
                            .withInitializer(new JLeftPadded<>(
                                    sourceBefore("="),
                                    new RewriteGroovyVisitor(defaultValue, this).visit(defaultValue),
                                    Markers.EMPTY)));
                }
                Space rightPad = sourceBefore(i == unparsedParams.length - 1 ? ")" : ",");

                params.add(JRightPadded.build((Statement) new J.VariableDeclarations(randomId(), EMPTY,
                        Markers.EMPTY, paramAnnotations, emptyList(), paramType,
                        null, emptyList(),
                        singletonList(paramName))).withAfter(rightPad));
            }

            if (unparsedParams.length == 0) {
                params.add(JRightPadded.build(new J.Empty(randomId(), sourceBefore(")"), Markers.EMPTY)));
            }

            JContainer<NameTree> throwz = method.getExceptions().length == 0 ? null : JContainer.build(
                    sourceBefore("throws"),
                    bodyVisitor.visitRightPadded(method.getExceptions(), null),
                    Markers.EMPTY
            );

            J.Block body = method.getCode() == null ? null :
                    bodyVisitor.visit(method.getCode());

            queue.add(new J.MethodDeclaration(
                    randomId(), fmt, Markers.EMPTY,
                    annotations,
                    modifiers,
                    null,
                    returnType,
                    new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                    JContainer.build(beforeParen, params, Markers.EMPTY),
                    throwz,
                    body,
                    null,
                    typeMapping.methodType(method)
            ));
        }

        @SuppressWarnings({"ConstantConditions", "unchecked"})
        private <T> T pollQueue() {
            return (T) queue.poll();
        }
    }

    private class RewriteGroovyVisitor extends CodeVisitorSupport {
        private Cursor nodeCursor;
        private final Queue<Object> queue = new LinkedList<>();
        private final RewriteGroovyClassVisitor classVisitor;

        public RewriteGroovyVisitor(ASTNode root, RewriteGroovyClassVisitor classVisitor) {
            this.nodeCursor = new Cursor(null, root);
            this.classVisitor = classVisitor;
        }

        private <T> T visit(ASTNode node) {
            nodeCursor = new Cursor(nodeCursor, node);
            node.visit(this);
            nodeCursor = nodeCursor.getParentOrThrow();
            return pollQueue();
        }

        private <T> List<JRightPadded<T>> visitRightPadded(ASTNode[] nodes, @Nullable String afterLast) {
            List<JRightPadded<T>> ts = new ArrayList<>(nodes.length);
            for (int i = 0; i < nodes.length; i++) {
                ASTNode node = nodes[i];
                @SuppressWarnings("unchecked") JRightPadded<T> converted = JRightPadded.build(
                        node instanceof ClassNode ? (T) visitTypeTree((ClassNode) node) : visit(node));
                if (i == nodes.length - 1) {
                    converted = converted.withAfter(whitespace());
                    if (',' == source.charAt(cursor)) {
                        // In Groovy trailing "," are allowed
                        cursor += 1;
                        converted = converted.withMarkers(Markers.EMPTY.add(new TrailingComma(randomId(), whitespace())));
                    }
                    ts.add(converted);
                    if (afterLast != null && source.startsWith(afterLast, cursor)) {
                        cursor += afterLast.length();
                    }
                } else {
                    ts.add(converted.withAfter(sourceBefore(",")));
                }
            }
            return ts;
        }

        @Override
        public void visitArgumentlistExpression(ArgumentListExpression expression) {
            List<JRightPadded<Expression>> args = new ArrayList<>(expression.getExpressions().size());

            int saveCursor = cursor;
            Space beforeOpenParen = whitespace();

            OmitParentheses omitParentheses = null;
            if (source.charAt(cursor) == '(') {
                cursor++;
            } else {
                omitParentheses = new OmitParentheses(randomId());
                beforeOpenParen = EMPTY;
                cursor = saveCursor;
            }

            List<org.codehaus.groovy.ast.expr.Expression> unparsedArgs = expression.getExpressions();
            // If the first parameter to a function is a Map, then groovy allows "named parameters" style invocations, see:
            //     https://docs.groovy-lang.org/latest/html/documentation/#_named_parameters_2
            // When named parameters are in use they may appear before, after, or intermixed with any positional arguments
            if (unparsedArgs.size() > 1 && unparsedArgs.get(0) instanceof MapExpression
                && (unparsedArgs.get(0).getLastLineNumber() > unparsedArgs.get(1).getLastLineNumber()
                    || (unparsedArgs.get(0).getLastLineNumber() == unparsedArgs.get(1).getLastLineNumber()
                        && unparsedArgs.get(0).getLastColumnNumber() > unparsedArgs.get(1).getLastColumnNumber()))) {

                // Figure out the source-code ordering of the expressions
                MapExpression namedArgExpressions = (MapExpression) unparsedArgs.get(0);
                unparsedArgs =
                        Stream.concat(
                                        namedArgExpressions.getMapEntryExpressions().stream(),
                                        unparsedArgs.subList(1, unparsedArgs.size()).stream())
                                .sorted(Comparator.comparing(ASTNode::getLastLineNumber)
                                        .thenComparing(ASTNode::getLastColumnNumber))
                                .collect(Collectors.toList());
            } else if (unparsedArgs.size() > 0 && unparsedArgs.get(0) instanceof MapExpression) {
                // The map literal may or may not be wrapped in "[]"
                // If it is wrapped in "[]" then this isn't a named arguments situation and we should not lift the parameters out of the enclosing MapExpression
                saveCursor = cursor;
                whitespace();
                boolean isOpeningBracketPresent = '[' == source.charAt(cursor);
                cursor = saveCursor;
                if (!isOpeningBracketPresent) {
                    // Bring named parameters out of their containing MapExpression so that they can be parsed correctly
                    MapExpression namedArgExpressions = (MapExpression) unparsedArgs.get(0);
                    unparsedArgs =
                            Stream.concat(
                                            namedArgExpressions.getMapEntryExpressions().stream(),
                                            unparsedArgs.subList(1, unparsedArgs.size()).stream())
                                    .collect(Collectors.toList());
                }
            }

            for (int i = 0; i < unparsedArgs.size(); i++) {
                org.codehaus.groovy.ast.expr.Expression rawArg = unparsedArgs.get(i);
                Expression arg;
                if (appearsInSource(rawArg)) {
                    arg = visit(rawArg);
                } else {
                    arg = new J.Empty(randomId(), whitespace(), Markers.EMPTY);
                }
                if (omitParentheses != null) {
                    arg = arg.withMarkers(arg.getMarkers().add(omitParentheses));
                }

                Space after = EMPTY;
                if (i == unparsedArgs.size() - 1) {
                    if (omitParentheses == null) {
                        after = sourceBefore(")");
                    }
                } else {
                    after = whitespace();
                    if (source.charAt(cursor) == ')') {
                        // the next argument will have an OmitParentheses marker
                        omitParentheses = new OmitParentheses(randomId());
                    }
                    cursor++;
                }

                args.add(JRightPadded.build(arg).withAfter(after));
            }

            if (unparsedArgs.isEmpty()) {
                Expression element = new J.Empty(randomId(),
                        omitParentheses == null ? sourceBefore(")") : EMPTY, Markers.EMPTY);
                if (omitParentheses != null) {
                    element = element.withMarkers(element.getMarkers().add(omitParentheses));
                }

                args.add(JRightPadded.build(element));
            }

            queue.add(JContainer.build(beforeOpenParen, args, Markers.EMPTY));
        }

        @Override
        public void visitClassExpression(ClassExpression clazz) {
            queue.add(TypeTree.build(clazz.getType().getUnresolvedName())
                    .withType(typeMapping.type(clazz.getType()))
                    .withPrefix(sourceBefore(clazz.getType().getUnresolvedName())));
        }

        @Override
        public void visitBinaryExpression(BinaryExpression binary) {
            Space fmt = whitespace();
            Expression left = visit(binary.getLeftExpression());

            Space opPrefix = whitespace();
            boolean assignment = false;
            J.AssignmentOperation.Type assignOp = null;
            J.Binary.Type binaryOp = null;
            G.Binary.Type gBinaryOp = null;
            switch (binary.getOperation().getText()) {
                case "+":
                    binaryOp = J.Binary.Type.Addition;
                    break;
                case "&&":
                    binaryOp = J.Binary.Type.And;
                    break;
                case "&":
                    binaryOp = J.Binary.Type.BitAnd;
                    break;
                case "|":
                    binaryOp = J.Binary.Type.BitOr;
                    break;
                case "^":
                    binaryOp = J.Binary.Type.BitXor;
                    break;
                case "/":
                    binaryOp = J.Binary.Type.Division;
                    break;
                case "==":
                    binaryOp = J.Binary.Type.Equal;
                    break;
                case ">":
                    binaryOp = J.Binary.Type.GreaterThan;
                    break;
                case ">=":
                    binaryOp = J.Binary.Type.GreaterThanOrEqual;
                    break;
                case "<<":
                    binaryOp = J.Binary.Type.LeftShift;
                    break;
                case "<":
                    binaryOp = J.Binary.Type.LessThan;
                    break;
                case "<=":
                    binaryOp = J.Binary.Type.LessThanOrEqual;
                    break;
                case "%":
                    binaryOp = J.Binary.Type.Modulo;
                    break;
                case "*":
                    binaryOp = J.Binary.Type.Multiplication;
                    break;
                case "!=":
                    binaryOp = J.Binary.Type.NotEqual;
                    break;
                case "||":
                    binaryOp = J.Binary.Type.Or;
                    break;
                case ">>":
                    binaryOp = J.Binary.Type.RightShift;
                    break;
                case "-":
                    binaryOp = J.Binary.Type.Subtraction;
                    break;
                case ">>>":
                    binaryOp = J.Binary.Type.UnsignedRightShift;
                    break;
                case "=":
                    assignment = true;
                    break;
                case "+=":
                    assignOp = J.AssignmentOperation.Type.Addition;
                    break;
                case "-=":
                    assignOp = J.AssignmentOperation.Type.Subtraction;
                    break;
                case "&=":
                    assignOp = J.AssignmentOperation.Type.BitAnd;
                    break;
                case "|=":
                    assignOp = J.AssignmentOperation.Type.BitOr;
                    break;
                case "^=":
                    assignOp = J.AssignmentOperation.Type.BitXor;
                    break;
                case "/=":
                    assignOp = J.AssignmentOperation.Type.Division;
                    break;
                case "<<=":
                    assignOp = J.AssignmentOperation.Type.LeftShift;
                    break;
                case "%=":
                    assignOp = J.AssignmentOperation.Type.Modulo;
                    break;
                case "*=":
                    assignOp = J.AssignmentOperation.Type.Multiplication;
                    break;
                case ">>=":
                    assignOp = J.AssignmentOperation.Type.RightShift;
                    break;
                case ">>>=":
                    assignOp = J.AssignmentOperation.Type.UnsignedRightShift;
                    break;
                case "=~":
                    gBinaryOp = G.Binary.Type.Find;
                    break;
                case "==~":
                    gBinaryOp = G.Binary.Type.Match;
                    break;
                case "[":
                    gBinaryOp = G.Binary.Type.Access;
                    break;
            }

            cursor += binary.getOperation().getText().length();
            Expression right = visit(binary.getRightExpression());

            if (assignment) {
                queue.add(new J.Assignment(randomId(), fmt, Markers.EMPTY,
                        left, JLeftPadded.build(right).withBefore(opPrefix),
                        typeMapping.type(binary.getType())));
            } else if (assignOp != null) {
                queue.add(new J.AssignmentOperation(randomId(), fmt, Markers.EMPTY,
                        left, JLeftPadded.build(assignOp).withBefore(opPrefix),
                        right, typeMapping.type(binary.getType())));
            } else if (binaryOp != null) {
                queue.add(new J.Binary(randomId(), fmt, Markers.EMPTY,
                        left, JLeftPadded.build(binaryOp).withBefore(opPrefix),
                        right, typeMapping.type(binary.getType())));
            } else if (gBinaryOp != null) {
                Space after = EMPTY;
                if (gBinaryOp == G.Binary.Type.Access) {
                    after = sourceBefore("]");
                }
                queue.add(new G.Binary(randomId(), fmt, Markers.EMPTY,
                        left, JLeftPadded.build(gBinaryOp).withBefore(opPrefix),
                        right, after, typeMapping.type(binary.getType())));
            }
        }

        @Override
        public void visitBlockStatement(BlockStatement block) {
            Space fmt = EMPTY;
            Object parent = nodeCursor.getParentOrThrow().getValue();
            if (!(parent instanceof ClosureExpression)) {
                fmt = sourceBefore("{");
            }
            List<JRightPadded<Statement>> statements = new ArrayList<>(block.getStatements().size());
            List<org.codehaus.groovy.ast.stmt.Statement> blockStatements = block.getStatements();
            for (int i = 0; i < blockStatements.size(); i++) {
                ASTNode statement = blockStatements.get(i);
                J expr = visit(statement);
                if (i == blockStatements.size() - 1 && (expr instanceof Expression)) {
                    if (parent instanceof ClosureExpression || (parent instanceof MethodNode &&
                                                                !JavaType.Primitive.Void.equals(typeMapping.type(((MethodNode) parent).getReturnType())))) {
                        expr = new J.Return(randomId(), expr.getPrefix(), Markers.EMPTY,
                                expr.withPrefix(EMPTY));
                        expr = expr.withMarkers(expr.getMarkers().add(new ImplicitReturn(randomId())));
                    }
                }

                JRightPadded<Statement> stat = JRightPadded.build((Statement) expr);
                int saveCursor = cursor;
                Space beforeSemicolon = whitespace();
                if (cursor < source.length() && source.charAt(cursor) == ';') {
                    stat = stat
                            .withMarkers(stat.getMarkers().add(new Semicolon(randomId())))
                            .withAfter(beforeSemicolon);
                    cursor++;
                } else {
                    cursor = saveCursor;
                }

                statements.add(stat);
            }

            Space beforeBrace = whitespace();
            queue.add(new J.Block(randomId(), fmt, Markers.EMPTY, JRightPadded.build(false), statements, beforeBrace));
            if (!(parent instanceof ClosureExpression)) {
                sourceBefore("}");
            }
        }

        @Override
        public void visitCatchStatement(CatchStatement node) {
            Space prefix = sourceBefore("catch");
            Space parenPrefix = sourceBefore("(");

            // This does not handle multi-catch statements like catch(ExceptionTypeA | ExceptionTypeB e)
            // The Groovy AST seems to only record the first type in the list, so some extra hacking is required to get the others
            Parameter param = node.getVariable();
            TypeTree paramType;
            Space paramPrefix = whitespace();
            // Groovy allows catch variables to omit their type, shorthand for being of type java.lang.Exception
            // Can't use isSynthetic() here because groovy doesn't record the line number on the Parameter
            if ("java.lang.Exception".equals(param.getType().getName())
                && !source.startsWith("Exception", cursor)
                && !source.startsWith("java.lang.Exception", cursor)) {
                paramType = new J.Identifier(randomId(), paramPrefix, Markers.EMPTY, "",
                        JavaType.ShallowClass.build("java.lang.Exception"), null);
            } else {
                paramType = visitTypeTree(param.getOriginType()).withPrefix(paramPrefix);
            }

            JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                    new J.VariableDeclarations.NamedVariable(randomId(), whitespace(), Markers.EMPTY,
                            new J.Identifier(randomId(), EMPTY, Markers.EMPTY, param.getName(), null, null),
                            emptyList(), null, null)
            );
            cursor += param.getName().length();
            Space rightPad = whitespace();
            cursor += 1; // skip )
            JRightPadded<J.VariableDeclarations> variable = JRightPadded.build(new J.VariableDeclarations(randomId(), paramType.getPrefix(),
                    Markers.EMPTY, emptyList(), emptyList(), paramType.withPrefix(EMPTY),
                    null, emptyList(),
                    singletonList(paramName))
            ).withAfter(rightPad);

            J.ControlParentheses<J.VariableDeclarations> catchControl = new J.ControlParentheses<>(randomId(), parenPrefix, Markers.EMPTY, variable);
            queue.add(new J.Try.Catch(randomId(), prefix, Markers.EMPTY, catchControl, visit(node.getCode())));
        }

        @Override
        public void visitBreakStatement(BreakStatement statement) {
            queue.add(new J.Break(randomId(),
                    sourceBefore("break"),
                    Markers.EMPTY,
                    (statement.getLabel() == null) ?
                            null :
                            new J.Identifier(randomId(),
                                    sourceBefore(statement.getLabel()),
                                    Markers.EMPTY, statement.getLabel(), null, null))
            );
        }

        @Override
        public void visitCaseStatement(CaseStatement statement) {
            queue.add(
                    new J.Case(randomId(),
                            sourceBefore("case"),
                            Markers.EMPTY,
                            J.Case.Type.Statement,
                            null,
                            JContainer.build(singletonList(JRightPadded.build(visit(statement.getExpression())))),
                            JContainer.build(sourceBefore(":"),
                                    visitRightPadded(((BlockStatement) statement.getCode()).getStatements().toArray(new ASTNode[0]), null), Markers.EMPTY),
                            null
                    )
            );
        }

        @Override
        public void visitCastExpression(CastExpression cast) {
            // Might be looking at a Java-style cast "(type)object" or a groovy-style cast "object as type"
            // If the CastExpression starts at the same place as the expression it contains, then it must be a groovy-style cast
            if (cast.getLineNumber() == cast.getExpression().getLineNumber() && cast.getColumnNumber() == cast.getExpression().getColumnNumber()) {
                Space prefix = whitespace();
                Expression expr = visit(cast.getExpression());
                Space asPrefix = sourceBefore("as");
                Space typePrefix = whitespace();
                String typeIdentifierText = name();

                queue.add(new J.TypeCast(randomId(), prefix, new Markers(randomId(), singletonList(new AsStyleTypeCast(randomId()))),
                        new J.ControlParentheses<>(randomId(), EMPTY, Markers.EMPTY,
                                new JRightPadded<>(new J.Identifier(randomId(), typePrefix, Markers.EMPTY, typeIdentifierText, typeMapping.type(staticType(cast)), null), asPrefix, Markers.EMPTY)
                        ),
                        expr));
            } else {
                Space prefix = sourceBefore("(");
                Space identifierPrefix = whitespace();

                String typeIdentifierText = name();
                Space closingParenPrefix = sourceBefore(")");

                queue.add(new J.TypeCast(randomId(), prefix, Markers.EMPTY,
                        new J.ControlParentheses<>(randomId(), EMPTY, Markers.EMPTY,
                                new JRightPadded<>(new J.Identifier(randomId(), identifierPrefix, Markers.EMPTY, typeIdentifierText, typeMapping.type(staticType(cast)), null), closingParenPrefix, Markers.EMPTY)
                        ),
                        visit(cast.getExpression())));
            }
        }

        @Override
        public void visitClosureExpression(ClosureExpression expression) {
            Space prefix = sourceBefore("{");
            JavaType closureType = typeMapping.type(staticType(expression));
            List<JRightPadded<J>> paramExprs = emptyList();
            if (expression.getParameters() != null && expression.getParameters().length > 0) {
                paramExprs = new ArrayList<>(expression.getParameters().length);
                Parameter[] parameters = expression.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter p = parameters[i];
                    JavaType type = typeMapping.type(staticType(p));
                    J expr = new J.VariableDeclarations(randomId(), whitespace(), Markers.EMPTY,
                            emptyList(), emptyList(), p.isDynamicTyped() ? null : visitTypeTree(p.getType()),
                            null, emptyList(),
                            singletonList(
                                    JRightPadded.build(
                                            new J.VariableDeclarations.NamedVariable(randomId(), sourceBefore(p.getName()), Markers.EMPTY,
                                                    new J.Identifier(randomId(), EMPTY, Markers.EMPTY, p.getName(), type, null),
                                                    emptyList(), null,
                                                    typeMapping.variableType(p.getName(), staticType(p)))
                                    )
                            ));
                    JRightPadded<J> param = JRightPadded.build(expr);
                    if (i != parameters.length - 1) {
                        param = param.withAfter(sourceBefore(","));
                    }
                    paramExprs.add(param);
                }
            }

            J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), EMPTY, Markers.EMPTY, false, paramExprs);
            int saveCursor = cursor;
            Space arrowPrefix = whitespace();
            if (source.startsWith("->", cursor)) {
                cursor += "->".length();
                if (params.getParameters().isEmpty()) {
                    params = params.getPadding().withParams(singletonList(JRightPadded
                            .build((J)new J.Empty(randomId(), EMPTY, Markers.EMPTY))
                            .withAfter(arrowPrefix)));
                } else {
                    params = params.getPadding().withParams(
                            ListUtils.mapLast(params.getPadding().getParams(), param -> param.withAfter(arrowPrefix))
                    );
                }
            } else {
                cursor = saveCursor;
            }

            J body = visit(expression.getCode());
            queue.add(new J.Lambda(randomId(), prefix, Markers.EMPTY, params,
                    EMPTY,
                    body,
                    closureType));

            cursor += 1; // skip '}'
        }

        @Override
        public void visitClosureListExpression(ClosureListExpression closureListExpression) {
            List<org.codehaus.groovy.ast.expr.Expression> expressions = closureListExpression.getExpressions();
            List<JRightPadded<Object>> results = new ArrayList<>(closureListExpression.getExpressions().size());
            for (int i = 0, expressionsSize = expressions.size(); i < expressionsSize; i++) {
                results.add(JRightPadded.build(visit(expressions.get(i))).withAfter(whitespace()));
                if (i < expressionsSize - 1) {
                    cursor++; // ","
                }
            }
            queue.add(results);
        }

        @Override
        public void visitConstantExpression(ConstantExpression expression) {
            Space prefix = whitespace();

            JavaType.Primitive jType;
            // The unaryPlus is not included in the expression and must be handled through the source.
            String text = expression.getText();
            Object value = expression.getValue();
            ClassNode type = expression.getType();
            if (type == ClassHelper.BigDecimal_TYPE) {
                // TODO: Proper support for BigDecimal literals
                jType = JavaType.Primitive.Double;
                value = ((BigDecimal) value).doubleValue();
            } else if (type == ClassHelper.boolean_TYPE) {
                jType = JavaType.Primitive.Boolean;
            } else if (type == ClassHelper.byte_TYPE) {
                jType = JavaType.Primitive.Byte;
            } else if (type == ClassHelper.char_TYPE) {
                jType = JavaType.Primitive.Char;
            } else if (type == ClassHelper.double_TYPE || "java.lang.Double".equals(type.getName())) {
                jType = JavaType.Primitive.Double;
            } else if (type == ClassHelper.float_TYPE || "java.lang.Float".equals(type.getName())) {
                jType = JavaType.Primitive.Float;
            } else if (type == ClassHelper.int_TYPE || "java.lang.Integer".equals(type.getName())) {
                jType = JavaType.Primitive.Int;
                if (expression.getNodeMetaData().get("_INTEGER_LITERAL_TEXT") instanceof String) {
                    String literalText = (String) expression.getNodeMetaData().get("_INTEGER_LITERAL_TEXT");
                    // Groovy automatically converts numbers that start with 0 from base-n to an int.
                    if (literalText.startsWith("0")) {
                        text = literalText;
                    }
                }
            } else if (type == ClassHelper.long_TYPE || "java.lang.Long".equals(type.getName())) {
                jType = JavaType.Primitive.Long;
            } else if (type == ClassHelper.short_TYPE || "java.lang.Short".equals(type.getName())) {
                jType = JavaType.Primitive.Short;
            } else if (type == ClassHelper.STRING_TYPE) {
                jType = JavaType.Primitive.String;
                if (source.startsWith("/", cursor)) {
                    text = "/" + text + "/";
                } else if (source.startsWith("\"\"\"", cursor)) {
                    text = "\"\"\"" + text + "\"\"\"";
                } else if (source.startsWith("'''", cursor)) {
                    text = "'''" + text + "'''";
                } else if (source.startsWith("'", cursor)) {
                    text = "'" + text + "'";
                } else if (source.startsWith("\"", cursor)) {
                    text = "\"" + text + "\"";
                }
            } else if (expression.isNullExpression()) {
                text = "null";
                jType = JavaType.Primitive.Null;
            } else {
                ctx.getOnError().accept(new IllegalStateException("Unexpected constant type " + type));
                return;
            }

            if (source.charAt(cursor) == '+' && !text.startsWith("+")) {
                // A unaryPlus operator is implied on numerics and needs to be manually detected / added via the source.
                text = "+" + text;
            }
            cursor += text.length();

            if (cursor < source.length() && (jType == JavaType.Primitive.Double || jType == JavaType.Primitive.Float || jType == JavaType.Primitive.Long)) {
                char c = Character.toLowerCase(source.charAt(cursor));
                if (c == 'f' || c == 'd' || c == 'l') {
                    text = text + source.charAt(cursor);
                    cursor++;
                }
            }
            queue.add(new J.Literal(randomId(), prefix, Markers.EMPTY, value, text,
                    null, jType));
        }

        @Override
        public void visitConstructorCallExpression(ConstructorCallExpression ctor) {
            Space fmt = sourceBefore("new");
            TypeTree clazz = visitTypeTree(ctor.getType());
            JContainer<Expression> args = visit(ctor.getArguments());
            J.Block body = null;
            if (ctor.isUsingAnonymousInnerClass() && ctor.getType() instanceof InnerClassNode) {
                body = classVisitor.visitClassBlock(ctor.getType());
            }
            MethodNode methodNode = (MethodNode) ctor.getNodeMetaData().get(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
            queue.add(new J.NewClass(randomId(), fmt, Markers.EMPTY, null, EMPTY,
                    clazz, args, body, typeMapping.methodType(methodNode)));
        }

        @Override
        public void visitNotExpression(NotExpression expression) {
            Space fmt = sourceBefore("!");
            JLeftPadded<J.Unary.Type> op = padLeft(EMPTY, J.Unary.Type.Not);
            Expression expr = visit(expression.getExpression());
            queue.add(new J.Unary(randomId(), fmt, Markers.EMPTY, op, expr, typeMapping.type(expression.getType())));
        }

        @Override
        public void visitDeclarationExpression(DeclarationExpression expression) {
            TypeTree typeExpr = visitVariableExpressionType(expression.getVariableExpression());

            J.VariableDeclarations.NamedVariable namedVariable;
            if (expression.isMultipleAssignmentDeclaration()) {
                // def (a, b) = [1, 2]
                throw new UnsupportedOperationException("FIXME");
            } else {
                J.Identifier name = visit(expression.getVariableExpression());
                namedVariable = new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        name.getPrefix(),
                        Markers.EMPTY,
                        name.withPrefix(EMPTY),
                        emptyList(),
                        null,
                        typeMapping.variableType(name.getSimpleName(), typeExpr.getType())
                );
            }

            if (!(expression.getRightExpression() instanceof EmptyExpression)) {
                Space beforeAssign = sourceBefore("=");
                Expression initializer = visit(expression.getRightExpression());
                namedVariable = namedVariable.getPadding().withInitializer(padLeft(beforeAssign, initializer));
            }

            J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    typeExpr,
                    null,
                    emptyList(),
                    singletonList(JRightPadded.build(namedVariable))
            );

            queue.add(variableDeclarations);
        }

        @Override
        public void visitEmptyExpression(EmptyExpression expression) {
            queue.add(new J.Empty(randomId(), EMPTY, Markers.EMPTY));
        }

        @Override
        public void visitExpressionStatement(ExpressionStatement statement) {
            List<J.Label> labels = null;
            if(statement.getStatementLabels() != null && !statement.getStatementLabels().isEmpty()) {
                labels = new ArrayList<>(statement.getStatementLabels().size());
                // Labels appear in statement.getStatementLabels() in reverse order of their appearance in source code
                // Could iterate over those in reverse order, but feels safer to just take the count and go off source code alone
                for(int i = 0; i < statement.getStatementLabels().size(); i++) {
                    labels.add(new J.Label(randomId(), whitespace(), Markers.EMPTY, JRightPadded.build(
                            new J.Identifier(randomId(), EMPTY, Markers.EMPTY, name(), null, null)).withAfter(sourceBefore(":")),
                            new J.Empty(randomId(), EMPTY, Markers.EMPTY)));
                }
            }
            super.visitExpressionStatement(statement);
            if (queue.peek() instanceof Expression && !(queue.peek() instanceof Statement)) {
                queue.add(new G.ExpressionStatement(randomId(), (Expression) queue.poll()));
            }
            if(labels != null) {
                //noinspection ConstantConditions
                Statement result = condenseLabels(labels, (Statement) queue.poll());
                queue.add(result);
            }
        }

        Statement condenseLabels(List<J.Label> labels, Statement s) {
            if(labels.size() == 0) {
                return s;
            }
            return labels.get(0).withStatement(condenseLabels(labels.subList(1, labels.size()), s));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void visitForLoop(ForStatement forLoop) {
            Space fmt = sourceBefore("for");
            Space controlFmt = sourceBefore("(");
            if (forLoop.getCollectionExpression() instanceof ClosureListExpression) {
                List<JRightPadded<?>> controls = visit(forLoop.getCollectionExpression());
                // There will always be exactly three elements in a for loop's ClosureListExpression
                List<JRightPadded<Statement>> init = controls.get(0).getElement() instanceof List ?
                        (List<JRightPadded<Statement>>) controls.get(0).getElement() :
                        singletonList((JRightPadded<Statement>) controls.get(0));

                JRightPadded<Expression> condition = (JRightPadded<Expression>) controls.get(1);

                List<JRightPadded<Statement>> update = controls.get(2).getElement() instanceof List ?
                        (List<JRightPadded<Statement>>) controls.get(2).getElement() :
                        singletonList((JRightPadded<Statement>) controls.get(2));
                cursor++; // skip ')'

                queue.add(new J.ForLoop(randomId(), fmt, Markers.EMPTY,
                        new J.ForLoop.Control(randomId(), controlFmt,
                                Markers.EMPTY, init, condition, update),
                        JRightPadded.build(visit(forLoop.getLoopBlock()))));
            } else {
                Parameter param = forLoop.getVariable();

                TypeTree paramType = visitTypeTree(param.getOriginType());
                JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                        new J.VariableDeclarations.NamedVariable(randomId(), whitespace(), Markers.EMPTY,
                                new J.Identifier(randomId(), EMPTY, Markers.EMPTY, param.getName(), null, null),
                                emptyList(), null, null)
                );
                cursor += param.getName().length();
                Space rightPad = whitespace();
                Markers forEachMarkers = Markers.EMPTY;
                if (source.charAt(cursor) == ':') {
                    cursor++; // Skip ":"
                } else {
                    cursor += 2; // Skip "in"
                    forEachMarkers = forEachMarkers.add(new InStyleForEachLoop(randomId()));
                }

                JRightPadded<J.VariableDeclarations> variable = JRightPadded.build(new J.VariableDeclarations(randomId(), paramType.getPrefix(),
                        Markers.EMPTY, emptyList(), emptyList(), paramType.withPrefix(EMPTY),
                        null, emptyList(),
                        singletonList(paramName))
                ).withAfter(rightPad);

                JRightPadded<Expression> iterable = JRightPadded.build((Expression) visit(forLoop.getCollectionExpression()))
                        .withAfter(sourceBefore(")"));

                queue.add(new J.ForEachLoop(randomId(), fmt, forEachMarkers,
                        new J.ForEachLoop.Control(randomId(), EMPTY, Markers.EMPTY, variable, iterable),
                        JRightPadded.build(visit(forLoop.getLoopBlock())))
                );
            }
        }

        @Override
        public void visitIfElse(IfStatement ifElse) {
            Space fmt = sourceBefore("if");
            J.ControlParentheses<Expression> ifCondition = new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                    JRightPadded.build((Expression) visit(ifElse.getBooleanExpression().getExpression())).withAfter(sourceBefore(")")));
            JRightPadded<Statement> then = maybeSemicolon(visit(ifElse.getIfBlock()));
            J.If.Else elze = ifElse.getElseBlock() instanceof EmptyStatement ? null :
                    new J.If.Else(randomId(), sourceBefore("else"), Markers.EMPTY,
                            maybeSemicolon(visit(ifElse.getElseBlock())));
            queue.add(new J.If(randomId(), fmt, Markers.EMPTY, ifCondition, then, elze));
        }

        @Override
        public void visitGStringExpression(GStringExpression gstring) {
            Space fmt = sourceBefore("\"");

            List<J> strings = new ArrayList<>(gstring.getStrings().size());
            int valueIndex = 0;
            for (ConstantExpression string : gstring.getStrings()) {
                if (string.getValue().equals("")) {
                    if (source.charAt(cursor) != '$') {
                        // Sometimes there are empty constant expressions which do not, apparently, correspond to anything
                        continue;
                    }
                    boolean inCurlies = source.charAt(cursor + 1) == '{';
                    if (inCurlies) {
                        cursor += 2; // skip ${
                    } else {
                        cursor += 1; // skip $
                    }
                    strings.add(new G.GString.Value(randomId(), Markers.EMPTY, visit(gstring.getValue(valueIndex++)), inCurlies));
                    if (inCurlies) {
                        cursor++; // skip }
                    }
                } else {
                    // If this string begins with any character that can open a string, such as "/"
                    // then visitConstantExpression will misinterpret it.
                    // There's no easy way for visitConstantExpression to know otherwise, so it has to be handled here
                    String value = (String) string.getValue();
                    strings.add(new J.Literal(randomId(), EMPTY, Markers.EMPTY, value, value, null, JavaType.Primitive.String));
                    cursor += value.length();
                }
            }

            queue.add(new G.GString(randomId(), fmt, Markers.EMPTY, strings,
                    typeMapping.type(gstring.getType())));
            cursor++; // skip "
        }

        @Override
        public void visitListExpression(ListExpression list) {
            if (list.getExpressions().isEmpty()) {
                queue.add(new G.ListLiteral(randomId(), sourceBefore("["), Markers.EMPTY,
                        JContainer.build(singletonList(new JRightPadded<>(new J.Empty(randomId(), EMPTY, Markers.EMPTY), sourceBefore("]"), Markers.EMPTY))),
                        typeMapping.type(list.getType())));
            } else {
                queue.add(new G.ListLiteral(randomId(), sourceBefore("["), Markers.EMPTY,
                        JContainer.build(visitRightPadded(list.getExpressions().toArray(new ASTNode[0]), "]")),
                        typeMapping.type(list.getType())));
            }
        }

        @Override
        public void visitMapEntryExpression(MapEntryExpression expression) {
            G.MapEntry mapEntry = new G.MapEntry(randomId(), EMPTY, Markers.EMPTY,
                    JRightPadded.build((Expression) visit(expression.getKeyExpression())).withAfter(sourceBefore(":")),
                    visit(expression.getValueExpression()),
                    null
            );
            queue.add(mapEntry);
        }

        @Override
        public void visitMapExpression(MapExpression map) {
            Space prefix = sourceBefore("[");
            JContainer<G.MapEntry> entries;
            if(map.getMapEntryExpressions().isEmpty()) {
                entries = JContainer.build(Collections.singletonList(JRightPadded.build(
                        new G.MapEntry(randomId(), whitespace(), Markers.EMPTY,
                                JRightPadded.build(new J.Empty(randomId(), sourceBefore(":"), Markers.EMPTY)),
                                new J.Empty(randomId(), sourceBefore("]"), Markers.EMPTY), null))));
            } else {
                entries = JContainer.build(visitRightPadded(map.getMapEntryExpressions().toArray(new ASTNode[0]), "]"));
            }
            queue.add(new G.MapLiteral(randomId(), prefix, Markers.EMPTY, entries, typeMapping.type(map.getType())));
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            Space fmt = whitespace();

            ImplicitDot implicitDot = null;
            JRightPadded<Expression> select = null;
            if (!call.isImplicitThis()) {
                Expression selectExpr = visit(call.getObjectExpression());
                int saveCursor = cursor;
                Space afterSelect = whitespace();
                if (source.charAt(cursor) == '.' || source.charAt(cursor) == '?' || source.charAt(cursor) == '*') {
                    cursor = saveCursor;
                    afterSelect = sourceBefore(call.isSpreadSafe() ? "*." : call.isSafe() ? "?." : ".");
                } else {
                    implicitDot = new ImplicitDot(randomId());
                }
                select = JRightPadded.build(selectExpr).withAfter(afterSelect);
            }

            J.Identifier name = new J.Identifier(randomId(), sourceBefore(call.getMethodAsString()), Markers.EMPTY,
                    call.getMethodAsString(), null, null);

            if (call.isSpreadSafe()) {
                name = name.withMarkers(name.getMarkers().add(new StarDot(randomId())));
            }
            if (call.isSafe()) {
                name = name.withMarkers(name.getMarkers().add(new NullSafe(randomId())));
            }
            if (implicitDot != null) {
                name = name.withMarkers(name.getMarkers().add(implicitDot));
            }
            // Method invocations may have type information that can enrich the type information of its parameters
            Markers markers = Markers.EMPTY;
            if (call.getArguments() instanceof ArgumentListExpression) {
                ArgumentListExpression args = (ArgumentListExpression) call.getArguments();
                if (call.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE) != null) {
                    for (org.codehaus.groovy.ast.expr.Expression arg : args.getExpressions()) {
                        if (!(arg instanceof ClosureExpression)) {
                            continue;
                        }
                        ClosureExpression cl = (ClosureExpression) arg;
                        ClassNode actualParamTypeRaw = call.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                        for (Parameter p : cl.getParameters()) {
                            if (p.isDynamicTyped()) {
                                p.setType(actualParamTypeRaw);
                                p.removeNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                            }
                        }
                        for (Map.Entry<String, Variable> declaredVariable : cl.getVariableScope().getDeclaredVariables().entrySet()) {
                            if (declaredVariable.getValue() instanceof Parameter && declaredVariable.getValue().isDynamicTyped()) {
                                Parameter p = (Parameter) declaredVariable.getValue();
                                p.setType(actualParamTypeRaw);
                                p.removeNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                            }
                        }
                    }
                }
                // handle the obscure case where there are empty parens ahead of a closure
                if (args.getExpressions().size() == 1 && args.getExpressions().get(0) instanceof ClosureExpression) {
                    int saveCursor = cursor;
                    Space prefix = whitespace();
                    if (source.charAt(cursor) == '(') {
                        cursor += 1;
                        Space infix = whitespace();
                        if (source.charAt(cursor) == ')') {
                            cursor += 1;
                            markers = markers.add(new EmptyArgumentListPrecedesArgument(randomId(), prefix, infix));
                        } else {
                            cursor = saveCursor;
                        }
                    } else {
                        cursor = saveCursor;
                    }
                }
            }
            JContainer<Expression> args = visit(call.getArguments());

            MethodNode methodNode = (MethodNode) call.getNodeMetaData().get(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
            JavaType.Method methodType = null;
            if (methodNode == null && call.getObjectExpression() instanceof VariableExpression
                && ((VariableExpression) call.getObjectExpression()).getAccessedVariable() != null) {
                // Groovy doesn't know what kind of object this method is being invoked on
                // But if this invocation is inside a Closure we may have already enriched its parameters with types from the static type checker
                // Use any such type information to attempt to find a matching method
                ClassNode parameterType = staticType(((VariableExpression) call.getObjectExpression()).getAccessedVariable());
                if (args.getElements().size() == 1 && args.getElements().get(0) instanceof J.Empty) {
                    methodType = typeMapping.methodType(parameterType.getMethod(name.getSimpleName(), new Parameter[]{}));
                } else if (call.getArguments() instanceof ArgumentListExpression) {
                    List<org.codehaus.groovy.ast.expr.Expression> rawArgs = ((ArgumentListExpression) call.getArguments()).getExpressions();
                    /*
                      Look through the methods returning the closest match on a best-effort basis
                      Factors which can result in a less accurate match, or no match, include:
                          * The type of each parameter may or may not be known to us
                          * Usage of Groovy's "named parameters" syntactic sugaring throwing off argument count and order
                    */
                    methodLoop:
                    for (MethodNode candidateMethod : parameterType.getAllDeclaredMethods()) {
                        if (!name.getSimpleName().equals(candidateMethod.getName())) {
                            continue;
                        }
                        if (rawArgs.size() != candidateMethod.getParameters().length) {
                            continue;
                        }
                        // Better than nothing
                        methodType = typeMapping.methodType(candidateMethod);

                        // If all parameter types agree then we have found an exact match
                        for (int i = 0; i < candidateMethod.getParameters().length; i++) {
                            JavaType param = typeMapping.type(staticType(candidateMethod.getParameters()[i]));
                            JavaType arg = typeMapping.type(staticType(rawArgs.get(i)));
                            if (!TypeUtils.isAssignableTo(param, arg)) {
                                continue methodLoop;
                            }
                        }
                        break;
                    }
                }

            } else {
                methodType = typeMapping.methodType(methodNode);
            }
            queue.add(new J.MethodInvocation(randomId(), fmt, markers,
                    select, null, name, args, methodType));
        }

        @Override
        public void visitPropertyExpression(PropertyExpression prop) {
            Space fmt = whitespace();
            Expression target = visit(prop.getObjectExpression());
            Space beforeDot = prop.isSafe() ? sourceBefore("?.") :
                    sourceBefore(prop.isSpreadSafe() ? "*." : ".");
            J name = visit(prop.getProperty());
            if (name instanceof J.Literal) {
                String nameStr = ((J.Literal) name).getValueSource();
                assert nameStr != null;
                name = new J.Identifier(randomId(), name.getPrefix(), Markers.EMPTY, nameStr, null, null);
            }
            if (prop.isSpreadSafe()) {
                name = name.withMarkers(name.getMarkers().add(new StarDot(randomId())));
            }
            if (prop.isSafe()) {
                name = name.withMarkers(name.getMarkers().add(new NullSafe(randomId())));
            }
            queue.add(new J.FieldAccess(randomId(), fmt, Markers.EMPTY, target, padLeft(beforeDot, (J.Identifier) name), null));
        }

        @Override
        public void visitReturnStatement(ReturnStatement retn) {
            Space fmt = sourceBefore("return");
            if (retn.getExpression() instanceof ConstantExpression && isSynthetic(retn.getExpression()) &&
                (((ConstantExpression) retn.getExpression()).getValue() == null)) {
                queue.add(new J.Return(randomId(), fmt, Markers.EMPTY, null));
            } else {
                queue.add(new J.Return(randomId(), fmt, Markers.EMPTY, visit(retn.getExpression())));
            }
        }

        @Override
        public void visitShortTernaryExpression(ElvisOperatorExpression ternary) {
            Space fmt = whitespace();
            Expression trueExpr = visit(ternary.getBooleanExpression());
            J.Ternary elvis = new J.Ternary(randomId(), fmt, Markers.EMPTY,
                    trueExpr,
                    padLeft(sourceBefore("?"), trueExpr),
                    padLeft(sourceBefore(":"), visit(ternary.getFalseExpression())),
                    typeMapping.type(staticType(ternary)));
            elvis = elvis.withMarkers(elvis.getMarkers().add(new Elvis(randomId())));
            queue.add(elvis);
        }

        @Override
        public void visitSwitch(SwitchStatement statement) {
            queue.add(new J.Switch(
                    randomId(),
                    sourceBefore("switch"),
                    Markers.EMPTY,
                    new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                            JRightPadded.build((Expression) visit(statement.getExpression())).withAfter(sourceBefore(")"))),
                    new J.Block(
                            randomId(), sourceBefore("{"), Markers.EMPTY,
                            JRightPadded.build(false),
                            visitRightPadded(statement.getCaseStatements().toArray(new CaseStatement[0]), null),
                            sourceBefore("}"))));
        }

        @Override
        public void visitSynchronizedStatement(SynchronizedStatement statement) {
            Space fmt = sourceBefore("synchronized");
            queue.add(new J.Synchronized(randomId(), fmt, Markers.EMPTY,
                    new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                            JRightPadded.build((Expression) visit(statement.getExpression())).withAfter(sourceBefore(")"))),
                    visit(statement.getCode())));
        }

        @Override
        public void visitTernaryExpression(TernaryExpression ternary) {
            queue.add(new J.Ternary(randomId(), whitespace(), Markers.EMPTY,
                    visit(ternary.getBooleanExpression()),
                    padLeft(sourceBefore("?"), visit(ternary.getTrueExpression())),
                    padLeft(sourceBefore(":"), visit(ternary.getFalseExpression())),
                    typeMapping.type(ternary.getType())));
        }

        @Override
        public void visitThrowStatement(ThrowStatement statement) {
            Space fmt = sourceBefore("throw");
            queue.add(new J.Throw(randomId(), fmt, Markers.EMPTY, visit(statement.getExpression())));
        }

        // the current understanding is that TupleExpression only exist as method invocation arguments.
        // this is the reason behind the simplifying assumption that there is one expression, and it is
        // a NamedArgumentListExpression.
        @Override
        public void visitTupleExpression(TupleExpression tuple) {
            int saveCursor = cursor;
            Space beforeOpenParen = whitespace();

            org.openrewrite.java.marker.OmitParentheses omitParentheses = null;
            if (source.charAt(cursor) == '(') {
                cursor++;
            } else {
                omitParentheses = new org.openrewrite.java.marker.OmitParentheses(randomId());
                beforeOpenParen = EMPTY;
                cursor = saveCursor;
            }

            List<JRightPadded<Expression>> args = new ArrayList<>(tuple.getExpressions().size());
            for (org.codehaus.groovy.ast.expr.Expression expression : tuple.getExpressions()) {
                NamedArgumentListExpression namedArgList = (NamedArgumentListExpression) expression;
                List<MapEntryExpression> mapEntryExpressions = namedArgList.getMapEntryExpressions();
                for (int i = 0; i < mapEntryExpressions.size(); i++) {
                    Expression arg = visit(mapEntryExpressions.get(i));
                    if (omitParentheses != null) {
                        arg = arg.withMarkers(arg.getMarkers().add(omitParentheses));
                    }

                    Space after = EMPTY;
                    if (i == mapEntryExpressions.size() - 1) {
                        if (omitParentheses == null) {
                            after = sourceBefore(")");
                        }
                    } else {
                        after = whitespace();
                        if (source.charAt(cursor) == ')') {
                            // the next argument will have an OmitParentheses marker
                            omitParentheses = new org.openrewrite.java.marker.OmitParentheses(randomId());
                        }
                        cursor++;
                    }

                    args.add(JRightPadded.build(arg).withAfter(after));
                }
            }

            queue.add(JContainer.build(beforeOpenParen, args, Markers.EMPTY));
        }

        @Override
        public void visitTryCatchFinally(TryCatchStatement node) {
            Space prefix = sourceBefore("try");
            // Recent versions of groovy support try-with-resources, usage of this pattern in groovy is uncommon
            JContainer<J.Try.Resource> resources = null;
            J.Block body = visit(node.getTryStatement());
            List<J.Try.Catch> catches;
            if (node.getCatchStatements().isEmpty()) {
                catches = null;
            } else {
                catches = new ArrayList<>(node.getCatchStatements().size());
                for (CatchStatement catchStatement : node.getCatchStatements()) {
                    visitCatchStatement(catchStatement);
                    catches.add((J.Try.Catch) queue.poll());
                }
            }

            // Strangely, groovy parses the finally's block as a BlockStatement which contains another BlockStatement
            // The true contents of the block are within the first statement of this apparently pointless enclosing BlockStatement
            JLeftPadded<J.Block> finallyy = !(node.getFinallyStatement() instanceof BlockStatement) ? null :
                    padLeft(sourceBefore("finally"), visit(((BlockStatement) node.getFinallyStatement()).getStatements().get(0)));

            queue.add(new J.Try(randomId(), prefix, Markers.EMPTY, resources, body, catches, finallyy));

        }

        @Override
        public void visitPostfixExpression(PostfixExpression unary) {
            Space fmt = whitespace();
            Expression expression = visit(unary.getExpression());

            Space operatorPrefix = whitespace();
            String typeToken = unary.getOperation().getText();
            cursor += typeToken.length();

            J.Unary.Type operator = null;
            switch (typeToken) {
                case "++":
                    operator = J.Unary.Type.PostIncrement;
                    break;
                case "--":
                    operator = J.Unary.Type.PostDecrement;
                    break;
            }
            assert operator != null;

            queue.add(new J.Unary(randomId(), fmt, Markers.EMPTY,
                    JLeftPadded.build(operator).withBefore(operatorPrefix),
                    expression, null));
        }

        @Override
        public void visitPrefixExpression(PrefixExpression unary) {
            Space fmt = whitespace();
            String typeToken = unary.getOperation().getText();
            cursor += typeToken.length();

            J.Unary.Type operator = null;
            switch (typeToken) {
                case "++":
                    operator = J.Unary.Type.PreIncrement;
                    break;
                case "--":
                    operator = J.Unary.Type.PreDecrement;
                    break;
                case "~":
                    operator = J.Unary.Type.Complement;
                    break;
            }

            assert operator != null;
            queue.add(new J.Unary(randomId(), fmt, Markers.EMPTY,
                    JLeftPadded.build(operator),
                    visit(unary.getExpression()),
                    null));
        }

        public TypeTree visitVariableExpressionType(VariableExpression expression) {
            JavaType type = typeMapping.type(staticType(((org.codehaus.groovy.ast.expr.Expression) expression)));

            if (expression.isDynamicTyped()) {
                return new J.Identifier(randomId(),
                        sourceBefore("def"),
                        Markers.EMPTY,
                        "def",
                        type, null);
            }
            Space prefix = sourceBefore(expression.getOriginType().getUnresolvedName());
            J.Identifier ident = new J.Identifier(randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    expression.getOriginType().getUnresolvedName(),
                    type, null);
            if (type instanceof JavaType.Parameterized) {
                return new J.ParameterizedType(randomId(), prefix, Markers.EMPTY, ident, visitTypeParameterizations(
                        staticType((org.codehaus.groovy.ast.expr.Expression) expression).getGenericsTypes()));
            }
            return ident.withPrefix(prefix);
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            JavaType type;
            if (expression.isDynamicTyped() && expression.getAccessedVariable() != null && expression.getAccessedVariable().getType() != expression.getOriginType()) {
                type = typeMapping.type(staticType(expression.getAccessedVariable()));
            } else {
                type = typeMapping.type(staticType((org.codehaus.groovy.ast.expr.Expression) expression));
            }
            queue.add(new J.Identifier(randomId(),
                    sourceBefore(expression.getName()),
                    Markers.EMPTY,
                    expression.getName(),
                    type, null)
            );
        }

        @Override
        public void visitWhileLoop(WhileStatement loop) {
            Space fmt = sourceBefore("while");
            queue.add(new J.WhileLoop(randomId(), fmt, Markers.EMPTY,
                    new J.ControlParentheses<>(randomId(), sourceBefore("("), Markers.EMPTY,
                            JRightPadded.build((Expression) visit(loop.getBooleanExpression().getExpression()))
                                    .withAfter(sourceBefore(")"))),
                    JRightPadded.build(visit(loop.getLoopBlock()))
            ));
        }

        @SuppressWarnings({"unchecked", "ConstantConditions"})
        private <T> T pollQueue() {
            return (T) queue.poll();
        }
    }

    private JRightPadded<Statement> convertTopLevelStatement(SourceUnit unit, ASTNode node) {
        if (node instanceof ClassNode) {
            ClassNode classNode = (ClassNode) node;
            RewriteGroovyClassVisitor classVisitor = new RewriteGroovyClassVisitor(unit);
            classVisitor.visitClass(classNode);
            return JRightPadded.build(classVisitor.pollQueue());
        } else if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            RewriteGroovyClassVisitor classVisitor = new RewriteGroovyClassVisitor(unit);
            classVisitor.visitMethod(methodNode);
            return JRightPadded.build(classVisitor.pollQueue());
        } else if (node instanceof ImportNode) {
            ImportNode importNode = (ImportNode) node;
            Space prefix = sourceBefore("import");
            JLeftPadded<Boolean> statik;
            if (importNode.isStatic()) {
                statik = padLeft(sourceBefore("static"), true);
            } else {
                statik = padLeft(EMPTY, false);
            }
            String packageName = importNode.getPackageName();
            J.FieldAccess qualid;
            if (packageName == null) {
                // Groovy implicitly imports various packages, including java.lang, in every source file
                // If you explicitly import one of these, or something from a subpackage that does require explicit import like java.lang.annotation.Retention
                // then ImportNode.getPackageName() may return null
                Space space = whitespace();
                qualid = TypeTree.build(name()).withPrefix(space);
            } else {
                if (importNode.isStar()) {
                    packageName += "*";
                }
                qualid = TypeTree.build(packageName).withPrefix(sourceBefore(packageName));
            }

            J.Import anImport = new J.Import(randomId(), prefix, Markers.EMPTY, statik, qualid);
            return maybeSemicolon(anImport);
        }

        RewriteGroovyVisitor groovyVisitor = new RewriteGroovyVisitor(node, new RewriteGroovyClassVisitor(unit));
        node.visit(groovyVisitor);
        return maybeSemicolon(groovyVisitor.pollQueue());
    }

    private static LineColumn pos(ASTNode node) {
        return new LineColumn(node.getLineNumber(), node.getColumnNumber());
    }

    private static boolean isSynthetic(ASTNode node) {
        return node.getLineNumber() == -1;
    }

    @Value
    private static class LineColumn implements Comparable<LineColumn> {
        int line;
        int column;

        @Override
        public int compareTo(@NotNull GroovyParserVisitor.LineColumn lc) {
            return line != lc.line ? line - lc.line : column - lc.column;
        }
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private int positionOfNext(String untilDelim) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment) {
                if (source.charAt(delimIndex) == '\n') {
                    inSingleLineComment = false;
                }
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = true;
                            delimIndex++;
                            break;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            break;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex = delimIndex + 2;
                            break;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    private Space whitespace() {
        String prefix = source.substring(cursor, indexOfNextNonWhitespace(cursor, source));
        cursor += prefix.length();
        return format(prefix);
    }

    private <T extends TypeTree & Expression> T typeTree(@Nullable ClassNode classNode) {
        Space prefix = whitespace();
        String maybeFullyQualified = name();
        String[] parts = maybeFullyQualified.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, part, typeMapping.type(classNode), null);
            } else {
                fullName += "." + part;

                Matcher whitespacePrefix = whitespacePrefixPattern.matcher(part);
                Space identFmt = whitespacePrefix.matches() ? format(whitespacePrefix.group(0)) : Space.EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                //noinspection ResultOfMethodCallIgnored
                whitespaceSuffix.matches();
                Space namePrefix = i == parts.length - 1 ? Space.EMPTY : format(whitespaceSuffix.group(1));

                expr = new J.FieldAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        expr,
                        padLeft(namePrefix, new J.Identifier(randomId(), identFmt, Markers.EMPTY, part.trim(), null, null)),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.ShallowClass.build(fullName) :
                                null
                );
            }
        }

        assert expr != null;
        if (classNode != null && classNode.isUsingGenerics() && !classNode.isGenericsPlaceHolder()) {
            expr = new J.ParameterizedType(randomId(), EMPTY, Markers.EMPTY, (NameTree) expr, visitTypeParameterizations(classNode.getGenericsTypes()));
        }
        return expr.withPrefix(prefix);
    }

    private Space sourceBefore(String untilDelim) {
        int delimIndex = positionOfNext(untilDelim);
        if (delimIndex < 0) {
            return EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return Space.format(prefix);
    }

    private TypeTree visitTypeTree(ClassNode classNode) {
        JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(classNode.getUnresolvedName());
        if (primitiveType != null) {
            return new J.Primitive(randomId(), sourceBefore(classNode.getUnresolvedName()), Markers.EMPTY, primitiveType);
        }

        int saveCursor = cursor;
        Space fmt = whitespace();
        if (cursor < source.length() && source.startsWith("def", cursor)) {
            cursor += 3;
            return new J.Identifier(randomId(), fmt, Markers.EMPTY, "def",
                    JavaType.ShallowClass.build("java.lang.Object"), null);
        } else {
            cursor = saveCursor;
        }

        return typeTree(classNode);
    }

    private List<J.Modifier> visitModifiers(int modifiers) {
        List<J.Modifier> unorderedModifiers = new ArrayList<>();

        if ((modifiers & Opcodes.ACC_ABSTRACT) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Abstract, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_FINAL) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Final, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_PRIVATE) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Private, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_PROTECTED) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Protected, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_PUBLIC) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Public, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_STATIC) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Static, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_SYNCHRONIZED) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Synchronized, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_TRANSIENT) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Transient, emptyList()));
        }
        if ((modifiers & Opcodes.ACC_VOLATILE) != 0) {
            unorderedModifiers.add(new J.Modifier(randomId(), EMPTY, Markers.EMPTY, J.Modifier.Type.Volatile, emptyList()));
        }

        List<J.Modifier> orderedModifiers = new ArrayList<>(unorderedModifiers.size());
        boolean foundModifier = true;
        nextModifier:
        while (foundModifier) {
            int saveCursor = cursor;
            Space fmt = whitespace();
            for (J.Modifier mod : unorderedModifiers) {
                String modName = mod.getType().name().toLowerCase();
                if (source.startsWith(modName, cursor)) {
                    orderedModifiers.add(mod.withPrefix(fmt));
                    unorderedModifiers.remove(mod);
                    cursor += modName.length();
                    continue nextModifier;
                }
            }
            foundModifier = false;
            cursor = saveCursor;
        }

        return orderedModifiers;
    }

    private <G2 extends J> JRightPadded<G2> maybeSemicolon(G2 g) {
        int saveCursor = cursor;
        Space beforeSemi = whitespace();
        Semicolon semicolon = null;
        if (cursor < source.length() && source.charAt(cursor) == ';') {
            semicolon = new Semicolon(randomId());
            cursor++;
        } else {
            beforeSemi = EMPTY;
            cursor = saveCursor;
        }

        JRightPadded<G2> paddedG = JRightPadded.build(g).withAfter(beforeSemi);
        if (semicolon != null) {
            paddedG = paddedG.withMarkers(paddedG.getMarkers().add(semicolon));
        }

        return paddedG;
    }

    private String name() {
        int i = cursor;
        char c = source.charAt(i);
        while (Character.isJavaIdentifierPart(c) || c == '.' || c == '*') {
            i++;
            c = source.charAt(i);
        }
        String result = source.substring(cursor, i);
        cursor += i - cursor;
        return result;
    }

    /*
       Visit the value filled in to a parameterized type, such as in a variable declaration.
       Not to be confused with the declaration of a type parameter on a class or method.

       Can contain a J.Identifier, as is the case in a typical variable declaration:
           List<String>
           Map<T, Y>
       Can contain a J.Blank, as in a diamond operator:
           List<String> s = new ArrayList< >()
       Can contain a J.FieldAccess, as in a variable declaration with fully qualified type parameterization:
           List<java.lang.String>
     */
    private JContainer<Expression> visitTypeParameterizations(@Nullable GenericsType[] genericsTypes) {
        Space prefix = sourceBefore("<");
        List<JRightPadded<Expression>> parameters;

        //noinspection ConstantConditions
        if (genericsTypes == null) {
            // Groovy compiler does not always bother to record type parameter info in places it does not care about
            Space paramPrefix = whitespace();
            if (source.charAt(cursor) == '>') {
                parameters = singletonList(JRightPadded.build(new J.Empty(randomId(), paramPrefix, Markers.EMPTY)));
            } else {
                parameters = new ArrayList<>();
                while (true) {
                    Expression param = typeTree(null).withPrefix(paramPrefix);
                    Space suffix = whitespace();
                    parameters.add(JRightPadded.build(param).withAfter(suffix));
                    if (source.charAt(cursor) == '>') {
                        cursor++;
                        break;
                    }
                    cursor++;
                    paramPrefix = whitespace();
                }
            }
        } else {
            parameters = new ArrayList<>(genericsTypes.length);
            for (int i = 0; i < genericsTypes.length; i++) {
                parameters.add(JRightPadded.build(visitTypeParameterization(genericsTypes[i]))
                        .withAfter(
                                i < genericsTypes.length - 1 ?
                                        sourceBefore(",") :
                                        sourceBefore(">")
                        ));
            }
        }


        return JContainer.build(prefix, parameters, Markers.EMPTY);
    }

    private Expression visitTypeParameterization(GenericsType genericsType) {
        int saveCursor = cursor;
        Space prefix = whitespace();
        if (source.charAt(cursor) == '?') {
            cursor = saveCursor;
            return visitWildcard(genericsType);
        } else if (source.charAt(cursor) == '>') {
            cursor++;
            return new J.Empty(randomId(), prefix, Markers.EMPTY);
        }
        cursor = saveCursor;
        return typeTree(null)
                .withType(typeMapping.type(genericsType));
    }

    /*
      Visit the declaration of a type parameter as part of a class declaration or method declaration
     */
    private JContainer<J.TypeParameter> visitTypeParameters(GenericsType[] genericsTypes) {
        Space prefix = sourceBefore("<");
        List<JRightPadded<J.TypeParameter>> typeParameters = new ArrayList<>(genericsTypes.length);
        for (int i = 0; i < genericsTypes.length; i++) {
            typeParameters.add(JRightPadded.build(visitTypeParameter(genericsTypes[i]))
                    .withAfter(
                            i < genericsTypes.length - 1 ?
                                    sourceBefore(",") :
                                    sourceBefore(">")
                    ));
        }
        return JContainer.build(prefix, typeParameters, Markers.EMPTY);
    }

    private J.TypeParameter visitTypeParameter(GenericsType genericType) {
        Space prefix = whitespace();
        Expression name = typeTree(null)
                .withType(typeMapping.type(genericType));
        JContainer<TypeTree> bounds = null;
        if (genericType.getUpperBounds() != null) {
            Space boundsPrefix = sourceBefore("extends");
            ClassNode[] upperBounds = genericType.getUpperBounds();
            List<JRightPadded<TypeTree>> convertedBounds = new ArrayList<>(upperBounds.length);
            for (int i = 0; i < upperBounds.length; i++) {
                convertedBounds.add(JRightPadded.build(visitTypeTree(upperBounds[i]))
                        .withAfter(
                                i < upperBounds.length - 1 ?
                                        sourceBefore("&") :
                                        EMPTY
                        ));
            }
            bounds = JContainer.build(boundsPrefix, convertedBounds, Markers.EMPTY);
        } else if (genericType.getLowerBound() != null) {
            Space boundsPrefix = sourceBefore("super");
            ClassNode lowerBound = genericType.getLowerBound();
            List<JRightPadded<TypeTree>> convertedBounds = new ArrayList<>(1);
            convertedBounds.add(JRightPadded.build(visitTypeTree(lowerBound))
                    .withAfter(EMPTY));
            bounds = JContainer.build(boundsPrefix, convertedBounds, Markers.EMPTY);
        }
        return new J.TypeParameter(randomId(), prefix, Markers.EMPTY, emptyList(), name, bounds);
    }

    private J.Wildcard visitWildcard(GenericsType genericType) {
        Space namePrefix = sourceBefore("?");

        JLeftPadded<J.Wildcard.Bound> bound;
        NameTree boundedType;
        if (genericType.getUpperBounds() != null) {
            bound = padLeft(sourceBefore("extends"), J.Wildcard.Bound.Extends);
            boundedType = visitTypeTree(genericType.getUpperBounds()[0]);
        } else if (genericType.getLowerBound() != null) {
            bound = padLeft(sourceBefore("super"), J.Wildcard.Bound.Super);
            boundedType = visitTypeTree(genericType.getLowerBound());
        } else {
            bound = null;
            boundedType = null;
        }

        return new J.Wildcard(randomId(), namePrefix, Markers.EMPTY, bound, boundedType);
    }

    /**
     * Sometimes the groovy compiler inserts phantom elements into argument lists and class bodies,
     * presumably to pass type information around. These elements do not appear in source code and should not
     * be represented in our AST.
     *
     * @param node possible phantom node
     * @return true if the node reports that it does have a position within the source code
     */
    private static boolean appearsInSource(ASTNode node) {
        return node.getColumnNumber() >= 0 && node.getLineNumber() >= 0 && node.getLastColumnNumber() >= 0 && node.getLastLineNumber() >= 0;
    }

    /**
     * Static type checking for groovy is an add-on that places the type information it discovers into expression metadata.
     *
     * @param expression which may have more accurate type information in its metadata
     * @return most accurate available type of the supplied node
     */
    private static ClassNode staticType(org.codehaus.groovy.ast.expr.Expression expression) {
        ClassNode inferred = (ClassNode) expression.getNodeMetaData().get(StaticTypesMarker.INFERRED_TYPE);
        if (inferred == null) {
            return expression.getType();
        } else {
            return inferred;
        }
    }

    private static ClassNode staticType(Variable variable) {
        if (variable instanceof Parameter) {
            return staticType((Parameter) variable);
        } else if (variable instanceof org.codehaus.groovy.ast.expr.Expression) {
            return staticType((org.codehaus.groovy.ast.expr.Expression) variable);
        }
        return variable.getType();
    }

    private static ClassNode staticType(Parameter parameter) {
        ClassNode inferred = (ClassNode) parameter.getNodeMetaData().get(StaticTypesMarker.INFERRED_TYPE);
        if (inferred == null) {
            return parameter.getType();
        } else {
            return inferred;
        }
    }
}
