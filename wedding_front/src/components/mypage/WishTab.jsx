import { useEffect, useState } from "react";
import { getListByMember, postAdd, deleteOne } from "../../api/companywishApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import WishFormModal from "./WishFormModal";

const WishTab = () => {
  const { loginState } = useCustomLogin();

  const [wishList, setWishList] = useState([]);
  const [refresh, setRefresh] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    if (!loginState.email) return;

    getListByMember(loginState.email).then((data) => setWishList(data));
  }, [loginState.email, refresh]);

  const handleAdd = (cmno) => {
    postAdd({ memberEmail: loginState.email, cmno })
      .then(() => {
        setModalOpen(false);
        setRefresh(!refresh);
      })
      .catch((e) => {
        console.error(e);
        alert("찜 등록에 실패했습니다. 이미 찜한 업체일 수 있습니다.");
      });
  };

  const handleDelete = (wishId) => {
    deleteOne(wishId)
      .then(() => setRefresh(!refresh))
      .catch((e) => console.error(e));
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <p className="text-sm text-ink-muted">찜한 업체 {wishList.length}곳</p>
        <button
          type="button"
          onClick={() => setModalOpen(true)}
          className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
        >
          + 업체 찜하기
        </button>
      </div>

      {wishList.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          찜한 업체가 없습니다.
        </div>
      )}

      <div className="grid grid-cols-3 gap-4">
        {wishList.map((item) => (
          <div
            key={item.wishId}
            className="bg-white rounded-2xl border border-line p-5"
          >
            <div className="aspect-square rounded-xl bg-surface flex items-center justify-center mb-3">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="#D4537E"
                strokeWidth="1.5"
                className="w-7 h-7 opacity-60"
              >
                <path d="M19.5 12.572 12 20l-7.5-7.428a5 5 0 1 1 7.5-6.566 5 5 0 1 1 7.5 6.566Z" />
              </svg>
            </div>

            <p className="text-sm font-medium text-ink mb-1">
              업체 번호 #{item.cmno}
            </p>
            <p className="text-xs text-ink-muted mb-4">{item.regDate}</p>

            <button
              type="button"
              onClick={() => handleDelete(item.wishId)}
              className="w-full h-9 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
            >
              찜 취소
            </button>
          </div>
        ))}
      </div>

      {modalOpen && (
        <WishFormModal
          onSubmit={handleAdd}
          onClose={() => setModalOpen(false)}
        />
      )}
    </div>
  );
};

export default WishTab;
