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
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {Expression, isSpace, J, sharedSpace, TextComment} from "./tree";
import {isTree, Tree} from "../tree";
import {Type} from "./type";
import {updateIfChanged} from "../util";
import Space = J.Space;

/**
 * A Space that {@link sharedSpace} has an instance for goes as a ref to that instance, so each repeat is a
 * ref-only message. Other values go as they are, since a ref stays in both peers' tables for the whole connection.
 */
export function spaceRef<T>(value: T): T {
    const shared = isSpace(value) && value.comments.length === 0 ? sharedSpace(value.whitespace) : undefined;
    return shared === undefined ? value : asRef(shared) as T;
}

/**
 * Dispatches a type to the sender's or receiver's method for its kind, like {@link JavaRpcVisitor}.
 */
abstract class TypeRpcVisitor<Q> {
    visit<T extends Type>(type: T | undefined, q: Q): T | undefined {
        if (!type) {
            return undefined;
        }
        switch (type.kind) {
            case Type.Kind.Array:
                return this.visitArray(type as unknown as Type.Array, q) as T | undefined;
            case Type.Kind.Annotation:
                return this.visitAnnotation(type as unknown as Type.Annotation, q) as T | undefined;
            case Type.Kind.Class:
            case Type.Kind.ShallowClass:
                return this.visitClass(type as unknown as Type.Class, q) as T | undefined;
            case Type.Kind.GenericTypeVariable:
                return this.visitGenericTypeVariable(type as unknown as Type.GenericTypeVariable, q) as T | undefined;
            case Type.Kind.Intersection:
                return this.visitIntersection(type as unknown as Type.Intersection, q) as T | undefined;
            case Type.Kind.Union:
                return this.visitUnion(type as unknown as Type.Union, q) as T | undefined;
            case Type.Kind.Parameterized:
                return this.visitParameterized(type as unknown as Type.Parameterized, q) as T | undefined;
            case Type.Kind.Primitive:
                return this.visitPrimitive(type as unknown as Type.Primitive, q) as T | undefined;
            case Type.Kind.Method:
                return this.visitMethod(type as unknown as Type.Method, q) as T | undefined;
            case Type.Kind.Variable:
                return this.visitVariable(type as unknown as Type.Variable, q) as T | undefined;
            default:
                return type;
        }
    }

    protected abstract visitArray(array: Type.Array, q: Q): Type | undefined;

    protected abstract visitAnnotation(annotation: Type.Annotation, q: Q): Type | undefined;

    protected abstract visitClass(cls: Type.Class, q: Q): Type | undefined;

    protected abstract visitGenericTypeVariable(generic: Type.GenericTypeVariable, q: Q): Type | undefined;

    protected abstract visitIntersection(intersection: Type.Intersection, q: Q): Type | undefined;

    protected abstract visitUnion(union: Type.Union, q: Q): Type | undefined;

    protected abstract visitParameterized(parameterized: Type.Parameterized, q: Q): Type | undefined;

    protected abstract visitPrimitive(primitive: Type.Primitive, q: Q): Type | undefined;

    protected abstract visitMethod(method: Type.Method, q: Q): Type | undefined;

    protected abstract visitVariable(variable: Type.Variable, q: Q): Type | undefined;
}

/**
 * Dispatches a J element to the sender's or receiver's method for its kind. The RPC codecs are
 * synchronous, which the asynchronous `JavaVisitor` cannot be.
 */
export abstract class JavaRpcVisitor<Q> {
    visit<R extends J>(tree: Tree | undefined, q: Q): R | undefined {
        if (!tree) {
            return undefined;
        }
        const t = this.preVisit(tree as J, q);
        return t === undefined ? undefined : this.accept(t, q) as R | undefined;
    }

    visitDefined<R extends J>(tree: Tree, q: Q): R {
        return this.visit<R>(tree, q)!;
    }

    protected abstract preVisit(tree: J, q: Q): J | undefined;

    protected accept(t: J, q: Q): J | undefined {
        switch (t.kind) {
            case J.Kind.AnnotatedType:
                return this.visitAnnotatedType(t as unknown as J.AnnotatedType, q);
            case J.Kind.Annotation:
                return this.visitAnnotation(t as unknown as J.Annotation, q);
            case J.Kind.ArrayAccess:
                return this.visitArrayAccess(t as unknown as J.ArrayAccess, q);
            case J.Kind.ArrayDimension:
                return this.visitArrayDimension(t as unknown as J.ArrayDimension, q);
            case J.Kind.ArrayType:
                return this.visitArrayType(t as unknown as J.ArrayType, q);
            case J.Kind.Assert:
                return this.visitAssert(t as unknown as J.Assert, q);
            case J.Kind.Assignment:
                return this.visitAssignment(t as unknown as J.Assignment, q);
            case J.Kind.AssignmentOperation:
                return this.visitAssignmentOperation(t as unknown as J.AssignmentOperation, q);
            case J.Kind.Binary:
                return this.visitBinary(t as unknown as J.Binary, q);
            case J.Kind.Block:
                return this.visitBlock(t as unknown as J.Block, q);
            case J.Kind.Break:
                return this.visitBreak(t as unknown as J.Break, q);
            case J.Kind.Case:
                return this.visitCase(t as unknown as J.Case, q);
            case J.Kind.ClassDeclaration:
                return this.visitClassDeclaration(t as unknown as J.ClassDeclaration, q);
            case J.Kind.ClassDeclarationKind:
                return this.visitClassDeclarationKind(t as unknown as J.ClassDeclaration.Kind, q);
            case J.Kind.CompilationUnit:
                return this.visitCompilationUnit(t as unknown as J.CompilationUnit, q);
            case J.Kind.Continue:
                return this.visitContinue(t as unknown as J.Continue, q);
            case J.Kind.ControlParentheses:
                return this.visitControlParentheses(t as unknown as J.ControlParentheses<J>, q);
            case J.Kind.DeconstructionPattern:
                return this.visitDeconstructionPattern(t as unknown as J.DeconstructionPattern, q);
            case J.Kind.DoWhileLoop:
                return this.visitDoWhileLoop(t as unknown as J.DoWhileLoop, q);
            case J.Kind.Empty:
                return this.visitEmpty(t as unknown as J.Empty, q);
            case J.Kind.EnumValue:
                return this.visitEnumValue(t as unknown as J.EnumValue, q);
            case J.Kind.EnumValueSet:
                return this.visitEnumValueSet(t as unknown as J.EnumValueSet, q);
            case J.Kind.Erroneous:
                return this.visitErroneous(t as unknown as J.Erroneous, q);
            case J.Kind.FieldAccess:
                return this.visitFieldAccess(t as unknown as J.FieldAccess, q);
            case J.Kind.ForEachLoop:
                return this.visitForEachLoop(t as unknown as J.ForEachLoop, q);
            case J.Kind.ForEachLoopControl:
                return this.visitForEachLoopControl(t as unknown as J.ForEachLoop.Control, q);
            case J.Kind.ForLoop:
                return this.visitForLoop(t as unknown as J.ForLoop, q);
            case J.Kind.ForLoopControl:
                return this.visitForLoopControl(t as unknown as J.ForLoop.Control, q);
            case J.Kind.Identifier:
                return this.visitIdentifier(t as unknown as J.Identifier, q);
            case J.Kind.If:
                return this.visitIf(t as unknown as J.If, q);
            case J.Kind.IfElse:
                return this.visitElse(t as unknown as J.If.Else, q);
            case J.Kind.Import:
                return this.visitImport(t as unknown as J.Import, q);
            case J.Kind.InstanceOf:
                return this.visitInstanceOf(t as unknown as J.InstanceOf, q);
            case J.Kind.IntersectionType:
                return this.visitIntersectionType(t as unknown as J.IntersectionType, q);
            case J.Kind.Label:
                return this.visitLabel(t as unknown as J.Label, q);
            case J.Kind.Lambda:
                return this.visitLambda(t as unknown as J.Lambda, q);
            case J.Kind.LambdaParameters:
                return this.visitLambdaParameters(t as unknown as J.Lambda.Parameters, q);
            case J.Kind.Literal:
                return this.visitLiteral(t as unknown as J.Literal, q);
            case J.Kind.MemberReference:
                return this.visitMemberReference(t as unknown as J.MemberReference, q);
            case J.Kind.MethodDeclaration:
                return this.visitMethodDeclaration(t as unknown as J.MethodDeclaration, q);
            case J.Kind.MethodInvocation:
                return this.visitMethodInvocation(t as unknown as J.MethodInvocation, q);
            case J.Kind.Modifier:
                return this.visitModifier(t as unknown as J.Modifier, q);
            case J.Kind.MultiCatch:
                return this.visitMultiCatch(t as unknown as J.MultiCatch, q);
            case J.Kind.NewArray:
                return this.visitNewArray(t as unknown as J.NewArray, q);
            case J.Kind.NewClass:
                return this.visitNewClass(t as unknown as J.NewClass, q);
            case J.Kind.NullableType:
                return this.visitNullableType(t as unknown as J.NullableType, q);
            case J.Kind.Package:
                return this.visitPackage(t as unknown as J.Package, q);
            case J.Kind.ParameterizedType:
                return this.visitParameterizedType(t as unknown as J.ParameterizedType, q);
            case J.Kind.Parentheses:
                return this.visitParentheses(t as unknown as J.Parentheses<J>, q);
            case J.Kind.ParenthesizedTypeTree:
                return this.visitParenthesizedTypeTree(t as unknown as J.ParenthesizedTypeTree, q);
            case J.Kind.Primitive:
                return this.visitPrimitive(t as unknown as J.Primitive, q);
            case J.Kind.Return:
                return this.visitReturn(t as unknown as J.Return, q);
            case J.Kind.Switch:
                return this.visitSwitch(t as unknown as J.Switch, q);
            case J.Kind.SwitchExpression:
                return this.visitSwitchExpression(t as unknown as J.SwitchExpression, q);
            case J.Kind.Synchronized:
                return this.visitSynchronized(t as unknown as J.Synchronized, q);
            case J.Kind.Ternary:
                return this.visitTernary(t as unknown as J.Ternary, q);
            case J.Kind.Throw:
                return this.visitThrow(t as unknown as J.Throw, q);
            case J.Kind.Try:
                return this.visitTry(t as unknown as J.Try, q);
            case J.Kind.TryResource:
                return this.visitTryResource(t as unknown as J.Try.Resource, q);
            case J.Kind.TryCatch:
                return this.visitTryCatch(t as unknown as J.Try.Catch, q);
            case J.Kind.TypeCast:
                return this.visitTypeCast(t as unknown as J.TypeCast, q);
            case J.Kind.TypeParameter:
                return this.visitTypeParameter(t as unknown as J.TypeParameter, q);
            case J.Kind.TypeParameters:
                return this.visitTypeParameters(t as unknown as J.TypeParameters, q);
            case J.Kind.Unary:
                return this.visitUnary(t as unknown as J.Unary, q);
            case J.Kind.Unknown:
                return this.visitUnknown(t as unknown as J.Unknown, q);
            case J.Kind.UnknownSource:
                return this.visitUnknownSource(t as unknown as J.UnknownSource, q);
            case J.Kind.VariableDeclarations:
                return this.visitVariableDeclarations(t as unknown as J.VariableDeclarations, q);
            case J.Kind.NamedVariable:
                return this.visitVariable(t as unknown as J.VariableDeclarations.NamedVariable, q);
            case J.Kind.WhileLoop:
                return this.visitWhileLoop(t as unknown as J.WhileLoop, q);
            case J.Kind.Wildcard:
                return this.visitWildcard(t as unknown as J.Wildcard, q);
            case J.Kind.Yield:
                return this.visitYield(t as unknown as J.Yield, q);
            default:
                throw new Error(`No RPC codec for kind ${t.kind}`);
        }
    }

