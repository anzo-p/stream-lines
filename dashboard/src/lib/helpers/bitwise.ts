import pako from 'pako';

export function decompress(data: any) {
  try {
    const compressed = new Uint8Array(
      atob(data)
        .split('')
        .map((char) => char.charCodeAt(0))
    );
    const decompressed = pako.inflate(compressed);
    return new TextDecoder('utf-8').decode(decompressed);
  } catch (err) {
    throw new Error(`Error decompressing data: ${err}`);
  }
}
