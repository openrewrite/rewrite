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
import {isTree} from '../..';
import {emptySpace, J} from '../../java';
import {emptyMarkers, Marker, markers, Markers} from '../../markers';
import {randomId} from '../../uuid';
import {JS} from '..';

// Known gap: a bare `in` in a C-style for-head needs parentheses this cannot see ([In] grammar parameter)

/** JavaScript operator precedence, as ordered by the ECMAScript grammar; a higher number binds tighter. */
export const Precedence = {
    /** `a, b` */
    Comma: 1,
    /** `=`, `+=`, `? :`, `=>`, `yield`, `...` — everything at `AssignmentExpression` level. */
    Assignment: 2,
    /** `||` and `??` */
    LogicalOr: 3,
    /** `&&` */
    LogicalAnd: 4,
    /** `|` */
    BitOr: 5,
    /** `^` */
    BitXor: 6,
    /** `&` */
    BitAnd: 7,
    /** `==` `!=` `===` `!==` */
    Equality: 8,
    /** `<` `>` `<=` `>=` `in` `instanceof` `as` `satisfies` */
    Relational: 9,
    /** `<<` `>>` `>>>` */
    Shift: 10,
    /** `+` `-` */
    Additive: 11,
    /** `*` `/` `%` */
    Multiplicative: 12,
    /** `**` (right-associative) */
    Exponentiation: 13,
    /** `!` `~` `+` `-` `++x` `--x` `typeof` `void` `delete` `await` */
    Prefix: 14,
    /** `x++` `x--` */
    Postfix: 15,
    /** `new X` without an argument list */
    New: 16,
    /** `a.b` `a[b]` `f()` `new X()` `` tag`...` `` */
    Call: 17,
    /** Literals, identifiers, parenthesized expressions, array/object literals, JSX. */
    Primary: 18
} as const;

/** What a slot demands of what sits in it, including the shape restrictions precedence cannot express. */
interface SlotConstraints {
    /** The lowest precedence that can sit here unparenthesized. */
    readonly precedence: number;
    /** The slot is a `MemberExpression`, which excludes a call: `new f()()` means `(new f())()`. */
    readonly noCallShape?: boolean;
    /** The slot forbids an optional chain anywhere in it, as `new` and tagged templates do. */
    readonly noOptionalChain?: boolean;
    /** The slot may not start with `{`, which would be read as a block. */
    readonly noLeadingObjectLiteral?: boolean;
    /** The slot may not start with `{`, `function` or `class`, as a statement may not. */
    readonly noLeadingDeclarationToken?: boolean;
    /** The slot is followed by `.`, so a bare integer literal would lex as a decimal point. */
    readonly followedByDot?: boolean;
}

/** The precedence of `expression` as printed; a kind this module does not model counts as Primary. */
export function precedenceOf(expression: J): number {
    switch (expression.kind) {
        case J.Kind.Binary:
            return binaryPrecedence((expression as J.Binary).operator.element) ?? Precedence.Primary;
        case JS.Kind.Binary:
            return jsBinaryPrecedence((expression as JS.Binary).operator.element) ?? Precedence.Primary;
        case J.Kind.Unary: {
            const operator = (expression as J.Unary).operator.element;
            return isPostfixOperator(operator) ? Precedence.Postfix : Precedence.Prefix;
        }
        case J.Kind.InstanceOf:
        case JS.Kind.As:
        case JS.Kind.SatisfiesExpression:
            return Precedence.Relational;
        case JS.Kind.AssignmentOperation:
            // `**` is the odd one out: every other JS.AssignmentOperation is a compound assignment
            return (expression as JS.AssignmentOperation).operator.element === JS.AssignmentOperation.Type.Power ?
                Precedence.Exponentiation : Precedence.Assignment;
        case J.Kind.Ternary:
        case J.Kind.Assignment:
        case J.Kind.AssignmentOperation:
        case J.Kind.Lambda:
        case J.Kind.Yield:
        case JS.Kind.ArrowFunction:
        case JS.Kind.Spread:
            return Precedence.Assignment;
        case J.Kind.TypeCast:
        case JS.Kind.Await:
        case JS.Kind.Delete:
        case JS.Kind.TypeOf:
        case JS.Kind.Void:
            return Precedence.Prefix;
        case J.Kind.FieldAccess:
        case J.Kind.ArrayAccess:
        case J.Kind.MethodInvocation:
        case JS.Kind.FunctionCall:
        case JS.Kind.ExpressionWithTypeArguments:
        case JS.Kind.TaggedTemplateExpression:
            return Precedence.Call;
        case J.Kind.NewClass:
            return isObjectLiteral(expression) ? Precedence.Primary :
                hasOmitParentheses(expression as J.NewClass) ? Precedence.New : Precedence.Call;
        case JS.Kind.ExpressionStatement:
            return precedenceOf((expression as JS.ExpressionStatement).expression);
        case JS.Kind.StatementExpression:
            // `yield x` is parsed as a J.Yield inside a JS.StatementExpression
            return precedenceOf((expression as JS.StatementExpression).statement);
        default:
            return Precedence.Primary;
    }
}

