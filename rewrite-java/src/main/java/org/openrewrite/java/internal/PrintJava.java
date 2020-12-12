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
package org.openrewrite.java.internal;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AbstractJavaSourceVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Collection;

import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.java.tree.J.Modifier.*;

public class PrintJava extends AbstractJavaSourceVisitor<String> {
    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String reduce(String r1, String r2) {
        return r1 + r2;
    }

    String visit(Collection<? extends Tree> nodes, String suffixBetween) {
        return visit(nodes, suffixBetween, "");
    }

    String visit(Collection<? extends Tree> nodes, String suffixBetween, String suffixEnd) {
        String acc = "";
        Tree[] array = nodes.toArray(new Tree[0]);
        for (int i = 0; i < array.length; i++) {
            Tree node = array[i];
            acc = reduce(acc, visit(node) + (i == array.length - 1 ? suffixEnd : suffixBetween));
        }
        return acc;
    }

    String visitStatements(Collection<? extends Tree> statements) {
        return statements.stream()
                .map(this::fmtStatement)
                .reduce("", this::reduce);
    }

    private String fmtStatement(Tree statement) {
        String semicolonOrEmpty = (statement instanceof Statement && ((Statement) statement).isSemicolonTerminated()) ||
                (statement instanceof MethodDecl && ((MethodDecl) statement).isAbstract()) ? ";" : "";
        return visit(statement) + semicolonOrEmpty;
    }

    private String fmt(@Nullable Tree tree, @Nullable String code) {
        return tree == null || code == null ? "" : tree.getPrefix() + code + tree.getSuffix();
    }

    String visitModifiers(Iterable<Modifier> modifiers) {
        return stream(modifiers.spliterator(), false)
                .map(mod -> {
                    String keyword = "";
                    if (mod instanceof Public) {
                        keyword = "public";
                    } else if (mod instanceof Protected) {
                        keyword = "protected";
                    } else if (mod instanceof Private) {
                        keyword = "private";
                    } else if (mod instanceof Abstract) {
                        keyword = "abstract";
                    } else if (mod instanceof Native) {
                        keyword = "native";
                    } else if (mod instanceof Static) {
                        keyword = "static";
                    } else if (mod instanceof Strictfp) {
                        keyword = "strictfp";
                    } else if (mod instanceof Final) {
                        keyword = "final";
                    } else if (mod instanceof Transient) {
                        keyword = "transient";
                    } else if (mod instanceof Volatile) {
                        keyword = "volatile";
                    } else if (mod instanceof Default) {
                        keyword = "default";
                    } else if (mod instanceof Modifier.Synchronized) {
                        keyword = "synchronized";
                    }
                    return fmt(mod, keyword);
                })
                .reduce("", this::reduce);
    }

    String visitDims(Collection<VariableDecls.Dimension> dims) {
        return dims.stream().map(d -> fmt(d, "[" + visit(d.getWhitespace()) + "]"))
                .reduce("", this::reduce);
    }

    @Override
    public String visitAnnotatedType(AnnotatedType annotatedType) {
        return fmt(annotatedType, visit(annotatedType.getAnnotations(), "") + visit(annotatedType.getTypeExpr()));
    }

    @Override
    public String visitAnnotation(Annotation annotation) {
        String args = annotation.getArgs() == null ? "" :
                fmt(annotation.getArgs(), "(" + visit(annotation.getArgs().getArgs(), ",") + ")");
        return fmt(annotation, "@" + visit(annotation.getAnnotationType()) + args);
    }

    @Override
    public String visitArrayAccess(ArrayAccess arrayAccess) {
        String dimension = fmt(arrayAccess.getDimension(), "[" + visit(arrayAccess.getDimension().getIndex()) + "]");
        return fmt(arrayAccess, visit(arrayAccess.getIndexed()) + dimension);
    }

    @Override
    public String visitArrayType(ArrayType arrayType) {
        String dimension = arrayType.getDimensions().stream()
                .map(d -> fmt(d, "[" + visit(d.getInner()) + "]"))
                .reduce("", this::reduce);
        return fmt(arrayType, visit(arrayType.getElementType()) + dimension);
    }

    @Override
    public String visitAssert(Assert azzert) {
        return fmt(azzert, "assert" + visit(azzert.getCondition()));
    }

    @Override
    public String visitAssign(Assign assign) {
        return fmt(assign, visit(assign.getVariable()) + "=" + visit(assign.getAssignment()));
    }

