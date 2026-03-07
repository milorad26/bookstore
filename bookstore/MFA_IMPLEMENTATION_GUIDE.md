# Multi-Factor Authentication (MFA) Implementation Guide

## Overview

This guide covers the complete Multi-Factor Authentication (MFA) implementation using Google Authenticator (TOTP - Time-based One-Time Password) for the Virtual Bookstore application.

### What is MFA?

Multi-Factor Authentication adds an extra layer of security by requiring users to provide a time-based code from their authenticator app (like Google Authenticator) in addition to their password when logging in.

### Key Features

- **TOTP-based**: Uses Time-based One-Time Passwords (6-digit codes that change every 30 seconds)
- **Google Authenticator Compatible**: Works with Google Authenticator, Microsoft Authenticator, Authy, etc.
- **QR Code Setup**: Easy setup by scanning a QR code with the authenticator app
- **Optional**: Users can choose to enable or disable MFA on their accounts
- **Secure**: Secret keys are stored securely and never exposed after initial setup

---

## Architecture

### Components

1. **MfaService** (`com.bookstore.service.MfaService`)
   - Generates TOTP secrets
   - Creates QR codes for easy setup
   - Verifies TOTP codes
   - Manages MFA enable/disable operations

2. **MfaController** (`com.bookstore.controller.MfaController`)
   - `/api/mfa/setup` - Generate QR code for setup
   - `/api/mfa/enable` - Enable MFA after verification
   - `/api/mfa/disable` - Disable MFA
   - `/api/mfa/status` - Check MFA status
   - `/api/mfa/verify` - Verify TOTP code during login

3. **AuthController Updates** (`com.bookstore.controller.AuthController`)
   - Modified login flow to check for MFA
   - Returns `mfaRequired=true` if user has MFA enabled
   - Provides temporary token for MFA verification

4. **User Entity Updates** (`com.bookstore.model.User`)
   - `mfaEnabled` (Boolean) - Whether MFA is enabled
   - `mfaSecret` (String) - TOTP secret key (encrypted/write-only)

### Database Schema

```sql
ALTER TABLE users
ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN mfa_secret VARCHAR(32);

CREATE INDEX idx_users_mfa_enabled ON users(mfa_enabled);
```

---

## User Flow

### 1. Enable MFA Flow

```
User Profile → Enable MFA → Scan QR Code → Verify Code → MFA Enabled
```

**Step-by-Step:**

1. **User initiates setup**: Call `GET /api/mfa/setup`
   - Backend generates a random secret
   - Creates QR code with secret embedded
   - Returns QR code as base64 data URI and manual entry key

2. **User scans QR code**: Opens Google Authenticator app and scans QR code
   - Authenticator app starts generating 6-digit codes every 30 seconds

3. **User verifies code**: Call `POST /api/mfa/enable` with secret and current code
   - Backend verifies the code matches
   - If valid, saves secret to user account and sets `mfaEnabled = true`

4. **MFA is active**: Future logins will require TOTP code

### 2. Login Flow with MFA

```
Username + Password → MFA Required? → Enter TOTP Code → Access Granted
```

**Step-by-Step:**

1. **User provides credentials**: Call `POST /api/auth/login` with username/password
   - Backend validates credentials
   - Checks if user has MFA enabled

2a. **No MFA**: Returns final JWT token immediately

2b. **MFA Enabled**: Returns `mfaRequired=true` with temporary `mfaToken`
   - Frontend prompts user for TOTP code
   - User enters 6-digit code from authenticator app

3. **Verify TOTP code**: Call `POST /api/mfa/verify` with code and `mfaToken`
   - Backend validates TOTP code
   - If valid, returns final JWT token
   - User is logged in

### 3. Disable MFA Flow

```
User Profile → Disable MFA → Enter TOTP Code → MFA Disabled
```

**Step-by-Step:**

1. **User requests to disable**: Call `POST /api/mfa/disable` with current TOTP code
   - Requires verification to prevent unauthorized disabling
   - Backend validates code

2. **MFA disabled**: 
   - Sets `mfaEnabled = false`
   - Clears `mfaSecret`
   - Future logins no longer require TOTP code

---

## API Reference

### 1. Setup MFA (Generate QR Code)

**Endpoint:** `GET /api/mfa/setup`

**Headers:**
```
Authorization: Bearer <user-jwt-token>
```

