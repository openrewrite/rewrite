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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('rewriting calls that carry type arguments', () => {
    const spec = new RecipeSpec();

    // The template defines the output, so a rule that keeps the type argument names it and goes first.
    test('a rule that names the type argument keeps it, ahead of one that does not', async () => {
        const t = capture('t'), a = capture('a'), b = capture('b');
        const rule = rewrite(() => ({
            before: pattern`useState<${t}>(${a})`,
            after: template`useMyState<${t}>(${a})`
        })).orElse(rewrite(() => ({
            before: pattern`useState(${b})`,
            after: template`useMyState(${b})`
        })));

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                method = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, method) || method;
            }
        });

        await spec.rewriteRun(
            //language=typescript
            typescript(
                `const a = useState<string[]>([]);\nconst b = useState([]);`,
                `const a = useMyState<string[]>([]);\nconst b = useMyState([]);`
            )
        );
    });
});
