import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/admin/products`;

// 관리자용 상품 리스트 조회
export const getAdminProductList = async ({
  page,
  size,
  keyword,
  category,
  saleStatus,
  sortType,
}) => {
  const res = await jwtAxios.get(host, {
    params: { page, size, keyword, category, saleStatus, sortType },
  });
  return res.data;
};
