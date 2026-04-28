import { useState, useEffect, useCallback } from "react";

const API = "http://localhost:8080/api";

// ─── API helper with auth token ───
async function api(path, options = {}) {
  try {
    const token = localStorage.getItem("pharma_token");
    const headers = { "Content-Type": "application/json" };
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const res = await fetch(`${API}${path}`, { headers, ...options });
    return await res.json();
  } catch (e) {
    return { success: false, message: "Connection failed.", data: null };
  }
}

// Auth-specific calls (no token needed)
async function authApi(path, body) {
  try {
    const res = await fetch(`${API}/auth${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    return await res.json();
  } catch (e) {
    return { success: false, message: "Connection failed.", data: null };
  }
}

function exportToCSV(data, headers, filename) {
  const csv = [headers.map(h => h.label).join(","), ...data.map(row => headers.map(h => { let v = typeof h.accessor === "function" ? h.accessor(row) : row[h.accessor]; return `"${String(v ?? "").replace(/"/g, '""')}"`; }).join(","))].join("\n");
  const link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" }));
  link.download = `${filename}.csv`;
  link.click();
}

// ─── Micro components ───

function Notif({ msg, type, onClose }) {
  useEffect(() => { const t = setTimeout(onClose, 3000); return () => clearTimeout(t); }, [onClose]);
  return (
    <div style={{ position: "fixed", bottom: 24, left: "50%", transform: "translateX(-50%)", zIndex: 9999,
      padding: "10px 24px", background: type === "error" ? "#b91c1c" : "#15803d", color: "#fafafa",
      fontSize: 13, fontFamily: "var(--mono)", borderRadius: 4, boxShadow: "0 4px 20px rgba(0,0,0,0.4)" }}>
      {msg}
    </div>
  );
}

function Overlay({ title, onClose, children, wide }) {
  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 1000, background: "rgba(0,0,0,0.7)", display: "flex", alignItems: "center", justifyContent: "center" }} onClick={onClose}>
      <div onClick={e => e.stopPropagation()} style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 6,
        padding: 28, width: "100%", maxWidth: wide ? 620 : 440, maxHeight: "85vh", overflowY: "auto" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20, paddingBottom: 12, borderBottom: "1px solid var(--border)" }}>
          <span style={{ fontSize: 14, fontWeight: 600, color: "var(--text)", textTransform: "uppercase", letterSpacing: 1.5, fontFamily: "var(--mono)" }}>{title}</span>
          <button onClick={onClose} style={{ background: "none", border: "none", color: "var(--dim)", cursor: "pointer", fontSize: 18 }}>✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

function Field({ label, ...props }) {
  return (
    <div style={{ marginBottom: 14 }}>
      <label style={{ display: "block", fontSize: 10, fontWeight: 700, color: "var(--dim)", marginBottom: 5, textTransform: "uppercase", letterSpacing: 1.2, fontFamily: "var(--mono)" }}>{label}</label>
      <input {...props} style={{ width: "100%", padding: "9px 12px", background: "var(--bg)", border: "1px solid var(--border)", borderRadius: 3,
        fontSize: 13, color: "var(--text)", outline: "none", fontFamily: "var(--body)", boxSizing: "border-box", ...(props.style || {}) }}
        onFocus={e => e.target.style.borderColor = "var(--accent)"} onBlur={e => e.target.style.borderColor = "var(--border)"} />
    </div>
  );
}

function Dropdown({ label, children, ...props }) {
  return (
    <div style={{ marginBottom: 14 }}>
      <label style={{ display: "block", fontSize: 10, fontWeight: 700, color: "var(--dim)", marginBottom: 5, textTransform: "uppercase", letterSpacing: 1.2, fontFamily: "var(--mono)" }}>{label}</label>
      <select {...props} style={{ width: "100%", padding: "9px 12px", background: "var(--bg)", border: "1px solid var(--border)",
        borderRadius: 3, fontSize: 13, color: "var(--text)", outline: "none", fontFamily: "var(--body)", boxSizing: "border-box" }}>{children}</select>
    </div>
  );
}

function Pill({ children, color = "var(--accent)" }) {
  return <span style={{ display: "inline-block", padding: "2px 8px", borderRadius: 2, fontSize: 11, fontWeight: 600, fontFamily: "var(--mono)",
    background: color + "18", color, border: `1px solid ${color}40` }}>{children}</span>;
}

function Act({ children, kind = "default", ...props }) {
  const map = {
    default: { bg: "var(--surface)", color: "var(--text)", border: "var(--border)" },
    accent: { bg: "var(--accent)", color: "#1a1a1a", border: "var(--accent)" },
    danger: { bg: "transparent", color: "#ef4444", border: "#ef444440" },
    green: { bg: "#15803d", color: "#fafafa", border: "#15803d" },
    ghost: { bg: "transparent", color: "var(--dim)", border: "var(--border)" },
  };
  const s = map[kind];
  return <button {...props} style={{ padding: "7px 16px", borderRadius: 3, fontSize: 12, fontWeight: 600, cursor: "pointer",
    display: "inline-flex", alignItems: "center", gap: 6, fontFamily: "var(--mono)", letterSpacing: 0.3,
    background: s.bg, color: s.color, border: `1px solid ${s.border}`, transition: "opacity 0.15s", ...(props.style || {}) }}>{children}</button>;
}

