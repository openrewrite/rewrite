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
    if (Type.isClass(t)) return t.fullyQualifiedName;
    return Type.signature(t);
}

function captureIdentifierTypes(names: string[], sink: Map<string, string>): Recipe {
    class CaptureRecipe extends Recipe {
        name = 'org.openrewrite.javascript.test.CapturePackageRecovery';
        displayName = 'Capture identifier types';
        description = 'Records the resolved type FQN of selected identifiers.';

        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitIdentifier(ident: J.Identifier, p: ExecutionContext): Promise<J.Identifier> {
                    const visited = await super.visitIdentifier(ident, p) as J.Identifier;
                    if (names.includes(visited.simpleName)) {
                        sink.set(visited.simpleName, fqn(visited.type));
                    }
                    return visited;
                }
            };
        }
    }

    return new CaptureRecipe();
}

describe('node_modules package recovery for bare FQNs', () => {
    // Some @types packages (e.g. @types/bunyan) `export = Logger` a single top-level class.
    // TypeScript's getFullyQualifiedName drops the package for such symbols (they have no
    // `parent`), yielding a bare `Logger` with the declaring package lost. Recover it from
    // the declaration file so the FQN identifies the package it actually came from.
    test('bare top-level export gets its declaring package prefixed (bunyan)', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureIdentifierTypes(['log', 'queue'], captured);

        const src = `
import bunyan from 'bunyan';
import Bull from 'bull';
const log = bunyan.createLogger({name: 'svc'});
const queue = new Bull('q');
log.info('hi');
queue.add('job', {});
`;
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(src),
                    //language=json
                    packageJson(`{
                      "name": "pkg-recovery-fixture",
                      "version": "1.0.0",
                      "dependencies": { "bunyan": "^1.8.15", "bull": "^4.12.0" },
                      "devDependencies": { "@types/bunyan": "^1.8.11", "@types/bull": "^3.15.9" }
                    }`)
                )
            );
        }, {unsafeCleanup: true});

        // The fix: bare `Logger` becomes `bunyan.Logger`.
        expect(captured.get('log')).toBe('bunyan.Logger');

        // `new Bull('q')` is declared in bull's internal `Bull` namespace; the importable
        // package name is `bull`, so the FQN is rewritten to `bull.Queue` (see
        // type-mapping-namespace-replacement.test.ts for the dedicated coverage).
        expect(captured.get('queue')).toMatch(/^bull\.Queue/);
    }, 180000);
});
