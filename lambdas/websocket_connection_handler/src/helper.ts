import pako from 'pako';

export function compress(content: string): string {
  const uint8Array = new TextEncoder().encode(content);
  const compressed = pako.deflate(uint8Array);
  return btoa(String.fromCharCode.apply(null, Array.from(compressed)));
}
