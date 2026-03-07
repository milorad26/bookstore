# Login Security - Quick Test Guide

## Testing the Account Lockout Feature

### Step 1: Trigger Account Lockout

**Using Postman/cURL:**
```bash
# Attempt 1 - Wrong password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "password": "wrongpassword1"}'

# Attempt 2 - Wrong password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "password": "wrongpassword2"}'

# Attempt 3 - Wrong password (Account now locked!)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "password": "wrongpassword3"}'
```

**Expected Response (3rd attempt):**
```json
{
  "message": "Too many failed login attempts. Your account has been locked for 60 seconds.",
  "status": 423
}
```

### Step 2: Verify Lockout is Active

```bash
# Try with CORRECT password - should still be locked
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "password": "password123"}'
```

**Expected Response:**
```json
{
  "message": "Account is temporarily locked due to multiple failed login attempts. Please try again in 45 seconds.",
  "status": 423
}
```

### Step 3: Wait and Try Again

```bash
# Wait 60 seconds, then try with correct password
sleep 60

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "password": "password123"}'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

## Database Verification

### Check User Lockout Status
```sql
SELECT username, failed_login_attempts, account_locked_until, last_failed_login
FROM users
WHERE username = 'johndoe';
```

### View Login Attempts
```sql
SELECT * FROM login_attempts
WHERE username = 'johndoe'
ORDER BY attempt_time DESC
LIMIT 10;
```

### Find Currently Locked Accounts
```sql
SELECT username, account_locked_until,
       TIMESTAMPDIFF(SECOND, NOW(), account_locked_until) as seconds_remaining
FROM users
WHERE account_locked_until > NOW();
```

## Frontend Integration Example

```vue
<template>
  <div>
    <form @submit.prevent="handleLogin">
      <input v-model="username" type="text" placeholder="Username" />
      <input v-model="password" type="password" placeholder="Password" />
      <button type="submit" :disabled="isLocked">Login</button>
    </form>
    
    <div v-if="lockoutMessage" class="error-message">
      {{ lockoutMessage }}
      <p v-if="countdown > 0">Try again in {{ countdown }} seconds</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { authService } from '@/services/authService'

const username = ref('')
const password = ref('')
const lockoutMessage = ref('')
const countdown = ref(0)
const isLocked = ref(false)

let countdownInterval = null

const handleLogin = async () => {
  try {
    lockoutMessage.value = ''
    const response = await authService.login(username.value, password.value)
    // Success - redirect to dashboard
    router.push('/dashboard')
  } catch (error) {
    if (error.response?.status === 423) {
      // Account locked
      lockoutMessage.value = error.response.data.message
      
      // Extract seconds from message if possible
      const secondsMatch = lockoutMessage.value.match(/(\d+) seconds/)
      if (secondsMatch) {
        countdown.value = parseInt(secondsMatch[1])
        isLocked.value = true
        startCountdown()
      }
    } else if (error.response?.status === 401) {
      lockoutMessage.value = 'Invalid username or password'
    } else {
      lockoutMessage.value = 'Login failed. Please try again.'
    }
  }
}

const startCountdown = () => {
  if (countdownInterval) clearInterval(countdownInterval)
  
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownInterval)
      isLocked.value = false
      lockoutMessage.value = 'You can now try logging in again'
      setTimeout(() => {
        lockoutMessage.value = ''
      }, 3000)
    }
  }, 1000)
}
</script>

<style scoped>
.error-message {
  color: red;
  margin-top: 10px;
  padding: 10px;
  background-color: #fee;
  border-radius: 4px;
}
</style>
```

## Test Checklist

- [ ] Failed login increases counter
- [ ] 3 failed attempts locks account
- [ ] Locked account rejects even correct password
- [ ] Lockout auto-expires after 60 seconds
- [ ] Successful login resets counter to 0
- [ ] Login attempts are recorded in database
- [ ] IP address is captured correctly
- [ ] User agent is captured correctly
- [ ] Localization works (English & Serbian messages)

## Common Issues & Solutions

### Issue: Account not locking after 3 attempts
**Solution**: Check that LoginAttemptService is properly injected in AuthController

### Issue: IP address shows as "unknown"
**Solution**: Check proxy headers if behind load balancer/reverse proxy

### Issue: Lockout doesn't expire
**Solution**: Verify system time is correct, check accountLockedUntil timestamp

---

**Pro Tip**: Test in multiple browsers/incognito windows to simulate different users and IPs.
