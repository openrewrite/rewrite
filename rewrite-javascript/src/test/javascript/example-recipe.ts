import {
    Column,
    DataTable,
    ExecutionContext,
    Option,
    Recipe,
    Registered,
    Transient,
    TreeVisitor
} from "../../main/javascript";
import {PlainText, PlainTextVisitor} from "../../main/javascript/text";

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

export class ChangeTextVisitor<P> extends PlainTextVisitor<P> {
    text: string = "Hello world!";

    visitText(text: PlainText, p: P): Promise<PlainText> {
        return this.produceTree(text, p, draft => {
            draft.text = "Hello World!";
        })
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
    text: string = "Hello World!"

    constructor(options: { text: string }) {
        super();
        if (options) {
            this.text = options.text;
        }
    }

    @Transient
    replacedText = ReplacedText.dataTable;

    get editor(): TreeVisitor<any, ExecutionContext> {
        let visitor = new ChangeTextVisitor<ExecutionContext>();
        visitor.text = this.text;
        return visitor
    }
}

@Registered("org.openrewrite.text.with-recipe-list")
export class RecipeWithRecipeList extends Recipe {
    displayName = "A recipe that has a recipe list";
    description = "To verify that it is possible for a recipe list to be called over RPC.";
    recipeList = [new ChangeText({text: "hello"})];
}
