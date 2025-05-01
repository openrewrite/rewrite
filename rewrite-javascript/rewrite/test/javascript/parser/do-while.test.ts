import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('do-while mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('empty do-while', () => {
        rewriteRun(
          //language=typescript
          typeScript('do {} while (true);')
        );
    });

    test('empty do-while with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('/*a*/ do /*b*/{/*c*/} /*d*/while/*e*/ (/*f*/true/*g*/)/*h*/;')
        );
    });

    test('empty do-while with expression and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('/*a*/ do /*b*/{/*c*/} /*d*/while/*e*/ (/*f*/Math/*0*/./*1*/random(/*2*/) /*3*/ > /*4*/0.7/*g*/)/*h*/;')
        );
    });

    test('do-while with statements', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                let count = 0;
                do {
                    console.log(count)
                    /*count*/
                    count++;
                } while (count < 10);
            `)
        );
    });

    test('do-while with labeled statement and semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function foo() {
                    partition: do {
                        break partition;
                    } while (from < to)/*a*/;/*b*/
                }
            `)
        );
    });

    test('do-while statement with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export function getMarkoRoot(path: t.NodePath<t.Node>) {
                    do curPath = curPath.parentPath/*a*/;/*b*/
                    while (curPath && !isMarko(curPath));
                    return curPath;
                }
            `)
        );
    });

});
