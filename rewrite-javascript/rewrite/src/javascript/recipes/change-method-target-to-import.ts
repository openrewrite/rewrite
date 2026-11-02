/*
 * Copyright 2026 the original author or authors.
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
import {Option, Recipe} from "../../recipe";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {check} from "../../preconditions";
import {emptyMarkers} from "../../markers";
import {randomId} from "../../uuid";
import {emptySpace, isIdentifier, J, rightPadded, Type} from "../../java";
import {JavaScriptVisitor} from "../visitor";
import {MethodMatcher} from "../method-matcher";
import {usesMethod} from "../preconditions";
import {maybeBind, maybeUnbind} from "../binding";
import {moduleScopeBindings} from "../add-import";
import {compilationUnitOf, scopeOf} from "../scope";

/**
 * Moves calls matching a method pattern onto a member imported from a module, such as `jest.fn()`
 * to `vi.fn()` with `import {vi} from "vitest"`.
 *
 * A call through an import of the old target, whether a namespace, default or named import, moves
 * too, and the import goes once nothing else uses it.
 */
export class ChangeMethodTargetToImport extends Recipe {
    readonly name = "org.openrewrite.javascript.change-method-target-to-import";
    readonly displayName = "Change method target to an import";
    readonly description = "Changes the target of calls matching a method pattern to a member imported from a module, " +
        "adding the import. A CommonJS file changes only where it already requires the target, and an AMD module not at all.";

    @Option({
        displayName: "Method pattern",
        description: "A method pattern matching the calls to move, as `<declaring type> <name>(<args>)`.",
        example: "jest *(..)"
    })
    methodPattern!: string;

    @Option({
        displayName: "Target module",
        description: "The module the new target is imported from.",
        example: "vitest"
    })
    targetModule!: string;

    @Option({
        displayName: "Target member",
        description: "The member of the target module the calls move onto.",
        example: "vi"
    })
    targetMember!: string;

    @Option({
        displayName: "Target type",
        description: "The fully-qualified type of the target member, which declares the methods the calls move to. " +
            "Without it, the moved calls are attributed to an unknown declaring type.",
        example: "vitest.VitestUtils",
        required: false
    })
    targetType?: string;

    @Option({
        displayName: "Match on overrides",
        description: "When enabled, find methods that are overrides of the method pattern.",
        required: false
    })
    matchOverrides?: boolean;

    constructor(options?: {
        methodPattern?: string;
        targetModule?: string;
        targetMember?: string;
        targetType?: string;
        matchOverrides?: boolean;
    }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const matcher = new MethodMatcher(this.methodPattern, this.matchOverrides);
        const module = this.targetModule;
        const member = this.targetMember;
        // Named after the specifier, as the parser names a module object.
        const moduleObject = classNamed(module);
        const memberType = this.targetType ? classNamed(this.targetType) : Type.unknownType as Type.FullyQualified;

        return check(usesMethod(this.methodPattern, this.matchOverrides), new class extends JavaScriptVisitor<ExecutionContext> {
            protected override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;
                if (!matcher.matches(m.methodType)) {
                    return m;
                }
                const select = m.select?.element;
                if (select !== undefined && !isIdentifier(select)) {
                    return m;
                }
                const oldName = select?.simpleName ?? m.name.simpleName;
                const cu = compilationUnitOf(this)!;
                const owner = scopeOf(this.cursor).declaringScope(oldName);
                const imported = owner === cu ?
                    moduleScopeBindings(cu).find(b => b.name === oldName && b.module !== undefined) : undefined;
                if (owner !== undefined && !imported) {
                    return m;
                }
                // A call without a receiver names the member it was imported as, which a default import lacks.
                const methodName = select || !imported ? m.name.simpleName : imported.member;
                if (methodName === undefined) {
                    return m;
                }

                const name = maybeBind(this, {module, member, onlyIfReferenced: false});
                if (name === undefined) {
                    return m;
                }
                if (imported) {
                    maybeUnbind(this, {module: imported.module!, member: imported.member ?? 'default'});
                }

                const methodType = m.methodType && {...m.methodType, declaringType: memberType};
                const target: J.Identifier = {
                    kind: J.Kind.Identifier,
                    id: randomId(),
                    prefix: select?.prefix ?? m.name.prefix,
                    markers: emptyMarkers,
                    annotations: [],
                    simpleName: name,
                    type: memberType,
                    fieldType: {
                        kind: Type.Kind.Variable,
                        name: member,
                        flags: 0,
                        owner: moduleObject,
                        type: memberType,
                        annotations: []
                    } as Type.Variable
                };
                const moved: J.MethodInvocation = {
                    ...m,
                    select: m.select ? {...m.select, element: target} : rightPadded(target, emptySpace),
                    name: {
                        ...m.name,
                        prefix: m.select ? m.name.prefix : emptySpace,
                        simpleName: methodName,
                        type: Type.isMethod(m.name.type) ? methodType : m.name.type,
                        fieldType: m.name.fieldType && {...m.name.fieldType, name: methodName, owner: memberType}
                    },
                    methodType
                };
                return moved;
            }
        }());
    }
}

function classNamed(fullyQualifiedName: string): Type.Class {
    return {
        kind: Type.Kind.Class,
        flags: 0,
        classKind: Type.Class.Kind.Interface,
        fullyQualifiedName,
        typeParameters: [],
        annotations: [],
        interfaces: [],
        members: [],
        methods: []
    } as Type.Class;
}
