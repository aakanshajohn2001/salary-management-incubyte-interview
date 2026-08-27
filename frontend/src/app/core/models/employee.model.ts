export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  countryCode: string;
  countryName: string;
  jobBand: string;
  hireDate: string;
  status: string;
  currentSalaryAmount: number | null;
  currentSalaryCurrency: string | null;
  currentSalaryEffectiveDate: string | null;
  belowBandAverage: boolean | null;
}

export interface EmployeeFilter {
  search?: string;
  departmentId?: number;
  countryCode?: string;
  jobBand?: string;
  status?: string;
}

export interface EmployeeSort {
  property: string;
  direction: 'asc' | 'desc';
}