/** What the slot of `parent` holding `childId` demands, or `undefined` for a slot not modelled here. */
function slotConstraints(parent: J, childId: string): SlotConstraints | undefined {
    switch (parent.kind) {
        case J.Kind.Parentheses:
        case J.Kind.ControlParentheses: {
            const parentheses = parent as J.Parentheses<J>;
            return parentheses.tree?.element?.id === childId ? {precedence: 0} : undefined;
        }

        case J.Kind.Binary: {
            const binary = parent as J.Binary;
            const precedence = binaryPrecedence(binary.operator.element);
            if (precedence === undefined) {
                return undefined;
            }
            // Left-associative: the right operand of an equally binding operator needs parentheses
            if (binary.left?.id === childId) {
                return {precedence};
            }
            return binary.right?.id === childId ? {precedence: precedence + 1} : undefined;
        }

        case JS.Kind.Binary: {
            const binary = parent as JS.Binary;
            const precedence = jsBinaryPrecedence(binary.operator.element);
            if (precedence === undefined) {
                return undefined;
            }
            if (binary.left?.id === childId) {
                return {precedence};
            }
            return binary.right?.id === childId ? {precedence: precedence + 1} : undefined;
        }

        case J.Kind.Unary: {
            const unary = parent as J.Unary;
            if (unary.expression?.id !== childId) {
                return undefined;
            }
            // `++`/`--` need a reference as their operand, not merely a unary expression
            return {precedence: isUpdateOperator(unary.operator.element) ? Precedence.Call : Precedence.Prefix};
        }

        case J.Kind.Ternary: {
            const ternary = parent as J.Ternary;
            if (ternary.condition?.id === childId) {
                // The condition is a ShortCircuitExpression, so it binds tighter than `? :` itself
                return {precedence: Precedence.LogicalOr};
            }
            if (ternary.truePart?.element?.id === childId || ternary.falsePart?.element?.id === childId) {
                return {precedence: Precedence.Assignment};
            }
            return undefined;
        }

        case J.Kind.Assignment: {
            const assignment = parent as J.Assignment;
            if (assignment.variable?.id === childId) {
                return {precedence: Precedence.Call};
            }
            return assignment.assignment?.element?.id === childId ? {precedence: Precedence.Assignment} : undefined;
        }

        case J.Kind.AssignmentOperation: {
            const assignment = parent as J.AssignmentOperation;
            if (assignment.variable?.id === childId) {
                return {precedence: Precedence.Call};
            }
            return assignment.assignment?.id === childId ? {precedence: Precedence.Assignment} : undefined;
        }

        case JS.Kind.AssignmentOperation: {
            const assignment = parent as JS.AssignmentOperation;
            const power = assignment.operator.element === JS.AssignmentOperation.Type.Power;
            if (assignment.variable?.id === childId) {
                // `-a ** b` is a syntax error, and `**` is right-associative besides
                return {precedence: power ? Precedence.Postfix : Precedence.Call};
            }
            if (assignment.assignment?.id === childId) {
                return {precedence: power ? Precedence.Exponentiation : Precedence.Assignment};
            }
            return undefined;
        }

        case J.Kind.FieldAccess: {
            const fieldAccess = parent as J.FieldAccess;
            return fieldAccess.target?.id === childId ?
                {precedence: Precedence.Call, followedByDot: !isOptional(fieldAccess.target)} : undefined;
        }

        case J.Kind.ArrayAccess: {
            const arrayAccess = parent as J.ArrayAccess;
            if (arrayAccess.indexed?.id === childId) {
                return {precedence: Precedence.Call};
            }
            return arrayAccess.dimension?.index?.element?.id === childId ? {precedence: 0} : undefined;
        }

        case J.Kind.MethodInvocation: {
            const method = parent as J.MethodInvocation;
            if (method.select?.element?.id === childId) {
                return {precedence: Precedence.Call, followedByDot: !isOptional(method.select.element)};
            }
            return isContainerElement(method.arguments, childId) ? {precedence: Precedence.Assignment} : undefined;
        }

        case JS.Kind.FunctionCall: {
            const call = parent as JS.FunctionCall;
            if (call.function?.element?.id === childId) {
                return {precedence: Precedence.Call};
            }
            return isContainerElement(call.arguments, childId) ? {precedence: Precedence.Assignment} : undefined;
        }

        case J.Kind.NewClass: {
            const newClass = parent as J.NewClass;
            if (newClass.class?.id === childId) {
                return {precedence: Precedence.Call, noCallShape: true, noOptionalChain: true};
            }
            return isContainerElement(newClass.arguments, childId) ? {precedence: Precedence.Assignment} : undefined;
        }

        case J.Kind.NewArray:
            return isContainerElement((parent as J.NewArray).initializer, childId) ?
                {precedence: Precedence.Assignment} : undefined;

        case J.Kind.InstanceOf:
            return (parent as J.InstanceOf).expression?.element?.id === childId ?
                {precedence: Precedence.Relational} : undefined;

        case JS.Kind.As:
            return (parent as JS.As).left?.element?.id === childId ? {precedence: Precedence.Relational} : undefined;

        case JS.Kind.SatisfiesExpression:
            return ((parent as JS.SatisfiesExpression).expression as J)?.id === childId ?
                {precedence: Precedence.Relational} : undefined;

        case J.Kind.TypeCast:
            return (parent as J.TypeCast).expression?.id === childId ? {precedence: Precedence.Prefix} : undefined;

        case JS.Kind.Await:
        case JS.Kind.Delete:
        case JS.Kind.TypeOf:
        case JS.Kind.Void:
            return (parent as JS.Await).expression?.id === childId ? {precedence: Precedence.Prefix} : undefined;

        case JS.Kind.Spread:
            return (parent as JS.Spread).expression?.id === childId ? {precedence: Precedence.Assignment} : undefined;

        case JS.Kind.TaggedTemplateExpression:
            return (parent as JS.TaggedTemplateExpression).tag?.element?.id === childId ?
                {precedence: Precedence.Call, noOptionalChain: true} : undefined;

        case JS.Kind.ExpressionWithTypeArguments:
            return ((parent as JS.ExpressionWithTypeArguments).clazz as J)?.id === childId ?
                {precedence: Precedence.Call} : undefined;

        case JS.Kind.PropertyAssignment:
            return (parent as JS.PropertyAssignment).initializer?.id === childId ?
                {precedence: Precedence.Assignment} : undefined;

        case J.Kind.NamedVariable: {
            const variable = parent as J.VariableDeclarations.NamedVariable;
            return variable.initializer?.element?.id === childId ? {precedence: Precedence.Assignment} : undefined;
        }

        case J.Kind.Lambda:
            return (parent as J.Lambda).body?.id === childId ?
                {precedence: Precedence.Assignment, noLeadingObjectLiteral: true} : undefined;

        case J.Kind.ClassDeclaration:
            // `class A extends B {}` takes a LeftHandSideExpression
            return (parent as J.ClassDeclaration).extends?.element?.id === childId ?
                {precedence: Precedence.Call} : undefined;

        case JS.Kind.ExpressionStatement:
            return (parent as JS.ExpressionStatement).expression?.id === childId ?
                {precedence: 0, noLeadingDeclarationToken: true} : undefined;

        default:
            return undefined;
    }
}

