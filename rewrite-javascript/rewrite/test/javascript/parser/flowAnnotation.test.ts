import {connect, disconnect, javaScript, rewriteRun, typeScript} from '../testHarness';

describe('flow annotation checking test', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('@flow in a one line comment in js', () => {
        const faultyTest = () => rewriteRun(
            //language=javascript
            javaScript(`
                //@flow

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        );

        expect(faultyTest).toThrow(/FlowSyntaxNotSupportedError/);
    });

    test('@flow in a comment in js', () => {
        const faultyTest = () => rewriteRun(
            //language=javascript
            javaScript(`
                /* @flow */

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        );

        expect(faultyTest).toThrow(/FlowSyntaxNotSupportedError/);
    });

    test('@flow in a multiline comment in js', () => {
        const faultyTest = () => rewriteRun(
            //language=javascript
            javaScript(`
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
        rewriteRun(
            //language=typescript
            typeScript(`
                //@flow

                import Rocket from './rocket';
                import RocketLaunch from './rocket-launch';
            `)
        );
    });

});
