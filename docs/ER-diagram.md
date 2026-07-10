# Bowmenn — Database Schema (ER Diagram)

Four tables model the entire booking→delivery domain. `users` is a single table with a
`role` discriminator (`CUSTOMER | DRIVER | ADMIN`). A shipment references `users` twice —
once as the customer who booked it, once as the driver assigned to it. Status history and
proof of delivery hang off the shipment.

A rendered image is available at [`er-diagram.svg`](er-diagram.svg); the Mermaid source is
in [`er-diagram.mmd`](er-diagram.mmd). The diagram below also renders natively on GitHub.

![Bowmenn ER diagram](er-diagram.svg)

```mermaid
erDiagram
    USERS ||--o{ SHIPMENTS : "books (customer_id)"
    USERS ||--o{ SHIPMENTS : "drives (driver_id)"
    USERS ||--o{ SHIPMENT_STATUS_LOGS : "changed_by"
    USERS ||--o{ PROOF_OF_DELIVERY : "uploaded_by"
    SHIPMENTS ||--o{ SHIPMENT_STATUS_LOGS : "has history"
    SHIPMENTS ||--|| PROOF_OF_DELIVERY : "has one"

    USERS {
        uuid id PK
        varchar full_name
        varchar email UK
        varchar phone
        varchar password_hash
        varchar role "CUSTOMER|DRIVER|ADMIN"
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }

    SHIPMENTS {
        uuid id PK
        varchar tracking_number UK "BWN-YYYYMMDD-XXXXX"
        uuid customer_id FK
        uuid driver_id FK "nullable until assigned"
        text pickup_address
        decimal pickup_lat
        decimal pickup_lng
        text delivery_address
        decimal delivery_lat
        decimal delivery_lng
        text cargo_description
        decimal cargo_weight
        varchar truck_type "MINI|MEDIUM|LARGE"
        varchar status "8-state lifecycle"
        decimal estimated_distance_km
        decimal estimated_price
        text notes
        timestamp created_at
        timestamp updated_at
    }

    SHIPMENT_STATUS_LOGS {
        uuid id PK
        uuid shipment_id FK "ON DELETE CASCADE"
        varchar old_status "null on creation"
        varchar new_status
        uuid changed_by FK
        text note
        timestamp created_at
    }

    PROOF_OF_DELIVERY {
        uuid id PK
        uuid shipment_id FK,UK "one POD per shipment"
        varchar image_url
        varchar image_file_id "storage provider id"
        text note
        uuid uploaded_by FK
        timestamp uploaded_at
    }
```

## Relationships

| From | To | Cardinality | Notes |
|---|---|---|---|
| `users` | `shipments` | 1 customer → many shipments | `shipments.customer_id` (required) |
| `users` | `shipments` | 1 driver → many shipments | `shipments.driver_id` (nullable until assigned) |
| `shipments` | `shipment_status_logs` | 1 → many | append-only audit trail; `ON DELETE CASCADE` |
| `shipments` | `proof_of_delivery` | 1 → 1 | `shipment_id` is `UNIQUE` |
| `users` | `shipment_status_logs` | 1 → many | `changed_by` (who made the change) |
| `users` | `proof_of_delivery` | 1 → many | `uploaded_by` |

## Design notes

- **UUID primary keys** — ids appear in URLs; sequential integers would leak business volume
  and invite enumeration.
- **`CHECK` constraints** on `role`, `truck_type`, and `status` enforce the enums at the
  database level, independent of the application.
- **`proof_of_delivery.shipment_id UNIQUE`** makes "one POD per shipment" a database
  guarantee, not just application logic.
- **Indexes** cover every read path: `users(email, role)` and
  `shipments(customer_id, driver_id, status, tracking_number)`.
- **Single `users` table** rather than per-role tables — the roles differ only by a
  discriminator today, and splitting prematurely would force a painful merge the first time
  one person is both a customer and a driver.

The authoritative schema is the Flyway migration set in
[`../src/main/resources/db/migration`](../src/main/resources/db/migration) (`V1`–`V6`).
