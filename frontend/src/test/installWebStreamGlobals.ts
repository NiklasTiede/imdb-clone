import {
  ReadableStream,
  TransformStream,
  WritableStream,
} from "node:stream/web";

if (typeof globalThis.ReadableStream === "undefined") {
  Object.assign(globalThis, {
    ReadableStream,
    TransformStream,
    WritableStream,
  });
}
