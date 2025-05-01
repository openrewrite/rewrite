import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('with mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('with statement', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                with (0) {
                    console.log("aaa");
                }
            `)
        );
    });

    test('with statement with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                /*a*/with /*b*/ (/*c*/0 /*d*/) /*e*/{/*f*/
                    console.log("aaa");
                    /*g*/}/*h*/
            `)
        );
    });

    test('with statement with try-catch', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                with(ctx)try{return eval("("+str+")")}catch(e){}
            `)
        );
    });

    test('with statement with empty body', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                with (0) {/*a*/}
            `)
        );
    });

    test('with statement with body without braces', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                with (0) 1;
            `)
        );
    });

    test('with statement with await expr', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export {};
                with ( await obj?.foo) {}
            `)
        );
    });

    test('with statement with empty expr and body', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                with({/*a*/}) {/*b*/}
            `)
        );
    });

    test('with statement with multiline statement', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                with ([]) {
                    console.log("aaa");
                    console.log("bbb")
                }
            `)
        );
    });

    test('with statement with internal with statements', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                with (bindingContext) {
                    with (data || {}) {
                        with (options.templateRenderingVariablesInScope || {}) {
                            // Dummy [renderTemplate:...] syntax
                            result = templateText.replace(/\\[renderTemplate\\:(.*?)\\]/g, function (match, templateName) {
                                return ko.renderTemplate(templateName, data, options);
                            });


                            var evalHandler = function (match, script) {
                                try {
                                    var evalResult = eval(script);
                                    return (evalResult === null) || (evalResult === undefined) ? "" : evalResult.toString();
                                } catch (ex) {
                                    throw new Error("Error evaluating script: [js: " + script + "]\\n\\nException: " + ex.toString());
                                }
                            }

                            // Dummy [[js:...]] syntax (in case you need to use square brackets inside the expression)
                            result = result.replace(/\\[\\[js\\:([\\s\\S]*?)\\]\\]/g, evalHandler);

                            // Dummy [js:...] syntax
                            result = result.replace(/\\[js\\:([\\s\\S]*?)\\]/g, evalHandler);
                        }
                    }
                }
            `)
        );
    });

});
