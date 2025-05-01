import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('class decorator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('unqualified', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo class A {}')
        );
    });
    test('unqualified parens', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo( ) class A {}')
        );
    });
    test('qualified', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo . bar class A {}')
        );
    });
    test('qualified parens', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo . bar ( ) class A {}')
        );
    });
    test('parameter decorator with params', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              export class WorkspaceMemberWorkspaceEntity extends BaseWorkspaceEntity {
                  @WorkspaceField({
                      standardId: WORKSPACE_MEMBER_STANDARD_FIELD_IDS.name,
                      type: FieldMetadataType.FULL_NAME,
                      label: 'Name',
                      description: 'Workspace member name',
                      icon: 'IconCircleUser',
                  })
                  [NAME_FIELD_NAME]: FullNameMetadata;
              }
          `)
        );
    });
    test('decorator with type params', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              @StaticInterfaceImplement/*a*/<ISpriteAssembler>/*b*/()
              export class SimpleSpriteAssembler {}
          `)
        );
    });
    test('decorator with parenthesized expression', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              class SimpleSpriteAssembler {
                  @/*a*/(/*b*/Ember.computed('fullName').readOnly()/*c*/)/*d*/
                  get fullNameReadonly() {
                      return 'fullName';
                  }
              }
          `)
        );
    });
    test.skip('decorator on class expression', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              const Foo = (x => x)(@dec('') class { })
          `)
        );
    });
    test('class / method / params / properties decorators', () => {
        rewriteRun(
          //language=typescript
          typeScript(`
              @UseGuards(WorkspaceAuthGuard)
              @Resolver()
              export class RelationMetadataResolver {
                  constructor(
                      @Args('input')
                      private readonly relationMetadataService: RelationMetadataService,
                  ) {}

                  @Args('input') input: DeleteOneRelationInput;

                  @Mutation(() => RelationMetadataDTO)
                  async deleteOneRelation(
                      @Args('input') input: DeleteOneRelationInput,
                      @AuthWorkspace() { id: workspaceId }: Workspace,
                  ) {
                      try {
                          return await this.relationMetadataService.deleteOneRelation(
                              input.id,
                              workspaceId,
                          );
                      } catch (error) {
                          relationMetadataGraphqlApiExceptionHandler(error);
                      }
                  }
              }
          `)
        );
    });

    test.skip('decorator after modifiers', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export @decorator() class Foo {}
                export default @decorator() class {}
            `)
        );
    });
});

// according to TypeScript documentation decorators are not allowed with
// standalone functions https://www.typescriptlang.org/docs/handbook/decorators.html
describe.skip('function decorator mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('unqualified', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo function f() {}')
        );
    });
    test('unqualified parens', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo( ) function f() {}')
        );
    });
    test('qualified', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo . bar function f() {}')
        );
    });
    test('qualified parens', () => {
        rewriteRun(
          //language=typescript
          typeScript('@foo . bar ( ) function f() {}')
        );
    });
});
