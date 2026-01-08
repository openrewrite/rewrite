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
import {isTree} from "../tree";
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



class TypeReceiver {
    protected visitPrimitive(primitive: Type.Primitive, q: RpcReceiveQueue): Type | undefined {
        const keyword: string = q.receive(primitive.keyword)!;
        return Type.Primitive.fromKeyword(keyword)!;
    }

    protected visitClass(cls: Type.Class, q: RpcReceiveQueue): Type | undefined {
        cls.flags = q.receive(cls.flags)!;
        cls.classKind = q.receive(cls.classKind)!;
        cls.fullyQualifiedName = q.receive(cls.fullyQualifiedName)!;
        cls.typeParameters = q.receiveList(cls.typeParameters, tp => this.visit(tp, q) as Type) || [];
        cls.supertype = q.receive(cls.supertype, st => this.visit(st, q) as Type.Class);
        cls.owningClass = q.receive(cls.owningClass, oc => this.visit(oc, q) as Type.Class);
        cls.annotations = q.receiveList(cls.annotations, a => this.visit(a, q) as Type.Annotation) || [];
        cls.interfaces = q.receiveList(cls.interfaces, i => this.visit(i, q) as Type.Class) || [];
        cls.members = q.receiveList(cls.members, m => this.visit(m, q) as Type.Variable) || [];
        cls.methods = q.receiveList(cls.methods, m => this.visit(m, q) as Type.Method) || [];
        return cls;
    }

    protected visitVariable(variable: Type.Variable, q: RpcReceiveQueue): Type | undefined {
        variable.name = q.receive(variable.name)!;
        variable.owner = q.receive(variable.owner, owner => this.visit(owner, q) as Type.FullyQualified);
        variable.type = q.receive(variable.type, t => this.visit(t, q) as Type)!;
        variable.annotations = q.receiveList(variable.annotations, a => this.visit(a, q) as Type.Annotation) || [];
        return variable;
    }