/** The precedence of the slot of `parent` holding `childId`; see {@link slotConstraints} for the rest. */
export function requiredPrecedence(parent: J, childId: string): number | undefined {
    return slotConstraints(parent, childId)?.precedence;
}

/**
 * Parenthesizes `expression` if the slot of `parent` holding `childId` would otherwise reparse it.
 * `slotMarkers` are the markers the slot contributed; {@link parenthesize} says what they decide.
 */
export function maybeParenthesize(parent: J | undefined, childId: string, expression: J,
                                  slotMarkers?: Markers): J {
    if (!parent) {
        return expression;
    }

    const constraints = slotConstraints(parent, childId);
    if (!constraints) {
        return expression;
    }

    // A statement wrapper is transparent here: the parentheses belong around the expression
    if (expression.kind === JS.Kind.ExpressionStatement) {
        const inner = (expression as JS.ExpressionStatement).expression;
        const wrapped = wrapIfNeeded(parent, childId, constraints, inner, slotMarkers);
        return wrapped === inner ? expression : {...expression, expression: wrapped} as JS.ExpressionStatement;
    }
    return wrapIfNeeded(parent, childId, constraints, expression, slotMarkers);
}

/** The nearest enclosing LST node in a cursor path, skipping the padding wrappers visitors push. */
export function enclosingTree(cursor: { value: any, parent?: any } | undefined): J | undefined {
    let current = cursor;
    while (current) {
        if (isTree(current.value)) {
            return current.value as J;
        }
        current = current.parent;
    }
    return undefined;
}

