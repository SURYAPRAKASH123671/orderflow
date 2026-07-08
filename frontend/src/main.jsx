import React, { useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import { Activity, Boxes, CheckCircle2, KeyRound, PackageSearch, Send } from "lucide-react";
import "./styles.css";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === "true";

const demoState = {
  token: "demo-jwt-token",
  product: { id: 1, name: "Mechanical Keyboard", stock: 25, price: 2499 },
  orderId: 1000,
  orders: new Map()
};

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" }
  });
}

async function demoFetch(url, options = {}) {
  await new Promise((resolve) => setTimeout(resolve, 450));
  const path = url.replace(API_URL, "");

  if (path === "/api/auth/token") {
    return jsonResponse({ accessToken: demoState.token, tokenType: "Bearer", expiresInSeconds: 3600 });
  }

  if (path.startsWith("/api/inventory/")) {
    return jsonResponse(demoState.product);
  }

  if (path === "/api/orders" && options.method === "POST") {
    const request = JSON.parse(options.body || "{}");
    const order = {
      id: ++demoState.orderId,
      customerEmail: request.customerEmail,
      status: "PENDING",
      createdAt: new Date().toISOString(),
      items: request.items.map((item, index) => ({ id: index + 1, ...item }))
    };
    demoState.orders.set(String(order.id), order);
    setTimeout(() => {
      const current = demoState.orders.get(String(order.id));
      if (!current || current.status !== "PENDING") {
        return;
      }
      const orderedQuantity = current.items.reduce((sum, item) => sum + item.quantity, 0);
      demoState.product = { ...demoState.product, stock: Math.max(0, demoState.product.stock - orderedQuantity) };
      demoState.orders.set(String(order.id), { ...current, status: "CONFIRMED" });
    }, 1200);
    return jsonResponse(order, 201);
  }

  if (path.startsWith("/api/orders/")) {
    const id = path.split("/").pop();
    const order = demoState.orders.get(id);
    return order ? jsonResponse(order) : jsonResponse({ message: `Order not found: ${id}` }, 404);
  }

  return jsonResponse({ message: "Demo endpoint not found" }, 404);
}

function request(url, options) {
  return DEMO_MODE ? demoFetch(url, options) : fetch(url, options);
}

function App() {
  const [token, setToken] = useState("");
  const [productId, setProductId] = useState("1");
  const [quantity, setQuantity] = useState("2");
  const [orderId, setOrderId] = useState("");
  const [output, setOutput] = useState(null);
  const [loading, setLoading] = useState(false);

  const authHeaders = useMemo(() => ({
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  }), [token]);

  async function callApi(label, request) {
    setLoading(true);
    try {
      const response = await request();
      const contentType = response.headers.get("content-type") || "";
      const body = contentType.includes("application/json") ? await response.json() : await response.text();
      setOutput({ label, status: response.status, body });
      return body;
    } catch (error) {
      setOutput({ label, status: "client-error", body: { message: error.message } });
    } finally {
      setLoading(false);
    }
  }

  async function issueToken() {
    const body = await callApi("Issued demo JWT", () => request(`${API_URL}/api/auth/token`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: "surya-demo", email: "surya@example.com", role: "CUSTOMER" })
    }));
    if (body?.accessToken) {
      setToken(body.accessToken);
    }
  }

  function getInventory() {
    return callApi("Inventory lookup", () => request(`${API_URL}/api/inventory/${productId}`, {
      headers: authHeaders
    }));
  }

  async function placeOrder() {
    const body = await callApi("Order placed", () => request(`${API_URL}/api/orders`, {
      method: "POST",
      headers: authHeaders,
      body: JSON.stringify({
        customerEmail: "customer@example.com",
        items: [{ productId: Number(productId), quantity: Number(quantity) }]
      })
    }));
    if (body?.id) {
      setOrderId(String(body.id));
    }
  }

  function getOrder() {
    return callApi("Order status", () => request(`${API_URL}/api/orders/${orderId}`, {
      headers: authHeaders
    }));
  }

  return (
    <main className="shell">
      <section className="topbar">
        <div>
          <h1>OrderFlow Console</h1>
          <p>Event-driven order, inventory, notification, security, caching, and observability demo.</p>
        </div>
        <div className="badges">
          {DEMO_MODE && <span className="status demo">Vercel demo</span>}
          <span className={token ? "status ready" : "status"}>{token ? "JWT ready" : "No token"}</span>
        </div>
      </section>

      <section className="grid">
        <Panel icon={<KeyRound />} title="Gateway Auth">
          <button onClick={issueToken} disabled={loading}>
            <KeyRound size={18} /> Issue Demo JWT
          </button>
          <p className="hint">Gateway validates the token and passes user headers downstream.</p>
        </Panel>

        <Panel icon={<PackageSearch />} title="Inventory">
          <label>
            Product ID
            <input value={productId} onChange={(event) => setProductId(event.target.value)} />
          </label>
          <button onClick={getInventory} disabled={loading || !token}>
            <PackageSearch size={18} /> Check Stock
          </button>
        </Panel>

        <Panel icon={<Send />} title="Place Order">
          <label>
            Quantity
            <input value={quantity} onChange={(event) => setQuantity(event.target.value)} />
          </label>
          <button onClick={placeOrder} disabled={loading || !token}>
            <Send size={18} /> Submit Order
          </button>
        </Panel>

        <Panel icon={<Activity />} title="Track Status">
          <label>
            Order ID
            <input value={orderId} onChange={(event) => setOrderId(event.target.value)} />
          </label>
          <button onClick={getOrder} disabled={loading || !token || !orderId}>
            <Activity size={18} /> Refresh Order
          </button>
        </Panel>
      </section>

      <section className="timeline">
        <Step icon={<CheckCircle2 />} title="1. Order Service" text="Saves order as PENDING and publishes orders.placed." />
        <Step icon={<Boxes />} title="2. Inventory Service" text="Consumes event, decrements stock, updates cache, publishes inventory.updated." />
        <Step icon={<Activity />} title="3. Notification Service" text="Consumes events and logs customer or low-stock notifications." />
      </section>

      <section className="output">
        <div className="output-head">
          <h2>{output?.label || "API Output"}</h2>
          <span>{loading ? "Loading" : output ? `HTTP ${output.status}` : "Waiting"}</span>
        </div>
        <pre>{output ? JSON.stringify(output.body, null, 2) : "Run an action to see the response."}</pre>
      </section>
    </main>
  );
}

function Panel({ icon, title, children }) {
  return (
    <article className="panel">
      <div className="panel-title">{icon}<h2>{title}</h2></div>
      {children}
    </article>
  );
}

function Step({ icon, title, text }) {
  return (
    <article className="step">
      <div>{icon}</div>
      <h3>{title}</h3>
      <p>{text}</p>
    </article>
  );
}

createRoot(document.getElementById("root")).render(<App />);
