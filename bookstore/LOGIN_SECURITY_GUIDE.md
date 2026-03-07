# Login Security & Account Lockout System

## Overview
This system protects your bookstore from brute force login attacks by temporarily locking accounts after multiple failed login attempts and tracking all login activity with IP addresses and other metadata.

## Features

### 🔒 Account Lockout Protection
- **3 Failed Attempts**: Account is locked after 3 consecutive failed login attempts
- **1-Minute Lockout**: Account is automatically locked for 60 seconds
- **Auto-Unlock**: After the lockout period expires, users can attempt to login again
- **Reset on Success**: Failed attempt counter resets to 0 upon successful login

### 📊 Login Attempt Tracking
All login attempts (successful and failed) are logged with:
- Username
- IP Address
- User Agent (browser/device information)
- Timestamp
- Success/Failure status
- Failure reason
- Optional: Country and City (for future GeoIP integration)

### 🛡️ Security Features
- **IP Tracking**: Track suspicious IPs attempting multiple different usernames
- **Comprehensive Logging**: All login events are logged for security audits
- **Data Retention**: Login attempts are stored for 30 days (cleanup can be scheduled)
- **Real-time Lockout Check**: System checks lockout status before authentication

## How It Works

### User Login Flow
1. User submits login credentials
2. System checks if account is locked
   - If locked: Reject with remaining time message
   - If not locked: Proceed to authentication
3. Spring Security authenticates credentials
4. **On Success**:
   - Record successful login attempt
   - Reset failed attempt counter to 0
   - Clear lockout timestamp
   - Update last login time
   - Return JWT token
5. **On Failure**:
   - Record failed login attempt
   - Increment failed attempt counter
   - If counter reaches 3: Set lockout until current time + 1 minute
   - Return appropriate error message

### Lockout Mechanism
```
Failed Attempt 1: Counter = 1, No lockout
Failed Attempt 2: Counter = 2, No lockout
Failed Attempt 3: Counter = 3, Account locked for 60 seconds

After 60 seconds: Lockout expires automatically
Next login attempt: Can try again
```

## Database Schema

### User Table (Extended)
```sql
ALTER TABLE users ADD COLUMN:
- failed_login_attempts INT DEFAULT 0
- account_locked_until DATETIME NULL
- last_failed_login DATETIME NULL
```

### Login Attempts Table (New)
```sql
CREATE TABLE login_attempts:
- id BIGINT (Primary Key)
- username VARCHAR(50)
- ip_address VARCHAR(45)
- user_agent VARCHAR(1000)
- attempt_time DATETIME
- successful BOOLEAN
- failure_reason VARCHAR(500)
- country VARCHAR(100)
- city VARCHAR(100)
```

## Configuration

### Customizable Constants (LoginAttemptService.java)
```java
MAX_FAILED_ATTEMPTS = 3;           // Lock after 3 failed attempts
LOCKOUT_DURATION_MINUTES = 1;      // Lock for 1 minute
ATTEMPT_WINDOW_MINUTES = 15;       // Time window to count attempts
```

To change these values, edit the constants in `LoginAttemptService.java`:
```java
private static final int MAX_FAILED_ATTEMPTS = 5;        // Change to 5 attempts
private static final int LOCKOUT_DURATION_MINUTES = 5;   // Change to 5 minutes
```

## API Error Messages

### Account Locked (Before Max Attempts)
```json
{
  "message": "Account is temporarily locked due to multiple failed login attempts. Please try again in 45 seconds."
}
```

### Account Locked (After Failed Attempt)
```json
{
  "message": "Too many failed login attempts. Your account has been locked for 60 seconds."
}
```

### Invalid Credentials
```json
{
  "message": "Invalid username or password"
}
```

## Internationalization (i18n)

Messages are available in both English and Serbian:

**English** (`messages_en.properties`):
- `auth.account.locked`: Account is temporarily locked
- `auth.account.locked.after.attempts`: Too many failed attempts
- `auth.invalid.credentials`: Invalid username or password

**Serbian** (`messages_sr.properties`):
- `auth.account.locked`: Nalog je privremeno zaključan
- `auth.account.locked.after.attempts`: Previše neuspešnih pokušaja
- `auth.invalid.credentials`: Neispravno korisničko ime ili lozinka

## Security Best Practices

### What This System Protects Against
✅ **Brute Force Attacks**: Automated password guessing is rate-limited
✅ **Credential Stuffing**: Stolen credentials from other breaches are slowed down
✅ **Dictionary Attacks**: Sequential password attempts are throttled

### What This System Does NOT Protect Against
❌ **Distributed Attacks**: Attackers using many different IP addresses
❌ **Account Enumeration**: Still possible to check if username exists
❌ **Social Engineering**: Phishing and other human-based attacks

### Recommended Additional Security Measures
1. **CAPTCHA**: Add after 1-2 failed attempts (before lockout)
2. **Rate Limiting by IP**: Limit requests from single IP across all users
3. **Email Notifications**: Alert users of failed login attempts
4. **Two-Factor Authentication (2FA)**: Add second authentication factor
5. **Strong Password Policy**: Enforce minimum complexity requirements

