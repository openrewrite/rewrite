import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('if mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('if (true) console.log("foo");')
        );
    });

    test('simple with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript('/*a*/if /*b*/(/*c*/true/*d*/)/*e*/ console.log("foo")/*f*/;/*g*/')
        );
    });

    test('braces', () => {
        rewriteRun(
          //language=typescript
          typeScript('if (true) /*a*/{/*b*/ console.log("foo")/*c*/; /*d*/}/*e*/')
        );
    });
    test('else', () => {
        rewriteRun(
          //language=typescript
          typeScript('if (true) console.log("foo"); /*a*/ else/*b*/ console.log("bar")/*c*/;/*d*/')
        );
    });

    test('if-else with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                if (true) {
                    console.log("foo")/*a*/;/*b*/
                } else
                    console.log("bar")/*a*/;/*b*/
            `)
        );
    });

    test('if-else-if with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                    if (false)
                        console.log("foo")/*b*/;/*c*/
                    else /*d*/if (true)
                        console.log("bar")/*e*/;/*f*/
            `)
        );
    });

    test('if-if-else with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                    if (false)
                        /*a*/if (true)
                            console.log("foo")/*b*/;/*c*/
                        else /*d*/if (true)
                            console.log("bar")/*e*/;/*f*/
            `)
        );
    });

    test('if with for with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                if (prevProps)
                    for (let name in prevProps) name in nextProps || (node[name] = void 0)/*a*/;/*b*/
            `)
        );
    });

    test('for with if with return and semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function foo() {
                    for (let opt of el.options)
                        if (opt.selected !== opt.defaultSelected) return !0;
                }
            `)
        );
    });


    test('for with if with semicolon', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                for(;;)
                    if (true)
                        console.log("foo")/*a*/;/*b*/
            `)
        );
    });

    test('if with return', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function foo() {
                    if (prevProps)
                        return abs(prevProps)/*a*/;/*b*/
                }
            `)
        );
    });

    test('if with break', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                for(;;) {
                    if (!len--) break;
                }
            `)
        );
    });

    test('if with continue', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                for(;;) {
                    if (!len--) continue;
                }
            `)
        );
    });

    test('if with do', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                if (newStart > newEnd)
                    do {
                        abs(x);
                    } while (oldStart <= oldEnd);
            `)
        );
    });

});
