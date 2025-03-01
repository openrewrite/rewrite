import {v4 as uuidv4} from "uuid";

export type UUID = Uint8Array;

export function randomId(): UUID {
    const buffer = new Uint8Array(16);
    uuidv4({}, buffer);
    return buffer;
}
