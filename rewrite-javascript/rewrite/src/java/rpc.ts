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
import {emptySpace, Expression, isSpace, J, TextComment} from "./tree";
import {emptyMarkers, Markers} from "../markers";
import {Type} from "./type";
import {TypeVisitor} from "./type-visitor";
import {updateIfChanged} from "../util";
import Space = J.Space;

class TypeSender extends TypeVisitor<RpcSendQueue> {
    protected async visitPrimitive(primitive: Type.Primitive, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(primitive, p => p.keyword);
        return primitive;
    }

    protected async visitClass(cls: Type.Class, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(cls, c => c.flags);
        await q.getAndSend(cls, c => c.classKind);
        await q.getAndSend(cls, c => c.fullyQualifiedName);
        await q.getAndSendList(cls, c => (c.typeParameters || []).map(t => asRef(t)), t => Type.signature(t), t => this.visit(t, q));
        await q.getAndSend(cls, c => asRef(c.supertype), st => this.visit(st, q));
        await q.getAndSend(cls, c => asRef(c.owningClass), oc => this.visit(oc, q));
        await q.getAndSendList(cls, c => (c.annotations || []).map(a => asRef(a)), t => Type.signature(t), a => this.visit(a, q));
        await q.getAndSendList(cls, c => (c.interfaces || []).map(i => asRef(i)), t => Type.signature(t), i => this.visit(i, q));
        await q.getAndSendList(cls, c => (c.members || []).map(m => asRef(m)), t => Type.signature(t), m => this.visit(m, q));
        await q.getAndSendList(cls, c => (c.methods || []).map(m => asRef(m)), t => Type.signature(t), m => this.visit(m, q));
        return cls;
    }

    protected async visitVariable(variable: Type.Variable, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(variable, v => v.name);
        await q.getAndSend(variable, v => v.owner ? asRef(v.owner) : undefined, owner => this.visit(owner, q));
        await q.getAndSend(variable, v => asRef(v.type), t => this.visit(t, q));
        await q.getAndSendList(variable, v => (v.annotations || []).map(v2 => asRef(v2)), t => Type.signature(t), a => this.visit(a, q));
        return variable;
    }

    protected async visitAnnotation(annotation: Type.Annotation, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(annotation, a => asRef(a.type), t => this.visit(t, q));
        // await q.getAndSendList(annotation, a => (a.values || []).map(v => asRef(v)), v => {
        //     let value: any;
        //     if (v.kind === Type.Kind.SingleElementValue) {
        //         const single = v as Type.Annotation.SingleElementValue;
        //         value = single.constantValue !== undefined ? single.constantValue : single.referenceValue;
        //     } else {
        //         const array = v as Type.Annotation.ArrayElementValue;
        //         value = array.constantValues || array.referenceValues;
        //     }
        //     return `${Type.signature(v.element)}:${value == null ? "null" : value.toString()}`;
        // }, async v => {
        //     // Handle element values inline like the Java implementation
        //     await q.getAndSend(v, e => asRef(e.element), elem => this.visit(elem, q));
        //     if (v.kind === Type.Kind.SingleElementValue) {
        //         const single = v as Type.Annotation.SingleElementValue;
        //         await q.getAndSend(single, s => s.constantValue);
        //         await q.getAndSend(single, s => asRef(s.referenceValue), ref => this.visit(ref, q));
        //     } else if (v.kind === Type.Kind.ArrayElementValue) {
        //         const array = v as Type.Annotation.ArrayElementValue;
        //         await q.getAndSendList(array, a => a.constantValues || [], val => val == null ? "null" : val.toString());
        //         await q.getAndSendList(array, a => (a.referenceValues || []).map(r => asRef(r)), t => Type.signature(t), r => this.visit(r, q));
        //     }
        // });
        return annotation;
    }

    protected async visitMethod(method: Type.Method, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(method, m => asRef(m.declaringType), dt => this.visit(dt, q));
        await q.getAndSend(method, m => m.name);
        await q.getAndSend(method, m => m.flags);
        await q.getAndSend(method, m => asRef(m.returnType), rt => this.visit(rt, q));
        await q.getAndSendList(method, m => m.parameterNames || [], v => v);
        await q.getAndSendList(method, m => (m.parameterTypes || []).map(t => asRef(t)), t => Type.signature(t), pt => this.visit(pt, q));
        await q.getAndSendList(method, m => (m.thrownExceptions || []).map(t => asRef(t)), t => Type.signature(t), et => this.visit(et, q));
        await q.getAndSendList(method, m => (m.annotations || []).map(a => asRef(a)), t => Type.signature(t), a => this.visit(a, q));
        await q.getAndSendList(method, m => m.defaultValue || undefined, v => v);
        await q.getAndSendList(method, m => m.declaredFormalTypeNames || [], v => v);
        return method;
    }

    protected async visitArray(array: Type.Array, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(array, a => asRef(a.elemType), et => this.visit(et, q));
        await q.getAndSendList(array, a => (a.annotations || []).map(ann => asRef(ann)), t => Type.signature(t), ann => this.visit(ann, q));
        return array;
    }

    protected async visitParameterized(parameterized: Type.Parameterized, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(parameterized, p => asRef(p.type), t => this.visit(t, q));
        await q.getAndSendList(parameterized, p => (p.typeParameters || []).map(tp => asRef(tp)), t => Type.signature(t), tp => this.visit(tp, q));
        return parameterized;
    }

    protected async visitGenericTypeVariable(generic: Type.GenericTypeVariable, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSend(generic, g => g.name);
        // Convert TypeScript enum to Java enum string
        await q.getAndSend(generic, g => {
            switch (g.variance) {
                case Type.GenericTypeVariable.Variance.Covariant:
                    return 'COVARIANT';
                case Type.GenericTypeVariable.Variance.Contravariant:
                    return 'CONTRAVARIANT';
                case Type.GenericTypeVariable.Variance.Invariant:
                default:
                    return 'INVARIANT';
            }
        });
        await q.getAndSendList(generic, g => (g.bounds || []).map(b => asRef(b)), t => Type.signature(t), b => this.visit(b, q));
        return generic;
    }

    protected async visitUnion(union: Type.Union, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSendList(union, u => (u.bounds || []).map(b => asRef(b)), t => Type.signature(t), b => this.visit(b, q));
        return union;
    }

    protected async visitIntersection(intersection: Type.Intersection, q: RpcSendQueue): Promise<Type | undefined> {
        await q.getAndSendList(intersection, i => (i.bounds || []).map(b => asRef(b)), t => Type.signature(t), b => this.visit(b, q));
        return intersection;
    }
}

class TypeReceiver extends TypeVisitor<RpcReceiveQueue> {
    async preVisit(_type: Type, _q: RpcReceiveQueue): Promise<Type | undefined> {
        // Don't call default preVisit to avoid circular references
        return _type;
    }

    async postVisit(_type: Type, _q: RpcReceiveQueue): Promise<Type | undefined> {
        // Don't call default postVisit to avoid circular references
        return _type;
    }

    protected async visitPrimitive(primitive: Type.Primitive, q: RpcReceiveQueue): Promise<Type | undefined> {
        const keyword: string = await q.receive(primitive.keyword);
        return Type.Primitive.fromKeyword(keyword)!;
    }

    protected async visitClass(cls: Type.Class, q: RpcReceiveQueue): Promise<Type | undefined> {
        cls.flags = await q.receive(cls.flags);
        cls.classKind = await q.receive(cls.classKind);
        cls.fullyQualifiedName = await q.receive(cls.fullyQualifiedName);
        cls.typeParameters = await q.receiveList(cls.typeParameters, tp => this.visit(tp, q)) || [];
        cls.supertype = await q.receive(cls.supertype, st => this.visit(st, q));
        cls.owningClass = await q.receive(cls.owningClass, oc => this.visit(oc, q));
        cls.annotations = await q.receiveList(cls.annotations, a => this.visit(a, q)) || [];
        cls.interfaces = await q.receiveList(cls.interfaces, i => this.visit(i, q)) || [];
        cls.members = await q.receiveList(cls.members, m => this.visit(m, q)) || [];
        cls.methods = await q.receiveList(cls.methods, m => this.visit(m, q)) || [];
        return cls;
    }