/**
 * Wraps in `J.Parentheses`, moving the prefix out so the surrounding whitespace survives. A trailing marker
 * `slotMarkers` also carries belongs to the slot around the expression, so it moves out too: `${x}!` with `x`
 * bound to `a + b` gives `(a + b)!`, while a bare `${x}` bound to `a!` gives `(a!)`.
 */
export function parenthesize(expression: J, slotMarkers?: Markers): J.Parentheses<J> {
    const hoisted = expression.markers.markers.filter(
        m => isTrailingMarker(m) && slotMarkers?.markers.some(slot => slot.kind === m.kind));
    const inner = hoisted.length === 0 ? expression : {
        ...expression,
        markers: markers(...expression.markers.markers.filter(m => !hoisted.includes(m)))
    };
    return {
        kind: J.Kind.Parentheses,
        id: randomId(),
        prefix: expression.prefix,
        markers: hoisted.length === 0 ? emptyMarkers : markers(...hoisted),
        tree: {
            kind: J.Kind.RightPadded,
            element: {...inner, prefix: emptySpace},
            after: emptySpace,
            markers: emptyMarkers
        }
    };
}

function wrapIfNeeded(parent: J, childId: string, constraints: SlotConstraints, expression: J,
                      slotMarkers: Markers | undefined): J {
    if (precedenceOf(expression) < constraints.precedence ||
        (constraints.noCallShape && isCallShaped(expression)) ||
        (constraints.noOptionalChain && hasOptionalChain(expression)) ||
        (constraints.noLeadingObjectLiteral && startsWithObjectLiteral(expression)) ||
        (constraints.noLeadingDeclarationToken && startsWithDeclarationToken(expression)) ||
        (constraints.followedByDot && isDotAdjacentNumber(expression)) ||
        mixesNullishWithLogical(parent, expression) ||
        wouldFuseSigns(parent, childId, expression)) {
        return parenthesize(expression, slotMarkers);
    }
    return expression;
}

function binaryPrecedence(operator: J.Binary.Type): number | undefined {
    switch (operator) {
        case J.Binary.Type.Multiplication:
        case J.Binary.Type.Division:
        case J.Binary.Type.Modulo:
            return Precedence.Multiplicative;
        case J.Binary.Type.Addition:
        case J.Binary.Type.Subtraction:
            return Precedence.Additive;
        case J.Binary.Type.LeftShift:
        case J.Binary.Type.RightShift:
        case J.Binary.Type.UnsignedRightShift:
            return Precedence.Shift;
        case J.Binary.Type.LessThan:
        case J.Binary.Type.GreaterThan:
        case J.Binary.Type.LessThanOrEqual:
        case J.Binary.Type.GreaterThanOrEqual:
            return Precedence.Relational;
        case J.Binary.Type.Equal:
        case J.Binary.Type.NotEqual:
            return Precedence.Equality;
        case J.Binary.Type.BitAnd:
            return Precedence.BitAnd;
        case J.Binary.Type.BitXor:
            return Precedence.BitXor;
        case J.Binary.Type.BitOr:
            return Precedence.BitOr;
        case J.Binary.Type.And:
            return Precedence.LogicalAnd;
        case J.Binary.Type.Or:
            return Precedence.LogicalOr;
        default:
            // An operator this table does not know; both callers then leave the expression alone
            return undefined;
    }
}

