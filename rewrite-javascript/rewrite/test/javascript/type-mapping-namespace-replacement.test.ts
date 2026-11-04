// noinspection JSUnusedLocalSymbols
import {RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, npm, packageJson, tsx, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";
import {withDir} from "tmp-promise";

function sig(t: Type | undefined): string {
    if (!t) return "none";
    if (Type.isClass(t)) return t.fullyQualifiedName;
    return Type.signature(t);
}

function captureIdentifierTypes(names: string[], sink: Map<string, string>): Recipe {
    class C extends Recipe {
        name = 'org.openrewrite.javascript.test.CaptureNamespaceReplacement';
        displayName = 'capture';
        description = 'capture';
        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitIdentifier(ident: J.Identifier, p: ExecutionContext): Promise<J.Identifier> {
                    const v = await super.visitIdentifier(ident, p) as J.Identifier;
                    if (names.includes(v.simpleName)) {
                        sink.set(v.simpleName, sig(v.type));
                    }
                    return v;
                }
            };
        }
    }
    return new C();
}

describe('internal namespace replaced with importable package name', () => {
    test('Bull.Queue -> bull.Queue, request.SuperAgentStatic -> superagent.*', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureIdentifierTypes(['queue', 'sa'], captured);

        const src = `
import Bull from 'bull';
import superagent from 'superagent';
const queue = new Bull('q');
const sa = superagent;
queue.add('job', {});
sa.get('/x');
`;
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(repo.path,
                    //language=typescript
                    typescript(src),
                    //language=json
                    packageJson(`{
                      "name": "ns-fixture", "version": "1.0.0",
                      "dependencies": { "bull": "^4.12.0", "superagent": "^8.1.2" },
                      "devDependencies": { "@types/bull": "^3.15.9", "@types/superagent": "^8.1.0" }
                    }`)
                )
            );
        }, {unsafeCleanup: true});

        expect(captured.get('queue')).toMatch(/^bull\.Queue/);
        expect(captured.get('sa')).toMatch(/^superagent\.SuperAgentStatic/);
    }, 180000);

    test('UMD global namespace (React) is preserved', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureIdentifierTypes(['comp'], captured);

        const src = `
import * as React from 'react';
const comp: React.Component = null as any;
`;
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(repo.path,
                    //language=tsx
                    tsx(src),
                    //language=json
                    packageJson(`{
                      "name": "react-fixture", "version": "1.0.0",
                      "devDependencies": { "@types/react": "^18.2.0" }
                    }`)
                )
            );
        }, {unsafeCleanup: true});

        // UMD guard: must stay `React.*`, NOT become `react.*`.
        expect(captured.get('comp')).toMatch(/^React\.Component/);
    }, 180000);
});