    protected async visitVariable(variable: Type.Variable, q: RpcReceiveQueue): Promise<Type | undefined> {
        variable.name = await q.receive(variable.name);
        variable.owner = await q.receive(variable.owner, owner => this.visit(owner, q));
        variable.type = await q.receive(variable.type, t => this.visit(t, q));
        variable.annotations = await q.receiveList(variable.annotations, a => this.visit(a, q)) || [];
        return variable;
    }

    protected async visitAnnotation(annotation: Type.Annotation, q: RpcReceiveQueue): Promise<Type | undefined> {
        annotation.type = await q.receive(annotation.type, t => this.visit(t, q));
        // annotation.values = await q.receiveList(annotation.values, async v => {
        //     // Handle element values inline like the Java implementation
        //     if (v.kind === Type.Kind.SingleElementValue) {
        //         const single = v as Type.Annotation.SingleElementValue;
        //         const element = await q.receive(single.element, elem => this.visit(elem, q));
        //         const constantValue = await q.receive(single.constantValue);
        //         const referenceValue = await q.receive(single.referenceValue, ref => this.visit(ref, q));
        //         return {
        //             kind: Type.Kind.SingleElementValue,
        //             element,
        //             constantValue,
        //             referenceValue
        //         } as Type.Annotation.SingleElementValue;
        //     } else if (v.kind === Type.Kind.ArrayElementValue) {
        //         const array = v as Type.Annotation.ArrayElementValue;
        //         const element = await q.receive(array.element, elem => this.visit(elem, q));
        //         const constantValues = await q.receiveList(array.constantValues);
        //         const referenceValues = await q.receiveList(array.referenceValues, r => this.visit(r, q));
        //         return {
        //             kind: Type.Kind.ArrayElementValue,
        //             element,
        //             constantValues,
        //             referenceValues
        //         } as Type.Annotation.ArrayElementValue;
        //     }
        //     return v;
        // }) || [];
        return annotation;
    }

    protected async visitMethod(method: Type.Method, q: RpcReceiveQueue): Promise<Type | undefined> {
        method.declaringType = await q.receive(method.declaringType, dt => this.visit(dt, q));
        method.name = await q.receive(method.name);
        method.flags = await q.receive(method.flags);
        method.returnType = await q.receive(method.returnType, rt => this.visit(rt, q));
        method.parameterNames = await q.receiveList(method.parameterNames) || [];
        method.parameterTypes = await q.receiveList(method.parameterTypes, pt => this.visit(pt, q)) || [];
        method.thrownExceptions = await q.receiveList(method.thrownExceptions, et => this.visit(et, q)) || [];
        method.annotations = await q.receiveList(method.annotations, a => this.visit(a, q)) || [];
        method.defaultValue = await q.receiveList(method.defaultValue);
        method.declaredFormalTypeNames = await q.receiveList(method.declaredFormalTypeNames) || [];
        return method;
    }

    protected async visitArray(array: Type.Array, q: RpcReceiveQueue): Promise<Type | undefined> {
        array.elemType = await q.receive(array.elemType, et => this.visit(et, q));
        array.annotations = await q.receiveList(array.annotations, ann => this.visit(ann, q)) || [];
        return array;
    }

    protected async visitParameterized(parameterized: Type.Parameterized, q: RpcReceiveQueue): Promise<Type | undefined> {
        parameterized.type = await q.receive(parameterized.type, t => this.visit(t, q));
        parameterized.typeParameters = await q.receiveList(parameterized.typeParameters, tp => this.visit(tp, q)) || [];
        return parameterized;
    }

    protected async visitGenericTypeVariable(generic: Type.GenericTypeVariable, q: RpcReceiveQueue): Promise<Type | undefined> {
        generic.name = await q.receive(generic.name);
        const varianceStr = await q.receive(generic.variance) as any as string;
        // Convert Java enum string to TypeScript enum
        switch (varianceStr) {
            case 'COVARIANT':
                generic.variance = Type.GenericTypeVariable.Variance.Covariant;
                break;
            case 'CONTRAVARIANT':
                generic.variance = Type.GenericTypeVariable.Variance.Contravariant;
                break;
            case 'INVARIANT':
            default:
                generic.variance = Type.GenericTypeVariable.Variance.Invariant;
                break;
        }
        generic.bounds = await q.receiveList(generic.bounds, b => this.visit(b, q)) || [];
        return generic;
    }

    protected async visitUnion(union: Type.Union, q: RpcReceiveQueue): Promise<Type | undefined> {
        union.bounds = await q.receiveList(union.bounds, b => this.visit(b, q)) || [];
        return union;
    }

