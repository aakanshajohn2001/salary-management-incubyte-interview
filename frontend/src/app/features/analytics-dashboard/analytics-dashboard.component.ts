import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { Color, NgxChartsModule, ScaleType } from '@swimlane/ngx-charts';
import { forkJoin } from 'rxjs';
import { AnalyticsSummary, RecentSalaryChange } from '../../core/models/analytics.model';
import { AnalyticsService } from '../../core/services/analytics.service';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [DecimalPipe, MatCardModule, MatProgressBarModule, MatTableModule, NgxChartsModule],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss',
})
export class AnalyticsDashboardComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);

  readonly colorScheme: Color = {
    name: 'acme',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#2e7d32', '#ef6c00', '#66bb6a', '#fb8c00', '#1b5e20', '#a5d6a7', '#e65100'],
  };

  readonly displayedBandColumns = ['jobBand', 'headcount', 'min', 'average', 'max'];
  readonly displayedRecentChangeColumns = ['employeeName', 'department', 'amount', 'effectiveDate', 'reason'];
  readonly displayedCountryColumns = ['countryName', 'headcount', 'totalPayrollUsd', 'averageSalaryUsd', 'share'];
  readonly loading = signal(false);
  readonly summary = signal<AnalyticsSummary | null>(null);
  readonly recentChanges = signal<RecentSalaryChange[]>([]);

  readonly headcountByDepartment = computed(() =>
    (this.summary()?.byDepartment ?? []).map((d) => ({ name: d.department, value: d.headcount })),
  );

  readonly payrollByCountry = computed(() =>
    (this.summary()?.byCountry ?? []).map((c) => ({
      name: c.countryName,
      value: Math.round(c.totalPayrollUsd),
    })),
  );

  readonly averageSalaryByBand = computed(() =>
    (this.summary()?.byJobBand ?? []).map((b) => ({ name: b.jobBand, value: Math.round(b.averageSalaryUsd) })),
  );

  readonly countrySpendRows = computed(() => {
    const summary = this.summary();
    if (!summary) {
      return [];
    }
    const total = summary.totalPayrollUsd || 1;
    return summary.byCountry.map((country) => ({
      ...country,
      sharePercent: (country.totalPayrollUsd / total) * 100,
    }));
  });

  ngOnInit(): void {
    this.loading.set(true);
    forkJoin([this.analyticsService.getSummary(), this.analyticsService.getRecentChanges(10)]).subscribe({
      next: ([summary, recentChanges]) => {
        this.summary.set(summary);
        this.recentChanges.set(recentChanges);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
