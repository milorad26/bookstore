# Stripe Test Payment Setup Guide

## 🎉 What's Been Implemented

Your bookstore now has **Stripe Checkout** integration for secure payment processing in test mode!

### Features Added:
- ✅ Stripe Checkout Session creation
- ✅ Secure payment processing through Stripe's hosted checkout page
- ✅ Order creation with payment integration
- ✅ Test mode - no real money transactions
- ✅ Automatic redirect after successful/failed payment

---

## 🚀 Quick Setup (3 Steps)

### Step 1: Get Your Free Stripe Test Keys

1. **Sign up for Stripe** (completely free):
   - Go to: https://stripe.com
   - Click "Sign up" (top right)
   - Create your account (no credit card required!)

2. **Get your test API keys**:
   - After login, you're automatically in **Test Mode** (see toggle in top right)
   - Go to: **Developers → API keys** (https://dashboard.stripe.com/test/apikeys)
   - You'll see two keys:
     - **Publishable key** - starts with `pk_test_...`
     - **Secret key** - starts with `sk_test_...` (click "Reveal test key")

### Step 2: Add Keys to Your Application

**Option A: Environment Variables (Recommended)**

Windows PowerShell:
```powershell
$env:STRIPE_SECRET_KEY="sk_test_your_secret_key_here"
$env:STRIPE_PUBLISHABLE_KEY="pk_test_your_publishable_key_here"
```

**Option B: Direct in application.yml**

Edit `src/main/resources/application.yml`:
```yaml
stripe:
  api-key: sk_test_51abc123def456...
  publishable-key: pk_test_51abc123def456...
  success-url: http://localhost:5173/orders
  cancel-url: http://localhost:5173/checkout
```

### Step 3: Restart Your Application

```bash
# Stop current backend (Ctrl+C in terminal)
# Start again
./start.bat   # Windows
# or
./start.sh    # Linux/Mac
```

---

## 🧪 Testing the Payment Flow

### 1. Place an Order

1. Go to your shop: http://localhost:5173
2. Add books to cart
3. Go to checkout
4. Fill in addresses
5. Click **"Proceed to Payment"**
6. You'll be redirected to Stripe's checkout page

### 2. Use Stripe Test Cards

Stripe provides test credit card numbers that simulate different scenarios:

| Card Number | Result | Description |
|-------------|--------|-------------|
| `4242 4242 4242 4242` | ✅ Success | Payment succeeds |
| `4000 0000 0000 9995` | ❌ Declined | Insufficient funds |
| `4000 0025 0000 3155` | ⚠️ Authentication | Requires 3D Secure |

**Other Test Data:**
- **Expiration Date**: Any future date (e.g., 12/34)
- **CVC**: Any 3 digits (e.g., 123)
- **ZIP Code**: Any 5 digits (e.g., 12345)

### 3. Complete Payment

1. Enter test card: `4242 4242 4242 4242`
2. Enter any future expiration (12/34)
3. Enter any CVC (123)
4. Click "Pay"
5. You'll be redirected back to orders page ✅

---

## 📊 View Payment Activity

**Stripe Dashboard** (Test Mode):
- Go to: https://dashboard.stripe.com/test/payments
- See all test payments in real-time
- View customer details, amounts, status
- Test webhooks and events

---

## 🔧 API Endpoints Added

### Backend (Java Spring Boot)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/payments/create-checkout-session/{orderId}` | POST | Create Stripe checkout session |
| `/api/payments/success` | GET | Handle successful payment |
| `/api/payments/config` | GET | Get Stripe public key |

### Example Flow:

```
1. User clicks "Proceed to Payment"
   ↓
2. Frontend creates order → POST /api/orders
   ↓
3. Frontend requests payment session → POST /api/payments/create-checkout-session/{orderId}
   ↓
4. Backend creates Stripe session and returns URL
   ↓
5. User redirected to Stripe checkout page
   ↓
6. User enters test card and pays
   ↓
7. Stripe redirects back to: http://localhost:5173/orders?session_id=xxx&order_id=yyy
```

---

## 🎯 What Happens During Payment?

### Order Creation
```java
POST /api/orders
{
  "userId": 1,
  "orderItems": [...],
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
→ Returns: Order with ID and PENDING status
```

### Payment Session Creation
```java
POST /api/payments/create-checkout-session/123
→ Returns: 
{
  "sessionId": "cs_test_abc123...",
  "sessionUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
  "publicKey": "pk_test_..."
}
```

### Stripe Checkout Page
- Hosted by Stripe (PCI compliant, secure)
- Shows order items, amounts, tax
- Collects payment details
- Processes payment

### Success Redirect
```
http://localhost:5173/orders?session_id=cs_test_xxx&order_id=123
→ Frontend can call: GET /api/payments/success?session_id=xxx&order_id=123
```

---

## 🔐 Security Features

- ✅ **PCI Compliant**: Card data never touches your server
- ✅ **JWT Authentication**: Only logged-in users can create payment sessions
- ✅ **SSL/TLS**: All communication encrypted (in production)
- ✅ **Test Mode**: No real money, no risk
- ✅ **Webhook Verification**: Stripe signs all webhook events (for production)

---

## 🚀 Going to Production

When ready to accept real payments:

1. **Complete Stripe Account Setup**:
   - Provide business information
   - Add bank account for payouts
   - Verify identity (required by law)

2. **Get Production Keys**:
   - Toggle to **Live Mode** in Stripe Dashboard
   - Get live keys: `sk_live_...` and `pk_live_...`

3. **Update Configuration**:
   - Set production keys in environment variables
   - Update success/cancel URLs to production domain
   - Enable webhooks for payment events

4. **Enable HTTPS**:
   - Required for production
   - Use Let's Encrypt (free SSL certificate)

---

## 🐛 Troubleshooting

### "Invalid API Key"
- Check you copied the full key (starts with `sk_test_` or `pk_test_`)
- Make sure you're using TEST mode keys
- Check for extra spaces or quotes

### "Payment session creation failed"
- Verify order exists in database
- Check backend logs for Stripe API errors
- Ensure order has items with valid prices

### "Redirect not working"
- Check success-url and cancel-url in application.yml
- Ensure frontend is running on http://localhost:5173
- Clear browser cache

### "CORS errors"
- Verify SecurityConfig allows localhost:5173
- Check browser console for specific CORS errors

---

## 📚 Additional Resources

- **Stripe Test Cards**: https://stripe.com/docs/testing
- **Stripe Dashboard**: https://dashboard.stripe.com/test
- **Stripe API Docs**: https://stripe.com/docs/api
- **Checkout Guide**: https://stripe.com/docs/payments/checkout

---

## ✅ Quick Test Checklist

- [ ] Signed up for Stripe account
- [ ] Got test API keys from dashboard
- [ ] Added keys to application.yml or environment variables
- [ ] Restarted backend application
- [ ] Added items to cart
- [ ] Filled checkout form
- [ ] Redirected to Stripe checkout
- [ ] Used test card: 4242 4242 4242 4242
- [ ] Payment succeeded
- [ ] Redirected back to orders page
- [ ] Viewed payment in Stripe dashboard

---

## 🎊 You're All Set!

Your bookstore now has professional payment processing with Stripe. Test as much as you want - it's completely free in test mode!

**Questions?** Check the Stripe documentation or test with different card numbers to see various payment scenarios.

Happy testing! 🚀💳
