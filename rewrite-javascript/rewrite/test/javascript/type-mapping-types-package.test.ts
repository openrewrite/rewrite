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
import {javascript, JavaScriptVisitor, npm, packageJson, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";
import {withDir} from "tmp-promise";

/**
 * Captures the resolved FQN of the value type attributed to identifiers with the
 * given names. The FQN of an identifier's type is what downstream recipes gate on
 * when detecting framework objects (e.g. an Express `Application`/`Router`).
 */
function captureIdentifierTypes(names: string[], sink: Map<string, string>): Recipe {
    class CaptureRecipe extends Recipe {
        name = 'org.openrewrite.javascript.test.CaptureIdentifierTypes';
        displayName = 'Capture identifier types';
        description = 'Records the resolved type FQN of selected identifiers.';

        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitIdentifier(ident: J.Identifier, p: ExecutionContext): Promise<J.Identifier> {
                    const visited = await super.visitIdentifier(ident, p) as J.Identifier;
                    if (names.includes(visited.simpleName)) {
                        const type = visited.type;
                        sink.set(visited.simpleName, Type.isClass(type) ? type.fullyQualifiedName : Type.signature(type));
                    }
                    return visited;
                }
            };
        }
    }

    return new CaptureRecipe();
}

const TS_SNIPPET = `
import express from 'express';
const app = express();
const router = express.Router();
app.get('/users', (req, res) => res.send('ok'));
router.post('/items', (req, res) => res.send('ok'));
`;

const JS_SNIPPET = `
const express = require('express');
const app = express();
const router = express.Router();
app.get('/users', (req, res) => res.send('ok'));
router.post('/items', (req, res) => res.send('ok'));
`;

function expressPackageJson(version: string, typesVersion: string): string {
    return `{
  "name": "express-fqn-fixture",
  "version": "1.0.0",
  "dependencies": { "express": "${version}" },
  "devDependencies": { "@types/express": "${typesVersion}" }
}`;
}

describe('@types package FQN normalization', () => {
    // Express's runtime functions return the interfaces declared in
    // `express-serve-static-core` (e.g. `function e(): core.Express`,
    // `Router(): core.Router`). The declaration package is the correct FQN root
    // (OpenRewrite names types by their declaration site), but it must be the
    // importable specifier `express-serve-static-core`, NOT `@types/express-serve-static-core`,
    // which is never something you can `import ... from`.

    test('@types/express v4: receivers resolve to express-serve-static-core.*', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureIdentifierTypes(['app', 'router'], captured);

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(TS_SNIPPET),
                    //language=json
                    packageJson(expressPackageJson('^4.18.2', '^4.17.21'))
                )
            );
        }, {unsafeCleanup: true});

        expect(captured.get('app')).toBe('express-serve-static-core.Express');
        expect(captured.get('router')).toBe('express-serve-static-core.Router');
    }, 120000);

    test('@types/express v5: receivers resolve to express-serve-static-core.*', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureIdentifierTypes(['app', 'router'], captured);

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(TS_SNIPPET),
                    //language=json
                    packageJson(expressPackageJson('^5.0.0', '^5.0.0'))
                )
            );
        }, {unsafeCleanup: true});

        expect(captured.get('app')).toBe('express-serve-static-core.Express');
        expect(captured.get('router')).toBe('express-serve-static-core.Router');
    }, 120000);

    test('plain JS without @types stays <unknown> (no synthetic type invented)', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureIdentifierTypes(['app', 'router'], captured);

        //language=javascript
        await spec.rewriteRun(javascript(JS_SNIPPET));

        expect(captured.get('app')).toBe('<unknown>');
        expect(captured.get('router')).toBe('<unknown>');
    }, 60000);
});