/** @see binaryPrecedence */
function jsBinaryPrecedence(operator: JS.Binary.Type): number | undefined {
    switch (operator) {
        case JS.Binary.Type.IdentityEquals:
        case JS.Binary.Type.IdentityNotEquals:
            return Precedence.Equality;
        case JS.Binary.Type.As:
        case JS.Binary.Type.In:
            return Precedence.Relational;
        case JS.Binary.Type.QuestionQuestion:
            return Precedence.LogicalOr;
        case JS.Binary.Type.Comma:
            return Precedence.Comma;
        default:
            return undefined;
    }
}

function isUpdateOperator(operator: J.Unary.Type): boolean {
    return operator === J.Unary.Type.PreIncrement || operator === J.Unary.Type.PreDecrement ||
        isPostfixOperator(operator);
}

function isPostfixOperator(operator: J.Unary.Type): boolean {
    return operator === J.Unary.Type.PostIncrement || operator === J.Unary.Type.PostDecrement;
}

function isContainerElement(container: J.Container<J> | undefined, childId: string): boolean {
    return !!container?.elements?.some(element => element?.element?.id === childId);
}

function hasOmitParentheses(newClass: J.NewClass): boolean {
    return !!newClass.arguments?.markers?.markers?.some(marker => marker.kind === J.Markers.OmitParentheses);
}

/** The JS parser reuses `J.NewClass` for object literals, which have no `class`. */
function isObjectLiteral(expression: J): boolean {
    return expression.kind === J.Kind.NewClass && !(expression as J.NewClass).class;
}

/** Whether this is a CallExpression rather than a MemberExpression. */
function isCallShaped(expression: J): boolean {
    switch (expression.kind) {
        case J.Kind.MethodInvocation:
        case JS.Kind.FunctionCall:
            return true;
        case J.Kind.FieldAccess:
            return isCallShaped((expression as J.FieldAccess).target);
        case J.Kind.ArrayAccess:
            return isCallShaped((expression as J.ArrayAccess).indexed);
        default:
            return false;
    }
}

/** `?.` is a marker on the node to the left of it rather than a node of its own. */
function isOptional(expression: J): boolean {
    return expression.markers.markers.some(marker => marker.kind === JS.Markers.Optional);
}

/** Whether any link of the member chain is optional (`?.`). */
function hasOptionalChain(expression: J): boolean {
    if (isOptional(expression)) {
        return true;
    }
    const next = leftmostChild(expression);
    return next !== undefined && hasOptionalChain(next);
}

/** Whether a following `.` lexes into the number: `1.toString()` fails, `1.5`/`1e3`/`0x10`/`1?.x` do not. */
function isDotAdjacentNumber(expression: J): boolean {
    if (expression.kind !== J.Kind.Literal) {
        return false;
    }
    const source = (expression as J.Literal).valueSource;
    return !!source && /^\d[\d_]*$/.test(source);
}

/** Whether the printed form begins with `{`. */
function startsWithObjectLiteral(expression: J): boolean {
    return isObjectLiteral(leftmostExpression(expression));
}

/** Whether the printed form begins with `{`, `function` or `class`. */
export function startsWithDeclarationToken(expression: J): boolean {
    const leftmost = leftmostExpression(expression);
    return isObjectLiteral(leftmost) || isFunctionOrClassExpression(leftmost);
}

/** A function or class *expression*, which the JS parser wraps in a `JS.StatementExpression`. */
function isFunctionOrClassExpression(expression: J): boolean {
    if (expression.kind !== JS.Kind.StatementExpression) {
        return false;
    }
    const statement = (expression as JS.StatementExpression).statement;
    return statement?.kind === J.Kind.MethodDeclaration || statement?.kind === J.Kind.ClassDeclaration;
}

/** Walks down the left spine, to the token the expression starts with. */
function leftmostExpression(expression: J): J {
    let current = expression;
    for (let next = leftmostChild(current); next; next = leftmostChild(current)) {
        current = next;
    }
    return current;
}