    protected visitAnnotation(annotation: Type.Annotation, q: RpcReceiveQueue): Type | undefined {
        annotation.type = q.receive(annotation.type, t => this.visit(t, q) as Type.FullyQualified)!;
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

    protected visitMethod(method: Type.Method, q: RpcReceiveQueue): Type | undefined {
        method.declaringType = q.receive(method.declaringType, dt => this.visit(dt, q) as Type.FullyQualified)!;
        method.name = q.receive(method.name)!;
        method.flags = q.receive(method.flags)!;
        method.returnType = q.receive(method.returnType, rt => this.visit(rt, q) as Type)!;
        method.parameterNames = q.receiveList(method.parameterNames) || [];
        method.parameterTypes = q.receiveList(method.parameterTypes, pt => this.visit(pt, q) as Type) || [];
        method.thrownExceptions = q.receiveList(method.thrownExceptions, et => this.visit(et, q) as Type.FullyQualified) || [];
        method.annotations = q.receiveList(method.annotations, a => this.visit(a, q) as Type.Annotation) || [];
        method.defaultValue = q.receiveList(method.defaultValue);
        method.declaredFormalTypeNames = q.receiveList(method.declaredFormalTypeNames) || [];
        return method;
    }

    protected visitArray(array: Type.Array, q: RpcReceiveQueue): Type | undefined {
        array.elemType = q.receive(array.elemType, et => this.visit(et, q) as Type)!;
        array.annotations = q.receiveList(array.annotations, ann => this.visit(ann, q) as Type.Annotation) || [];
        return array;
    }

    protected visitParameterized(parameterized: Type.Parameterized, q: RpcReceiveQueue): Type | undefined {
        parameterized.type = q.receive(parameterized.type, t => this.visit(t, q) as Type.FullyQualified)!;
        parameterized.typeParameters = q.receiveList(parameterized.typeParameters, tp => this.visit(tp, q) as Type) || [];
        return parameterized;
    }

    protected visitGenericTypeVariable(generic: Type.GenericTypeVariable, q: RpcReceiveQueue): Type | undefined {
        generic.name = q.receive(generic.name)!;
        const varianceStr = q.receive(generic.variance) as any as string;
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
        generic.bounds = q.receiveList(generic.bounds, b => this.visit(b, q)) || [];
        return generic;
    }

    protected visitUnion(union: Type.Union, q: RpcReceiveQueue): Type | undefined {
        union.bounds = q.receiveList(union.bounds, b => this.visit(b, q)) || [];
        return union;
    }

    protected visitIntersection(intersection: Type.Intersection, q: RpcReceiveQueue): Type | undefined {
        intersection.bounds = q.receiveList(intersection.bounds, b => this.visit(b, q)) || [];
        return intersection;
    }

    public visit(type: Type | undefined, q: RpcReceiveQueue): Type | undefined {
        if (!type) return undefined;

        // Dispatch to specific visitor based on kind
        switch (type.kind) {
            case Type.Kind.Primitive:
                return this.visitPrimitive(type as Type.Primitive, q);
            case Type.Kind.Class:
            case Type.Kind.ShallowClass:
                return this.visitClass(type as Type.Class, q);
            case Type.Kind.Variable:
                return this.visitVariable(type as Type.Variable, q);
            case Type.Kind.Annotation:
                return this.visitAnnotation(type as Type.Annotation, q);
            case Type.Kind.Method:
                return this.visitMethod(type as Type.Method, q);
            case Type.Kind.Array:
                return this.visitArray(type as Type.Array, q);
            case Type.Kind.Parameterized:
                return this.visitParameterized(type as Type.Parameterized, q);
            case Type.Kind.GenericTypeVariable:
                return this.visitGenericTypeVariable(type as Type.GenericTypeVariable, q);
            case Type.Kind.Union:
                return this.visitUnion(type as Type.Union, q);
            case Type.Kind.Intersection:
                return this.visitIntersection(type as Type.Intersection, q);
            case Type.Kind.Unknown:
                // Unknown types don't need special handling
                return type;
        }

        return type;
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

    private typeVisitor = new TypeSender();

    public override async visitType(javaType: Type | undefined, q: RpcSendQueue): Promise<Type | undefined> {
        if (!javaType) {
            return undefined;
        }

        return await this.typeVisitor.visit(javaType, q);
    }
}

export class JavaReceiver {

    protected preVisit(j: J, q: RpcReceiveQueue): J | undefined {
        try {
            const updates = {
                id: q.receive(j.id),
                prefix: q.receive(j.prefix, space => this.visitSpace(space, q)),
                markers: q.receive(j.markers)
            };
            return updateIfChanged(j, updates);
        } catch (e: any) {
            throw e;
        }
    }

    public visit<T extends J>(tree: T | undefined, q: RpcReceiveQueue): T | undefined {
        if (!tree) return undefined;

        // Call preVisit first
        let result: J | undefined = this.preVisit(tree, q);
        if (!result) return undefined;

        // Dispatch to specific visitor based on kind
        switch (tree.kind) {
            case J.Kind.AnnotatedType:
                result = this.visitAnnotatedType(result as J.AnnotatedType, q);
                break;
            case J.Kind.Annotation:
                result = this.visitAnnotation(result as J.Annotation, q);
                break;
            case J.Kind.ArrayAccess:
                result = this.visitArrayAccess(result as J.ArrayAccess, q);
                break;
            case J.Kind.ArrayDimension:
                result = this.visitArrayDimension(result as J.ArrayDimension, q);
                break;
            case J.Kind.ArrayType:
                result = this.visitArrayType(result as J.ArrayType, q);
                break;
            case J.Kind.Assert:
                result = this.visitAssert(result as J.Assert, q);
                break;
            case J.Kind.Assignment:
                result = this.visitAssignment(result as J.Assignment, q);
                break;
            case J.Kind.AssignmentOperation:
                result = this.visitAssignmentOperation(result as J.AssignmentOperation, q);
                break;
            case J.Kind.Binary:
                result = this.visitBinary(result as J.Binary, q);
                break;
            case J.Kind.Block:
                result = this.visitBlock(result as J.Block, q);
                break;
            case J.Kind.Break:
                result = this.visitBreak(result as J.Break, q);
                break;
            case J.Kind.Case:
                result = this.visitCase(result as J.Case, q);
                break;
            case J.Kind.ClassDeclaration:
                result = this.visitClassDeclaration(result as J.ClassDeclaration, q);
                break;
            case J.Kind.ClassDeclarationKind:
                result = this.visitClassDeclarationKind(result as J.ClassDeclaration.Kind, q);
                break;
            case J.Kind.CompilationUnit:
                result = this.visitCompilationUnit(result as J.CompilationUnit, q);
                break;
            case J.Kind.Continue:
                result = this.visitContinue(result as J.Continue, q);
                break;
            case J.Kind.ControlParentheses:
                result = this.visitControlParentheses(result as J.ControlParentheses<J>, q);
                break;
            case J.Kind.DeconstructionPattern:
                result = this.visitDeconstructionPattern(result as J.DeconstructionPattern, q);
                break;
            case J.Kind.DoWhileLoop:
                result = this.visitDoWhileLoop(result as J.DoWhileLoop, q);
                break;
            case J.Kind.Empty:
                result = this.visitEmpty(result as J.Empty);
                break;
            case J.Kind.EnumValue:
                result = this.visitEnumValue(result as J.EnumValue, q);
                break;
            case J.Kind.EnumValueSet:
                result = this.visitEnumValueSet(result as J.EnumValueSet, q);
                break;
            case J.Kind.Erroneous:
                result = this.visitErroneous(result as J.Erroneous, q);
                break;
            case J.Kind.FieldAccess:
                result = this.visitFieldAccess(result as J.FieldAccess, q);
                break;
            case J.Kind.ForEachLoop:
                result = this.visitForEachLoop(result as J.ForEachLoop, q);
                break;
            case J.Kind.ForEachLoopControl:
                result = this.visitForEachLoopControl(result as J.ForEachLoop.Control, q);
                break;
            case J.Kind.ForLoop:
                result = this.visitForLoop(result as J.ForLoop, q);
                break;
            case J.Kind.ForLoopControl:
                result = this.visitForLoopControl(result as J.ForLoop.Control, q);
                break;
            case J.Kind.Identifier:
                result = this.visitIdentifier(result as J.Identifier, q);
                break;
            case J.Kind.If:
                result = this.visitIf(result as J.If, q);
                break;
            case J.Kind.IfElse:
                result = this.visitElse(result as J.If.Else, q);
                break;
            case J.Kind.Import:
                result = this.visitImport(result as J.Import, q);
                break;
            case J.Kind.InstanceOf:
                result = this.visitInstanceOf(result as J.InstanceOf, q);
                break;
            case J.Kind.IntersectionType:
                result = this.visitIntersectionType(result as J.IntersectionType, q);
                break;
            case J.Kind.Label:
                result = this.visitLabel(result as J.Label, q);
                break;
            case J.Kind.Lambda:
                result = this.visitLambda(result as J.Lambda, q);
                break;
            case J.Kind.LambdaParameters:
                result = this.visitLambdaParameters(result as J.Lambda.Parameters, q);
                break;
            case J.Kind.Literal:
                result = this.visitLiteral(result as J.Literal, q);
                break;
            case J.Kind.MemberReference:
                result = this.visitMemberReference(result as J.MemberReference, q);
                break;
            case J.Kind.MethodDeclaration:
                result = this.visitMethodDeclaration(result as J.MethodDeclaration, q);
                break;
            case J.Kind.MethodInvocation:
                result = this.visitMethodInvocation(result as J.MethodInvocation, q);
                break;
            case J.Kind.Modifier:
                result = this.visitModifier(result as J.Modifier, q);
                break;
            case J.Kind.MultiCatch:
                result = this.visitMultiCatch(result as J.MultiCatch, q);
                break;
            case J.Kind.NamedVariable:
                result = this.visitVariable(result as J.VariableDeclarations.NamedVariable, q);
                break;
            case J.Kind.NewArray:
                result = this.visitNewArray(result as J.NewArray, q);
                break;
            case J.Kind.NewClass:
                result = this.visitNewClass(result as J.NewClass, q);
                break;
            case J.Kind.NullableType:
                result = this.visitNullableType(result as J.NullableType, q);
                break;
            case J.Kind.Package:
                result = this.visitPackage(result as J.Package, q);
                break;
            case J.Kind.ParameterizedType:
                result = this.visitParameterizedType(result as J.ParameterizedType, q);
                break;
            case J.Kind.Parentheses:
                result = this.visitParentheses(result as J.Parentheses<J>, q);
                break;
            case J.Kind.ParenthesizedTypeTree:
                result = this.visitParenthesizedTypeTree(result as J.ParenthesizedTypeTree, q);
                break;
            case J.Kind.Primitive:
                result = this.visitPrimitive(result as J.Primitive, q);
                break;
            case J.Kind.Return:
                result = this.visitReturn(result as J.Return, q);
                break;
            case J.Kind.Switch:
                result = this.visitSwitch(result as J.Switch, q);
                break;
            case J.Kind.SwitchExpression:
                result = this.visitSwitchExpression(result as J.SwitchExpression, q);
                break;
            case J.Kind.Synchronized:
                result = this.visitSynchronized(result as J.Synchronized, q);
                break;
            case J.Kind.Ternary:
                result = this.visitTernary(result as J.Ternary, q);
                break;
            case J.Kind.Throw:
                result = this.visitThrow(result as J.Throw, q);
                break;
            case J.Kind.Try:
                result = this.visitTry(result as J.Try, q);
                break;
            case J.Kind.TryResource:
                result = this.visitTryResource(result as J.Try.Resource, q);
                break;
            case J.Kind.TryCatch:
                result = this.visitTryCatch(result as J.Try.Catch, q);
                break;
            case J.Kind.TypeCast:
                result = this.visitTypeCast(result as J.TypeCast, q);
                break;
            case J.Kind.TypeParameter:
                result = this.visitTypeParameter(result as J.TypeParameter, q);
                break;
            case J.Kind.TypeParameters:
                result = this.visitTypeParameters(result as J.TypeParameters, q);
                break;
            case J.Kind.Unary:
                result = this.visitUnary(result as J.Unary, q);
                break;
            case J.Kind.Unknown:
                result = this.visitUnknown(result as J.Unknown, q);
                break;
            case J.Kind.UnknownSource:
                result = this.visitUnknownSource(result as J.UnknownSource, q);
                break;
            case J.Kind.VariableDeclarations:
                result = this.visitVariableDeclarations(result as J.VariableDeclarations, q);
                break;
            case J.Kind.WhileLoop:
                result = this.visitWhileLoop(result as J.WhileLoop, q);
                break;
            case J.Kind.Wildcard:
                result = this.visitWildcard(result as J.Wildcard, q);
                break;
            case J.Kind.Yield:
                result = this.visitYield(result as J.Yield, q);
                break;
        }
        return result as T | undefined;
    }

    protected visitAnnotatedType(annotatedType: J.AnnotatedType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotations: q.receiveListDefined(annotatedType.annotations, annot => this.visit(annot, q)),
            typeExpression: q.receive(annotatedType.typeExpression, type => this.visit(type, q))
        };
        return updateIfChanged(annotatedType, updates);
    }

    protected visitAnnotation(annotation: J.Annotation, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotationType: q.receive(annotation.annotationType, type => this.visit(type, q)),
            arguments: q.receive(annotation.arguments, args => this.visitContainer(args, q))
        };
        return updateIfChanged(annotation, updates);
    }

