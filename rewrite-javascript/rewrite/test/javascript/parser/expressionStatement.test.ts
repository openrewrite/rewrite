import {connect, disconnect, rewriteRun, rewriteRunWithOptions, typeScript} from '../testHarness';

describe('expression statement mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('literal with semicolon', () => {
        rewriteRunWithOptions(
            {normalizeIndent: false},
            typeScript('1 ;')
        );
    });
    test('multiple', () => {
        rewriteRunWithOptions(
            {normalizeIndent: false},
            typeScript(
                //language=ts
                `
                    1; // foo
                    // bar
                    /*baz*/
                    2;`
            )
        );
    });

    test('simple non-null expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const length = user ! . profile ! . username ! . length ;
            `)
        );
    });

    test('simple non-null expression with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const length = /*0*/user/*a*/!/*b*/./*c*/ profile /*d*/!/*e*/ ./*f*/ username /*g*/!/*h*/ ./*j*/ length/*l*/ ;
            `)
        );
    });

    test('simple question-dot expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const length = user ?. profile ?. username ?. length ;
            `)
        );
    });

    test('simple question-dot expression with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const length = /*0*/user/*a*/ ?./*b*/ profile/*c*/ ?./*d*/ username /*e*/?./*f*/ length /*g*/;
            `)
        );
    });

    test('simple default expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const length = user ?? 'default' ;
            `)
        );
    });

    test('simple default expression with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const length = user /*a*/??/*b*/ 'default' /*c*/;
            `)
        );
    });

    test('mixed expression with special tokens', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                class Profile {
                    username?: string; // Optional property
                }

                class User {
                    profile?: Profile; // Optional property
                }

                function getUser(id: number) : User | null {
                    if (id === 1) {
                        return new User();
                    }
                    return null;
                }

                const user = getUser(1);
                const length = user  ! .   profile ?.  username  !. length /*test*/ ;
                const username2 = getUser(1) ! .  profile  ?. username ; // test;
                const username = user!.profile?.username ?? 'Guest' ;
            `)
        );
    });

    test('mixed expression with methods with special tokens', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                interface Profile {
                    username?(): string; // Optional property

                }

                interface User {
                    profile?(): Profile; // Optional property
                }

                function getUser(id: number) : User | null {
                    return null;
                }

                const user = getUser(1);
                const username1 = user  ! .   profile() ?.  username()  !. toLowerCase() /*test*/ ;
                const username2 = getUser(1) ! .  profile()  ?. username() ; // test;
                const username3 = getUser(1) ?. profile()?.username() ?? 'Guest' ;
            `)
        );
    });

    test('optional chaining operator with ?.', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
            const func1 = (msg: string) => {
                return {
                    func2: (greeting: string) => greeting + msg
                };
            };

            const result1 = func1?.("World")?.func2("Hello"); // Invokes func1 and then func2 if func1 is not null/undefined.
            `)
        );
    });

    test('optional chaining operator with ?. and custom type', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const func1: ((msg: string) => { func2: (greeting: string) => string }) | undefined = undefined;
                const result2 = func1?.("Test")?.func2("Hi"); // Does not invoke and returns \`undefined\`.
            `)
        );
    });

    test('satisfies expression', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type Person = {
                    name: string;
                    age: number;
                };

                const user = /*o*/ {
                    name: "Alice",
                    age: 25,
                    occupation: "Engineer"
                } /*a*/ satisfies /*b*/ Person /*c*/;
            `)
        );
    });

    test('atisfies expression with complex type ', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                type ApiResponse<T> = {
                    data: T;
                    status: "success" | "error";
                };

                const response = {
                    data: { userId: 1 },
                    status: "success",
                } satisfies ApiResponse<{ userId: number }>;
            `)
        );
    });

    test('debugging statement', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
              function calculate(value: number) {
                  /*a*/debugger/*b*/;/*c*/ // Pauses execution when debugging
                  return value * 2;
              }
          `)
        );
    });

    test('shorthand property assignment with initializer', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                ({
                    initialState,
                    resultSelector/*a*/ = /*b*/identity as ResultFunc<S, T>,
                } = initialStateOrOptions as GenerateOptions<T, S>);
            `)
        );
    });

    test('new expression with array access', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const results = new this.constructor[Symbol.species]<Key, Value>();
            `)
        );
    });

});
