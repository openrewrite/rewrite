/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";



describe('class decorator mapping', () => {
    const spec = new RecipeSpec();

    test('unqualified', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('@foo class A {}')
        );
    });
    test('unqualified parens', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('@foo( ) class A {}')
        );
    });
    test('qualified', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('@foo . bar class A {}')
        );
    });
    test('qualified parens', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('@foo . bar ( ) class A {}')
        );
    });
    test('parameter decorator with params', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
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
       return spec.rewriteRun(
          //language=typescript
          typescript(`
              @StaticInterfaceImplement/*a*/<ISpriteAssembler>/*b*/()
              export class SimpleSpriteAssembler {}
          `)
        );
    });
    test('decorator with parenthesized expression', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
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
       return spec.rewriteRun(
          //language=typescript
          typescript(`
              const Foo = (x => x)(@dec('') class { })
          `)
        );
    });
    test('class / method / params / properties decorators', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript(`
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
       return spec.rewriteRun(
            //language=typescript
            typescript(`
                export @decorator() class Foo {}
                export default @decorator() class {}
            `)
        );
    });
});
