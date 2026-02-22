export function clampText(value: string, maxChars: number) {
  return value.slice(0, maxChars);
}

export function countChars(value: string) {
  return value.length;
}
