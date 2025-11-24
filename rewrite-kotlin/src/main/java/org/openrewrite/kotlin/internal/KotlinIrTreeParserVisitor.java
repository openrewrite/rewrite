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
package org.openrewrite.kotlin.internal;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.*;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport;
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess;
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference;
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol;
import org.jetbrains.kotlin.ir.IrElement;
import org.jetbrains.kotlin.ir.declarations.*;
import org.jetbrains.kotlin.ir.expressions.*;
import org.jetbrains.kotlin.ir.visitors.IrVisitor;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.ParseUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Quoted;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIrTypeMapping;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * IR based parser
 */
@SuppressWarnings("ConstantValue")
public class KotlinIrTreeParserVisitor extends IrVisitor<J, ExecutionContext> {
    private final KotlinSource kotlinSource;
    private final KotlinIrTypeMapping typeMapping;
    private final List<NamedStyles> styles;
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final Charset charset;
    private final Boolean charsetBomMarked;
    private final ExecutionContext executionContext;
    private final List<Integer> cRLFLocations;

    public KotlinIrTreeParserVisitor(KotlinSource kotlinSource, KotlinIrTypeMapping typeMapping,
                                     List<NamedStyles> styles,
                                     @Nullable Path relativeTo,
                                     ExecutionContext ctx) {
        this.kotlinSource = kotlinSource;
        this.typeMapping = typeMapping;
        this.styles = styles;
        sourcePath = kotlinSource.getInput().getRelativePath(relativeTo);
        fileAttributes = kotlinSource.getInput().getFileAttributes();
        EncodingDetectingInputStream stream = kotlinSource.getInput().getSource(ctx);
        charset = stream.getCharset();
        charsetBomMarked = stream.isCharsetBomMarked();
        executionContext = ctx;
        cRLFLocations = kotlinSource.getCRLFLocations();
    }

    public K.CompilationUnit parse() {
        return (K.CompilationUnit) visitFile(requireNonNull(kotlinSource.getIrFile()), executionContext);
    }

    // TODO: Implement method bodies
    private static J empty() {
        return new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
    }

    private static K.ExpressionStatement exprStmt(Expression e) {
        return new K.ExpressionStatement(e);
    }

    private static Statement toStatement(J j) {
        if (j instanceof Statement) {
            return (Statement) j;
        }
        if (j instanceof Expression) {
            return exprStmt((Expression) j);
        }
        return (Statement) empty();
    }

    @Nullable
    private static Expression asExpression(J j) {
        return j instanceof Expression ? (Expression) j : null;
    }

    @Override
    public J visitAnonymousInitializer(@NotNull IrAnonymousInitializer irAnonymousInitializer, ExecutionContext executionContext) {
        return empty();
    }

