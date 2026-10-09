
# API Design

## 1. Append Event
POST /audit/events

Request:
{
"eventType": "USER_LOGIN",
"actorId": "user-123",
"resourceType": "ACCOUNT",
"resourceId": "account-456",
"payload": {
"source": "web"
}
}

Expected: 201 Created with the stored event,
sequence number, and hash metadata.

## 2. Query Events
GET /audit/events

Optional query parameters:
- actorId
- resourceType
- resourceId
- eventType
- from
- to
- page
- size

Default order: sequence number ascending.
Default page: 0.
Default page size: 20.

## 3. Verify Chain
GET /audit/verify

Expected response:
- valid: whether verification succeeded
- firstInvalidSequence: first inconsistency, if any
- violationType: mismatch or missing record, if detected
- message: explanation of the result

## 4. Immutability
No event update or delete endpoint is exposed.
