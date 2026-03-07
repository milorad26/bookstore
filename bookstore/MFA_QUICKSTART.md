# Multi-Factor Authentication (MFA) - Quick Setup Guide

## Overview
Enable two-factor authentication using Google Authenticator for enhanced account security.

---

## Quick Start (5 Minutes)

### Prerequisites
- User account in Virtual Bookstore
- Google Authenticator app on your phone ([Android](https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2) | [iOS](https://apps.apple.com/app/google-authenticator/id388497605))
- Valid JWT token from login

---

## For Users: Enable MFA

### Step 1: Generate QR Code

```bash
curl -X GET http://localhost:8080/api/mfa/setup \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeDataUri": "data:image/png;base64,iVBORw0KGgoAAAANSU...",
  "manualEntryKey": "JBSW Y3DP EHPK 3PXP"
}
```

### Step 2: Scan QR Code

1. Open Google Authenticator app
2. Tap **"+"** or **"Add"**
3. Choose **"Scan a QR code"**
4. Point camera at QR code displayed on screen
5. App will show 6-digit code changing every 30 seconds

*Alternative: Choose "Enter a setup key" and type the `manualEntryKey`*

### Step 3: Verify and Enable

```bash
curl -X POST http://localhost:8080/api/mfa/enable \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "secret": "JBSWY3DPEHPK3PXP",
    "code": "123456"
  }'
```

Replace `123456` with the current code from your authenticator app.

**Success Response:**
```json
{
  "mfaEnabled": true,
  "message": "Multi-factor authentication has been enabled successfully"
}
```

✅ **MFA is now active!** Future logins will require a code from your authenticator app.

---

## For Users: Login with MFA

### Step 1: Login with Username/Password

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "password123"
  }'
```

**Response (MFA Required):**
```json
{
  "mfaRequired": true,
  "mfaToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Step 2: Verify MFA Code

Open Google Authenticator and get the current 6-digit code.

```bash
curl -X POST http://localhost:8080/api/mfa/verify \
  -H "Authorization: Bearer MFA_TOKEN_FROM_STEP_1" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "789012"
  }'
```

**Success Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "message": "Authentication code verified successfully"
}
```

✅ **You're logged in!** Use the final `token` for authenticated requests.

---

## For Users: Disable MFA

```bash
curl -X POST http://localhost:8080/api/mfa/disable \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "456789"
  }'
```

Replace `456789` with the current code from your authenticator app.

**Success Response:**
```json
{
  "mfaEnabled": false,
  "message": "Multi-factor authentication has been disabled"
}
```

---

## For Users: Check MFA Status

```bash
curl -X GET http://localhost:8080/api/mfa/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "mfaEnabled": true
}
```

---

## Frontend JavaScript Example

### Complete MFA Setup Flow

```javascript
// 1. Generate QR Code
async function setupMFA() {
  const token = localStorage.getItem('token');
  
  const response = await fetch('http://localhost:8080/api/mfa/setup', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  const data = await response.json();
  
  // Display QR code image
  document.getElementById('qrImage').src = data.qrCodeDataUri;
  
  // Show manual entry as backup
  document.getElementById('manualKey').innerText = 
    `Or enter manually: ${data.manualEntryKey}`;
  
  // Save secret for next step
  sessionStorage.setItem('mfaSecret', data.secret);
}

// 2. Enable MFA after scanning
async function enableMFA() {
  const token = localStorage.getItem('token');
  const secret = sessionStorage.getItem('mfaSecret');
  const code = document.getElementById('codeInput').value;
  
  const response = await fetch('http://localhost:8080/api/mfa/enable', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ secret, code })
  });
  
  if (response.ok) {
    alert('MFA enabled successfully!');
    sessionStorage.removeItem('mfaSecret');
  } else {
    const error = await response.json();
    alert(error.error || 'Invalid code. Try again.');
  }
}

// 3. Login with MFA
async function login() {
  const username = document.getElementById('username').value;
  const password = document.getElementById('password').value;
  
  // Step 1: Authenticate
  const loginResponse = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  
  const loginData = await loginResponse.json();
  
  // Check if MFA is required
  if (loginData.mfaRequired) {
    // Show MFA input field
    document.getElementById('mfaSection').style.display = 'block';
    sessionStorage.setItem('mfaToken', loginData.mfaToken);
  } else {
    // No MFA - save token and redirect
    localStorage.setItem('token', loginData.token);
    window.location.href = '/dashboard';
  }
}

// 4. Verify MFA code
async function verifyMFA() {
  const mfaToken = sessionStorage.getItem('mfaToken');
  const code = document.getElementById('mfaCode').value;
  
  const response = await fetch('http://localhost:8080/api/mfa/verify', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${mfaToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ code })
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('token', data.token);
    sessionStorage.removeItem('mfaToken');
    window.location.href = '/dashboard';
  } else {
    alert('Invalid code. Please try again.');
  }
}
```

---

## Testing Checklist

- [ ] User can generate QR code
- [ ] Google Authenticator successfully scans QR code
- [ ] User can enable MFA with valid code
- [ ] Invalid code throws error
- [ ] Login returns `mfaRequired: true` for users with MFA
- [ ] Login returns normal token for users without MFA
- [ ] MFA code verification works with valid code
- [ ] MFA code verification rejects invalid code
- [ ] User can disable MFA with valid code
- [ ] MFA status endpoint returns correct status

---

## Common Issues

### "Invalid authentication code"
- **Cause:** Time mismatch between server and phone
- **Fix:** Ensure server uses NTP time sync. Check phone's automatic time setting.

### QR code not displaying
- **Cause:** Invalid data URI or rendering issue
- **Fix:** Check browser console. Ensure `<img src="${qrCodeDataUri}" />` is used correctly.

### Lost authenticator app
- **Admin Recovery:**
  ```sql
  UPDATE users 
  SET mfa_enabled = FALSE, mfa_secret = NULL 
  WHERE username = 'affected_user';
  ```

### MFA verification always fails
- **Cause:** Using old mfaToken (expired after 5 minutes)
- **Fix:** Restart login process to get new mfaToken

---

## Security Best Practices

1. ✅ **Secure mfaToken**: Treat it like a password - use HTTPS in production
2. ✅ **Short expiry**: mfaToken should expire in 5 minutes
3. ✅ **Rate limiting**: Limit MFA verification attempts (max 5/minute)
4. ✅ **Backup codes**: Implement recovery codes for emergencies
5. ✅ **User education**: Remind users to backup their authenticator app

---

## API Endpoints Summary

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/api/mfa/setup` | GET | User JWT | Generate QR code |
| `/api/mfa/enable` | POST | User JWT | Enable MFA |
| `/api/mfa/disable` | POST | User JWT | Disable MFA |
| `/api/mfa/status` | GET | User JWT | Check MFA status |
| `/api/mfa/verify` | POST | MFA Token | Verify TOTP code |

---

## Next Steps

1. Build frontend UI for QR code display
2. Add MFA section to user profile page
3. Implement backup codes for recovery
4. Add rate limiting to MFA verify endpoint
5. Consider making MFA mandatory for admin users

---

**For detailed implementation details, see:** [MFA_IMPLEMENTATION_GUIDE.md](MFA_IMPLEMENTATION_GUIDE.md)

**Last Updated:** March 7, 2026