function Metric({ label, value, mark }) {
  return (
    <div style={{ padding: "16px 20px", background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, flex: "1 1 180px" }}>
      <div style={{ fontSize: 10, color: "var(--dim)", fontFamily: "var(--mono)", textTransform: "uppercase", letterSpacing: 1.5, marginBottom: 8 }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 700, color: mark || "var(--text)", fontFamily: "var(--mono)" }}>{value}</div>
    </div>
  );
}

function Tag({ name, onRemove }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 4, padding: "2px 8px", background: "var(--accent)15", border: "1px solid var(--accent)30",
      borderRadius: 2, fontSize: 11, fontFamily: "var(--mono)", color: "var(--accent)" }}>
      {name}
      {onRemove && <button onClick={onRemove} style={{ background: "none", border: "none", cursor: "pointer", color: "var(--accent)", padding: 0, fontSize: 13, lineHeight: 1, marginLeft: 2 }}>×</button>}
    </span>
  );
}

function TH({ children, right }) {
  return <th style={{ textAlign: right ? "right" : "left", padding: "10px 16px", fontSize: 10, fontWeight: 700, color: "var(--dim)",
    textTransform: "uppercase", letterSpacing: 1.5, fontFamily: "var(--mono)", borderBottom: "1px solid var(--border)", background: "var(--bg)" }}>{children}</th>;
}

function TD({ children, right, mono, accent }) {
  return <td style={{ padding: "11px 16px", fontSize: 13, color: accent || "var(--text)", textAlign: right ? "right" : "left",
    fontFamily: mono ? "var(--mono)" : "var(--body)", fontWeight: mono ? 600 : 400, borderBottom: "1px solid var(--border)" }}>{children}</td>;
}

