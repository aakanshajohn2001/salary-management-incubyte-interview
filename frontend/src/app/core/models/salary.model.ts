export interface SalaryHistoryEntry {
  id: number;
  amount: number;
  currencyCode: string;
  effectiveDate: string;
  reason: string;
  createdAt: string;
}

export interface SalaryAdjustmentRequest {
  amount: number;
  effectiveDate: string;
  reason: string;
}
