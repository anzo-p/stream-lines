export function getEpoch({
    date = new Date(),
    year = date.getFullYear(),
    month = date.getMonth(),
    day = date.getDate(),
    hours = 0,
    minutes = 0,
    seconds = 0,
    asMilliseconds = false
}: {
    date?: Date;
    year?: number;
    month?: number;
    day?: number;
    hours?: number;
    minutes?: number;
    seconds?: number;
    asMilliseconds?: boolean;
} = {}): number {
    const newDate = new Date(year, month, day, hours, minutes, seconds);
    return asMilliseconds ? newDate.getTime() : Math.floor(newDate.getTime() / 1000);
}

export function getEpochNow({
    asMilliseconds = false
}: {
    asMilliseconds?: boolean;
} = {}): number {
    const now = new Date();
    return asMilliseconds ? now.getTime() : Math.floor(now.getTime() / 1000);
}

export function normalizeEpoch(t: number): number {
    const magnituedes = [
        { magnitude: 1000000000000000000, factor: 1000000 },
        { magnitude: 1000000000000000, factor: 1000 },
        { magnitude: 1000000000000, factor: 1 },
        { magnitude: 1000000000, factor: 0.001 },
        { magnitude: 1000000, factor: 0.000001 }
    ];

    for (const { magnitude, factor } of magnituedes) {
        if (t > magnitude) {
            return t / factor;
        }
    }

    return t;
}