    @Override
    public J visitElement(@NotNull IrElement irElement, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitDeclaration(@NotNull IrDeclarationBase irDeclarationBase, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitValueParameter(@NotNull IrValueParameter irValueParameter, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitClass(@NotNull IrClass irClass, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitTypeParameter(@NotNull IrTypeParameter irTypeParameter, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitFunction(@NotNull IrFunction irFunction, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitConstructor(@NotNull IrConstructor irConstructor, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitEnumEntry(@NotNull IrEnumEntry irEnumEntry, ExecutionContext executionContext) {
        return null;
    }

//    @Override
//    public J visitErrorDeclaration(@NotNull IrErrorDeclaration irErrorDeclaration, ExecutionContext executionContext) {
//        return null;
//    }

    @Override
    public J visitField(@NotNull IrField irField, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitLocalDelegatedProperty(@NotNull IrLocalDelegatedProperty irLocalDelegatedProperty, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitModuleFragment(@NotNull IrModuleFragment irModuleFragment, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitProperty(@NotNull IrProperty irProperty, ExecutionContext executionContext) {
        JavaType.Variable vType = (JavaType.Variable) typeMapping.type(irProperty);
        String name = irProperty.getName() == null ? "<unknown>" : irProperty.getName().asString();
        J.Identifier nameId = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name,
                vType == null ? null : vType.getType(), vType);
        // Initializer: try backing field initializer if present
        JLeftPadded<Expression> init = null;
        IrField backing = irProperty.getBackingField();
        if (backing != null && backing.getInitializer() != null) {
            J initJ = backing.getInitializer().getExpression() == null ? empty() : backing.getInitializer().getExpression().accept(this, executionContext);
            Expression initExpr = asExpression(initJ);
            if (initExpr != null) {
                init = JLeftPadded.build(initExpr);
            }
        }
        J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                nameId,
                emptyList(),
                init,
                vType
        );
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>();
        vars.add(JRightPadded.build(namedVar));
        return new J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                vars
        );
    }

    @Override
    public J visitScript(@NotNull IrScript irScript, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitSimpleFunction(@NotNull IrSimpleFunction irSimpleFunction, ExecutionContext executionContext) {
        // Minimal method declaration for top-level/local functions
        JavaType.Method mt = typeMapping.methodDeclarationType(irSimpleFunction);
        String nameStr;
        try {
            nameStr = irSimpleFunction.getName().asString();
        } catch (Throwable t) {
            nameStr = "<fn>";
        }
        J.MethodDeclaration.IdentifierWithAnnotations name = new J.MethodDeclaration.IdentifierWithAnnotations(
                new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), nameStr, mt, null), emptyList());

        // Parameters container: keep it simple for now with empty parens if none
        List<JRightPadded<Statement>> paramElems = new ArrayList<>();
        if (irSimpleFunction.getValueParameters().isEmpty()) {
            paramElems.add(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)));
        } else {
            for (IrValueParameter p : irSimpleFunction.getValueParameters()) {
                // Represent parameters as variable declarations with just the name
                JavaType.Variable pv = (JavaType.Variable) typeMapping.type(p);
                String pn = p.getName() == null ? "p" : p.getName().asString();
                J.Identifier pid = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), pn,
                        pv == null ? null : pv.getType(), pv);
                J.VariableDeclarations.NamedVariable nv = new J.VariableDeclarations.NamedVariable(
                        randomId(), Space.EMPTY, Markers.EMPTY, pid, emptyList(), null, pv);
                J.VariableDeclarations vd = new J.VariableDeclarations(randomId(), Space.EMPTY, Markers.EMPTY,
                        emptyList(), emptyList(), null, null, singletonList(JRightPadded.build(nv)));
                paramElems.add(JRightPadded.build(vd));
            }
        }
        JContainer<Statement> params = JContainer.build(Space.EMPTY, paramElems, Markers.EMPTY);

        J.Block body = null;
        if (irSimpleFunction.getBody() != null) {
            J b = irSimpleFunction.getBody().accept(this, executionContext);
            if (b instanceof J.Block) {
                body = (J.Block) b;
            } else if (b instanceof Expression) {
                // expression to block
                body = new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false),
                        singletonList(JRightPadded.build(exprStmt((Expression) b))), Space.EMPTY);
            }
        }

        return new J.MethodDeclaration(randomId(), Space.EMPTY, Markers.EMPTY,
                emptyList(), emptyList(), null, null, name, params, null, body, null, mt);
    }