// ═══════════════════════════════════════
// LOGIN PAGE
// ═══════════════════════════════════════
function LoginPage({ onLogin }) {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const pwChecks = [
    { label: "8+ characters", test: p => p.length >= 8 },
    { label: "Lowercase letter", test: p => /[a-z]/.test(p) },
    { label: "Uppercase letter", test: p => /[A-Z]/.test(p) },
    { label: "Number", test: p => /[0-9]/.test(p) },
    { label: "Symbol (!@#$...)", test: p => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(p) },
    { label: "Not contain username", test: p => !username || !p.toLowerCase().includes(username.toLowerCase()) },
  ];

  const allValid = pwChecks.every(c => c.test(password));
  const pwStrength = pwChecks.filter(c => c.test(password)).length;
  const strengthLabel = pwStrength <= 2 ? "Weak" : pwStrength <= 4 ? "Medium" : pwStrength <= 5 ? "Strong" : "Excellent";
  const strengthColor = pwStrength <= 2 ? "#ef4444" : pwStrength <= 4 ? "#d97706" : pwStrength <= 5 ? "#2563eb" : "#15803d";

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (isRegister && !allValid) {
      setError("Please fix password requirements");
      return;
    }

    setLoading(true);
    const path = isRegister ? "/register" : "/login";
    const r = await authApi(path, { username, password });
    setLoading(false);

    if (r.success) {
      localStorage.setItem("pharma_token", r.data.token);
      localStorage.setItem("pharma_user", r.data.username);
      localStorage.setItem("pharma_role", r.data.role);
      onLogin(r.data);
    } else {
      setError(r.message);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "var(--body)" }}>
      <div style={{ width: 400, padding: 36, background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 6 }}>
        <div style={{ textAlign: "center", marginBottom: 32 }}>
          <div style={{ fontFamily: "var(--mono)", fontSize: 22, fontWeight: 700, color: "var(--accent)", letterSpacing: 2 }}>PHARMA</div>
          <div style={{ fontFamily: "var(--mono)", fontSize: 10, color: "var(--dim)", letterSpacing: 4, marginTop: 4 }}>MANAGEMENT SYSTEM</div>
        </div>

        <div style={{ display: "flex", marginBottom: 24, borderBottom: "1px solid var(--border)" }}>
          {["Login", "Register"].map(tab => (
            <button key={tab} onClick={() => { setIsRegister(tab === "Register"); setError(""); setPassword(""); }}
              style={{ flex: 1, padding: "10px 0", background: "none", border: "none", cursor: "pointer",
                fontFamily: "var(--mono)", fontSize: 11, letterSpacing: 1, textTransform: "uppercase",
                color: (tab === "Register") === isRegister ? "var(--accent)" : "var(--dim)",
                borderBottom: (tab === "Register") === isRegister ? "2px solid var(--accent)" : "2px solid transparent",
                fontWeight: (tab === "Register") === isRegister ? 700 : 400 }}>
              {tab}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 14 }}>
            <label style={{ display: "block", fontSize: 10, fontWeight: 700, color: "var(--dim)", marginBottom: 5, textTransform: "uppercase", letterSpacing: 1.2, fontFamily: "var(--mono)" }}>Username</label>
            <input value={username} onChange={e => setUsername(e.target.value)} required
              style={{ width: "100%", padding: "11px 14px", background: "var(--bg)", border: "1px solid var(--border)", borderRadius: 3,
                fontSize: 14, color: "var(--text)", outline: "none", fontFamily: "var(--body)", boxSizing: "border-box" }}
              onFocus={e => e.target.style.borderColor = "var(--accent)"} onBlur={e => e.target.style.borderColor = "var(--border)"} />
            {isRegister && username.length > 0 && (username.length < 3 || !/^[a-zA-Z0-9_]+$/.test(username)) && (
              <div style={{ fontSize: 10, fontFamily: "var(--mono)", color: "#ef4444", marginTop: 4 }}>
                {username.length < 3 ? "Min 3 characters" : "Only letters, numbers, underscores"}
              </div>
            )}
          </div>

          <div style={{ marginBottom: isRegister ? 12 : 20 }}>
            <label style={{ display: "block", fontSize: 10, fontWeight: 700, color: "var(--dim)", marginBottom: 5, textTransform: "uppercase", letterSpacing: 1.2, fontFamily: "var(--mono)" }}>Password</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required
              style={{ width: "100%", padding: "11px 14px", background: "var(--bg)", border: "1px solid var(--border)", borderRadius: 3,
                fontSize: 14, color: "var(--text)", outline: "none", fontFamily: "var(--body)", boxSizing: "border-box" }}
              onFocus={e => e.target.style.borderColor = "var(--accent)"} onBlur={e => e.target.style.borderColor = "var(--border)"} />
          </div>

          {isRegister && password.length > 0 && (
            <div style={{ marginBottom: 20, padding: "12px 14px", background: "var(--bg)", border: "1px solid var(--border)", borderRadius: 3 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10 }}>
                <div style={{ flex: 1, height: 4, background: "var(--border)", borderRadius: 2, overflow: "hidden" }}>
                  <div style={{ height: "100%", width: `${(pwStrength / 6) * 100}%`, background: strengthColor, borderRadius: 2, transition: "all 0.3s" }} />
                </div>
                <span style={{ fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: strengthColor, minWidth: 55, textAlign: "right" }}>{strengthLabel}</span>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "4px 12px" }}>
                {pwChecks.map((c, i) => {
                  const ok = c.test(password);
                  return (
                    <div key={i} style={{ display: "flex", alignItems: "center", gap: 6 }}>
                      <span style={{ fontFamily: "var(--mono)", fontSize: 12, color: ok ? "#15803d" : "#ef4444" }}>
                        {ok ? "✓" : "✕"}
                      </span>
                      <span style={{ fontSize: 11, fontFamily: "var(--mono)", color: ok ? "var(--dim)" : "var(--text)", textDecoration: ok ? "line-through" : "none" }}>
                        {c.label}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {error && (
            <div style={{ padding: "8px 12px", background: "#b91c1c18", border: "1px solid #b91c1c40", borderRadius: 3,
              color: "#ef4444", fontSize: 12, fontFamily: "var(--mono)", marginBottom: 16 }}>
              {error}
            </div>
          )}

          <button type="submit" disabled={loading || (isRegister && !allValid)}
            style={{ width: "100%", padding: "12px", background: (isRegister && !allValid) ? "var(--border)" : "var(--accent)",
              color: (isRegister && !allValid) ? "var(--dim)" : "#1a1a1a", border: "none",
              borderRadius: 3, fontSize: 13, fontWeight: 700, fontFamily: "var(--mono)", letterSpacing: 1,
              cursor: (loading || (isRegister && !allValid)) ? "not-allowed" : "pointer", textTransform: "uppercase",
              opacity: loading ? 0.6 : 1 }}>
            {loading ? "..." : isRegister ? "Create Account" : "Sign In"}
          </button>
        </form>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════
// DASHBOARD
// ═══════════════════════════════════════
function DashboardPage() {
  const [summary, setSummary] = useState(null);
  const [catStats, setCatStats] = useState([]);
  const [movements, setMovements] = useState([]);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([api("/reports/stock-summary"), api("/reports/category-stats"), api("/stock/movements?limit=8"), api("/logs?limit=10")])
      .then(([s, c, m, l]) => { if (s.success) setSummary(s.data); if (c.success) setCatStats(c.data); if (m.success) setMovements(m.data); if (l.success) setLogs(l.data); setLoading(false); });
  }, []);

  if (loading) return <div style={{ padding: 40, color: "var(--dim)", fontFamily: "var(--mono)" }}>LOADING...</div>;
  const ac = { CREATE: "#d97706", UPDATE: "#2563eb", DELETE: "#ef4444", STOCK_IN: "#15803d", STOCK_OUT: "#dc2626" };

  return (
    <div>
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 28 }}>
        <Metric label="Medicines" value={summary?.totalMedicines || 0} />
        <Metric label="Out of Stock" value={summary?.outOfStock || 0} mark="#ef4444" />
        <Metric label="Low Stock" value={summary?.lowStock || 0} mark="#d97706" />
        <Metric label="Total Value" value={`€${(summary?.totalStockValue || 0).toLocaleString()}`} mark="#15803d" />
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "3fr 2fr", gap: 16, marginBottom: 16 }}>
        <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, overflow: "hidden" }}>
          <div style={{ padding: "14px 16px", borderBottom: "1px solid var(--border)", fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 1.5 }}>Categories</div>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead><tr><TH>Name</TH><TH right>Items</TH><TH right>Stock</TH><TH right>Value</TH></tr></thead>
            <tbody>
              {catStats.map((c, i) => <tr key={i}><TD>{c.categoryName}</TD><TD right mono>{c.medicineCount}</TD><TD right mono>{c.totalStock}</TD><TD right mono accent="#15803d">€{c.totalValue?.toLocaleString()}</TD></tr>)}
              {catStats.length === 0 && <tr><TD>No categories yet</TD></tr>}
            </tbody>
          </table>
        </div>
        <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, overflow: "hidden" }}>
          <div style={{ padding: "14px 16px", borderBottom: "1px solid var(--border)", fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 1.5 }}>Activity Feed</div>
          <div style={{ padding: 8 }}>
            {logs.length === 0 ? <div style={{ padding: 16, color: "var(--dim)", fontSize: 12 }}>No activity.</div> :
              logs.map((l, i) => (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 10, padding: "7px 8px", borderBottom: i < logs.length - 1 ? "1px solid var(--border)" : "none" }}>
                  <div style={{ width: 6, height: 6, borderRadius: 1, background: ac[l.action] || "var(--dim)", flexShrink: 0 }} />
                  <span style={{ fontSize: 11, fontFamily: "var(--mono)", color: ac[l.action], fontWeight: 600, minWidth: 62 }}>{l.action}</span>
                  <span style={{ fontSize: 12, color: "var(--text)", flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{l.description}</span>
                  <span style={{ fontSize: 10, color: "var(--dim)", fontFamily: "var(--mono)", flexShrink: 0 }}>
                    {l.occurredAt ? new Date(l.occurredAt).toLocaleString("el-GR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" }) : ""}
                  </span>
                </div>
              ))}
          </div>
        </div>
      </div>
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, overflow: "hidden" }}>
        <div style={{ padding: "14px 16px", borderBottom: "1px solid var(--border)", fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 1.5 }}>Recent Movements</div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: 1, background: "var(--border)" }}>
          {movements.map((m, i) => (
            <div key={i} style={{ background: "var(--surface)", padding: "12px 16px", display: "flex", alignItems: "center", gap: 12 }}>
              <span style={{ fontFamily: "var(--mono)", fontSize: 14, fontWeight: 700, color: m.type === "IN" ? "#15803d" : "#ef4444", minWidth: 50 }}>{m.type === "IN" ? "▲ IN" : "▼ OUT"}</span>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 500, color: "var(--text)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{m.medicineName}</div>
                <div style={{ fontSize: 11, color: "var(--dim)" }}>{m.note || "—"}</div>
              </div>
              <span style={{ fontFamily: "var(--mono)", fontSize: 15, fontWeight: 700, color: m.type === "IN" ? "#15803d" : "#ef4444" }}>{m.type === "IN" ? "+" : "−"}{m.quantity}</span>
            </div>
          ))}
          {movements.length === 0 && <div style={{ background: "var(--surface)", padding: 20, color: "var(--dim)", fontSize: 12 }}>No movements.</div>}
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════
// CATEGORIES
// ═══════════════════════════════════════
function CategoriesPage({ notify }) {
  const [cats, setCats] = useState([]);
  const [modal, setModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: "", description: "" });
  const load = useCallback(() => { api("/categories").then(r => r.success && setCats(r.data)); }, []);
  useEffect(() => { load(); }, [load]);

  const submit = async () => {
    if (!form.name.trim()) return notify("Name required", "error");
    const r = await api(editing ? `/categories/${editing.id}` : "/categories", { method: editing ? "PUT" : "POST", body: JSON.stringify(form) });
    if (r.success) { notify(editing ? "Updated" : "Created"); setModal(false); setEditing(null); setForm({ name: "", description: "" }); load(); }
    else notify(r.message, "error");
  };
  const remove = async id => { if (!confirm("Delete?")) return; const r = await api(`/categories/${id}`, { method: "DELETE" }); if (r.success) { notify("Deleted"); load(); } else notify(r.message, "error"); };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
        <span style={{ fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 2 }}>{cats.length} categories</span>
        <Act kind="accent" onClick={() => { setEditing(null); setForm({ name: "", description: "" }); setModal(true); }}>+ NEW</Act>
      </div>
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead><tr><TH>Name</TH><TH>Description</TH><TH right>Medicines</TH><TH right>Actions</TH></tr></thead>
          <tbody>
            {cats.map(c => (
              <tr key={c.id}><TD mono>{c.name}</TD><TD>{c.description || "—"}</TD><TD right><Pill>{c.medicineCount}</Pill></TD>
                <TD right><div style={{ display: "flex", gap: 6, justifyContent: "flex-end" }}><Act kind="ghost" onClick={() => { setEditing(c); setForm({ name: c.name, description: c.description || "" }); setModal(true); }}>edit</Act><Act kind="danger" onClick={() => remove(c.id)}>del</Act></div></TD></tr>
            ))}
            {cats.length === 0 && <tr><td colSpan={4} style={{ padding: 30, textAlign: "center", color: "var(--dim)", fontSize: 12 }}>Empty.</td></tr>}
          </tbody>
        </table>
      </div>
      {modal && (
        <Overlay title={editing ? "Edit Category" : "New Category"} onClose={() => setModal(false)}>
          <Field label="Name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g. Antibiotics" />
          <Field label="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Optional" />
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 12 }}><Act kind="ghost" onClick={() => setModal(false)}>Cancel</Act><Act kind="accent" onClick={submit}>{editing ? "Save" : "Create"}</Act></div>
        </Overlay>
      )}
    </div>
  );
}

