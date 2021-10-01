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
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.marker.ImplicitReturn;
import org.openrewrite.groovy.marker.NullSafe;
import org.openrewrite.groovy.marker.OmitParentheses;
import org.openrewrite.groovy.marker.Semicolon;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

/**
 * See the <a href="https://groovy-lang.org/syntax.html">language syntax reference</a>.
 */
public class GroovyParserVisitor {
    private final Path sourcePath;
    private final String source;
    private final TypeMapping typeMapping;
    private final ExecutionContext ctx;

    private int cursor = 0;

    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    public GroovyParserVisitor(Path sourcePath, String source,
                               boolean relaxedClassTypeMatching,
                               Map<String, JavaType.Class> sharedClassTypes,
                               ExecutionContext ctx) {
        this.sourcePath = sourcePath;
        this.source = source;
        this.typeMapping = new TypeMapping(relaxedClassTypeMatching, sharedClassTypes);
        this.ctx = ctx;
    }

    public G.CompilationUnit visit(SourceUnit unit, ModuleNode ast) {
//        for (ClassNode aClass : ast.getClasses()) {
//            new StaticTypeCheckingVisitor(unit, aClass).visitClass(aClass);
//        }

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
            pkg = JRightPadded.build(new J.Package(randomId(), EMPTY, Markers.EMPTY,
                    buildName(pkgName), emptyList()));
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
        public void visitClass(ClassNode clazz) {
            Space fmt = whitespace();
            List<J.Modifier> modifiers = visitModifiers(clazz.getModifiers());

            Space kindPrefix = whitespace();
            J.ClassDeclaration.Kind.Type kindType = null;
            if (source.startsWith("class", cursor)) {
                kindType = J.ClassDeclaration.Kind.Type.Class;
                cursor += "class".length();
            } else if (source.startsWith("interface", cursor)) {
                kindType = J.ClassDeclaration.Kind.Type.Interface;
                cursor += "interface".length();
            }
            assert kindType != null;
            J.ClassDeclaration.Kind kind = new J.ClassDeclaration.Kind(randomId(), kindPrefix, Markers.EMPTY, emptyList(), kindType);

            J.Identifier name = J.Identifier.build(randomId(), sourceBefore(clazz.getName()), Markers.EMPTY, clazz.getName(), null);

            JLeftPadded<TypeTree> extendings = null;
            if (clazz.getSuperClass().getLineNumber() >= 0) {
                extendings = padLeft(sourceBefore("extends"), visitTypeTree(clazz.getSuperClass()));
            }

            JContainer<TypeTree> implementings = null;
            if (clazz.getInterfaces().length > 0) {
                Space implPrefix = sourceBefore("implements");
                List<JRightPadded<TypeTree>> implTypes = new ArrayList<>(clazz.getInterfaces().length);
                ClassNode[] interfaces = clazz.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    ClassNode anInterface = interfaces[i];
                    implTypes.add(JRightPadded.build(visitTypeTree(anInterface))
                            .withAfter(i == interfaces.length - 1 ? EMPTY : sourceBefore(",")));
                }

                implementings = JContainer.build(implPrefix, implTypes, Markers.EMPTY);
            }

            NavigableMap<LineColumn, List<ASTNode>> sortedByPosition = new TreeMap<>();
            for (MethodNode method : clazz.getMethods()) {
                sortedByPosition.computeIfAbsent(pos(method), i -> new ArrayList<>()).add(method);
            }
            for (FieldNode field : clazz.getFields()) {
                sortedByPosition.computeIfAbsent(pos(field), i -> new ArrayList<>()).add(field);
            }

            J.Block body = new J.Block(randomId(), sourceBefore("{"), Markers.EMPTY,
                    JRightPadded.build(false),
                    sortedByPosition.values().stream()
                            .flatMap(asts -> asts.stream()
                                    .map(ast -> {
                                        if (ast instanceof FieldNode) {
                                            visitField((FieldNode) ast);
                                        } else if (ast instanceof MethodNode) {
                                            visitMethod((MethodNode) ast);
                                        }
                                        Statement stat = pollQueue();
                                        return maybeSemicolon(stat);
                                    }))
                            .collect(Collectors.toList()),
                    sourceBefore("}"));

            queue.add(new J.ClassDeclaration(randomId(), fmt, Markers.EMPTY,
                    emptyList(),
                    modifiers,
                    kind,
                    name,
                    null,
                    extendings,
                    implementings,
                    body,
                    TypeUtils.asFullyQualified(typeMapping.type(clazz))));
        }