    @Override
    public J visitTypeAlias(@NotNull IrTypeAlias irTypeAlias, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitVariable(@NotNull IrVariable irVariable, ExecutionContext executionContext) {
        JavaType.Variable vType = (JavaType.Variable) typeMapping.type(irVariable);
        // Name
        String name = irVariable.getName() == null ? "<unknown>" : irVariable.getName().asString();
        J.Identifier nameId = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name,
                vType == null ? null : vType.getType(), vType);
        // Initializer
        JLeftPadded<Expression> init = null;
        if (irVariable.getInitializer() != null) {
            J initJ = irVariable.getInitializer().accept(this, executionContext);
            Expression initExpr = asExpression(initJ);
            if (initExpr == null) {
                initExpr = (Expression) empty();
            }
            init = JLeftPadded.build(initExpr);
        }
        J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                nameId,
                emptyList(),
                init,
                vType
        );
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>();
        vars.add(JRightPadded.build(namedVar));
        // Type expression: omit for now, rely on attributed type
        return new J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                vars
        );
    }

    @Override
    public J visitPackageFragment(@NotNull IrPackageFragment irPackageFragment, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitExternalPackageFragment(@NotNull IrExternalPackageFragment irExternalPackageFragment, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitFile(@NotNull IrFile irFile, ExecutionContext executionContext) {
        String shebang = null;
        List<J.Annotation> annotations = emptyList();
        JRightPadded<J.Package> pkg = null;
        if (!irFile.getPackageFqName().isRoot()) {
            String fq = irFile.getPackageFqName().asString();
            J.Identifier pkgName = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), fq, null, null);
            J.Package p = new J.Package(randomId(), Space.EMPTY, Markers.EMPTY, pkgName, emptyList());
            pkg = JRightPadded.build(p);
        }
        List<JRightPadded<J.Import>> imports = new ArrayList<>();
        List<JRightPadded<Statement>> statements = new ArrayList<>();
        for (IrDeclaration dec : irFile.getDeclarations()) {
            J result = dec.accept(this, executionContext);
            if (result instanceof Statement) {
                statements.add(JRightPadded.build((Statement) result));
            } else {
                statements.add(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)));
            }
        }
        return new K.CompilationUnit(
                Tree.randomId(),
                shebang,
                Space.EMPTY,
                Markers.build(styles),
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                annotations,
                pkg,
                imports,
                statements,
                Space.EMPTY
        );
    }

    @Override
    public J visitExpression(@NotNull IrExpression irExpression, ExecutionContext executionContext) {
        // Fallback for any IR expression not yet handled
        return empty();
    }

    @Override
    public J visitBody(@NotNull IrBody irBody, ExecutionContext executionContext) {
        if (irBody instanceof IrBlockBody) {
            return visitBlockBody((IrBlockBody) irBody, executionContext);
        }
        if (irBody instanceof IrExpressionBody) {
            return visitExpressionBody((IrExpressionBody) irBody, executionContext);
        }
        return J.Block.createEmptyBlock();
    }

    @Override
    public J visitExpressionBody(@NotNull IrExpressionBody irExpressionBody, ExecutionContext executionContext) {
        J expr = irExpressionBody.getExpression() == null ? empty() : irExpressionBody.getExpression().accept(this, executionContext);
        Statement stmt = toStatement(expr);
        List<JRightPadded<Statement>> stmts = new ArrayList<>();
        stmts.add(JRightPadded.build(stmt));
        return new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY);
    }

    @Override
    public J visitBlockBody(@NotNull IrBlockBody irBlockBody, ExecutionContext executionContext) {
        List<JRightPadded<Statement>> stmts = new ArrayList<>();
        for (IrElement s : irBlockBody.getStatements()) {
            J j = s.accept(this, executionContext);
            stmts.add(JRightPadded.build(toStatement(j)));
        }
        return new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY);
    }

    @Override
    public J visitDeclarationReference(@NotNull IrDeclarationReference irDeclarationReference, ExecutionContext executionContext) {
        // Fallback: render as a simple identifier with attributed type
        String name = "<ref>";
        try {
            name = irDeclarationReference.getSymbol().getOwner().getClass().getSimpleName();
        } catch (Throwable ignored) {
        }
        JavaType t = typeMapping.type(irDeclarationReference);
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, t, null);
    }

    @Override
    public J visitMemberAccess(@NotNull IrMemberAccessExpression<?> irMemberAccessExpression, ExecutionContext executionContext) {
        // Fallback to expression
        return visitExpression((IrExpression) irMemberAccessExpression, executionContext);
    }

    @Override
    public J visitFunctionAccess(@NotNull IrFunctionAccessExpression irFunctionAccessExpression, ExecutionContext executionContext) {
        if (irFunctionAccessExpression instanceof IrCall) {
            return visitCall((IrCall) irFunctionAccessExpression, executionContext);
        }
        return visitExpression(irFunctionAccessExpression, executionContext);
    }

    @Override
    public J visitConstructorCall(@NotNull IrConstructorCall irConstructorCall, ExecutionContext executionContext) {
        // new class expression: type and args
        Expression select = null;
        JavaType.Method mt = (JavaType.Method) typeMapping.type(irConstructorCall);
        List<JRightPadded<Expression>> args = new ArrayList<>();
        int vc = irConstructorCall.getValueArgumentsCount();
        for (int i = 0; i < vc; i++) {
            IrExpression a = irConstructorCall.getValueArgument(i);
            if (a == null) continue;
            J aj = a.accept(this, executionContext);
            Expression eax = asExpression(aj);
            if (eax == null) eax = (Expression) empty();
            args.add(JRightPadded.build(eax));
        }
        JContainer<Expression> arguments = JContainer.build(Space.EMPTY, args, Markers.EMPTY);
        // Qualifier type for allocation
        TypeTree clazz = null;
        JavaType allocatedType = mt == null ? null : mt.getDeclaringType();
        if (allocatedType != null) {
            clazz = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), TypeUtils.asFullyQualified(allocatedType) != null ? TypeUtils.asFullyQualified(allocatedType).getClassName() : allocatedType.toString(), allocatedType, null);
        }
        return new J.NewClass(randomId(), Space.EMPTY, Markers.EMPTY, null, Space.EMPTY, clazz, arguments, null, mt);
    }

    @Override
    public J visitSingletonReference(@NotNull IrGetSingletonValue irGetSingletonValue, ExecutionContext executionContext) {
        // reference to an object singleton; map to identifier with type
        JavaType t = typeMapping.type(irGetSingletonValue);
        String name = "<object>";
        if (t instanceof JavaType.FullyQualified) {
            name = ((JavaType.FullyQualified) t).getClassName();
        }
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, t, null);
    }

    @Override
    public J visitGetObjectValue(@NotNull IrGetObjectValue irGetObjectValue, ExecutionContext executionContext) {
        JavaType t = typeMapping.type(irGetObjectValue);
        String name = "<object>";
        if (t instanceof JavaType.FullyQualified) {
            name = ((JavaType.FullyQualified) t).getClassName();
        }
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, t, null);
    }

    @Override
    public J visitGetEnumValue(@NotNull IrGetEnumValue irGetEnumValue, ExecutionContext executionContext) {
        JavaType t = typeMapping.type(irGetEnumValue);
        String name = "<enum>";
        try {
            name = irGetEnumValue.getSymbol().getOwner().getName().asString();
        } catch (Throwable ignored) {
        }
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, t, null);
    }

    @Override
    public J visitRawFunctionReference(@NotNull IrRawFunctionReference irRawFunctionReference, ExecutionContext executionContext) {
        JavaType t = typeMapping.type(irRawFunctionReference);
        String name = "<fun>";
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, t, null);
    }

    @Override
    public J visitContainerExpression(@NotNull IrContainerExpression irContainerExpression, ExecutionContext executionContext) {
        if (irContainerExpression instanceof IrBlock) {
            return visitBlock((IrBlock) irContainerExpression, executionContext);
        }
        if (irContainerExpression instanceof IrComposite) {
            return visitComposite((IrComposite) irContainerExpression, executionContext);
        }
        // Fallback: collect statements into a block if available
        List<JRightPadded<Statement>> stmts = new ArrayList<>();
        for (IrElement e : irContainerExpression.getStatements()) {
            J j = e.accept(this, executionContext);
            stmts.add(JRightPadded.build(toStatement(j)));
        }
        return new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY);
    }

    @Override
    public J visitBlock(@NotNull IrBlock irBlock, ExecutionContext executionContext) {
        List<JRightPadded<Statement>> stmts = new ArrayList<>();
        for (IrElement e : irBlock.getStatements()) {
            J j = e.accept(this, executionContext);
            stmts.add(JRightPadded.build(toStatement(j)));
        }
        return new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY);
    }

    @Override
    public J visitComposite(@NotNull IrComposite irComposite, ExecutionContext executionContext) {
        List<JRightPadded<Statement>> stmts = new ArrayList<>();
        for (IrElement e : irComposite.getStatements()) {
            J j = e.accept(this, executionContext);
            stmts.add(JRightPadded.build(toStatement(j)));
        }
        return new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY);
    }

    @Override
    public J visitSyntheticBody(@NotNull IrSyntheticBody irSyntheticBody, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitBreakContinue(@NotNull IrBreakContinue irBreakContinue, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitBreak(@NotNull IrBreak irBreak, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitContinue(@NotNull IrContinue irContinue, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitCall(@NotNull IrCall irCall, ExecutionContext executionContext) {
        // Handle unary logical not: Kotlin '!' operator is a call to 'not' on Boolean
        String simpleName = null;
        try {
            simpleName = irCall.getSymbol().getOwner().getName().asString();
        } catch (Throwable ignore) {
        }
        if ("not".equals(simpleName) && irCall.getDispatchReceiver() != null) {
            J recv = irCall.getDispatchReceiver().accept(this, executionContext);
            Expression expr = asExpression(recv);
            if (expr == null) expr = (Expression) empty();
            return new J.Unary(randomId(), Space.EMPTY, Markers.EMPTY, JLeftPadded.build(J.Unary.Type.Not), expr, typeMapping.type(irCall));
        }

        // Regular method invocation
        JRightPadded<Expression> select = null;
        IrExpression dispatch = irCall.getDispatchReceiver();
        if (dispatch == null) {
            dispatch = irCall.getExtensionReceiver();
        }
        if (dispatch != null) {
            J selJ = dispatch.accept(this, executionContext);
            Expression selExpr = asExpression(selJ);
            if (selExpr != null) {
                select = JRightPadded.build(selExpr);
            }
        }
        if (simpleName == null) {
            simpleName = "<invoke>";
        }
        JavaType.Method mt = typeMapping.methodInvocationType(irCall);
        J.Identifier name = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), simpleName, mt, null);
        List<JRightPadded<Expression>> argsList = new ArrayList<>();
        int argc = irCall.getValueArgumentsCount();
        for (int i = 0; i < argc; i++) {
            IrExpression a = irCall.getValueArgument(i);
            if (a == null) continue;
            J aj = a.accept(this, executionContext);
            Expression ex = asExpression(aj);
            if (ex == null) ex = (Expression) empty();
            argsList.add(JRightPadded.build(ex));
        }
        JContainer<Expression> args = JContainer.build(Space.EMPTY, argsList.isEmpty() ? singletonList(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))) : argsList, Markers.EMPTY);
        return new J.MethodInvocation(randomId(), Space.EMPTY, Markers.EMPTY, select, null, name, args, mt);
    }

    @Override
    public J visitCallableReference(@NotNull IrCallableReference<?> irCallableReference, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitFunctionReference(@NotNull IrFunctionReference irFunctionReference, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitPropertyReference(@NotNull IrPropertyReference irPropertyReference, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitLocalDelegatedPropertyReference(@NotNull IrLocalDelegatedPropertyReference irLocalDelegatedPropertyReference, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitClassReference(@NotNull IrClassReference irClassReference, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitConst(@NotNull IrConst expression, ExecutionContext data) {
        JavaType t = typeMapping.type(expression);
        JavaType.Primitive pt = t instanceof JavaType.Primitive ? (JavaType.Primitive) t : JavaType.Primitive.String;
        Object value = expression.getValue();
        // valueSource can be null for now; pretty printers will synthesize as needed
        return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, null, null, pt);
    }

    @Override
    public J visitConstantValue(@NotNull IrConstantValue irConstantValue, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitConstantPrimitive(@NotNull IrConstantPrimitive irConstantPrimitive, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitConstantObject(@NotNull IrConstantObject irConstantObject, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitConstantArray(@NotNull IrConstantArray irConstantArray, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitDelegatingConstructorCall(@NotNull IrDelegatingConstructorCall irDelegatingConstructorCall, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitDynamicExpression(@NotNull IrDynamicExpression irDynamicExpression, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitDynamicOperatorExpression(@NotNull IrDynamicOperatorExpression irDynamicOperatorExpression, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitDynamicMemberExpression(@NotNull IrDynamicMemberExpression irDynamicMemberExpression, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitEnumConstructorCall(@NotNull IrEnumConstructorCall irEnumConstructorCall, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitErrorExpression(@NotNull IrErrorExpression irErrorExpression, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitErrorCallExpression(@NotNull IrErrorCallExpression irErrorCallExpression, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitFieldAccess(@NotNull IrFieldAccessExpression irFieldAccessExpression, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitGetField(@NotNull IrGetField irGetField, ExecutionContext executionContext) {
        String name;
        try {
            name = irGetField.getSymbol().getOwner().getName().asString();
        } catch (Throwable t) {
            name = "<field>";
        }
        JavaType t = typeMapping.type(irGetField);
        JavaType.Variable var = t instanceof JavaType.Variable ? (JavaType.Variable) t : null;
        JavaType exprType = var != null ? var.getType() : t;
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, exprType, var);
    }

    @Override
    public J visitSetField(@NotNull IrSetField irSetField, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitFunctionExpression(@NotNull IrFunctionExpression irFunctionExpression, ExecutionContext executionContext) {
        IrFunction func = irFunctionExpression.getFunction();
        // Parameters
        List<JRightPadded<J>> params = new ArrayList<>();
        boolean parenthesized = true;
        for (IrValueParameter p : func.getValueParameters()) {
            JavaType.Variable pv = (JavaType.Variable) typeMapping.type(p);
            String pn = p.getName() == null ? "p" : p.getName().asString();
            J.Identifier pid = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), pn,
                    pv == null ? null : pv.getType(), pv);
            params.add(JRightPadded.build(pid));
        }
        J.Lambda.Parameters lparams = new J.Lambda.Parameters(randomId(), Space.EMPTY, Markers.EMPTY, parenthesized, params);
        // Body
        J body = null;
        if (func.getBody() != null) {
            body = func.getBody().accept(this, executionContext);
            if (!(body instanceof J.Block)) {
                // wrap expressions into block per Kotlin printer expectations
                Expression as = asExpression(body);
                if (as != null) {
                    body = new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false),
                            singletonList(JRightPadded.build(exprStmt(as))), Space.EMPTY);
                }
            }
        } else {
            body = new J.Block(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), emptyList(), Space.EMPTY);
        }
        JavaType t = typeMapping.type(irFunctionExpression);
        return new J.Lambda(randomId(), Space.EMPTY, Markers.EMPTY, lparams, Space.EMPTY, body, t);
    }

    @Override
    public J visitGetClass(@NotNull IrGetClass irGetClass, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitInstanceInitializerCall(@NotNull IrInstanceInitializerCall irInstanceInitializerCall, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitLoop(@NotNull IrLoop irLoop, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitWhileLoop(@NotNull IrWhileLoop irWhileLoop, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitDoWhileLoop(@NotNull IrDoWhileLoop irDoWhileLoop, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitReturn(@NotNull IrReturn irReturn, ExecutionContext executionContext) {
        Expression expr = null;
        if (irReturn.getValue() != null) {
            J j = irReturn.getValue().accept(this, executionContext);
            expr = asExpression(j);
        }
        return new J.Return(randomId(), Space.EMPTY, Markers.EMPTY, expr);
    }

    @Override
    public J visitStringConcatenation(@NotNull IrStringConcatenation irStringConcatenation, ExecutionContext executionContext) {
        List<IrExpression> args = irStringConcatenation.getArguments();
        if (args.isEmpty()) {
            return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "", "\"\"", null, JavaType.Primitive.String);
        }
        Expression expr = asExpression(args.get(0).accept(this, executionContext));
        if (expr == null) {
            expr = (Expression) empty();
        }
        for (int i = 1; i < args.size(); i++) {
            Expression right = asExpression(args.get(i).accept(this, executionContext));
            if (right == null) {
                right = (Expression) empty();
            }
            expr = new J.Binary(randomId(), Space.EMPTY, Markers.EMPTY, expr, JLeftPadded.build(J.Binary.Type.Addition), right, JavaType.Primitive.String);
        }
        return expr;
    }

    @Override
    public J visitSuspensionPoint(@NotNull IrSuspensionPoint irSuspensionPoint, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitSuspendableExpression(@NotNull IrSuspendableExpression irSuspendableExpression, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitThrow(@NotNull IrThrow irThrow, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitTry(@NotNull IrTry irTry, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitCatch(@NotNull IrCatch irCatch, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitTypeOperator(@NotNull IrTypeOperatorCall irTypeOperatorCall, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitValueAccess(@NotNull IrValueAccessExpression irValueAccessExpression, ExecutionContext executionContext) {
        if (irValueAccessExpression instanceof IrGetValue) {
            return visitGetValue((IrGetValue) irValueAccessExpression, executionContext);
        }
        return empty();
    }

    @Override
    public J visitGetValue(@NotNull IrGetValue irGetValue, ExecutionContext executionContext) {
        String name;
        try {
            name = irGetValue.getSymbol().getOwner().getName().asString();
        } catch (Throwable t) {
            name = "<value>";
        }
        JavaType t = typeMapping.type(irGetValue);
        JavaType.Variable var = t instanceof JavaType.Variable ? (JavaType.Variable) t : null;
        JavaType exprType = var != null ? var.getType() : t;
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, exprType, var);
    }

    @Override
    public J visitSetValue(@NotNull IrSetValue irSetValue, ExecutionContext executionContext) {
        // Treat as a simple assignment to a local variable
        String name;
        try {
            name = irSetValue.getSymbol().getOwner().getName().asString();
        } catch (Throwable t) {
            name = "<value>";
        }
        JavaType t = typeMapping.type(irSetValue);
        J.Identifier id = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, t, null);
        Expression value = null;
        if (irSetValue.getValue() != null) {
            J v = irSetValue.getValue().accept(this, executionContext);
            value = asExpression(v);
        }
        if (value == null) {
            value = (Expression) empty();
        }
        return new J.Assignment(randomId(), Space.EMPTY, Markers.EMPTY, id, JLeftPadded.build(value), t);
    }

    @Override
    public J visitVararg(@NotNull IrVararg irVararg, ExecutionContext executionContext) {
        // Represent as a new array with given elements
        List<JRightPadded<Expression>> elems = new ArrayList<>();
        for (IrVarargElement e : irVararg.getElements()) {
            J je;
            if (e instanceof IrSpreadElement) {
                je = ((IrSpreadElement) e).getExpression().accept(this, executionContext);
            } else if (e instanceof IrExpression) {
                je = ((IrExpression) e).accept(this, executionContext);
            } else {
                je = empty();
            }
            Expression ex = asExpression(je);
            if (ex == null) ex = (Expression) empty();
            elems.add(JRightPadded.build(ex));
        }
        JContainer<Expression> init = JContainer.build(Space.EMPTY, elems, Markers.EMPTY);
        JavaType arrType = typeMapping.type(irVararg);
        return new J.NewArray(randomId(), Space.EMPTY, Markers.EMPTY, null, Collections.emptyList(), init, arrType);
    }

    @Override
    public J visitSpreadElement(@NotNull IrSpreadElement irSpreadElement, ExecutionContext executionContext) {
        // Just return the underlying expression
        if (irSpreadElement.getExpression() != null) {
            return irSpreadElement.getExpression().accept(this, executionContext);
        }
        return empty();
    }

    @Override
    public J visitWhen(@NotNull IrWhen irWhen, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitBranch(@NotNull IrBranch irBranch, ExecutionContext executionContext) {
        return null;
    }

    @Override
    public J visitElseBranch(@NotNull IrElseBranch irElseBranch, ExecutionContext executionContext) {
        return null;
    }
}
