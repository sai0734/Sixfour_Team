const TONE_STYLES = {
  light: { background: "#FFE2E2", color: "#C06080" },
  white: { background: "#FFFFFF", color: "#C06080" },
  lavender: { background: "#DDD3E8", color: "#6B5A8A" },
};

const ShopTapeLabel = ({
  children,
  tone = "light",
  rotate = -2,
  className = "",
}) => {
  const style = TONE_STYLES[tone] || TONE_STYLES.light;

  return (
    <div
      className={`relative inline-block ${className}`}
      style={{ transform: `rotate(${rotate}deg)` }}
    >
      <span
        className="absolute -top-1.5 left-1/2 -translate-x-1/2 w-2.5 h-2.5 rounded-full z-10"
        style={{
          background: "#C87070",
          boxShadow: "0 1.5px 4px rgba(0,0,0,0.35)",
        }}
      />
      <span
        className="block font-['Gaegu'] text-lg font-bold px-4 py-2 rounded-sm shadow-[0_6px_14px_-6px_rgba(58,54,47,0.4)] whitespace-nowrap"
        style={style}
      >
        {children}
      </span>
    </div>
  );
};

export default ShopTapeLabel;