// ═══════════════════════════════════════
// MEDICINES
// ═══════════════════════════════════════
function MedicinesPage({ notify }) {
  const [meds, setMeds] = useState([]);
  const [cats, setCats] = useState([]);
  const [q, setQ] = useState("");
  const [results, setResults] = useState(null);
  const [modal, setModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ code: "", name: "", price: "", categoryId: "", ingredients: [] });
  const [ingInput, setIngInput] = useState("");

  const load = useCallback(() => { api("/medicines").then(r => r.success && setMeds(r.data)); api("/categories").then(r => r.success && setCats(r.data)); }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    if (!q.trim()) { setResults(null); return; }
    const t = setTimeout(() => { api(`/medicines/search?q=${encodeURIComponent(q)}`).then(r => r.success && setResults(r.data)); }, 300);
    return () => clearTimeout(t);
  }, [q]);

  const display = results !== null ? results : meds;
  const addIng = () => { if (ingInput.trim() && !form.ingredients.includes(ingInput.trim())) { setForm({ ...form, ingredients: [...form.ingredients, ingInput.trim()] }); setIngInput(""); } };

  const submit = async () => {
    if (!form.code || !form.name || !form.price || !form.categoryId) return notify("All fields required", "error");
    const body = { code: form.code, name: form.name, price: parseFloat(form.price), categoryId: parseInt(form.categoryId), ingredients: form.ingredients };
    const r = await api(editing ? `/medicines/${editing.id}` : "/medicines", { method: editing ? "PUT" : "POST", body: JSON.stringify(body) });
    if (r.success) { notify(editing ? "Updated" : "Created"); setModal(false); setEditing(null); setForm({ code: "", name: "", price: "", categoryId: "", ingredients: [] }); load(); }
    else notify(r.message, "error");
  };
  const remove = async id => { if (!confirm("Delete?")) return; const r = await api(`/medicines/${id}`, { method: "DELETE" }); if (r.success) { notify("Deleted"); load(); } else notify(r.message, "error"); };
  const stockColor = qty => qty === 0 ? "#ef4444" : qty <= 10 ? "#d97706" : "#15803d";

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <span style={{ fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 2 }}>{display.length} medicines</span>
          {results !== null && <button onClick={() => { setQ(""); setResults(null); }} style={{ background: "none", border: "none", color: "var(--accent)", cursor: "pointer", fontSize: 11, fontFamily: "var(--mono)" }}>✕ clear</button>}
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <input placeholder="Search name, code, ingredient..." value={q} onChange={e => setQ(e.target.value)}
            style={{ padding: "7px 12px", background: "var(--bg)", border: "1px solid var(--border)", borderRadius: 3, fontSize: 12, color: "var(--text)", outline: "none", width: 260, fontFamily: "var(--body)" }} />
          <Act kind="accent" onClick={() => { setEditing(null); setForm({ code: "", name: "", price: "", categoryId: cats[0]?.id || "", ingredients: [] }); setModal(true); }}>+ NEW</Act>
        </div>
      </div>
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead><tr><TH>Code</TH><TH>Name</TH><TH>Ingredients</TH><TH>Category</TH><TH right>Price</TH><TH right>Stock</TH><TH right>Actions</TH></tr></thead>
          <tbody>
            {display.map(m => (
              <tr key={m.id}>
                <TD mono accent="var(--accent)">{m.code}</TD><TD>{m.name}</TD>
                <TD><div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>{(m.ingredients || []).length > 0 ? m.ingredients.map((ing, i) => <Tag key={i} name={ing} />) : <span style={{ color: "var(--dim)", fontSize: 11 }}>—</span>}</div></TD>
                <TD>{m.categoryName}</TD><TD right mono>€{m.price?.toFixed(2)}</TD>
                <TD right><Pill color={stockColor(m.stockQty)}>{m.stockQty}</Pill></TD>
                <TD right><div style={{ display: "flex", gap: 6, justifyContent: "flex-end" }}><Act kind="ghost" onClick={() => { setEditing(m); setForm({ code: m.code, name: m.name, price: m.price.toString(), categoryId: m.categoryId.toString(), ingredients: m.ingredients || [] }); setModal(true); }}>edit</Act><Act kind="danger" onClick={() => remove(m.id)}>del</Act></div></TD>
              </tr>
            ))}
            {display.length === 0 && <tr><td colSpan={7} style={{ padding: 30, textAlign: "center", color: "var(--dim)", fontSize: 12 }}>{q ? "No matches." : "No medicines."}</td></tr>}
          </tbody>
        </table>
      </div>
      {modal && (
        <Overlay title={editing ? "Edit Medicine" : "New Medicine"} onClose={() => setModal(false)} wide>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            <Field label="Code" value={form.code} onChange={e => setForm({ ...form, code: e.target.value })} placeholder="ASP-500" />
            <Field label="Name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Aspirin 500mg" />
            <Field label="Price €" type="number" step="0.01" value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} />
            <Dropdown label="Category" value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })}><option value="">Select...</option>{cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</Dropdown>
          </div>
          <div style={{ marginBottom: 14 }}>
            <label style={{ display: "block", fontSize: 10, fontWeight: 700, color: "var(--dim)", marginBottom: 5, textTransform: "uppercase", letterSpacing: 1.2, fontFamily: "var(--mono)" }}>Ingredients</label>
            <div style={{ display: "flex", gap: 8, marginBottom: 6 }}>
              <input value={ingInput} onChange={e => setIngInput(e.target.value)} onKeyDown={e => { if (e.key === "Enter") { e.preventDefault(); addIng(); } }}
                placeholder="Type + Enter" style={{ flex: 1, padding: "8px 12px", background: "var(--bg)", border: "1px solid var(--border)", borderRadius: 3, fontSize: 13, color: "var(--text)", outline: "none", fontFamily: "var(--body)" }} />
              <Act kind="ghost" onClick={addIng}>Add</Act>
            </div>
            <div style={{ display: "flex", gap: 5, flexWrap: "wrap" }}>{form.ingredients.map((ing, i) => <Tag key={i} name={ing} onRemove={() => setForm({ ...form, ingredients: form.ingredients.filter((_, j) => j !== i) })} />)}</div>
          </div>
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 12 }}><Act kind="ghost" onClick={() => setModal(false)}>Cancel</Act><Act kind="accent" onClick={submit}>{editing ? "Save" : "Create"}</Act></div>
        </Overlay>
      )}
    </div>
  );
}

