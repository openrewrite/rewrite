import * as J from "../../../dist/src/java";
import * as JS from "../../../dist/src/javascript/tree";
import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('object literal mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('empty', () => {
        rewriteRun(
          //language=typescript
          typeScript('const c = {}')
        );
    });

    test('single', () => {
        rewriteRun(
          //language=typescript
          typeScript('const c = { foo: 1 }')
        );
    });

    test('multiple', () => {
        rewriteRun(
          //language=typescript
          typeScript('const c = { foo: 1, bar: 2 }')
        );
    });
    test('trailing comma', () => {
        rewriteRun(
          //language=typescript
          typeScript('const c = { foo: 1, /*1*/ }')
        );
    });
    test('string key', () => {
        rewriteRun(
          //language=typescript
          typeScript('const c = { "foo": 1 }')
        );
    });
    test('undefined key', () => {
        rewriteRun(
          //language=typescript
          typeScript('const c = { undefined: 1 }')
        );
    });
    test('computed property', () => {
        rewriteRun(
          //language=typescript
          typeScript(
            'const c = { [ 1 + 1 ] : 1 }', undefined,
            cu => {
                const literal = (<J.NewClass>(<J.VariableDeclarations>(<JS.ScopedVariableDeclarations>cu.statements[0]).variables[0]).variables[0].initializer);
                expect(literal.body).toBeDefined();
                const computedName = (<J.NewArray>(<JS.PropertyAssignment>literal.body?.statements[0]).name);
                expect(computedName).toBeDefined();
                const expression = <J.Binary>computedName.initializer![0];
                expect(expression).toBeDefined();
                expect((<J.Literal>expression.left).valueSource).toBe("1");
            }
          )
        );
    });
});
