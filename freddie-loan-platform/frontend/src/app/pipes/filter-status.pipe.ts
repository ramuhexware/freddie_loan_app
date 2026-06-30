import { Pipe, PipeTransform } from '@angular/core';
import { LoanApplication } from '../services/loan.service';

@Pipe({
  name: 'filterStatus',
  standalone: true
})
export class FilterStatusPipe implements PipeTransform {
  transform(applications: LoanApplication[], status: string): LoanApplication[] {
    if (!applications || !status) {
      return applications;
    }
    return applications.filter(app => app.status === status);
  }
}