**Response (200 OK):**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeDataUri": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "manualEntryKey": "JBSW Y3DP EHPK 3PXP"
}
```

- `qrCodeDataUri`: Base64-encoded PNG image to display as QR code
- `manualEntryKey`: Formatted secret for manual entry (if QR scan fails)
- `secret`: Raw secret (needed for enable call)

**Error Responses:**
- `400 Bad Request`: MFA already enabled
- `401 Unauthorized`: Invalid or missing JWT token

---

### 2. Enable MFA

**Endpoint:** `POST /api/mfa/enable`

**Headers:**
```
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "code": "123456"
}
```

- `secret`: The secret returned from `/api/mfa/setup`
- `code`: Current 6-digit TOTP code from authenticator app

**Response (200 OK):**
```json
{
  "mfaEnabled": true,
  "message": "Multi-factor authentication has been enabled successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid TOTP code or missing parameters
- `401 Unauthorized`: Invalid JWT token

---

### 3. Disable MFA

**Endpoint:** `POST /api/mfa/disable`

**Headers:**
```
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "code": "123456"
}
```

**Response (200 OK):**
```json
{
  "mfaEnabled": false,
  "message": "Multi-factor authentication has been disabled"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid TOTP code
- `401 Unauthorized`: Invalid JWT token

---

### 4. Check MFA Status

**Endpoint:** `GET /api/mfa/status`

**Headers:**
```
Authorization: Bearer <user-jwt-token>
```

**Response (200 OK):**
```json
{
  "mfaEnabled": true
}
```

---

### 5. Verify MFA Code (During Login)

**Endpoint:** `POST /api/mfa/verify`

**Headers:**
```
Authorization: Bearer <mfa-temporary-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "code": "123456"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "message": "Authentication code verified successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid TOTP code
- `401 Unauthorized`: Invalid temporary token

---

### 6. Login (Modified for MFA)

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "username": "johndoe",
  "password": "password123"
}
```

**Response when MFA is DISABLED (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "mfaRequired": false,
  "mfaToken": null
}
```

**Response when MFA is ENABLED (200 OK):**
```json
{
  "token": null,
  "type": "Bearer",
  "mfaRequired": true,
  "mfaToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Frontend should:
1. Check `mfaRequired` flag
2. If `true`, prompt user for TOTP code
3. Call `/api/mfa/verify` with `mfaToken` and code
4. Use the final token from verify response

---

## Frontend Integration

### Step 1: Enable MFA UI

```javascript
// 1. Get QR code
async function setupMfa() {
  const response = await fetch('/api/mfa/setup', {
    headers: {
      'Authorization': `Bearer ${userToken}`
    }
  });
  
  const data = await response.json();
  
  // Display QR code
  document.getElementById('qrCode').src = data.qrCodeDataUri;
  
  // Show manual entry key as fallback
  document.getElementById('manualKey').textContent = data.manualEntryKey;
  
  // Store secret temporarily for verification
  sessionStorage.setItem('mfaSecret', data.secret);
}

// 2. Verify and enable
async function enableMfa(code) {
  const secret = sessionStorage.getItem('mfaSecret');
  
  const response = await fetch('/api/mfa/enable', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${userToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ secret, code })
  });
  
  if (response.ok) {
    alert('MFA enabled successfully!');
    sessionStorage.removeItem('mfaSecret');
  } else {
    const error = await response.json();
    alert(error.error || 'Invalid code');
  }
}
```

### Step 2: Login with MFA

```javascript
async function login(username, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  
  const data = await response.json();
  
  if (data.mfaRequired) {
    // Show MFA code input
    const code = prompt('Enter your 6-digit authentication code:');
    
    // Verify MFA code
    const mfaResponse = await fetch('/api/mfa/verify', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${data.mfaToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ code })
    });
    
    if (mfaResponse.ok) {
      const finalData = await mfaResponse.json();
      // Store final token
      localStorage.setItem('token', finalData.token);
      console.log('Logged in with MFA!');
    } else {
      alert('Invalid MFA code');
    }
  } else {
    // No MFA - store token directly
    localStorage.setItem('token', data.token);
    console.log('Logged in!');
  }
}
```

### Step 3: Disable MFA

```javascript
async function disableMfa(code) {
  const response = await fetch('/api/mfa/disable', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${userToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ code })
  });
  
  if (response.ok) {
    alert('MFA disabled successfully');
  } else {
    alert('Invalid code or MFA not enabled');
  }
}
```

---

## Security Considerations

### 1. Secret Storage
- **NEVER** expose the `mfaSecret` in API responses after initial setup
- Mark field as `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`
- Consider encrypting secrets in database using AES-256

### 2. Rate Limiting
- Implement rate limiting on `/api/mfa/verify` to prevent brute force attacks
- Recommended: Max 5 attempts per minute per user

### 3. Token Expiry
- The `mfaToken` returned during login should have a short expiry (e.g., 5 minutes)
- Regular tokens remain at their configured expiry

### 4. Backup Codes
- Consider implementing backup codes for account recovery
- Generate 10 single-use codes when MFA is enabled
- Store hashed versions in database

### 5. Account Recovery
- Provide a recovery flow if user loses authenticator app
- Options: Email-based recovery, backup codes, admin intervention

---

## Testing

### Manual Testing with Postman

#### 1. Setup MFA
```bash
GET http://localhost:8080/api/mfa/setup
Authorization: Bearer <user-token>
```

- Copy the `secret` value
- Open Google Authenticator app
- Add account manually using the secret
- Note the 6-digit code displayed

#### 2. Enable MFA
```bash
POST http://localhost:8080/api/mfa/enable
Authorization: Bearer <user-token>
Content-Type: application/json

