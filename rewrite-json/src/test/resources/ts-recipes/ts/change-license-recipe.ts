import { OpenRewrite } from '@openrewrite/types';

export class Options {
  public license: string;

  public getOptionsDescriptors() {
    return [
      {
        displayName: 'license',
        description: 'Enter the license',
        required: true
      }
    ];
  }

  public setOptionsValue(values: { [key: string]: string }) {
    this.license = values['license'];
  }
}

export default class ChangeLicenseRecipe extends OpenRewrite.Recipe<Options> {
  public recipeList: OpenRewrite.RecipeDescriptor[]
  
  constructor(options?: Options) {
    super(options);
    super.doNext<OpenRewrite.JsonChangeValue>({
      name: 'org.openrewrite.json.ChangeValue',
      options: {
        oldKeyPath: '$.license',
        value: options?.license || '',
        fileMatcher: '**/package.json'
      }
    });
  }

  public getDisplayName = () => {
    return `Change License`;
  };

  public getDescription = () => {
    return 'Changes License in package.json';
  };

  public getTags(): string[] {
    return [''];
  }

  public getLanguages(): string[] {
    return ['json'];
  }
}