// ═══════════════════════════════════════
// STOCK
// ═══════════════════════════════════════
function StockPage({ notify }) {
  const [moves, setMoves] = useState([]);
  const [meds, setMeds] = useState([]);
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState({ medicineId: "", type: "IN", quantity: "", note: "" });
  const load = useCallback(() => { api("/stock/movements?limit=100").then(r => r.success && setMoves(r.data)); api("/medicines").then(r => r.success && setMeds(r.data)); }, []);
  useEffect(() => { load(); }, [load]);

  const submit = async () => {
    if (!form.medicineId || !form.quantity) return notify("Required", "error");
    const r = await api("/stock/movement", { method: "POST", body: JSON.stringify({ medicineId: parseInt(form.medicineId), type: form.type, quantity: parseInt(form.quantity), note: form.note }) });
    if (r.success) { notify(`Stock ${form.type}`); setModal(false); setForm({ medicineId: "", type: "IN", quantity: "", note: "" }); load(); } else notify(r.message, "error");
  };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
        <span style={{ fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 2 }}>{moves.length} movements</span>
        <Act kind="green" onClick={() => setModal(true)}>+ MOVEMENT</Act>
      </div>
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead><tr><TH>Type</TH><TH>Medicine</TH><TH right>Qty</TH><TH>Date</TH><TH>Note</TH></tr></thead>
          <tbody>
            {moves.map((m, i) => (
              <tr key={i}><TD><Pill color={m.type === "IN" ? "#15803d" : "#ef4444"}>▲ {m.type}</Pill></TD><TD>{m.medicineName}</TD>
                <TD right mono accent={m.type === "IN" ? "#15803d" : "#ef4444"}>{m.type === "IN" ? "+" : "−"}{m.quantity}</TD>
                <TD mono>{m.occurredAt ? new Date(m.occurredAt).toLocaleString("el-GR") : "—"}</TD><TD>{m.note || "—"}</TD></tr>
            ))}
            {moves.length === 0 && <tr><td colSpan={5} style={{ padding: 30, textAlign: "center", color: "var(--dim)", fontSize: 12 }}>No movements.</td></tr>}
          </tbody>
        </table>
      </div>
      {modal && (
        <Overlay title="Record Movement" onClose={() => setModal(false)}>
          <Dropdown label="Medicine" value={form.medicineId} onChange={e => setForm({ ...form, medicineId: e.target.value })}><option value="">Select...</option>{meds.map(m => <option key={m.id} value={m.id}>{m.name} [{m.stockQty}]</option>)}</Dropdown>
          <div style={{ display: "flex", gap: 8, marginBottom: 14 }}>
            {["IN", "OUT"].map(t => (
              <button key={t} onClick={() => setForm({ ...form, type: t })} style={{
                flex: 1, padding: 10, borderRadius: 3, cursor: "pointer", fontFamily: "var(--mono)", fontSize: 13, fontWeight: 700, letterSpacing: 1,
                border: `2px solid ${form.type === t ? (t === "IN" ? "#15803d" : "#ef4444") : "var(--border)"}`,
                background: form.type === t ? (t === "IN" ? "#15803d15" : "#ef444415") : "var(--bg)",
                color: form.type === t ? (t === "IN" ? "#15803d" : "#ef4444") : "var(--dim)" }}>
                {t === "IN" ? "▲ STOCK IN" : "▼ STOCK OUT"}
              </button>
            ))}
          </div>
          <Field label="Quantity" type="number" min="1" value={form.quantity} onChange={e => setForm({ ...form, quantity: e.target.value })} />
          <Field label="Note" value={form.note} onChange={e => setForm({ ...form, note: e.target.value })} placeholder="Optional" />
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 12 }}><Act kind="ghost" onClick={() => setModal(false)}>Cancel</Act><Act kind={form.type === "IN" ? "green" : "accent"} onClick={submit}>Record</Act></div>
        </Overlay>
      )}
    </div>
  );
}

