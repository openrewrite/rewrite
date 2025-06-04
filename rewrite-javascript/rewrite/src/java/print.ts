import {PrintOutputCapture, TreePrinters} from "../print";
import {J} from "./tree";
import {JavaVisitor} from "./visitor";

class JavaPrinter extends JavaVisitor<PrintOutputCapture> {
    protected async visitCompilationUnit(): Promise<J | undefined> {
        throw new Error("Printing Java source files from JavaScript is not supported.");
    }
}

TreePrinters.register(J.Kind.CompilationUnit, () => new JavaPrinter());
