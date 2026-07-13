import { useEffect, useRef, useState } from "react";

const DRAG_THRESHOLD = 5;

const HorizontalDragScroll = ({ children, className = "" }) => {
  const scrollRef = useRef(null);
  const dragState = useRef({
    active: false,
    dragging: false,
    startX: 0,
    startLeft: 0,
    pointerId: null,
  });
  const suppressClickRef = useRef(false);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  const [dragging, setDragging] = useState(false);

  const updateScrollState = () => {
    const el = scrollRef.current;
    if (!el) return;
    const maxLeft = Math.max(0, el.scrollWidth - el.clientWidth);
    setCanScrollLeft(el.scrollLeft > 2);
    setCanScrollRight(el.scrollLeft < maxLeft - 2);
  };

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return undefined;

    updateScrollState();
    const observer = new ResizeObserver(updateScrollState);
    observer.observe(el);
    window.addEventListener("resize", updateScrollState);

    return () => {
      observer.disconnect();
      window.removeEventListener("resize", updateScrollState);
    };
  }, [children]);

  const scrollByAmount = (direction) => {
    const el = scrollRef.current;
    if (!el) return;
    el.scrollBy({
      left: direction * Math.max(180, el.clientWidth * 0.7),
      behavior: "smooth",
    });
  };

  const handlePointerDown = (event) => {
    if (event.pointerType === "touch" || event.button !== 0) return;

    const el = scrollRef.current;
    if (!el) return;

    dragState.current = {
      active: true,
      dragging: false,
      startX: event.clientX,
      startLeft: el.scrollLeft,
      pointerId: event.pointerId,
    };
    suppressClickRef.current = false;
  };

  const handlePointerMove = (event) => {
    const state = dragState.current;
    if (!state.active) return;

    const el = scrollRef.current;
    if (!el) return;

    const deltaX = event.clientX - state.startX;

    if (!state.dragging && Math.abs(deltaX) < DRAG_THRESHOLD) {
      return;
    }

    if (!state.dragging) {
      state.dragging = true;
      suppressClickRef.current = true;
      setDragging(true);
      el.setPointerCapture?.(event.pointerId);
    }

    event.preventDefault();
    el.scrollLeft = state.startLeft - deltaX;
  };

  const endDrag = (event) => {
    const el = scrollRef.current;
    const wasDragging = dragState.current.dragging;

    dragState.current.active = false;
    dragState.current.dragging = false;
    setDragging(false);

    if (wasDragging && el?.hasPointerCapture?.(event.pointerId)) {
      el.releasePointerCapture(event.pointerId);
    }

    if (wasDragging) {
      window.setTimeout(() => {
        suppressClickRef.current = false;
      }, 0);
    }
  };

  const handleClickCapture = (event) => {
    if (!suppressClickRef.current) return;
    event.preventDefault();
    event.stopPropagation();
  };

  return (
    <div className="relative min-w-0">
      {canScrollLeft && (
        <button
          type="button"
          aria-label="왼쪽 필터 보기"
          onClick={() => scrollByAmount(-1)}
          className="absolute left-1 top-[42%] z-20 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full border border-[#DED7CB] bg-white/95 text-sm text-[#5C6B4F] shadow-md lg:hidden"
        >
          ‹
        </button>
      )}

      <div
        ref={scrollRef}
        onScroll={updateScrollState}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
        onPointerLeave={(event) => dragState.current.active && endDrag(event)}
        onClickCapture={handleClickCapture}
        className={`flex gap-2 overflow-x-auto pb-3 select-none touch-pan-x lg:flex-col lg:gap-1 lg:overflow-visible lg:pb-0 ${
          dragging ? "cursor-grabbing" : "cursor-grab"
        } ${className}`}
        style={{
          scrollbarWidth: "thin",
          scrollbarColor: "#9EAA92 #EEE8DE",
          WebkitOverflowScrolling: "touch",
        }}
      >
        {children}
      </div>

      {canScrollRight && (
        <button
          type="button"
          aria-label="오른쪽 필터 보기"
          onClick={() => scrollByAmount(1)}
          className="absolute right-1 top-[42%] z-20 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full border border-[#DED7CB] bg-white/95 text-sm text-[#5C6B4F] shadow-md lg:hidden"
        >
          ›
        </button>
      )}

      <style>{`
        @media (max-width: 1023px) {
          div[style*="scrollbar-color"]::-webkit-scrollbar { height: 6px; }
          div[style*="scrollbar-color"]::-webkit-scrollbar-track {
            background: #EEE8DE;
            border-radius: 999px;
          }
          div[style*="scrollbar-color"]::-webkit-scrollbar-thumb {
            background: #9EAA92;
            border-radius: 999px;
          }
        }
      `}</style>
    </div>
  );
};

export default HorizontalDragScroll;
