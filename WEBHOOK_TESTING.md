Testing Payment Webhook

A simple webhook endpoint has been added at POST /api/payments/webhook.
It expects a JSON body with fields: bookingId (Long), amount (Double), paymentReference (String optional).

Security: provide header X-Payment-Secret with value from `application.properties` key `app.payment.webhook-secret` (default `local-secret`).

Example curl for local testing (server on localhost:8086):

```bash
curl -X POST http://localhost:8086/api/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Payment-Secret: local-secret" \
  -d '{"bookingId":75, "amount":3300000, "paymentReference":"TEST-REF-1"}'
```

What it does:
- Creates an Invoice record with the provided amount
- Attaches any unpaid confirmed ServiceOrder rows for the booking to the invoice
- Marks the booking status to include "Đã thanh toán" if it didn't already

Notes:
- This is intentionally simple. For production, validate webhook signatures, idempotency (avoid double-processing), and map provider-specific payment statuses.
- Consider adding a separate payment records table for audit and reconciliation.