    protected abstract visitAnnotatedType(tree: J.AnnotatedType, q: Q): J | undefined;
    protected abstract visitAnnotation(tree: J.Annotation, q: Q): J | undefined;
    protected abstract visitArrayAccess(tree: J.ArrayAccess, q: Q): J | undefined;
    protected abstract visitArrayDimension(tree: J.ArrayDimension, q: Q): J | undefined;
    protected abstract visitArrayType(tree: J.ArrayType, q: Q): J | undefined;
    protected abstract visitAssert(tree: J.Assert, q: Q): J | undefined;
    protected abstract visitAssignment(tree: J.Assignment, q: Q): J | undefined;
    protected abstract visitAssignmentOperation(tree: J.AssignmentOperation, q: Q): J | undefined;
    protected abstract visitBinary(tree: J.Binary, q: Q): J | undefined;
    protected abstract visitBlock(tree: J.Block, q: Q): J | undefined;
    protected abstract visitBreak(tree: J.Break, q: Q): J | undefined;
    protected abstract visitCase(tree: J.Case, q: Q): J | undefined;
    protected abstract visitClassDeclaration(tree: J.ClassDeclaration, q: Q): J | undefined;
    protected abstract visitClassDeclarationKind(tree: J.ClassDeclaration.Kind, q: Q): J | undefined;
    protected abstract visitCompilationUnit(tree: J.CompilationUnit, q: Q): J | undefined;
    protected abstract visitContinue(tree: J.Continue, q: Q): J | undefined;
    protected abstract visitControlParentheses(tree: J.ControlParentheses<J>, q: Q): J | undefined;
    protected abstract visitDeconstructionPattern(tree: J.DeconstructionPattern, q: Q): J | undefined;
    protected abstract visitDoWhileLoop(tree: J.DoWhileLoop, q: Q): J | undefined;
    protected abstract visitEmpty(tree: J.Empty, q: Q): J | undefined;
    protected abstract visitEnumValue(tree: J.EnumValue, q: Q): J | undefined;
    protected abstract visitEnumValueSet(tree: J.EnumValueSet, q: Q): J | undefined;
    protected abstract visitErroneous(tree: J.Erroneous, q: Q): J | undefined;
    protected abstract visitFieldAccess(tree: J.FieldAccess, q: Q): J | undefined;
    protected abstract visitForEachLoop(tree: J.ForEachLoop, q: Q): J | undefined;
    protected abstract visitForEachLoopControl(tree: J.ForEachLoop.Control, q: Q): J | undefined;
    protected abstract visitForLoop(tree: J.ForLoop, q: Q): J | undefined;
    protected abstract visitForLoopControl(tree: J.ForLoop.Control, q: Q): J | undefined;
    protected abstract visitIdentifier(tree: J.Identifier, q: Q): J | undefined;
    protected abstract visitIf(tree: J.If, q: Q): J | undefined;
    protected abstract visitElse(tree: J.If.Else, q: Q): J | undefined;
    protected abstract visitImport(tree: J.Import, q: Q): J | undefined;
    protected abstract visitInstanceOf(tree: J.InstanceOf, q: Q): J | undefined;
    protected abstract visitIntersectionType(tree: J.IntersectionType, q: Q): J | undefined;
    protected abstract visitLabel(tree: J.Label, q: Q): J | undefined;
    protected abstract visitLambda(tree: J.Lambda, q: Q): J | undefined;
    protected abstract visitLambdaParameters(tree: J.Lambda.Parameters, q: Q): J | undefined;
    protected abstract visitLiteral(tree: J.Literal, q: Q): J | undefined;
    protected abstract visitMemberReference(tree: J.MemberReference, q: Q): J | undefined;
    protected abstract visitMethodDeclaration(tree: J.MethodDeclaration, q: Q): J | undefined;
    protected abstract visitMethodInvocation(tree: J.MethodInvocation, q: Q): J | undefined;
    protected abstract visitModifier(tree: J.Modifier, q: Q): J | undefined;
    protected abstract visitMultiCatch(tree: J.MultiCatch, q: Q): J | undefined;
    protected abstract visitNewArray(tree: J.NewArray, q: Q): J | undefined;
    protected abstract visitNewClass(tree: J.NewClass, q: Q): J | undefined;
    protected abstract visitNullableType(tree: J.NullableType, q: Q): J | undefined;
    protected abstract visitPackage(tree: J.Package, q: Q): J | undefined;
    protected abstract visitParameterizedType(tree: J.ParameterizedType, q: Q): J | undefined;
    protected abstract visitParentheses(tree: J.Parentheses<J>, q: Q): J | undefined;
    protected abstract visitParenthesizedTypeTree(tree: J.ParenthesizedTypeTree, q: Q): J | undefined;
    protected abstract visitPrimitive(tree: J.Primitive, q: Q): J | undefined;
    protected abstract visitReturn(tree: J.Return, q: Q): J | undefined;
    protected abstract visitSwitch(tree: J.Switch, q: Q): J | undefined;
    protected abstract visitSwitchExpression(tree: J.SwitchExpression, q: Q): J | undefined;
    protected abstract visitSynchronized(tree: J.Synchronized, q: Q): J | undefined;
    protected abstract visitTernary(tree: J.Ternary, q: Q): J | undefined;
    protected abstract visitThrow(tree: J.Throw, q: Q): J | undefined;
    protected abstract visitTry(tree: J.Try, q: Q): J | undefined;
    protected abstract visitTryResource(tree: J.Try.Resource, q: Q): J | undefined;
    protected abstract visitTryCatch(tree: J.Try.Catch, q: Q): J | undefined;
    protected abstract visitTypeCast(tree: J.TypeCast, q: Q): J | undefined;
    protected abstract visitTypeParameter(tree: J.TypeParameter, q: Q): J | undefined;
    protected abstract visitTypeParameters(tree: J.TypeParameters, q: Q): J | undefined;
    protected abstract visitUnary(tree: J.Unary, q: Q): J | undefined;
    protected abstract visitUnknown(tree: J.Unknown, q: Q): J | undefined;
    protected abstract visitUnknownSource(tree: J.UnknownSource, q: Q): J | undefined;
    protected abstract visitVariableDeclarations(tree: J.VariableDeclarations, q: Q): J | undefined;
    protected abstract visitVariable(tree: J.VariableDeclarations.NamedVariable, q: Q): J | undefined;
    protected abstract visitWhileLoop(tree: J.WhileLoop, q: Q): J | undefined;
    protected abstract visitWildcard(tree: J.Wildcard, q: Q): J | undefined;
    protected abstract visitYield(tree: J.Yield, q: Q): J | undefined;