    @Override
    public String visitAssignOp(AssignOp assignOp) {
        String keyword = "";
        if (assignOp.getOperator() instanceof AssignOp.Operator.Addition) {
            keyword = "+=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.Subtraction) {
            keyword = "-=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.Multiplication) {
            keyword = "*=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.Division) {
            keyword = "/=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.Modulo) {
            keyword = "%=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.BitAnd) {
            keyword = "&=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.BitOr) {
            keyword = "|=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.BitXor) {
            keyword = "^=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.LeftShift) {
            keyword = "<<=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.RightShift) {
            keyword = ">>=";
        } else if (assignOp.getOperator() instanceof AssignOp.Operator.UnsignedRightShift) {
            keyword = ">>>=";
        }

        return fmt(assignOp, visit(assignOp.getVariable()) + fmt(assignOp.getOperator(), keyword) + visit(assignOp.getAssignment()));
    }

    @Override
    public String visitBinary(Binary binary) {
        String keyword = "";
        if (binary.getOperator() instanceof Binary.Operator.Addition) {
            keyword = "+";
        } else if (binary.getOperator() instanceof Binary.Operator.Subtraction) {
            keyword = "-";
        } else if (binary.getOperator() instanceof Binary.Operator.Multiplication) {
            keyword = "*";
        } else if (binary.getOperator() instanceof Binary.Operator.Division) {
            keyword = "/";
        } else if (binary.getOperator() instanceof Binary.Operator.Modulo) {
            keyword = "%";
        } else if (binary.getOperator() instanceof Binary.Operator.LessThan) {
            keyword = "<";
        } else if (binary.getOperator() instanceof Binary.Operator.GreaterThan) {
            keyword = ">";
        } else if (binary.getOperator() instanceof Binary.Operator.LessThanOrEqual) {
            keyword = "<=";
        } else if (binary.getOperator() instanceof Binary.Operator.GreaterThanOrEqual) {
            keyword = ">=";
        } else if (binary.getOperator() instanceof Binary.Operator.Equal) {
            keyword = "==";
        } else if (binary.getOperator() instanceof Binary.Operator.NotEqual) {
            keyword = "!=";
        } else if (binary.getOperator() instanceof Binary.Operator.BitAnd) {
            keyword = "&";
        } else if (binary.getOperator() instanceof Binary.Operator.BitOr) {
            keyword = "|";
        } else if (binary.getOperator() instanceof Binary.Operator.BitXor) {
            keyword = "^";
        } else if (binary.getOperator() instanceof Binary.Operator.LeftShift) {
            keyword = "<<";
        } else if (binary.getOperator() instanceof Binary.Operator.RightShift) {
            keyword = ">>";
        } else if (binary.getOperator() instanceof Binary.Operator.UnsignedRightShift) {
            keyword = ">>>";
        } else if (binary.getOperator() instanceof Binary.Operator.Or) {
            keyword = "||";
        } else if (binary.getOperator() instanceof Binary.Operator.And) {
            keyword = "&&";
        }

        return fmt(binary, visit(binary.getLeft()) + fmt(binary.getOperator(), keyword) + visit(binary.getRight()));
    }

    @Override
    public String visitBlock(Block<J> block) {
        return fmt(block,
                (block.getAfterStatic() == null ? "" : "static") +
                        visit(block.getAfterStatic()) +
                        "{" + visitStatements(block.getStatements()) +
                        block.getEnd().getPrefix() + "}"
        );
    }

    @Override
    public String visitBreak(Break breakStatement) {
        return fmt(breakStatement, "break" + visit(breakStatement.getLabel()));
    }

    @Override
    public String visitCase(Case caze) {
        return fmt(caze, visit(caze.getPattern()) + ":" + visitStatements(caze.getStatements()));
    }

    @Override
    public String visitCatch(Try.Catch catzh) {
        return fmt(catzh, "catch" + visit(catzh.getParam()) + visit(catzh.getBody()));
    }

    @Override
    public String visitClassDecl(ClassDecl classDecl) {
        String modifiers = visitModifiers(classDecl.getModifiers());

        String kind = "";
        if (classDecl.getKind() instanceof ClassDecl.Kind.Class) {
            kind = "class";
        } else if (classDecl.getKind() instanceof ClassDecl.Kind.Enum) {
            kind = "enum";
        } else if (classDecl.getKind() instanceof ClassDecl.Kind.Interface) {
            kind = "interface";
        } else if (classDecl.getKind() instanceof ClassDecl.Kind.Annotation) {
            kind = "@interface";
        }

        return fmt(classDecl, visit(classDecl.getAnnotations()) +
                modifiers + fmt(classDecl.getKind(), kind) +
                visit(classDecl.getName()) +
                visit(classDecl.getTypeParameters()) +
                (classDecl.getExtends() == null ? "" :
                        fmt(classDecl.getExtends(), "extends" + visit(classDecl.getExtends().getFrom()))) +
                (classDecl.getImplements() == null ? "" :
                        fmt(classDecl.getImplements(),
                                (classDecl.getKind() instanceof ClassDecl.Kind.Interface ? "extends" : "implements") +
                                        visit(classDecl.getImplements().getFrom(), ","))) +
                visit(classDecl.getBody()));
    }

