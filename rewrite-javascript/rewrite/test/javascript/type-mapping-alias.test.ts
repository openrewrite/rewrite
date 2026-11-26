// noinspection JSUnusedLocalSymbols

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
import {RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, npm, packageJson, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";
import {withDir} from "tmp-promise";

function fqn(t: Type | undefined): string {
    if (!t) return "none";
    if (Type.isFullyQualified(t)) return Type.FullyQualified.getFullyQualifiedName(t);
    return Type.signature(t);
}

function captureIdentifierTypes(names: string[], sink: Map<string, string>): Recipe {
    class C extends Recipe {
        name = 'org.openrewrite.javascript.test.CaptureAliasTypes';
        displayName = 'capture';
        description = 'capture';
        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitIdentifier(ident: J.Identifier, p: ExecutionContext): Promise<J.Identifier> {
                    const v = await super.visitIdentifier(ident, p) as J.Identifier;
                    if (names.includes(v.simpleName)) {
                        sink.set(v.simpleName, fqn(v.type));
                    }
                    return v;
                }
            };
        }
    }
    return new C();
}

describe('type-alias name recovery via aliasSymbol', () => {
    // kafkajs declares `producer(): Producer` / `consumer(): Consumer`, where
    // `export type Producer = Sender & {...}` and `Consumer` are type aliases to an intersection /
    // an anonymous object. TypeScript erases the alias name from the resolved return type, but
    // preserves `type.aliasSymbol`, which we use to attribute the nominal `kafkajs.Producer` /
    // `kafkajs.Consumer` instead of an unnamed intersection / `<unknown>`.
    test('kafkajs producer()/consumer() recover their aliased type names', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        // Use binding names distinct from the `producer`/`consumer` method names so the capture
        // isn't overwritten by the method-name identifiers in `kafka.producer()`/`kafka.consumer()`.
        spec.recipe = captureIdentifierTypes(['kafka', 'prod', 'cons'], captured);

        const src = `
import {Kafka} from 'kafkajs';
const kafka = new Kafka({clientId: 'a', brokers: ['b']});
const prod = kafka.producer();
const cons = kafka.consumer({groupId: 'g'});
`;
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(repo.path,
                    //language=typescript
                    typescript(src),
                    //language=json
                    packageJson(`{
                      "name": "kafka-fixture", "version": "1.0.0",
                      "dependencies": { "kafkajs": "^2.2.4" }
                    }`)
                )
            );
        }, {unsafeCleanup: true});

        expect(captured.get('kafka')).toBe('kafkajs.Kafka');
        expect(captured.get('prod')).toBe('kafkajs.Producer');
        expect(captured.get('cons')).toBe('kafkajs.Consumer');
    }, 240000);

    // Mongoose's `orFail()` returns `NonNullable<HydratedDocument<...>>`, where the document is an
    // intersection: `NonNullable<Doc & Extra>` stays an intersection carrying the `NonNullable` alias.
    test('NonNullable<T> maps to T without null and undefined', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureIdentifierTypes(['plain', 'stripped', 'found', 'own'], captured);

        await spec.rewriteRun(
            //language=typescript
            typescript(`
                export {};
                class Doc { name = ''; }
                interface Extra { id: string; }
                type NonNullable<T> = T & {brand: true};
                declare function orFail<T>(value: T): globalThis.NonNullable<T>;
                declare const plain: Doc & Extra;
                declare const stripped: globalThis.NonNullable<Doc & Extra>;
                const found = orFail(null as (Doc & Extra) | null | undefined);
                declare const own: NonNullable<Doc & Extra>;
            `)
        );

        expect(captured.get('stripped')).toBe(captured.get('plain'));
        expect(captured.get('found')).toBe(captured.get('plain'));
        expect(captured.get('own')).not.toBe(captured.get('plain'));
    });
});