{
  "secret": "JBSWY3DPEHPK3PXP",
  "code": "123456"
}
```

#### 3. Test Login with MFA
```bash
# Step 1: Login with credentials
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "password123"
}

# Response will have mfaRequired: true and mfaToken

# Step 2: Verify MFA code
POST http://localhost:8080/api/mfa/verify
Authorization: Bearer <mfa-token-from-step-1>
Content-Type: application/json

{
  "code": "789012"
}

# Response will have final JWT token
```

#### 4. Disable MFA
```bash
POST http://localhost:8080/api/mfa/disable
Authorization: Bearer <user-token>
Content-Type: application/json

{
  "code": "345678"
}
```

---

## Troubleshooting

### Issue: "Invalid authentication code" during enable

**Causes:**
- Time sync issues between server and authenticator app
- Wrong secret used
- Code expired (codes are valid for 30 seconds)

**Solutions:**
- Ensure server time is synchronized (NTP)
- Check device time is accurate
- Try entering a fresh code
- Verify the secret matches between setup and enable calls

---

### Issue: QR code not displaying

**Causes:**
- Invalid base64 data
- Missing image rendering in frontend

**Solutions:**
- Check console for errors
- Verify `qrCodeDataUri` starts with `data:image/png;base64,`
- Use `<img src="${qrCodeDataUri}" />` in HTML

---

### Issue: Login returns 401 after MFA verification

**Causes:**
- mfaToken expired
- Invalid TOTP code

**Solutions:**
- Ensure mfaToken is used within 5 minutes of login
- Verify code is current (refresh if near 30-second boundary)
- Check server logs for detailed error

---

### Issue: Can't disable MFA

**Causes:**
- Invalid current TOTP code
- MFA not actually enabled

**Solutions:**
- Check `/api/mfa/status` first
- Ensure TOTP code is current
- Verify authenticator app is still configured

---

## Dependencies

### Maven Dependencies Added

```xml
<!-- Google Authenticator TOTP -->
<dependency>
    <groupId>dev.samstevens.totp</groupId>
    <artifactId>totp</artifactId>
    <version>1.7.1</version>
</dependency>

<!-- QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

---

## Files Modified/Created

### Created Files
1. `MfaService.java` - Core MFA business logic
2. `MfaController.java` - MFA REST endpoints
3. `MfaSetupResponse.java` - DTO for QR code response
4. `MfaVerifyRequest.java` - DTO for code verification
5. `MfaStatusResponse.java` - DTO for MFA status
6. `add_mfa_support.sql` - Database migration

### Modified Files
1. `User.java` - Added `mfaEnabled` and `mfaSecret` fields
2. `LoginResponse.java` - Added `mfaRequired` and `mfaToken` fields
3. `AuthController.java` - Updated login flow for MFA
4. `messages_en.properties` - Added MFA messages
5. `messages_sr.properties` - Added Serbian MFA messages
6. `pom.xml` - Added TOTP and QR code dependencies

---

## Future Enhancements

1. **Backup Codes**: Generate single-use recovery codes
2. **SMS/Email MFA**: Alternative to authenticator app
3. **WebAuthn**: Support hardware security keys (FIDO2)
4. **Trusted Devices**: Remember devices for 30 days
5. **MFA Enforcement**: Admin can require MFA for all users
6. **Audit Log**: Track MFA enable/disable events

---

## Support

For questions about Multi-Factor Authentication:
- Check authentication logs: `tail -f logs/application.log | grep MFA`
- Review MFA status: `GET /api/mfa/status`
- Test TOTP codes manually: Use online TOTP generators with your secret

**Common Support Scenarios:**

1. **User lost authenticator app**: Admin uses SQL to disable: `UPDATE users SET mfa_enabled = FALSE, mfa_secret = NULL WHERE username = 'user'`
2. **Time sync issues**: Verify server time with `date` command
3. **QR code issues**: Provide manual entry key as fallback

---

**Last Updated:** March 7, 2026