    @Override
    public String visitCompilationUnit(CompilationUnit cu) {
        return fmt(cu, (cu.getPackageDecl() == null ? "" : visit(cu.getPackageDecl()) + ";") +
                visit(cu.getImports(), ";", ";") +
                visit(cu.getClasses())) +
                visit(cu.getEof());
    }

    @Override
    public String visitContinue(Continue continueStatement) {
        return fmt(continueStatement, "continue" + visit(continueStatement.getLabel()));
    }

    @Override
    public String visitDoWhileLoop(DoWhileLoop doWhileLoop) {
        return fmt(doWhileLoop, "do" + fmtStatement(doWhileLoop.getBody()) + fmt(doWhileLoop.getWhileCondition(), "while") + visit(doWhileLoop.getWhileCondition().getCondition()));
    }

    @Override
    public String visitEmpty(Empty empty) {
        return fmt(empty, "");
    }

    @Override
    public String visitEnumValue(EnumValue enoom) {
        String init = "";

        NewClass initializer = enoom.getInitializer();
        if (initializer != null) {
            if (initializer.getArgs() != null) {
                init = fmt(initializer.getArgs(), "(" + visit(initializer.getArgs().getArgs(), ",") + ")");
            }
            init = fmt(initializer, init + visit(initializer.getBody()));
        }

        return fmt(enoom, visit(enoom.getName()) + init);
    }

    @Override
    public String visitEnumValueSet(EnumValueSet enums) {
        return fmt(enums, visit(enums.getEnums(), ",") +
                (enums.isTerminatedWithSemicolon() ? ";" : ""));
    }

    @Override
    public String visitFieldAccess(FieldAccess fieldAccess) {
        return fmt(fieldAccess, visit(fieldAccess.getTarget()) + "." + visit(fieldAccess.getName()));
    }

    public String visitFinally(Try.Finally finallie) {
        return fmt(finallie, "finally" + visit(finallie.getBody()));
    }

    @Override
    public String visitForLoop(ForLoop forLoop) {
        ForLoop.Control ctrl = forLoop.getControl();
        String expr = fmt(ctrl, "(" + visit(ctrl.getInit()) + ";" + visit(ctrl.getCondition()) + ";" + visit(ctrl.getUpdate(), ",", "") + ")");
        return fmt(forLoop, "for" + expr + fmtStatement(forLoop.getBody()));
    }

    @Override
    public String visitForEachLoop(ForEachLoop forEachLoop) {
        ForEachLoop.Control ctrl = forEachLoop.getControl();
        String expr = fmt(ctrl, "(" + visit(ctrl.getVariable()) + ":" + visit(ctrl.getIterable()) + ")");
        return fmt(forEachLoop, "for" + expr + fmtStatement(forEachLoop.getBody()));
    }

    @Override
    public String visitIdentifier(Ident ident) {
        return fmt(ident, ident.getSimpleName());
    }

    @Override
    public String visitIf(If iff) {
        String elsePart = iff.getElsePart() == null ? "" :
                fmt(iff.getElsePart(), "else" + fmtStatement(iff.getElsePart().getStatement()));
        return fmt(iff, "if" + visit(iff.getIfCondition()) + fmtStatement(iff.getThenPart()) + elsePart);
    }

    @Override
    public String visitImport(Import impoort) {
        return fmt(impoort, "import" + (impoort.isStatic() ? " static" : "") + visit(impoort.getQualid()));
    }

    @Override
    public String visitInstanceOf(InstanceOf instanceOf) {
        return fmt(instanceOf, visit(instanceOf.getExpr()) + "instanceof" + visit(instanceOf.getClazz()));
    }

    @Override
    public String visitLabel(Label label) {
        return fmt(label, visit(label.getLabel()) +
                visit(label.getBeforeColon()) + ":" +
                visit(label.getStatement()));
    }

    @Override
    public String visitLambda(Lambda lambda) {
        String params = visit(lambda.getParamSet().getParams(), ",");
        String paramSet = fmt(lambda.getParamSet(), lambda.getParamSet().isParenthesized() ? "(" + params + ")" : params);
        return fmt(lambda, paramSet + fmt(lambda.getArrow(), "->") + visit(lambda.getBody()));
    }