function leftmostChild(expression: J): J | undefined {
    switch (expression.kind) {
        case J.Kind.Binary:
            return (expression as J.Binary).left;
        case JS.Kind.Binary:
            return (expression as JS.Binary).left;
        case J.Kind.Ternary:
            return (expression as J.Ternary).condition;
        case J.Kind.Assignment:
            return (expression as J.Assignment).variable;
        case J.Kind.AssignmentOperation:
            return (expression as J.AssignmentOperation).variable;
        case JS.Kind.AssignmentOperation:
            return (expression as JS.AssignmentOperation).variable;
        case J.Kind.FieldAccess:
            return (expression as J.FieldAccess).target;
        case J.Kind.ArrayAccess:
            return (expression as J.ArrayAccess).indexed;
        case J.Kind.MethodInvocation:
            return (expression as J.MethodInvocation).select?.element;
        case JS.Kind.FunctionCall:
            return (expression as JS.FunctionCall).function?.element;
        case J.Kind.InstanceOf:
            return (expression as J.InstanceOf).expression?.element;
        case JS.Kind.As:
            return (expression as JS.As).left?.element;
        case JS.Kind.SatisfiesExpression:
            return (expression as JS.SatisfiesExpression).expression as J;
        case JS.Kind.TaggedTemplateExpression:
            return (expression as JS.TaggedTemplateExpression).tag?.element;
        case JS.Kind.ExpressionStatement:
            return (expression as JS.ExpressionStatement).expression;
        case J.Kind.Unary: {
            const unary = expression as J.Unary;
            return isPostfixOperator(unary.operator.element) ? unary.expression : undefined;
        }
        default:
            return undefined;
    }
}

/** Markers the printer emits *after* the node they sit on. */
function isTrailingMarker(marker: Marker): boolean {
    return marker.kind === JS.Markers.NonNullAssertion || marker.kind === JS.Markers.Optional;
}

function isNullishCoalescing(expression: J): boolean {
    return expression.kind === JS.Kind.Binary &&
        (expression as JS.Binary).operator.element === JS.Binary.Type.QuestionQuestion;
}

function isLogicalAndOr(expression: J): boolean {
    if (expression.kind !== J.Kind.Binary) {
        return false;
    }
    const operator = (expression as J.Binary).operator.element;
    return operator === J.Binary.Type.And || operator === J.Binary.Type.Or;
}

/** `??` beside `||` or `&&` is a syntax error rather than a re-association, so precedence misses it. */
function mixesNullishWithLogical(parent: J, child: J): boolean {
    return (isNullishCoalescing(parent) && isLogicalAndOr(child)) ||
        (isLogicalAndOr(parent) && isNullishCoalescing(child));
}

/** Whether a `+`/`-` would fuse with the sign after it into `++`/`--`, turning `-(-a)` into `--a`. */
function wouldFuseSigns(parent: J, childId: string, child: J): boolean {
    const preceding = precedingSign(parent, childId);
    return preceding !== undefined && preceding === leadingSign(child) && !isSeparated(child);
}

/** The sign the parent prints immediately before the slot holding `childId`, if any. */
function precedingSign(parent: J, childId: string): Sign | undefined {
    if (parent.kind === J.Kind.Unary) {
        const unary = parent as J.Unary;
        return unary.expression?.id === childId ? signOf(unary.operator.element) : undefined;
    }
    if (parent.kind === J.Kind.Binary) {
        const binary = parent as J.Binary;
        if (binary.right?.id !== childId) {
            return undefined;
        }
        switch (binary.operator.element) {
            case J.Binary.Type.Subtraction:
                return 'minus';
            case J.Binary.Type.Addition:
                return 'plus';
            default:
                return undefined;
        }
    }
    return undefined;
}

/** The sign the expression prints first, if any; it can sit arbitrarily deep on the left spine. */
function leadingSign(expression: J): Sign | undefined {
    const leftmost = leftmostExpression(expression);
    return leftmost.kind === J.Kind.Unary ? signOf((leftmost as J.Unary).operator.element) : undefined;
}

/** Whether anything is printed between the expression and the token that precedes it. */
function isSeparated(expression: J): boolean {
    for (let current: J | undefined = expression; current; current = leftmostChild(current)) {
        if (current.prefix.whitespace || current.prefix.comments.length > 0) {
            return true;
        }
    }
    return false;
}

type Sign = 'minus' | 'plus';

function signOf(operator: J.Unary.Type): Sign | undefined {
    switch (operator) {
        case J.Unary.Type.Negative:
        case J.Unary.Type.PreDecrement:
            return 'minus';
        case J.Unary.Type.Positive:
        case J.Unary.Type.PreIncrement:
            return 'plus';
        default:
            return undefined;
    }
}
