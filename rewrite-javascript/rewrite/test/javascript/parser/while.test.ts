import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('while mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('empty while', () => {
        rewriteRun(
          //language=typescript
          typeScript('while (true);')
        );
    });

    test('empty while with empty statements', () => {
        rewriteRun(
            //language=typescript
            typeScript('while (true/*a*/);/*b*/;/*c*/')
        );
    });

    test('empty while with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('/*a*/while/*b*/ (/*c*/true/*d*/)/*e*/;/*f*/')
        );
    });

    test('empty while with empty statement', () => {
        rewriteRun(
            //language=typescript
            typeScript('while (true) { };')
        );
    });

    test('empty while with empty statement and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('/*a*/ while /*b*/(/*c*/true /*d*/)/*e*/ {/*f*/}/*g*/;/*h*/')
        );
    });

    test('while with statements', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                let count = 0;
                while (count < 10) {
                    console.log(count);
                    /*count*/
                    count++;
                };
            `)
        );
    });

    test('while-if with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript('function foo() { while (i--) if (nodeList[i] == elem) return true;}')
        );
    });

    test('if-do-while with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                if (true)
                    do console.log("a")
                    while (true)
                if (true) {
                    do console.log("b")
                    while (true)
                }
            `)
        );
    });

});