    @Override
    public String visitLiteral(Literal literal) {
        return fmt(literal, literal.getValueSource());
    }

    @Override
    public String visitMemberReference(MemberReference memberRef) {
        return fmt(memberRef, visit(memberRef.getContaining()) + "::" +
                visit(memberRef.getTypeParameters()) + visit(memberRef.getReference()));
    }

    @Override
    public String visitMethod(MethodDecl method) {
        String modifiers = visitModifiers(method.getModifiers());
        String params = fmt(method.getParams(), "(" + visit(method.getParams().getParams(), ",")) + ")";
        String defaultValue = method.getDefaultValue() == null ? "" :
                fmt(method.getDefaultValue(), "default" + visit(method.getDefaultValue().getValue()));
        String thrown = method.getThrows() == null ? "" :
                fmt(method.getThrows(), "throws" + visit(method.getThrows().getExceptions(), ","));

        return fmt(method, visit(method.getAnnotations()) + modifiers + visit(method.getTypeParameters()) +
                visit(method.getReturnTypeExpr()) + visit(method.getName()) + params +
                thrown + visit(method.getBody()) + defaultValue);
    }

    @Override
    public String visitMethodInvocation(MethodInvocation method) {
        String args = fmt(method.getArgs(), "(" + visit(method.getArgs().getArgs(), ",") + ")");
        String typeParams = method.getTypeParameters() == null ? "" :
                fmt(method.getTypeParameters(), "<" + visit(method.getTypeParameters().getParams(), ",") + ">");
        String selectSeparator = method.getSelect() == null ? "" : ".";
        return fmt(method, visit(method.getSelect()) + selectSeparator + typeParams + visit(method.getName()) + args);
    }

    @Override
    public String visitMultiCatch(MultiCatch multiCatch) {
        return fmt(multiCatch, visit(multiCatch.getAlternatives(), "|"));
    }

    @Override
    public String visitMultiVariable(VariableDecls multiVariable) {
        String modifiers = visitModifiers(multiVariable.getModifiers());
        String varargs = multiVariable.getVarargs() == null ? "" :
                fmt(multiVariable.getVarargs(), "...");

        return fmt(multiVariable, visit(multiVariable.getAnnotations()) + modifiers +
                visit(multiVariable.getTypeExpr()) + visitDims(multiVariable.getDimensionsBeforeName()) +
                varargs + visit(multiVariable.getVars(), ","));
    }

    @Override
    public String visitNewArray(NewArray newArray) {
        String typeExpr = newArray.getTypeExpr() == null ? "" :
                "new" + visit(newArray.getTypeExpr());
        String dimensions = newArray.getDimensions().stream()
                .map(d -> fmt(d, "[" + visit(d.getSize()) + "]"))
                .reduce("", this::reduce);
        String init = newArray.getInitializer() == null ? "" :
                fmt(newArray.getInitializer(), "{" + visit(newArray.getInitializer().getElements(), ",") + "}");

        return fmt(newArray, typeExpr + dimensions + init);
    }

    @Override
    public String visitNewClass(NewClass newClass) {
        String encl = newClass.getEncl() == null ? "" : visit(newClass.getEncl()) + ".";
        String nooh = newClass.getNooh() == null ? "" : fmt(newClass.getNooh(), "");
        String args = newClass.getArgs() == null ? "" :
                fmt(newClass.getArgs(), "(" + visit(newClass.getArgs().getArgs(), ",") + ")");
        return fmt(newClass, encl + nooh + "new" + visit(newClass.getClazz()) + args + visit(newClass.getBody()));
    }

    @Override
    public String visitPackage(J.Package pkg) {
        return fmt(pkg, "package" + visit(pkg.getExpr()) + visit(pkg.getBeforeSemicolon()));
    }

    @Override
    public String visitParameterizedType(ParameterizedType type) {
        return fmt(type, visit(type.getClazz()) + visit(type.getTypeParameters()));
    }

    @Override
    public String visitPrimitive(Primitive primitive) {
        String keyword;
        switch (primitive.getType()) {
            case Boolean:
                keyword = "boolean";
                break;
            case Byte:
                keyword = "byte";
                break;
            case Char:
                keyword = "char";
                break;
            case Double:
                keyword = "double";
                break;
            case Float:
                keyword = "float";
                break;
            case Int:
                keyword = "int";
                break;
            case Long:
                keyword = "long";
                break;
            case Short:
                keyword = "short";
                break;
            case Void:
                keyword = "void";
                break;
            case String:
                keyword = "String";
                break;
            case Wildcard:
                keyword = "*";
                break;
            case None:
                throw new IllegalStateException("Unable to print None primitive");
            case Null:
                throw new IllegalStateException("Unable to print Null primitive");
            default:
                throw new IllegalStateException("Unable to print non-primitive type");
        }

        return fmt(primitive, keyword);
    }