    abstract visitSpace(space: J.Space, q: Q): J.Space;

    abstract visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: Q): J.LeftPadded<T>;

    abstract visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: Q): J.RightPadded<T>;

    abstract visitContainer<T extends J>(container: J.Container<T>, q: Q): J.Container<T>;

    abstract visitType(javaType: Type | undefined, q: Q): Type | undefined;

    visitOptionalLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T> | undefined, q: Q): J.LeftPadded<T> | undefined {
        return left ? this.visitLeftPadded(left, q) : undefined;
    }

    visitOptionalRightPadded<T extends J | boolean>(right: J.RightPadded<T> | undefined, q: Q): J.RightPadded<T> | undefined {
        return right ? this.visitRightPadded(right, q) : undefined;
    }
}


class TypeSender extends TypeRpcVisitor<RpcSendQueue> {
    protected visitPrimitive(primitive: Type.Primitive, q: RpcSendQueue): Type | undefined {
        q.getAndSend(primitive, p => p.keyword);
        return primitive;
    }

    protected visitClass(cls: Type.Class, q: RpcSendQueue): Type | undefined {
        q.getAndSend(cls, c => c.flags);
        q.getAndSend(cls, c => c.classKind);
        q.getAndSend(cls, c => c.fullyQualifiedName);
        q.getAndSendList(cls, c => (c.typeParameters || []).map(t => asRef(t)), t => Type.signature(t), t => this.visit(t, q));
        q.getAndSend(cls, c => asRef(c.supertype), st => this.visit(st, q));
        q.getAndSend(cls, c => asRef(c.owningClass), oc => this.visit(oc, q));
        q.getAndSendList(cls, c => (c.annotations || []).map(a => asRef(a)), t => Type.signature(t), a => this.visit(a, q));
        q.getAndSendList(cls, c => (c.interfaces || []).map(i => asRef(i)), t => Type.signature(t), i => this.visit(i, q));
        q.getAndSendList(cls, c => (c.members || []).map(m => asRef(m)), t => Type.signature(t), m => this.visit(m, q));
        q.getAndSendList(cls, c => (c.methods || []).map(m => asRef(m)), t => Type.signature(t), m => this.visit(m, q));
        return cls;
    }

    protected visitVariable(variable: Type.Variable, q: RpcSendQueue): Type | undefined {
        q.getAndSend(variable, v => v.flags);
        q.getAndSend(variable, v => v.name);
        q.getAndSend(variable, v => v.owner ? asRef(v.owner) : undefined, owner => this.visit(owner, q));
        q.getAndSend(variable, v => asRef(v.type), t => this.visit(t, q));
        q.getAndSendList(variable, v => (v.annotations || []).map(v2 => asRef(v2)), t => Type.signature(t), a => this.visit(a, q));
        return variable;
    }

    protected visitAnnotation(annotation: Type.Annotation, q: RpcSendQueue): Type | undefined {
        q.getAndSend(annotation, a => asRef(a.type), t => this.visit(t, q));
        q.getAndSendList(
            annotation,
            a => (a.values || []).map(v => asRef(v)),
            v => Type.signature(v.element),
            v => {
                q.getAndSend(v, e => asRef(e.element), elem => this.visit(elem, q));
                if (v.kind === Type.Kind.ArrayElementValue) {
                    const array = v as Type.Annotation.ArrayElementValue;
                    // constantValues are sent as raw JSON-native values; numeric subtypes
                    // (int/long/float/double) and char/string distinctions are not preserved.
                    q.getAndSendList(
                        array,
                        a => a.constantValues,
                        v => v == null ? "null" : v.toString(),
                    );
                    q.getAndSendList(
                        array,
                        a => (a.referenceValues || []).map(r => asRef(r)),
                        t => Type.signature(t),
                        t => this.visit(t, q),
                    );
                } else {
                    const single = v as Type.Annotation.SingleElementValue;
                    q.getAndSend(single, s => s.constantValue);
                    q.getAndSend(single, s => asRef(s.referenceValue), ref => this.visit(ref, q));
                }
            },
        );
        return annotation;
    }

    protected visitMethod(method: Type.Method, q: RpcSendQueue): Type | undefined {
        q.getAndSend(method, m => asRef(m.declaringType), dt => this.visit(dt, q));
        q.getAndSend(method, m => m.name);
        q.getAndSend(method, m => m.flags);
        q.getAndSend(method, m => asRef(m.returnType), rt => this.visit(rt, q));
        q.getAndSendList(method, m => m.parameterNames || [], v => v);
        q.getAndSendList(method, m => (m.parameterTypes || []).map(t => asRef(t)), t => Type.signature(t), pt => this.visit(pt, q));
        q.getAndSendList(method, m => (m.thrownExceptions || []).map(t => asRef(t)), t => Type.signature(t), et => this.visit(et, q));
        q.getAndSendList(method, m => (m.annotations || []).map(a => asRef(a)), t => Type.signature(t), a => this.visit(a, q));
        q.getAndSendList(method, m => m.defaultValue || undefined, v => v);
        q.getAndSendList(method, m => m.declaredFormalTypeNames || [], v => v);
        return method;
    }

    protected visitArray(array: Type.Array, q: RpcSendQueue): Type | undefined {
        q.getAndSend(array, a => asRef(a.elemType), et => this.visit(et, q));
        q.getAndSendList(array, a => (a.annotations || []).map(ann => asRef(ann)), t => Type.signature(t), ann => this.visit(ann, q));
        return array;
    }

    protected visitParameterized(parameterized: Type.Parameterized, q: RpcSendQueue): Type | undefined {
        q.getAndSend(parameterized, p => asRef(p.type), t => this.visit(t, q));
        q.getAndSendList(parameterized, p => (p.typeParameters || []).map(tp => asRef(tp)), t => Type.signature(t), tp => this.visit(tp, q));
        return parameterized;
    }