## Frontend Integration

### Login Error Handling
```javascript
try {
  const response = await login(username, password);
  // Success - redirect to dashboard
} catch (error) {
  if (error.response?.status === 423) { // Locked
    const message = error.response.data.message;
    // Extract seconds from message and show countdown timer
    showError(message);
  } else if (error.response?.status === 401) { // Invalid credentials
    showError("Invalid username or password");
  }
}
```

### Countdown Timer Example
```javascript
function showLockoutCountdown(seconds) {
  const interval = setInterval(() => {
    seconds--;
    if (seconds <= 0) {
      clearInterval(interval);
      showMessage("You can now try logging in again");
    } else {
      showMessage(`Account locked. Try again in ${seconds} seconds`);
    }
  }, 1000);
}
```

## Testing the System

### Test Scenario 1: Account Lockout
1. Attempt login with wrong password: ❌ Failure
2. Attempt login with wrong password: ❌ Failure
3. Attempt login with wrong password: ❌ Failure (Account now locked)
4. Attempt login with CORRECT password: ❌ Failure (Still locked)
5. Wait 60 seconds
6. Attempt login with correct password: ✅ Success

### Test Scenario 2: Reset on Success
1. Attempt login with wrong password: ❌ Failure (Counter = 1)
2. Attempt login with wrong password: ❌ Failure (Counter = 2)
3. Attempt login with correct password: ✅ Success (Counter reset to 0)
4. Failed attempts counter is now 0

### Postman Testing
```json
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "wrongpassword"
}
```

## Monitoring & Alerts

### Suspicious Activity Indicators
- Single user with many failed attempts
- Single IP attempting many different usernames
- Failed attempts from unusual geographic locations
- Login attempts outside normal business hours

### Recommended Monitoring
```sql
-- Find users with most failed attempts today
SELECT username, COUNT(*) as failed_attempts
FROM login_attempts
WHERE successful = FALSE 
  AND attempt_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)
GROUP BY username
ORDER BY failed_attempts DESC
LIMIT 10;

-- Find suspicious IPs (5+ different users)
SELECT ip_address, COUNT(DISTINCT username) as user_count
FROM login_attempts
WHERE successful = FALSE
  AND attempt_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY ip_address
HAVING user_count >= 5;

-- Current locked accounts
SELECT username, account_locked_until, 
       TIMESTAMPDIFF(SECOND, NOW(), account_locked_until) as seconds_remaining
FROM users
WHERE account_locked_until > NOW();
```

## Migration & Deployment

### Database Migration
The system uses Flyway migrations. The migration file `add_login_security.sql` will automatically run on application startup.

### Zero-Downtime Deployment
1. Deploy new code (User model and services)
2. Run database migration (adds new columns with defaults)
3. Existing users have `failed_login_attempts = 0` and no lockout
4. System immediately starts tracking new login attempts

### Rollback Plan
If you need to rollback:
```sql
-- Remove login tracking
DROP TABLE login_attempts;

-- Remove user lockout fields
ALTER TABLE users
DROP COLUMN failed_login_attempts,
DROP COLUMN account_locked_until,
DROP COLUMN last_failed_login;
```

## Performance Considerations

### Database Indexes
The system includes indexes on:
- `username` - Fast lookup for user attempts
- `ip_address` - Fast lookup for IP attempts  
- `attempt_time` - Fast time-range queries

### Cleanup Strategy
Schedule a cron job to cleanup old attempts:
```java
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void scheduledCleanup() {
    loginAttemptService.cleanupOldAttempts();
}
```

## Troubleshooting

### User Locked Out Permanently
- Check `account_locked_until` timestamp
- If in the past, lockout should auto-expire
- Database admin can manually reset: `UPDATE users SET failed_login_attempts = 0, account_locked_until = NULL WHERE username = 'username'`

### Login Attempts Not Being Recorded
- Check LoginAttemptService is autowired in AuthController
- Verify database table exists
- Check application logs for exceptions

### Lockout Not Working
- Verify MAX_FAILED_ATTEMPTS constant
- Check CustomUserDetailsService has LoginAttemptService
- Ensure database migration ran successfully

## Future Enhancements

### Potential Improvements
1. **IP-based Rate Limiting**: Block IPs with excessive failures
2. **GeoIP Integration**: Track and alert on unusual locations
3. **Progressive Delays**: Increase lockout time with repeated violations
4. **Admin Dashboard**: UI to view and manage locked accounts
5. **Email Notifications**: Alert users of failed login attempts
6. **Risk Scoring**: ML-based detection of suspicious patterns
7. **Device Fingerprinting**: Track and verify known devices

## Support

For questions or issues with the login security system:
1. Check application logs for detailed error messages
2. Query `login_attempts` table for attempt history
3. Verify database migrations completed successfully
4. Test with simple Postman requests first

---

**Last Updated**: March 7, 2026
**Version**: 1.0.0