    @Override
    public <T extends J> String visitParentheses(Parentheses<T> parens) {
        return fmt(parens, "(" + visit(parens.getTree()) + ")");
    }

    @Override
    public String visitReturn(Return retrn) {
        return fmt(retrn, "return" + visit(retrn.getExpr()));
    }

    @Override
    public String visitSwitch(Switch switzh) {
        return fmt(switzh, "switch" + visit(switzh.getSelector()) + visit(switzh.getCases()));
    }

    @Override
    public String visitSynchronized(J.Synchronized synch) {
        return fmt(synch, "synchronized" + visit(synch.getLock()) + visit(synch.getBody()));
    }

    @Override
    public String visitTernary(Ternary ternary) {
        return fmt(ternary, visit(ternary.getCondition()) + "?" + visit(ternary.getTruePart()) + ":" +
                visit(ternary.getFalsePart()));
    }

    @Override
    public String visitThrow(Throw thrown) {
        return fmt(thrown, "throw" + visit(thrown.getException()));
    }

    @Override
    public String visitTry(Try tryable) {
        String resources = tryable.getResources() == null ? "" :
                fmt(tryable.getResources(), "(" + visit(tryable.getResources().getDecls(), ";") + ")");

        return fmt(tryable, "try" + resources + visit(tryable.getBody()) + visit(tryable.getCatches()) + visit(tryable.getFinally()));
    }

    @Override
    public String visitTypeCast(TypeCast typeCast) {
        return fmt(typeCast, visit(typeCast.getClazz()) + visit(typeCast.getExpr()));
    }

    @Override
    public String visitTypeParameters(TypeParameters typeParams) {
        return fmt(typeParams, "<" + visit(typeParams.getParams(), ",", "") + ">");
    }

    @Override
    public String visitTypeParameter(TypeParameter typeParam) {
        String bounds = typeParam.getBounds() == null ? "" :
                fmt(typeParam.getBounds(), "extends" + visit(typeParam.getBounds().getTypes(), "&"));
        return fmt(typeParam, visit(typeParam.getAnnotations(), "") + visit(typeParam.getName()) + bounds);
    }

    @Override
    public String visitUnary(Unary unary) {
        String code = "";
        if (unary.getOperator() instanceof J.Unary.Operator.PreIncrement) {
            code = "++" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.PreDecrement) {
            code = "--" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.PostIncrement) {
            code = visit(unary.getExpr()) + fmt(unary.getOperator(), "++");
        } else if (unary.getOperator() instanceof J.Unary.Operator.PostDecrement) {
            code = visit(unary.getExpr()) + fmt(unary.getOperator(), "--");
        } else if (unary.getOperator() instanceof J.Unary.Operator.Positive) {
            code = "+" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.Negative) {
            code = "-" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.Complement) {
            code = "~" + visit(unary.getExpr());
        } else if (unary.getOperator() instanceof J.Unary.Operator.Not) {
            code = "!" + visit(unary.getExpr());
        }

        return fmt(unary, code);
    }

    @Override
    public String visitUnparsedSource(UnparsedSource unparsed) {
        return fmt(unparsed, unparsed.getSource());
    }

    @Override
    public String visitVariable(VariableDecls.NamedVar variable) {
        return fmt(variable, visit(variable.getName()) +
                visit(variable.getAfterName()) +
                visitDims(variable.getDimensionsAfterName()) +
                (variable.getInitializer() == null ? "" : "=" + visit(variable.getInitializer())) +
                visit(variable.getBeforeComma()));
    }

    @Override
    public String visitWhileLoop(WhileLoop whileLoop) {
        return fmt(whileLoop, "while" + visit(whileLoop.getCondition()) + fmtStatement(whileLoop.getBody()));
    }

    @Override
    public String visitWildcard(Wildcard wildcard) {
        String bound = "";
        if (wildcard.getBound() instanceof Wildcard.Bound.Extends) {
            bound = fmt(wildcard.getBound(), "extends");
        } else if (wildcard.getBound() instanceof Wildcard.Bound.Super) {
            bound = fmt(wildcard.getBound(), "super");
        }
        return fmt(wildcard, "?" + bound + visit(wildcard.getBoundedType()));
    }
}