    protected visitArrayAccess(arrayAccess: J.ArrayAccess, q: RpcReceiveQueue): J | undefined {
        const updates = {
            indexed: q.receive(arrayAccess.indexed, indexed => this.visit(indexed, q)),
            dimension: q.receive(arrayAccess.dimension, dim => this.visit(dim, q))
        };
        return updateIfChanged(arrayAccess, updates);
    }

    protected visitArrayDimension(dimension: J.ArrayDimension, q: RpcReceiveQueue): J | undefined {
        const updates = {
            index: q.receive(dimension.index, idx => this.visitRightPadded(idx, q))
        };
        return updateIfChanged(dimension, updates);
    }

    protected visitArrayType(arrayType: J.ArrayType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            elementType: q.receive(arrayType.elementType, type => this.visit(type, q)),
            annotations: q.receiveListDefined(arrayType.annotations || [], annot => this.visit(annot, q)),
            dimension: q.receive(arrayType.dimension, d => this.visitLeftPadded(d, q)),
            type: q.receive(arrayType.type, type => this.visitType(type, q))
        };

        return updateIfChanged(arrayType, updates);
    }

    protected visitAssert(assert: J.Assert, q: RpcReceiveQueue): J | undefined {
        const updates = {
            condition: q.receive(assert.condition, cond => this.visit(cond, q)),
            detail: q.receive(assert.detail, detail => this.visitOptionalLeftPadded(detail, q))
        };

        return updateIfChanged(assert, updates);
    }

    protected visitAssignment(assignment: J.Assignment, q: RpcReceiveQueue): J | undefined {
        const updates = {
            variable: q.receive(assignment.variable, variable => this.visit(variable, q)),
            assignment: q.receive(assignment.assignment, assign => this.visitLeftPadded(assign, q)),
            type: q.receive(assignment.type, type => this.visitType(type, q))
        };

        return updateIfChanged(assignment, updates);
    }

    protected visitAssignmentOperation(assignOp: J.AssignmentOperation, q: RpcReceiveQueue): J | undefined {
        const updates = {
            variable: q.receive(assignOp.variable, variable => this.visit(variable, q)),
            operator: q.receive(assignOp.operator, op => this.visitLeftPadded(op, q)),
            assignment: q.receive(assignOp.assignment, assign => this.visit(assign, q)),
            type: q.receive(assignOp.type, type => this.visitType(type, q))
        };

        return updateIfChanged(assignOp, updates);
    }

    protected visitBinary(binary: J.Binary, q: RpcReceiveQueue): J | undefined {
        const updates = {
            left: q.receive(binary.left, left => this.visit(left, q)),
            operator: q.receive(binary.operator, op => this.visitLeftPadded(op, q)),
            right: q.receive(binary.right, right => this.visit(right, q)),
            type: q.receive(binary.type, type => this.visitType(type, q))
        };

        return updateIfChanged(binary, updates);
    }

    protected visitBreak(breakStmt: J.Break, q: RpcReceiveQueue): J | undefined {
        const updates = {
            label: q.receive(breakStmt.label, label => this.visit(label, q))
        };

        return updateIfChanged(breakStmt, updates);
    }

    protected visitCase(caseStmt: J.Case, q: RpcReceiveQueue): J | undefined {
        const updates = {
            type: q.receive(caseStmt.type),
            caseLabels: q.receive(caseStmt.caseLabels, labels => this.visitContainer(labels, q)),
            statements: q.receive(caseStmt.statements, stmts => this.visitContainer(stmts, q)),
            body: q.receive(caseStmt.body, body => this.visitRightPadded(body, q)),
            guard: q.receive(caseStmt.guard, guard => this.visit(guard, q))
        };

        return updateIfChanged(caseStmt, updates);
    }

    protected visitContinue(continueStmt: J.Continue, q: RpcReceiveQueue): J | undefined {
        const updates = {
            label: q.receive(continueStmt.label, label => this.visit(label, q))
        };

        return updateIfChanged(continueStmt, updates);
    }

    protected visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, q: RpcReceiveQueue): J | undefined {
        const updates = {
            tree: q.receive(controlParens.tree, tree => this.visitRightPadded(tree, q))
        };

        return updateIfChanged(controlParens, updates);
    }

    protected visitDeconstructionPattern(pattern: J.DeconstructionPattern, q: RpcReceiveQueue): J | undefined {
        const updates = {
            deconstructor: q.receive(pattern.deconstructor, deconstructor => this.visit(deconstructor, q)),
            nested: q.receive(pattern.nested, nested => this.visitContainer(nested, q)),
            type: q.receive(pattern.type, type => this.visitType(type, q))
        };

        return updateIfChanged(pattern, updates);
    }

    protected visitDoWhileLoop(doWhile: J.DoWhileLoop, q: RpcReceiveQueue): J | undefined {
        const updates = {
            body: q.receive(doWhile.body, body => this.visitOptionalRightPadded(body, q)),
            whileCondition: q.receive(doWhile.whileCondition, cond => this.visitLeftPadded(cond, q))
        };

        return updateIfChanged(doWhile, updates);
    }

    protected visitEmpty(empty: J.Empty): J | undefined {
        // no additional properties to receive
        return empty;
    }

    protected visitEnumValueSet(enumValueSet: J.EnumValueSet, q: RpcReceiveQueue): J | undefined {
        const updates = {
            enums: q.receiveListDefined(enumValueSet.enums, enumValue => this.visitRightPadded(enumValue, q)),
            terminatedWithSemicolon: q.receive(enumValueSet.terminatedWithSemicolon)
        };

        return updateIfChanged(enumValueSet, updates);
    }

    protected visitEnumValue(enumValue: J.EnumValue, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotations: q.receiveListDefined(enumValue.annotations, annot => this.visit(annot, q)),
            name: q.receive(enumValue.name, name => this.visit(name, q)),
            initializer: q.receive(enumValue.initializer, init => this.visit(init, q))
        };

        return updateIfChanged(enumValue, updates);
    }

    protected visitErroneous(erroneous: J.Erroneous, q: RpcReceiveQueue): J | undefined {
        const updates = {
            text: q.receive(erroneous.text)
        };

        return updateIfChanged(erroneous, updates);
    }

    protected visitFieldAccess(fieldAccess: J.FieldAccess, q: RpcReceiveQueue): J | undefined {
        const updates = {
            target: q.receive(fieldAccess.target, target => this.visit(target, q)),
            name: q.receive(fieldAccess.name, name => this.visitLeftPadded(name, q)),
            type: q.receive(fieldAccess.type, type => this.visitType(type, q))
        };

        return updateIfChanged(fieldAccess, updates);
    }

    protected visitForEachLoop(forEachLoop: J.ForEachLoop, q: RpcReceiveQueue): J | undefined {
        const updates = {
            control: q.receive(forEachLoop.control, c => this.visit(c, q)),
            body: q.receive(forEachLoop.body, body => this.visitRightPadded(body, q))
        };

        return updateIfChanged(forEachLoop, updates);
    }

    protected visitForEachLoopControl(control: J.ForEachLoop.Control, q: RpcReceiveQueue): J | undefined {
        const updates = {
            variable: q.receive(control.variable, variable => this.visitRightPadded(variable, q)),
            iterable: q.receive(control.iterable, expr => this.visitRightPadded(expr, q))
        };

        return updateIfChanged(control, updates);
    }

    protected visitForLoop(forLoop: J.ForLoop, q: RpcReceiveQueue): J | undefined {
        const updates = {
            control: q.receive(forLoop.control, c => this.visit(c, q)),
            body: q.receive(forLoop.body, body => this.visitRightPadded(body, q))
        };

        return updateIfChanged(forLoop, updates);
    }

    protected visitForLoopControl(control: J.ForLoop.Control, q: RpcReceiveQueue): J | undefined {
        const updates = {
            init: q.receiveListDefined(control.init, init => this.visitRightPadded(init, q)),
            condition: q.receive(control.condition, cond => this.visitRightPadded(cond, q)),
            update: q.receiveListDefined(control.update, update => this.visitRightPadded(update, q))
        };

        return updateIfChanged(control, updates);
    }

    protected visitIf(ifStmt: J.If, q: RpcReceiveQueue): J | undefined {
        const updates = {
            ifCondition: q.receive(ifStmt.ifCondition, cond => this.visit(cond, q)),
            thenPart: q.receive(ifStmt.thenPart, thenPart => this.visitRightPadded(thenPart, q)),
            elsePart: q.receive(ifStmt.elsePart, elsePart => this.visit(elsePart, q))
        };

        return updateIfChanged(ifStmt, updates);
    }

    protected visitElse(ifElse: J.If.Else, q: RpcReceiveQueue): J | undefined {
        const updates = {
            body: q.receive(ifElse.body, body => this.visitRightPadded(body, q))
        };

        return updateIfChanged(ifElse, updates);
    }

    protected visitImport(importStmt: J.Import, q: RpcReceiveQueue): J | undefined {
        const updates = {
            static: q.receive(importStmt.static, s => this.visitLeftPadded(s, q)),
            qualid: q.receive(importStmt.qualid, qualid => this.visit(qualid, q)),
            alias: q.receive(importStmt.alias, alias => this.visitLeftPadded(alias, q))
        };

        return updateIfChanged(importStmt, updates);
    }

    protected visitInstanceOf(instanceOf: J.InstanceOf, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(instanceOf.expression, expr => this.visitRightPadded(expr, q)),
            class: q.receive(instanceOf.class, clazz => this.visit(clazz, q)),
            pattern: q.receive(instanceOf.pattern, pattern => this.visit(pattern, q)),
            type: q.receive(instanceOf.type, type => this.visitType(type, q)),
            modifier: q.receive(instanceOf.modifier, mod => this.visit(mod, q))
        };
        return updateIfChanged(instanceOf, updates);
    }

    protected visitIntersectionType(intersectionType: J.IntersectionType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            bounds: q.receive(intersectionType.bounds, bounds => this.visitContainer(bounds, q))
        };
        return updateIfChanged(intersectionType, updates);
    }

    protected visitLabel(label: J.Label, q: RpcReceiveQueue): J | undefined {
        const updates = {
            label: q.receive(label.label, lbl => this.visitRightPadded(lbl, q)),
            statement: q.receive(label.statement, stmt => this.visit(stmt, q))
        };
        return updateIfChanged(label, updates);
    }

    protected visitLambda(lambda: J.Lambda, q: RpcReceiveQueue): J | undefined {
        const updates = {
            parameters: q.receive(lambda.parameters, params => this.visit(params, q)),
            arrow: q.receive(lambda.arrow, arrow => this.visitSpace(arrow, q)),
            body: q.receive(lambda.body, body => this.visit(body, q)),
            type: q.receive(lambda.type, type => this.visitType(type, q))
        };
        return updateIfChanged(lambda, updates);
    }

    protected visitLambdaParameters(params: J.Lambda.Parameters, q: RpcReceiveQueue): J | undefined {
        const updates = {
            parenthesized: q.receive(params.parenthesized),
            parameters: q.receiveListDefined(params.parameters, param => this.visitRightPadded(param, q))
        };
        return updateIfChanged(params, updates);
    }

    protected visitLiteral(literal: J.Literal, q: RpcReceiveQueue): J | undefined {
        const updates = {
            value: q.receive(literal.value),
            valueSource: q.receive(literal.valueSource),
            unicodeEscapes: q.receiveList(literal.unicodeEscapes),
            type: q.receive(literal.type, type => this.visitType(type, q) as unknown as Type.Primitive)
        };
        return updateIfChanged(literal, updates);
    }

    protected visitMemberReference(memberRef: J.MemberReference, q: RpcReceiveQueue): J | undefined {
        const updates = {
            containing: q.receive(memberRef.containing, container => this.visitRightPadded(container, q)),
            typeParameters: q.receive(memberRef.typeParameters, typeParams => this.visitContainer(typeParams, q)),
            reference: q.receive(memberRef.reference, ref => this.visitLeftPadded(ref, q)),
            type: q.receive(memberRef.type, type => this.visitType(type, q)),
            methodType: q.receive(memberRef.methodType, type => this.visitType(type, q) as unknown as Type.Method),
            variableType: q.receive(memberRef.variableType, type => this.visitType(type, q) as unknown as Type.Variable)
        };
        return updateIfChanged(memberRef, updates);
    }

    protected visitMethodInvocation(methodInvoc: J.MethodInvocation, q: RpcReceiveQueue): J | undefined {
        const updates = {
            select: q.receive(methodInvoc.select, select => this.visitRightPadded(select, q)),
            typeParameters: q.receive(methodInvoc.typeParameters, typeParams => this.visitContainer(typeParams, q)),
            name: q.receive(methodInvoc.name, name => this.visit(name, q)),
            arguments: q.receive(methodInvoc.arguments, args => this.visitContainer(args, q)),
            methodType: q.receive(methodInvoc.methodType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(methodInvoc, updates);
    }

    protected visitModifier(modifier: J.Modifier, q: RpcReceiveQueue): J | undefined {
        const updates = {
            keyword: q.receive(modifier.keyword),
            type: q.receive(modifier.type),
            annotations: q.receiveListDefined(modifier.annotations, annot => this.visit(annot, q))
        };
        return updateIfChanged(modifier, updates);
    }

    protected visitMultiCatch(multiCatch: J.MultiCatch, q: RpcReceiveQueue): J | undefined {
        const updates = {
            alternatives: q.receiveListDefined(multiCatch.alternatives, alt => this.visitRightPadded(alt, q))
        };
        return updateIfChanged(multiCatch, updates);
    }

    protected visitNewArray(newArray: J.NewArray, q: RpcReceiveQueue): J | undefined {
        const updates = {
            typeExpression: q.receive(newArray.typeExpression, type => this.visit(type, q)),
            dimensions: q.receiveListDefined(newArray.dimensions, dim => this.visit(dim, q)),
            initializer: q.receive(newArray.initializer, init => this.visitContainer(init, q)),
            type: q.receive(newArray.type, type => this.visitType(type, q))
        };
        return updateIfChanged(newArray, updates);
    }

    protected visitNewClass(newClass: J.NewClass, q: RpcReceiveQueue): J | undefined {
        const updates = {
            enclosing: q.receive(newClass.enclosing, encl => this.visitRightPadded(encl, q)),
            new: q.receive(newClass.new, new_ => this.visitSpace(new_, q)),
            class: q.receive(newClass.class, clazz => this.visit(clazz, q)),
            arguments: q.receive(newClass.arguments, args => this.visitContainer(args, q)),
            body: q.receive(newClass.body, body => this.visit(body, q)),
            constructorType: q.receive(newClass.constructorType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(newClass, updates);
    }

    protected visitNullableType(nullableType: J.NullableType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotations: q.receiveListDefined(nullableType.annotations, annot => this.visit(annot, q)),
            typeTree: q.receive(nullableType.typeTree, type => this.visitRightPadded(type, q))
        };
        return updateIfChanged(nullableType, updates);
    }

    protected visitParameterizedType(paramType: J.ParameterizedType, q: RpcReceiveQueue): J | undefined {
        const updates = {
            class: q.receive(paramType.class, clazz => this.visit(clazz, q)),
            typeParameters: q.receive(paramType.typeParameters, params => this.visitContainer(params, q)),
            type: q.receive(paramType.type, type => this.visitType(type, q))
        };
        return updateIfChanged(paramType, updates);
    }

    protected visitParentheses<T extends J>(parentheses: J.Parentheses<T>, q: RpcReceiveQueue): J | undefined {
        const updates = {
            tree: q.receive(parentheses.tree, tree => this.visitRightPadded(tree, q))
        };
        return updateIfChanged(parentheses, updates);
    }

    protected visitParenthesizedTypeTree(parenthesizedType: J.ParenthesizedTypeTree, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotations: q.receiveListDefined(parenthesizedType.annotations, annot => this.visit(annot, q)),
            parenthesizedType: q.receive(parenthesizedType.parenthesizedType, tree => this.visit(tree, q))
        };
        return updateIfChanged(parenthesizedType, updates);
    }

    protected visitPrimitive(primitive: J.Primitive, q: RpcReceiveQueue): J | undefined {
        const updates = {
            type: q.receive(primitive.type, type => this.visitType(type, q) as unknown as Type.Primitive)
        };
        return updateIfChanged(primitive, updates);
    }

    protected visitSwitch(switchStmt: J.Switch, q: RpcReceiveQueue): J | undefined {
        const updates = {
            selector: q.receive(switchStmt.selector, selector => this.visit(selector, q)),
            cases: q.receive(switchStmt.cases, cases => this.visit(cases, q))
        };
        return updateIfChanged(switchStmt, updates);
    }

    protected visitSwitchExpression(switchExpr: J.SwitchExpression, q: RpcReceiveQueue): J | undefined {
        const updates = {
            selector: q.receive(switchExpr.selector, selector => this.visit(selector, q)),
            cases: q.receive(switchExpr.cases, cases => this.visit(cases, q)),
            type: q.receive(switchExpr.type, type => this.visitType(type, q))
        };
        return updateIfChanged(switchExpr, updates);
    }

    protected visitTernary(ternary: J.Ternary, q: RpcReceiveQueue): J | undefined {
        const updates = {
            condition: q.receive(ternary.condition, cond => this.visit(cond, q)),
            truePart: q.receive(ternary.truePart, truePart => this.visitLeftPadded(truePart, q)),
            falsePart: q.receive(ternary.falsePart, falsePart => this.visitLeftPadded(falsePart, q)),
            type: q.receive(ternary.type, type => this.visitType(type, q))
        };
        return updateIfChanged(ternary, updates);
    }

    protected visitThrow(throwStmt: J.Throw, q: RpcReceiveQueue): J | undefined {
        const updates = {
            exception: q.receive(throwStmt.exception, exception => this.visit(exception, q))
        };
        return updateIfChanged(throwStmt, updates);
    }

    protected visitTry(tryStmt: J.Try, q: RpcReceiveQueue): J | undefined {
        const updates = {
            resources: q.receive(tryStmt.resources, resources => this.visitContainer(resources, q)),
            body: q.receive(tryStmt.body, body => this.visit(body, q)),
            catches: q.receiveListDefined(tryStmt.catches, catchBlock => this.visit(catchBlock, q)),
            finally: q.receive(tryStmt.finally, finallyBlock => this.visitOptionalLeftPadded(finallyBlock, q))
        };
        return updateIfChanged(tryStmt, updates);
    }

    protected visitTryResource(resource: J.Try.Resource, q: RpcReceiveQueue): J | undefined {
        const updates = {
            variableDeclarations: q.receive(resource.variableDeclarations, variables => this.visit(variables, q)),
            terminatedWithSemicolon: q.receive(resource.terminatedWithSemicolon)
        };
        return updateIfChanged(resource, updates);
    }

    protected visitTryCatch(tryCatch: J.Try.Catch, q: RpcReceiveQueue): J | undefined {
        const updates = {
            parameter: q.receive(tryCatch.parameter, param => this.visit(param, q)),
            body: q.receive(tryCatch.body, body => this.visit(body, q))
        };
        return updateIfChanged(tryCatch, updates);
    }

    protected visitUnary(unary: J.Unary, q: RpcReceiveQueue): J | undefined {
        const updates = {
            operator: q.receive(unary.operator, op => this.visitLeftPadded(op, q)),
            expression: q.receive(unary.expression, expr => this.visit(expr, q)),
            type: q.receive(unary.type, type => this.visitType(type, q))
        };
        return updateIfChanged(unary, updates);
    }

    protected visitUnknown(unknown: J.Unknown, q: RpcReceiveQueue): J | undefined {
        const updates = {
            source: q.receive(unknown.source, source => this.visit(source, q))
        };
        return updateIfChanged(unknown, updates);
    }

    protected visitUnknownSource(unknownSource: J.UnknownSource, q: RpcReceiveQueue): J | undefined {
        const updates = {
            text: q.receive(unknownSource.text)
        };
        return updateIfChanged(unknownSource, updates);
    }

    protected visitVariable(variable: J.VariableDeclarations.NamedVariable, q: RpcReceiveQueue): J | undefined {
        const updates = {
            name: q.receive(variable.name, name => this.visit(name, q)),
            dimensionsAfterName: q.receiveListDefined(variable.dimensionsAfterName, dim => this.visitLeftPadded(dim, q)),
            initializer: q.receive(variable.initializer, init => this.visitOptionalLeftPadded(init, q)),
            variableType: q.receive(variable.variableType, type => this.visitType(type, q) as unknown as Type.Variable)
        };
        return updateIfChanged(variable, updates);
    }

    protected visitYield(yieldStmt: J.Yield, q: RpcReceiveQueue): J | undefined {
        const updates = {
            implicit: q.receive(yieldStmt.implicit),
            value: q.receive(yieldStmt.value, value => this.visit(value, q))
        };
        return updateIfChanged(yieldStmt, updates);
    }

    protected visitTypeParameters(typeParams: J.TypeParameters, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotations: q.receiveListDefined(typeParams.annotations, annot => this.visit(annot, q)),
            typeParameters: q.receiveListDefined(typeParams.typeParameters, param => this.visitRightPadded(param, q))
        };
        return updateIfChanged(typeParams, updates);
    }

    protected visitReturn(returnStmt: J.Return, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive(returnStmt.expression, expr => this.visit(expr, q))
        };
        return updateIfChanged(returnStmt, updates);
    }

    protected visitSynchronized(synchronizedStmt: J.Synchronized, q: RpcReceiveQueue): J | undefined {
        const updates = {
            lock: q.receive(synchronizedStmt.lock, lock => this.visit(lock, q)),
            body: q.receive(synchronizedStmt.body, body => this.visit(body, q))
        };
        return updateIfChanged(synchronizedStmt, updates);
    }

    protected visitTypeCast(typeCast: J.TypeCast, q: RpcReceiveQueue): J | undefined {
        const updates = {
            class: q.receive(typeCast.class, typeExpr => this.visit(typeExpr, q)),
            expression: q.receive(typeCast.expression, expr => this.visit(expr, q))
        };
        return updateIfChanged(typeCast, updates);
    }

    protected visitTypeParameter(typeParameter: J.TypeParameter, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotations: q.receiveListDefined(typeParameter.annotations, annot => this.visit(annot, q)),
            modifiers: q.receiveListDefined(typeParameter.modifiers, annot => this.visit(annot, q)),
            name: q.receive(typeParameter.name, name => this.visit(name, q)),
            bounds: q.receive(typeParameter.bounds, bounds => this.visitContainer(bounds, q))
        };
        return updateIfChanged(typeParameter, updates);
    }

    protected visitWhileLoop(whileLoop: J.WhileLoop, q: RpcReceiveQueue): J | undefined {
        const updates = {
            condition: q.receive(whileLoop.condition, cond => this.visit(cond, q)),
            body: q.receive(whileLoop.body, body => this.visitOptionalRightPadded(body, q))
        };
        return updateIfChanged(whileLoop, updates);
    }

    protected visitWildcard(wildcard: J.Wildcard, q: RpcReceiveQueue): J | undefined {
        const updates = {
            bound: q.receive(wildcard.bound, bound => this.visitLeftPadded(bound, q)),
            boundedType: q.receive(wildcard.boundedType, type => this.visit(type, q))
        };
        return updateIfChanged(wildcard, updates);
    }

    protected visitCompilationUnit(cu: J.CompilationUnit, q: RpcReceiveQueue): J | undefined {
        const updates = {
            sourcePath: q.receive(cu.sourcePath),
            charsetName: q.receive(cu.charsetName),
            charsetBomMarked: q.receive(cu.charsetBomMarked),
            checksum: q.receive(cu.checksum),
            fileAttributes: q.receive(cu.fileAttributes),
            packageDeclaration: q.receive(cu.packageDeclaration, pkg => this.visitRightPadded(pkg, q)),
            imports: q.receiveListDefined(cu.imports, imp => this.visitRightPadded(imp, q)),
            classes: q.receiveListDefined(cu.classes, cls => this.visit(cls, q)),
            eof: q.receive(cu.eof, space => this.visitSpace(space, q))
        };
        return updateIfChanged(cu, updates);
    }

    protected visitPackage(pkg: J.Package, q: RpcReceiveQueue): J | undefined {
        const updates = {
            expression: q.receive<Expression>(pkg.expression, expr => this.visit(expr, q)),
            annotations: q.receiveListDefined(pkg.annotations, annot => this.visit(annot, q))
        };
        return updateIfChanged(pkg, updates);
    }

    protected visitClassDeclaration(cls: J.ClassDeclaration, q: RpcReceiveQueue): J | undefined {
        const updates = {
            leadingAnnotations: q.receiveListDefined(cls.leadingAnnotations, annot => this.visit(annot, q)),
            modifiers: q.receiveListDefined(cls.modifiers, mod => this.visit(mod, q)),
            classKind: q.receive(cls.classKind, kind => this.visit(kind, q)),
            name: q.receive(cls.name, name => this.visit(name, q)),
            typeParameters: q.receive(cls.typeParameters, params => this.visitContainer(params, q)),
            primaryConstructor: q.receive(cls.primaryConstructor, cons => this.visitContainer(cons, q)),
            extends: q.receive(cls.extends, ext => this.visitLeftPadded(ext, q)),
            implements: q.receive(cls.implements, impl => this.visitContainer(impl, q)),
            permitting: q.receive(cls.permitting, perm => this.visitContainer(perm, q)),
            body: q.receive(cls.body, body => this.visit(body, q))
        };
        return updateIfChanged(cls, updates);
    }

    protected visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, q: RpcReceiveQueue): J | undefined {
        const updates = {
            annotations: q.receiveListDefined(kind.annotations, annot => this.visit(annot, q)),
            type: q.receive(kind.type)
        };
        return updateIfChanged(kind, updates);
    }

    protected visitBlock(block: J.Block, q: RpcReceiveQueue): J | undefined {
        const updates = {
            static: q.receive(block.static, s => this.visitRightPadded(s, q)),
            statements: q.receiveListDefined(block.statements, stmt => this.visitRightPadded(stmt, q)),
            end: q.receive(block.end, space => this.visitSpace(space, q))
        };
        return updateIfChanged(block, updates);
    }

    protected visitMethodDeclaration(method: J.MethodDeclaration, q: RpcReceiveQueue): J | undefined {
        const updates = {
            leadingAnnotations: q.receiveListDefined(method.leadingAnnotations, annot => this.visit(annot, q)),
            modifiers: q.receiveListDefined(method.modifiers, mod => this.visit(mod, q)),
            typeParameters: q.receive(method.typeParameters, params => this.visit(params, q)),
            returnTypeExpression: q.receive(method.returnTypeExpression, type => this.visit(type, q)),
            nameAnnotations: (q.receiveList(method.nameAnnotations, name => this.visit(name, q)))!,
            name: q.receive(method.name, name => this.visit(name, q)),
            parameters: q.receive(method.parameters, params => this.visitContainer(params, q)),
            throws: q.receive(method.throws, throws => this.visitContainer(throws, q)),
            body: q.receive(method.body, body => this.visit(body, q)),
            defaultValue: q.receive(method.defaultValue, def => this.visitLeftPadded(def, q)),
            methodType: q.receive(method.methodType, type => this.visitType(type, q) as unknown as Type.Method)
        };
        return updateIfChanged(method, updates);
    }

    protected visitVariableDeclarations(varDecls: J.VariableDeclarations, q: RpcReceiveQueue): J | undefined {
        const updates = {
            leadingAnnotations: q.receiveListDefined(varDecls.leadingAnnotations, annot => this.visit(annot, q)),
            modifiers: q.receiveListDefined(varDecls.modifiers, mod => this.visit(mod, q)),
            typeExpression: q.receive(varDecls.typeExpression, type => this.visit(type, q)),
            varargs: q.receive(varDecls.varargs, space => this.visitSpace(space, q)),
            variables: q.receiveListDefined(varDecls.variables, variable => this.visitRightPadded(variable, q))
        };
        return updateIfChanged(varDecls, updates);
    }

    protected visitIdentifier(ident: J.Identifier, q: RpcReceiveQueue): J | undefined {
        // inlined `updateIfChanged()` for performance
        const annotations = q.receiveListDefined(ident.annotations, annot => this.visit(annot, q));
        const simpleName = q.receive(ident.simpleName)!;
        const type = q.receive(ident.type, type => this.visitType(type, q));
        const fieldType = q.receive(ident.fieldType, type => this.visitType(type, q) as any as Type.Variable);
        return annotations === ident.annotations && simpleName === ident.simpleName &&
               type === ident.type && fieldType === ident.fieldType
            ? ident
            : { ...ident, annotations, simpleName, type, fieldType } as J.Identifier;
    }

    public visitSpace(space: J.Space, q: RpcReceiveQueue): J.Space {
        // inlined `updateIfChanged()` for performance
        const comments = q.receiveListDefined(space.comments, c => {
            if (c.kind === J.Kind.TextComment) {
                const tc = c as TextComment;
                const multiline = q.receive(tc.multiline);
                const text = q.receive(tc.text);
                const suffix = q.receive(c.suffix);
                const markers = q.receive(c.markers);
                return multiline === tc.multiline && text === tc.text &&
                       suffix === tc.suffix && markers === tc.markers
                    ? tc
                    : { ...tc, multiline, text, suffix, markers } as TextComment;
            } else {
                throw new Error(`Unexpected comment type ${c.kind}`);
            }
        });
        const whitespace = q.receive(space.whitespace)!;
        return comments === space.comments && whitespace === space.whitespace
            ? space
            : { ...space, comments, whitespace };
    }

    public visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): J.LeftPadded<T> {
        if (!left) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty left padding");
        }

        const updates = {
            before: q.receive(left.before, space => this.visitSpace(space, q)),
            element: q.receive(left.element, elem => {
                if (isSpace(elem)) {
                    return this.visitSpace(elem as J.Space, q) as any as T;
                } else if (typeof elem === 'object' && elem.kind) {
                    // FIXME find a better way to check if it is a `Tree`
                    return this.visit(elem as J, q) as any as T;
                }
                return elem;
            }),
            markers: q.receive(left.markers)
        };
        return updateIfChanged(left, updates) as J.LeftPadded<T>;
    }

    public visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): J.RightPadded<T> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty right padding");
        }

        // inlined `updateIfChanged()` for performance
        const element = q.receive(right.element, elem => {
            if (isSpace(elem)) {
                return this.visitSpace(elem as J.Space, q) as any as T;
            } else if (typeof elem === 'object' && elem.kind) {
                // FIXME find a better way to check if it is a `Tree`
                return this.visit(elem as J, q) as any as T;
            }
            return elem as any as T;
        });
        const after = q.receive(right.after, space => this.visitSpace(space, q))!;
        const markers = q.receive(right.markers)!;
        return element === right.element && after === right.after && markers === right.markers
            ? right
            : { ...right, element, after, markers } as J.RightPadded<T>;
    }

    public visitOptionalLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T> | undefined, q: RpcReceiveQueue): J.LeftPadded<T> | undefined {
        if (!left) return undefined;
        return this.visitLeftPadded(left, q);
    }

    public visitOptionalRightPadded<T extends J | boolean>(right: J.RightPadded<T> | undefined, q: RpcReceiveQueue): J.RightPadded<T> | undefined {
        if (!right) return undefined;
        return this.visitRightPadded(right, q);
    }

    public visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): J.Container<T> {
        const updates = {
            before: q.receive(container.before, space => this.visitSpace(space, q)),
            elements: q.receiveListDefined(container.elements, elem => this.visitRightPadded(elem, q)),
            markers: q.receive(container.markers)
        };
        return updateIfChanged(container, updates) as J.Container<T>;
    }

    private typeVisitor = new TypeReceiver();

    public visitType(javaType: Type | undefined, q: RpcReceiveQueue): Type | undefined {
        if (!javaType) {
            return undefined;
        } else if (javaType.kind === Type.Kind.Unknown) {
            return Type.unknownType;
        }
        return this.typeVisitor.visit(javaType, q);
    }
}

