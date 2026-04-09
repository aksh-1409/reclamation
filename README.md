# SaaS License Reclamation Engine (Zombie Subscription Auditor)

An automated internal IT tool built with Spring Boot to identify, verify, and revoke wasted software subscriptions, targeting zombie licenses (terminated employees) and low-usage premium licenses.

## Features

### Core Functionality
- **Automated Data Sync**: Integrates with HRIS and Vendor APIs to identify license waste
- **State-Machine Workflow**: "Inbox Zero" UX with automated state transitions
- **Zombie License Detection**: Immediate revocation for terminated employees
- **Low Usage Verification**: Email-based user verification for 90+ day inactive licenses
- **Admin Accountability**: Mandatory justifications for all actions
- **Comprehensive Audit Trail**: Complete history of all decisions and actions

### Workflow States
- `NEW` → Initial detection
- `AWAITING_RESPONSE` → Email sent, hidden from dashboard
- `READY_FOR_REVIEW` → User responded, resurfaces on dashboard
- `RESPONSE_OVERDUE` → Timeout exceeded, requires admin action
- `APPROVED_EXTENSION` → Temporary approval with expiration timer
- `EXTENSION_EXPIRED` → Extension period ended
- `RESOLVED` → Final state (license revoked)

## Technology Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: MySQL 8.0+
- **Frontend**: Thymeleaf + Bootstrap 5
- **Security**: Spring Security
- **Batch Processing**: Spring Batch
- **Scheduling**: Spring @Scheduled tasks

## Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0+

## Setup Instructions

### 1. Database Setup

Create a MySQL database:
```sql
CREATE DATABASE license_engine;
```

### 3. Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd license-reclamation-engine

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

The application will be available at: http://localhost:9090

### 4. Default Login

- **Username**: `admin`
- **Password**: `admin123`

## Usage Guide

### Phase 1: Data Synchronization
1. Login to the admin dashboard
2. Click **"SYNC DATA"** button
3. System fetches data from HRIS and Vendor APIs
4. Creates alerts for zombies and low-usage licenses

### Phase 2: Zombie License Handling
1. View zombie alerts (red badges) on dashboard
2. Click **"REVOKE"** button
3. Enter mandatory justification
4. License is immediately revoked via vendor API

### Phase 3: Low Usage Verification
1. View low-usage alerts (yellow badges) on dashboard
2. Click **"SEND EMAIL"** button
3. System sends verification email to employee
4. Alert disappears from dashboard (Inbox Zero)

### Phase 4: User Response Processing
- **User surrenders license**: Automatically revoked
- **User wants to keep**: Alert resurfaces for admin review
- **No response**: Alert becomes overdue after 3 days

### Phase 5: Admin Review & Resolution
1. Review user responses via **"REVIEW RESPONSE"** button
2. **Approve Extension**: Set temporary approval with expiration
3. **Reject & Revoke**: Revoke license with justification
4. Handle overdue responses with extend/revoke options

## API Endpoints

### Admin Dashboard
- `GET /` - Main dashboard
- `POST /sync` - Trigger data synchronization
- `POST /revoke-zombie/{id}` - Revoke zombie license
- `POST /send-email/{id}` - Send verification email
- `POST /approve-extension/{id}` - Approve extension request
- `POST /reject-and-revoke/{id}` - Reject and revoke license
- `POST /extend-deadline/{id}` - Extend response deadline

### User Verification
- `GET /verify/{token}` - Display verification form
- `POST /verify/{token}` - Process user response

### History & Audit
- `GET /history` - View resolved alerts
- `GET /alert/{id}` - Get alert details (AJAX)

## Mock Data

The application includes mock APIs that simulate:

### HRIS Employees
- Active employees: John Doe, Jane Smith, Alice Brown, Diana Miller, Frank Garcia
- Terminated employees: Bob Wilson, Charlie Davis, Grace Lee

### Vendor Licenses
- Recent usage: John (Zoom), Jane (Jira), Diana (Slack)
- Low usage (90+ days): Alice (Adobe), Frank (Salesforce)
- Zombie licenses: Bob (Office 365), Charlie (Tableau), Grace (Zoom)

## Scheduled Tasks

- **Deadline Check**: Every hour - marks overdue responses
- **Extension Check**: Every hour at 30 minutes - marks expired extensions
- **Manual Triggers**: Available via "Manual Checks" button for testing

## Database Schema

### Core Tables
- `audit_alerts` - Main alert records with state tracking
- `action_history_log` - Complete audit trail of admin actions
- `email_responses` - User responses to verification emails

### Key Relationships
- One-to-Many: AuditAlert → ActionHistoryLog
- One-to-One: AuditAlert → EmailResponse

## Security Features

- Spring Security with form-based authentication
- CSRF protection (disabled for verification endpoints)
- Session management
- Public access only for user verification links

## Development Features

- Hot reload with Spring DevTools
- H2 database for testing
- Comprehensive logging
- Bootstrap-based responsive UI
- Modal-driven interactions

## Customization

### Adding New Vendors
1. Update `MockApiService.getLicensesFromVendor()`
2. Add vendor-specific revocation logic
3. Update UI vendor badges if needed

### Modifying Business Rules
1. Update detection logic in `DataSyncService.synchronizeData()`
2. Adjust timeouts in `application.yml`
3. Modify state transitions in `AuditAlertService`

### Email Integration
Replace mock email service in `MockApiService` with actual SMTP configuration:
```yaml
spring:
  mail:
    host: smtp.company.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
```

## Monitoring & Maintenance

- Check application logs for sync errors
- Monitor database growth and cleanup resolved alerts periodically
- Review audit trails for compliance
- Update mock data or integrate with real APIs as needed

## License

This project is for internal company use only.