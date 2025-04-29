import {Marker} from "../markers";
import {Space} from "./tree";

export interface Semicolon extends Marker {
    kind: "org.openrewrite.java.marker.Semicolon"
}

export interface TrailingComma extends Marker {
    kind: "org.openrewrite.java.marker.TrailingComma"
    readonly suffix: Space;
}

export interface OmitParentheses extends Marker {
    kind: "org.openrewrite.java.marker.OmitParentheses"
}