export function registerJLanguageCodecs(sourceFileType: string,
                                        receiver: JavaReceiver,
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
                rpcReceive(before: J.Space, q: RpcReceiveQueue): J.Space {
                    return (receiver.visitSpace(before, q))!;
                },

                async rpcSend(after: J.Space, q: RpcSendQueue): Promise<void> {
                    await sender.visitSpace(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.RightPadded) {
            RpcCodecs.registerCodec(kind, {
                rpcReceive<T extends J | boolean>(before: J.RightPadded<T>, q: RpcReceiveQueue): J.RightPadded<T> {
                    return (receiver.visitRightPadded(before, q))!;
                },

                async rpcSend<T extends J | boolean>(after: J.RightPadded<T>, q: RpcSendQueue): Promise<void> {
                    await sender.visitRightPadded(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.LeftPadded) {
            RpcCodecs.registerCodec(kind, {
                rpcReceive<T extends J | Space | number | string | boolean>(before: J.LeftPadded<T>, q: RpcReceiveQueue): J.LeftPadded<T> {
                    return (receiver.visitLeftPadded(before, q))!;
                },

                async rpcSend<T extends J | Space | number | string | boolean>(after: J.LeftPadded<T>, q: RpcSendQueue): Promise<void> {
                    await sender.visitLeftPadded(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.Container) {
            RpcCodecs.registerCodec(kind, {
                rpcReceive<T extends J>(before: J.Container<T>, q: RpcReceiveQueue): J.Container<T> {
                    return (receiver.visitContainer(before, q))!;
                },

                async rpcSend<T extends J>(after: J.Container<T>, q: RpcSendQueue): Promise<void> {
                    await sender.visitContainer(after, q);
                }
            }, sourceFileType);
        } else {
            RpcCodecs.registerCodec(kind as string, {
                rpcReceive(before: J, q: RpcReceiveQueue): J {
                    return (receiver.visit(before, q))!;
                },

                async rpcSend(after: J, q: RpcSendQueue): Promise<void> {
                    await sender.visit(after, q);
                }
            }, sourceFileType);
        }
    }
}

registerJLanguageCodecs(J.Kind.CompilationUnit, new JavaReceiver(), new JavaSender());
