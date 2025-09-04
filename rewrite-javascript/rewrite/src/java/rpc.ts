/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {JavaVisitor} from "./visitor";
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {Expression, isSpace, J, TextComment} from "./tree";
import {produceAsync} from "../visitor";
import {createDraft, Draft, finishDraft, WritableDraft} from "immer";
import {isTree} from "../tree";
import {JavaType} from "./type";

export class JavaSender extends JavaVisitor<RpcSendQueue> {

    protected async preVisit(j: J, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => j2.prefix, space => this.visitSpace(space, q));
        await q.getAndSend(j, j2 => j2.markers);
        return j;
    }

    protected async visitAnnotatedType(annotatedType: J.AnnotatedType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(annotatedType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(annotatedType, a => a.typeExpression, type => this.visit(type, q));
        return annotatedType;
    }

    protected async visitAnnotation(annotation: J.Annotation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(annotation, a => a.annotationType, type => this.visit(type, q));
        await q.getAndSend(annotation, a => a.arguments, args => this.visitContainer(args, q));
        return annotation;
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(arrayAccess, a => a.indexed, indexed => this.visit(indexed, q));
        await q.getAndSend(arrayAccess, a => a.dimension, dim => this.visit(dim, q));
        return arrayAccess;
    }

    protected async visitArrayDimension(dimension: J.ArrayDimension, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(dimension, d => d.index, idx => this.visitRightPadded(idx, q));
        return dimension;
    }

    protected async visitArrayType(arrayType: J.ArrayType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(arrayType, a => a.elementType, type => this.visit(type, q));
        await q.getAndSendList(arrayType, a => a.annotations || [], annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(arrayType, a => a.dimension, d => this.visitLeftPadded(d, q));
        await q.getAndSend(arrayType, a => asRef(a.type), type => this.visitType(type, q));
        return arrayType;
    }

    protected async visitAssert(assert: J.Assert, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assert, a => a.condition, cond => this.visit(cond, q));
        await q.getAndSend(assert, a => a.detail, detail => this.visitLeftPadded(detail, q));
        return assert;
    }

    protected async visitAssignment(assignment: J.Assignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignment, a => a.variable, variable => this.visit(variable, q));
        await q.getAndSend(assignment, a => a.assignment, assign => this.visitLeftPadded(assign, q));
        await q.getAndSend(assignment, a => asRef(a.type), type => this.visitType(type, q));
        return assignment;
    }

    protected async visitAssignmentOperation(assignOp: J.AssignmentOperation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignOp, a => a.variable, variable => this.visit(variable, q));
        await q.getAndSend(assignOp, a => a.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(assignOp, a => a.assignment, assign => this.visit(assign, q));
        await q.getAndSend(assignOp, a => asRef(a.type), type => this.visitType(type, q));
        return assignOp;
    }

    protected async visitBinary(binary: J.Binary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(binary, b => b.left, left => this.visit(left, q));
        await q.getAndSend(binary, b => b.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(binary, b => b.right, right => this.visit(right, q));
        await q.getAndSend(binary, a => asRef(a.type), type => this.visitType(type, q));
        return binary;
    }

    protected async visitBreak(breakStmt: J.Break, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(breakStmt, b => b.label, label => this.visit(label, q));
        return breakStmt;
    }

    protected async visitCase(caseStmt: J.Case, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(caseStmt, c => c.type);
        await q.getAndSend(caseStmt, c => c.caseLabels, labels => this.visitContainer(labels, q));
        await q.getAndSend(caseStmt, c => c.statements, stmts => this.visitContainer(stmts, q));
        await q.getAndSend(caseStmt, c => c.body, body => this.visitRightPadded(body, q));
        await q.getAndSend(caseStmt, c => c.guard, guard => this.visit(guard, q));
        return caseStmt;
    }

    protected async visitContinue(continueStmt: J.Continue, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(continueStmt, c => c.label, label => this.visit(label, q));
        return continueStmt;
    }

    protected async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(controlParens, c => c.tree, tree => this.visitRightPadded(tree, q));
        return controlParens;
    }

    protected async visitDeconstructionPattern(pattern: J.DeconstructionPattern, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(pattern, p => p.deconstructor, deconstructor => this.visit(deconstructor, q));
        await q.getAndSend(pattern, p => p.nested, nested => this.visitContainer(nested, q));
        await q.getAndSend(pattern, p => asRef(p.type), type => this.visitType(type, q));
        return pattern;
    }

    protected async visitDoWhileLoop(doWhile: J.DoWhileLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(doWhile, d => d.body, body => this.visitRightPadded(body, q));
        await q.getAndSend(doWhile, d => d.whileCondition, cond => this.visitLeftPadded(cond, q));
        return doWhile;
    }

    protected async visitEmpty(empty: J.Empty, _q: RpcSendQueue): Promise<J | undefined> {
        // No additional properties to send
        return empty;
    }

    protected async visitEnumValueSet(enumValueSet: J.EnumValueSet, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(enumValueSet, e => e.enums, enumValue => enumValue.element.id, enumValue => this.visitRightPadded(enumValue, q));
        await q.getAndSend(enumValueSet, e => e.terminatedWithSemicolon);
        return enumValueSet;
    }

    protected async visitEnumValue(enumValue: J.EnumValue, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(enumValue, e => e.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(enumValue, e => e.name, name => this.visit(name, q));
        await q.getAndSend(enumValue, e => e.initializer, init => this.visit(init, q));
        return enumValue;
    }

    protected async visitErroneous(erroneous: J.Erroneous, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(erroneous, e => e.text);
        return erroneous;
    }

    protected async visitFieldAccess(fieldAccess: J.FieldAccess, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(fieldAccess, f => f.target, target => this.visit(target, q));
        await q.getAndSend(fieldAccess, f => f.name, name => this.visitLeftPadded(name, q));
        await q.getAndSend(fieldAccess, a => asRef(a.type), type => this.visitType(type, q));
        return fieldAccess;
    }

    protected async visitForEachLoop(forEach: J.ForEachLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forEach, f => f.control, control => this.visit(control, q));
        await q.getAndSend(forEach, f => f.body, body => this.visitRightPadded(body, q));
        return forEach;
    }

    protected async visitForEachLoopControl(control: J.ForEachLoop.Control, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(control, c => c.variable, variable => this.visitRightPadded(variable, q));
        await q.getAndSend(control, c => c.iterable, iterable => this.visitRightPadded(iterable, q));
        return control;
    }

    protected async visitForLoop(forLoop: J.ForLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forLoop, f => f.control, control => this.visit(control, q));
        await q.getAndSend(forLoop, f => f.body, body => this.visitRightPadded(body, q));
        return forLoop;
    }

    protected async visitForLoopControl(control: J.ForLoop.Control, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(control, c => c.init, i => i.element.id, i => this.visitRightPadded(i, q));
        await q.getAndSend(control, c => c.condition, c => this.visitRightPadded(c, q));
        await q.getAndSendList(control, c => c.update, u => u.element.id, u => this.visitRightPadded(u, q));
        return control;
    }

    protected async visitIf(ifStmt: J.If, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ifStmt, i => i.ifCondition, cond => this.visit(cond, q));
        await q.getAndSend(ifStmt, i => i.thenPart, then => this.visitRightPadded(then, q));
        await q.getAndSend(ifStmt, i => i.elsePart, elsePart => this.visit(elsePart, q));
        return ifStmt;
    }

    protected async visitElse(ifElse: J.If.Else, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ifElse, e => e.body, body => this.visitRightPadded(body, q));
        return ifElse;
    }

    protected async visitImport(importStmt: J.Import, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importStmt, i => i.static, static_ => this.visitLeftPadded(static_, q));
        await q.getAndSend(importStmt, i => i.qualid, qualid => this.visit(qualid, q));
        await q.getAndSend(importStmt, i => i.alias, alias => this.visitLeftPadded(alias, q));
        return importStmt;
    }

    protected async visitInstanceOf(instanceOf: J.InstanceOf, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(instanceOf, i => i.expression, expr => this.visitRightPadded(expr, q));
        await q.getAndSend(instanceOf, i => i.class, clazz => this.visit(clazz, q));
        await q.getAndSend(instanceOf, i => i.pattern, pattern => this.visit(pattern, q));
        await q.getAndSend(instanceOf, i => asRef(i.type), type => this.visitType(type, q));
        await q.getAndSend(instanceOf, i => i.modifier, modifier => this.visit(modifier, q));
        return instanceOf;
    }

    protected async visitIntersectionType(intersectionType: J.IntersectionType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(intersectionType, i => i.bounds, bounds => this.visitContainer(bounds, q));
        return intersectionType;
    }

    protected async visitLabel(label: J.Label, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(label, l => l.label, id => this.visitRightPadded(id, q));
        await q.getAndSend(label, l => l.statement, stmt => this.visit(stmt, q));
        return label;
    }

    protected async visitLambda(lambda: J.Lambda, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(lambda, l => l.parameters, params => this.visit(params, q));
        await q.getAndSend(lambda, l => l.arrow, arrow => this.visitSpace(arrow, q));
        await q.getAndSend(lambda, l => l.body, body => this.visit(body, q));
        await q.getAndSend(lambda, l => asRef(l.type), type => this.visitType(type, q));
        return lambda;
    }

    protected async visitLambdaParameters(params: J.Lambda.Parameters, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(params, p => p.parenthesized);
        await q.getAndSendList(params, p => p.parameters, param => param.element.id, param => this.visitRightPadded(param, q));
        return params;
    }

    protected async visitLiteral(literal: J.Literal, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(literal, l => l.value);
        await q.getAndSend(literal, l => l.valueSource);
        await q.getAndSendList(literal, l => l.unicodeEscapes, e => e.valueSourceIndex + e.codePoint);
        await q.getAndSend(literal, l => asRef(l.type), type => this.visitType(type, q));
        return literal;
    }

    protected async visitMemberReference(memberRef: J.MemberReference, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(memberRef, m => m.containing, cont => this.visitRightPadded(cont, q));
        await q.getAndSend(memberRef, m => m.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(memberRef, m => m.reference, ref => this.visitLeftPadded(ref, q));
        await q.getAndSend(memberRef, m => asRef(m.type), type => this.visitType(type, q));
        await q.getAndSend(memberRef, m => asRef(m.methodType), type => this.visitType(type, q));
        await q.getAndSend(memberRef, m => asRef(m.variableType), type => this.visitType(type, q));
        return memberRef;
    }

    protected async visitMethodInvocation(invocation: J.MethodInvocation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(invocation, m => m.select, select => this.visitRightPadded(select, q));
        await q.getAndSend(invocation, m => m.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(invocation, m => m.name, name => this.visit(name, q));
        await q.getAndSend(invocation, m => m.arguments, args => this.visitContainer(args, q));
        await q.getAndSend(invocation, m => asRef(m.methodType), type => this.visitType(type, q));
        return invocation;
    }

    protected async visitModifier(modifier: J.Modifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(modifier, m => m.keyword);
        await q.getAndSend(modifier, m => m.type);
        await q.getAndSendList(modifier, m => m.annotations, annot => annot.id, annot => this.visit(annot, q));
        return modifier;
    }

    protected async visitMultiCatch(multiCatch: J.MultiCatch, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(multiCatch, m => m.alternatives, alt => alt.element.id, alt => this.visitRightPadded(alt, q));
        return multiCatch;
    }

    protected async visitNewArray(newArray: J.NewArray, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(newArray, n => n.typeExpression, type => this.visit(type, q));
        await q.getAndSendList(newArray, n => n.dimensions, dim => dim.id, dim => this.visit(dim, q));
        await q.getAndSend(newArray, n => n.initializer, init => this.visitContainer(init, q));
        await q.getAndSend(newArray, n => asRef(n.type), type => this.visitType(type, q));
        return newArray;
    }

    protected async visitNewClass(newClass: J.NewClass, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(newClass, n => n.enclosing, encl => this.visitRightPadded(encl, q));
        await q.getAndSend(newClass, n => n.new, n => this.visitSpace(n, q));
        await q.getAndSend(newClass, n => n.class, clazz => this.visit(clazz, q));
        await q.getAndSend(newClass, n => n.arguments, args => this.visitContainer(args, q));
        await q.getAndSend(newClass, n => n.body, body => this.visit(body, q));
        await q.getAndSend(newClass, n => asRef(n.constructorType), type => this.visitType(type, q));
        return newClass;
    }

    protected async visitNullableType(nullableType: J.NullableType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(nullableType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(nullableType, n => n.typeTree, type => this.visitRightPadded(type, q));
        return nullableType;
    }

    protected async visitParameterizedType(paramType: J.ParameterizedType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(paramType, p => p.class, clazz => this.visit(clazz, q));
        await q.getAndSend(paramType, p => p.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(paramType, p => asRef(p.type), type => this.visitType(type, q));
        return paramType;
    }

    protected async visitParentheses<T extends J>(parentheses: J.Parentheses<T>, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(parentheses, p => p.tree, tree => this.visitRightPadded(tree, q));
        return parentheses;
    }

    protected async visitParenthesizedTypeTree(parenthesizedType: J.ParenthesizedTypeTree, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(parenthesizedType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(parenthesizedType, p => p.parenthesizedType, tree => this.visit(tree, q));
        return parenthesizedType;
    }

    protected async visitPrimitive(primitive: J.Primitive, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(primitive, p => asRef(p.type), type => this.visitType(type, q));
        return primitive;
    }

    protected async visitReturn(returnStmt: J.Return, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(returnStmt, r => r.expression, expr => this.visit(expr, q));
        return returnStmt;
    }

    protected async visitSwitch(aSwitch: J.Switch, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(aSwitch, s => s.selector, sel => this.visit(sel, q));
        await q.getAndSend(aSwitch, s => s.cases, block => this.visit(block, q));
        return aSwitch;
    }

    protected async visitSwitchExpression(switchExpr: J.SwitchExpression, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(switchExpr, s => s.selector, sel => this.visit(sel, q));
        await q.getAndSend(switchExpr, s => s.cases, block => this.visit(block, q));
        await q.getAndSend(switchExpr, s => asRef(s.type), type => this.visitType(type, q));
        return switchExpr;
    }

    protected async visitSynchronized(syncStmt: J.Synchronized, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(syncStmt, s => s.lock, lock => this.visit(lock, q));
        await q.getAndSend(syncStmt, s => s.body, body => this.visit(body, q));
        return syncStmt;
    }

    protected async visitTernary(ternary: J.Ternary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ternary, t => t.condition, cond => this.visit(cond, q));
        await q.getAndSend(ternary, t => t.truePart, truePart => this.visitLeftPadded(truePart, q));
        await q.getAndSend(ternary, t => t.falsePart, falsePart => this.visitLeftPadded(falsePart, q));
        await q.getAndSend(ternary, t => asRef(t.type), type => this.visitType(type, q));
        return ternary;
    }

    protected async visitThrow(throwStmt: J.Throw, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(throwStmt, t => t.exception, exc => this.visit(exc, q));
        return throwStmt;
    }

    protected async visitTry(tryStmt: J.Try, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(tryStmt, t => t.resources, res => this.visitContainer(res, q));
        await q.getAndSend(tryStmt, t => t.body, body => this.visit(body, q));
        await q.getAndSendList(tryStmt, t => t.catches, catch_ => catch_.id, catch_ => this.visit(catch_, q));
        await q.getAndSend(tryStmt, t => t.finally, fin => this.visitLeftPadded(fin, q));
        return tryStmt;
    }

    protected async visitTryResource(resource: J.Try.Resource, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(resource, r => r.variableDeclarations, variable => this.visit(variable, q));
        await q.getAndSend(resource, r => r.terminatedWithSemicolon);
        return resource;
    }

    protected async visitTryCatch(aCatch: J.Try.Catch, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(aCatch, c => c.parameter, param => this.visit(param, q));
        await q.getAndSend(aCatch, c => c.body, body => this.visit(body, q));
        return aCatch;
    }

    protected async visitTypeCast(typeCast: J.TypeCast, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(typeCast, t => t.class, clazz => this.visit(clazz, q));
        await q.getAndSend(typeCast, t => t.expression, expr => this.visit(expr, q));
        return typeCast;
    }

    protected async visitTypeParameter(typeParam: J.TypeParameter, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(typeParam, t => t.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(typeParam, t => t.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(typeParam, t => t.name, name => this.visit(name, q));
        await q.getAndSend(typeParam, t => t.bounds, bounds => this.visitContainer(bounds, q));
        return typeParam;
    }

    protected async visitTypeParameters(typeParams: J.TypeParameters, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(typeParams, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(typeParams, t => t.typeParameters, p => p.element.id, params => this.visitRightPadded(params, q));
        return typeParams;
    }

    protected async visitUnary(unary: J.Unary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(unary, u => u.operator, op => this.visitLeftPadded(op, q));
        await q.getAndSend(unary, u => u.expression, expr => this.visit(expr, q));
        await q.getAndSend(unary, u => asRef(u.type), type => this.visitType(type, q));
        return unary;
    }

    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(variable, v => v.name, name => this.visit(name, q));
        await q.getAndSendList(variable, v => v.dimensionsAfterName, d => JSON.stringify(d.element), dims => this.visitLeftPadded(dims, q));
        await q.getAndSend(variable, v => v.initializer, init => this.visitLeftPadded(init, q));
        await q.getAndSend(variable, v => asRef(v.variableType), type => this.visitType(type, q));
        return variable;
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(whileLoop, w => w.condition, cond => this.visit(cond, q));
        await q.getAndSend(whileLoop, w => w.body, body => this.visitRightPadded(body, q));
        return whileLoop;
    }

    protected async visitWildcard(wildcard: J.Wildcard, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(wildcard, w => w.bound, b => this.visitLeftPadded(b, q));
        await q.getAndSend(wildcard, w => w.boundedType, type => this.visit(type, q));
        return wildcard;
    }

    protected async visitYield(yieldExpr: J.Yield, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(yieldExpr, y => y.implicit);
        await q.getAndSend(yieldExpr, y => y.value, value => this.visit(value, q));
        return yieldExpr;
    }

    protected async visitUnknown(unknown: J.Unknown, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(unknown, u => u.source, source => this.visit(source, q));
        return unknown;
    }

    protected async visitUnknownSource(source: J.UnknownSource, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(source, s => s.text);
        return source;
    }

    protected async visitCompilationUnit(cu: J.CompilationUnit, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(cu, c => c.sourcePath);
        await q.getAndSend(cu, c => c.charsetName);
        await q.getAndSend(cu, c => c.charsetBomMarked);
        await q.getAndSend(cu, c => c.checksum);
        await q.getAndSend(cu, c => c.fileAttributes);
        await q.getAndSend(cu, c => c.packageDeclaration, pkg => this.visitRightPadded(pkg, q));
        await q.getAndSendList(cu, c => c.imports, imp => imp.element.id, imp => this.visitRightPadded(imp, q));
        await q.getAndSendList(cu, c => c.classes, cls => cls.id, cls => this.visit(cls, q));
        await q.getAndSend(cu, c => c.eof, space => this.visitSpace(space, q));
        return cu;
    }

    protected async visitPackage(pkg: J.Package, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(pkg, p => p.expression, expr => this.visit(expr, q));
        await q.getAndSendList(pkg, p => p.annotations, annot => annot.id, annot => this.visit(annot, q));
        return pkg;
    }

    protected async visitClassDeclaration(cls: J.ClassDeclaration, q: RpcSendQueue): Promise<J | undefined> {
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

    protected async visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(kind, k => k.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(kind, k => k.type);
        return kind;
    }

    protected async visitBlock(block: J.Block, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(block, b => b.static, s => this.visitRightPadded(s, q));
        await q.getAndSendList(block, b => b.statements, stmt => stmt.element.id, stmt => this.visitRightPadded(stmt, q));
        await q.getAndSend(block, b => b.end, space => this.visitSpace(space, q));
        return block;
    }

    protected async visitMethodDeclaration(method: J.MethodDeclaration, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(method, m => m.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(method, m => m.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(method, m => m.typeParameters, params => this.visit(params, q));
        await q.getAndSend(method, m => m.returnTypeExpression, type => this.visit(type, q));
        await q.getAndSendList(method, m => m.nameAnnotations, a => a.id, name => this.visit(name, q));
        await q.getAndSend(method, m => m.name, name => this.visit(name, q));
        await q.getAndSend(method, m => m.parameters, params => this.visitContainer(params, q));
        await q.getAndSend(method, m => m.throws, throws => this.visitContainer(throws, q));
        await q.getAndSend(method, m => m.body, body => this.visit(body, q));
        await q.getAndSend(method, m => m.defaultValue, def => this.visitLeftPadded(def, q));
        await q.getAndSend(method, m => asRef(m.methodType), type => this.visitType(type, q));
        return method;
    }

    protected async visitVariableDeclarations(varDecls: J.VariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(varDecls, v => v.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(varDecls, v => v.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(varDecls, v => v.typeExpression, type => this.visit(type, q));
        await q.getAndSend(varDecls, v => v.varargs, space => this.visitSpace(space, q));
        await q.getAndSendList(varDecls, v => v.variables, variable => variable.element.id, variable => this.visitRightPadded(variable, q));
        return varDecls;
    }

    protected async visitIdentifier(ident: J.Identifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(ident, id => id.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(ident, id => id.simpleName);
        await q.getAndSend(ident, id => id.type, type => this.visitType(type, q));
        await q.getAndSend(ident, id => id.fieldType, type => this.visitType(type, q));
        return ident;
    }

    public override async visitSpace(space: J.Space, q: RpcSendQueue): Promise<J.Space> {
        await q.getAndSendList(space, s => s.comments,
            c => {
                if (c.kind === J.Kind.TextComment) {
                    return (c as TextComment).text + c.suffix;
                }
                throw new Error(`Unexpected comment type ${c.kind}`);
            },
            async c => {
                if (c.kind === J.Kind.TextComment) {
                    const tc = c as TextComment;
                    await q.getAndSend(tc, c2 => c2.multiline);
                    await q.getAndSend(tc, c2 => c2.text);
                } else {
                    throw new Error(`Unexpected comment type ${c.kind}`);
                }
                await q.getAndSend(c, c2 => c2.suffix);
                await q.getAndSend(c, c2 => c2.markers);
            });
        await q.getAndSend(space, s => s.whitespace);
        return space;
    }

    public override async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcSendQueue): Promise<J.LeftPadded<T>> {
        await q.getAndSend(left, l => l.before, space => this.visitSpace(space, q));
        if (isTree(left.element)) {
            await q.getAndSend(left, l => l.element, elem => this.visit(elem as J, q));
        } else if (isSpace(left.element)) {
            await q.getAndSend(left, l => l.element, space => this.visitSpace(space as J.Space, q));
        } else {
            await q.getAndSend(left, l => l.element);
        }
        await q.getAndSend(left, l => l.markers);
        return left;
    }

    public override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcSendQueue): Promise<J.RightPadded<T>> {
        if (isTree(right.element)) {
            await q.getAndSend(right, r => r.element, elem => this.visit(elem as J, q));
        } else {
            await q.getAndSend(right, r => r.element);
        }
        await q.getAndSend(right, r => r.after, space => this.visitSpace(space, q));
        await q.getAndSend(right, r => r.markers);
        return right;
    }

    public override async visitContainer<T extends J>(container: J.Container<T>, q: RpcSendQueue): Promise<J.Container<T>> {
        await q.getAndSend(container, c => c.before, space => this.visitSpace(space, q));
        await q.getAndSendList(container, c => c.elements, elem => elem.element.id, elem => this.visitRightPadded(elem, q));
        await q.getAndSend(container, c => c.markers);
        return container;
    }

    public override async visitType(javaType: JavaType | undefined, q: RpcSendQueue): Promise<JavaType | undefined> {
        const codec = RpcCodecs.forInstance(javaType);
        await codec?.rpcSend(javaType, q);
        return javaType;
    }
}

export class JavaReceiver extends JavaVisitor<RpcReceiveQueue> {

    protected async preVisit(j: J, q: RpcReceiveQueue): Promise<J | undefined> {
        try {
            const draft = createDraft(j);

            draft.id = await q.receive(j.id);
            draft.prefix = await q.receive(j.prefix, space => this.visitSpace(space, q));
            draft.markers = await q.receive(j.markers);

            return finishDraft(draft);
        } catch (e: any) {
            throw e;
        }
    }

    protected async visitAnnotatedType(annotatedType: J.AnnotatedType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(annotatedType);

        draft.annotations = await q.receiveListDefined(annotatedType.annotations, annot => this.visit(annot, q));
        draft.typeExpression = await q.receive(annotatedType.typeExpression, type => this.visit(type, q));

        return finishDraft(draft);
    }

    protected async visitAnnotation(annotation: J.Annotation, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(annotation);

        draft.annotationType = await q.receive(annotation.annotationType, type => this.visit(type, q));
        draft.arguments = await q.receive(annotation.arguments, args => this.visitContainer(args, q));

        return finishDraft(draft);
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(arrayAccess);

        draft.indexed = await q.receive(arrayAccess.indexed, indexed => this.visit(indexed, q));
        draft.dimension = await q.receive(arrayAccess.dimension, dim => this.visit(dim, q));

        return finishDraft(draft);
    }

    protected async visitArrayDimension(dimension: J.ArrayDimension, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(dimension);

        draft.index = await q.receive(dimension.index, idx => this.visitRightPadded(idx, q));

        return finishDraft(draft);
    }

    protected async visitArrayType(arrayType: J.ArrayType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(arrayType);

        draft.elementType = await q.receive(arrayType.elementType, type => this.visit(type, q));
        draft.annotations = await q.receiveListDefined(arrayType.annotations || [], annot => this.visit(annot, q));
        draft.dimension = await q.receive(arrayType.dimension, d => this.visitLeftPadded(d, q));
        draft.type = await q.receive(arrayType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitAssert(assert: J.Assert, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(assert);

        draft.condition = await q.receive(assert.condition, cond => this.visit(cond, q));
        draft.detail = await q.receive(assert.detail, detail => this.visitOptionalLeftPadded(detail, q));

        return finishDraft(draft);
    }

    protected async visitAssignment(assignment: J.Assignment, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(assignment);

        draft.variable = await q.receive(assignment.variable, variable => this.visit(variable, q));
        draft.assignment = await q.receive(assignment.assignment, assign => this.visitLeftPadded(assign, q));
        draft.type = await q.receive(assignment.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitAssignmentOperation(assignOp: J.AssignmentOperation, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(assignOp);

        draft.variable = await q.receive(assignOp.variable, variable => this.visit(variable, q));
        draft.operator = await q.receive(assignOp.operator, op => this.visitLeftPadded(op, q));
        draft.assignment = await q.receive(assignOp.assignment, assign => this.visit(assign, q));
        draft.type = await q.receive(assignOp.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitBinary(binary: J.Binary, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(binary);

        draft.left = await q.receive(binary.left, left => this.visit(left, q));
        draft.operator = await q.receive(binary.operator, op => this.visitLeftPadded(op, q));
        draft.right = await q.receive(binary.right, right => this.visit(right, q));
        draft.type = await q.receive(binary.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitBreak(breakStmt: J.Break, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(breakStmt);

        draft.label = await q.receive(breakStmt.label, label => this.visit(label, q));

        return finishDraft(draft);
    }

    protected async visitCase(caseStmt: J.Case, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(caseStmt);

        draft.type = await q.receive(caseStmt.type);
        draft.caseLabels = await q.receive(caseStmt.caseLabels, labels => this.visitContainer(labels, q));
        draft.statements = await q.receive(caseStmt.statements, stmts => this.visitContainer(stmts, q));
        draft.body = await q.receive(caseStmt.body, body => this.visitRightPadded(body, q));
        draft.guard = await q.receive(caseStmt.guard, guard => this.visit(guard, q));

        return finishDraft(draft);
    }

    protected async visitContinue(continueStmt: J.Continue, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(continueStmt);

        draft.label = await q.receive(continueStmt.label, label => this.visit(label, q));

        return finishDraft(draft);
    }

    protected async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(controlParens);

        draft.tree = await q.receive(controlParens.tree, tree => this.visitRightPadded(tree, q)) as unknown as WritableDraft<J.RightPadded<T>>;

        return finishDraft(draft);
    }

    protected async visitDeconstructionPattern(pattern: J.DeconstructionPattern, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(pattern);

        draft.deconstructor = await q.receive(pattern.deconstructor, deconstructor => this.visit(deconstructor, q));
        draft.nested = await q.receive(pattern.nested, nested => this.visitContainer(nested, q));
        draft.type = await q.receive(pattern.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitDoWhileLoop(doWhile: J.DoWhileLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(doWhile);

        draft.body = await q.receive(doWhile.body, body => this.visitOptionalRightPadded(body, q));
        draft.whileCondition = await q.receive(doWhile.whileCondition, cond => this.visitLeftPadded(cond, q));

        return finishDraft(draft);
    }

    protected async visitEmpty(empty: J.Empty): Promise<J | undefined> {
        // no additional properties to receive
        return empty;
    }

    protected async visitEnumValueSet(enumValueSet: J.EnumValueSet, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(enumValueSet);

        draft.enums = await q.receiveListDefined(enumValueSet.enums, enumValue => this.visitRightPadded(enumValue, q));
        draft.terminatedWithSemicolon = await q.receive(enumValueSet.terminatedWithSemicolon);

        return finishDraft(draft);
    }

    protected async visitEnumValue(enumValue: J.EnumValue, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(enumValue);

        draft.annotations = await q.receiveListDefined(enumValue.annotations, annot => this.visit(annot, q));
        draft.name = await q.receive(enumValue.name, name => this.visit(name, q));
        draft.initializer = await q.receive(enumValue.initializer, init => this.visit(init, q));

        return finishDraft(draft);
    }

    protected async visitErroneous(erroneous: J.Erroneous, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(erroneous);

        draft.text = await q.receive(erroneous.text);

        return finishDraft(draft);
    }

    protected async visitFieldAccess(fieldAccess: J.FieldAccess, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(fieldAccess);

        draft.target = await q.receive(fieldAccess.target, target => this.visit(target, q));
        draft.name = await q.receive(fieldAccess.name, name => this.visitLeftPadded(name, q));
        draft.type = await q.receive(fieldAccess.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitForEachLoop(forEachLoop: J.ForEachLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(forEachLoop);

        draft.control = await q.receive(forEachLoop.control, c => this.visit(c, q));
        draft.body = await q.receive(forEachLoop.body, body => this.visitRightPadded(body, q));

        return finishDraft(draft);
    }

    protected async visitForEachLoopControl(control: J.ForEachLoop.Control, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(control);

        draft.variable = await q.receive(control.variable, variable => this.visitRightPadded(variable, q));
        draft.iterable = await q.receive(control.iterable, expr => this.visitRightPadded(expr, q));

        return finishDraft(draft);
    }

    protected async visitForLoop(forLoop: J.ForLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(forLoop);

        draft.control = await q.receive(forLoop.control, c => this.visit(c, q));
        draft.body = await q.receive(forLoop.body, body => this.visitRightPadded(body, q));

        return finishDraft(draft);
    }

    protected async visitForLoopControl(control: J.ForLoop.Control, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(control);

        draft.init = await q.receiveListDefined(control.init, init => this.visitRightPadded(init, q));
        draft.condition = await q.receive(control.condition, cond => this.visitRightPadded(cond, q));
        draft.update = await q.receiveListDefined(control.update, update => this.visitRightPadded(update, q));

        return finishDraft(draft);
    }

    protected async visitIf(ifStmt: J.If, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(ifStmt);

        draft.ifCondition = await q.receive(ifStmt.ifCondition, cond => this.visit(cond, q));
        draft.thenPart = await q.receive(ifStmt.thenPart, thenPart => this.visitRightPadded(thenPart, q));
        draft.elsePart = await q.receive(ifStmt.elsePart, elsePart => this.visit(elsePart, q));

        return finishDraft(draft);
    }

    protected async visitElse(ifElse: J.If.Else, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(ifElse);

        draft.body = await q.receive(ifElse.body, body => this.visitRightPadded(body, q));

        return finishDraft(draft);
    }

    protected async visitImport(importStmt: J.Import, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(importStmt);

        draft.static = await q.receive(importStmt.static, s => this.visitLeftPadded(s, q));
        draft.qualid = await q.receive(importStmt.qualid, qualid => this.visit(qualid, q));
        draft.alias = await q.receive(importStmt.alias, alias => this.visitLeftPadded(alias, q));

        return finishDraft(draft);
    }

    protected async visitInstanceOf(instanceOf: J.InstanceOf, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(instanceOf);

        draft.expression = await q.receive(instanceOf.expression, expr => this.visitRightPadded(expr, q));
        draft.class = await q.receive(instanceOf.class, clazz => this.visit(clazz, q));
        draft.pattern = await q.receive(instanceOf.pattern, pattern => this.visit(pattern, q));
        draft.type = await q.receive(instanceOf.type, type => this.visitType(type, q));
        draft.modifier = await q.receive(instanceOf.modifier, mod => this.visit(mod, q));

        return finishDraft(draft);
    }

    protected async visitIntersectionType(intersectionType: J.IntersectionType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(intersectionType);

        draft.bounds = await q.receive(intersectionType.bounds, bounds => this.visitContainer(bounds, q));

        return finishDraft(draft);
    }

    protected async visitLabel(label: J.Label, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(label);

        draft.label = await q.receive(label.label, lbl => this.visitRightPadded(lbl, q));
        draft.statement = await q.receive(label.statement, stmt => this.visit(stmt, q));

        return finishDraft(draft);
    }

    protected async visitLambda(lambda: J.Lambda, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(lambda);

        draft.parameters = await q.receive(lambda.parameters, params => this.visit(params, q));
        draft.arrow = await q.receive(lambda.arrow, arrow => this.visitSpace(arrow, q));
        draft.body = await q.receive(lambda.body, body => this.visit(body, q));
        draft.type = await q.receive(lambda.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitLambdaParameters(params: J.Lambda.Parameters, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(params);

        draft.parenthesized = await q.receive(params.parenthesized);
        draft.parameters = await q.receiveListDefined(params.parameters, param => this.visitRightPadded(param, q));

        return finishDraft(draft);
    }

    protected async visitLiteral(literal: J.Literal, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(literal);

        draft.value = await q.receive(literal.value);
        draft.valueSource = await q.receive(literal.valueSource);
        draft.unicodeEscapes = await q.receiveList(literal.unicodeEscapes);
        draft.type = await q.receive(literal.type, type => this.visitType(type, q) as unknown as JavaType.Primitive);

        return finishDraft(draft);
    }

    protected async visitMemberReference(memberRef: J.MemberReference, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(memberRef);

        draft.containing = await q.receive(memberRef.containing, container => this.visitRightPadded(container, q));
        draft.typeParameters = await q.receive(memberRef.typeParameters, typeParams => this.visitContainer(typeParams, q));
        draft.reference = await q.receive(memberRef.reference, ref => this.visitLeftPadded(ref, q));
        draft.type = await q.receive(memberRef.type, type => this.visitType(type, q));
        draft.methodType = await q.receive(memberRef.methodType, type => this.visitType(type, q) as unknown as JavaType.Method);
        draft.variableType = await q.receive(memberRef.variableType, type => this.visitType(type, q) as unknown as JavaType.Variable);

        return finishDraft(draft);
    }

    protected async visitMethodInvocation(methodInvoc: J.MethodInvocation, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(methodInvoc);

        draft.select = await q.receive(methodInvoc.select, select => this.visitRightPadded(select, q));
        draft.typeParameters = await q.receive(methodInvoc.typeParameters, typeParams => this.visitContainer(typeParams, q));
        draft.name = await q.receive(methodInvoc.name, name => this.visit(name, q));
        draft.arguments = await q.receive(methodInvoc.arguments, args => this.visitContainer(args, q));
        draft.methodType = await q.receive(methodInvoc.methodType, type => this.visitType(type, q) as unknown as JavaType.Method);

        return finishDraft(draft);
    }

    protected async visitModifier(modifier: J.Modifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(modifier);

        draft.keyword = await q.receive(modifier.keyword);
        draft.type = await q.receive(modifier.type);
        draft.annotations = await q.receiveListDefined(modifier.annotations, annot => this.visit(annot, q));

        return finishDraft(draft);
    }

    protected async visitMultiCatch(multiCatch: J.MultiCatch, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(multiCatch);

        draft.alternatives = await q.receiveListDefined(multiCatch.alternatives, alt => this.visitRightPadded(alt, q));

        return finishDraft(draft);
    }

    protected async visitNewArray(newArray: J.NewArray, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(newArray);

        draft.typeExpression = await q.receive(newArray.typeExpression, type => this.visit(type, q));
        draft.dimensions = await q.receiveListDefined(newArray.dimensions, dim => this.visit(dim, q));
        draft.initializer = await q.receive(newArray.initializer, init => this.visitContainer(init, q));
        draft.type = await q.receive(newArray.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitNewClass(newClass: J.NewClass, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(newClass);

        draft.enclosing = await q.receive(newClass.enclosing, encl => this.visitRightPadded(encl, q));
        draft.new = await q.receive(newClass.new, new_ => this.visitSpace(new_, q));
        draft.class = await q.receive(newClass.class, clazz => this.visit(clazz, q));
        draft.arguments = await q.receive(newClass.arguments, args => this.visitContainer(args, q));
        draft.body = await q.receive(newClass.body, body => this.visit(body, q));
        draft.constructorType = await q.receive(newClass.constructorType, type => this.visitType(type, q) as unknown as JavaType.Method);

        return finishDraft(draft);
    }

    protected async visitNullableType(nullableType: J.NullableType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(nullableType);

        draft.annotations = await q.receiveListDefined(nullableType.annotations, annot => this.visit(annot, q));
        draft.typeTree = await q.receive(nullableType.typeTree, type => this.visitRightPadded(type, q));

        return finishDraft(draft);
    }

    protected async visitParameterizedType(paramType: J.ParameterizedType, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(paramType);

        draft.class = await q.receive(paramType.class, clazz => this.visit(clazz, q));
        draft.typeParameters = await q.receive(paramType.typeParameters, params => this.visitContainer(params, q));
        draft.type = await q.receive(paramType.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitParentheses<T extends J>(parentheses: J.Parentheses<T>, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(parentheses);

        draft.tree = await q.receive(parentheses.tree, tree =>
            this.visitRightPadded(tree, q)) as unknown as WritableDraft<J.RightPadded<T>>;

        return finishDraft(draft);
    }

    protected async visitParenthesizedTypeTree(parenthesizedType: J.ParenthesizedTypeTree, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(parenthesizedType);

        draft.annotations = await q.receiveListDefined(parenthesizedType.annotations, annot => this.visit(annot, q));
        draft.parenthesizedType = await q.receive(parenthesizedType.parenthesizedType, tree => this.visit(tree, q));

        return finishDraft(draft);
    }

    protected async visitPrimitive(primitive: J.Primitive, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(primitive);

        draft.type = await q.receive(primitive.type, type => this.visitType(type, q) as unknown as JavaType.Primitive);

        return finishDraft(draft);
    }

    protected async visitSwitch(switchStmt: J.Switch, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(switchStmt);

        draft.selector = await q.receive(switchStmt.selector, selector => this.visit(selector, q));
        draft.cases = await q.receive(switchStmt.cases, cases => this.visit(cases, q));

        return finishDraft(draft);
    }

    protected async visitSwitchExpression(switchExpr: J.SwitchExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(switchExpr);

        draft.selector = await q.receive(switchExpr.selector, selector => this.visit(selector, q));
        draft.cases = await q.receive(switchExpr.cases, cases => this.visit(cases, q));
        draft.type = await q.receive(switchExpr.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitTernary(ternary: J.Ternary, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(ternary);

        draft.condition = await q.receive(ternary.condition, cond => this.visit(cond, q));
        draft.truePart = await q.receive(ternary.truePart, truePart => this.visitLeftPadded(truePart, q));
        draft.falsePart = await q.receive(ternary.falsePart, falsePart => this.visitLeftPadded(falsePart, q));
        draft.type = await q.receive(ternary.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitThrow(throwStmt: J.Throw, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(throwStmt);

        draft.exception = await q.receive(throwStmt.exception, exception => this.visit(exception, q));

        return finishDraft(draft);
    }

    protected async visitTry(tryStmt: J.Try, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(tryStmt);

        draft.resources = await q.receive(tryStmt.resources, resources => this.visitContainer(resources, q));
        draft.body = await q.receive(tryStmt.body, body => this.visit(body, q));
        draft.catches = await q.receiveListDefined(tryStmt.catches, catchBlock => this.visit(catchBlock, q));
        draft.finally = await q.receive(tryStmt.finally, finallyBlock => this.visitOptionalLeftPadded(finallyBlock, q));

        return finishDraft(draft);
    }

    protected async visitTryResource(resource: J.Try.Resource, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(resource);

        draft.variableDeclarations = await q.receive(resource.variableDeclarations, variables => this.visit(variables, q));
        draft.terminatedWithSemicolon = await q.receive(resource.terminatedWithSemicolon);

        return finishDraft(draft);
    }

    protected async visitTryCatch(tryCatch: J.Try.Catch, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(tryCatch);

        draft.parameter = await q.receive(tryCatch.parameter, param => this.visit(param, q));
        draft.body = await q.receive(tryCatch.body, body => this.visit(body, q));

        return finishDraft(draft);
    }

    protected async visitUnary(unary: J.Unary, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(unary);

        draft.operator = await q.receive(unary.operator, op => this.visitLeftPadded(op, q));
        draft.expression = await q.receive(unary.expression, expr => this.visit(expr, q));
        draft.type = await q.receive(unary.type, type => this.visitType(type, q));

        return finishDraft(draft);
    }

    protected async visitUnknown(unknown: J.Unknown, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(unknown);

        draft.source = await q.receive(unknown.source, source => this.visit(source, q));

        return finishDraft(draft);
    }

    protected async visitUnknownSource(unknownSource: J.UnknownSource, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(unknownSource);

        draft.text = await q.receive(unknownSource.text);

        return finishDraft(draft);
    }

    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(variable);

        draft.name = await q.receive(variable.name, name => this.visit(name, q));
        draft.dimensionsAfterName = await q.receiveListDefined(variable.dimensionsAfterName, dim => this.visitLeftPadded(dim, q));
        draft.initializer = await q.receive(variable.initializer, init => this.visitOptionalLeftPadded(init, q));
        draft.variableType = await q.receive(variable.variableType, type => this.visitType(type, q) as unknown as JavaType.Variable);

        return finishDraft(draft);
    }

    protected async visitYield(yieldStmt: J.Yield, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(yieldStmt);

        draft.implicit = await q.receive(yieldStmt.implicit);
        draft.value = await q.receive(yieldStmt.value, value => this.visit(value, q));

        return finishDraft(draft);
    }

    protected async visitTypeParameters(typeParams: J.TypeParameters, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeParams);

        draft.annotations = await q.receiveListDefined(typeParams.annotations, annot => this.visit(annot, q));
        draft.typeParameters = await q.receiveListDefined(typeParams.typeParameters, param => this.visitRightPadded(param, q));

        return finishDraft(draft);
    }

    protected async visitReturn(returnStmt: J.Return, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(returnStmt);

        draft.expression = await q.receive(returnStmt.expression, expr => this.visit(expr, q));

        return finishDraft(draft);
    }

    protected async visitSynchronized(synchronizedStmt: J.Synchronized, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(synchronizedStmt);

        draft.lock = await q.receive(synchronizedStmt.lock, lock => this.visit(lock, q));
        draft.body = await q.receive(synchronizedStmt.body, body => this.visit(body, q));

        return finishDraft(draft);
    }

    protected async visitTypeCast(typeCast: J.TypeCast, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeCast);

        draft.class = await q.receive(typeCast.class, typeExpr => this.visit(typeExpr, q));
        draft.expression = await q.receive(typeCast.expression, expr => this.visit(expr, q));

        return finishDraft(draft);
    }

    protected async visitTypeParameter(typeParameter: J.TypeParameter, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(typeParameter);

        draft.annotations = await q.receiveListDefined(typeParameter.annotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(typeParameter.modifiers, annot => this.visit(annot, q));
        draft.name = await q.receive(typeParameter.name, name => this.visit(name, q));
        draft.bounds = await q.receive(typeParameter.bounds, bounds => this.visitContainer(bounds, q));

        return finishDraft(draft);
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(whileLoop);

        draft.condition = await q.receive(whileLoop.condition, cond => this.visit(cond, q));
        draft.body = await q.receive(whileLoop.body, body => this.visitOptionalRightPadded(body, q));

        return finishDraft(draft);
    }

    protected async visitWildcard(wildcard: J.Wildcard, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(wildcard);

        draft.bound = await q.receive(wildcard.bound, bound => this.visitLeftPadded(bound, q));
        draft.boundedType = await q.receive(wildcard.boundedType, type => this.visit(type, q));

        return finishDraft(draft);
    }

    protected async visitCompilationUnit(cu: J.CompilationUnit, q: RpcReceiveQueue): Promise<J | undefined> {
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

    protected async visitPackage(pkg: J.Package, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(pkg);

        draft.expression = await q.receive<Expression>(pkg.expression, expr => this.visit(expr, q));
        draft.annotations = await q.receiveListDefined(pkg.annotations, annot => this.visit(annot, q));

        return finishDraft(draft);
    }

    protected async visitClassDeclaration(cls: J.ClassDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
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

    protected async visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(kind);

        draft.annotations = await q.receiveListDefined(kind.annotations, annot => this.visit(annot, q));
        draft.type = await q.receive(kind.type);

        return finishDraft(draft);
    }

    protected async visitBlock(block: J.Block, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(block);

        draft.static = await q.receive(block.static, s => this.visitRightPadded(s, q));
        draft.statements = await q.receiveListDefined(block.statements, stmt => this.visitRightPadded(stmt, q));
        draft.end = await q.receive(block.end, space => this.visitSpace(space, q));

        return finishDraft(draft);
    }

    protected async visitMethodDeclaration(method: J.MethodDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(method);

        draft.leadingAnnotations = await q.receiveListDefined(method.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(method.modifiers, mod => this.visit(mod, q));
        draft.typeParameters = await q.receive(method.typeParameters, params => this.visit(params, q));
        draft.returnTypeExpression = await q.receive(method.returnTypeExpression, type => this.visit(type, q));
        draft.nameAnnotations = (await q.receiveList(method.nameAnnotations, name => this.visit(name, q)))!;
        draft.name = await q.receive(method.name, name => this.visit(name, q));
        draft.parameters = await q.receive(method.parameters, params => this.visitContainer(params, q));
        draft.throws = await q.receive(method.throws, throws => this.visitContainer(throws, q));
        draft.body = await q.receive(method.body, body => this.visit(body, q));
        draft.defaultValue = await q.receive(method.defaultValue, def => this.visitLeftPadded(def, q));
        draft.methodType = await q.receive(method.methodType, type => this.visitType(type, q) as unknown as JavaType.Method);

        return finishDraft(draft);
    }

    protected async visitVariableDeclarations(varDecls: J.VariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(varDecls);

        draft.leadingAnnotations = await q.receiveListDefined(varDecls.leadingAnnotations, annot => this.visit(annot, q));
        draft.modifiers = await q.receiveListDefined(varDecls.modifiers, mod => this.visit(mod, q));
        draft.typeExpression = await q.receive(varDecls.typeExpression, type => this.visit(type, q));
        draft.varargs = await q.receive(varDecls.varargs, space => this.visitSpace(space, q));
        draft.variables = await q.receiveListDefined(varDecls.variables, variable => this.visitRightPadded(variable, q));

        return finishDraft(draft);
    }

    protected async visitIdentifier(ident: J.Identifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const draft = createDraft(ident);

        draft.annotations = await q.receiveListDefined(ident.annotations, annot => this.visit(annot, q));
        draft.simpleName = await q.receive(ident.simpleName);
        draft.type = await q.receive(ident.type, type => this.visitType(type, q));
        draft.fieldType = await q.receive(ident.fieldType, type => this.visitType(type, q) as any as JavaType.Variable);

        return finishDraft(draft);
    }

    public override async visitSpace(space: J.Space, q: RpcReceiveQueue): Promise<J.Space> {
        const draft = createDraft(space);

        draft.comments = await q.receiveListDefined(space.comments, async c => {
            if (c.kind === J.Kind.TextComment) {
                const tc = c as TextComment;
                return await produceAsync(tc, async draft => {
                    draft.multiline = await q.receive(tc.multiline);
                    draft.text = await q.receive(tc.text);
                    draft.suffix = await q.receive(c.suffix);
                    draft.markers = await q.receive(c.markers);
                });
            } else {
                throw new Error(`Unexpected comment type ${c.kind}`);
            }
        });
        draft.whitespace = await q.receive(space.whitespace);

        return finishDraft(draft);
    }

    public override async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): Promise<J.LeftPadded<T>> {
        if (!left) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty left padding");
        }

        const draft = createDraft(left);

        draft.before = await q.receive(left.before, space => this.visitSpace(space, q));
        draft.element = await q.receive(left.element, elem => {
            if (isSpace(elem)) {
                return this.visitSpace(elem as J.Space, q) as any as T;
            } else if (typeof elem === 'object' && elem.kind) {
                // FIXME find a better way to check if it is a `Tree`
                return this.visit(elem as J, q) as any as T;
            }
            return elem;
        }) as Draft<T>;
        draft.markers = await q.receive(left.markers);

        return finishDraft(draft) as J.LeftPadded<T>;
    }

    public override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): Promise<J.RightPadded<T>> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty right padding");
        }

        const draft = createDraft(right);

        draft.element = await q.receive(right.element, elem => {
            if (isSpace(elem)) {
                return this.visitSpace(elem as J.Space, q) as any as T;
            } else if (typeof elem === 'object' && elem.kind) {
                // FIXME find a better way to check if it is a `Tree`
                return this.visit(elem as J, q) as any as T;
            }
            return elem as any as T;
        }) as Draft<T>;
        draft.after = await q.receive(right.after, space => this.visitSpace(space, q));
        draft.markers = await q.receive(right.markers);

        return finishDraft(draft) as J.RightPadded<T>;
    }

    public override async visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): Promise<J.Container<T>> {
        const draft = createDraft(container);

        draft.before = await q.receive(container.before, space => this.visitSpace(space, q));
        draft.elements = await q.receiveListDefined(container.elements, elem => this.visitRightPadded(elem, q)) as Draft<J.RightPadded<T>[]>;
        draft.markers = await q.receive(container.markers);

        return finishDraft(draft) as J.Container<T>;
    }

    public override async visitType(javaType: JavaType | undefined, q: RpcReceiveQueue): Promise<JavaType | undefined> {
        const codec = RpcCodecs.forInstance(javaType);
        if (codec) {
            return await codec.rpcReceive(javaType, q);
        }
        return super.visitType(javaType, q);
    }
}
