import {randomUUID} from "crypto";

export type UUID = string;

export function randomId(): UUID {
    return randomUUID();
}
