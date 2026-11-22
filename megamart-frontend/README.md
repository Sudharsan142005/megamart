# MegaMart frontend

React + Vite UI for the existing Spring Boot backend. Start with `npm install`, then `npm run dev`; the default API origin is `http://localhost:8080`. Set `VITE_API_URL` in a `.env` file to use a different backend origin. Requests include browser session cookies (`withCredentials: true`). The backend disables CSRF, so this frontend does not invent a CSRF token flow. Rollup is configured to use its WebAssembly build (`@rollup/wasm-node`) because Windows Application Control blocked the native Rollup binary in this environment.

## Backend architecture and integration notes

The backend is a Spring Boot 3.3.5 REST application (`com.megamart`) organized into controllers, DTOs, services, repositories, entities, and Spring Security session configuration. API responses use `{ success, message, data }`. Authentication returns `UserResponse`; the frontend identifies roles from `roles` and restores sessions through `/api/auth/me`. Security permits public auth, catalog and category reads, then gates `/api/admin/**`, `/api/seller/**`, `/api/cart/**`, and `/api/orders/**` by role. CORS allows credentialed calls from localhost ports 5173, 3000 and 127.0.0.1:5173.

| Endpoint | Method | Request | Response `data` | Role |
|---|---|---|---|---|
| `/api/auth/register` | POST | `username,email,password,fullName,phone,address,storeName?,role` | UserResponse | Public |
| `/api/auth/login` | POST | `username,password` | UserResponse; session cookie | Public |
| `/api/auth/logout` | POST | none | none | Public |
| `/api/auth/me` | GET | none | UserResponse | Authenticated |
| `/api/auth/profile` | PUT | RegisterRequest fields | UserResponse | Authenticated |
| `/api/products?search=&categoryId=` | GET | query params | ProductResponse[] | Public |
| `/api/products/{id}`; `/api/products/category/{categoryId}` | GET | path | ProductResponse / ProductResponse[] | Public |
| `/api/categories`; `/api/categories/{id}` | GET | path optional | Category[] / Category | Public |
| `/api/cart` | GET, DELETE | none | CartResponse / none | CUSTOMER |
| `/api/cart/items` | POST | `productId,quantity` | CartResponse | CUSTOMER |
| `/api/cart/items/{itemId}` | PUT, DELETE | PUT `{quantity}` | CartResponse | CUSTOMER |
| `/api/orders` | POST | `shippingAddress,contactPhone,paymentMethod` | OrderResponse | CUSTOMER |
| `/api/orders` | GET | none | OrderResponse[] | CUSTOMER |
| `/api/orders/{id}`; `/api/orders/{id}/cancel` | GET, POST | path | OrderResponse | CUSTOMER |
| `/api/admin/stats` | GET | none | AdminStatsResponse | ADMIN |
| `/api/admin/customers`; `/api/admin/sellers`; `/api/admin/sellers/pending` | GET | none | UserResponse[] | ADMIN |
| `/api/admin/sellers/{id}/approval` | POST | `{status}` (`APPROVED`/`REJECTED`) | UserResponse | ADMIN |
| `/api/admin/users/{id}/status` | PATCH | `{enabled}` | UserResponse | ADMIN |
| `/api/admin/products`; `/api/admin/orders` | GET | none | ProductResponse[] / OrderResponse[] | ADMIN |
| `/api/admin/orders/{id}/status` | PATCH | `{status}` | OrderResponse | ADMIN |
| `/api/admin/categories` | POST | `name,description,imageUrl` | Category | ADMIN |
| `/api/admin/categories/{id}` | PUT, DELETE | category fields / none | Category / none | ADMIN |
| `/api/seller/stats`; `/api/seller/products`; `/api/seller/orders` | GET | none | SellerStatsResponse / ProductResponse[] / OrderItemResponse[] | SELLER |
| `/api/seller/products` | POST | `name,description,price,stockQuantity,imageUrl,categoryId,active` | ProductResponse | SELLER |
| `/api/seller/products/{id}` | PUT, DELETE | product fields / none | ProductResponse / none | SELLER |
| `/api/seller/products/{id}/stock` | PATCH | `{stockQuantity}` | ProductResponse | SELLER |
| `/api/seller/orders/{orderItemId}/status` | PATCH | `{status}` | OrderItemResponse | SELLER |

### Gaps in the existing backend

- Ratings and reviews, payment gateway processing, customer address book, order filtering/pagination, and search facets beyond `search` and `categoryId` are absent.
- Admin product endpoint is read-only. Product create/update/delete and stock changes exist only for the seller's own products; UI respects this.
- Products referenced by `order_items` cannot be hard-deleted because PostgreSQL protects order history with a foreign key. The seller UI therefore deactivates/reactivates listings through the existing product update API (`active: false/true`); public catalog queries already filter inactive products out. The backend's hard-delete endpoint remains available but can fail for products in past orders.
- Profile update is shared (`PUT /api/auth/profile`) and expects `RegisterRequest`; there are no separate role profile endpoints.
- Seller orders are order items, while customer/admin order APIs return orders. Seller status changes operate on an order item ID.
- The actual payment method in the backend supports `CASH_ON_DELIVERY`; no card or online checkout should be shown as functional.
- Seller registration enters approval workflow; rejected/pending behavior and disabled accounts are controlled by the backend.

The admin seeded by `DataInitializer` can log in with `admin` / `Admin@123`; sample seller and customer accounts are also defined there. Change seeded credentials before production use.
