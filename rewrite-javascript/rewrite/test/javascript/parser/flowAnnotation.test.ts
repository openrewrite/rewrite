/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {RecipeSpec} from "../../../src/test";
import {javascript, typescript} from "../../../src/javascript";

describe('flow annotation checking test', () => {
    const spec = new RecipeSpec();

    test('@flow in a one line comment in js', () => {
        const faultyTest = () =>spec.rewriteRun(
            //language=javascript
            javascript(`
                //@flow
                // noinspection JSAnnotator

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        );

        expect(faultyTest).toThrow(/FlowSyntaxNotSupportedError/);
    });

    test('@flow in a comment in js', () => {
        const faultyTest = () =>spec.rewriteRun(
            //language=javascript
            javascript(`
                /* @flow */

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        );

        expect(faultyTest).toThrow(/FlowSyntaxNotSupportedError/);
    });

    test('@flow in a multiline comment in js', () => {
        const faultyTest = () =>spec.rewriteRun(
            //language=javascript
            javascript(`
                /*
                    @flow
                */

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        );

        expect(faultyTest).toThrow(/FlowSyntaxNotSupportedError/);
    });

    test('@flow in a comment in ts', () => {
        // noinspection ES6UnusedImports,TypeScriptCheckImport
        return spec.rewriteRun(
            //language=typescript
            typescript(`
                //@flow

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        );
    });
});