        @Override
        public void visitField(FieldNode field) {
            RewriteGroovyVisitor visitor = new RewriteGroovyVisitor(field);

            List<J.Modifier> modifiers = visitModifiers(field.getModifiers());
            TypeTree typeExpr = visitTypeTree(field.getOriginType());

            J.Identifier name = J.Identifier.build(randomId(), sourceBefore(field.getName()), Markers.EMPTY,
                    field.getName(), typeMapping.type(field.getOriginType()));

            J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    name.getPrefix(),
                    Markers.EMPTY,
                    name.withPrefix(EMPTY),
                    emptyList(),
                    null,
                    name.getType()
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
                    emptyList(),
                    modifiers,
                    typeExpr,
                    null,
                    emptyList(),
                    singletonList(JRightPadded.build(namedVariable))
            );

            queue.add(variableDeclarations);
        }

        @Override
        public void visitMethod(MethodNode method) {
            Space fmt = whitespace();

            List<J.Modifier> modifiers = visitModifiers(method.getModifiers());

            TypeTree returnType = visitTypeTree(method.getReturnType());

            J.Identifier name = J.Identifier.build(randomId(),
                    sourceBefore(method.getName()),
                    Markers.EMPTY,
                    method.getName(),
                    null);

            RewriteGroovyVisitor bodyVisitor = new RewriteGroovyVisitor(method);

            // Parameter has no visit implementation, so we've got to do this by hand
            Space beforeParen = sourceBefore("(");
            List<JRightPadded<Statement>> params = new ArrayList<>(method.getParameters().length);
            Parameter[] unparsedParams = method.getParameters();
            for (int i = 0; i < unparsedParams.length; i++) {
                Parameter param = unparsedParams[i];
                TypeTree paramType = visitTypeTree(param.getOriginType());
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
            if(source.charAt(cursor) == ')') {
                cursor++;
            }

            J.Block body = bodyVisitor.visit(method.getCode());

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
        private Cursor nodeCursor;
        private final Queue<Object> queue = new LinkedList<>();

        public RewriteGroovyVisitor(ASTNode root) {
            this.nodeCursor = new Cursor(null, root);
        }

        private <T> T visit(ASTNode node) {
            nodeCursor = new Cursor(nodeCursor, node);
            node.visit(this);
            nodeCursor = nodeCursor.getParentOrThrow();
            return pollQueue();
        }

        private <T> List<JRightPadded<T>> visitRightPadded(ASTNode[] nodes, char lastPadTo) {
            List<JRightPadded<T>> ts = new ArrayList<>(nodes.length);
            for (ASTNode node : nodes) {
                //noinspection unchecked
                ts.add((JRightPadded<T>) JRightPadded.build(visit(node)).withAfter(whitespace()));
            }
            return ts;
        }

        @Override
        public void visitArgumentlistExpression(ArgumentListExpression expression) {
            List<JRightPadded<Expression>> args = new ArrayList<>(expression.getExpressions().size());
            Space beforeOpenParen = EMPTY;
            List<org.codehaus.groovy.ast.expr.Expression> unparsedArgs = expression.getExpressions();
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
                    pad = sourceBefore(",");
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

            queue.add(JContainer.build(beforeOpenParen, args, Markers.EMPTY));
        }

        @Override
        public void visitBinaryExpression(BinaryExpression binary) {
            Space fmt = whitespace();
            Expression left = visit(binary.getLeftExpression());

            Space opPrefix = whitespace();
            boolean assignment = false;
            J.AssignmentOperation.Type assignOp = null;
            J.Binary.Type binaryOp = null;
            switch (binary.getOperation().getText()) {
                case "+":
                    binaryOp = J.Binary.Type.Addition;
                    break;
                case "&&":
                    binaryOp = J.Binary.Type.And;
                    break;
                case "&=":
                    binaryOp = J.Binary.Type.BitAnd;
                    break;
                case "|=":
                    binaryOp = J.Binary.Type.BitOr;
                    break;
                case "^=":
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
                case "<<=":
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
                case ">>=":
                    binaryOp = J.Binary.Type.RightShift;
                    break;
                case "-":
                    binaryOp = J.Binary.Type.Subtraction;
                    break;
                case ">>>=":
                    binaryOp = J.Binary.Type.UnsignedRightShift;
                    break;
                case "=":
                    assignment = true;
                    break;
                case "-=":
                    assignOp = J.AssignmentOperation.Type.Subtraction;
                    break;
                case "/=":
                    assignOp = J.AssignmentOperation.Type.Division;
                    break;
                case "*=":
                    assignOp = J.AssignmentOperation.Type.Multiplication;
                    break;
                case "%=":
                    assignOp = J.AssignmentOperation.Type.Modulo;
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
                } else if (source.startsWith("\"", cursor)) {
                    text = "\"" + text + "\"";
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
        public void visitConstructorCallExpression(ConstructorCallExpression ctor) {
            Space fmt = sourceBefore("new");
            TypeTree clazz = visitTypeTree(ctor.getType());
            JContainer<Expression> args = visit(ctor.getArguments());
            queue.add(new J.NewClass(randomId(), fmt, Markers.EMPTY, null, EMPTY,
                    clazz, args, null, null, typeMapping.type(ctor.getType())));
        }

        @Override
        public void visitDeclarationExpression(DeclarationExpression expression) {
            TypeTree typeExpr = visitVariableExpressionType(expression.getVariableExpression());

            J.VariableDeclarations.NamedVariable namedVariable = null;
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
                        name.getType()
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
        public void visitIfElse(IfStatement ifElse) {
            Space fmt = sourceBefore("if");
            J.ControlParentheses<Expression> ifCondition = new J.ControlParentheses<Expression>(randomId(), sourceBefore("("), Markers.EMPTY,
                    JRightPadded.build((Expression) visit(ifElse.getBooleanExpression().getExpression())).withAfter(sourceBefore(")")));
            JRightPadded<Statement> then = maybeSemicolon(visit(ifElse.getIfBlock()));
            J.If.Else elze = ifElse.getElseBlock() instanceof EmptyStatement ? null :
                    new J.If.Else(randomId(), sourceBefore("else"), Markers.EMPTY,
                            maybeSemicolon(visit(ifElse.getElseBlock())));
            queue.add(new J.If(randomId(), fmt, Markers.EMPTY, ifCondition, then, elze));
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
        public void visitMethodCallExpression(MethodCallExpression call) {
            JRightPadded<Expression> select = null;
            if (!call.isImplicitThis()) {
                select = JRightPadded.build((Expression) visit(call.getObjectExpression()))
                        .withAfter(sourceBefore(call.isSafe() ? "?." : "."));
            }

            J.Identifier name = J.Identifier.build(randomId(), sourceBefore(call.getMethodAsString()), Markers.EMPTY,
                    call.getMethodAsString(), null);
            if(call.isSafe()) {
                name = name.withMarkers(name.getMarkers().add(new NullSafe(randomId())));
            }

            JContainer<Expression> args = visit(call.getArguments());

            queue.add(new J.MethodInvocation(randomId(), EMPTY, Markers.EMPTY,
                    select,
                    null, name, args, null));
        }

        @Override
        public void visitPropertyExpression(PropertyExpression expression) {
            Space fmt = whitespace();
            Expression target = visit(expression.getObjectExpression());
            Space beforeDot = sourceBefore(".");
            J name = visit(expression.getProperty());
            if (name instanceof J.Literal) {
                String nameStr = ((J.Literal) name).getValueSource();
                assert nameStr != null;
                name = J.Identifier.build(randomId(), name.getPrefix(), Markers.EMPTY, nameStr, null);
            }
            queue.add(new J.FieldAccess(randomId(), fmt, Markers.EMPTY, target, padLeft(beforeDot, (J.Identifier) name), null));
        }

        @Override
        public void visitReturnStatement(ReturnStatement retn) {
            Space fmt = sourceBefore("return");
            queue.add(new J.Return(randomId(), fmt, Markers.EMPTY, visit(retn.getExpression())));
        }

        @Override
        public void visitSynchronizedStatement(SynchronizedStatement statement) {
            Space fmt = sourceBefore("synchronized");
            queue.add(new J.Synchronized(randomId(), fmt, Markers.EMPTY,
                    new J.ControlParentheses<>(randomId(),sourceBefore("("), Markers.EMPTY,
                            JRightPadded.build((Expression) visit(statement.getExpression())).withAfter(sourceBefore(")"))),
                    visit(statement.getCode())));
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
            Space beforeOpenParen = EMPTY;
            List<JRightPadded<Expression>> args = new ArrayList<>(tuple.getExpressions().size());

            List<org.codehaus.groovy.ast.expr.Expression> expressions = tuple.getExpressions();
            for (org.codehaus.groovy.ast.expr.Expression expression : expressions) {
                NamedArgumentListExpression namedArgList = (NamedArgumentListExpression) expression;
                List<MapEntryExpression> mapEntryExpressions = namedArgList.getMapEntryExpressions();
                for (int i = 0; i < mapEntryExpressions.size(); i++) {
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
                    } else if (i == mapEntryExpressions.size() - 1) {
                        int saveCursor = cursor;
                        pad = whitespace();
                        if (cursor >= source.length() || source.charAt(cursor) != ')') {
                            cursor = saveCursor;
                        } else {
                            cursor++;
                        }
                    } else {
                        pad = sourceBefore(",");
                    }

                    Expression arg = visit(mapEntryExpressions.get(i));
                    if (omitParentheses != null) {
                        arg = arg.withMarkers(arg.getMarkers().add(omitParentheses));
                    }

                    args.add(JRightPadded.build(arg.withPrefix(pad)));
                }
            }

            queue.add(JContainer.build(beforeOpenParen, args, Markers.EMPTY));
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
                case "+":
                    operator = J.Unary.Type.Positive;
                    break;
                case "-":
                    operator = J.Unary.Type.Negative;
                    break;
                case "~":
                    operator = J.Unary.Type.Complement;
                    break;
                case "!":
                    operator = J.Unary.Type.Not;
                    break;
            }

            assert operator != null;
            queue.add(new J.Unary(randomId(), fmt, Markers.EMPTY,
                    JLeftPadded.build(operator),
                    visit(unary.getExpression()),
                    null));
        }

        public TypeTree visitVariableExpressionType(VariableExpression expression) {
            JavaType type = typeMapping.type(expression.getOriginType());
            if (expression.isDynamicTyped()) {
                return J.Identifier.build(randomId(),
                        sourceBefore("def"),
                        Markers.EMPTY,
                        "def",
                        type);
            }
            return J.Identifier.build(randomId(),
                    sourceBefore(expression.getOriginType().getUnresolvedName()),
                    Markers.EMPTY,
                    expression.getOriginType().getUnresolvedName(),
                    type);
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            JavaType type = typeMapping.type(expression.getOriginType());
            queue.add(J.Identifier.build(randomId(),
                    sourceBefore(expression.getName()),
                    Markers.EMPTY,
                    expression.getName(),
                    type)
            );
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
            return JRightPadded.build(classVisitor.pollQueue());
        } else if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            RewriteGroovyClassVisitor classVisitor = new RewriteGroovyClassVisitor(unit);
            classVisitor.visitMethod(methodNode);
            return JRightPadded.build(classVisitor.pollQueue());
        }

        RewriteGroovyVisitor groovyVisitor = new RewriteGroovyVisitor(node);
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

        assert expr != null;
        return expr.withPrefix(prefix);
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
            JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(classNode.getUnresolvedName());
            if (primitiveType == null) {
                return buildName(classNode.getUnresolvedName());
            }
            Space fmt = whitespace();
            cursor += classNode.getUnresolvedName().length();
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
}
