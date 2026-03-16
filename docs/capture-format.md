# Capture Format (v0.1)

This page defines the Testloom capture JSON contract for schema version `0.1.0`.

## 1. Envelope

Top-level object:

```json
{
  "schemaVersion": "0.1.0",
  "recordedAt": "2026-03-16T10:15:30.123Z",
  "transport": "HTTP",
  "request": { "...": "..." },
  "response": { "...": "..." }
}
```

Field rules:

| Field | Type | Nullable | Constraints |
| --- | --- | --- | --- |
| `schemaVersion` | `string` | no | Must equal `0.1.0` |
| `recordedAt` | `string` | no | ISO-8601 UTC timestamp |
| `transport` | `string` | no | For v0.1: `HTTP` |
| `request` | `object` | no | Must follow request schema below |
| `response` | `object` | no | Must follow response schema below |

## 2. Request Object

| Field | Type | Nullable | Constraints |
| --- | --- | --- | --- |
| `method` | `string` | no | HTTP method, e.g. `GET`, `POST` |
| `path` | `string` | no | Request path, e.g. `/api/hello` |
| `query` | `string` | yes | Raw query string without `?` |
| `headers` | `object<string, string[]>` | no | Header name -> list of values |
| `body` | `string` | yes | Captured only when `include-bodies=true` |
| `contentType` | `string` | yes | Request content type |

## 3. Response Object

| Field | Type | Nullable | Constraints |
| --- | --- | --- | --- |
| `status` | `integer` | no | HTTP status code |
| `headers` | `object<string, string[]>` | no | Header name -> list of values |
| `body` | `string` | yes | Captured only when `include-bodies=true` |
| `contentType` | `string` | yes | Response content type |
| `durationMs` | `integer` | no | Processing duration in milliseconds, `>= 0` |

## 4. File Naming Contract

Output directory default:

```text
./.testloom/captures
```

File name contract:

```text
<timestamp>_<route>.json
```

Examples:

- `20260316T101530123Z_api_hello.json`
- `20260316T101530123Z_api_orders_123.json`

Collision policy:

- First file uses the base name.
- Next collisions append deterministic suffixes:
  - `<timestamp>_<route>_2.json`
  - `<timestamp>_<route>_3.json`

## 5. Example Capture

```json
{
  "schemaVersion": "0.1.0",
  "recordedAt": "2026-03-16T10:15:30.123Z",
  "transport": "HTTP",
  "request": {
    "method": "GET",
    "path": "/api/hello",
    "query": "lang=en",
    "headers": {
      "accept": [
        "application/json"
      ]
    },
    "body": null,
    "contentType": null
  },
  "response": {
    "status": 200,
    "headers": {
      "content-type": [
        "application/json"
      ]
    },
    "body": "{\"message\":\"hello\"}",
    "contentType": "application/json",
    "durationMs": 12
  }
}
```
