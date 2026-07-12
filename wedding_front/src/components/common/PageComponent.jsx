const PageComponent = ({ serverData, movePage }) => {
  return (
    <div className="my-6 flex justify-center items-center gap-1.5">
      {serverData.prev ? (
        <button
          onClick={() => movePage({ page: serverData.prevPage })}
          className="w-8 h-8 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-blush-100"
        >
          ‹
        </button>
      ) : (
        <span className="w-8 h-8 rounded-full border border-line-soft text-xs text-line-soft opacity-30 flex items-center justify-center cursor-not-allowed">
          ‹
        </span>
      )}

      {serverData.pageNumList.map((pageNum) => (
        <button
          key={pageNum}
          onClick={() => movePage({ page: pageNum })}
          className={`w-8 h-8 rounded-full text-xs font-medium ${
            serverData.current === pageNum
              ? "bg-brand text-white"
              : "text-ink-soft hover:bg-blush-100"
          }`}
        >
          {pageNum}
        </button>
      ))}

      {serverData.next ? (
        <button
          onClick={() => movePage({ page: serverData.nextPage })}
          className="w-8 h-8 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-blush-100"
        >
          ›
        </button>
      ) : (
        <span className="w-8 h-8 rounded-full border border-line-soft text-xs text-line-soft opacity-30 flex items-center justify-center cursor-not-allowed">
          ›
        </span>
      )}
    </div>
  );
};

export default PageComponent;
