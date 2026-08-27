export interface DepartmentBreakdown {
  department: string;
  headcount: number;
  totalPayrollUsd: number;
  averageSalaryUsd: number;
}

export interface CountryBreakdown {
  countryCode: string;
  countryName: string;
  headcount: number;
  totalPayrollUsd: number;
  averageSalaryUsd: number;
}

export interface BandBreakdown {
  jobBand: string;
  headcount: number;
  minSalaryUsd: number;
  maxSalaryUsd: number;
  averageSalaryUsd: number;
}

export interface AnalyticsSummary {
  totalHeadcount: number;
  totalPayrollUsd: number;
  averageSalaryUsd: number;
  byDepartment: DepartmentBreakdown[];
  byCountry: CountryBreakdown[];
  byJobBand: BandBreakdown[];
}

export interface RecentSalaryChange {
  employeeId: number;
  employeeName: string;
  department: string;
  amount: number;
  currencyCode: string;
  effectiveDate: string;
  reason: string;
  createdAt: string;
}
