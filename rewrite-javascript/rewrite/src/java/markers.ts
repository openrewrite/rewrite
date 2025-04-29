import {Marker} from "../markers";
import {Space} from "./tree";

export interface Semicolon extends Marker {
}

export interface TrailingComma extends Marker {
    readonly suffix: Space;
}

export interface OmitParentheses extends Marker {
}
