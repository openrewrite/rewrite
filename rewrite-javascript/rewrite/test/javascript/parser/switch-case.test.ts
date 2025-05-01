import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('switch-case mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('empty switch', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              let txt: string;
              switch /*a*/(/*b*/txt/*c*/)/*d*/ {
                  /*e*/
              }
          `)
        );
    });

    test('simple switch-case', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                let txt: string;
                switch (txt) {
                    case 'a':
                        console.log('A');
                        break;
                }
            `)
        );
    });

    test('simple switch-case with comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
              let  txt: string;
              switch (txt) {
                  /*a*/ case /*b*/'a'/*c*/:/*d*/
                      console.log('A');
                      /*e*/break /*f*/;/*g*/
              }
          `)
        );
    });

    test('switch-case with several different cases', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                let txt: string;
                switch (txt) {
                    // first
                    case 'a':
                        console.log('A');
                        break;
                    //second
                    case 'b':
                        console.log('B');
                        break;
                }
            `)
        );
    });

    test('switch-case with several cases with one body', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                let txt: string;
                switch (txt) {
                    // first
                    case 'a':
                    //second
                    case 'b':
                        console.log('B');
                        break;
                }
            `)
        );
    });

    test('switch-case with cases and default', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                let txt: string;
                switch (txt) {
                    // first
                    case 'a':
                        console.log('A');
                        break
                    //second
                    case 'b':
                        console.log('B')
                        break;
                    //default
                    default:
                        console.log('C, ...')
                        break
                }
            `)
        );
    });

    test('switch-case with default and comments', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                let txt: string;
                switch (txt) {
                    //default
                    /*a*/default/*b*/:/*c*/
                        console.log('C, ...');
                        break;
                }
            `)
        );
    });
});
