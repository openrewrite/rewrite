import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('mapped type mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type OptionsFlags<Type> = {
                    [Property in keyof Type]: /*a*/boolean/*b*/
                };
            `)
        );
    });

    test('with readonly', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type ReadonlyType<T> = {
                  /*a*/readonly/*b*/ [K in keyof T]: T[K];
              };
          `)
        );
    });

    test('with -readonly', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type CreateMutable<Type> = {
                  /*a*/-/*b*/readonly/*c*/[Property in keyof Type]: Type[Property];
              };
          `)
        );
    });

    test('with suffix +?', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type Concrete<Type> = {
                  [Property in keyof Type]+?: Type[Property];
              };
          `)
        );
    });

    test('with suffix -?', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Concrete<Type> = {
                    [Property in keyof Type]/*a*/-/*b*/?/*c*/: Type[Property];
                };
            `)
        );
    });

    test('record mapped type', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type Record<K extends keyof any, T> = {
                  [P in K]: T;
                  /*a*/}/*b*/
              ;
          `)
        );
    });

    test('record mapped type with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Record<K extends keyof any, T> = /*a*/{ /*b*/
                    /*0*/[/*1*/P /*2*/in /*3*/K/*4*/]/*c*/:/*d*/ T/*e*/;/*f*/
                }/*g*/;
            `)
        );
    });

    test('mapped type with "as" nameType', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type ReadonlyRenamed<T> = {
                  + readonly [K in keyof T as \`readonly_\${string & K\}\`]: T[K];
              };
          `)
        );
    });

    test('recursive mapped types', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type DeepPartial<T> = {
                  [K in keyof T]/*a*/?/*b*/: T[K] extends object /*c*/? DeepPartial<T[K]> /*d*/: T[K];
              };
          `)
        );
    });

    test('with function type', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              type Getters<Type> = {
                  [Property in keyof Type as \`get\${Capitalize<string & Property>/*a*/}\`]: () => Type[Property] /*b*/
              };
          `)
        );
    });

    test('with repeating tokens', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type EventConfig<Events extends { kind: string }> = {
                    /*a*/[/*b*/E /*c*/in /*d*/Events as /*e*/E/*f*/[/*g*/"kind"/*h*/]/*i*/]/*j*/:/*k*/ (/*l*/event/*m*/: /*n*/E)/*o*/ => void;
                }
          `)
        );
    });

    test('with conditional', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ExtractPII<Type> = {
                    [Property in keyof Type]: Type[Property] extends { pii: true } ? true : false;
                };
          `)
        );
    });

    test('mapped types intersection', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type MakeSpecificKeysReadonly<T, K extends keyof T> = {
                    readonly [P in K]: T[P];
                } & {
                    [P in Exclude<keyof T, K>]: T[P];
                };
          `)
        );
    });

    test('specific type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type UnionMapper<T> = {
                    [K in T]: { type: K };
                }[T];
          `)
        );
    });

    test('complex key remapping', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type SnakeCaseKeys<T> = {
                    [K in keyof T as K extends string
                        ? \`\${K extends \`\${infer First}\${infer Rest}\`
                        ? \`\${Lowercase<First>}\${Rest extends Capitalize<Rest> ? \`_\${Lowercase<Rest>}\` : Rest}\`
                        : K}\`
                    : never]: T[K];
                };
            `)
        );
    });

    test('no node type ', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Type = {
                    // comment
                    readonly [T in number] /*a*/
                };
            `)
        );
    });

    test('no node type with ;', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Type = {
                    // comment
                    readonly [T in number] /*a*/;/*b*/
                };
          `)
        );
    });

});
