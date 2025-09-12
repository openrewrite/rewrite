import ts from 'typescript';
import path from 'path';

// Create a simple test program
const testCode = `
import { ClipLoader } from 'react-spinners';

const App = () => {
  return <ClipLoader color="#36d7b7" />;
};
`;

const fileName = 'test.tsx';
const compilerOptions: ts.CompilerOptions = {
    target: ts.ScriptTarget.Latest,
    module: ts.ModuleKind.CommonJS,
    jsx: ts.JsxEmit.Preserve,
    moduleResolution: ts.ModuleResolutionKind.Node10,
    allowJs: true,
    esModuleInterop: true,
    allowSyntheticDefaultImports: true,
    baseUrl: './.working-dir'
};

// Create a compiler host
const host = ts.createCompilerHost(compilerOptions);
const originalGetSourceFile = host.getSourceFile;
host.getSourceFile = (fileName, languageVersion, onError) => {
    if (fileName === 'test.tsx') {
        return ts.createSourceFile(fileName, testCode, languageVersion, true);
    }
    return originalGetSourceFile(fileName, languageVersion, onError);
};

// Create the program
const program = ts.createProgram(['test.tsx'], compilerOptions, host);
const checker = program.getTypeChecker();
const sourceFile = program.getSourceFile('test.tsx');

if (sourceFile) {
    // Find the ClipLoader identifier
    function visit(node: ts.Node) {
        if (ts.isImportDeclaration(node)) {
            const clause = node.importClause;
            if (clause && clause.namedBindings && ts.isNamedImports(clause.namedBindings)) {
                for (const element of clause.namedBindings.elements) {
                    if (element.name.text === 'ClipLoader') {
                        const symbol = checker.getSymbolAtLocation(element.name);
                        if (symbol) {
                            const type = checker.getDeclaredTypeOfSymbol(symbol);
                            console.log('ClipLoader type:', checker.typeToString(type));
                            
                            // Try to get base types
                            const baseTypes = checker.getBaseTypes(type as ts.InterfaceType);
                            console.log('Base types count:', baseTypes?.length || 0);
                            if (baseTypes) {
                                for (const baseType of baseTypes) {
                                    console.log('Base type:', checker.typeToString(baseType));
                                }
                            }
                        }
                    }
                }
            }
        }
        ts.forEachChild(node, visit);
    }
    visit(sourceFile);
}
