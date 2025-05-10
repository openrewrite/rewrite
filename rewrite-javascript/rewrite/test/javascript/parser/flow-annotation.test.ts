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
import {RecipeSpec, SourceSpec} from "../../../src/test";
import {javascript} from "../../../src/javascript";
import {MarkersKind, ParseExceptionResult, SourceFile} from "../../../src";

describe('flow annotation checking test', () => {
    const spec = new RecipeSpec();

    test('@flow in a one line comment in js', () =>
        expectFlowSyntaxError(
            //language=javascript
            javascript(`
                //@flow
                // noinspection JSAnnotator

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        ));

    test('@flow in a comment in js', () =>
        expectFlowSyntaxError(
            //language=javascript
            javascript(`
                /* @flow */

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        ));

    test('@flow in a multiline comment in js', () =>
        expectFlowSyntaxError(
            //language=javascript
            javascript(`
                /*
                    @flow
                */

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        ));

    test('@flow in a comment in ts', () =>
        expectFlowSyntaxError(
            //language=javascript
            javascript(`
                //@flow

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        ));

    async function expectFlowSyntaxError(sourceSpec: SourceSpec<any>) {
        await expect(spec.rewriteRun(sourceSpec)).rejects.toThrow('FlowSyntaxNotSupportedError');
    }
});
