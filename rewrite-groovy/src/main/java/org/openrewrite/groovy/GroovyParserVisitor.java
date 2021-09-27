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
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.marker.OmitParentheses;
import org.openrewrite.groovy.marker.Semicolon;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

/**
 * See the <a href="https://groovy-lang.org/syntax.html">language syntax reference</a>.
 */
@RequiredArgsConstructor
public class GroovyParserVisitor {
    private final Path sourcePath;
    private final String source;
    private final boolean relaxedClassTypeMatching;
    private final Map<String, JavaType.Class> sharedClassTypes;
    private final ExecutionContext ctx;

    private int cursor = 0;

    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    public G.CompilationUnit visit(SourceUnit unit, ModuleNode ast) {
        NavigableMap<LineColumn, List<ASTNode>> sortedByPosition = new TreeMap<>();
        for (org.codehaus.groovy.ast.stmt.Statement s : ast.getStatementBlock().getStatements()) {
            if (!isSynthetic(s)) {
                sortedByPosition.computeIfAbsent(pos(s), i -> new ArrayList<>()).add(s);
            }
        }

        JRightPadded<J.Package> pkg = null;
        if (ast.getPackage() != null) {
            cursor += "package".length();
            String pkgName = ast.getPackage().getName();
            if (pkgName.endsWith(".")) {
                pkgName = pkgName.substring(0, pkgName.length() - 1);
            }
            pkg = padRight(new J.Package(randomId(), EMPTY, Markers.EMPTY,
                    buildName(pkgName), emptyList()), EMPTY);
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
            if (aClass.getSuperClass() == null || !aClass.getSuperClass().getName().equals("groovy.lang.Script")) {
                sortedByPosition.computeIfAbsent(pos(aClass), i -> new ArrayList<>()).add(aClass);
            }
        }

        for (MethodNode method : ast.getMethods()) {
            sortedByPosition.computeIfAbsent(pos(method), i -> new ArrayList<>()).add(method);
        }

        List<JRightPadded<Statement>> statements = new ArrayList<>(sortedByPosition.size());
        for (List<ASTNode> values : sortedByPosition.values()) {
            for (ASTNode value : values) {
                statements.add(convertTopLevelStatement(unit, ast, value));
            }
        }

        return new G.CompilationUnit(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                sourcePath,
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
        public void visitMethod(MethodNode node) {
            Space fmt = sourceBefore("def");

            List<J.Modifier> modifiers = visitModifiers(node.getModifiers());

            TypeTree returnType = visitTypeTree(node.getReturnType());

            J.Identifier name = J.Identifier.build(randomId(),
                    sourceBefore(node.getName()),
                    Markers.EMPTY,
                    node.getName(),
                    null);

            RewriteGroovyVisitor bodyVisitor = new RewriteGroovyVisitor();

            // Parameter has no visit implementation, so we've got to do this by hand
            Space beforeParen = sourceBefore("(");
            List<JRightPadded<Statement>> params = new ArrayList<>(node.getParameters().length);
            Parameter[] unparsedParams = node.getParameters();
            for (int i = 0; i < unparsedParams.length; i++) {
                Parameter param = unparsedParams[i];
                TypeTree paramType = visitTypeTree(param.getType());
                JRightPadded<J.VariableDeclarations.NamedVariable> paramName = JRightPadded.build(
                        new J.VariableDeclarations.NamedVariable(randomId(), whitespace(), Markers.EMPTY,
                                J.Identifier.build(randomId(), EMPTY, Markers.EMPTY, param.getName(), null),
                                emptyList(), null, null)
                );
                cursor += param.getName().length();

                Space rightPad = sourceBefore(i == unparsedParams.length - 1 ? ")" : ",");

                params.add(JRightPadded.build((Statement) new J.VariableDeclarations(randomId(), paramType.getPrefix(),
                        Markers.EMPTY, emptyList(), emptyList(), paramType.withPrefix(EMPTY),
                        null, emptyList(),
                        singletonList(paramName))).withAfter(rightPad));
            }

            J.Block body = bodyVisitor.visit(node.getCode());

            queue.add(new J.MethodDeclaration(
                    randomId(), fmt, Markers.EMPTY,
                    emptyList(),
                    modifiers,
                    null,
                    returnType,
                    new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                    JContainer.build(beforeParen, params, Markers.EMPTY),
                    null,
                    body,
                    null,
                    null
            ));
        }

        @SuppressWarnings({"ConstantConditions", "unchecked"})
        private <T> T pollQueue() {
            return (T) queue.poll();
        }
    }

    private class RewriteGroovyVisitor extends CodeVisitorSupport {
        private final Stack<ASTNode> nodeStack = new Stack<>();
        private final Queue<Object> queue = new LinkedList<>();

        private <T> T visit(ASTNode node) {
            nodeStack.push(node);
            node.visit(this);
            nodeStack.pop();
            return pollQueue();
        }

        private <T> List<JRightPadded<T>> visitRightPadded(List<? extends ASTNode> nodes, char lastPadTo) {
            List<JRightPadded<T>> ts = new ArrayList<>(nodes.size());
            for (int i = 0; i < nodes.size(); i++) {
                ASTNode node = nodes.get(i);

                Space pad;
                if (i == nodes.size() - 1) {
                    int saveCursor = cursor;
                    pad = whitespace();
                    if (cursor >= source.length() || source.charAt(cursor) != lastPadTo) {
                        cursor = saveCursor;
                    }
                } else {
                    pad = whitespace();
                }

                //noinspection unchecked
                ts.add((JRightPadded<T>) JRightPadded.build(visit(node)).withAfter(pad));
            }
            return ts;
        }

        private <T> List<JRightPadded<T>> visitRightPadded(ASTNode[] nodes, char lastPadTo) {
            List<JRightPadded<T>> ts = new ArrayList<>(nodes.length);
            for (ASTNode node : nodes) {
                //noinspection unchecked
                ts.add((JRightPadded<T>) JRightPadded.build(visit(node)).withAfter(whitespace()));
            }
            return ts;
        }

        private <T> List<T> visit(ASTNode[] nodes) {
            List<T> ts = new ArrayList<>(nodes.length);
            for (ASTNode node : nodes) {
                ts.add(visit(node));
            }
            return ts;
        }

        @Override
        public void visitBlockStatement(BlockStatement block) {
            Space fmt = EMPTY;
            if (!(nodeStack.peek() instanceof ClosureExpression)) {
                fmt = sourceBefore("{");
            }
            List<JRightPadded<Statement>> statements = new ArrayList<>(block.getStatements().size());
            for (ASTNode statement : block.getStatements()) {
                JRightPadded<Statement> stat = JRightPadded.build(visit(statement));
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
            if (!(nodeStack.peek() instanceof ClosureExpression)) {
                sourceBefore("}");
            }
        }

        @Override
        public void visitConstantExpression(ConstantExpression expression) {
            Space prefix = whitespace();

            JavaType.Primitive jType;
            String text = expression.getText();

            ClassNode type = expression.getType();
            if (type == ClassHelper.boolean_TYPE) {
                jType = JavaType.Primitive.Boolean;
            } else if (type == ClassHelper.byte_TYPE) {
                jType = JavaType.Primitive.Byte;
            } else if (type == ClassHelper.char_TYPE) {
                jType = JavaType.Primitive.Char;
            } else if (type == ClassHelper.double_TYPE) {
                jType = JavaType.Primitive.Double;
            } else if (type == ClassHelper.float_TYPE) {
                jType = JavaType.Primitive.Float;
            } else if (type == ClassHelper.int_TYPE) {
                jType = JavaType.Primitive.Int;
            } else if (type == ClassHelper.long_TYPE) {
                jType = JavaType.Primitive.Long;
            } else if (type == ClassHelper.short_TYPE) {
                jType = JavaType.Primitive.Short;
            } else if (type == ClassHelper.STRING_TYPE) {
                jType = JavaType.Primitive.String;
                if (source.startsWith("'", cursor)) {
                    text = "'" + text + "'";
                }
            } else if (expression.isNullExpression()) {
                text = "null";
                jType = JavaType.Primitive.Null;
            } else {
                throw new IllegalStateException("Unexpected constant type " + type);
            }

            cursor += text.length();
            queue.add(new J.Literal(randomId(), prefix, Markers.EMPTY, expression.getValue(), text,
                    null, jType));
        }

        @Override
        public void visitClosureExpression(ClosureExpression expression) {
            Space prefix = whitespace();
            cursor += 1; // skip '{'

            J.Lambda.Parameters params = new J.Lambda.Parameters(randomId(), EMPTY, Markers.EMPTY, false, visitRightPadded(expression.getParameters(), '-'));
            Space arrow = EMPTY;
            if (!params.getParameters().isEmpty()) {
                arrow = whitespace();
                cursor += 2;
            }

            queue.add(new J.Lambda(randomId(), prefix, Markers.EMPTY, params, arrow, visit(expression.getCode()), null));
            cursor += 1; // skip '}'
        }

        @Override
        public void visitDeclarationExpression(DeclarationExpression expression) {
            if (expression.isMultipleAssignmentDeclaration()) {
                // def (a, b) = [1, 2]
                throw new UnsupportedOperationException("FIXME");
            } else {
                VariableExpression variableExpression = expression.getVariableExpression();
                visitVariableExpression(variableExpression);
            }

            J.Identifier name = pollQueue();
            J.VariableDeclarations.NamedVariable namedVariable = pollQueue();

            if (expression.getRightExpression() != null) {
                Space beforeAssign = sourceBefore("=");
                expression.getRightExpression().visit(this);
                Expression initializer = pollQueue();
                namedVariable = namedVariable.getPadding().withInitializer(padLeft(beforeAssign, initializer));
            }

            J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    emptyList(),
                    emptyList(),
                    name,
                    null,
                    emptyList(),
                    singletonList(padRight(namedVariable, EMPTY))
            );

            queue.add(variableDeclarations);
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            Expression select = null;
            if (!call.isImplicitThis()) {
                call.getObjectExpression().visit(this);
                select = pollQueue();
            }

            J.Identifier name = J.Identifier.build(randomId(), whitespace(), Markers.EMPTY,
                    call.getMethodAsString(), null);
            cursor += call.getMethodAsString().length();

            Space beforeOpenParen = EMPTY;
            List<org.codehaus.groovy.ast.expr.Expression> unparsedArgs = ((ArgumentListExpression) call.getArguments()).getExpressions();
            List<JRightPadded<Expression>> args = new ArrayList<>(unparsedArgs.size());
            for (int i = 0; i < unparsedArgs.size(); i++) {
                org.codehaus.groovy.ast.expr.Expression unparsedArg = unparsedArgs.get(i);

                OmitParentheses omitParentheses = null;
                Space pad;
                if (i == 0) {
                    pad = whitespace();
                    if (source.charAt(cursor) != '(') {
                        omitParentheses = new OmitParentheses(randomId());
                    } else {
                        beforeOpenParen = pad;
                        pad = EMPTY;
                        cursor++;
                    }
                } else if (i == unparsedArgs.size() - 1) {
                    int saveCursor = cursor;
                    pad = whitespace();
                    if (cursor >= source.length() || source.charAt(cursor) != ')') {
                        cursor = saveCursor;
                    } else {
                        cursor++;
                    }
                } else {
                    pad = whitespace();
                }

                Expression arg = visit(unparsedArg);
                if (omitParentheses != null) {
                    arg = arg.withMarkers(arg.getMarkers().add(omitParentheses));
                }

                args.add(JRightPadded.build(arg.withPrefix(pad)));
            }

            if (unparsedArgs.isEmpty()) {
                int saveCursor = cursor;
                Space pad = whitespace();
                OmitParentheses omitParentheses = null;
                if (source.charAt(cursor) != '(') {
                    omitParentheses = new OmitParentheses(randomId());
                    pad = EMPTY;
                    cursor = saveCursor;
                } else {
                    cursor++;
                }

                Expression element = new J.Empty(randomId(), pad, Markers.EMPTY);
                if (omitParentheses != null) {
                    element = element.withMarkers(element.getMarkers().add(omitParentheses));
                }

                args.add(JRightPadded.build(element).withAfter(whitespace()));
                cursor++;
            }

            queue.add(new J.MethodInvocation(randomId(), EMPTY, Markers.EMPTY,
                    select == null ? null : JRightPadded.build(select),
                    null, name, JContainer.build(beforeOpenParen, args, Markers.EMPTY), null));
        }

        @Override
        public void visitReturnStatement(ReturnStatement retn) {
            Space fmt = sourceBefore("return");
            queue.add(new J.Return(randomId(), fmt, Markers.EMPTY, visit(retn.getExpression())));
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            JavaType type = type(expression);
            if (expression.isDynamicTyped()) {
                queue.add(J.Identifier.build(randomId(),
                        sourceBefore("def"),
                        Markers.EMPTY,
                        "def",
                        type));
            } else {
                queue.add(J.Identifier.build(randomId(),
                        sourceBefore(expression.getOriginType().getName()),
                        Markers.EMPTY,
                        expression.getOriginType().getName(),
                        type));
            }

            queue.add(new J.VariableDeclarations.NamedVariable(randomId(),
                    sourceBefore(expression.getName()),
                    Markers.EMPTY,
                    J.Identifier.build(randomId(),
                            EMPTY,
                            Markers.EMPTY,
                            expression.getName(),
                            type),
                    emptyList(),
                    null,
                    type
            ));
        }

        @SuppressWarnings({"unchecked", "ConstantConditions"})
        private <T> T pollQueue() {
            return (T) queue.poll();
        }
    }

    private JRightPadded<Statement> convertTopLevelStatement(SourceUnit unit, ModuleNode ast, ASTNode node) {
        if (node instanceof ClassNode) {
            ClassNode classNode = (ClassNode) node;
            RewriteGroovyClassVisitor classVisitor = new RewriteGroovyClassVisitor(unit);
            classVisitor.visitClass(classNode);
            return classVisitor.pollQueue();
        } else if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            RewriteGroovyClassVisitor classVisitor = new RewriteGroovyClassVisitor(unit);
            classVisitor.visitMethod(methodNode);
            return JRightPadded.build(classVisitor.pollQueue());
        }

        for (ClassNode aClass : ast.getClasses()) {
            if (aClass.getSuperClass() != null && aClass.getSuperClass().getName().equals("groovy.lang.Script")) {
                StaticTypeCheckingVisitor staticTypeChecker = new StaticTypeCheckingVisitor(unit, aClass);
                node.visit(staticTypeChecker);
            }
        }

        RewriteGroovyVisitor groovyVisitor = new RewriteGroovyVisitor();
        node.visit(groovyVisitor);
        return padRight(groovyVisitor.pollQueue(), EMPTY);
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

    private <T> JRightPadded<T> padRight(T tree, Space right) {
        return new JRightPadded<>(tree, right, Markers.EMPTY);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment && source.charAt(delimIndex) == '\n') {
                inSingleLineComment = false;
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
                    if (stop != null && source.charAt(delimIndex) == stop)
                        return -1; // reached stop word before finding the delimiter

                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    private Space whitespace() {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length(); delimIndex++) {
            if (inSingleLineComment && (source.charAt(delimIndex) == '\n' || source.charAt(delimIndex) == '\r')) {
                inSingleLineComment = false;
            } else {
                if (source.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = true;
                            delimIndex++;
                            continue;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            continue;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex++;
                            continue;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (!Character.isWhitespace(source.substring(delimIndex, delimIndex + 1).charAt(0))) {
                        break; // found it!
                    }
                }
            }
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length();
        return format(prefix);
    }

    private String skip(@Nullable String token) {
        if (token == null) {
            //noinspection ConstantConditions
            return null;
        }
        if (source.startsWith(token, cursor))
            cursor += token.length();
        return token;
    }

    @Nullable
    private JavaType type(@Nullable ASTNode node) {
        if (node == null || node.getMetaDataMap() == null) {
            return null;
        }
        ClassNode type = (ClassNode) node.getMetaDataMap().get(StaticTypesMarker.INFERRED_TYPE);

        JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(type.getName());
        if (primitive != null) {
            return primitive;
        }

        return JavaType.Class.build(type.getName());
    }

    private <T extends TypeTree & Expression> T buildName(String fullyQualifiedName) {
        Space prefix = whitespace();
        cursor += fullyQualifiedName.length();
        String[] parts = fullyQualifiedName.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = J.Identifier.build(randomId(), EMPTY, Markers.EMPTY, part, null);
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
                        padLeft(namePrefix, J.Identifier.build(randomId(), identFmt, Markers.EMPTY, part.trim(), null)),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.Class.build(fullName) :
                                null
                );
            }
        }

        //noinspection unchecked,ConstantConditions
        return ((T) expr).withPrefix(prefix);
    }

    private Space sourceBefore(String untilDelim) {
        return sourceBefore(untilDelim, null);
    }

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private Space sourceBefore(String untilDelim, @Nullable Character stop) {
        int delimIndex = positionOfNext(untilDelim, stop);
        if (delimIndex < 0) {
            return EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return Space.format(prefix);
    }

    private TypeTree visitTypeTree(ClassNode classNode) {
        if (classNode.getLineNumber() >= 0) {
            JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(classNode.getName());
            if (primitiveType == null) {
                return buildName(classNode.getName());
            }
            Space fmt = whitespace();
            cursor += classNode.getName().length();
            return new J.Primitive(randomId(), fmt, Markers.EMPTY, primitiveType);
        }
        return J.Identifier.build(randomId(), sourceBefore("def"), Markers.EMPTY, "def",
                JavaType.Class.build("java.lang.Object"));
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
}
