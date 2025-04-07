import {
    Column,
    DataTable,
    ExecutionContext,
    Option,
    Recipe,
    Registered,
    ScanningRecipe,
    SourceFile,
    Transient,
    TreeVisitor
} from "../../main/javascript";
import {PlainText, PlainTextParser, PlainTextVisitor} from "../../main/javascript/text";

export class ReplacedText {
    @Column({
        displayName: "Source Path",
        description: "The path of the file that was changed."
    })
    readonly sourcePath: string

    @Column({
        displayName: "Text",
        description: "The text that was replaced."
    })
    readonly text: string

    constructor(sourcePath: string, text: string) {
        this.sourcePath = sourcePath;
        this.text = text;
    }

    static dataTable = new DataTable<ReplacedText>(
        "org.openrewrite.text.replaced-text",
        "Replaced text",
        "Text that was replaced.",
        ReplacedText
    );
}

@Registered("org.openrewrite.text.create-text")
export class CreateText extends ScanningRecipe<{ exists: boolean }> {
    displayName: string = "Create text file";
    description: string = "Create a new text file.";

    @Option({
        displayName: "Text",
        description: "Text to change to"
    })
    text!: string

    @Option({
        displayName: "Source path",
        description: "The source path of the file to create."
    })
    sourcePath!: string

    constructor(options: { text: string, sourcePath: string }) {
        super(options);
    }

    initialValue(_ctx: ExecutionContext): { exists: boolean; } {
        return {exists: false};
    }

    async generate(acc: { exists: boolean }, _ctx: ExecutionContext): Promise<SourceFile[]> {
        if (acc.exists) {
            return [];
        }
        return new PlainTextParser().parse({text: this.text, sourcePath: this.sourcePath});
    }
}

@Registered("org.openrewrite.text.change-text")
export class ChangeText extends Recipe {
    displayName = "Change text";
    description = "Change the text of a file.";

    @Option({
        displayName: "Text",
        description: "Text to change to"
    })
    text!: string;

    constructor(options: { text: string }) {
        super(options);
    }

    @Transient
    replacedText = ReplacedText.dataTable;

    instanceName(): string {
        return `Change text to '${this.text}'`;
    }

    get editor(): TreeVisitor<any, ExecutionContext> {
        const toText = this.text;
        const replacedText = this.replacedText;
        return new class extends PlainTextVisitor<ExecutionContext> {
            visitText(text: PlainText, ctx: ExecutionContext): Promise<PlainText> {
                return this.produceTree(text, ctx, draft => {
                    if (draft.text != toText) {
                        replacedText.insertRow(ctx, new ReplacedText(text.sourcePath, draft.text));
                    }
                    draft.text = toText;
                })
            }
        }
    }
}

@Registered("org.openrewrite.text.with-recipe-list")
export class RecipeWithRecipeList extends Recipe {
    displayName = "A recipe that has a recipe list";
    description = "To verify that it is possible for a recipe list to be called over RPC.";
    recipeList = [new ChangeText({text: "hello"})];
}
