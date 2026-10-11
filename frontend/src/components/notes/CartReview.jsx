import { useState, useEffect } from "react";
import { cartService } from "../../services/cartService";

const describeError = (err) => {
  const code = err.response?.data?.code;
  if (code === "SWIGGY_NOT_CONNECTED")
    return "Connect your Swiggy account first.";
  if (code === "SWIGGY_TOKEN_EXPIRED")
    return "Your Swiggy session expired. Click Connect Swiggy again.";
  return (
    err.response?.data?.message || "Something went wrong. Please try again."
  );
};

export const CartReview = ({ token, groceryList, onClose }) => {
  const [addresses, setAddresses] = useState([]);
  const [addressId, setAddressId] = useState("");
  const [preview, setPreview] = useState(null);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const list = await cartService.getAddresses(token);
        if (cancelled) return;
        setAddresses(list);
        if (list[0]) setAddressId(list[0].id);
      } catch (err) {
        if (!cancelled) setMessage({ ok: false, text: describeError(err) });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const handlePreview = async () => {
    setBusy(true);
    setMessage(null);
    try {
      setPreview(
        await cartService.previewCart(addressId, groceryList.items, token),
      );
    } catch (err) {
      setMessage({ ok: false, text: describeError(err) });
    } finally {
      setBusy(false);
    }
  };

  const removeLine = (spinId) =>
    setPreview((p) => ({
      ...p,
      matched: p.matched.filter((m) => m.spinId !== spinId),
    }));

  const handleConfirm = async () => {
    setBusy(true);
    setMessage(null);
    try {
      const lines = preview.matched.map((m) => ({
        spinId: m.spinId,
        skuId: m.skuId,
        quantity: m.quantity,
      }));
      const res = await cartService.syncCart(addressId, lines, token);
      setMessage({
        ok: true,
        text: `Added ${res.itemsSent} items to your Instamart cart. ${res.warnings.join(" ")}`,
      });
    } catch (err) {
      setMessage({ ok: false, text: describeError(err) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mt-3 p-3 rounded-lg border border-accent/30 bg-lightBg dark:bg-darkBg space-y-3 text-xs">
      <div className="flex gap-2">
        <select
          value={addressId}
          onChange={(e) => setAddressId(e.target.value)}
          className="flex-1 p-2 rounded border border-slate-300 dark:border-slate-600 dark:bg-slate-800"
        >
          {addresses.map((a) => (
            <option key={a.id} value={a.id}>
              {a.addressTag} {a.addressLine}
            </option>
          ))}
        </select>
        <button
          onClick={handlePreview}
          disabled={busy || !addressId}
          className="px-3 py-1.5 bg-primary text-white rounded font-semibold"
        >
          Find products
        </button>
      </div>

      {preview && (
        <>
          <ul className="space-y-1">
            {preview.matched.map((m) => (
              <li key={m.spinId} className="flex justify-between gap-2">
                <span>
                  {m.quantity} × {m.productName} ({m.variationName})
                </span>
                <span>
                  ₹{m.lineTotal}
                  <button
                    onClick={() => removeLine(m.spinId)}
                    className="ml-2 text-red-500"
                  >
                    ✕
                  </button>
                </span>
              </li>
            ))}
          </ul>
          {preview.unmatched.length > 0 && (
            <p className="text-red-500">
              Not found: {preview.unmatched.join(", ")}
            </p>
          )}
          <p className="font-semibold">
            Total: ₹{preview.matched.reduce((s, m) => s + m.lineTotal, 0)}
          </p>
          <p className="text-slate-500">
            Warning: this replaces everything currently in your Instamart cart.
          </p>
          <button
            onClick={handleConfirm}
            disabled={busy || preview.matched.length === 0}
            className="w-full py-1.5 bg-accent text-white rounded font-semibold"
          >
            Add to Instamart cart
          </button>
        </>
      )}

      {message && (
        <p className={message.ok ? "text-accent" : "text-red-500"}>
          {message.text}
        </p>
      )}
      <button onClick={onClose} className="underline text-slate-500">
        Close
      </button>
    </div>
  );
};
