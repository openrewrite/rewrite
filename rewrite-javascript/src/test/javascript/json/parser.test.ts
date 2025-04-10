import {RecipeSpec} from "../../../main/javascript/test";
import {json} from "../../../main/javascript/json";

describe('JSON parsing', () => {
    const spec = new RecipeSpec();

    test('parses JSON', () => spec.rewriteRun(
        //language=json
        json(
            `
              {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string"
                  }
                }
              }
            `
        )
    ));
});
