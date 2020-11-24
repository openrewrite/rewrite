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

import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class Java11ParserVisitor extends TreePathScanner<J, Formatting> {
    private static final Logger logger = LoggerFactory.getLogger(Java11ParserVisitor.class);

    private final URI uri;
    private final String source;
    private final boolean relaxedClassTypeMatching;
    private final Collection<JavaStyle> styles;

    private EndPosTable endPosTable;
    private int cursor = 0;

    public Java11ParserVisitor(URI uri, String source, boolean relaxedClassTypeMatching, Collection<JavaStyle> styles) {
        this.uri = uri;
        this.source = source;
        this.relaxedClassTypeMatching = relaxedClassTypeMatching;
        this.styles = styles;
    }

    @Override
    public J visitAnnotation(AnnotationTree node, Formatting fmt) {
        skip("@");
        NameTree name = convert(node.getAnnotationType());

        J.Annotation.Arguments args = null;
        if (node.getArguments().size() > 0) {
            var argsPrefix = sourceBefore("(");
            List<Expression> expressions;
            if (node.getArguments().size() == 1) {
                var arg = node.getArguments().get(0);
                if (arg instanceof JCAssign) {
                    if (endPos(arg) < 0) {
                        expressions = singletonList(convert(((JCAssign) arg).rhs, t -> sourceBefore(")")));
                    } else {
                        expressions = singletonList(convert(arg, t -> sourceBefore(")")));
                    }
                } else {
                    expressions = singletonList(convert(arg, t -> sourceBefore(")")));
                }
            } else {
                expressions = convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")"));
            }

            args = new J.Annotation.Arguments(randomId(), expressions, format(argsPrefix));
        } else {
            var remaining = source.substring(cursor, endPos(node));

            // NOTE: technically, if there is code like this, we have a bug, but seems exceedingly unlikely:
            // @MyAnnotation /* Comment () that contains parentheses */ ()

            if (remaining.contains("(") && remaining.contains(")")) {
                var parenPrefix = sourceBefore("(");
                args = new J.Annotation.Arguments(randomId(),
                        singletonList(new J.Empty(randomId(), format(sourceBefore(")")))),
                        format(parenPrefix)
                );
            }
        }

        return new J.Annotation(randomId(), name, args, fmt);
    }

    @Override
    public J visitArrayAccess(ArrayAccessTree node, Formatting fmt) {
        Expression indexed = convert(node.getExpression());

        var dimensionPrefix = sourceBefore("[");
        var dimension = new J.ArrayAccess.Dimension(randomId(), convert(node.getIndex(), t -> sourceBefore("]")),
                format(dimensionPrefix));

        return new J.ArrayAccess(randomId(), indexed, dimension, type(node), fmt);
    }

    @Override
    public J visitArrayType(ArrayTypeTree node, Formatting fmt) {
        var typeIdent = node.getType();
        var dimCount = 1;

        while (typeIdent instanceof ArrayTypeTree) {
            dimCount++;
            typeIdent = ((ArrayTypeTree) typeIdent).getType();
        }

        TypeTree elemType = convert(typeIdent);

        var dimensions = IntStream.range(0, dimCount).mapToObj(n -> {
            var dimPrefix = sourceBefore("[");
            return new J.ArrayType.Dimension(randomId(), new J.Empty(randomId(), format(sourceBefore("]"))), format(dimPrefix));
        }).collect(toList());

        return new J.ArrayType(randomId(), elemType, dimensions, fmt);
    }

    @Override
    public J visitAssert(AssertTree node, Formatting fmt) {
        skip("assert");
        return new J.Assert(randomId(), convert(((JCAssert) node).cond), fmt);
    }

    @Override
    public J visitAssignment(AssignmentTree node, Formatting fmt) {
        Expression variable = convert(node.getVariable(), t -> sourceBefore("="));
        return new J.Assign(randomId(), variable, convert(node.getExpression()), type(node), fmt);
    }

    @Override
    public J visitBinary(BinaryTree node, Formatting fmt) {
        Expression left = convert(node.getLeftOperand());

        var opPrefix = format(whitespace());
        J.Binary.Operator op;
        switch (((JCBinary) node).getTag()) {
            case PLUS:
                skip("+");
                op = new J.Binary.Operator.Addition(randomId(), opPrefix);
                break;
            case MINUS:
                skip("-");
                op = new J.Binary.Operator.Subtraction(randomId(), opPrefix);
                break;
            case DIV:
                skip("/");
                op = new J.Binary.Operator.Division(randomId(), opPrefix);
                break;
            case MUL:
                skip("*");
                op = new J.Binary.Operator.Multiplication(randomId(), opPrefix);
                break;
            case MOD:
                skip("%");
                op = new J.Binary.Operator.Modulo(randomId(), opPrefix);
                break;
            case AND:
                skip("&&");
                op = new J.Binary.Operator.And(randomId(), opPrefix);
                break;
            case OR:
                skip("||");
                op = new J.Binary.Operator.Or(randomId(), opPrefix);
                break;
            case BITAND:
                skip("&");
                op = new J.Binary.Operator.BitAnd(randomId(), opPrefix);
                break;
            case BITOR:
                skip("|");
                op = new J.Binary.Operator.BitOr(randomId(), opPrefix);
                break;
            case BITXOR:
                skip("^");
                op = new J.Binary.Operator.BitXor(randomId(), opPrefix);
                break;
            case SL:
                skip("<<");
                op = new J.Binary.Operator.LeftShift(randomId(), opPrefix);
                break;
            case SR:
                skip(">>");
                op = new J.Binary.Operator.RightShift(randomId(), opPrefix);
                break;
            case USR:
                skip(">>>");
                op = new J.Binary.Operator.UnsignedRightShift(randomId(), opPrefix);
                break;
            case LT:
                skip("<");
                op = new J.Binary.Operator.LessThan(randomId(), opPrefix);
                break;
            case GT:
                skip(">");
                op = new J.Binary.Operator.GreaterThan(randomId(), opPrefix);
                break;
            case LE:
                skip("<=");
                op = new J.Binary.Operator.LessThanOrEqual(randomId(), opPrefix);
                break;
            case GE:
                skip(">=");
                op = new J.Binary.Operator.GreaterThanOrEqual(randomId(), opPrefix);
                break;
            case EQ:
                skip("==");
                op = new J.Binary.Operator.Equal(randomId(), opPrefix);
                break;
            case NE:
                skip("!=");
                op = new J.Binary.Operator.NotEqual(randomId(), opPrefix);
                break;
            default:
                throw new IllegalArgumentException("Unexpected binary tag " + ((JCBinary) node).getTag());
        }

        return new J.Binary(randomId(), left, op, convert(node.getRightOperand()), type(node), fmt);
    }

    @Override
    public J visitBlock(BlockTree node, Formatting fmt) {
        J.Empty stat = null;

        if ((((JCBlock) node).flags & (long) Flags.STATIC) != 0L) {
            skip("static");
            stat = new J.Empty(randomId(), format("", sourceBefore("{")));
        } else {
            skip("{");
        }

        List<Statement> statements = convertPossibleMultiVariable(node.getStatements().stream()
                .filter(s -> {
                    // filter out synthetic super() invocations and the like
                    return endPos(s) > 0;
                })
                .collect(toList()));

        return new J.Block<>(randomId(), stat, statements, fmt, new J.Block.End(randomId(), format(sourceBefore("}"))));
    }

    @Override
    public J visitBreak(BreakTree node, Formatting fmt) {
        skip("break");

        J.Ident label = null;
        Name labelName = node.getLabel();
        if (labelName != null) {
            label = J.Ident.build(randomId(), labelName.toString(), null, format(sourceBefore(labelName.toString())));
            skip(labelName.toString());
        }

        return new J.Break(randomId(), label, fmt);
    }

    @Override
    public J visitCase(CaseTree node, Formatting fmt) {
        Expression pattern = convertOrNull(node.getExpression(), t -> sourceBefore(":"));
        if (pattern == null) {
            pattern = J.Ident.build(randomId(), skip("default"), null, format(sourceBefore(":")));
        }
        return new J.Case(randomId(),
                pattern,
                convertPossibleMultiVariable(node.getStatements()),
                fmt
        );
    }

    @Override
    public J visitCatch(CatchTree node, Formatting fmt) {
        skip("catch");

        var paramPrefix = sourceBefore("(");
        J.VariableDecls paramDecl = convert(node.getParameter(), t -> sourceBefore(")"));
        var param = new J.Parentheses<>(randomId(), paramDecl, format(paramPrefix));

        return new J.Try.Catch(randomId(), param, convert(node.getBlock()), fmt);
    }

    @Override
    public J visitClass(ClassTree node, Formatting fmt) {
        List<J.Annotation> annotations = convertAll(node.getModifiers().getAnnotations(), noDelim, noDelim);
        List<J.Modifier> modifiers = sortedFlags(node.getModifiers());

        J.ClassDecl.Kind kind;
        if (hasFlag(node.getModifiers(), Flags.ENUM)) {
            kind = new J.ClassDecl.Kind.Enum(randomId(), format(sourceBefore("enum")));
        } else if (hasFlag(node.getModifiers(), Flags.ANNOTATION)) {
            // note that annotations ALSO have the INTERFACE flag
            kind = new J.ClassDecl.Kind.Annotation(randomId(), format(sourceBefore("@interface")));
        } else if (hasFlag(node.getModifiers(), Flags.INTERFACE)) {
            kind = new J.ClassDecl.Kind.Interface(randomId(), format(sourceBefore("interface")));
        } else {
            kind = new J.ClassDecl.Kind.Class(randomId(), format(sourceBefore("class")));
        }

        var name = J.Ident.build(randomId(), ((JCClassDecl) node).getSimpleName().toString(), type(node),
                format(sourceBefore(node.getSimpleName().toString())));

        J.TypeParameters typeParams = null;
        if (!node.getTypeParameters().isEmpty()) {
            var genericPrefix = sourceBefore("<");
            typeParams = new J.TypeParameters(randomId(), convertAll(node.getTypeParameters(), commaDelim, t -> sourceBefore(">")),
                    format(genericPrefix));
        }

        J.ClassDecl.Extends extendings = null;
        if (node.getExtendsClause() != null) {
            var extendsPrefix = sourceBefore("extends");
            extendings = new J.ClassDecl.Extends(
                    randomId(),
                    convertOrNull(node.getExtendsClause()),
                    format(extendsPrefix)
            );
        }

        J.ClassDecl.Implements implementings = null;
        if (node.getImplementsClause() != null && !node.getImplementsClause().isEmpty()) {
            var implementsPrefix = sourceBefore(kind instanceof J.ClassDecl.Kind.Interface ?
                    "extends" : "implements");

            implementings = new J.ClassDecl.Implements(
                    randomId(),
                    convertAll(node.getImplementsClause(), commaDelim, noDelim),
                    format(implementsPrefix)
            );
        }

        var bodyPrefix = sourceBefore("{");

        // enum values are required by the grammar to occur before any ordinary field, constructor, or method members
        var jcEnums = node.getMembers().stream()
                .filter(JCVariableDecl.class::isInstance)
                .filter(m -> hasFlag(((JCVariableDecl) m).getModifiers(), Flags.ENUM))
                .collect(toList());

        J.EnumValueSet enumSet = null;
        if (!jcEnums.isEmpty()) {
            AtomicBoolean semicolonPresent = new AtomicBoolean(false);

            List<J.EnumValue> enumValues = convertAll(jcEnums, commaDelim, t -> {
                // this semicolon is required when there are non-value members, but can still
                // be present when there are not
                semicolonPresent.set(positionOfNext(";", '}') > 0);
                return semicolonPresent.get() ? sourceBefore(";", '}') : "";
            });

            enumSet = new J.EnumValueSet(randomId(), enumValues, semicolonPresent.get(), EMPTY);
        }

        List<? extends Tree> membersMultiVariablesSeparated = node.getMembers().stream()
                .filter(m -> {
                    // we don't care about the compiler-inserted default constructor,
                    // since it will never be subject to refactoring
                    if (m instanceof JCMethodDecl) {
                        return !hasFlag(((JCMethodDecl) m).getModifiers(), Flags.GENERATEDCONSTR);
                    }
                    if (m instanceof JCVariableDecl) {
                        return !hasFlag(((JCVariableDecl) m).getModifiers(), Flags.ENUM);
                    }
                    return true;
                })
                .collect(toList());

        var members = Stream.concat(
                Stream.ofNullable((J) enumSet),
                convertPossibleMultiVariable(membersMultiVariablesSeparated).stream()
        ).collect(toList());

        var body = new J.Block<>(randomId(), null, members, format(bodyPrefix), new J.Block.End(randomId(), format(sourceBefore("}"))));

        return new J.ClassDecl(randomId(), annotations, modifiers, kind, name, typeParams, extendings, implementings, body, (JavaType.Class) type(node), fmt);
    }

    @Override
    public J visitCompilationUnit(CompilationUnitTree node, Formatting fmt) {
        logger.debug("Building AST for: " + uri);

        JCCompilationUnit cu = (JCCompilationUnit) node;
        var prefix = source.substring(0, cu.getStartPosition());
        cursor(cu.getStartPosition());

        endPosTable = cu.endPositions;

        // TODO when we want to implement refactoring into javadoc comments as well, refer to this table by JCTree node
//        DocCommentTable docTable = cu.docComments;

        J.Package packageDecl = null;
        if (cu.getPackageName() != null) {
            String packagePrefix = sourceBefore("package");
            packageDecl = new J.Package(randomId(), convert(cu.getPackageName()), format(packagePrefix, sourceBefore(";")));
        }

        return new J.CompilationUnit(randomId(),
                uri.toString(),
                emptyList(),
                packageDecl,
                convertAll(node.getImports(), semiDelim, semiDelim),
                convertAll(node.getTypeDecls().stream()
                                .filter(JCClassDecl.class::isInstance)
                                .collect(toList()),
                        this::whitespace, noDelim),
                format(prefix, source.substring(cursor)),
                styles
        );
    }

    @Override
    public J visitCompoundAssignment(CompoundAssignmentTree node, Formatting fmt) {
        Expression left = convert(((JCAssignOp) node).lhs);

        var opPrefix = format(whitespace());
        J.AssignOp.Operator op;
        switch (((JCAssignOp) node).getTag()) {
            case PLUS_ASG:
                skip("+=");
                op = new J.AssignOp.Operator.Addition(randomId(), opPrefix);
                break;
            case MINUS_ASG:
                skip("-=");
                op = new J.AssignOp.Operator.Subtraction(randomId(), opPrefix);
                break;
            case DIV_ASG:
                skip("/=");
                op = new J.AssignOp.Operator.Division(randomId(), opPrefix);
                break;
            case MUL_ASG:
                skip("*=");
                op = new J.AssignOp.Operator.Multiplication(randomId(), opPrefix);
                break;
            case MOD_ASG:
                skip("%=");
                op = new J.AssignOp.Operator.Modulo(randomId(), opPrefix);
                break;
            case BITAND_ASG:
                skip("&=");
                op = new J.AssignOp.Operator.BitAnd(randomId(), opPrefix);
                break;
            case BITOR_ASG:
                skip("|=");
                op = new J.AssignOp.Operator.BitOr(randomId(), opPrefix);
                break;
            case BITXOR_ASG:
                skip("^=");
                op = new J.AssignOp.Operator.BitXor(randomId(), opPrefix);
                break;
            case SL_ASG:
                skip("<<=");
                op = new J.AssignOp.Operator.LeftShift(randomId(), opPrefix);
                break;
            case SR_ASG:
                skip(">>=");
                op = new J.AssignOp.Operator.RightShift(randomId(), opPrefix);
                break;
            case USR_ASG:
                skip(">>>=");
                op = new J.AssignOp.Operator.UnsignedRightShift(randomId(), opPrefix);
                break;
            default:
                throw new IllegalArgumentException("Unexpected compound assignment tag " + ((JCAssignOp) node).getTag());
        }

        return new J.AssignOp(randomId(),
                left,
                op,
                convert(((JCAssignOp) node).rhs),
                type(node),
                fmt
        );
    }

    @Override
    public J visitConditionalExpression(ConditionalExpressionTree node, Formatting fmt) {
        return new J.Ternary(randomId(),
                convert(node.getCondition(), t -> sourceBefore("?")),
                convert(node.getTrueExpression(), t -> sourceBefore(":")),
                convert(node.getFalseExpression()),
                type(node),
                fmt
        );
    }

    @Override
    public J visitContinue(ContinueTree node, Formatting fmt) {
        skip("continue");
        Name label = node.getLabel();
        return new J.Continue(randomId(),
                label == null ? null : J.Ident.build(randomId(), label.toString(), null, format(sourceBefore(label.toString()))),
                fmt
        );
    }

    @Override
    public J visitDoWhileLoop(DoWhileLoopTree node, Formatting fmt) {
        skip("do");
        Statement stat = convert(node.getStatement());
        var whilePrefix = sourceBefore("while");
        return new J.DoWhileLoop(randomId(),
                stat,
                new J.DoWhileLoop.While(randomId(), convert(node.getCondition()), format(whilePrefix)),
                fmt
        );
    }

    @Override
    public J visitEmptyStatement(EmptyStatementTree node, Formatting fmt) {
        return new J.Empty(randomId(), fmt);
    }

    @Override
    public J visitEnhancedForLoop(EnhancedForLoopTree node, Formatting fmt) {
        skip("for");
        var ctrlPrefix = sourceBefore("(");
        J.VariableDecls variable = convert(node.getVariable(), t -> sourceBefore(":"));
        Expression expression = convert(node.getExpression(), t -> sourceBefore(")"));

        return new J.ForEachLoop(randomId(),
                new J.ForEachLoop.Control(randomId(), variable, expression, format(ctrlPrefix)),
                convert(node.getStatement(), statementDelim),
                fmt
        );
    }

    private J visitEnumVariable(VariableTree node, Formatting fmt) {
        skip(node.getName().toString());
        var name = J.Ident.build(randomId(), node.getName().toString(), type(node), EMPTY);

        J.NewClass initializer = null;
        if (source.charAt(endPos(node) - 1) == ')' || source.charAt(endPos(node) - 1) == '}') {
            initializer = convert(node.getInitializer());
        }

        return new J.EnumValue(randomId(), name, initializer, fmt);
    }

    @Override
    public J visitForLoop(ForLoopTree node, Formatting fmt) {
        skip("for");
        var ctrlPrefix = sourceBefore("(");

        Statement init = convertPossibleMultiVariable(node.getInitializer())
                .stream()
                .filter(Statement.class::isInstance)
                .map(Statement.class::cast)
                .findAny()
                .orElseGet(() -> new J.Empty(randomId(), format("", sourceBefore(";"))));

        Expression condition = convertOrNull(node.getCondition(), semiDelim);
        if (condition == null) {
            condition = new J.Empty(randomId(), format("", sourceBefore(";")));
        }

        List<Statement> update;
        if (node.getUpdate().isEmpty()) {
            update = singletonList(new J.Empty(randomId(), format("", sourceBefore(")"))));
        } else {
            update = new ArrayList<>();
            List<? extends ExpressionStatementTree> nodeUpdate = node.getUpdate();
            for (int i = 0; i < nodeUpdate.size(); i++) {
                ExpressionStatementTree tree = nodeUpdate.get(i);
                update.add(convert(tree, i == nodeUpdate.size() - 1 ? t -> sourceBefore(")") : commaDelim));
            }
        }

        return new J.ForLoop(randomId(),
                new J.ForLoop.Control(randomId(), init, condition, update, format(ctrlPrefix)),
                convert(node.getStatement(), statementDelim),
                fmt
        );
    }

    @Override
    public J visitIdentifier(IdentifierTree node, Formatting fmt) {
        cursor += node.getName().toString().length();
        return J.Ident.build(randomId(), node.getName().toString(), type(node), fmt);
    }

    @Override
    public J visitIf(IfTree node, Formatting fmt) {
        skip("if");

        J.Parentheses<Expression> ifPart = convert(node.getCondition());
        Statement then = convert(node.getThenStatement());

        J.If.Else elsePart = null;
        if (node.getElseStatement() instanceof JCTree.JCStatement) {
            var elsePrefix = sourceBefore("else");
            elsePart = new J.If.Else(randomId(), convert(node.getElseStatement(), statementDelim), format(elsePrefix));
        }

        return new J.If(randomId(), ifPart, then, elsePart, fmt);
    }

    @Override
    public J visitImport(ImportTree node, Formatting fmt) {
        skip("import");
        skipPattern("\\s+static");
        return new J.Import(randomId(), convert(node.getQualifiedIdentifier()), node.isStatic(), fmt);
    }

    @Override
    public J visitInstanceOf(InstanceOfTree node, Formatting fmt) {
        return new J.InstanceOf(randomId(),
                convert(node.getExpression(), t -> sourceBefore("instanceof")),
                convert(node.getType()),
                type(node),
                fmt
        );
    }

    @Override
    public J visitLabeledStatement(LabeledStatementTree node, Formatting fmt) {
        skip(node.getLabel().toString());
        return new J.Label(randomId(),
                J.Ident.build(randomId(), node.getLabel().toString(), null, format("", sourceBefore(":"))),
                convert(node.getStatement()),
                fmt
        );
    }

    @Override
    public J visitLambdaExpression(LambdaExpressionTree node, Formatting fmt) {
        var parenthesized = source.charAt(cursor) == '(';
        skip("(");

        List<Expression> paramList;
        if (parenthesized && node.getParameters().isEmpty()) {
            paramList = singletonList(new J.Empty(randomId(), format(sourceBefore(")"))));
        } else {
            paramList = convertAll(node.getParameters(), commaDelim,
                    t -> parenthesized ? sourceBefore(")") : "");
        }

        var params = new J.Lambda.Parameters(randomId(), parenthesized, paramList);
        var arrow = new J.Lambda.Arrow(randomId(), format(sourceBefore("->")));

        J body;
        if (node.getBody() instanceof JCTree.JCBlock) {
            var prefix = sourceBefore("{");
            cursor--;
            body = convert(node.getBody());
            body = body.withPrefix(prefix);
        } else {
            body = convert(node.getBody());
        }

        return new J.Lambda(randomId(),
                params,
                arrow,
                body,
                type(node),
                fmt
        );
    }

    @Override
    public J visitLiteral(LiteralTree node, Formatting fmt) {
        cursor(endPos(node));
        var value = node.getValue();
        var type = primitive(((JCTree.JCLiteral) node).typetag);
        return new J.Literal(randomId(),
                value,
                source.substring(((JCLiteral) node).getStartPosition(), endPos(node)),
                type,
                fmt
        );
    }

    @Override
    public J visitMemberReference(MemberReferenceTree node, Formatting fmt) {
        JCMemberReference ref = (JCMemberReference) node;
        Expression expr = convert(ref.expr, t -> sourceBefore("::"));

        String referenceName;
        switch (ref.getMode()) {
            case NEW:
                referenceName = "new";
                break;
            case INVOKE:
            default:
                referenceName = node.getName().toString();
                break;
        }

        var typeParams = convertTypeParameters(node.getTypeArguments());
        var reference = J.Ident.build(randomId(), referenceName, null, format(sourceBefore(referenceName)));

        return new J.MemberReference(randomId(), expr, typeParams, reference, type(node), fmt);
    }

    @Override
    public J visitMemberSelect(MemberSelectTree node, Formatting fmt) {
        JCFieldAccess fieldAccess = (JCFieldAccess) node;
        Expression target = convert(fieldAccess.selected, t -> sourceBefore("."));
        var name = J.Ident.build(randomId(), fieldAccess.name.toString(), null, format(sourceBefore(fieldAccess.name.toString())));
        return new J.FieldAccess(randomId(), target, name, type(node), fmt);
    }

    @Override
    public J visitMethodInvocation(MethodInvocationTree node, Formatting fmt) {
        var jcSelect = ((JCTree.JCMethodInvocation) node).getMethodSelect();

        Expression select = null;
        if (jcSelect instanceof JCFieldAccess) {
            select = convert(((JCFieldAccess) jcSelect).selected, t -> sourceBefore("."));
        } else if (!(jcSelect instanceof JCIdent)) {
            throw new IllegalStateException("Unexpected method select type " + jcSelect.getClass().getSimpleName());
        }

        // generic type parameters can only exist on qualified targets
        J.MethodInvocation.TypeParameters typeParams = null;
        if (!node.getTypeArguments().isEmpty()) {
            var genericPrefix = sourceBefore("<");
            List<Expression> genericParams = convertAll(node.getTypeArguments(), commaDelim, t -> sourceBefore(">"));
            typeParams = new J.TypeParameters(randomId(), genericParams.stream()
                    .map(gp -> new J.TypeParameter(randomId(), emptyList(), gp.withFormatting(EMPTY), null, gp.getFormatting()))
                    .collect(toList()),
                    format(genericPrefix));
        }

        J.Ident name;
        if (jcSelect instanceof JCFieldAccess) {
            String selectName = ((JCFieldAccess) jcSelect).name.toString();
            name = J.Ident.build(randomId(), selectName, null, format(sourceBefore(selectName)));
        } else {
            name = convert(jcSelect);
        }

        var argsPrefix = sourceBefore("(");
        var args = new J.MethodInvocation.Arguments(randomId(),
                node.getArguments().isEmpty() ?
                        singletonList(new J.Empty(randomId(), format(sourceBefore(")")))) :
                        convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")),
                format(argsPrefix)
        );

        var genericSymbolAny = (jcSelect instanceof JCFieldAccess) ? ((JCFieldAccess) jcSelect).sym : ((JCIdent) jcSelect).sym;

        // if the symbol is not a method symbol, there is a parser error in play
        Symbol.MethodSymbol genericSymbol = genericSymbolAny instanceof Symbol.MethodSymbol ? (Symbol.MethodSymbol) genericSymbolAny : null;

        JavaType.Method type = null;
        if (genericSymbol != null && jcSelect.type != null) {
            Function<com.sun.tools.javac.code.Type, JavaType.Method.Signature> signature = t -> {
                if (t instanceof com.sun.tools.javac.code.Type.MethodType) {
                    com.sun.tools.javac.code.Type.MethodType mt = (com.sun.tools.javac.code.Type.MethodType) t;
                    return new JavaType.Method.Signature(type(mt.restype), mt.argtypes.stream().filter(Objects::nonNull)
                            .map(this::type).collect(toList()));
                }
                return null;
            };

            JavaType.Method.Signature genericSignature;
            if (genericSymbol.type instanceof com.sun.tools.javac.code.Type.ForAll) {
                genericSignature = signature.apply(((com.sun.tools.javac.code.Type.ForAll) genericSymbol.type).qtype);
            } else {
                genericSignature = signature.apply(genericSymbol.type);
            }

            type = JavaType.Method.build(
                    TypeUtils.asClass(type(genericSymbol.owner)),
                    name.getSimpleName(),
                    genericSignature,
                    signature.apply(jcSelect.type),
                    genericSymbol.params().stream().map(p -> p.name.toString()).collect(toList()),
                    filteredFlags(genericSymbol)
            );
        }

        return new J.MethodInvocation(randomId(), select, typeParams, name, args, type, fmt);
    }

    @Override
    public J visitMethod(MethodTree node, Formatting fmt) {
        logger.trace("Visiting method {}", node.getName());

        List<J.Annotation> annotations = convertAll(node.getModifiers().getAnnotations(), noDelim, noDelim);
        List<J.Modifier> modifiers = sortedFlags(node.getModifiers());

        // see https://docs.oracle.com/javase/tutorial/java/generics/methods.html
        J.TypeParameters typeParams = null;
        if (!node.getTypeParameters().isEmpty()) {
            var genericPrefix = sourceBefore("<");
            typeParams = new J.TypeParameters(randomId(), convertAll(node.getTypeParameters(), commaDelim, t -> sourceBefore(">")),
                    format(genericPrefix));
        }

        TypeTree returnType = convertOrNull(node.getReturnType());

        J.Ident name;
        if ("<init>".equals(node.getName().toString())) {
            var nodeSym = ((JCMethodDecl) node).sym;
            var owner = nodeSym == null ?
                    stream(getCurrentPath().spliterator(), false)
                            .filter(JCClassDecl.class::isInstance)
                            .map(JCClassDecl.class::cast)
                            .findFirst()
                            .map(cd -> cd.getSimpleName().toString())
                            .orElseThrow() :
                    ((JCMethodDecl) node).sym.owner.name.toString();
            name = J.Ident.build(randomId(), owner, null, format(sourceBefore(owner)));
        } else {
            name = J.Ident.build(randomId(), node.getName().toString(), null, format(sourceBefore(node.getName().toString())));
        }

        var paramFmt = format(sourceBefore("("));
        var params = !node.getParameters().isEmpty() ?
                new J.MethodDecl.Parameters(randomId(), convertAll(node.getParameters(), commaDelim, t -> sourceBefore(")")), paramFmt) :
                new J.MethodDecl.Parameters(randomId(), singletonList(new J.Empty(randomId(), format(sourceBefore(")")))), paramFmt);

        J.MethodDecl.Throws throwss = null;
        if (!node.getThrows().isEmpty()) {
            var throwsPrefix = sourceBefore("throws");
            throwss = new J.MethodDecl.Throws(randomId(), convertAll(node.getThrows(), commaDelim, noDelim), format(throwsPrefix));
        }

        J.Block<Statement> body = convertOrNull(node.getBody());

        J.MethodDecl.Default defaultValue = null;
        if (node.getDefaultValue() != null) {
            var defaultPrefix = sourceBefore("default");
            defaultValue = new J.MethodDecl.Default(randomId(), convert(node.getDefaultValue()), format(defaultPrefix));
        }

        return new J.MethodDecl(randomId(), annotations, modifiers, typeParams, returnType, name, params, throwss, body, defaultValue, fmt);
    }

    @Override
    public J visitNewArray(NewArrayTree node, Formatting fmt) {
        skip("new");

        var jcVarType = ((JCNewArray) node).elemtype;
        TypeTree typeExpr;
        if (jcVarType instanceof JCArrayTypeTree) {
            // we'll capture the array dimensions in a bit, just convert the element type
            var elementType = ((JCArrayTypeTree) jcVarType).elemtype;
            while (elementType instanceof JCArrayTypeTree) {
                elementType = ((JCArrayTypeTree) elementType).elemtype;
            }
            typeExpr = convertOrNull(elementType);
        } else {
            typeExpr = convertOrNull(jcVarType);
        }

        List<J.NewArray.Dimension> dimensions = new ArrayList<>();
        List<? extends ExpressionTree> nodeDimensions = node.getDimensions();
        for (int i = 0; i < nodeDimensions.size(); i++) {
            ExpressionTree dim = nodeDimensions.get(i);
            var dimensionPrefix = sourceBefore("[");
            dimensions.add(new J.NewArray.Dimension(randomId(), convert(dim, t -> sourceBefore("]")),
                    format(dimensionPrefix, (i == node.getDimensions().size() - 1 && node.getInitializers() != null) ? sourceBefore("}") : "")));
        }

        var matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)]").matcher(source);
        while (matcher.find(cursor)) {
            cursor(matcher.end());
            var ws = new J.Empty(randomId(), format(matcher.group(2)));
            dimensions.add(new J.NewArray.Dimension(randomId(), ws, format(matcher.group(1))));
        }

        J.NewArray.Initializer initializer = null;
        if (node.getInitializers() != null) {
            var initPrefix = sourceBefore("{");
            List<Expression> initializers = node.getInitializers().isEmpty() ?
                    singletonList(new J.Empty(randomId(), format("", sourceBefore("}")))) :
                    convertAll(node.getInitializers(), commaDelim, t -> sourceBefore("}"));
            initializer = new J.NewArray.Initializer(randomId(), initializers, format(initPrefix));
        }

        return new J.NewArray(randomId(), typeExpr, dimensions, initializer, type(node), fmt);
    }

    @Override
    public J visitNewClass(NewClassTree node, Formatting fmt) {
        Expression encl = node.getEnclosingExpression() == null ? null : convert(node.getEnclosingExpression());

        if (encl != null) {
            encl = encl.withSuffix(sourceBefore("."));
        }
        String whitespaceBeforeNew = sourceBefore("new");
        skip("new");

        // for enum definitions with anonymous class initializers, endPos of node identifier will be -1
        TypeTree clazz = endPos(node.getIdentifier()) >= 0 ? convertOrNull(node.getIdentifier()) : null;

        J.NewClass.Arguments args = null;
        if (positionOfNext("(", '{') > -1) {
            var argPrefix = sourceBefore("(");
            args = new J.NewClass.Arguments(randomId(),
                    node.getArguments().isEmpty() ?
                            singletonList(new J.Empty(randomId(), format(sourceBefore(")")))) :
                            convertAll(node.getArguments(), commaDelim, t -> sourceBefore(")")),
                    format(argPrefix));
        }

        J.Block<?> body = null;
        if (node.getClassBody() != null) {
            var bodyPrefix = sourceBefore("{");

            var members = convertAll(node.getClassBody().getMembers().stream()
                    // we don't care about the compiler-inserted default constructor,
                    // since it will never be subject to refactoring
                    .filter(m -> !(m instanceof JCMethodDecl) || (((JCMethodDecl) m).getModifiers().flags & Flags.GENERATEDCONSTR) == 0L)
                    .collect(toList()), noDelim, noDelim);

            body = new J.Block<>(randomId(), null, members, format(bodyPrefix), new J.Block.End(randomId(), format(sourceBefore("}"))));
        }

        return new J.NewClass(
                randomId(),
                encl,
                new J.NewClass.New(UUID.randomUUID(), format(whitespaceBeforeNew)),
                clazz,
                args,
                body,
                type(((JCNewClass) node).type),
                fmt);
    }

    @Override
    public J visitParameterizedType(ParameterizedTypeTree node, Formatting fmt) {
        return new J.ParameterizedType(randomId(), convert(node.getType()), convertTypeParameters(node.getTypeArguments()), fmt);
    }

    @Override
    public J visitParenthesized(ParenthesizedTree node, Formatting fmt) {
        skip("(");
        return new J.Parentheses<Expression>(randomId(), convert(node.getExpression(), t -> sourceBefore(")")), fmt);
    }

    @Override
    public J visitPrimitiveType(PrimitiveTypeTree node, Formatting fmt) {
        cursor(endPos(node));

        JavaType.Primitive primitiveType;
        switch (node.getPrimitiveTypeKind()) {
            case BOOLEAN:
                primitiveType = JavaType.Primitive.Boolean;
                break;
            case BYTE:
                primitiveType = JavaType.Primitive.Byte;
                break;
            case CHAR:
                primitiveType = JavaType.Primitive.Char;
                break;
            case DOUBLE:
                primitiveType = JavaType.Primitive.Double;
                break;
            case FLOAT:
                primitiveType = JavaType.Primitive.Float;
                break;
            case INT:
                primitiveType = JavaType.Primitive.Int;
                break;
            case LONG:
                primitiveType = JavaType.Primitive.Long;
                break;
            case SHORT:
                primitiveType = JavaType.Primitive.Short;
                break;
            case VOID:
                primitiveType = JavaType.Primitive.Void;
                break;
            default:
                throw new IllegalArgumentException("Unknown primitive type " + node.getPrimitiveTypeKind());
        }

        return new J.Primitive(randomId(), primitiveType, fmt);
    }

    @Override
    public J visitReturn(ReturnTree node, Formatting fmt) {
        skip("return");
        return new J.Return(randomId(), convertOrNull(node.getExpression()), fmt);
    }

    @Override
    public J visitSwitch(SwitchTree node, Formatting fmt) {
        skip("switch");
        J.Parentheses<Expression> selector = convert(node.getExpression());

        var casePrefix = sourceBefore("{");
        List<J.Case> cases = convertAll(node.getCases(), noDelim, noDelim);

        return new J.Switch(randomId(), selector, new J.Block<>(randomId(), null, cases, format(casePrefix),
                new J.Block.End(randomId(), format(sourceBefore("}")))), fmt);
    }

    @Override
    public J visitSynchronized(SynchronizedTree node, Formatting fmt) {
        skip("synchronized");
        return new J.Synchronized(randomId(),
                convert(node.getExpression()),
                convert(node.getBlock()),
                fmt
        );
    }

    @Override
    public J visitThrow(ThrowTree node, Formatting fmt) {
        skip("throw");
        return new J.Throw(randomId(), convert(node.getExpression()), fmt);
    }

    @Override
    public J visitTry(TryTree node, Formatting fmt) {
        skip("try");
        J.Try.Resources resources = null;
        if (!node.getResources().isEmpty()) {
            var resourcesPrefix = sourceBefore("(");
            List<J.VariableDecls> decls = convertAll(node.getResources(), semiDelim, t -> sourceBefore(")"));
            resources = new J.Try.Resources(randomId(), decls, format(resourcesPrefix));
        }

        J.Block<Statement> block = convert(node.getBlock());
        List<J.Try.Catch> catches = convertAll(node.getCatches(), noDelim, noDelim);

        J.Try.Finally finallyy = null;
        if (node.getFinallyBlock() != null) {
            var finallyPrefix = sourceBefore("finally");
            finallyy = new J.Try.Finally(randomId(), convert(node.getFinallyBlock()),
                    format(finallyPrefix));
        }

        return new J.Try(randomId(), resources, block, catches, finallyy, fmt);
    }

    @Override
    public J visitTypeCast(TypeCastTree node, Formatting fmt) {
        var clazzPrefix = sourceBefore("(");
        var clazz = new J.Parentheses<TypeTree>(randomId(), convert(node.getType(), t -> sourceBefore(")")),
                format(clazzPrefix));

        return new J.TypeCast(randomId(), clazz, convert(node.getExpression()), fmt);
    }

    @Override
    public J visitAnnotatedType(AnnotatedTypeTree node, Formatting formatting) {
        List<J.Annotation> annotations = convertAll(node.getAnnotations(), noDelim, noDelim);
        return new J.AnnotatedType(randomId(), annotations, convert(node.getUnderlyingType()), formatting);
    }

    @Override
    public J visitTypeParameter(TypeParameterTree node, Formatting fmt) {
        List<J.Annotation> annotations = convertAll(node.getAnnotations(), noDelim, noDelim);

        var name = TreeBuilder.buildName(node.getName().toString(), format(sourceBefore(node.getName().toString())));

        J.TypeParameter.Bounds bounds = null;
        if (!node.getBounds().isEmpty()) {
            var boundPrefix = !node.getBounds().isEmpty() ? sourceBefore("extends") : "";
            // see https://docs.oracle.com/javase/tutorial/java/generics/bounded.html
            bounds = new J.TypeParameter.Bounds(randomId(), convertAll(node.getBounds(), t -> sourceBefore("&"), noDelim),
                    format(boundPrefix));
        }

        return new J.TypeParameter(randomId(), annotations, name, bounds, fmt);
    }

    @Override
    public J visitUnionType(UnionTypeTree node, Formatting fmt) {
        return new J.MultiCatch(randomId(), convertAll(node.getTypeAlternatives(), t -> sourceBefore("|"), noDelim), fmt);
    }

    @Override
    public J visitUnary(UnaryTree node, Formatting fmt) {
        JCUnary unary = (JCUnary) node;
        var tag = unary.getTag();
        J.Unary.Operator op;
        Expression expr;

        switch (tag) {
            case POS:
                skip("+");
                op = new J.Unary.Operator.Positive(randomId());
                expr = convert(unary.arg);
                break;
            case NEG:
                skip("-");
                op = new J.Unary.Operator.Negative(randomId());
                expr = convert(unary.arg);
                break;
            case PREDEC:
                skip("--");
                op = new J.Unary.Operator.PreDecrement(randomId());
                expr = convert(unary.arg);
                break;
            case PREINC:
                skip("++");
                op = new J.Unary.Operator.PreIncrement(randomId());
                expr = convert(unary.arg);
                break;
            case POSTDEC:
                expr = convert(unary.arg);
                op = new J.Unary.Operator.PostDecrement(randomId(), format(sourceBefore("--")));
                break;
            case POSTINC:
                expr = convert(unary.arg);
                op = new J.Unary.Operator.PostIncrement(randomId(), format(sourceBefore("++")));
                break;
            case COMPL:
                skip("~");
                op = new J.Unary.Operator.Complement(randomId(), EMPTY);
                expr = convert(unary.arg);
                break;
            case NOT:
                skip("!");
                op = new J.Unary.Operator.Not(randomId(), EMPTY);
                expr = convert(unary.arg);
                break;
            default:
                throw new IllegalArgumentException("Unexpected unary tag " + tag);
        }

        return new J.Unary(randomId(), op, expr, type(node), fmt);
    }

    @Override
    public J visitVariable(VariableTree node, Formatting fmt) {
        return hasFlag(node.getModifiers(), Flags.ENUM) ?
                visitEnumVariable(node, fmt) :
                visitVariables(singletonList(node), fmt); // method arguments cannot be multi-declarations
    }

    private J.VariableDecls visitVariables(List<VariableTree> nodes, Formatting fmt) {
        JCTree.JCVariableDecl node = (JCVariableDecl) nodes.get(0);
        List<J.Annotation> annotations = convertAll(node.getModifiers().annotations, noDelim, noDelim);

        var vartype = node.vartype;

        List<J.Modifier> modifiers;
        if (node.getModifiers().pos >= 0) {
            modifiers = sortedFlags(node.getModifiers());
        } else {
            modifiers = emptyList(); // these are implicit modifiers, like "final" on try-with-resources variable declarations
        }

        TypeTree typeExpr;
        if (vartype == null || endPos(vartype) < 0 || vartype instanceof JCErroneous) {
            typeExpr = null; // this is a lambda parameter with an inferred type expression
        } else if (vartype instanceof JCArrayTypeTree) {
            // we'll capture the array dimensions in a bit, just convert the element type
            var elementType = ((JCArrayTypeTree) vartype).elemtype;
            while (elementType instanceof JCArrayTypeTree) {
                elementType = ((JCArrayTypeTree) elementType).elemtype;
            }
            typeExpr = convert(elementType);
        } else {
            typeExpr = convert(vartype);
        }

        Supplier<List<J.VariableDecls.Dimension>> dimensions = () -> {
            var matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)]").matcher(source);
            List<J.VariableDecls.Dimension> dims = new ArrayList<>();
            while (matcher.find(cursor)) {
                cursor(matcher.end());
                var ws = new J.Empty(randomId(), format(matcher.group(2)));
                dims.add(new J.VariableDecls.Dimension(randomId(), ws, format(matcher.group(1))));
            }
            return dims;
        };

        var beforeDimensions = dimensions.get();

        var vartypeString = typeExpr == null ? "" : source.substring(vartype.getStartPosition(), endPos(vartype));
        var varargMatcher = Pattern.compile("(\\s*)\\.{3}").matcher(vartypeString);
        J.VariableDecls.Varargs varargs = null;
        if (varargMatcher.find()) {
            skipPattern("(\\s*)\\.{3}");
            varargs = new J.VariableDecls.Varargs(randomId(), format(varargMatcher.group(1)));
        }

        List<J.VariableDecls.NamedVar> vars = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            VariableTree n = nodes.get(i);

            var namedVarPrefix = sourceBefore(n.getName().toString());
            JCVariableDecl vd = (JCVariableDecl) n;

            var dimensionsAfterName = dimensions.get();
            if (!dimensionsAfterName.isEmpty()) {
                dimensionsAfterName = Formatting.formatLastSuffix(dimensionsAfterName, vd.init != null ? sourceBefore("=") : "");
            }

            var name = J.Ident.build(randomId(), n.getName().toString(), type(node),
                    format("", (dimensionsAfterName.isEmpty() && vd.init != null) ? sourceBefore("=") : ""));

            vars.add(
                    new J.VariableDecls.NamedVar(randomId(),
                            name,
                            dimensionsAfterName,
                            convertOrNull(vd.init),
                            type(n),
                            i == nodes.size() - 1 ? format(namedVarPrefix) : format(namedVarPrefix, sourceBefore(","))
                    )
            );
        }

        return new J.VariableDecls(randomId(), annotations, modifiers, typeExpr, varargs, beforeDimensions, vars, fmt);
    }

    @Override
    public J visitWhileLoop(WhileLoopTree node, Formatting fmt) {
        skip("while");
        return new J.WhileLoop(randomId(),
                convert(node.getCondition()),
                convert(node.getStatement(), statementDelim),
                fmt
        );
    }

    @Override
    public J visitWildcard(WildcardTree node, Formatting fmt) {
        skip("?");

        JCWildcard wildcard = (JCWildcard) node;

        J.Wildcard.Bound bound;
        switch (wildcard.kind.kind) {
            case EXTENDS:
                bound = new J.Wildcard.Bound.Extends(randomId(), format(sourceBefore("extends")));
                break;
            case SUPER:
                bound = new J.Wildcard.Bound.Super(randomId(), format(sourceBefore("super")));
                break;
            case UNBOUND:
            default:
                bound = null;
        }

        return new J.Wildcard(randomId(), bound, convertOrNull(wildcard.inner), fmt);
    }

    /**
     * --------------
     * Conversion utilities
     * --------------
     */

    private <T extends J> T convert(Tree t) {
        return convert(t, t2 -> "");
    }

    private <T extends J> T convert(Tree t2, Function<Tree, String> suffix) {
        try {
            var prefix = source.substring(cursor, max(((JCTree) t2).getStartPosition(), cursor));
            cursor += prefix.length();
            @SuppressWarnings("unchecked") T t = (T) scan(t2, format(prefix));
            if (t != null) {
                t = t.withSuffix(suffix.apply(t2));
            }
            cursor(max(endPos(t2), cursor)); // if there is a non-empty suffix, the cursor may have already moved past it
            return t;
        } catch (Throwable ex) {
            // this SHOULD never happen, but is here simply as a diagnostic measure in the event of unexpected exceptions
            logger.error("Failed to convert " + t2.getClass().getSimpleName() + " for the following cursor stack:");
            logCurrentPathAsError();
            throw ex;
        }
    }

    private void logCurrentPathAsError() {
        logger.error("--- BEGIN PATH ---");

        var paths = stream(getCurrentPath().spliterator(), false).collect(toList());
        for (int i = paths.size(); i-- > 0; ) {
            JCTree tree = (JCTree) paths.get(i);
            if (tree instanceof JCCompilationUnit) {
                logger.error("JCCompilationUnit(sourceFile = " + ((JCCompilationUnit) tree).sourcefile.getName() + ")");
            } else if (tree instanceof JCClassDecl) {
                logger.error("JCClassDecl(name = " + ((JCClassDecl) tree).name + ", line = " + lineNumber(tree) + ")");
            } else if (tree instanceof JCVariableDecl) {
                logger.error("JCVariableDecl(name = " + ((JCVariableDecl) tree).name + ", line = " + lineNumber(tree) + ")");
            } else {
                logger.error(tree.getClass().getSimpleName() + "(line = " + lineNumber(tree) + ")");
            }
        }

        logger.error("--- END PATH ---");
    }

    private long lineNumber(Tree tree) {
        return source.substring(0, ((JCTree) tree).getStartPosition()).chars().filter(c -> c == '\n').count() + 1;
    }

    private <T extends J> T convertOrNull(@Nullable Tree t) {
        return convertOrNull(t, t2 -> "");
    }

    @Nullable
    private <T extends J> T convertOrNull(@Nullable Tree t, Function<Tree, String> suffix) {
        return t == null ? null : convert(t, suffix);
    }

    private <T extends J> List<T> convertAll(List<? extends Tree> trees, Function<Tree, String> innerSuffix, Function<Tree, String> suffix) {
        List<T> converted = new ArrayList<>(trees.size());
        for (int i = 0; i < trees.size(); i++) {
            converted.add(convert(trees.get(i), i == trees.size() - 1 ? suffix : innerSuffix));
        }
        return converted;
    }

    private J.TypeParameters convertTypeParameters(List<? extends Tree> typeArguments) {
        if (typeArguments == null) {
            return null;
        }

        var typeArgPrefix = sourceBefore("<");
        List<Expression> typeArgs;
        if (typeArguments.isEmpty()) {
            // raw type, see http://docs.oracle.com/javase/tutorial/java/generics/rawTypes.html
            // adding space before > as a suffix to be consistent with space before > for non-empty lists of type args
            typeArgs = singletonList(new J.Empty(randomId(), format("", sourceBefore(">"))));
        } else {
            typeArgs = convertAll(typeArguments, commaDelim, t -> sourceBefore(">"));
        }

        // pull formatting up to TypeParameter rather than Expression, to match what happens in type parameter conversions
        // elsewhere in the tree
        return new J.TypeParameters(randomId(), typeArgs.stream()
                .map(gp -> new J.TypeParameter(randomId(), emptyList(), gp.withFormatting(EMPTY), null, gp.getFormatting()))
                .collect(toList()),
                format(typeArgPrefix));
    }

    private final Function<Tree, String> statementDelim = (@Nullable Tree t) -> {
        if (t instanceof JCThrow ||
                t instanceof JCBreak ||
                t instanceof JCAssert ||
                t instanceof JCContinue ||
                t instanceof JCExpressionStatement ||
                t instanceof JCReturn ||
                t instanceof JCVariableDecl ||
                t instanceof JCDoWhileLoop ||
                t instanceof JCSkip) {
            return sourceBefore(";");
        }
        if (t instanceof JCCase) {
            return sourceBefore(":");
        }
        if (t instanceof JCMethodDecl) {
            return sourceBefore(((JCMethodDecl) t).body == null ? ";" : "");
        }
        return sourceBefore("");
    };

    @SuppressWarnings("unchecked")
    private <T extends J> List<T> convertPossibleMultiVariable(@Nullable List<? extends Tree> trees) {
        if (trees == null)
            return emptyList();

        return trees.stream()
                .collect(Collectors.groupingBy(t -> ((JCTree) t).getStartPosition(), LinkedHashMap::new, toList()))
                .values()
                .stream()
                .map(treeGroup -> {
                    if (treeGroup.size() == 1) {
                        return (T) convert(treeGroup.get(0), statementDelim);
                    } else {
                        // multi-variable declarations are split into independent overlapping JCVariableDecl's by the OpenJDK AST
                        var prefix = source.substring(cursor, max(((JCTree) treeGroup.get(0)).getStartPosition(), cursor));
                        cursor += prefix.length();

                        var last = treeGroup.get(treeGroup.size() - 1);

                        @SuppressWarnings("unchecked")
                        J.VariableDecls vars = visitVariables((List<VariableTree>) treeGroup, format(prefix));
                        vars = vars.withSuffix(semiDelim.apply(last));
                        cursor(max(endPos(last), cursor));
                        return (T) vars;
                    }
                })
                .collect(toList());
    }

    /**
     * --------------
     * Type conversion
     * --------------
     */

    private final Map<Long, Flag> flagMasks = Map.of(
            1L, Flag.Public,
            1L << 1, Flag.Private,
            1L << 2, Flag.Protected,
            1L << 3, Flag.Static,
            1L << 4, Flag.Final,
            1L << 5, Flag.Synchronized,
            1L << 6, Flag.Volatile,
            1L << 7, Flag.Transient,
            1L << 10, Flag.Abstract
    );

    private Set<Flag> filteredFlags(Symbol sym) {
        return flagMasks.entrySet().stream()
                .filter(mask -> (sym.flags() & mask.getKey()) != 0L)
                .map(Map.Entry::getValue)
                .collect(toSet());
    }

    @Nullable
    private JavaType type(@Nullable Symbol symbol) {
        if (symbol instanceof Symbol.ClassSymbol || symbol instanceof Symbol.TypeVariableSymbol) {
            return type(symbol.type);
        } else if (symbol instanceof Symbol.VarSymbol) {
            return new JavaType.GenericTypeVariable(symbol.name.toString(), null);
        }
        return null;
    }

    @Nullable
    private JavaType type(@Nullable com.sun.tools.javac.code.Type type) {
        return type(type, emptyList());
    }

    @Nullable
    private JavaType type(@Nullable com.sun.tools.javac.code.Type type, List<Symbol> stack) {
        return type(type, stack, false);
    }

    @Nullable
    private JavaType type(@Nullable com.sun.tools.javac.code.Type type,
                          List<Symbol> stack, boolean shallow) {
        if (type instanceof com.sun.tools.javac.code.Type.ClassType) {
            if (type instanceof Type.ErrorType) {
                return null;
            }

            var sym = (Symbol.ClassSymbol) type.tsym;

            if (stack.contains(sym))
                return new JavaType.Cyclic(sym.className());
            else {
                if (shallow) {
                    return new JavaType.ShallowClass(sym.className());
                } else {
                    List<Symbol> stackWithSym = new ArrayList<>(stack);
                    stackWithSym.add(sym);

                    var fields = (sym.members_field == null ? Stream.empty() : stream(sym.members_field.getSymbols().spliterator(), false))
                            .filter(elem -> elem instanceof Symbol.VarSymbol)
                            .map(Symbol.VarSymbol.class::cast)
                            .map(elem -> new JavaType.Var(
                                    elem.name.toString(),
                                    type(elem.type, stackWithSym),
                                    filteredFlags(elem)
                            ))
                            .collect(toList());

                    var classType = (com.sun.tools.javac.code.Type.ClassType) type;
                    var symType = (com.sun.tools.javac.code.Type.ClassType) sym.type;
                    return JavaType.Class.build(sym.className(), fields,
                            classType.typarams_field == null ? emptyList() : classType.typarams_field.stream().map(tParam -> type(tParam, stackWithSym, true)).filter(Objects::nonNull).collect(toList()),
                            symType.interfaces_field == null ? emptyList() : symType.interfaces_field.stream().map(iParam -> type(iParam, stackWithSym, false)).filter(Objects::nonNull).collect(toList()),
                            null,
                            TypeUtils.asClass(type(classType.supertype_field, stackWithSym)),
                            relaxedClassTypeMatching);
                }
            }
        } else if (type instanceof com.sun.tools.javac.code.Type.TypeVar) {
            return new JavaType.GenericTypeVariable(type.tsym.name.toString(), TypeUtils.asClass(type(((com.sun.tools.javac.code.Type.TypeVar) type).getUpperBound(), stack)));
        } else if (type instanceof com.sun.tools.javac.code.Type.JCPrimitiveType) {
            return primitive(type.getTag());
        } else if (type instanceof com.sun.tools.javac.code.Type.ArrayType) {
            return new JavaType.Array(type(((com.sun.tools.javac.code.Type.ArrayType) type).elemtype, stack));
        } else if (com.sun.tools.javac.code.Type.noType.equals(type)) {
            return null;
        } else {
            return null;
        }
    }

    @Nullable
    private JavaType type(Tree t) {
        return type(((JCTree) t).type);
    }

    private JavaType.Primitive primitive(TypeTag tag) {
        switch (tag) {
            case BOOLEAN:
                return JavaType.Primitive.Boolean;
            case BYTE:
                return JavaType.Primitive.Byte;
            case CHAR:
                return JavaType.Primitive.Char;
            case DOUBLE:
                return JavaType.Primitive.Double;
            case FLOAT:
                return JavaType.Primitive.Float;
            case INT:
                return JavaType.Primitive.Int;
            case LONG:
                return JavaType.Primitive.Long;
            case SHORT:
                return JavaType.Primitive.Short;
            case VOID:
                return JavaType.Primitive.Void;
            case NONE:
                return JavaType.Primitive.None;
            case CLASS:
                return JavaType.Primitive.String;
            case BOT:
                return JavaType.Primitive.Null;
            default:
                throw new IllegalArgumentException("Unknown type tag " + tag);
        }
    }

    /**
     * --------------
     * Other convenience utilities
     * --------------
     */

    private int endPos(Tree t) {
        return ((JCTree) t).getEndPosition(endPosTable);
    }

    private String sourceBefore(String untilDelim) {
        return sourceBefore(untilDelim, null);
    }

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private String sourceBefore(String untilDelim, @Nullable Character stop) {
        var delimIndex = positionOfNext(untilDelim, stop);
        if (delimIndex < 0) {
            return ""; // unable to find this delimiter
        }

        var prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return prefix;
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        var inMultiLineComment = false;
        var inSingleLineComment = false;

        var delimIndex = cursor;
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
                            delimIndex++;
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

    private final Function<Tree, String> semiDelim = ignored -> sourceBefore(";");
    private final Function<Tree, String> commaDelim = ignored -> sourceBefore(",");
    private final Function<Tree, String> noDelim = ignored -> "";

    private String whitespace() {
        return whitespace(null);
    }

    private String whitespace(@Nullable Tree t) {
        var inMultiLineComment = false;
        var inSingleLineComment = false;

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
        return prefix;
    }

    @Nullable
    private String skip(@Nullable String token) {
        if (token == null)
            return null;
        if (source.startsWith(token, cursor))
            cursor += token.length();
        return token;
    }

    private void skipPattern(String pattern) {
        var matcher = Pattern.compile("\\G" + pattern).matcher(source);
        if (matcher.find(cursor)) {
            cursor(matcher.end());
        }
    }

    // Only exists as a function to make it easier to debug unexpected cursor shifts
    private void cursor(int n) {
        cursor = n;
    }

    private boolean hasFlag(ModifiersTree modifiers, long flag) {
        return (((JCModifiers) modifiers).flags & flag) != 0L;
    }

    private List<String> listFlags(long flags) {
        var allFlags = Arrays.stream(Flags.class.getDeclaredFields())
                .filter(field -> {
                    field.setAccessible(true);
                    try {
                        // FIXME instanceof probably not right here...
                        return field.get(null) instanceof Long &&
                                field.getName().matches("[A-Z_]+");
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(Field::getName, field -> {
                    try {
                        return (Long) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }));

        List<String> all = new ArrayList<>();
        for (Map.Entry<String, Long> flagNameAndCode : allFlags.entrySet()) {
            if ((flagNameAndCode.getValue() & flags) != 0L) {
                all.add(flagNameAndCode.getKey());
            }
        }
        return all;
    }

    /**
     * Modifiers in the order they appear in the source, which is not necessarily the same as the order in
     * which they appear in the OpenJDK AST
     */
    private List<J.Modifier> sortedFlags(ModifiersTree modifiers) {
        if (modifiers.getFlags().isEmpty()) {
            return emptyList();
        }

        var sortedModifiers = new ArrayList<Modifier>();

        var inComment = false;
        var inMultilineComment = false;
        final var word = new AtomicReference<>("");
        for (int i = cursor; i < source.length(); i++) {
            var c = source.charAt(i);
            if (c == '/' && source.length() > i + 1) {
                char next = source.charAt(i + 1);
                if (next == '*') {
                    inMultilineComment = true;
                } else if (next == '/') {
                    inComment = true;
                }
            }

            if (inMultilineComment && c == '/' && source.charAt(i - 1) == '*') {
                inMultilineComment = false;
            } else if (inComment && c == '\n' || c == '\r') {
                inComment = false;
            } else if (!inMultilineComment && !inComment) {
                if (Character.isWhitespace(c)) {
                    if (!word.get().isEmpty()) {
                        Optional<Modifier> matching = modifiers.getFlags().stream()
                                .filter(mod -> mod.name().toLowerCase().equals(word.get()))
                                .findAny();

                        matching.ifPresent(mod -> {
                            sortedModifiers.add(mod);
                            word.set("");
                        });

                        if (matching.isEmpty()) {
                            break;
                        }
                    }
                } else {
                    word.getAndUpdate(w -> w + c);
                }
            }
        }

        return sortedModifiers.stream()
                .map(mod -> {
                    var modFormat = format(whitespace());
                    cursor += mod.name().length();
                    switch (mod) {
                        case DEFAULT:
                            return new J.Modifier.Default(randomId(), modFormat);
                        case PUBLIC:
                            return new J.Modifier.Public(randomId(), modFormat);
                        case PROTECTED:
                            return new J.Modifier.Protected(randomId(), modFormat);
                        case PRIVATE:
                            return new J.Modifier.Private(randomId(), modFormat);
                        case ABSTRACT:
                            return new J.Modifier.Abstract(randomId(), modFormat);
                        case STATIC:
                            return new J.Modifier.Static(randomId(), modFormat);
                        case FINAL:
                            return new J.Modifier.Final(randomId(), modFormat);
                        case NATIVE:
                            return new J.Modifier.Native(randomId(), modFormat);
                        case STRICTFP:
                            return new J.Modifier.Strictfp(randomId(), modFormat);
                        case SYNCHRONIZED:
                            return new J.Modifier.Synchronized(randomId(), modFormat);
                        case TRANSIENT:
                            return new J.Modifier.Transient(randomId(), modFormat);
                        case VOLATILE:
                            return new J.Modifier.Volatile(randomId(), modFormat);
                        default:
                            throw new IllegalArgumentException("Unexpected modifier " + mod);
                    }
                })
                .collect(toList());
    }
}