    protected async visitIntersection(intersection: Type.Intersection, q: RpcReceiveQueue): Promise<Type | undefined> {
        intersection.bounds = await q.receiveList(intersection.bounds, b => this.visit(b, q)) || [];
        return intersection;
    }
}

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
        await q.getAndSend(dimension, d => d.index, idx => this.visitRightPadded(idx, q), J.Kind.RightPadded);
        return dimension;
    }

    protected async visitArrayType(arrayType: J.ArrayType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(arrayType, a => a.elementType, type => this.visit(type, q));
        await q.getAndSendList(arrayType, a => a.annotations || [], annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(arrayType, a => a.dimension, d => this.visitLeftPadded(d, q), J.Kind.LeftPadded);
        await q.getAndSend(arrayType, a => asRef(a.type), type => this.visitType(type, q));
        return arrayType;
    }

    protected async visitAssert(assert: J.Assert, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assert, a => a.condition, cond => this.visit(cond, q));
        await q.getAndSend(assert, a => a.detail, detail => this.visitLeftPadded(detail, q), J.Kind.LeftPadded);
        return assert;
    }

    protected async visitAssignment(assignment: J.Assignment, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignment, a => a.variable, variable => this.visit(variable, q));
        await q.getAndSend(assignment, a => a.assignment, assign => this.visitLeftPadded(assign, q), J.Kind.LeftPadded);
        await q.getAndSend(assignment, a => asRef(a.type), type => this.visitType(type, q));
        return assignment;
    }

    protected async visitAssignmentOperation(assignOp: J.AssignmentOperation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(assignOp, a => a.variable, variable => this.visit(variable, q));
        await q.getAndSend(assignOp, a => a.operator, op => this.visitLeftPadded(op, q), J.Kind.LeftPadded);
        await q.getAndSend(assignOp, a => a.assignment, assign => this.visit(assign, q));
        await q.getAndSend(assignOp, a => asRef(a.type), type => this.visitType(type, q));
        return assignOp;
    }

    protected async visitBinary(binary: J.Binary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(binary, b => b.left, left => this.visit(left, q));
        await q.getAndSend(binary, b => b.operator, op => this.visitLeftPadded(op, q), J.Kind.LeftPadded);
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
        await q.getAndSend(caseStmt, c => c.body, body => this.visitRightPadded(body, q), J.Kind.RightPadded);
        await q.getAndSend(caseStmt, c => c.guard, guard => this.visit(guard, q));
        return caseStmt;
    }

    protected async visitContinue(continueStmt: J.Continue, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(continueStmt, c => c.label, label => this.visit(label, q));
        return continueStmt;
    }

    protected async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(controlParens, c => c.tree, tree => this.visitRightPadded(tree, q), J.Kind.RightPadded);
        return controlParens;
    }

    protected async visitDeconstructionPattern(pattern: J.DeconstructionPattern, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(pattern, p => p.deconstructor, deconstructor => this.visit(deconstructor, q));
        await q.getAndSend(pattern, p => p.nested, nested => this.visitContainer(nested, q));
        await q.getAndSend(pattern, p => asRef(p.type), type => this.visitType(type, q));
        return pattern;
    }

    protected async visitDoWhileLoop(doWhile: J.DoWhileLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(doWhile, d => d.body, body => this.visitRightPadded(body, q), J.Kind.RightPadded);
        await q.getAndSend(doWhile, d => d.whileCondition, cond => this.visitLeftPadded(cond, q), J.Kind.LeftPadded);
        return doWhile;
    }

    protected async visitEmpty(empty: J.Empty, _q: RpcSendQueue): Promise<J | undefined> {
        // No additional properties to send
        return empty;
    }

    protected async visitEnumValueSet(enumValueSet: J.EnumValueSet, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(enumValueSet, e => e.enums, enumValue => enumValue.id, enumValue => this.visitRightPadded(enumValue, q), J.Kind.RightPadded);
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
        await q.getAndSend(fieldAccess, f => f.name, name => this.visitLeftPadded(name, q), J.Kind.LeftPadded);
        await q.getAndSend(fieldAccess, a => asRef(a.type), type => this.visitType(type, q));
        return fieldAccess;
    }

    protected async visitForEachLoop(forEach: J.ForEachLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forEach, f => f.control, control => this.visit(control, q));
        await q.getAndSend(forEach, f => f.body, body => this.visitRightPadded(body, q), J.Kind.RightPadded);
        return forEach;
    }

    protected async visitForEachLoopControl(control: J.ForEachLoop.Control, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(control, c => c.variable, variable => this.visitRightPadded(variable, q), J.Kind.RightPadded);
        await q.getAndSend(control, c => c.iterable, iterable => this.visitRightPadded(iterable, q), J.Kind.RightPadded);
        return control;
    }

    protected async visitForLoop(forLoop: J.ForLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(forLoop, f => f.control, control => this.visit(control, q));
        await q.getAndSend(forLoop, f => f.body, body => this.visitRightPadded(body, q), J.Kind.RightPadded);
        return forLoop;
    }

    protected async visitForLoopControl(control: J.ForLoop.Control, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(control, c => c.init, i => i.id, i => this.visitRightPadded(i, q), J.Kind.RightPadded);
        await q.getAndSend(control, c => c.condition, c => this.visitRightPadded(c, q), J.Kind.RightPadded);
        await q.getAndSendList(control, c => c.update, u => u.id, u => this.visitRightPadded(u, q), J.Kind.RightPadded);
        return control;
    }

    protected async visitIf(ifStmt: J.If, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ifStmt, i => i.ifCondition, cond => this.visit(cond, q));
        await q.getAndSend(ifStmt, i => i.thenPart, then => this.visitRightPadded(then, q), J.Kind.RightPadded);
        await q.getAndSend(ifStmt, i => i.elsePart, elsePart => this.visit(elsePart, q));
        return ifStmt;
    }

    protected async visitElse(ifElse: J.If.Else, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(ifElse, e => e.body, body => this.visitRightPadded(body, q), J.Kind.RightPadded);
        return ifElse;
    }

    protected async visitImport(importStmt: J.Import, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(importStmt, i => i.static, static_ => this.visitLeftPadded(static_, q), J.Kind.LeftPadded);
        await q.getAndSend(importStmt, i => i.qualid, qualid => this.visit(qualid, q));
        await q.getAndSend(importStmt, i => i.alias, alias => this.visitLeftPadded(alias, q), J.Kind.LeftPadded);
        return importStmt;
    }

    protected async visitInstanceOf(instanceOf: J.InstanceOf, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(instanceOf, i => i.expression, expr => this.visitRightPadded(expr, q), J.Kind.RightPadded);
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
        await q.getAndSend(label, l => l.label, id => this.visitRightPadded(id, q), J.Kind.RightPadded);
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
        await q.getAndSendList(params, p => p.parameters, param => param.id, param => this.visitRightPadded(param, q), J.Kind.RightPadded);
        return params;
    }

    protected async visitLiteral(literal: J.Literal, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(literal, l => l.value);
        await q.getAndSend(literal, l => l.valueSource);
        await q.getAndSendList(literal, l => l.unicodeEscapes, e => e.valueSourceIndex + e.codePoint, async (e) => {
            await q.getAndSend(e, u => u.valueSourceIndex);
            await q.getAndSend(e, u => u.codePoint);
        });
        await q.getAndSend(literal, l => asRef(l.type), type => this.visitType(type, q));
        return literal;
    }

    protected async visitMemberReference(memberRef: J.MemberReference, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(memberRef, m => m.containing, cont => this.visitRightPadded(cont, q), J.Kind.RightPadded);
        await q.getAndSend(memberRef, m => m.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(memberRef, m => m.reference, ref => this.visitLeftPadded(ref, q), J.Kind.LeftPadded);
        await q.getAndSend(memberRef, m => asRef(m.type), type => this.visitType(type, q));
        await q.getAndSend(memberRef, m => asRef(m.methodType), type => this.visitType(type, q));
        await q.getAndSend(memberRef, m => asRef(m.variableType), type => this.visitType(type, q));
        return memberRef;
    }

    protected async visitMethodInvocation(invocation: J.MethodInvocation, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(invocation, m => m.select, select => this.visitRightPadded(select, q), J.Kind.RightPadded);
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
        await q.getAndSendList(multiCatch, m => m.alternatives, alt => alt.id, alt => this.visitRightPadded(alt, q), J.Kind.RightPadded);
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
        await q.getAndSend(newClass, n => n.enclosing, encl => this.visitRightPadded(encl, q), J.Kind.RightPadded);
        await q.getAndSend(newClass, n => n.new, n => this.visitSpace(n, q));
        await q.getAndSend(newClass, n => n.class, clazz => this.visit(clazz, q));
        await q.getAndSend(newClass, n => n.arguments, args => this.visitContainer(args, q));
        await q.getAndSend(newClass, n => n.body, body => this.visit(body, q));
        await q.getAndSend(newClass, n => asRef(n.constructorType), type => this.visitType(type, q));
        return newClass;
    }

    protected async visitNullableType(nullableType: J.NullableType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(nullableType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(nullableType, n => n.typeTree, type => this.visitRightPadded(type, q), J.Kind.RightPadded);
        return nullableType;
    }

    protected async visitParameterizedType(paramType: J.ParameterizedType, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(paramType, p => p.class, clazz => this.visit(clazz, q));
        await q.getAndSend(paramType, p => p.typeParameters, params => this.visitContainer(params, q));
        await q.getAndSend(paramType, p => asRef(p.type), type => this.visitType(type, q));
        return paramType;
    }

    protected async visitParentheses<T extends J>(parentheses: J.Parentheses<T>, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(parentheses, p => p.tree, tree => this.visitRightPadded(tree, q), J.Kind.RightPadded);
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
        await q.getAndSend(ternary, t => t.truePart, truePart => this.visitLeftPadded(truePart, q), J.Kind.LeftPadded);
        await q.getAndSend(ternary, t => t.falsePart, falsePart => this.visitLeftPadded(falsePart, q), J.Kind.LeftPadded);
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
        await q.getAndSend(tryStmt, t => t.finally, fin => this.visitLeftPadded(fin, q), J.Kind.LeftPadded);
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
        await q.getAndSendList(typeParams, t => t.typeParameters, p => p.id, params => this.visitRightPadded(params, q), J.Kind.RightPadded);
        return typeParams;
    }

    protected async visitUnary(unary: J.Unary, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(unary, u => u.operator, op => this.visitLeftPadded(op, q), J.Kind.LeftPadded);
        await q.getAndSend(unary, u => u.expression, expr => this.visit(expr, q));
        await q.getAndSend(unary, u => asRef(u.type), type => this.visitType(type, q));
        return unary;
    }

    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(variable, v => v.name, name => this.visit(name, q));
        // For LeftPadded<Space>, Space now uses intersection - access whitespace directly (not via .element)
        await q.getAndSendList(variable, v => v.dimensionsAfterName, d => JSON.stringify({whitespace: d.whitespace, before: d.padding.before.whitespace}), dims => this.visitLeftPadded(dims, q), J.Kind.LeftPadded);
        await q.getAndSend(variable, v => v.initializer, init => this.visitLeftPadded(init, q), J.Kind.LeftPadded);
        await q.getAndSend(variable, v => asRef(v.variableType), type => this.visitType(type, q));
        return variable;
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(whileLoop, w => w.condition, cond => this.visit(cond, q));
        await q.getAndSend(whileLoop, w => w.body, body => this.visitRightPadded(body, q), J.Kind.RightPadded);
        return whileLoop;
    }

    protected async visitWildcard(wildcard: J.Wildcard, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSend(wildcard, w => w.bound, b => this.visitLeftPadded(b, q), J.Kind.LeftPadded);
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
        await q.getAndSend(cu, c => c.packageDeclaration, pkg => this.visitRightPadded(pkg, q), J.Kind.RightPadded);
        await q.getAndSendList(cu, c => c.imports, imp => imp.id, imp => this.visitRightPadded(imp, q), J.Kind.RightPadded);
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
        await q.getAndSend(cls, c => c.extends, ext => this.visitLeftPadded(ext, q), J.Kind.LeftPadded);
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
        await q.getAndSend(block, b => b.static, s => this.visitRightPadded(s, q), J.Kind.RightPadded);
        await q.getAndSendList(block, b => b.statements, stmt => stmt.id, stmt => this.visitRightPadded(stmt, q), J.Kind.RightPadded);
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
        await q.getAndSend(method, m => m.defaultValue, def => this.visitLeftPadded(def, q), J.Kind.LeftPadded);
        await q.getAndSend(method, m => asRef(m.methodType), type => this.visitType(type, q));
        return method;
    }

    protected async visitVariableDeclarations(varDecls: J.VariableDeclarations, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(varDecls, v => v.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSendList(varDecls, v => v.modifiers, mod => mod.id, mod => this.visit(mod, q));
        await q.getAndSend(varDecls, v => v.typeExpression, type => this.visit(type, q));
        await q.getAndSend(varDecls, v => v.varargs, space => this.visitSpace(space, q));
        await q.getAndSendList(varDecls, v => v.variables, variable => variable.id, variable => this.visitRightPadded(variable, q), J.Kind.RightPadded);
        return varDecls;
    }

    protected async visitIdentifier(ident: J.Identifier, q: RpcSendQueue): Promise<J | undefined> {
        await q.getAndSendList(ident, id => id.annotations, annot => annot.id, annot => this.visit(annot, q));
        await q.getAndSend(ident, id => id.simpleName);
        await q.getAndSend(ident, id => asRef(id.type), type => this.visitType(type, q));
        await q.getAndSend(ident, id => asRef(id.fieldType), type => this.visitType(type, q));
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
        // Serialization order: before, element, paddingMarkers
        // With intersection types, element is either a separate 'element' property (for primitives)
        // or the left object itself (for tree nodes)
        const hasElement = 'element' in left;

        await q.getAndSend(left, l => l.padding.before, space => this.visitSpace(space, q));

        if (hasElement) {
            // Primitive wrapper - element is a separate property
            await q.getAndSend(left as J.PaddedPrimitive<any>, l => l.element);
        } else if (isSpace(left)) {
            // Space - the padded value IS the space (intersection type)
            await q.getAndSend(left, l => l as J.Space, space => this.visitSpace(space, q));
        } else {
            // Tree node - the padded value IS the element (intersection type)
            await q.getAndSend(left, l => l as J, elem => this.visit(elem, q));
        }

        await q.getAndSend(left, l => l.padding.markers);
        return left;
    }

    public override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcSendQueue): Promise<J.RightPadded<T>> {
        // Serialization order: element, after, paddingMarkers
        // With intersection types, element is either a separate 'element' property (for booleans)
        // or the right object itself (for tree nodes)
        const hasElement = 'element' in right;

        if (hasElement) {
            // Boolean wrapper - element is a separate property
            await q.getAndSend(right as J.PaddedPrimitive<any>, r => r.element);
        } else {
            // Tree node - the padded value IS the element (intersection type)
            await q.getAndSend(right, r => r as J, elem => this.visit(elem, q));
        }

        await q.getAndSend(right, r => r.padding.after, space => this.visitSpace(space, q));
        await q.getAndSend(right, r => r.padding.markers);
        return right;
    }

    public override async visitContainer<T extends J>(container: J.Container<T>, q: RpcSendQueue): Promise<J.Container<T>> {
        await q.getAndSend(container, c => c.before, space => this.visitSpace(space, q));
        // For tree nodes, the padded value IS the element (has id directly)
        await q.getAndSendList(container, c => c.elements, elem => elem.id, elem => this.visitRightPadded(elem, q), J.Kind.RightPadded);
        await q.getAndSend(container, c => c.markers);
        return container;
    }

    private typeVisitor = new TypeSender();

    public override async visitType(javaType: Type | undefined, q: RpcSendQueue): Promise<Type | undefined> {
        if (!javaType) {
            return undefined;
        }

        return await this.typeVisitor.visit(javaType, q);
    }
}

