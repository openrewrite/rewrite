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

function captureAnnotationTypes(sink: Map<string, string>): Recipe {
    class C extends Recipe {
        name = 'org.openrewrite.javascript.test.CaptureAnnotationTypes';
        displayName = 'capture';
        description = 'capture';
        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitAnnotation(a: J.Annotation, p: ExecutionContext): Promise<J.Annotation> {
                    const v = await super.visitAnnotation(a, p) as J.Annotation;
                    const at = v.annotationType as J.Identifier;
                    if (at && at.simpleName) {
                        sink.set(at.simpleName, fqn(at.type));
                    }
                    return v;
                }
            };
        }
    }
    return new C();
}

describe('decorator type attribution', () => {
    test('typeorm decorators resolve to their fully qualified package name', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureAnnotationTypes(captured);

        const src = `
import {Entity, Column} from 'typeorm';

@Entity()
class User {
    @Column()
    name: string = '';
}
`;
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(repo.path,
                    //language=typescript
                    typescript(src),
                    //language=json
                    packageJson(`{
                      "name": "decorator-fixture", "version": "1.0.0",
                      "dependencies": { "typeorm": "^0.3.20", "reflect-metadata": "^0.2.1" }
                    }`)
                )
            );
        }, {unsafeCleanup: true});

        expect(captured.get('Entity')).toBe('typeorm.Entity');
        expect(captured.get('Column')).toBe('typeorm.Column');
    }, 240000);
});
