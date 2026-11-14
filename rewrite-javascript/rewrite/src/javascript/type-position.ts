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
import {Cursor} from "../tree";
import {J} from "../java";
import {JS, JSX} from "./tree";

/**
 * Whether a type may stand where the cursor is: a type annotation, an array element type, a union or
 * intersection member, a type argument, a bound, `as` and `satisfies`, `implements`, a tuple element or
 * rest, a `keyof` operand, an index type, a mapped type's key remapping. A value read, a `typeof` operand
 * and a JSX tag are not.
 */
export function isTypePosition(cursor: Cursor): boolean {
    const name = cursor.value as J;
    const parentCursor = cursor.parentTree();
    const parent = parentCursor?.value as J | undefined;
    const holds = (container?: J.Container<J>) => container?.elements.some(e => e.element === name) ?? false;
    const grandparentKind = () => (parentCursor?.parentTree()?.value as J | undefined)?.kind;
    switch (parent?.kind) {
        case JS.Kind.TypeInfo:
        case JS.Kind.Union:
        case JS.Kind.Intersection:
        case JS.Kind.Tuple:
        case JS.Kind.TypeOperator:
        case JS.Kind.IndexedAccessType:
        case JS.Kind.IndexedAccessTypeIndexType:
        case JS.Kind.MappedType:
        case J.Kind.ArrayType:
            return true;
        case JS.Kind.MappedTypeParameter:
            return (parent as JS.MappedType.Parameter).iterateType.element === name;
        case JS.Kind.MappedTypeKeysRemapping:
            return (parent as JS.MappedType.KeysRemapping).nameType?.element === name;
        case JS.Kind.Spread:
            return grandparentKind() === JS.Kind.Tuple;
        case JS.Kind.TemplateExpressionSpan: {
            const template = parentCursor!.parentTree();
            return (parent as JS.TemplateExpression.Span).expression === name && template !== undefined && isTypePosition(template);
        }
        case JS.Kind.TypeDeclaration:
            return (parent as JS.TypeDeclaration).initializer.element === name;
        case JS.Kind.FunctionType:
            return (parent as JS.FunctionType).returnType.element === name;
        case JS.Kind.ConditionalType:
            return (parent as JS.ConditionalType).checkType === name;
        case J.Kind.Ternary:
            return grandparentKind() === JS.Kind.ConditionalType;
        case JS.Kind.TypePredicate:
            return (parent as JS.TypePredicate).expression?.element === name;
        case JS.Kind.IndexSignatureDeclaration:
            return (parent as JS.IndexSignatureDeclaration).typeExpression.element === name;
        case J.Kind.TypeParameter:
            return holds((parent as J.TypeParameter).bounds);
        case JS.Kind.As:
            return (parent as JS.As).right === name;
        case JS.Kind.SatisfiesExpression:
            return (parent as JS.SatisfiesExpression).satisfiesType.element === name;
        case J.Kind.ParameterizedType:
            return holds((parent as J.ParameterizedType).typeParameters);
        case J.Kind.MethodInvocation:
            return holds((parent as J.MethodInvocation).typeParameters);
        case JS.Kind.FunctionCall:
            return holds((parent as JS.FunctionCall).typeParameters);
        case JS.Kind.ImportType:
            return holds((parent as JS.ImportType).typeArguments);
        case JS.Kind.TypeQuery:
            return holds((parent as JS.TypeQuery).typeArguments);
        case JS.Kind.ExpressionWithTypeArguments:
            return holds((parent as JS.ExpressionWithTypeArguments).typeArguments);
        case JS.Kind.JsxTag:
            return holds((parent as JSX.Tag).typeArguments);
        case JS.Kind.TaggedTemplateExpression:
            return holds((parent as JS.TaggedTemplateExpression).typeArguments);
        case J.Kind.ControlParentheses:
            return grandparentKind() === J.Kind.TypeCast;
        case J.Kind.ClassDeclaration:
            return holds((parent as J.ClassDeclaration).implements);
        case J.Kind.Parentheses:
            return grandparentKind() === J.Kind.ParenthesizedTypeTree;
        default:
            return false;
    }
}