export class JavaReceiver extends JavaVisitor<RpcReceiveQueue> {

    protected async preVisit(j: J, q: RpcReceiveQueue): Promise<J | undefined> {
        try {
            const updates = {
                id: await q.receive(j.id),
                prefix: await q.receive(j.prefix, space => this.visitSpace(space, q)),
                markers: await q.receive(j.markers)
            };
            return updateIfChanged(j, updates);
        } catch (e: any) {
            throw e;
        }
    }

    protected async visitAnnotatedType(annotatedType: J.AnnotatedType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(annotatedType.annotations, annot => this.visit(annot, q)),
            typeExpression: await q.receive(annotatedType.typeExpression, type => this.visit(type, q))
        };
        return updateIfChanged(annotatedType, updates);
    }

    protected async visitAnnotation(annotation: J.Annotation, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotationType: await q.receive(annotation.annotationType, type => this.visit(type, q)),
            arguments: await q.receive(annotation.arguments, args => this.visitContainer(args, q))
        };
        return updateIfChanged(annotation, updates);
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            indexed: await q.receive(arrayAccess.indexed, indexed => this.visit(indexed, q)),
            dimension: await q.receive(arrayAccess.dimension, dim => this.visit(dim, q))
        };
        return updateIfChanged(arrayAccess, updates);
    }

    protected async visitArrayDimension(dimension: J.ArrayDimension, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            index: await q.receive(dimension.index, idx => this.visitRightPadded(idx, q))
        };
        return updateIfChanged(dimension, updates);
    }

    protected async visitArrayType(arrayType: J.ArrayType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            elementType: await q.receive(arrayType.elementType, type => this.visit(type, q)),
            annotations: await q.receiveListDefined(arrayType.annotations || [], annot => this.visit(annot, q)),
            dimension: await q.receive(arrayType.dimension, d => this.visitLeftPadded(d, q) as any),
            type: await q.receive(arrayType.type, type => this.visitType(type, q))
        };

        return updateIfChanged(arrayType, updates);
    }

    protected async visitAssert(assert: J.Assert, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            condition: await q.receive(assert.condition, cond => this.visit(cond, q)),
            detail: await q.receive(assert.detail, detail => this.visitOptionalLeftPadded(detail, q))
        };

        return updateIfChanged(assert, updates);
    }

    protected async visitAssignment(assignment: J.Assignment, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            variable: await q.receive(assignment.variable, variable => this.visit(variable, q)),
            assignment: await q.receive(assignment.assignment, assign => this.visitLeftPadded(assign, q)),
            type: await q.receive(assignment.type, type => this.visitType(type, q))
        };

        return updateIfChanged(assignment, updates);
    }

    protected async visitAssignmentOperation(assignOp: J.AssignmentOperation, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            variable: await q.receive(assignOp.variable, variable => this.visit(variable, q)),
            operator: await q.receive(assignOp.operator, op => this.visitLeftPadded(op, q) as any),
            assignment: await q.receive(assignOp.assignment, assign => this.visit(assign, q)),
            type: await q.receive(assignOp.type, type => this.visitType(type, q))
        };

        return updateIfChanged(assignOp, updates);
    }

    protected async visitBinary(binary: J.Binary, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            left: await q.receive(binary.left, left => this.visit(left, q)),
            operator: await q.receive(binary.operator, op => this.visitLeftPadded(op, q) as any),
            right: await q.receive(binary.right, right => this.visit(right, q)),
            type: await q.receive(binary.type, type => this.visitType(type, q))
        };

        return updateIfChanged(binary, updates);
    }

    protected async visitBreak(breakStmt: J.Break, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            label: await q.receive(breakStmt.label, label => this.visit(label, q))
        };

        return updateIfChanged(breakStmt, updates);
    }

    protected async visitCase(caseStmt: J.Case, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            type: await q.receive(caseStmt.type),
            caseLabels: await q.receive(caseStmt.caseLabels, labels => this.visitContainer(labels, q)),
            statements: await q.receive(caseStmt.statements, stmts => this.visitContainer(stmts, q)),
            body: await q.receive(caseStmt.body, body => this.visitRightPadded(body, q)),
            guard: await q.receive(caseStmt.guard, guard => this.visit(guard, q))
        };

        return updateIfChanged(caseStmt, updates);
    }

    protected async visitContinue(continueStmt: J.Continue, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            label: await q.receive(continueStmt.label, label => this.visit(label, q))
        };

        return updateIfChanged(continueStmt, updates);
    }

    protected async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            tree: await q.receive(controlParens.tree, tree => this.visitRightPadded(tree, q))
        };

        return updateIfChanged(controlParens, updates);
    }

    protected async visitDeconstructionPattern(pattern: J.DeconstructionPattern, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            deconstructor: await q.receive(pattern.deconstructor, deconstructor => this.visit(deconstructor, q)),
            nested: await q.receive(pattern.nested, nested => this.visitContainer(nested, q)),
            type: await q.receive(pattern.type, type => this.visitType(type, q))
        };

        return updateIfChanged(pattern, updates);
    }

    protected async visitDoWhileLoop(doWhile: J.DoWhileLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            body: await q.receive(doWhile.body, body => this.visitOptionalRightPadded(body, q)),
            whileCondition: await q.receive(doWhile.whileCondition, cond => this.visitLeftPadded(cond, q))
        };

        return updateIfChanged(doWhile, updates);
    }

    protected async visitEmpty(empty: J.Empty): Promise<J | undefined> {
        // no additional properties to receive
        return empty;
    }

    protected async visitEnumValueSet(enumValueSet: J.EnumValueSet, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            enums: await q.receiveListDefined(enumValueSet.enums, enumValue => this.visitRightPadded(enumValue, q)),
            terminatedWithSemicolon: await q.receive(enumValueSet.terminatedWithSemicolon)
        };

        return updateIfChanged(enumValueSet, updates);
    }

    protected async visitEnumValue(enumValue: J.EnumValue, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(enumValue.annotations, annot => this.visit(annot, q)),
            name: await q.receive(enumValue.name, name => this.visit(name, q)),
            initializer: await q.receive(enumValue.initializer, init => this.visit(init, q))
        };

        return updateIfChanged(enumValue, updates);
    }

    protected async visitErroneous(erroneous: J.Erroneous, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            text: await q.receive(erroneous.text)
        };

        return updateIfChanged(erroneous, updates);
    }

    protected async visitFieldAccess(fieldAccess: J.FieldAccess, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            target: await q.receive(fieldAccess.target, target => this.visit(target, q)),
            name: await q.receive(fieldAccess.name, name => this.visitLeftPadded(name, q)),
            type: await q.receive(fieldAccess.type, type => this.visitType(type, q))
        };

        return updateIfChanged(fieldAccess, updates);
    }

    protected async visitForEachLoop(forEachLoop: J.ForEachLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            control: await q.receive(forEachLoop.control, c => this.visit(c, q)),
            body: await q.receive(forEachLoop.body, body => this.visitRightPadded(body, q))
        };

        return updateIfChanged(forEachLoop, updates);
    }

    protected async visitForEachLoopControl(control: J.ForEachLoop.Control, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            variable: await q.receive(control.variable, variable => this.visitRightPadded(variable, q)),
            iterable: await q.receive(control.iterable, expr => this.visitRightPadded(expr, q))
        };

        return updateIfChanged(control, updates);
    }

    protected async visitForLoop(forLoop: J.ForLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            control: await q.receive(forLoop.control, c => this.visit(c, q)),
            body: await q.receive(forLoop.body, body => this.visitRightPadded(body, q))
        };

        return updateIfChanged(forLoop, updates);
    }

    protected async visitForLoopControl(control: J.ForLoop.Control, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            init: await q.receiveListDefined(control.init, init => this.visitRightPadded(init, q)),
            condition: await q.receive(control.condition, cond => this.visitRightPadded(cond, q)),
            update: await q.receiveListDefined(control.update, update => this.visitRightPadded(update, q))
        };

        return updateIfChanged(control, updates);
    }

    protected async visitIf(ifStmt: J.If, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            ifCondition: await q.receive(ifStmt.ifCondition, cond => this.visit(cond, q)),
            thenPart: await q.receive(ifStmt.thenPart, thenPart => this.visitRightPadded(thenPart, q)),
            elsePart: await q.receive(ifStmt.elsePart, elsePart => this.visit(elsePart, q))
        };

        return updateIfChanged(ifStmt, updates);
    }

    protected async visitElse(ifElse: J.If.Else, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            body: await q.receive(ifElse.body, body => this.visitRightPadded(body, q))
        };

        return updateIfChanged(ifElse, updates);
    }

    protected async visitImport(importStmt: J.Import, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            static: await q.receive(importStmt.static, s => this.visitLeftPadded(s, q) as any),
            qualid: await q.receive(importStmt.qualid, qualid => this.visit(qualid, q)),
            alias: await q.receive(importStmt.alias, alias => this.visitLeftPadded(alias, q))
        };

        return updateIfChanged(importStmt, updates);
    }

    protected async visitInstanceOf(instanceOf: J.InstanceOf, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(instanceOf.expression, expr => this.visitRightPadded(expr, q)),
            class: await q.receive(instanceOf.class, clazz => this.visit(clazz, q)),
            pattern: await q.receive(instanceOf.pattern, pattern => this.visit(pattern, q)),
            type: await q.receive(instanceOf.type, type => this.visitType(type, q)),
            modifier: await q.receive(instanceOf.modifier, mod => this.visit(mod, q))
        };
        return updateIfChanged(instanceOf, updates);
    }

    protected async visitIntersectionType(intersectionType: J.IntersectionType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            bounds: await q.receive(intersectionType.bounds, bounds => this.visitContainer(bounds, q))
        };
        return updateIfChanged(intersectionType, updates);
    }

    protected async visitLabel(label: J.Label, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            label: await q.receive(label.label, lbl => this.visitRightPadded(lbl, q)),
            statement: await q.receive(label.statement, stmt => this.visit(stmt, q))
        };
        return updateIfChanged(label, updates);
    }

    protected async visitLambda(lambda: J.Lambda, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            parameters: await q.receive(lambda.parameters, params => this.visit(params, q)),
            arrow: await q.receive(lambda.arrow, arrow => this.visitSpace(arrow, q)),
            body: await q.receive(lambda.body, body => this.visit(body, q)),
            type: await q.receive(lambda.type, type => this.visitType(type, q))
        };
        return updateIfChanged(lambda, updates);
    }

    protected async visitLambdaParameters(params: J.Lambda.Parameters, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            parenthesized: await q.receive(params.parenthesized),
            parameters: await q.receiveListDefined(params.parameters, param => this.visitRightPadded(param, q))
        };
        return updateIfChanged(params, updates);
    }

    protected async visitLiteral(literal: J.Literal, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            value: await q.receive(literal.value),
            valueSource: await q.receive(literal.valueSource),
            unicodeEscapes: await q.receiveList(literal.unicodeEscapes, async (e) => ({
                valueSourceIndex: await q.receive(e?.valueSourceIndex),
                codePoint: await q.receive(e?.codePoint),
            })),
            type: await q.receive(literal.type, type => this.visitType(type, q) as unknown as Type.Primitive)
        };
        return updateIfChanged(literal, updates);
    }

    protected async visitMemberReference(memberRef: J.MemberReference, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            containing: await q.receive(memberRef.containing, container => this.visitRightPadded(container, q)),
            typeParameters: await q.receive(memberRef.typeParameters, typeParams => this.visitContainer(typeParams, q)),
            reference: await q.receive(memberRef.reference, ref => this.visitLeftPadded(ref, q)),
            type: await q.receive(memberRef.type, type => this.visitType(type, q)),
            methodType: await q.receive(memberRef.methodType, type => this.visitType(type, q) as unknown as Type.Method),
            variableType: await q.receive(memberRef.variableType, type => this.visitType(type, q) as unknown as Type.Variable)
        };
        return updateIfChanged(memberRef, updates);
    }

    protected async visitMethodInvocation(methodInvoc: J.MethodInvocation, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            select: await q.receive(methodInvoc.select, select => this.visitRightPadded(select, q)),
            typeParameters: await q.receive(methodInvoc.typeParameters, typeParams => this.visitContainer(typeParams, q)),
            name: await q.receive(methodInvoc.name, name => this.visit(name, q)),
            arguments: await q.receive(methodInvoc.arguments, args => this.visitContainer(args, q)),
            methodType: await q.receive(methodInvoc.methodType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(methodInvoc, updates);
    }

    protected async visitModifier(modifier: J.Modifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            keyword: await q.receive(modifier.keyword),
            type: await q.receive(modifier.type),
            annotations: await q.receiveListDefined(modifier.annotations, annot => this.visit(annot, q))
        };
        return updateIfChanged(modifier, updates);
    }

    protected async visitMultiCatch(multiCatch: J.MultiCatch, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            alternatives: await q.receiveListDefined(multiCatch.alternatives, alt => this.visitRightPadded(alt, q))
        };
        return updateIfChanged(multiCatch, updates);
    }

    protected async visitNewArray(newArray: J.NewArray, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            typeExpression: await q.receive(newArray.typeExpression, type => this.visit(type, q)),
            dimensions: await q.receiveListDefined(newArray.dimensions, dim => this.visit(dim, q)),
            initializer: await q.receive(newArray.initializer, init => this.visitContainer(init, q)),
            type: await q.receive(newArray.type, type => this.visitType(type, q))
        };
        return updateIfChanged(newArray, updates);
    }

    protected async visitNewClass(newClass: J.NewClass, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            enclosing: await q.receive(newClass.enclosing, encl => this.visitRightPadded(encl, q)),
            new: await q.receive(newClass.new, new_ => this.visitSpace(new_, q)),
            class: await q.receive(newClass.class, clazz => this.visit(clazz, q)),
            arguments: await q.receive(newClass.arguments, args => this.visitContainer(args, q)),
            body: await q.receive(newClass.body, body => this.visit(body, q)),
            constructorType: await q.receive(newClass.constructorType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(newClass, updates);
    }

    protected async visitNullableType(nullableType: J.NullableType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(nullableType.annotations, annot => this.visit(annot, q)),
            typeTree: await q.receive(nullableType.typeTree, type => this.visitRightPadded(type, q))
        };
        return updateIfChanged(nullableType, updates);
    }

    protected async visitParameterizedType(paramType: J.ParameterizedType, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            class: await q.receive(paramType.class, clazz => this.visit(clazz, q)),
            typeParameters: await q.receive(paramType.typeParameters, params => this.visitContainer(params, q)),
            type: await q.receive(paramType.type, type => this.visitType(type, q))
        };
        return updateIfChanged(paramType, updates);
    }

    protected async visitParentheses<T extends J>(parentheses: J.Parentheses<T>, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            tree: await q.receive(parentheses.tree, tree => this.visitRightPadded(tree, q))
        };
        return updateIfChanged(parentheses, updates);
    }

    protected async visitParenthesizedTypeTree(parenthesizedType: J.ParenthesizedTypeTree, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(parenthesizedType.annotations, annot => this.visit(annot, q)),
            parenthesizedType: await q.receive(parenthesizedType.parenthesizedType, tree => this.visit(tree, q))
        };
        return updateIfChanged(parenthesizedType, updates);
    }

    protected async visitPrimitive(primitive: J.Primitive, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            type: await q.receive(primitive.type, type => this.visitType(type, q) as unknown as Type.Primitive)
        };
        return updateIfChanged(primitive, updates);
    }

    protected async visitSwitch(switchStmt: J.Switch, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            selector: await q.receive(switchStmt.selector, selector => this.visit(selector, q)),
            cases: await q.receive(switchStmt.cases, cases => this.visit(cases, q))
        };
        return updateIfChanged(switchStmt, updates);
    }

    protected async visitSwitchExpression(switchExpr: J.SwitchExpression, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            selector: await q.receive(switchExpr.selector, selector => this.visit(selector, q)),
            cases: await q.receive(switchExpr.cases, cases => this.visit(cases, q)),
            type: await q.receive(switchExpr.type, type => this.visitType(type, q))
        };
        return updateIfChanged(switchExpr, updates);
    }

    protected async visitTernary(ternary: J.Ternary, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            condition: await q.receive(ternary.condition, cond => this.visit(cond, q)),
            truePart: await q.receive(ternary.truePart, truePart => this.visitLeftPadded(truePart, q)),
            falsePart: await q.receive(ternary.falsePart, falsePart => this.visitLeftPadded(falsePart, q)),
            type: await q.receive(ternary.type, type => this.visitType(type, q))
        };
        return updateIfChanged(ternary, updates);
    }

    protected async visitThrow(throwStmt: J.Throw, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            exception: await q.receive(throwStmt.exception, exception => this.visit(exception, q))
        };
        return updateIfChanged(throwStmt, updates);
    }

    protected async visitTry(tryStmt: J.Try, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            resources: await q.receive(tryStmt.resources, resources => this.visitContainer(resources, q)),
            body: await q.receive(tryStmt.body, body => this.visit(body, q)),
            catches: await q.receiveListDefined(tryStmt.catches, catchBlock => this.visit(catchBlock, q)),
            finally: await q.receive(tryStmt.finally, finallyBlock => this.visitOptionalLeftPadded(finallyBlock, q))
        };
        return updateIfChanged(tryStmt, updates);
    }

    protected async visitTryResource(resource: J.Try.Resource, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            variableDeclarations: await q.receive(resource.variableDeclarations, variables => this.visit(variables, q)),
            terminatedWithSemicolon: await q.receive(resource.terminatedWithSemicolon)
        };
        return updateIfChanged(resource, updates);
    }

    protected async visitTryCatch(tryCatch: J.Try.Catch, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            parameter: await q.receive(tryCatch.parameter, param => this.visit(param, q)),
            body: await q.receive(tryCatch.body, body => this.visit(body, q))
        };
        return updateIfChanged(tryCatch, updates);
    }

    protected async visitUnary(unary: J.Unary, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            operator: await q.receive(unary.operator, op => this.visitLeftPadded(op, q) as any),
            expression: await q.receive(unary.expression, expr => this.visit(expr, q)),
            type: await q.receive(unary.type, type => this.visitType(type, q))
        };
        return updateIfChanged(unary, updates);
    }

    protected async visitUnknown(unknown: J.Unknown, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            source: await q.receive(unknown.source, source => this.visit(source, q))
        };
        return updateIfChanged(unknown, updates);
    }

    protected async visitUnknownSource(unknownSource: J.UnknownSource, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            text: await q.receive(unknownSource.text)
        };
        return updateIfChanged(unknownSource, updates);
    }

    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            name: await q.receive(variable.name, name => this.visit(name, q)),
            dimensionsAfterName: await q.receiveListDefined(variable.dimensionsAfterName, dim => this.visitLeftPadded(dim, q) as any),
            initializer: await q.receive(variable.initializer, init => this.visitOptionalLeftPadded(init, q)),
            variableType: await q.receive(variable.variableType, type => this.visitType(type, q) as unknown as Type.Variable)
        };
        return updateIfChanged(variable, updates);
    }

    protected async visitYield(yieldStmt: J.Yield, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            implicit: await q.receive(yieldStmt.implicit),
            value: await q.receive(yieldStmt.value, value => this.visit(value, q))
        };
        return updateIfChanged(yieldStmt, updates);
    }

    protected async visitTypeParameters(typeParams: J.TypeParameters, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(typeParams.annotations, annot => this.visit(annot, q)),
            typeParameters: await q.receiveListDefined(typeParams.typeParameters, param => this.visitRightPadded(param, q))
        };
        return updateIfChanged(typeParams, updates);
    }

    protected async visitReturn(returnStmt: J.Return, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive(returnStmt.expression, expr => this.visit(expr, q))
        };
        return updateIfChanged(returnStmt, updates);
    }

    protected async visitSynchronized(synchronizedStmt: J.Synchronized, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            lock: await q.receive(synchronizedStmt.lock, lock => this.visit(lock, q)),
            body: await q.receive(synchronizedStmt.body, body => this.visit(body, q))
        };
        return updateIfChanged(synchronizedStmt, updates);
    }

    protected async visitTypeCast(typeCast: J.TypeCast, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            class: await q.receive(typeCast.class, typeExpr => this.visit(typeExpr, q)),
            expression: await q.receive(typeCast.expression, expr => this.visit(expr, q))
        };
        return updateIfChanged(typeCast, updates);
    }

    protected async visitTypeParameter(typeParameter: J.TypeParameter, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(typeParameter.annotations, annot => this.visit(annot, q)),
            modifiers: await q.receiveListDefined(typeParameter.modifiers, annot => this.visit(annot, q)),
            name: await q.receive(typeParameter.name, name => this.visit(name, q)),
            bounds: await q.receive(typeParameter.bounds, bounds => this.visitContainer(bounds, q))
        };
        return updateIfChanged(typeParameter, updates);
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            condition: await q.receive(whileLoop.condition, cond => this.visit(cond, q)),
            body: await q.receive(whileLoop.body, body => this.visitOptionalRightPadded(body, q))
        };
        return updateIfChanged(whileLoop, updates);
    }

    protected async visitWildcard(wildcard: J.Wildcard, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            bound: await q.receive(wildcard.bound, bound => this.visitLeftPadded(bound, q) as any),
            boundedType: await q.receive(wildcard.boundedType, type => this.visit(type, q))
        };
        return updateIfChanged(wildcard, updates);
    }

    protected async visitCompilationUnit(cu: J.CompilationUnit, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            sourcePath: await q.receive(cu.sourcePath),
            charsetName: await q.receive(cu.charsetName),
            charsetBomMarked: await q.receive(cu.charsetBomMarked),
            checksum: await q.receive(cu.checksum),
            fileAttributes: await q.receive(cu.fileAttributes),
            packageDeclaration: await q.receive(cu.packageDeclaration, pkg => this.visitRightPadded(pkg, q)),
            imports: await q.receiveListDefined(cu.imports, imp => this.visitRightPadded(imp, q)),
            classes: await q.receiveListDefined(cu.classes, cls => this.visit(cls, q)),
            eof: await q.receive(cu.eof, space => this.visitSpace(space, q))
        };
        return updateIfChanged(cu, updates);
    }

    protected async visitPackage(pkg: J.Package, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            expression: await q.receive<Expression>(pkg.expression, expr => this.visit(expr, q)),
            annotations: await q.receiveListDefined(pkg.annotations, annot => this.visit(annot, q))
        };
        return updateIfChanged(pkg, updates);
    }

    protected async visitClassDeclaration(cls: J.ClassDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            leadingAnnotations: await q.receiveListDefined(cls.leadingAnnotations, annot => this.visit(annot, q)),
            modifiers: await q.receiveListDefined(cls.modifiers, mod => this.visit(mod, q)),
            classKind: await q.receive(cls.classKind, kind => this.visit(kind, q)),
            name: await q.receive(cls.name, name => this.visit(name, q)),
            typeParameters: await q.receive(cls.typeParameters, params => this.visitContainer(params, q)),
            primaryConstructor: await q.receive(cls.primaryConstructor, cons => this.visitContainer(cons, q)),
            extends: await q.receive(cls.extends, ext => this.visitLeftPadded(ext, q)),
            implements: await q.receive(cls.implements, impl => this.visitContainer(impl, q)),
            permitting: await q.receive(cls.permitting, perm => this.visitContainer(perm, q)),
            body: await q.receive(cls.body, body => this.visit(body, q))
        };
        return updateIfChanged(cls, updates);
    }

    protected async visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(kind.annotations, annot => this.visit(annot, q)),
            type: await q.receive(kind.type)
        };
        return updateIfChanged(kind, updates);
    }

    protected async visitBlock(block: J.Block, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            static: await q.receive(block.static, s => this.visitRightPadded(s, q) as any),
            statements: await q.receiveListDefined(block.statements, stmt => this.visitRightPadded(stmt, q)),
            end: await q.receive(block.end, space => this.visitSpace(space, q))
        };
        return updateIfChanged(block, updates);
    }

    protected async visitMethodDeclaration(method: J.MethodDeclaration, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            leadingAnnotations: await q.receiveListDefined(method.leadingAnnotations, annot => this.visit(annot, q)),
            modifiers: await q.receiveListDefined(method.modifiers, mod => this.visit(mod, q)),
            typeParameters: await q.receive(method.typeParameters, params => this.visit(params, q)),
            returnTypeExpression: await q.receive(method.returnTypeExpression, type => this.visit(type, q)),
            nameAnnotations: (await q.receiveList(method.nameAnnotations, name => this.visit(name, q)))!,
            name: await q.receive(method.name, name => this.visit(name, q)),
            parameters: await q.receive(method.parameters, params => this.visitContainer(params, q)),
            throws: await q.receive(method.throws, throws => this.visitContainer(throws, q)),
            body: await q.receive(method.body, body => this.visit(body, q)),
            defaultValue: await q.receive(method.defaultValue, def => this.visitLeftPadded(def, q)),
            methodType: await q.receive(method.methodType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(method, updates);
    }

    protected async visitVariableDeclarations(varDecls: J.VariableDeclarations, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            leadingAnnotations: await q.receiveListDefined(varDecls.leadingAnnotations, annot => this.visit(annot, q)),
            modifiers: await q.receiveListDefined(varDecls.modifiers, mod => this.visit(mod, q)),
            typeExpression: await q.receive(varDecls.typeExpression, type => this.visit(type, q)),
            varargs: await q.receive(varDecls.varargs, space => this.visitSpace(space, q)),
            variables: await q.receiveListDefined(varDecls.variables, variable => this.visitRightPadded(variable, q))
        };
        return updateIfChanged(varDecls, updates);
    }

    protected async visitIdentifier(ident: J.Identifier, q: RpcReceiveQueue): Promise<J | undefined> {
        const updates = {
            annotations: await q.receiveListDefined(ident.annotations, annot => this.visit(annot, q)),
            simpleName: await q.receive(ident.simpleName),
            type: await q.receive(ident.type, type => this.visitType(type, q)),
            fieldType: await q.receive(ident.fieldType, type => this.visitType(type, q) as any as Type.Variable)
        };
        return updateIfChanged(ident, updates);
    }

    public override async visitSpace(space: J.Space, q: RpcReceiveQueue): Promise<J.Space> {
        const updates = {
            comments: await q.receiveListDefined(space.comments, async c => {
                if (c.kind === J.Kind.TextComment) {
                    const tc = c as TextComment;
                    const commentUpdates = {
                        multiline: await q.receive(tc.multiline),
                        text: await q.receive(tc.text),
                        suffix: await q.receive(c.suffix),
                        markers: await q.receive(c.markers)
                    };
                    return updateIfChanged(tc, commentUpdates);
                } else {
                    throw new Error(`Unexpected comment type ${c.kind}`);
                }
            }),
            whitespace: await q.receive(space.whitespace)
        };
        return updateIfChanged(space, updates);
    }

    public override async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): Promise<J.LeftPadded<T>> {
        if (!left) {
            left = { element: false, padding: { before: emptySpace, markers: emptyMarkers } } as J.LeftPadded<T>;
        }

        // Check if this is a virtual RPC wrapper (kind: LeftPadded) or an intersection type
        const isRpcWrapper = (left as any).kind === J.Kind.LeftPadded;
        const rpcWrapper = left as any;

        // Deserialization order: before, element, paddingMarkers
        const beforeSpace = await q.receive(
            isRpcWrapper ? rpcWrapper.before ?? emptySpace : left.padding?.before ?? emptySpace,
            space => this.visitSpace(space, q)
        );

        // Receive element
        const elementBefore = isRpcWrapper ? rpcWrapper.element : ('element' in left ? (left as unknown as { element: T }).element : left);
        let receivedElement: T;
        if (typeof elementBefore === 'boolean' || typeof elementBefore === 'number' || typeof elementBefore === 'string' || elementBefore === undefined) {
            receivedElement = await q.receive(elementBefore ?? false) as unknown as T;
        } else if (isSpace(elementBefore)) {
            receivedElement = await q.receive(elementBefore, space => this.visitSpace(space as J.Space, q)) as unknown as T;
        } else {
            receivedElement = await q.receive(elementBefore, elem => this.visit(elem, q)) as unknown as T;
        }

        const markers = await q.receive(isRpcWrapper ? rpcWrapper.markers ?? emptyMarkers : left.padding?.markers ?? emptyMarkers);

        // Build padding preserving identity if unchanged
        const existingPadding = (receivedElement as any).padding ?? left.padding;
        const paddingUnchanged = existingPadding && beforeSpace === existingPadding.before && markers === existingPadding.markers;
        const padding = paddingUnchanged ? existingPadding : { before: beforeSpace, markers };

        // Return format based on element type
        if (typeof receivedElement === 'boolean' || typeof receivedElement === 'number' || typeof receivedElement === 'string') {
            return updateIfChanged(left as any, { element: receivedElement, padding }) as J.LeftPadded<T>;
        }
        // Space and tree nodes use intersection type
        return updateIfChanged(receivedElement as (J | J.Space) & { padding: J.Prefix }, { padding }) as J.LeftPadded<T>;
    }

    public override async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): Promise<J.RightPadded<T>> {
        if (!right) {
            right = { element: false, padding: { after: emptySpace, markers: emptyMarkers } } as J.RightPadded<T>;
        }

        // Check if this is a virtual RPC wrapper (kind: RightPadded) or an intersection type
        const isRpcWrapper = (right as any).kind === J.Kind.RightPadded;
        const rpcWrapper = right as any;

        // Deserialization order: element, after, paddingMarkers
        let receivedElement: T;
        let after: J.Space;
        let markers: Markers;

        // Receive element
        const elementBefore = isRpcWrapper ? rpcWrapper.element : ('element' in right ? (right as { element: boolean }).element : right);
        if (typeof elementBefore === 'boolean' || elementBefore === undefined) {
            receivedElement = await q.receive(elementBefore ?? false) as unknown as T;
        } else {
            receivedElement = await q.receive(elementBefore, elem => this.visit(elem, q)) as unknown as T;
        }

        // Receive padding
        after = await q.receive(isRpcWrapper ? rpcWrapper.after ?? emptySpace : right.padding?.after ?? emptySpace, space => this.visitSpace(space, q));
        markers = await q.receive(isRpcWrapper ? rpcWrapper.markers ?? emptyMarkers : right.padding?.markers ?? emptyMarkers);

        // Build padding preserving identity if unchanged
        const existingPadding = (receivedElement as any).padding ?? right.padding;
        const paddingUnchanged = existingPadding && after === existingPadding.after && markers === existingPadding.markers;
        const padding = paddingUnchanged ? existingPadding : { after, markers };

        // Return format based on element type
        if (typeof receivedElement === 'boolean') {
            return updateIfChanged(right as any, { element: receivedElement, padding }) as J.RightPadded<T>;
        }
        return updateIfChanged(receivedElement as J & { padding: J.Suffix }, { padding }) as J.RightPadded<T>;
    }

    public override async visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): Promise<J.Container<T>> {
        const updates = {
            before: await q.receive(container.before, space => this.visitSpace(space, q)),
            elements: await q.receiveListDefined(container.elements, elem => this.visitRightPadded(elem, q)),
            markers: await q.receive(container.markers)
        };
        return updateIfChanged(container, updates) as J.Container<T>;
    }

    private typeVisitor = new TypeReceiver();

    public override async visitType(javaType: Type | undefined, q: RpcReceiveQueue): Promise<Type | undefined> {
        if (!javaType) {
            return undefined;
        } else if (javaType.kind === Type.Kind.Unknown) {
            return Type.unknownType;
        }
        return await this.typeVisitor.visit(javaType, q);
    }
}

