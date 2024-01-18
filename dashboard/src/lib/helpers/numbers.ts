export function minMax(x: number, min: number, max: number, multi: number): number {
    return ((x - min) / (max - min)) * multi;
}
