/*
 * Copyright 2025 the original author or authors.
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
import { JavaVisitor } from "./visitor";
import { asRef, RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue } from "../rpc";
import {
    Annotation,
    AnnotatedType,
    ArrayAccess,
    ArrayDimension,
    ArrayType,
    Assert,
    Assignment,
    AssignmentOperation,
    Binary,
    Block,
    Break,
    Case,
    ClassDeclaration,
    ClassDeclarationKind,
    CompilationUnit,
    Continue,
    ControlParentheses,
    DeconstructionPattern,
    DoWhileLoop,
    Empty,
    EnumValue,
    EnumValueSet,
    Erroneous,
    Expression,
    FieldAccess,
    ForEachLoop,
    ForEachLoopControl,
    ForLoop,
    ForLoopControl,
    Identifier,
    IdentifierWithAnnotations,
    If,
    IfElse,
    Import,
    InstanceOf,
    IntersectionType,
    isJava,
    isSpace,
    J,
    JavaKind,
    JLeftPadded,
    JRightPadded,
    JContainer,
    Label,
    Lambda,
    LambdaParameters,
    Literal,
    MemberReference,
    MethodDeclaration,
    MethodInvocation,
    Modifier,
    MultiCatch,
    NewArray,
    NewClass,
    NullableType,
    Package,
    ParameterizedType,
    Parentheses,
    ParenthesizedTypeTree,
    Primitive,
    Return,
    Space,
    Switch,
    SwitchExpression,
    Synchronized,
    Ternary,
    Throw,
    Try,
    TryResource,
    TryCatch,
    TypeCast,
    TypeParameter,
    TypeParameters,
    Unary,
    Unknown,
    UnknownSource,
    VariableDeclarations,
    Variable,
    WhileLoop,
    Wildcard,
    Yield,
} from "./tree";
import { produceAsync } from "../visitor";
import { createDraft, Draft, finishDraft } from "immer";

class JavaSender extends JavaVisitor<RpcSendQueue> {

    protected async preVisit(j: J, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => asRef(j2.prefix), space => this.visitSpace(space, q));
        await q.sendMarkers(j, j2 => j2.markers);

        return j;
    }

    protected async visitAnnotatedType(annotatedType: AnnotatedType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(annotatedType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(annotatedType, a => a.typeExpression, type => this.visit(type, q));
        await q.getAndSend(annotatedType, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return annotatedType;
    }

    protected async visitAnnotation(annotation: Annotation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(annotation, a => a.annotationType, type => this.visit(type, q));
        await q.getAndSend(annotation, a => a.arguments, args => this.visitOptionalContainer(args, q));
        await q.getAndSend(annotation, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return annotation;
    }

    protected async visitArrayAccess(arrayAccess: ArrayAccess, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(arrayAccess, a => a.indexed, indexed => this.visit(indexed, q));
        await q.getAndSend(arrayAccess, a => a.dimension, dim => this.visit(dim, q));
        await q.getAndSend(arrayAccess, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return arrayAccess;
    }

    protected async visitArrayDimension(dimension: ArrayDimension, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(dimension, d => d.index, idx => this.visitRightPadded(idx, q));

        return dimension;
    }

    protected async visitArrayType(arrayType: ArrayType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(arrayType, a => a.elementType, type => this.visit(type, q));
        await q.getAndSendList(arrayType, a => a.annotations || [], annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(arrayType, a => a.dimension, dim => this.visitOptionalLeftPadded(dim, q));
        await q.getAndSend(arrayType, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return arrayType;
    }

    protected async visitAssert(assert: Assert, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assert, a => a.condition, cond => this.visit(cond, q));
        await q.getAndSend(assert, a => a.detail, detail => this.visitOptionalLeftPadded(detail, q));

        return assert;
    }

    protected async visitAssignment(assignment: Assignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignment, a => a.variable, variable => this.visit(variable, q));
        await q.getAndSend(assignment, a => a.assignment, assign => this.visitLeftPadded(assign, q));
        await q.getAndSend(assignment, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return assignment;
    }

    protected async visitAssignmentOperation(assignOp: AssignmentOperation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignOp, a => a.variable, variable => this.visit(variable, q));
        await q.getAndSend(assignOp, a => a.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(assignOp, a => a.assignment, assign => this.visit(assign, q));
        await q.getAndSend(assignOp, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return assignOp;
    }

    protected async visitBinary(binary: Binary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(binary, b => b.left, left => this.visit(left, q));
        await q.getAndSend(binary, b => b.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(binary, b => b.right, right => this.visit(right, q));
        await q.getAndSend(binary, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return binary;
    }

    protected async visitBreak(breakStmt: Break, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(breakStmt, b => b.label, label => this.visit(label, q));

        return breakStmt;
    }

    protected async visitCase(caseStmt: Case, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(caseStmt, c => c.caseType);
        await q.getAndSend(caseStmt, c => c.caseLabels, labels => this.visitContainer(labels, q));
        await q.getAndSend(caseStmt, c => c.statements, stmts => this.visitContainer(stmts, q));
        await q.getAndSend(caseStmt, c => c.body, body => this.visitRightPadded(body, q));
        await q.getAndSend(caseStmt, c => c.guard, guard => this.visit(guard, q));

        return caseStmt;
    }

    protected async visitContinue(continueStmt: Continue, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(continueStmt, c => c.label, label => this.visit(label, q));

        return continueStmt;
    }

    protected async visitControlParentheses<T extends J>(controlParens: ControlParentheses<T>, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(controlParens, c => c.tree, tree => this.visitRightPadded(tree, q));

        return controlParens;
    }

    protected async visitDeconstructionPattern(pattern: DeconstructionPattern, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(pattern, p => p.deconstructor, deconstructor => this.visit(deconstructor, q));
        await q.getAndSend(pattern, p => p.nested, nested => this.visitContainer(nested, q));
        await q.getAndSend(pattern, p => p.type && asRef(p.type), type => this.visitType(type, q));
        return pattern;
    }

    protected async visitDoWhileLoop(doWhile: DoWhileLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(doWhile, d => d.body, body => this.visitOptionalRightPadded(body, q));
        await q.getAndSend(doWhile, d => d.whileCondition, cond => this.visit(cond, q));

        return doWhile;
    }

    protected async visitEmpty(empty: Empty, q: RpcSendQueue): Promise<J | undefined> {
        // No additional properties to send
        return empty;
    }

    protected async visitEnumValueSet(enumValueSet: EnumValueSet, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(enumValueSet, e => e.enums, enumValue => enumValue.element.id, enumValue => this.visitRightPadded(enumValue, q));
        await q.getAndSend(enumValueSet, e => e.terminatedWithSemicolon);

        return enumValueSet;
    }

    protected async visitEnumValue(enumValue: EnumValue, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(enumValue, e => e.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(enumValue, e => e.name, name => this.visit(name, q));
        await q.getAndSend(enumValue, e => e.initializer, init => this.visit(init, q));

        return enumValue;
    }

    protected async visitErroneous(erroneous: Erroneous, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(erroneous, e => e.text);

        return erroneous;
    }

    protected async visitFieldAccess(fieldAccess: FieldAccess, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(fieldAccess, f => f.target, target => this.visit(target, q));
        await q.getAndSend(fieldAccess, f => f.name, name => this.visitLeftPadded(name, q));
        await q.getAndSend(fieldAccess, a => a.type && asRef(a.type), type => this.visitType(type, q));

        return fieldAccess;
    }

    protected async visitForEachLoop(forEach: ForEachLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forEach, f => f.control, control => this.visit(control, q));
        await q.getAndSend(forEach, f => f.body, body => this.visitRightPadded(body, q));

        return forEach;
    }

    protected async visitForEachLoopControl(control: ForEachLoopControl, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(control, c => c.variable, variable => this.visitRightPadded(variable, q));
        await q.getAndSend(control, c => c.iterable, iterable => this.visitRightPadded(iterable, q));

        return control;
    }

    protected async visitForLoop(forLoop: ForLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forLoop, f => f.control, control => this.visit(control, q));
        await q.getAndSend(forLoop, f => f.body, body => this.visitRightPadded(body, q));

        return forLoop;
    }

    protected async visitForLoopControl(control: ForLoopControl, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(control, c => c.init, init => this.visitRightPadded(init, q));
        await q.getAndSend(control, c => c.condition, cond => this.visitRightPadded(cond, q));
        await q.getAndSendList(control, c => c.update, update => this.visitRightPadded(update, q));

        return control;
    }

    protected async visitIf(ifStmt: If, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ifStmt, i => i.ifCondition, cond => this.visit(cond, q));
        await q.getAndSend(ifStmt, i => i.thenPart, then => this.visitRightPadded(then, q));
        await q.getAndSend(ifStmt, i => i.elsePart, elsePart => this.visit(elsePart, q));

        return ifStmt;
    }

    protected async visitIfElse(ifElse: IfElse, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ifElse, e => e.body, body => this.visitRightPadded(body, q));

        return ifElse;
    }

    protected async visitImport(importStmt: Import, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importStmt, i => i.static, static_ => this.visitLeftPadded(static_, q));
        await q.getAndSend(importStmt, i => i.qualid, qualid => this.visit(qualid, q));
        await q.getAndSend(importStmt, i => i.alias, alias => this.visitLeftPadded(alias, q));

        return importStmt;
    }

    protected async visitInstanceOf(instanceOf: InstanceOf, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(instanceOf, i => i.expression, expr => this.visitRightPadded(expr, q));
        await q.getAndSend(instanceOf, i => i.clazz, clazz => this.visit(clazz, q));
        await q.getAndSend(instanceOf, i => i.pattern, pattern => this.visit(pattern, q));
        await q.getAndSend(instanceOf, i => i.type && asRef(i.type), type => this.visitType(type, q));
        await q.getAndSend(instanceOf, i => i.modifier, modifier => this.visit(modifier, q));

        return instanceOf;
    }

    protected async visitIntersectionType(intersectionType: IntersectionType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(intersectionType, i => i.bounds, bounds => this.visitContainer(bounds, q));
        await q.getAndSend(intersectionType, i => i.type && asRef(i.type), type => this.visitType(type, q));

        return intersectionType;
    }

    protected async visitLabel(label: Label, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(label, l => l.label, id => this.visitRightPadded(id, q));
        await q.getAndSend(label, l => l.statement, stmt => this.visit(stmt, q));

        return label;
    }

    protected async visitLambda(lambda: Lambda, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(lambda, l => l.parameters, params => this.visit(params, q));
        await q.getAndSend(lambda, l => l.arrow, arrow => this.visitSpace(arrow, q));
        await q.getAndSend(lambda, l => l.body, body => this.visit(body, q));
        await q.getAndSend(lambda, l => l.type && asRef(l.type), type => this.visitType(type, q));

        return lambda;
    }

    protected async visitLambdaParameters(params: LambdaParameters, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(params, p => p.parenthesized);
        await q.getAndSendList(params, p => p.parameters, param => param.element.id, param => this.visitRightPadded(param, q));

        return params;
    }

    protected async visitLiteral(literal: Literal, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(literal, l => l.value);
        await q.getAndSend(literal, l => l.valueSource);
        await q.getAndSend(literal, l => l.type && asRef(l.type), type => this.visitType(type, q));

        return literal;
    }

    protected async visitMemberReference(memberRef: MemberReference, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(memberRef, m => m.containing, cont => this.visitRightPadded(cont, q));
        await q.getAndSend(memberRef, m => m.reference, ref => this.visitLeftPadded(ref, q));
        await q.getAndSend(memberRef, m => m.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(memberRef, m => m.type && asRef(m.type), type => this.visitType(type, q));

        return memberRef;
    }

    protected async visitMethodInvocation(invocation: MethodInvocation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(invocation, m => m.select, select => this.visitRightPadded(select, q));
        await q.getAndSend(invocation, m => m.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(invocation, m => m.name, name => this.visit(name, q));
        await q.getAndSend(invocation, m => m.arguments, args => this.visitContainer(args, q));
        await q.getAndSend(invocation, m => m.methodType && asRef(m.methodType), type => this.visitType(type, q));
        await q.getAndSend(invocation, m => m.type && asRef(m.type), type => this.visitType(type, q));

        return invocation;
    }

    protected async visitModifier(modifier: Modifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(modifier, m => m.type);

        return modifier;
    }

    protected async visitMultiCatch(multiCatch: MultiCatch, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(multiCatch, m => m.alternatives, alt => alt.element.id, alt => this.visitRightPadded(alt, q));
        await q.getAndSend(multiCatch, m => m.type && asRef(m.type), type => this.visitType(type, q));

        return multiCatch;
    }

    protected async visitNewArray(newArray: NewArray, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(newArray, n => n.typeExpression, type => this.visit(type, q));
        await q.getAndSendList(newArray, n => n.dimensions, dim => dim.id, dim => this.visit(dim, q));
        await q.getAndSend(newArray, n => n.initializer, init => this.visitContainer(init, q));
        await q.getAndSend(newArray, n => n.type && asRef(n.type), type => this.visitType(type, q));

        return newArray;
    }

    protected async visitNewClass(newClass: NewClass, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(newClass, n => n.enclosing, encl => this.visitRightPadded(encl, q));
        await q.getAndSend(newClass, n => n.clazz, clazz => this.visit(clazz, q));
        await q.getAndSend(newClass, n => n.arguments, args => this.visitContainer(args, q));
        await q.getAndSend(newClass, n => n.body, body => this.visit(body, q));
        await q.getAndSend(newClass, n => n.type && asRef(n.type), type => this.visitType(type, q));

        return newClass;
    }

    protected async visitNullableType(nullableType: NullableType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(nullableType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(nullableType, n => n.typeTree, type => this.visitRightPadded(type, q));
        await q.getAndSend(nullableType, n => n.type && asRef(n.type), type => this.visitType(type, q));

        return nullableType;
    }

    protected async visitParameterizedType(paramType: ParameterizedType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(paramType, p => p.clazz, clazz => this.visit(clazz, q));
        await q.getAndSend(paramType, p => p.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(paramType, p => p.type && asRef(p.type), type => this.visitType(type, q));

        return paramType;
    }

    protected async visitParentheses<T extends J>(parentheses: Parentheses<T>, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(parentheses, p => p.tree, tree => this.visitRightPadded(tree, q));
        await q.getAndSend(parentheses, p => p.type && asRef(p.type), type => this.visitType(type, q));

        return parentheses;
    }

    protected async visitParenthesizedTypeTree(parenthesizedType: ParenthesizedTypeTree, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(parenthesizedType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(parenthesizedType, p => p.parenthesizedType, tree => this.visit(tree, q));
        await q.getAndSend(parenthesizedType, p => p.type && asRef(p.type), type => this.visitType(type, q));

        return parenthesizedType;
    }

    protected async visitPrimitive(primitive: Primitive, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(primitive, p => p.type && asRef(p.type), type => this.visitType(type, q));

        return primitive;
    }

    protected async visitReturn(returnStmt: Return, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(returnStmt, r => r.expression, expr => this.visit(expr, q));

        return returnStmt;
    }

    protected async visitSwitch(switchStmt: Switch, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(switchStmt, s => s.selector, sel => this.visit(sel, q));
        await q.getAndSend(switchStmt, s => s.cases, block => this.visit(block, q));

        return switchStmt;
    }

    protected async visitSwitchExpression(switchExpr: SwitchExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(switchExpr, s => s.selector, sel => this.visit(sel, q));
        await q.getAndSend(switchExpr, s => s.cases, block => this.visit(block, q));
        await q.getAndSend(switchExpr, s => s.type && asRef(s.type), type => this.visitType(type, q));

        return switchExpr;
    }

    protected async visitSynchronized(syncStmt: Synchronized, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(syncStmt, s => s.lock, lock => this.visit(lock, q));
        await q.getAndSend(syncStmt, s => s.body, body => this.visit(body, q));

        return syncStmt;
    }

    protected async visitTernary(ternary: Ternary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ternary, t => t.condition, cond => this.visit(cond, q));
        await q.getAndSend(ternary, t => t.truePart, truePart => this.visitLeftPadded(truePart, q));
        await q.getAndSend(ternary, t => t.falsePart, falsePart => this.visitLeftPadded(falsePart, q));
        await q.getAndSend(ternary, t => t.type && asRef(t.type), type => this.visitType(type, q));

        return ternary;
    }

    protected async visitThrow(throwStmt: Throw, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(throwStmt, t => t.exception, exc => this.visit(exc, q));

        return throwStmt;
    }

    protected async visitTry(tryStmt: Try, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(tryStmt, t => t.resources, res => this.visitContainer(res, q));
        await q.getAndSend(tryStmt, t => t.body, body => this.visit(body, q));
        await q.getAndSendList(tryStmt, t => t.catches, catch_ => catch_.id, catch_ => this.visit(catch_, q));
        await q.getAndSend(tryStmt, t => t.finally, fin => this.visitLeftPadded(fin, q));

        return tryStmt;
    }

    protected async visitTryResource(resource: TryResource, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(resource, r => r.variableDeclarations, variable => this.visit(variable, q));
        await q.getAndSend(resource, r => r.terminatedWithSemicolon);

        return resource;
    }

    protected async visitTryCatch(catch_: TryCatch, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(catch_, c => c.parameter, param => this.visit(param, q));
        await q.getAndSend(catch_, c => c.body, body => this.visit(body, q));

        return catch_;
    }

    protected async visitTypeCast(typeCast: TypeCast, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeCast, t => t.clazz, clazz => this.visit(clazz, q));
        await q.getAndSend(typeCast, t => t.expression, expr => this.visit(expr, q));
        await q.getAndSend(typeCast, t => t.type && asRef(t.type), type => this.visitType(type, q));

        return typeCast;
    }

    protected async visitTypeParameter(typeParam: TypeParameter, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(typeParam, t => t.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(typeParam, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(typeParam, a => a.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(typeParam, t => t.name, name => this.visit(name, q));
        await q.getAndSend(typeParam, t => t.bounds, bounds => this.visitContainer(bounds, q));

        return typeParam;
    }

    protected async visitTypeParameters(typeParams: TypeParameters, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(typeParams, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(typeParams, t => t.typeParameters, p => p.element.id, params => this.visitRightPadded(params, q));

        return typeParams;
    }

    protected async visitUnary(unary: Unary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(unary, u => u.operator);
        await q.getAndSend(unary, u => u.expression, expr => this.visit(expr, q));
        await q.getAndSend(unary, u => u.type && asRef(u.type), type => this.visitType(type, q));

        return unary;
    }

    protected async visitVariable(variable: Variable, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(variable, v => v.name, name => this.visit(name, q));
        await q.getAndSendList(variable, v => v.dimensionsAfterName, d => d.element, dims => this.visitLeftPadded(dims, q));
        await q.getAndSend(variable, v => v.initializer, init => this.visitLeftPadded(init, q));
        await q.getAndSend(variable, v => v.variableType && asRef(v.variableType), type => this.visitType(type, q));

        return variable;
    }

    protected async visitWhileLoop(whileLoop: WhileLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(whileLoop, w => w.condition, cond => this.visit(cond, q));
        await q.getAndSend(whileLoop, w => w.body, body => this.visitRightPadded(body, q));

        return whileLoop;
    }

    protected async visitWildcard(wildcard: Wildcard, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(wildcard, w => w.bound);
        await q.getAndSend(wildcard, w => w.boundedType, type => this.visit(type, q));
        await q.getAndSend(wildcard, w => w.type && asRef(w.type), type => this.visitType(type, q));

        return wildcard;
    }

    protected async visitYield(yieldExpr: Yield, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(yieldExpr, y => y.implicit);
        await q.getAndSend(yieldExpr, y => y.value, value => this.visit(value, q));

        return yieldExpr;
    }

    protected async visitUnknown(unknown: Unknown, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(unknown, u => u.source, source => this.visit(source, q));

        return unknown;
    }

    protected async visitUnknownSource(source: UnknownSource, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(source, s => s.text);

        return source;
    }

    protected async visitCompilationUnit(cu: CompilationUnit, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(cu, c => c.sourcePath);
        await q.getAndSend(cu, c => c.charsetName);
        await q.getAndSend(cu, c => c.charsetBomMarked);
        await q.getAndSend(cu, c => c.checksum);
        await q.getAndSend(cu, c => c.fileAttributes);
        await q.getAndSend(cu, c => c.packageDeclaration, pkg => this.visitRightPadded(pkg, q));
        await q.getAndSendList(cu, c => c.imports, imp => imp.element.id, imp => this.visitRightPadded(imp, q));
        await q.getAndSendList(cu, c => c.classes, cls => cls.id, cls => this.visit(cls, q));
        await q.getAndSend(cu, c => asRef(c.eof), space => this.visitSpace(space, q));

        return cu;
    }

    protected async visitPackage(pkg: Package, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(pkg, p => p.expression, expr => this.visit(expr, q));
        await q.getAndSendList(pkg, p => p.annotations, annot => annot.id, annot => this.visit(annot, q));

        return pkg;
    }

    protected async visitClassDeclaration(cls: ClassDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(cls, c => c.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(cls, c => c.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(cls, c => c.classKind, kind => this.visit(kind, q));
        await q.getAndSend(cls, c => c.name, name => this.visit(name, q));
        await q.getAndSend(cls, c => c.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(cls, c => c.primaryConstructor, cons => this.visitContainer(cons, q));
        await q.getAndSend(cls, c => c.extends, ext => this.visitLeftPadded(ext, q));
        await q.getAndSend(cls, c => c.implements, impl => this.visitContainer(impl, q));
        await q.getAndSend(cls, c => c.permitting, perm => this.visitContainer(perm, q));
        await q.getAndSend(cls, c => c.body, body => this.visit(body, q));

        return cls;
    }

    protected async visitClassDeclarationKind(kind: ClassDeclarationKind, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(kind, k => k.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(kind, k => k.type);

        return kind;
    }

    protected async visitBlock(block: Block, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(block, b => b.static, s => this.visitRightPadded(s, q));
        await q.getAndSendList(block, b => b.statements, stmt => stmt.element.id, stmt => this.visitRightPadded(stmt, q));
        await q.getAndSend(block, b => asRef(b.end), space => this.visitSpace(space, q));

        return block;
    }

    protected async visitMethodDeclaration(method: MethodDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(method, m => m.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(method, m => m.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(method, m => m.typeParameters, params => this.visit(params, q));
        await q.getAndSend(method, m => m.returnTypeExpression, type => this.visit(type, q));
        await q.getAndSend(method, m => m.name, name => this.visit(name, q));
        await q.getAndSend(method, m => m.parameters, params => this.visitContainer(params, q));
        await q.getAndSend(method, m => m.throws, throws => this.visitContainer(throws, q));
        await q.getAndSend(method, m => m.body, body => this.visit(body, q));
        await q.getAndSend(method, m => m.defaultValue, def => this.visitLeftPadded(def, q));

        return method;
    }

    protected async visitIdentifierWithAnnotations(identWithAnnot: IdentifierWithAnnotations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(identWithAnnot, i => i.identifier, identifier => this.visit(identifier, q));
        await q.getAndSendList(identWithAnnot, i => i.annotations, annot => annot.id, annot => this.visit(annot, q));

        return identWithAnnot;
    }

    protected async visitVariableDeclarations(varDecls: VariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(varDecls, v => v.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(varDecls, v => v.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(varDecls, v => v.typeExpression, type => this.visit(type, q));
        await q.getAndSend(varDecls, v => v.varargs && asRef(v.varargs), space => this.visitSpace(space, q));
        await q.getAndSendList(varDecls, v => v.variables, variable => variable.element.id, variable => this.visitRightPadded(variable, q));

        return varDecls;
    }

    protected async visitIdentifier(ident: Identifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(ident, id => id.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(ident, id => id.simpleName);

        return ident;
    }

    protected async visitSpace(space: Space, q: RpcSendQueue): Promise<Space> {
        await q.getAndSendList(space, s => s.comments, c => c.text + c.suffix, async c => {
            await q.getAndSend(c, c2 => c2.multiline);
            await q.getAndSend(c, c2 => c2.text);
            await q.getAndSend(c, c2 => c2.suffix);
            await q.sendMarkers(c, c2 => c2.markers);
        });
        await q.getAndSend(space, s => s.whitespace);
        return space;
    }

    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T>, q: RpcSendQueue): Promise<JLeftPadded<T>> {
        if (isJava(left.element)) {
            await q.getAndSend(left, l => l.element, elem => this.visit(elem as J, q));
        } else if (isSpace(left.element)) {
            await q.getAndSend(left, l => asRef(l.before), space => this.visitSpace(space, q));
        } else {
            await q.getAndSend(left, l => l.element);
        }

        await q.getAndSend(left, l => asRef(l.before), space => this.visitSpace(space, q));
        await q.sendMarkers(left, l => l.markers);

        return left;
    }

    protected async visitRightPadded<T extends J | boolean>(right: JRightPadded<T>, q: RpcSendQueue): Promise<JRightPadded<T>> {
        await q.getAndSend(right, r => r.element,
            async elem => typeof elem === 'object' && 'kind' in elem
                ? await this.visit(elem as J, q)
                : elem);

        await q.getAndSend(right, r => asRef(r.after), space => this.visitSpace(space, q));
        await q.sendMarkers(right, r => r.markers);

        return right;
    }

    protected async visitContainer<T extends J>(container: JContainer<T>, q: RpcSendQueue): Promise<JContainer<T>> {
        await q.getAndSend(container, c => asRef(c.before), space => this.visitSpace(space, q));
        await q.getAndSendList(container, c => c.elements, elem => elem.element.id, elem => this.visitRightPadded(elem, q));
        await q.sendMarkers(container, c => c.markers);

        return container;
    }
}

class JavaReceiver extends JavaVisitor<RpcReceiveQueue> {

    protected async preVisit(j: J, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(j);

        draft.id = await q.receive(j.id);
        draft.prefix = await q.receive(j.prefix, space => this.visitSpace(space, q));
        draft.markers = await q.receiveMarkers(j.markers);

        return finishDraft(draft);
    }

    protected async visitCompilationUnit(cu: CompilationUnit, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(cu);

        draft.sourcePath = await q.receive(cu.sourcePath);
        draft.charsetName = await q.receive(cu.charsetName);
        draft.charsetBomMarked = await q.receive(cu.charsetBomMarked);
        draft.checksum = await q.receive(cu.checksum);
        draft.fileAttributes = await q.receive(cu.fileAttributes);
        draft.packageDeclaration = await q.receive(cu.packageDeclaration, pkg => this.visitRightPadded(pkg, q));
        draft.imports = await q.receiveListDefined(cu.imports, imp => this.visitRightPadded(imp, q));
        draft.classes = await q.receiveListDefined(cu.classes, cls => this.visit(cls, q));
        draft.eof = await q.receive(cu.eof, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    protected async visitPackage(pkg: Package, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(pkg);

        draft.expression = await q.receive<Expression>(pkg.expression, expr => this.visit(expr, q));
        draft.annotations = await q.receiveListDefined(pkg.annotations, annot => this.visit(annot, q));

        return finishDraft(draft);
    }

    protected async visitClassDeclaration(cls: ClassDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(cls);

        draft.leadingAnnotations = await q.receiveListDefined(cls.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(cls.modifiers, mod => this.visit(mod, q));
        draft.classKind = await q.receive(cls.classKind, kind => this.visit(kind, q));
        draft.name = await q.receive(cls.name, name => this.visit(name, q));
        draft.typeParameters = await q.receive(cls.typeParameters, params => this.visitContainer(params, q));
        draft.primaryConstructor = await q.receive(cls.primaryConstructor, cons => this.visitContainer(cons, q));
        draft.extends = await q.receive(cls.extends, ext => this.visitLeftPadded(ext, q));
        draft.implements = await q.receive(cls.implements, impl => this.visitContainer(impl, q));
        draft.permitting = await q.receive(cls.permitting, perm => this.visitContainer(perm, q));
        draft.body = await q.receive(cls.body, body => this.visit(body, q));

        return finishDraft(draft);
    }

    protected async visitBlock(block: Block, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(block);

        draft.static = await q.receive(block.static, s => this.visitRightPadded(s, q));
        draft.statements = await q.receiveListDefined(block.statements, stmt => this.visitRightPadded(stmt, q));
        draft.end = await q.receive(block.end, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    protected async visitMethodDeclaration(method: MethodDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(method);

        draft.leadingAnnotations = await q.receiveListDefined(method.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(method.modifiers, mod => this.visit(mod, q));
        draft.typeParameters = await q.receive(method.typeParameters, params => this.visit(params, q));
        draft.returnTypeExpression = await q.receive(method.returnTypeExpression, type => this.visit(type, q));
        draft.name = await q.receive(method.name, name => this.visit(name, q));
        draft.parameters = await q.receive(method.parameters, params => this.visitContainer(params, q));
        draft.throws = await q.receive(method.throws, throws => this.visitContainer(throws, q));
        draft.body = await q.receive(method.body, body => this.visit(body, q));
        draft.defaultValue = await q.receive(method.defaultValue, def => this.visitLeftPadded(def, q));

        return finishDraft(draft);
    }

    protected async visitVariableDeclarations(varDecls: VariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(varDecls);

        draft.leadingAnnotations = await q.receiveListDefined(varDecls.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(varDecls.modifiers, mod => this.visit(mod, q));
        draft.typeExpression = await q.receive(varDecls.typeExpression, type => this.visit(type, q));
        draft.varargs = await q.receive(varDecls.varargs, space => this.visitSpace(space, q));
        draft.variables = await q.receiveListDefined(varDecls.variables, variable => this.visitRightPadded(variable, q));

        return finishDraft(draft);
    }

    protected async visitIdentifier(ident: Identifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(ident);

        draft.annotations = await q.receiveListDefined(ident.annotations, annot => this.visit(annot, q));
        draft.simpleName = await q.receive(ident.simpleName);

        return finishDraft(draft);
    }

    protected async visitSpace(space: Space, q: RpcReceiveQueue): Promise<Space> {
        return produceAsync<Space>(space, async draft => {
            draft.comments = await q.receiveListDefined(space.comments, async c => {
                return await produceAsync(c, async draft => {
                    draft.multiline = await q.receive(c.multiline);
                    draft.text = await q.receive(c.text);
                    draft.suffix = await q.receive(c.suffix);
                    draft.markers = await q.receiveMarkers(c.markers);
                });
            });
            draft.whitespace = await q.receive(space.whitespace);
        });
    }

    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T>, q: RpcReceiveQueue): Promise<JLeftPadded<T>> {
        if (!left) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty left padding");
        }

        return produceAsync<JLeftPadded<T>>(left, async draft => {
            // Handle different element types
            if (isJava(left.element)) {
                draft.element = await q.receive(left.element, elem => this.visit(elem, q)) as Draft<T>;
            } else if (isSpace(left.element)) {
                draft.element = await q.receive<Space>(left.element, space => this.visitSpace(space, q)) as Draft<T>;
            } else {
                draft.element = await q.receive(left.element) as Draft<T>;
            }

            draft.before = await q.receive(left.before, space => this.visitSpace(space, q));
            draft.markers = await q.receiveMarkers(left.markers);
        });
    }

    protected async visitRightPadded<T extends J | boolean>(right: JRightPadded<T>, q: RpcReceiveQueue): Promise<JRightPadded<T>> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty right padding");
        }

        return produceAsync<JRightPadded<T>>(right, async draft => {
            // Handle different element types
            if (isJava(right.element)) {
                draft.element = await q.receive(right.element, elem => this.visit(elem as J, q)) as Draft<T>;
            } else {
                draft.element = await q.receive(right.element) as Draft<T>;
            }

            draft.after = await q.receive(right.after, space => this.visitSpace(space, q));
            draft.markers = await q.receiveMarkers(right.markers);
        });
    }

    protected async visitContainer<T extends J>(container: JContainer<T>, q: RpcReceiveQueue): Promise<JContainer<T>> {
        return produceAsync<JContainer<T>>(container, async draft => {
            draft.before = await q.receive(container.before, space => this.visitSpace(space, q));
            draft.elements = await q.receiveListDefined(container.elements, elem => this.visitRightPadded(elem, q)) as Draft<JRightPadded<T>[]>;
            draft.markers = await q.receiveMarkers(container.markers);
        });
    }
}

const javaCodec: RpcCodec<J> = {
    async rpcReceive(before: J, q: RpcReceiveQueue): Promise<J> {
        return (await new JavaReceiver().visit(before, q))!;
    },

    async rpcSend(after: J, q: RpcSendQueue): Promise<void> {
        await new JavaSender().visit(after, q);
    }
}

// Register codec for all Java AST node types
Object.values(JavaKind).forEach(kind => {
    RpcCodecs.registerCodec(kind, javaCodec);
});
