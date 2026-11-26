import { useEffect, useState } from "react";
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";
import api, { dataOf, messageOf } from "./services/api";
import { AuthProvider, useAuth } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import ProductCard from "./components/ProductCard";
import Sidebar from "./components/Sidebar";
const money = (v) => `$${Number(v || 0).toFixed(2)}`;
function Layout() {
  return (
    <>
      <Navbar />
      <main className="main">
        <Outlet />
      </main>
      <Footer />
    </>
  );
}
function Notice({ text, error }) {
  return text ? (
    <div className={error ? "notice error" : "notice"}>{text}</div>
  ) : null;
}
function FormPage({
  title,
  children,
  onSubmit,
  submit = "Continue",
  busy = false,
  note,
}) {
  return (
    <section className="auth-wrap">
      <form className="panel auth-panel" onSubmit={onSubmit}>
        <div className="eyebrow">MEGAMART ACCOUNT</div>
        <h1>{title}</h1>
        {note && <p className="muted">{note}</p>}
        {children}
        <button disabled={busy} className="button button-dark full">
          {busy ? "Please wait…" : submit}
        </button>
      </form>
    </section>
  );
}
function Login() {
  const { login } = useAuth();
  const [err, setErr] = useState(""),
    [busy, setBusy] = useState(false);
  const nav = useNavigate();
  const [form, setForm] = useState({ username: "", password: "" });
  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  async function submit(e) {
    e.preventDefault();
    setErr("");
    setBusy(true);
    try {
      const u = await login(form.username, form.password);
      const r = u.roles?.[0]?.replace("ROLE_", "");
      nav(r === "ADMIN" ? "/admin" : r === "SELLER" ? "/seller" : "/");
    } catch (x) {
      setErr(messageOf(x));
    } finally {
      setBusy(false);
    }
  }
  return (
    <FormPage
      title="Welcome back"
      onSubmit={submit}
      submit="Sign in"
      busy={busy}
    >
      <Notice text={err} error />
      <label>
        Username
        <input
          required
          name="username"
          autoComplete="username"
          value={form.username}
          onChange={change}
        />
      </label>
      <label>
        Password
        <input
          required
          type="password"
          name="password"
          autoComplete="current-password"
          value={form.password}
          onChange={change}
        />
      </label>
      <p className="small muted">Use your MegaMart account credentials.</p>
      <p className="small">
        New here? <Link to="/register">Create an account</Link>
      </p>
    </FormPage>
  );
}
function Register() {
  const nav = useNavigate();
  const [role, setRole] = useState("CUSTOMER"),
    [err, setErr] = useState(""),
    [busy, setBusy] = useState(false);
  const [f, setF] = useState({
    username: "",
    email: "",
    password: "",
    fullName: "",
    phone: "",
    address: "",
    storeName: "",
  });
  const ch = (e) => setF({ ...f, [e.target.name]: e.target.value });
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      const r = await api.post("/api/auth/register", { ...f, role });
      nav("/login", { state: { message: r.data.message } });
    } catch (x) {
      setErr(messageOf(x));
    } finally {
      setBusy(false);
    }
  }
  return (
    <FormPage
      title="Create your account"
      onSubmit={submit}
      busy={busy}
      submit={role === "SELLER" ? "Apply as seller" : "Create account"}
      note="One account for everything you love."
    >
      <Notice text={err} error />
      <label>
        Account type
        <select value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="CUSTOMER">Customer</option>
          <option value="SELLER">Seller</option>
        </select>
      </label>
      {[
        ["fullName", "Full name"],
        ["username", "Username"],
        ["email", "Email"],
        ["phone", "Phone"],
        ["address", "Address"],
        ...(role === "SELLER" ? [["storeName", "Store name"]] : []),
        ["password", "Password"],
      ].map(([name, label]) => (
        <label key={name}>
          {label}
          <input
            required
            name={name}
            type={
              name === "password"
                ? "password"
                : name === "email"
                  ? "email"
                  : "text"
            }
            value={f[name]}
            onChange={ch}
          />
        </label>
      ))}
      {role === "SELLER" && (
        <p className="small muted">
          Seller accounts are available after administrator approval.
        </p>
      )}
    </FormPage>
  );
}
function Home() {
  const [products, setProducts] = useState([]),
    [cats, setCats] = useState([]),
    [search, setSearch] = useState(""),
    [category, setCategory] = useState(""),
    [err, setErr] = useState("");
  const { user } = useAuth();
  async function load(overrides = {}) {
    try {
      const params = {};
      if (search.trim()) params.search = search.trim();
      const cat = overrides.categoryId ?? category;
      if (cat) params.categoryId = cat;
      setProducts(dataOf(await api.get("/api/products", { params })) || []);
      setErr("");
    } catch (e) {
      setErr(messageOf(e));
    }
  }
  useEffect(() => {
    load();
    api
      .get("/api/categories")
      .then((r) => setCats(dataOf(r) || []))
      .catch(() => {});
  }, []);
  async function add(p) {
    try {
      await api.post("/api/cart/items", { productId: p.id, quantity: 1 });
      alert("Added to cart");
    } catch (e) {
      alert(
        user
          ? messageOf(e)
          : "Please sign in as a customer to add items to your cart.",
      );
    }
  }
  return (
    <>
      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow light">THE EVERYDAY MARKETPLACE</span>
          <h1>
            Find your next
            <br />
            <em>favorite thing.</em>
          </h1>
          <p>Thoughtful picks from independent sellers. All in one place.</p>
          <a className="button button-light" href="#discover">
            Explore the collection <span>↘</span>
          </a>
        </div>
        <div className="hero-art">
          <div className="hero-orbit orbit-one" />
          <div className="hero-orbit orbit-two" />
          <div className="hero-product">✳</div>
          <div className="hero-sticker">
            GOOD
            <br />
            THINGS
            <br />
            INSIDE
          </div>
          <div className="hero-caption">CURATED FOR YOUR EVERYDAY</div>
        </div>
        <div className="hero-index">01 / 03</div>
      </section>
      <section className="trust-row">
        <span>◉ Thoughtful products</span>
        <span>◇ Independent sellers</span>
        <span>↗ Secure checkout</span>
        <span>✦ Made for everyday</span>
      </section>
      <section id="discover" className="catalog">
        <div className="section-heading">
          <div>
            <div className="eyebrow">THE GOOD STUFF</div>
            <h2>
              Shop the collection<span className="dot">.</span>
            </h2>
          </div>
          <span className="muted">{products.length} products</span>
        </div>
        <div className="filters">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              load();
            }}
            className="search"
          >
            <span>⌕</span>
            <input
              placeholder="Search products..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button>Search</button>
          </form>
          <select
            value={category}
            onChange={(e) => {
              setCategory(e.target.value);
              load({ categoryId: e.target.value });
            }}
          >
            <option value="">All categories</option>
            {cats.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <Notice text={err} error />
        <div className="product-grid">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} onAdd={add} />
          ))}
        </div>
        {!products.length && !err && (
          <div className="empty">Nothing here yet. Try a different search.</div>
        )}
      </section>
    </>
  );
}
function Product() {
  const { id } = useParams(),
    { user } = useAuth(),
    [p, setP] = useState(null),
    [err, setErr] = useState("");
  useEffect(() => {
    api
      .get(`/api/products/${id}`)
      .then((r) => setP(dataOf(r)))
      .catch((e) => setErr(messageOf(e)));
  }, [id]);
  if (err) return <Notice text={err} error />;
  if (!p) return <div className="loading">Loading product…</div>;
  return (
    <div className="detail">
      <img src={p.imageUrl} alt={p.name} />
      <div>
        <div className="eyebrow">{p.categoryName}</div>
        <h1>{p.name}</h1>
        <h2>{money(p.price)}</h2>
        <p>{p.description}</p>
        <p className="stock">
          {p.stockQuantity > 0 ? `${p.stockQuantity} in stock` : "Out of stock"}
        </p>
        <button
          className="button button-dark"
          disabled={!p.stockQuantity}
          onClick={async () => {
            try {
              await api.post("/api/cart/items", {
                productId: p.id,
                quantity: 1,
              });
              alert("Added to cart");
            } catch (e) {
              alert(user ? messageOf(e) : "Sign in as a customer first.");
            }
          }}
        >
          Add to cart
        </button>
      </div>
    </div>
  );
}
function Cart() {
  const [cart, setCart] = useState(null),
    [err, setErr] = useState("");
  const load = () =>
    api
      .get("/api/cart")
      .then((r) => setCart(dataOf(r)))
      .catch((e) => setErr(messageOf(e)));
  useEffect(() => {
    load();
  }, []);
  async function act(url, method, body) {
    try {
      const r = await api[method](url, body);
      setCart(dataOf(r) || cart);
      if (!r.data?.data) load();
    } catch (e) {
      setErr(messageOf(e));
    }
  }
  return (
    <section className="content">
      <div className="eyebrow">YOUR BAG</div>
      <h1>Shopping cart</h1>
      <Notice text={err} error />
      {cart?.items?.length ? (
        <div className="cart-layout">
          <div className="panel">
            {cart.items.map((i) => (
              <div className="cart-row" key={i.id}>
                <img src={i.productImageUrl} alt="" />
                <div className="grow">
                  <strong>{i.productName}</strong>
                  <p className="muted small">{money(i.unitPrice)} each</p>
                </div>
                <input
                  className="qty"
                  aria-label="Quantity"
                  type="number"
                  min="1"
                  max={i.availableStock}
                  value={i.quantity}
                  onChange={(e) =>
                    act(`/api/cart/items/${i.id}`, "put", {
                      quantity: Number(e.target.value),
                    })
                  }
                />
                <strong>{money(i.subtotal)}</strong>
                <button
                  className="text-button"
                  onClick={() => act(`/api/cart/items/${i.id}`, "delete")}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
          <div className="panel summary">
            <h3>Summary</h3>
            <div className="sum-line">
              <span>Subtotal</span>
              <b>{money(cart.totalPrice)}</b>
            </div>
            <p className="muted small">
              Shipping and payment are handled when you place the order.
            </p>
            <Link className="button button-dark full" to="/checkout">
              Continue to checkout
            </Link>
            <button
              className="text-button"
              onClick={() => act("/api/cart", "delete")}
            >
              Clear cart
            </button>
          </div>
        </div>
      ) : (
        <div className="empty panel">
          Your cart is waiting for something good.{" "}
          <Link to="/">Start shopping →</Link>
        </div>
      )}
    </section>
  );
}
function Checkout() {
  const nav = useNavigate(),
    [err, setErr] = useState(""),
    [f, setF] = useState({
      shippingAddress: "",
      contactPhone: "",
      paymentMethod: "CASH_ON_DELIVERY",
    });
  async function submit(e) {
    e.preventDefault();
    try {
      const r = await api.post("/api/orders", f);
      nav("/orders", { state: { message: r.data.message } });
    } catch (x) {
      setErr(messageOf(x));
    }
  }
  return (
    <section className="content narrow">
      <div className="eyebrow">ALMOST THERE</div>
      <h1>Delivery details</h1>
      <form className="panel form-grid" onSubmit={submit}>
        <Notice text={err} error />
        <label className="wide">
          Shipping address
          <textarea
            required
            value={f.shippingAddress}
            onChange={(e) => setF({ ...f, shippingAddress: e.target.value })}
          />
        </label>
        <label>
          Contact phone
          <input
            required
            value={f.contactPhone}
            onChange={(e) => setF({ ...f, contactPhone: e.target.value })}
          />
        </label>
        <label>
          Payment method
          <select
            value={f.paymentMethod}
            onChange={(e) => setF({ ...f, paymentMethod: e.target.value })}
          >
            <option value="CASH_ON_DELIVERY">Cash on delivery</option>
          </select>
        </label>
        <p className="small muted wide">
          The backend currently accepts cash on delivery only.
        </p>
        <button className="button button-dark">Place order</button>
      </form>
    </section>
  );
}
function Orders() {
  const [list, setList] = useState([]),
    [err, setErr] = useState("");
  useEffect(() => {
    api
      .get("/api/orders")
      .then((r) => setList(dataOf(r) || []))
      .catch((e) => setErr(messageOf(e)));
  }, []);
  return (
    <section className="content">
      <div className="eyebrow">YOUR PURCHASES</div>
      <h1>My orders</h1>
      <Notice text={err} error />
      <div className="stack">
        {list.map((o) => (
          <article className="panel order" key={o.id}>
            <div className="order-head">
              <div>
                <strong>Order {o.orderNumber}</strong>
                <p className="small muted">
                  {new Date(o.createdAt).toLocaleDateString()}
                </p>
              </div>
              <span className="pill">{o.orderStatus}</span>
            </div>
            {o.orderItems?.map((i) => (
              <div className="order-item" key={i.id}>
                <span>
                  {i.productName} × {i.quantity}
                </span>
                <span>
                  {money(i.subtotal)} · {i.status}
                </span>
              </div>
            ))}
            <div className="sum-line">
              <span>Total</span>
              <b>{money(o.totalAmount)}</b>
            </div>
            {o.orderStatus === "PENDING" && (
              <button
                className="text-button"
                onClick={async () => {
                  try {
                    await api.post(`/api/orders/${o.id}/cancel`);
                    setList((await api.get("/api/orders")).data.data);
                  } catch (e) {
                    setErr(messageOf(e));
                  }
                }}
              >
                Cancel order
              </button>
            )}
          </article>
        ))}
      </div>
      {!list.length && !err && (
        <div className="empty">
          No orders yet. <Link to="/">Shop now →</Link>
        </div>
      )}
    </section>
  );
}
function Profile() {
  const { user, refresh } = useAuth(),
    [f, setF] = useState({}),
    [msg, setMsg] = useState(""),
    [err, setErr] = useState("");
  useEffect(() => {
    if (user)
      setF({
        username: user.username,
        email: user.email,
        fullName: user.fullName || "",
        phone: user.phone || "",
        address: user.address || "",
        storeName: user.storeName || "",
      });
  }, [user]);
  async function submit(e) {
    e.preventDefault();
    try {
      const payload = {
        fullName: f.fullName,
        phone: f.phone,
        address: f.address,
        storeName: f.storeName,
      };
      const r = await api.put("/api/auth/profile", payload);
      setMsg(r.data.message);
      setErr("");
      await refresh();
    } catch (x) {
      setErr(messageOf(x));
    }
  }
  return (
    <section className="content narrow">
      <div className="eyebrow">ACCOUNT SETTINGS</div>
      <h1>Your profile</h1>
      <form onSubmit={submit} className="panel form-grid">
        <Notice text={msg} />
        <Notice text={err} error />
        <label>
          Full name
          <input
            required
            value={f.fullName || ""}
            onChange={(e) => setF({ ...f, fullName: e.target.value })}
          />
        </label>
        <label>
          Username
          <input readOnly value={f.username || ""} />
        </label>
        <label>
          Email
          <input readOnly value={f.email || ""} />
        </label>
        <label>
          Phone
          <input
            value={f.phone || ""}
            onChange={(e) => setF({ ...f, phone: e.target.value })}
          />
        </label>
        <label className="wide">
          Address
          <textarea
            value={f.address || ""}
            onChange={(e) => setF({ ...f, address: e.target.value })}
          />
        </label>
        {f.storeName !== undefined && (
          <label className="wide">
            Store name
            <input
              value={f.storeName || ""}
              onChange={(e) => setF({ ...f, storeName: e.target.value })}
            />
          </label>
        )}
        <p className="small muted wide">
          The backend only allows profile changes to name, phone, address and
          store name.
        </p>
        <button className="button button-dark">Save profile</button>
      </form>
    </section>
  );
}
const adminLinks = [
  ["Overview", "/admin"],
  ["Customers", "/admin/customers"],
  ["Sellers", "/admin/sellers"],
  ["Approvals", "/admin/approvals"],
  ["Categories", "/admin/categories"],
  ["Products", "/admin/products"],
  ["Orders", "/admin/orders"],
];
const sellerLinks = [
  ["Overview", "/seller"],
  ["My products", "/seller/products"],
  ["Add product", "/seller/products/new"],
  ["Orders", "/seller/orders"],
];
function Workspace({ seller = false }) {
  const { pathname } = useLocation();
  const role = seller ? "seller" : "admin";
  const title =
    pathname === `/${role}`
      ? "Overview"
      : pathname.split("/").at(-1).replaceAll("-", " ");
  return (
    <div className="workspace">
      <Sidebar items={seller ? sellerLinks : adminLinks} />
      <div className="workspace-main">
        <div className="workspace-title">
          <div>
            <div className="eyebrow">
              {seller ? "SELLER STUDIO" : "ADMIN CONSOLE"}
            </div>
            <h1>{title.charAt(0).toUpperCase() + title.slice(1)}</h1>
          </div>
        </div>
        <WorkspaceContent seller={seller} pathname={pathname} />
      </div>
    </div>
  );
}
function WorkspaceContent({ seller, pathname }) {
  const [items, setItems] = useState([]),
    [stats, setStats] = useState(null),
    [cats, setCats] = useState([]),
    [msg, setMsg] = useState(""),
    [err, setErr] = useState(""),
    [loading, setLoading] = useState(true);
  const isNew = pathname.endsWith("/new"),
    editId = /\/products\/\d+/.test(pathname);
  const load = async () => {
    setLoading(true);
    setErr("");
    try {
      if (pathname === `/${seller ? "seller" : "admin"}`) {
        setStats(
          dataOf(await api.get(`/api/${seller ? "seller" : "admin"}/stats`)),
        );
        return;
      }
      if (pathname.endsWith("/products/new") || editId) {
        setCats(dataOf(await api.get("/api/categories")) || []);
        return;
      }
      const endpoint = seller
        ? pathname.includes("orders")
          ? "/api/seller/orders"
          : "/api/seller/products"
        : pathname.includes("customers")
          ? "/api/admin/customers"
          : pathname.includes("approvals")
            ? "/api/admin/sellers/pending"
            : pathname.includes("sellers")
              ? "/api/admin/sellers"
              : pathname.includes("categories")
                ? "/api/categories"
                : pathname.includes("products")
                  ? "/api/admin/products"
                  : pathname.includes("orders")
                    ? "/api/admin/orders"
                    : null;
      if (endpoint) setItems(dataOf(await api.get(endpoint)) || []);
    } catch (e) {
      setErr(messageOf(e));
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    setMsg("");
    load();
  }, [pathname]);
  const action = async (fn) => {
    try {
      await fn();
      setMsg("Updated successfully");
      setErr("");
      await load();
    } catch (e) {
      setErr(messageOf(e));
    }
  };
  if (loading)
    return (
      <>
        <Notice text={err} error />
        <div className="panel loading">
          Loading {pathname.split("/").at(-1)}…
        </div>
      </>
    );
  if (pathname === `/${seller ? "seller" : "admin"}`)
    return (
      <>
        <Notice text={err} error />
        <div className="stats-grid">
          {Object.entries(stats || {}).map(([k, v]) => (
            <div className="panel stat" key={k}>
              <span>{k.replace(/([A-Z])/g, " $1")}</span>
              <b>{k.toLowerCase().includes("revenue") ? money(v) : v}</b>
            </div>
          ))}
        </div>
        <div className="panel callout">
          <div>
            <h3>
              {seller
                ? "Your storefront, at a glance"
                : "A clear view of your marketplace"}
            </h3>
            <p className="muted">
              {seller
                ? "Manage listings, stock and fulfillment from one place."
                : "Review sellers and keep the marketplace running smoothly."}
            </p>
          </div>
          <span className="callout-mark">↗</span>
        </div>
      </>
    );
  if ((isNew || editId) && seller)
    return (
      <ProductForm
        id={editId ? pathname.split("/").at(-1) : null}
        categories={cats}
        onDone={() => {
          setMsg("Product saved");
          window.location.href = "/seller/products";
        }}
      />
    );
  if (pathname.endsWith("/categories"))
    return (
      <CategoryManager
        items={items}
        action={action}
        msg={msg}
        err={err}
        refresh={load}
      />
    );
  if (pathname.endsWith("/products/new") || editId)
    return (
      <div className="panel">
        <p>Admin product editing is not available from the backend API.</p>
      </div>
    );
  return (
    <>
      <Notice text={msg} />
      <Notice text={err} error />
      <DataTable
        items={items}
        kind={pathname.split("/").at(-1)}
        seller={seller}
        action={action}
      />
    </>
  );
}
function ProductForm({ id, categories, onDone }) {
  const [f, setF] = useState({
      name: "",
      description: "",
      price: "",
      stockQuantity: 0,
      imageUrl: "",
      categoryId: "",
      active: true,
    }),
    [err, setErr] = useState("");
  useEffect(() => {
    if (id)
      api.get("/api/seller/products").then((r) => {
        const p = dataOf(r).find((x) => String(x.id) === id);
        if (p) setF({ ...p, categoryId: p.categoryId || "" });
      });
  }, [id]);
  async function submit(e) {
    e.preventDefault();
    try {
      const body = {
        ...f,
        price: Number(f.price),
        stockQuantity: Number(f.stockQuantity),
        categoryId: Number(f.categoryId),
      };
      await (id
        ? api.put(`/api/seller/products/${id}`, body)
        : api.post("/api/seller/products", body));
      onDone();
    } catch (e) {
      setErr(messageOf(e));
    }
  }
  return (
    <form onSubmit={submit} className="panel form-grid">
      <Notice text={err} error />
      <label>
        Product name
        <input
          required
          value={f.name}
          onChange={(e) => setF({ ...f, name: e.target.value })}
        />
      </label>
      <label>
        Category
        <select
          required
          value={f.categoryId}
          onChange={(e) => setF({ ...f, categoryId: e.target.value })}
        >
          <option value="">Choose category</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </label>
      <label className="wide">
        Description
        <textarea
          required
          value={f.description}
          onChange={(e) => setF({ ...f, description: e.target.value })}
        />
      </label>
      <label>
        Price
        <input
          type="number"
          min="0.01"
          step="0.01"
          required
          value={f.price}
          onChange={(e) => setF({ ...f, price: e.target.value })}
        />
      </label>
      <label>
        Stock quantity
        <input
          type="number"
          min="0"
          required
          value={f.stockQuantity}
          onChange={(e) => setF({ ...f, stockQuantity: e.target.value })}
        />
      </label>
      <label className="wide">
        Image URL
        <input
          value={f.imageUrl || ""}
          onChange={(e) => setF({ ...f, imageUrl: e.target.value })}
        />
      </label>
      <button className="button button-dark">
        {id ? "Save changes" : "Add product"}
      </button>
    </form>
  );
}
function CategoryManager({ items, action, msg, err, refresh }) {
  const [f, setF] = useState({ name: "", description: "", imageUrl: "" });
  return (
    <>
      <form
        className="panel inline-form"
        onSubmit={(e) => {
          e.preventDefault();
          action(async () => {
            await api.post("/api/admin/categories", f);
            setF({ name: "", description: "", imageUrl: "" });
          });
        }}
      >
        <input
          placeholder="Category name"
          required
          value={f.name}
          onChange={(e) => setF({ ...f, name: e.target.value })}
        />
        <input
          placeholder="Description"
          value={f.description}
          onChange={(e) => setF({ ...f, description: e.target.value })}
        />
        <input
          placeholder="Image URL"
          value={f.imageUrl}
          onChange={(e) => setF({ ...f, imageUrl: e.target.value })}
        />
        <button className="button button-dark">Add category</button>
      </form>
      <Notice text={msg} />
      <Notice text={err} error />
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Category</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td>{c.description}</td>
                <td>
                  <button
                    className="text-button"
                    onClick={() =>
                      action(async () => {
                        const name = prompt("Category name", c.name);
                        if (name)
                          await api.put(`/api/admin/categories/${c.id}`, {
                            name,
                            description: c.description,
                            imageUrl: c.imageUrl,
                          });
                      })
                    }
                  >
                    Edit
                  </button>
                  <button
                    className="text-button danger"
                    onClick={() =>
                      action(() => api.delete(`/api/admin/categories/${c.id}`))
                    }
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
function DataTable({ items, kind, seller, action }) {
  const type = kind === "approvals" ? "sellers" : kind;
  let headers =
    type === "customers" || type === "sellers"
      ? ["Name", "Email", "Status", "Actions"]
      : type === "orders"
        ? ["Order", "Customer / Product", "Status", "Amount", "Actions"]
        : ["Name", "Category", "Price", "Stock", "Actions"];
  return (
    <div className="panel table-wrap">
      <table>
        <thead>
          <tr>
            {headers.map((h) => (
              <th key={h}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {items.map((x, i) => (
            <tr key={x.id || i}>
              {type === "customers" || type === "sellers" ? (
                <>
                  <td>
                    <strong>{x.fullName || x.username}</strong>
                    <small>@{x.username}</small>
                  </td>
                  <td>{x.email}</td>
                  <td>
                    <span className="pill">
                      {x.approvalStatus || (x.enabled ? "Enabled" : "Disabled")}
                    </span>
                  </td>
                  <td>
                    {type === "sellers" && x.approvalStatus === "PENDING" && (
                      <>
                        <button
                          className="text-button"
                          onClick={() =>
                            action(() =>
                              api.post(`/api/admin/sellers/${x.id}/approval`, {
                                status: "APPROVED",
                              }),
                            )
                          }
                        >
                          Approve
                        </button>
                        <button
                          className="text-button danger"
                          onClick={() =>
                            action(() =>
                              api.post(`/api/admin/sellers/${x.id}/approval`, {
                                status: "REJECTED",
                              }),
                            )
                          }
                        >
                          Reject
                        </button>
                      </>
                    )}
                    {kind !== "approvals" && (
                      <button
                        className="text-button"
                        onClick={() =>
                          action(() =>
                            api.patch(`/api/admin/users/${x.id}/status`, {
                              enabled: !x.enabled,
                            }),
                          )
                        }
                      >
                        {x.enabled ? "Disable" : "Enable"}
                      </button>
                    )}
                  </td>
                </>
              ) : type === "orders" ? (
                <>
                  <td>{x.orderNumber || `Order item ${x.id}`}</td>
                  <td>{x.customerName || x.productName}</td>
                  <td>
                    <span className="pill">{x.orderStatus || x.status}</span>
                  </td>
                  <td>{money(x.totalAmount || x.subtotal)}</td>
                  <td>
                    {!seller && (
                      <select
                        value={x.orderStatus}
                        onChange={(e) =>
                          action(() =>
                            api.patch(`/api/admin/orders/${x.id}/status`, {
                              status: e.target.value,
                            }),
                          )
                        }
                      >
                        <option>PENDING</option>
                        <option>CONFIRMED</option>
                        <option>SHIPPED</option>
                        <option>DELIVERED</option>
                        <option>CANCELLED</option>
                      </select>
                    )}
                    {seller && (
                      <select
                        value={x.status}
                        onChange={(e) =>
                          action(() =>
                            api.patch(`/api/seller/orders/${x.id}/status`, {
                              status: e.target.value,
                            }),
                          )
                        }
                      >
                        <option>PENDING</option>
                        <option>CONFIRMED</option>
                        <option>SHIPPED</option>
                        <option>DELIVERED</option>
                        <option>CANCELLED</option>
                      </select>
                    )}
                  </td>
                </>
              ) : (
                <>
                  <td>
                    <strong>{x.name}</strong>
                    <small>
                      {seller
                        ? `${x.sellerStoreName || ""} · ${x.active ? "Active" : "Inactive"}`
                        : x.sellerStoreName || ""}
                    </small>
                  </td>
                  <td>{x.categoryName}</td>
                  <td>{money(x.price)}</td>
                  <td>
                    {seller ? (
                      <input
                        aria-label="Stock quantity"
                        className="qty"
                        type="number"
                        min="0"
                        defaultValue={x.stockQuantity}
                        onBlur={(e) =>
                          e.target.value !== String(x.stockQuantity) &&
                          action(() =>
                            api.patch(`/api/seller/products/${x.id}/stock`, {
                              stockQuantity: Number(e.target.value),
                            }),
                          )
                        }
                      />
                    ) : (
                      x.stockQuantity
                    )}
                  </td>
                  <td>
                    {seller && (
                      <>
                        <Link
                          className="text-button"
                          to={`/seller/products/${x.id}`}
                        >
                          Edit
                        </Link>
                        <button
                          className={
                            x.active ? "text-button danger" : "text-button"
                          }
                          onClick={() =>
                            action(() =>
                              api.put(`/api/seller/products/${x.id}`, {
                                name: x.name,
                                description: x.description,
                                price: x.price,
                                stockQuantity: x.stockQuantity,
                                imageUrl: x.imageUrl,
                                categoryId: x.categoryId,
                                active: !x.active,
                              }),
                            )
                          }
                        >
                          {x.active ? "Deactivate" : "Reactivate"}
                        </button>
                      </>
                    )}
                  </td>
                </>
              )}
            </tr>
          ))}
        </tbody>
      </table>
      {!items.length && <div className="empty">No records to show.</div>}
    </div>
  );
}
function Missing() {
  return (
    <div className="empty">
      <h2>Page not found</h2>
      <Link to="/">Back to MegaMart</Link>
    </div>
  );
}
function AppRoutes() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={user ? <Navigate to="/" /> : <Login />} />
        <Route
          path="/register"
          element={user ? <Navigate to="/" /> : <Register />}
        />
        <Route path="/products/:id" element={<Product />} />
        <Route element={<ProtectedRoute roles={["CUSTOMER"]} />}>
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/orders" element={<Orders />} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="/profile" element={<Profile />} />
        </Route>
        <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
          <Route path="/admin/*" element={<Workspace />} />
        </Route>
        <Route element={<ProtectedRoute roles={["SELLER"]} />}>
          <Route path="/seller/*" element={<Workspace seller />} />
        </Route>
        <Route path="*" element={<Missing />} />
      </Route>
    </Routes>
  );
}
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