// ═══════════════════════════════════════
// LOG
// ═══════════════════════════════════════
function LogPage() {
  const [logs, setLogs] = useState([]);
  const [limit, setLimit] = useState(50);
  const load = useCallback(() => { api(`/logs?limit=${limit}`).then(r => r.success && setLogs(r.data)); }, [limit]);
  useEffect(() => { load(); }, [load]);
  const ac = { CREATE: "#d97706", UPDATE: "#2563eb", DELETE: "#ef4444", STOCK_IN: "#15803d", STOCK_OUT: "#dc2626" };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
        <div style={{ display: "flex", gap: 6 }}>{[50, 100, 200].map(n => <Act key={n} kind={limit === n ? "accent" : "ghost"} onClick={() => setLimit(n)} style={{ padding: "5px 12px" }}>{n}</Act>)}</div>
        <Act kind="ghost" onClick={() => exportToCSV(logs, [{ label: "Date", accessor: l => l.occurredAt ? new Date(l.occurredAt).toLocaleString("el-GR") : "" }, { label: "Action", accessor: "action" }, { label: "Type", accessor: "entityType" }, { label: "Description", accessor: "description" }], "activity-log")}>↓ Export</Act>
      </div>
      <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, overflow: "hidden" }}>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead><tr><TH>Date</TH><TH>Action</TH><TH>Entity</TH><TH>Description</TH></tr></thead>
          <tbody>
            {logs.map((l, i) => (<tr key={i}><TD mono>{l.occurredAt ? new Date(l.occurredAt).toLocaleString("el-GR") : "—"}</TD><TD><Pill color={ac[l.action] || "var(--dim)"}>{l.action}</Pill></TD><TD mono>{l.entityType}</TD><TD>{l.description || "—"}</TD></tr>))}
            {logs.length === 0 && <tr><td colSpan={4} style={{ padding: 30, textAlign: "center", color: "var(--dim)", fontSize: 12 }}>No activity.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════
// STATISTICS
// ═══════════════════════════════════════
function StatsPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [preset, setPreset] = useState("year");

  const ranges = {
    month: () => { const n = new Date(); return { from: new Date(n.getFullYear(), n.getMonth(), 1), to: n }; },
    quarter: () => { const n = new Date(); return { from: new Date(n.getFullYear(), n.getMonth() - 2, 1), to: n }; },
    half: () => { const n = new Date(); return { from: new Date(n.getFullYear(), n.getMonth() - 5, 1), to: n }; },
    year: () => { const n = new Date(); return { from: new Date(n.getFullYear(), 0, 1), to: n }; },
  };

  const loadStats = useCallback(() => {
    setLoading(true);
    const { from, to } = ranges[preset]();
    api(`/logs/stats?from=${from.toISOString()}&to=${to.toISOString()}&groupBy=${preset === "month" ? "day" : "month"}`)
      .then(r => { if (r.success) setStats(r.data); setLoading(false); });
  }, [preset]);
  useEffect(() => { loadStats(); }, [loadStats]);

  const ac = { CREATE: "#d97706", UPDATE: "#2563eb", DELETE: "#ef4444", STOCK_IN: "#15803d", STOCK_OUT: "#dc2626" };
  const labels = { month: "Month", quarter: "3 Months", half: "6 Months", year: "Year" };
  const maxC = Math.max(1, ...(stats?.breakdown || []).map(b => b.count));

  const doExport = () => {
    if (!stats) return;
    const rows = [];
    (stats.summary || []).forEach(s => rows.push({ A: "Summary", B: s.action, C: s.count }));
    (stats.breakdown || []).forEach(b => rows.push({ A: b.period, B: b.action, C: b.count }));
    (stats.topMedicines || []).forEach(m => rows.push({ A: m.medicineName, B: `IN:${m.totalIn} OUT:${m.totalOut}`, C: m.movementCount }));
    exportToCSV(rows, [{ label: "Period", accessor: "A" }, { label: "Detail", accessor: "B" }, { label: "Count", accessor: "C" }], `stats-${preset}`);
  };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
        <div style={{ display: "flex", gap: 6 }}>{Object.entries(labels).map(([k, v]) => <Act key={k} kind={preset === k ? "accent" : "ghost"} onClick={() => setPreset(k)} style={{ padding: "5px 12px" }}>{v}</Act>)}</div>
        <Act kind="green" onClick={doExport}>↓ Export</Act>
      </div>
      {loading ? <div style={{ padding: 30, color: "var(--dim)", fontFamily: "var(--mono)" }}>Loading...</div> : !stats ? <div style={{ padding: 30, color: "var(--dim)" }}>No data.</div> : (
        <div>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginBottom: 20 }}>
            {(stats.summary || []).map((s, i) => (
              <div key={i} style={{ padding: "14px 18px", background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, borderLeft: `3px solid ${ac[s.action] || "var(--dim)"}`, flex: "1 1 130px" }}>
                <div style={{ fontSize: 10, fontFamily: "var(--mono)", color: "var(--dim)", textTransform: "uppercase", letterSpacing: 1.2, marginBottom: 6 }}>{s.action}</div>
                <div style={{ fontSize: 22, fontWeight: 700, fontFamily: "var(--mono)", color: ac[s.action] || "var(--text)" }}>{s.count}</div>
              </div>
            ))}
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
            <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, padding: 20 }}>
              <div style={{ fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 1.5, marginBottom: 14 }}>Breakdown</div>
              {(stats.breakdown || []).map((b, i) => (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
                  <span style={{ width: 60, fontSize: 10, fontFamily: "var(--mono)", color: "var(--dim)" }}>{b.period}</span>
                  <span style={{ width: 60, fontSize: 10, fontFamily: "var(--mono)", color: ac[b.action], fontWeight: 600 }}>{b.action}</span>
                  <div style={{ flex: 1, height: 16, background: "var(--bg)", borderRadius: 2, overflow: "hidden" }}><div style={{ height: "100%", width: `${(b.count / maxC) * 100}%`, background: ac[b.action] || "var(--dim)", borderRadius: 2 }} /></div>
                  <span style={{ width: 28, fontSize: 11, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--text)", textAlign: "right" }}>{b.count}</span>
                </div>
              ))}
            </div>
            <div style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 4, padding: 20 }}>
              <div style={{ fontSize: 10, fontFamily: "var(--mono)", fontWeight: 700, color: "var(--dim)", textTransform: "uppercase", letterSpacing: 1.5, marginBottom: 14 }}>Top Medicines</div>
              {(stats.topMedicines || []).map((m, i) => (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 10, padding: "8px 0", borderBottom: i < stats.topMedicines.length - 1 ? "1px solid var(--border)" : "none" }}>
                  <span style={{ fontFamily: "var(--mono)", fontSize: 12, fontWeight: 700, color: "var(--accent)", width: 20 }}>{i + 1}</span>
                  <div style={{ flex: 1 }}><div style={{ fontSize: 13, fontWeight: 500, color: "var(--text)" }}>{m.medicineName}</div><div style={{ fontSize: 10, fontFamily: "var(--mono)", color: "var(--dim)" }}>{m.movementCount} moves</div></div>
                  <span style={{ fontSize: 11, fontFamily: "var(--mono)", fontWeight: 600, color: "#15803d" }}>+{m.totalIn}</span>
                  <span style={{ fontSize: 11, fontFamily: "var(--mono)", fontWeight: 600, color: "#ef4444" }}>−{m.totalOut}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════
// MAIN APP
// ═══════════════════════════════════════
export default function App() {
  const [user, setUser] = useState(null);
  const [page, setPage] = useState("dashboard");
  const [toast, setToast] = useState(null);
  const notify = useCallback((msg, type = "ok") => setToast({ msg, type }), []);

  // Check if already logged in
  useEffect(() => {
    const token = localStorage.getItem("pharma_token");
    const username = localStorage.getItem("pharma_user");
    const role = localStorage.getItem("pharma_role");
    if (token && username) {
      setUser({ token, username, role });
    }
  }, []);

  const handleLogin = (data) => {
    setUser(data);
  };

  const handleLogout = () => {
    localStorage.removeItem("pharma_token");
    localStorage.removeItem("pharma_user");
    localStorage.removeItem("pharma_role");
    setUser(null);
  };

  const nav = [
    { id: "dashboard", label: "Overview" },
    { id: "categories", label: "Categories" },
    { id: "medicines", label: "Medicines" },
    { id: "stock", label: "Stock" },
    { id: "logs", label: "Log" },
    { id: "stats", label: "Statistics" },
  ];

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", fontFamily: "var(--body)", color: "var(--text)" }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600;700&family=Outfit:wght@300;400;500;600;700&display=swap');
        :root {
          --bg: #131516;
          --surface: #1a1d1f;
          --border: #2a2d2f;
          --text: #e0ddd5;
          --dim: #6b6966;
          --accent: #d4a843;
          --mono: 'IBM Plex Mono', monospace;
          --body: 'Outfit', sans-serif;
        }
        * { margin: 0; box-sizing: border-box; }
        button:hover { opacity: 0.88; }
        tr:hover td { background: #1e2123 !important; }
        ::selection { background: var(--accent); color: #1a1a1a; }
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-track { background: var(--bg); }
        ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }
      `}</style>

      {!user ? (
        <LoginPage onLogin={handleLogin} />
      ) : (
        <div style={{ display: "flex", minHeight: "100vh" }}>
          {/* Sidebar */}
          <div style={{ width: 200, background: "var(--surface)", borderRight: "1px solid var(--border)", padding: "24px 0", display: "flex", flexDirection: "column" }}>
            <div style={{ padding: "0 20px", marginBottom: 32 }}>
              <div style={{ fontFamily: "var(--mono)", fontSize: 15, fontWeight: 700, color: "var(--accent)", letterSpacing: 1 }}>PHARMA</div>
              <div style={{ fontFamily: "var(--mono)", fontSize: 9, color: "var(--dim)", letterSpacing: 3, marginTop: 2 }}>MGMT SYSTEM</div>
            </div>

            <nav style={{ flex: 1 }}>
              {nav.map(item => (
                <button key={item.id} onClick={() => setPage(item.id)} style={{
                  display: "block", width: "100%", textAlign: "left", padding: "10px 20px", border: "none", cursor: "pointer",
                  fontFamily: "var(--mono)", fontSize: 11, letterSpacing: 1, textTransform: "uppercase",
                  background: page === item.id ? "var(--bg)" : "transparent",
                  color: page === item.id ? "var(--accent)" : "var(--dim)",
                  borderLeft: page === item.id ? "2px solid var(--accent)" : "2px solid transparent",
                  fontWeight: page === item.id ? 700 : 400 }}>{item.label}</button>
              ))}
            </nav>

            <div style={{ padding: "16px 20px", borderTop: "1px solid var(--border)" }}>
              <div style={{ fontSize: 11, fontFamily: "var(--mono)", color: "var(--accent)", marginBottom: 4 }}>{user.username}</div>
              <div style={{ fontSize: 10, fontFamily: "var(--mono)", color: "var(--dim)", marginBottom: 10 }}>{user.role}</div>
              <button onClick={handleLogout} style={{ background: "none", border: "1px solid var(--border)", borderRadius: 3,
                padding: "5px 12px", color: "var(--dim)", fontSize: 10, fontFamily: "var(--mono)", cursor: "pointer", letterSpacing: 1, textTransform: "uppercase" }}>
                Logout
              </button>
            </div>
          </div>

          {/* Content */}
          <div style={{ flex: 1, padding: "28px 36px", maxWidth: 1100, overflowY: "auto" }}>
            <div style={{ marginBottom: 24, paddingBottom: 16, borderBottom: "1px solid var(--border)" }}>
              <h1 style={{ fontSize: 18, fontWeight: 600, color: "var(--text)", fontFamily: "var(--body)", margin: 0 }}>
                {nav.find(n => n.id === page)?.label}
              </h1>
            </div>
            {page === "dashboard" && <DashboardPage />}
            {page === "categories" && <CategoriesPage notify={notify} />}
            {page === "medicines" && <MedicinesPage notify={notify} />}
            {page === "stock" && <StockPage notify={notify} />}
            {page === "logs" && <LogPage />}
            {page === "stats" && <StatsPage />}
          </div>
        </div>
      )}

      {toast && <Notif msg={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
}