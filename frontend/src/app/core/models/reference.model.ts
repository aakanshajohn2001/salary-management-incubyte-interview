export interface Department {
  id: number;
  name: string;
}

export interface Country {
  code: string;
  name: string;
}

export const JOB_BANDS = ['L1', 'L2', 'L3', 'L4', 'L5', 'L6'] as const;
export type JobBand = (typeof JOB_BANDS)[number];
