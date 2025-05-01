import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';
import * as JS from '../../../dist/src/javascript';
import {JavaType} from "../../../dist/src/java";

describe('void operator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('void', () => {
        rewriteRun(
          //language=typescript
          typeScript('void 1', undefined, cu => {
              const statement = cu.statements[0] as JS.ExpressionStatement;
              expect(statement).toBeInstanceOf(JS.Void);
              const type = statement.type as JavaType.Primitive;
              expect(type.kind).toBe(JavaType.PrimitiveKind.Void);
          })
        );
    });
});
