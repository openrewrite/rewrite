import {CategoryDescriptor, JavaScript} from "../../marketplace";

export * from './find-dependency';
export * from './is-vendored-or-bundled';
export * from './uses-method';
export * from './uses-type';

export const Search: CategoryDescriptor[] = [...JavaScript, {displayName: "Search"}]
