import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';
import * as JS from '../../../dist/src/javascript';
import {JavaType} from "../../../dist/src/java";

describe('yield mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('function* generatorFunction() { yield 42;}')
        );
    });
    test('empty', () => {
        rewriteRun(
          //language=typescript
          typeScript('yield')
        );
    });
    test('delegated', () => {
        rewriteRun(
          //language=typescript
          typeScript('yield* other')
        );
    });

    test('yield expression', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              DenseMatrix.prototype[Symbol.iterator] = function* () {
                  const recurse = function* (value, index) {
                      if (isArray(value)) {
                          for (let i = 0; i < value.length; i++) {
                              yield* recurse(value[i], index.concat(i))
                          }
                      } else {
                          yield ({value, index})
                      }
                  }
                  yield/*a*/* /*b*/recurse(this._data, [])
              }
          `)
        );
    });
});
