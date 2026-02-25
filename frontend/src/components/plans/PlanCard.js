export default function PlanCard({
  title,
  priceLabel,
  badge,
  features,
  ctaLabel,
  ctaDisabled,
  onCta,
}) {
  return (
    <div style={styles.card}>
      <div style={styles.top}>
        <div>
          <h2 style={{ margin: 0 }}>{title}</h2>
          <div style={styles.price}>{priceLabel}</div>
        </div>
        {badge ? <span style={styles.badge}>{badge}</span> : null}
      </div>

      <ul style={styles.list}>
        {(features || []).map((f, idx) => (
          <li key={idx} style={styles.item}>
            {f}
          </li>
        ))}
      </ul>

      <button style={styles.button} disabled={ctaDisabled} onClick={onCta}>
        {ctaLabel}
      </button>
    </div>
  );
}

const styles = {
  card: {
    border: "1px solid #ddd",
    borderRadius: 12,
    padding: 16,
    background: "#fff",
  },
  top: { display: "flex", justifyContent: "space-between", gap: 12 },
  price: { marginTop: 6, fontWeight: 600 },
  badge: {
    fontSize: 12,
    padding: "4px 8px",
    borderRadius: 999,
    border: "1px solid #ccc",
    height: "fit-content",
  },
  list: { marginTop: 12, marginBottom: 12, paddingLeft: 18 },
  item: { marginBottom: 6 },
  button: {
    width: "100%",
    padding: "10px 12px",
    borderRadius: 10,
    border: "1px solid #ccc",
    cursor: "pointer",
  },
};