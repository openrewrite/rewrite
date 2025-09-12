import ts from "typescript";

const code = `let x = /pattern/gi;`;
const sourceFile = ts.createSourceFile("test.ts", code, ts.ScriptTarget.Latest, true);
const program = ts.createProgram(["test.ts"], {
    getSourceFile: (fileName) => fileName === "test.ts" ? sourceFile : undefined,
    writeFile: () => {},
    getCurrentDirectory: () => "",
    getDirectories: () => [],
    fileExists: () => true,
    readFile: () => "",
    getCanonicalFileName: (fileName) => fileName,
    useCaseSensitiveFileNames: () => true,
    getNewLine: () => "\n",
    getDefaultLibFileName: () => "lib.d.ts"
}, {
});

const checker = program.getTypeChecker();

function visit(node: ts.Node) {
    if (ts.isRegularExpressionLiteral(node)) {
        const type = checker.getTypeAtLocation(node);
        console.log("Regex literal found:", node.getText());
        console.log("Type flags:", type.flags);
        console.log("Type string:", checker.typeToString(type));
        console.log("Symbol:", type.symbol?.name);
        const regExpSymbol = checker.resolveName("RegExp", undefined, ts.SymbolFlags.Type, false);
        console.log("RegExp symbol:", regExpSymbol?.name);
        console.log("Is same RegExp symbol?", type.symbol === regExpSymbol);
        
        // Check if it's an object type
        if (type.flags & ts.TypeFlags.Object) {
            console.log("It's an object type");
            const objectType = type as ts.ObjectType;
            console.log("Object flags:", objectType.objectFlags);
        }
    }
    ts.forEachChild(node, visit);
}

visit(sourceFile);