export function registerJLanguageCodecs(sourceFileType: string,
                                        receiver: JavaVisitor<RpcReceiveQueue>,
                                        sender: JavaVisitor<RpcSendQueue>,
                                        extendedKinds?: any) {
    const kinds = new Set([
        ...Object.values(J.Kind),
        ...(extendedKinds ? Object.values(extendedKinds) : [])
    ]);

    // Register codec for all Java AST node types
    for (const kind of kinds) {
        if (kind === J.Kind.Space) {
            RpcCodecs.registerCodec(kind, {
                async rpcReceive(before: J.Space, q: RpcReceiveQueue): Promise<J.Space> {
                    return (await receiver.visitSpace(before, q))!;
                },

                async rpcSend(after: J.Space, q: RpcSendQueue): Promise<void> {
                    await sender.visitSpace(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.RightPadded) {
            RpcCodecs.registerCodec(kind, {
                async rpcReceive<T extends J | boolean>(before: J.RightPadded<T>, q: RpcReceiveQueue): Promise<J.RightPadded<T>> {
                    return (await receiver.visitRightPadded(before, q))!;
                },

                async rpcSend<T extends J | boolean>(after: J.RightPadded<T>, q: RpcSendQueue): Promise<void> {
                    await sender.visitRightPadded(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.LeftPadded) {
            RpcCodecs.registerCodec(kind, {
                async rpcReceive<T extends J | Space | number | string | boolean>(before: J.LeftPadded<T>, q: RpcReceiveQueue): Promise<J.LeftPadded<T>> {
                    return (await receiver.visitLeftPadded(before, q))!;
                },

                async rpcSend<T extends J | Space | number | string | boolean>(after: J.LeftPadded<T>, q: RpcSendQueue): Promise<void> {
                    await sender.visitLeftPadded(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.Container) {
            RpcCodecs.registerCodec(kind, {
                async rpcReceive<T extends J>(before: J.Container<T>, q: RpcReceiveQueue): Promise<J.Container<T>> {
                    return (await receiver.visitContainer(before, q))!;
                },

                async rpcSend<T extends J>(after: J.Container<T>, q: RpcSendQueue): Promise<void> {
                    await sender.visitContainer(after, q);
                }
            }, sourceFileType);
        } else {
            RpcCodecs.registerCodec(kind as string, {
                async rpcReceive(before: J, q: RpcReceiveQueue): Promise<J> {
                    return (await receiver.visit(before, q))!;
                },

                async rpcSend(after: J, q: RpcSendQueue): Promise<void> {
                    await sender.visit(after, q);
                }
            }, sourceFileType);
        }
    }
}

registerJLanguageCodecs(J.Kind.CompilationUnit, new JavaReceiver(), new JavaSender());
