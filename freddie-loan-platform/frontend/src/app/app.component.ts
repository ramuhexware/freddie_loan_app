import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoanService, LoanApplication } from './services/loan.service';
import { FilterStatusPipe } from './pipes/filter-status.pipe';

interface AuditLog {
  timestamp: Date;
  user: string;
  action: string;
  details: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, FilterStatusPipe],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'Freddie Mac Mortgage Portal';
  
  // Login & Session State
  isLoggedIn = false;
  isLoggingIn = false;
  loginEmail = 'borrower@freddiemac.com';
  loginPassword = '••••••••';
  selectedLoginRole: 'Borrower' | 'LoanOfficer' | 'Underwriter' | 'OpsManager' | 'Compliance' = 'Borrower';
  loginError = '';

  // Navigation & Role State
  activeRole: 'Borrower' | 'LoanOfficer' | 'Underwriter' | 'OpsManager' | 'Compliance' = 'Borrower';
  activeTab: 'dashboard' | 'origination' | 'underwriting' | 'documents' | 'servicing' | 'compliance' | 'borrowerStatus' = 'dashboard';
  
  // Data State
  applications: LoanApplication[] = [];
  selectedApp: LoanApplication | null = null;
  
  // Origination Form Model
  formCustomerName = '';
  formCustomerId = 'CUST-' + Math.floor(100 + Math.random() * 900);
  formLoanType = 'Purchase (Fixed 30Y)';
  formLoanAmount = 250000;
  formPropertyValue = 300000;
  formAnnualIncome = 95000;
  formMonthlyDebt = 1200;
  formCreditScore = 720;
  
  // Underwriter Controls
  overrideDecision: 'APPROVED' | 'REJECTED' = 'APPROVED';
  overrideReason = '';
  
  // Document Uploads
  uploadDocType = 'INCOME_PROOF';
  uploadedFiles: { [loanId: string]: string[] } = {};
  selectedFile: File | null = null;
  myActiveApp: LoanApplication | null = null;
  
  // Audits logs
  auditLogs: AuditLog[] = [
    { timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), user: 'SYSTEM_GATEWAY', action: 'TOKEN_VALIDATION', details: 'OAuth2 JWT token validated successfully for User: LOAN_OFFICER_01' },
    { timestamp: new Date(Date.now() - 45 * 60 * 1000), user: 'ACTIVE_MQ_LISTENER', action: 'AUDIT_EMITTED', details: 'Audit queue dispatched for balance request CUST-802' }
  ];

  constructor(private loanService: LoanService) {}

  ngOnInit() {
    this.loanService.getApplications().subscribe(apps => {
      this.applications = apps;
      if (apps.length > 0) {
        if (!this.selectedApp) {
          this.selectedApp = apps[0];
        } else {
          const updated = apps.find(a => a.loanId === this.selectedApp?.loanId);
          if (updated) {
            this.selectedApp = updated;
          }
        }
        if (this.myActiveApp) {
          const updatedActive = apps.find(a => a.loanId === this.myActiveApp?.loanId);
          if (updatedActive) {
            this.myActiveApp = updatedActive;
          }
        }
      }
    });
  }

  get borrowerActiveApp(): LoanApplication | null {
    if (this.myActiveApp) {
      return this.myActiveApp;
    }
    const sessionApp = this.applications.find(a => a.customerId === this.formCustomerId);
    if (sessionApp) {
      return sessionApp;
    }
    if (this.applications.length > 0) {
      return [...this.applications].sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())[0];
    }
    return null;
  }

  selectLoginRole(role: typeof this.selectedLoginRole) {
    this.selectedLoginRole = role;
    if (role === 'Borrower') {
      this.loginEmail = 'borrower@freddiemac.com';
      this.loginPassword = '••••••••';
    } else if (role === 'Underwriter') {
      this.loginEmail = 'underwriter_09@freddiemac.com';
      this.loginPassword = '••••••••';
    } else if (role === 'LoanOfficer') {
      this.loginEmail = 'lo_officer_01@freddiemac.com';
      this.loginPassword = '••••••••';
    } else if (role === 'OpsManager') {
      this.loginEmail = 'ops_manager@freddiemac.com';
      this.loginPassword = '••••••••';
    } else {
      this.loginEmail = 'compliance_officer@freddiemac.com';
      this.loginPassword = '••••••••';
    }
  }

  handleLogin() {
    this.isLoggingIn = true;
    this.loginError = '';
    
    // Simulate OAuth2 authorization gateway latency
    setTimeout(() => {
      this.isLoggingIn = false;
      this.isLoggedIn = true;
      this.activeRole = this.selectedLoginRole;
      this.addAuditLog('USER_LOGIN', `Logged in successfully via OAuth2 JWT token. Persona: ${this.activeRole}`);
      
      // Auto-navigate to correct view based on role
      if (this.activeRole === 'Borrower') this.activeTab = 'origination';
      else if (this.activeRole === 'Underwriter') this.activeTab = 'underwriting';
      else if (this.activeRole === 'Compliance') this.activeTab = 'compliance';
      else this.activeTab = 'dashboard';
    }, 850);
  }

  handleLogout() {
    this.addAuditLog('USER_LOGOUT', `Logged out user from current session`);
    this.isLoggedIn = false;
    this.loginPassword = '';
    this.selectLoginRole('Borrower');
  }

  selectRole(role: typeof this.activeRole) {
    this.activeRole = role;
    this.addAuditLog(role + '_SESSION', `Switched workspace role to ${role}`);
    // Auto-adjust default tab based on roles
    if (role === 'Underwriter') this.activeTab = 'underwriting';
    else if (role === 'Compliance') this.activeTab = 'compliance';
    else this.activeTab = 'dashboard';
  }

  selectTab(tab: typeof this.activeTab) {
    this.activeTab = tab;
  }

  selectApp(app: LoanApplication) {
    this.selectedApp = app;
  }

  // Dynamic getters for Origination Form
  get computedLtv(): number {
    if (!this.formPropertyValue) return 0;
    return Number(((this.formLoanAmount / this.formPropertyValue) * 100).toFixed(2));
  }

  get computedDti(): number {
    const monthlyIncome = this.formAnnualIncome / 12;
    if (!monthlyIncome) return 0;
    return Number(((this.formMonthlyDebt / monthlyIncome) * 100).toFixed(2));
  }

  // Business Actions
  submitApplication() {
    if (!this.formCustomerName) {
      alert('Please provide borrower name.');
      return;
    }

    const payload = {
      customerId: this.formCustomerId,
      customerName: this.formCustomerName,
      loanType: this.formLoanType,
      loanAmount: this.formLoanAmount,
      propertyValue: this.formPropertyValue,
      annualIncome: this.formAnnualIncome,
      monthlyDebt: this.formMonthlyDebt,
      creditScore: this.formCreditScore
    };

    this.loanService.submitApplication(payload).subscribe(newApp => {
      this.addAuditLog(this.activeRole + '_ACTION', `Submitted loan application ${newApp.loanId} for ${newApp.customerName}`);
      this.selectedApp = newApp;
      this.myActiveApp = newApp;
      if (this.activeRole === 'Borrower') {
        this.activeTab = 'borrowerStatus';
      } else {
        this.activeTab = 'dashboard';
      }
      // Reset form variables
      this.formCustomerName = '';
      this.formCustomerId = 'CUST-' + Math.floor(100 + Math.random() * 900);
    });
  }

  triggerUnderwriting(app: LoanApplication) {
    this.addAuditLog('AUTOMATED_UW', `Started automated engine evaluation for ${app.loanId}`);
    
    // Evaluate rules
    let decision: 'APPROVED' | 'REJECTED' | 'UNDER_REVIEW' = 'APPROVED';
    let reason = 'System evaluation passed. Approved under standard portfolio thresholds.';

    if (app.creditScore < 600 || app.dtiRatio > 50 || app.ltvRatio > 95) {
      decision = 'REJECTED';
      reason = 'Auto-declined: Score < 600, LTV > 95% or DTI > 50%.';
    } else if (app.creditScore < 660 || app.dtiRatio > 45 || app.ltvRatio > 90) {
      decision = 'UNDER_REVIEW';
      reason = 'Risk parameters referred to Manual Underwriter Override.';
    }

    this.loanService.updateStatus(app.loanId, decision, reason, 'AUTOMATED_ENGINE').subscribe(() => {
      this.addAuditLog('AUTOMATED_UW', `Assessment complete for ${app.loanId}: Decision = ${decision}`);
    });
  }

  submitManualOverride(app: LoanApplication) {
    if (!this.overrideReason) {
      alert('Please specify the justification reason for the underwriter override.');
      return;
    }

    const reviewer = 'Underwriter_Agent_09';
    const status = this.overrideDecision === 'APPROVED' ? 'APPROVED' : 'REJECTED';
    
    this.loanService.updateStatus(app.loanId, status, this.overrideReason, reviewer).subscribe(() => {
      this.addAuditLog('MANUAL_OVERRIDE', `Manual override applied on ${app.loanId}. Decision = ${status}. Reason: ${this.overrideReason}`);
      this.overrideReason = '';
    });
  }

  onFileSelected(event: any) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList = element.files;
    if (fileList && fileList.length > 0) {
      const file = fileList[0];
      const maxSizeBytes = 50 * 1024 * 1024; // 50MB limit
      if (file.size > maxSizeBytes) {
        alert('File size exceeds the 50MB limit. Please select a smaller document.');
        this.selectedFile = null;
        element.value = '';
      } else {
        this.selectedFile = file;
      }
    } else {
      this.selectedFile = null;
    }
  }

  uploadSelectedFile(app: LoanApplication, fileInput: HTMLInputElement) {
    if (this.selectedFile) {
      const fileName = this.selectedFile.name;
      this.loanService.addDocument(app.loanId, fileName, this.uploadDocType).subscribe(() => {
        this.addAuditLog('GRIDFS_UPLOAD', `Streamed binary ${fileName} to Reactive GridFS database. Map tag: ${this.uploadDocType}`);
        this.selectedFile = null;
        fileInput.value = '';
      });
    }
  }

  private addAuditLog(action: string, details: string) {
    this.auditLogs.unshift({
      timestamp: new Date(),
      user: this.activeRole === 'Borrower' ? 'BORROWER_PORTAL' : 'OFFICE_CLIENT',
      action,
      details
    });
  }
}