    protected visitGenericTypeVariable(generic: Type.GenericTypeVariable, q: RpcSendQueue): Type | undefined {
        q.getAndSend(generic, g => g.name);
        // Convert TypeScript enum to Java enum string
        q.getAndSend(generic, g => {
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
        q.getAndSendList(generic, g => (g.bounds || []).map(b => asRef(b)), t => Type.signature(t), b => this.visit(b, q));
        return generic;
    }

    protected visitUnion(union: Type.Union, q: RpcSendQueue): Type | undefined {
        q.getAndSendList(union, u => (u.bounds || []).map(b => asRef(b)), t => Type.signature(t), b => this.visit(b, q));
        return union;
    }

    protected visitIntersection(intersection: Type.Intersection, q: RpcSendQueue): Type | undefined {
        q.getAndSendList(intersection, i => (i.bounds || []).map(b => asRef(b)), t => Type.signature(t), b => this.visit(b, q));
        return intersection;
    }
}

class TypeReceiver extends TypeRpcVisitor<RpcReceiveQueue> {
    preVisit(_type: Type, _q: RpcReceiveQueue): Type | undefined {
        // Don't call default preVisit to avoid circular references
        return _type;
    }

    postVisit(_type: Type, _q: RpcReceiveQueue): Type | undefined {
        // Don't call default postVisit to avoid circular references
        return _type;
    }

    protected visitPrimitive(primitive: Type.Primitive, q: RpcReceiveQueue): Type | undefined {
        const keyword: string = q.receive(primitive.keyword);
        return Type.Primitive.fromKeyword(keyword)!;
    }

    protected visitClass(cls: Type.Class, q: RpcReceiveQueue): Type | undefined {
        cls.flags = q.receive(cls.flags);
        cls.classKind = q.receive(cls.classKind);
        cls.fullyQualifiedName = q.receive(cls.fullyQualifiedName);
        cls.typeParameters = q.receiveList(cls.typeParameters, tp => this.visit(tp, q)) || [];
        cls.supertype = q.receive(cls.supertype, st => this.visit(st, q));
        cls.owningClass = q.receive(cls.owningClass, oc => this.visit(oc, q));
        cls.annotations = q.receiveList(cls.annotations, a => this.visit(a, q)) || [];
        cls.interfaces = q.receiveList(cls.interfaces, i => this.visit(i, q)) || [];
        cls.members = q.receiveList(cls.members, m => this.visit(m, q)) || [];
        cls.methods = q.receiveList(cls.methods, m => this.visit(m, q)) || [];
        return cls;
    }

    protected visitVariable(variable: Type.Variable, q: RpcReceiveQueue): Type | undefined {
        variable.flags = q.receive(variable.flags);
        variable.name = q.receive(variable.name);
        variable.owner = q.receive(variable.owner, owner => this.visit(owner, q));
        variable.type = q.receive(variable.type, t => this.visit(t, q));
        variable.annotations = q.receiveList(variable.annotations, a => this.visit(a, q)) || [];
        return variable;
    }

    protected visitAnnotation(annotation: Type.Annotation, q: RpcReceiveQueue): Type | undefined {
        annotation.type = q.receive(annotation.type, t => this.visit(t, q));
        annotation.values = (q.receiveList(annotation.values, v => {
            const element = q.receive((v as Type.Annotation.ElementValue).element, elem => this.visit(elem, q));
            if (v.kind === Type.Kind.ArrayElementValue) {
                const array = v as Type.Annotation.ArrayElementValue;
                const constantValues = q.receiveList(array.constantValues);
                const referenceValues = q.receiveList(array.referenceValues, r => this.visit(r, q));
                return {
                    kind: Type.Kind.ArrayElementValue,
                    element,
                    constantValues,
                    referenceValues,
                } as Type.Annotation.ArrayElementValue;
            }
            const single = v as Type.Annotation.SingleElementValue;
            const constantValue = q.receive(single.constantValue);
            const referenceValue = q.receive(single.referenceValue, ref => this.visit(ref, q));
            return {
                kind: Type.Kind.SingleElementValue,
                element,
                constantValue,
                referenceValue,
            } as Type.Annotation.SingleElementValue;
        })) || [];
        return annotation;
    }

    protected visitMethod(method: Type.Method, q: RpcReceiveQueue): Type | undefined {
        method.declaringType = q.receive(method.declaringType, dt => this.visit(dt, q));
        method.name = q.receive(method.name);
        method.flags = q.receive(method.flags);
        method.returnType = q.receive(method.returnType, rt => this.visit(rt, q));
        method.parameterNames = q.receiveList(method.parameterNames) || [];
        method.parameterTypes = q.receiveList(method.parameterTypes, pt => this.visit(pt, q)) || [];
        method.thrownExceptions = q.receiveList(method.thrownExceptions, et => this.visit(et, q)) || [];
        method.annotations = q.receiveList(method.annotations, a => this.visit(a, q)) || [];
        method.defaultValue = q.receiveList(method.defaultValue);
        method.declaredFormalTypeNames = q.receiveList(method.declaredFormalTypeNames) || [];
        return method;
    }

    protected visitArray(array: Type.Array, q: RpcReceiveQueue): Type | undefined {
        array.elemType = q.receive(array.elemType, et => this.visit(et, q));
        array.annotations = q.receiveList(array.annotations, ann => this.visit(ann, q)) || [];
        return array;
    }

    protected visitParameterized(parameterized: Type.Parameterized, q: RpcReceiveQueue): Type | undefined {
        parameterized.type = q.receive(parameterized.type, t => this.visit(t, q));
        parameterized.typeParameters = q.receiveList(parameterized.typeParameters, tp => this.visit(tp, q)) || [];
        return parameterized;
    }

    protected visitGenericTypeVariable(generic: Type.GenericTypeVariable, q: RpcReceiveQueue): Type | undefined {
        generic.name = q.receive(generic.name);
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
}

export class JavaSender extends JavaRpcVisitor<RpcSendQueue> {

    protected preVisit(j: J, q: RpcSendQueue): J | undefined {
        q.getAndSend(j, j2 => j2.id);
        q.getAndSend(j, j2 => spaceRef(j2.prefix), space => this.visitSpace(space, q));
        q.getAndSend(j, j2 => j2.markers);
        return j;
    }

    protected visitAnnotatedType(annotatedType: J.AnnotatedType, q: RpcSendQueue): J | undefined {
        q.getAndSendList(annotatedType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSend(annotatedType, a => a.typeExpression, type => this.visit(type, q));
        return annotatedType;
    }

    protected visitAnnotation(annotation: J.Annotation, q: RpcSendQueue): J | undefined {
        q.getAndSend(annotation, a => a.annotationType, type => this.visit(type, q));
        q.getAndSend(annotation, a => a.arguments, args => this.visitContainer(args, q));
        return annotation;
    }

    protected visitArrayAccess(arrayAccess: J.ArrayAccess, q: RpcSendQueue): J | undefined {
        q.getAndSend(arrayAccess, a => a.indexed, indexed => this.visit(indexed, q));
        q.getAndSend(arrayAccess, a => a.dimension, dim => this.visit(dim, q));
        return arrayAccess;
    }

    protected visitArrayDimension(dimension: J.ArrayDimension, q: RpcSendQueue): J | undefined {
        q.getAndSend(dimension, d => d.index, idx => this.visitRightPadded(idx, q));
        return dimension;
    }

    protected visitArrayType(arrayType: J.ArrayType, q: RpcSendQueue): J | undefined {
        q.getAndSend(arrayType, a => a.elementType, type => this.visit(type, q));
        q.getAndSendList(arrayType, a => a.annotations || [], annot => annot.id, annot => this.visit(annot, q));
        q.getAndSend(arrayType, a => a.dimension, d => this.visitLeftPadded(d, q));
        q.getAndSend(arrayType, a => asRef(a.type), type => this.visitType(type, q));
        return arrayType;
    }

    protected visitAssert(assert: J.Assert, q: RpcSendQueue): J | undefined {
        q.getAndSend(assert, a => a.condition, cond => this.visit(cond, q));
        q.getAndSend(assert, a => a.detail, detail => this.visitLeftPadded(detail, q));
        return assert;
    }

    protected visitAssignment(assignment: J.Assignment, q: RpcSendQueue): J | undefined {
        q.getAndSend(assignment, a => a.variable, variable => this.visit(variable, q));
        q.getAndSend(assignment, a => a.assignment, assign => this.visitLeftPadded(assign, q));
        q.getAndSend(assignment, a => asRef(a.type), type => this.visitType(type, q));
        return assignment;
    }

    protected visitAssignmentOperation(assignOp: J.AssignmentOperation, q: RpcSendQueue): J | undefined {
        q.getAndSend(assignOp, a => a.variable, variable => this.visit(variable, q));
        q.getAndSend(assignOp, a => a.operator, op => this.visitLeftPadded(op, q));
        q.getAndSend(assignOp, a => a.assignment, assign => this.visit(assign, q));
        q.getAndSend(assignOp, a => asRef(a.type), type => this.visitType(type, q));
        return assignOp;
    }

    protected visitBinary(binary: J.Binary, q: RpcSendQueue): J | undefined {
        q.getAndSend(binary, b => b.left, left => this.visit(left, q));
        q.getAndSend(binary, b => b.operator, op => this.visitLeftPadded(op, q));
        q.getAndSend(binary, b => b.right, right => this.visit(right, q));
        q.getAndSend(binary, a => asRef(a.type), type => this.visitType(type, q));
        return binary;
    }

    protected visitBreak(breakStmt: J.Break, q: RpcSendQueue): J | undefined {
        q.getAndSend(breakStmt, b => b.label, label => this.visit(label, q));
        return breakStmt;
    }

    protected visitCase(caseStmt: J.Case, q: RpcSendQueue): J | undefined {
        q.getAndSend(caseStmt, c => c.type);
        q.getAndSend(caseStmt, c => c.caseLabels, labels => this.visitContainer(labels, q));
        q.getAndSend(caseStmt, c => c.statements, stmts => this.visitContainer(stmts, q));
        q.getAndSend(caseStmt, c => c.body, body => this.visitRightPadded(body, q));
        q.getAndSend(caseStmt, c => c.guard, guard => this.visit(guard, q));
        return caseStmt;
    }

    protected visitContinue(continueStmt: J.Continue, q: RpcSendQueue): J | undefined {
        q.getAndSend(continueStmt, c => c.label, label => this.visit(label, q));
        return continueStmt;
    }

    protected visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, q: RpcSendQueue): J | undefined {
        q.getAndSend(controlParens, c => c.tree, tree => this.visitRightPadded(tree, q));
        return controlParens;
    }

    protected visitDeconstructionPattern(pattern: J.DeconstructionPattern, q: RpcSendQueue): J | undefined {
        q.getAndSend(pattern, p => p.deconstructor, deconstructor => this.visit(deconstructor, q));
        q.getAndSend(pattern, p => p.nested, nested => this.visitContainer(nested, q));
        q.getAndSend(pattern, p => asRef(p.type), type => this.visitType(type, q));
        return pattern;
    }

    protected visitDoWhileLoop(doWhile: J.DoWhileLoop, q: RpcSendQueue): J | undefined {
        q.getAndSend(doWhile, d => d.body, body => this.visitRightPadded(body, q));
        q.getAndSend(doWhile, d => d.whileCondition, cond => this.visitLeftPadded(cond, q));
        return doWhile;
    }

    protected visitEmpty(empty: J.Empty, _q: RpcSendQueue): J | undefined {
        // No additional properties to send
        return empty;
    }

    protected visitEnumValueSet(enumValueSet: J.EnumValueSet, q: RpcSendQueue): J | undefined {
        q.getAndSendList(enumValueSet, e => e.enums, enumValue => enumValue.element.id, enumValue => this.visitRightPadded(enumValue, q));
        q.getAndSend(enumValueSet, e => e.terminatedWithSemicolon);
        return enumValueSet;
    }

    protected visitEnumValue(enumValue: J.EnumValue, q: RpcSendQueue): J | undefined {
        q.getAndSendList(enumValue, e => e.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSend(enumValue, e => e.name, name => this.visit(name, q));
        q.getAndSend(enumValue, e => e.initializer, init => this.visit(init, q));
        return enumValue;
    }

    protected visitErroneous(erroneous: J.Erroneous, q: RpcSendQueue): J | undefined {
        q.getAndSend(erroneous, e => e.text);
        return erroneous;
    }

    protected visitFieldAccess(fieldAccess: J.FieldAccess, q: RpcSendQueue): J | undefined {
        q.getAndSend(fieldAccess, f => f.target, target => this.visit(target, q));
        q.getAndSend(fieldAccess, f => f.name, name => this.visitLeftPadded(name, q));
        q.getAndSend(fieldAccess, a => asRef(a.type), type => this.visitType(type, q));
        return fieldAccess;
    }

    protected visitForEachLoop(forEach: J.ForEachLoop, q: RpcSendQueue): J | undefined {
        q.getAndSend(forEach, f => f.control, control => this.visit(control, q));
        q.getAndSend(forEach, f => f.body, body => this.visitRightPadded(body, q));
        return forEach;
    }

    protected visitForEachLoopControl(control: J.ForEachLoop.Control, q: RpcSendQueue): J | undefined {
        q.getAndSend(control, c => c.variable, variable => this.visitRightPadded(variable, q));
        q.getAndSend(control, c => c.iterable, iterable => this.visitRightPadded(iterable, q));
        return control;
    }

    protected visitForLoop(forLoop: J.ForLoop, q: RpcSendQueue): J | undefined {
        q.getAndSend(forLoop, f => f.control, control => this.visit(control, q));
        q.getAndSend(forLoop, f => f.body, body => this.visitRightPadded(body, q));
        return forLoop;
    }

    protected visitForLoopControl(control: J.ForLoop.Control, q: RpcSendQueue): J | undefined {
        q.getAndSendList(control, c => c.init, i => i.element.id, i => this.visitRightPadded(i, q));
        q.getAndSend(control, c => c.condition, c => this.visitRightPadded(c, q));
        q.getAndSendList(control, c => c.update, u => u.element.id, u => this.visitRightPadded(u, q));
        return control;
    }

    protected visitIf(ifStmt: J.If, q: RpcSendQueue): J | undefined {
        q.getAndSend(ifStmt, i => i.ifCondition, cond => this.visit(cond, q));
        q.getAndSend(ifStmt, i => i.thenPart, then => this.visitRightPadded(then, q));
        q.getAndSend(ifStmt, i => i.elsePart, elsePart => this.visit(elsePart, q));
        return ifStmt;
    }

    protected visitElse(ifElse: J.If.Else, q: RpcSendQueue): J | undefined {
        q.getAndSend(ifElse, e => e.body, body => this.visitRightPadded(body, q));
        return ifElse;
    }

    protected visitImport(importStmt: J.Import, q: RpcSendQueue): J | undefined {
        q.getAndSend(importStmt, i => i.static, static_ => this.visitLeftPadded(static_, q));
        q.getAndSend(importStmt, i => i.qualid, qualid => this.visit(qualid, q));
        q.getAndSend(importStmt, i => i.alias, alias => this.visitLeftPadded(alias, q));
        return importStmt;
    }

    protected visitInstanceOf(instanceOf: J.InstanceOf, q: RpcSendQueue): J | undefined {
        q.getAndSend(instanceOf, i => i.expression, expr => this.visitRightPadded(expr, q));
        q.getAndSend(instanceOf, i => i.class, clazz => this.visit(clazz, q));
        q.getAndSend(instanceOf, i => i.pattern, pattern => this.visit(pattern, q));
        q.getAndSend(instanceOf, i => asRef(i.type), type => this.visitType(type, q));
        q.getAndSend(instanceOf, i => i.modifier, modifier => this.visit(modifier, q));
        return instanceOf;
    }

    protected visitIntersectionType(intersectionType: J.IntersectionType, q: RpcSendQueue): J | undefined {
        q.getAndSend(intersectionType, i => i.bounds, bounds => this.visitContainer(bounds, q));
        return intersectionType;
    }

    protected visitLabel(label: J.Label, q: RpcSendQueue): J | undefined {
        q.getAndSend(label, l => l.label, id => this.visitRightPadded(id, q));
        q.getAndSend(label, l => l.statement, stmt => this.visit(stmt, q));
        return label;
    }

    protected visitLambda(lambda: J.Lambda, q: RpcSendQueue): J | undefined {
        q.getAndSend(lambda, l => l.parameters, params => this.visit(params, q));
        q.getAndSend(lambda, l => spaceRef(l.arrow), arrow => this.visitSpace(arrow, q));
        q.getAndSend(lambda, l => l.body, body => this.visit(body, q));
        q.getAndSend(lambda, l => asRef(l.type), type => this.visitType(type, q));
        return lambda;
    }

    protected visitLambdaParameters(params: J.Lambda.Parameters, q: RpcSendQueue): J | undefined {
        q.getAndSend(params, p => p.parenthesized);
        q.getAndSendList(params, p => p.parameters, param => param.element.id, param => this.visitRightPadded(param, q));
        return params;
    }

    protected visitLiteral(literal: J.Literal, q: RpcSendQueue): J | undefined {
        q.getAndSend(literal, l => l.value);
        q.getAndSend(literal, l => l.valueSource);
        q.getAndSendList(literal, l => l.unicodeEscapes, e => e.valueSourceIndex + e.codePoint, (e) => {
            q.getAndSend(e, u => u.valueSourceIndex);
            q.getAndSend(e, u => u.codePoint);
        });
        q.getAndSend(literal, l => asRef(l.type), type => this.visitType(type, q));
        return literal;
    }

    protected visitMemberReference(memberRef: J.MemberReference, q: RpcSendQueue): J | undefined {
        q.getAndSend(memberRef, m => m.containing, cont => this.visitRightPadded(cont, q));
        q.getAndSend(memberRef, m => m.typeParameters, params => this.visitContainer(params, q));
        q.getAndSend(memberRef, m => m.reference, ref => this.visitLeftPadded(ref, q));
        q.getAndSend(memberRef, m => asRef(m.type), type => this.visitType(type, q));
        q.getAndSend(memberRef, m => asRef(m.methodType), type => this.visitType(type, q));
        q.getAndSend(memberRef, m => asRef(m.variableType), type => this.visitType(type, q));
        return memberRef;
    }

    protected visitMethodInvocation(invocation: J.MethodInvocation, q: RpcSendQueue): J | undefined {
        q.getAndSend(invocation, m => m.select, select => this.visitRightPadded(select, q));
        q.getAndSend(invocation, m => m.typeParameters, params => this.visitContainer(params, q));
        q.getAndSend(invocation, m => m.name, name => this.visit(name, q));
        q.getAndSend(invocation, m => m.arguments, args => this.visitContainer(args, q));
        q.getAndSend(invocation, m => asRef(m.methodType), type => this.visitType(type, q));
        return invocation;
    }

    protected visitModifier(modifier: J.Modifier, q: RpcSendQueue): J | undefined {
        q.getAndSend(modifier, m => m.keyword);
        q.getAndSend(modifier, m => m.type);
        q.getAndSendList(modifier, m => m.annotations, annot => annot.id, annot => this.visit(annot, q));
        return modifier;
    }

    protected visitMultiCatch(multiCatch: J.MultiCatch, q: RpcSendQueue): J | undefined {
        q.getAndSendList(multiCatch, m => m.alternatives, alt => alt.element.id, alt => this.visitRightPadded(alt, q));
        return multiCatch;
    }

    protected visitNewArray(newArray: J.NewArray, q: RpcSendQueue): J | undefined {
        q.getAndSend(newArray, n => n.typeExpression, type => this.visit(type, q));
        q.getAndSendList(newArray, n => n.dimensions, dim => dim.id, dim => this.visit(dim, q));
        q.getAndSend(newArray, n => n.initializer, init => this.visitContainer(init, q));
        q.getAndSend(newArray, n => asRef(n.type), type => this.visitType(type, q));
        return newArray;
    }

    protected visitNewClass(newClass: J.NewClass, q: RpcSendQueue): J | undefined {
        q.getAndSend(newClass, n => n.enclosing, encl => this.visitRightPadded(encl, q));
        q.getAndSend(newClass, n => spaceRef(n.new), n => this.visitSpace(n, q));
        q.getAndSend(newClass, n => n.class, clazz => this.visit(clazz, q));
        q.getAndSend(newClass, n => n.arguments, args => this.visitContainer(args, q));
        q.getAndSend(newClass, n => n.body, body => this.visit(body, q));
        q.getAndSend(newClass, n => asRef(n.constructorType), type => this.visitType(type, q));
        return newClass;
    }

    protected visitNullableType(nullableType: J.NullableType, q: RpcSendQueue): J | undefined {
        q.getAndSendList(nullableType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSend(nullableType, n => n.typeTree, type => this.visitRightPadded(type, q));
        return nullableType;
    }

    protected visitParameterizedType(paramType: J.ParameterizedType, q: RpcSendQueue): J | undefined {
        q.getAndSend(paramType, p => p.class, clazz => this.visit(clazz, q));
        q.getAndSend(paramType, p => p.typeParameters, params => this.visitContainer(params, q));
        q.getAndSend(paramType, p => asRef(p.type), type => this.visitType(type, q));
        return paramType;
    }

    protected visitParentheses<T extends J>(parentheses: J.Parentheses<T>, q: RpcSendQueue): J | undefined {
        q.getAndSend(parentheses, p => p.tree, tree => this.visitRightPadded(tree, q));
        return parentheses;
    }

    protected visitParenthesizedTypeTree(parenthesizedType: J.ParenthesizedTypeTree, q: RpcSendQueue): J | undefined {
        q.getAndSendList(parenthesizedType, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSend(parenthesizedType, p => p.parenthesizedType, tree => this.visit(tree, q));
        return parenthesizedType;
    }

    protected visitPrimitive(primitive: J.Primitive, q: RpcSendQueue): J | undefined {
        q.getAndSend(primitive, p => asRef(p.type), type => this.visitType(type, q));
        return primitive;
    }

    protected visitReturn(returnStmt: J.Return, q: RpcSendQueue): J | undefined {
        q.getAndSend(returnStmt, r => r.expression, expr => this.visit(expr, q));
        return returnStmt;
    }

    protected visitSwitch(aSwitch: J.Switch, q: RpcSendQueue): J | undefined {
        q.getAndSend(aSwitch, s => s.selector, sel => this.visit(sel, q));
        q.getAndSend(aSwitch, s => s.cases, block => this.visit(block, q));
        return aSwitch;
    }

    protected visitSwitchExpression(switchExpr: J.SwitchExpression, q: RpcSendQueue): J | undefined {
        q.getAndSend(switchExpr, s => s.selector, sel => this.visit(sel, q));
        q.getAndSend(switchExpr, s => s.cases, block => this.visit(block, q));
        q.getAndSend(switchExpr, s => asRef(s.type), type => this.visitType(type, q));
        return switchExpr;
    }

    protected visitSynchronized(syncStmt: J.Synchronized, q: RpcSendQueue): J | undefined {
        q.getAndSend(syncStmt, s => s.lock, lock => this.visit(lock, q));
        q.getAndSend(syncStmt, s => s.body, body => this.visit(body, q));
        return syncStmt;
    }

    protected visitTernary(ternary: J.Ternary, q: RpcSendQueue): J | undefined {
        q.getAndSend(ternary, t => t.condition, cond => this.visit(cond, q));
        q.getAndSend(ternary, t => t.truePart, truePart => this.visitLeftPadded(truePart, q));
        q.getAndSend(ternary, t => t.falsePart, falsePart => this.visitLeftPadded(falsePart, q));
        q.getAndSend(ternary, t => asRef(t.type), type => this.visitType(type, q));
        return ternary;
    }

    protected visitThrow(throwStmt: J.Throw, q: RpcSendQueue): J | undefined {
        q.getAndSend(throwStmt, t => t.exception, exc => this.visit(exc, q));
        return throwStmt;
    }

    protected visitTry(tryStmt: J.Try, q: RpcSendQueue): J | undefined {
        q.getAndSend(tryStmt, t => t.resources, res => this.visitContainer(res, q));
        q.getAndSend(tryStmt, t => t.body, body => this.visit(body, q));
        q.getAndSendList(tryStmt, t => t.catches, catch_ => catch_.id, catch_ => this.visit(catch_, q));
        q.getAndSend(tryStmt, t => t.finally, fin => this.visitLeftPadded(fin, q));
        return tryStmt;
    }

    protected visitTryResource(resource: J.Try.Resource, q: RpcSendQueue): J | undefined {
        q.getAndSend(resource, r => r.variableDeclarations, variable => this.visit(variable, q));
        q.getAndSend(resource, r => r.terminatedWithSemicolon);
        return resource;
    }

    protected visitTryCatch(aCatch: J.Try.Catch, q: RpcSendQueue): J | undefined {
        q.getAndSend(aCatch, c => c.parameter, param => this.visit(param, q));
        q.getAndSend(aCatch, c => c.body, body => this.visit(body, q));
        return aCatch;
    }

    protected visitTypeCast(typeCast: J.TypeCast, q: RpcSendQueue): J | undefined {
        q.getAndSend(typeCast, t => t.class, clazz => this.visit(clazz, q));
        q.getAndSend(typeCast, t => t.expression, expr => this.visit(expr, q));
        return typeCast;
    }

    protected visitTypeParameter(typeParam: J.TypeParameter, q: RpcSendQueue): J | undefined {
        q.getAndSendList(typeParam, t => t.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSendList(typeParam, t => t.modifiers, mod => mod.id, mod => this.visit(mod, q));
        q.getAndSend(typeParam, t => t.name, name => this.visit(name, q));
        q.getAndSend(typeParam, t => t.bounds, bounds => this.visitContainer(bounds, q));
        return typeParam;
    }

    protected visitTypeParameters(typeParams: J.TypeParameters, q: RpcSendQueue): J | undefined {
        q.getAndSendList(typeParams, a => a.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSendList(typeParams, t => t.typeParameters, p => p.element.id, params => this.visitRightPadded(params, q));
        return typeParams;
    }

    protected visitUnary(unary: J.Unary, q: RpcSendQueue): J | undefined {
        q.getAndSend(unary, u => u.operator, op => this.visitLeftPadded(op, q));
        q.getAndSend(unary, u => u.expression, expr => this.visit(expr, q));
        q.getAndSend(unary, u => asRef(u.type), type => this.visitType(type, q));
        return unary;
    }

    protected visitVariable(variable: J.VariableDeclarations.NamedVariable, q: RpcSendQueue): J | undefined {
        q.getAndSend(variable, v => v.name, name => this.visit(name, q));
        q.getAndSendList(variable, v => v.dimensionsAfterName, d => JSON.stringify(d.element), dims => this.visitLeftPadded(dims, q));
        q.getAndSend(variable, v => v.initializer, init => this.visitLeftPadded(init, q));
        q.getAndSend(variable, v => asRef(v.variableType), type => this.visitType(type, q));
        return variable;
    }

    protected visitWhileLoop(whileLoop: J.WhileLoop, q: RpcSendQueue): J | undefined {
        q.getAndSend(whileLoop, w => w.condition, cond => this.visit(cond, q));
        q.getAndSend(whileLoop, w => w.body, body => this.visitRightPadded(body, q));
        return whileLoop;
    }

    protected visitWildcard(wildcard: J.Wildcard, q: RpcSendQueue): J | undefined {
        q.getAndSend(wildcard, w => w.bound, b => this.visitLeftPadded(b, q));
        q.getAndSend(wildcard, w => w.boundedType, type => this.visit(type, q));
        return wildcard;
    }

    protected visitYield(yieldExpr: J.Yield, q: RpcSendQueue): J | undefined {
        q.getAndSend(yieldExpr, y => y.implicit);
        q.getAndSend(yieldExpr, y => y.value, value => this.visit(value, q));
        return yieldExpr;
    }

    protected visitUnknown(unknown: J.Unknown, q: RpcSendQueue): J | undefined {
        q.getAndSend(unknown, u => u.source, source => this.visit(source, q));
        return unknown;
    }

    protected visitUnknownSource(source: J.UnknownSource, q: RpcSendQueue): J | undefined {
        q.getAndSend(source, s => s.text);
        return source;
    }

    protected visitCompilationUnit(cu: J.CompilationUnit, q: RpcSendQueue): J | undefined {
        q.getAndSend(cu, c => c.sourcePath);
        q.getAndSend(cu, c => c.charsetName);
        q.getAndSend(cu, c => c.charsetBomMarked);
        q.getAndSend(cu, c => c.checksum);
        q.getAndSend(cu, c => c.fileAttributes);
        q.getAndSend(cu, c => c.packageDeclaration, pkg => this.visitRightPadded(pkg, q));
        q.getAndSendList(cu, c => c.imports, imp => imp.element.id, imp => this.visitRightPadded(imp, q));
        q.getAndSendList(cu, c => c.classes, cls => cls.id, cls => this.visit(cls, q));
        q.getAndSend(cu, c => spaceRef(c.eof), space => this.visitSpace(space, q));
        return cu;
    }

    protected visitPackage(pkg: J.Package, q: RpcSendQueue): J | undefined {
        q.getAndSend(pkg, p => p.expression, expr => this.visit(expr, q));
        q.getAndSendList(pkg, p => p.annotations, annot => annot.id, annot => this.visit(annot, q));
        return pkg;
    }

    protected visitClassDeclaration(cls: J.ClassDeclaration, q: RpcSendQueue): J | undefined {
        q.getAndSendList(cls, c => c.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSendList(cls, c => c.modifiers, mod => mod.id, mod => this.visit(mod, q));
        q.getAndSend(cls, c => c.classKind, kind => this.visit(kind, q));
        q.getAndSend(cls, c => c.name, name => this.visit(name, q));
        q.getAndSend(cls, c => c.typeParameters, params => this.visitContainer(params, q));
        q.getAndSend(cls, c => c.primaryConstructor, cons => this.visitContainer(cons, q));
        q.getAndSend(cls, c => c.extends, ext => this.visitLeftPadded(ext, q));
        q.getAndSend(cls, c => c.implements, impl => this.visitContainer(impl, q));
        q.getAndSend(cls, c => c.permitting, perm => this.visitContainer(perm, q));
        q.getAndSend(cls, c => c.body, body => this.visit(body, q));
        q.getAndSend(cls, c => asRef(c.type), type => this.visitType(type, q));
        return cls;
    }

    protected visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, q: RpcSendQueue): J | undefined {
        q.getAndSendList(kind, k => k.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSend(kind, k => k.type);
        return kind;
    }

    protected visitBlock(block: J.Block, q: RpcSendQueue): J | undefined {
        q.getAndSend(block, b => b.static, s => this.visitRightPadded(s, q));
        q.getAndSendList(block, b => b.statements, stmt => stmt.element.id, stmt => this.visitRightPadded(stmt, q));
        q.getAndSend(block, b => spaceRef(b.end), space => this.visitSpace(space, q));
        return block;
    }

    protected visitMethodDeclaration(method: J.MethodDeclaration, q: RpcSendQueue): J | undefined {
        q.getAndSendList(method, m => m.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSendList(method, m => m.modifiers, mod => mod.id, mod => this.visit(mod, q));
        q.getAndSend(method, m => m.typeParameters, params => this.visit(params, q));
        q.getAndSend(method, m => m.returnTypeExpression, type => this.visit(type, q));
        q.getAndSendList(method, m => m.nameAnnotations, a => a.id, name => this.visit(name, q));
        q.getAndSend(method, m => m.name, name => this.visit(name, q));
        q.getAndSend(method, m => m.parameters, params => this.visitContainer(params, q));
        q.getAndSendList(method, m => m.dimensionsAfterName, d => JSON.stringify(d.element), dims => this.visitLeftPadded(dims, q));
        q.getAndSend(method, m => m.throws, throws => this.visitContainer(throws, q));
        q.getAndSend(method, m => m.body, body => this.visit(body, q));
        q.getAndSend(method, m => m.defaultValue, def => this.visitLeftPadded(def, q));
        q.getAndSend(method, m => asRef(m.methodType), type => this.visitType(type, q));
        return method;
    }

    protected visitVariableDeclarations(varDecls: J.VariableDeclarations, q: RpcSendQueue): J | undefined {
        q.getAndSendList(varDecls, v => v.leadingAnnotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSendList(varDecls, v => v.modifiers, mod => mod.id, mod => this.visit(mod, q));
        q.getAndSend(varDecls, v => v.typeExpression, type => this.visit(type, q));
        q.getAndSend(varDecls, v => spaceRef(v.varargs), space => this.visitSpace(space, q));
        q.getAndSendList(varDecls, v => v.variables, variable => variable.element.id, variable => this.visitRightPadded(variable, q));
        return varDecls;
    }

    protected visitIdentifier(ident: J.Identifier, q: RpcSendQueue): J | undefined {
        q.getAndSendList(ident, id => id.annotations, annot => annot.id, annot => this.visit(annot, q));
        q.getAndSend(ident, id => id.simpleName);
        q.getAndSend(ident, id => asRef(id.type), type => this.visitType(type, q));
        q.getAndSend(ident, id => asRef(id.fieldType), type => this.visitType(type, q));
        return ident;
    }

    public override visitSpace(space: J.Space, q: RpcSendQueue): J.Space {
        q.getAndSendList(space, s => s.comments,
            c => {
                if (c.kind === J.Kind.TextComment) {
                    return (c as TextComment).text + c.suffix;
                }
                throw new Error(`Unexpected comment type ${c.kind}`);
            },
            c => {
                if (c.kind === J.Kind.TextComment) {
                    const tc = c as TextComment;
                    q.getAndSend(tc, c2 => c2.multiline);
                    q.getAndSend(tc, c2 => c2.text);
                } else {
                    throw new Error(`Unexpected comment type ${c.kind}`);
                }
                q.getAndSend(c, c2 => c2.suffix);
                q.getAndSend(c, c2 => c2.markers);
            });
        q.getAndSend(space, s => s.whitespace);
        return space;
    }

    public override visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcSendQueue): J.LeftPadded<T> {
        q.getAndSend(left, l => spaceRef(l.before), space => this.visitSpace(space, q));
        if (isTree(left.element)) {
            q.getAndSend(left, l => l.element, elem => this.visit(elem as J, q));
        } else if (isSpace(left.element)) {
            q.getAndSend(left, l => spaceRef(l.element), space => this.visitSpace(space as J.Space, q));
        } else {
            q.getAndSend(left, l => l.element);
        }
        q.getAndSend(left, l => l.markers);
        return left;
    }

    public override visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcSendQueue): J.RightPadded<T> {
        if (isTree(right.element)) {
            q.getAndSend(right, r => r.element, elem => this.visit(elem as J, q));
        } else {
            q.getAndSend(right, r => r.element);
        }
        q.getAndSend(right, r => spaceRef(r.after), space => this.visitSpace(space, q));
        q.getAndSend(right, r => r.markers);
        return right;
    }

    public override visitContainer<T extends J>(container: J.Container<T>, q: RpcSendQueue): J.Container<T> {
        q.getAndSend(container, c => spaceRef(c.before), space => this.visitSpace(space, q));
        q.getAndSendList(container, c => c.elements, elem => elem.element.id, elem => this.visitRightPadded(elem, q));
        q.getAndSend(container, c => c.markers);
        return container;
    }

    private typeVisitor = new TypeSender();

    public override visitType(javaType: Type | undefined, q: RpcSendQueue): Type | undefined {
        if (!javaType) {
            return undefined;
        }

        return this.typeVisitor.visit(javaType, q);
    }
}

export class JavaReceiver extends JavaRpcVisitor<RpcReceiveQueue> {

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
            unicodeEscapes: q.receiveList(literal.unicodeEscapes, (e) => ({
                valueSourceIndex: q.receive(e?.valueSourceIndex),
                codePoint: q.receive(e?.codePoint),
            })),
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
            body: q.receive(cls.body, body => this.visit(body, q)),
            type: q.receive(cls.type, type => this.visitType(type, q)) as Type.Class
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
            dimensionsAfterName: q.receiveListDefined(method.dimensionsAfterName, dim => this.visitLeftPadded(dim, q)),
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
        const updates = {
            annotations: q.receiveListDefined(ident.annotations, annot => this.visit(annot, q)),
            simpleName: q.receive(ident.simpleName),
            type: q.receive(ident.type, type => this.visitType(type, q)),
            fieldType: q.receive(ident.fieldType, type => this.visitType(type, q) as any as Type.Variable)
        };
        return updateIfChanged(ident, updates);
    }

    public override visitSpace(space: J.Space, q: RpcReceiveQueue): J.Space {
        const updates = {
            comments: q.receiveListDefined(space.comments, c => {
                if (c.kind === J.Kind.TextComment) {
                    const tc = c as TextComment;
                    const commentUpdates = {
                        multiline: q.receive(tc.multiline),
                        text: q.receive(tc.text),
                        suffix: q.receive(c.suffix),
                        markers: q.receive(c.markers)
                    };
                    return updateIfChanged(tc, commentUpdates);
                } else {
                    throw new Error(`Unexpected comment type ${c.kind}`);
                }
            }),
            whitespace: q.receive(space.whitespace)
        };
        return (updates.comments.length === 0 ? sharedSpace(updates.whitespace) : undefined)
            ?? updateIfChanged(space, updates);
    }

    public override visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, q: RpcReceiveQueue): J.LeftPadded<T> {
        if (!left) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty left padding");
        }

        const updates = {
            before: q.receive(left.before, space => this.visitSpace(space, q)),
            element: this.receivePaddedElement(left.element, q),
            markers: q.receive(left.markers)
        };
        return updateIfChanged(left, updates) as J.LeftPadded<T>;
    }

    public override visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, q: RpcReceiveQueue): J.RightPadded<T> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty right padding");
        }

        const updates = {
            element: this.receivePaddedElement(right.element, q),
            after: q.receive(right.after, space => this.visitSpace(space, q)),
            markers: q.receive(right.markers)
        };
        return updateIfChanged(right, updates) as J.RightPadded<T>;
    }

    private receivePaddedElement<T>(before: T, q: RpcReceiveQueue): T {
        if (before !== undefined && before !== null && typeof before !== 'object') {
            // a scalar's new value is inlined in the message, which an onChange callback is never shown
            return q.receive(before);
        }
        return q.receive(before, elem => {
            if (isSpace(elem)) {
                return this.visitSpace(elem as J.Space, q) as any as T;
            } else if (typeof elem === 'object' && (elem as any).kind) {
                // FIXME find a better way to check if it is a `Tree`
                return this.visit(elem as J, q) as any as T;
            }
            return elem;
        });
    }

    public override visitContainer<T extends J>(container: J.Container<T>, q: RpcReceiveQueue): J.Container<T> {
        const updates = {
            before: q.receive(container.before, space => this.visitSpace(space, q)),
            elements: q.receiveListDefined(container.elements, elem => this.visitRightPadded(elem, q)),
            markers: q.receive(container.markers)
        };
        return updateIfChanged(container, updates) as J.Container<T>;
    }

    private typeVisitor = new TypeReceiver();

    public override visitType(javaType: Type | undefined, q: RpcReceiveQueue): Type | undefined {
        if (!javaType) {
            return undefined;
        } else if (javaType.kind === Type.Kind.Unknown) {
            return Type.unknownType;
        }
        return this.typeVisitor.visit(javaType, q);
    }
}

type JRpcCodec<Q> = Pick<JavaRpcVisitor<Q>, "visit" | "visitSpace" | "visitLeftPadded" | "visitRightPadded" | "visitContainer">;

export function registerJLanguageCodecs(sourceFileType: string,
                                        receiver: JRpcCodec<RpcReceiveQueue>,
                                        sender: JRpcCodec<RpcSendQueue>,
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

                rpcSend(after: J.Space, q: RpcSendQueue): void {
                    sender.visitSpace(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.RightPadded) {
            RpcCodecs.registerCodec(kind, {
                rpcReceive<T extends J | boolean>(before: J.RightPadded<T>, q: RpcReceiveQueue): J.RightPadded<T> {
                    return (receiver.visitRightPadded(before, q))!;
                },

                rpcSend<T extends J | boolean>(after: J.RightPadded<T>, q: RpcSendQueue): void {
                    sender.visitRightPadded(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.LeftPadded) {
            RpcCodecs.registerCodec(kind, {
                rpcReceive<T extends J | Space | number | string | boolean>(before: J.LeftPadded<T>, q: RpcReceiveQueue): J.LeftPadded<T> {
                    return (receiver.visitLeftPadded(before, q))!;
                },

                rpcSend<T extends J | Space | number | string | boolean>(after: J.LeftPadded<T>, q: RpcSendQueue): void {
                    sender.visitLeftPadded(after, q);
                }
            }, sourceFileType);
        } else if (kind === J.Kind.Container) {
            RpcCodecs.registerCodec(kind, {
                rpcReceive<T extends J>(before: J.Container<T>, q: RpcReceiveQueue): J.Container<T> {
                    return (receiver.visitContainer(before, q))!;
                },

                rpcSend<T extends J>(after: J.Container<T>, q: RpcSendQueue): void {
                    sender.visitContainer(after, q);
                }
            }, sourceFileType);
        } else {
            RpcCodecs.registerCodec(kind as string, {
                rpcReceive(before: J, q: RpcReceiveQueue): J {
                    return (receiver.visit(before, q))!;
                },

                rpcSend(after: J, q: RpcSendQueue): void {
                    sender.visit(after, q);
                }
            }, sourceFileType);
        }
    }
}

registerJLanguageCodecs(J.Kind.CompilationUnit, new JavaReceiver(), new JavaSender());
