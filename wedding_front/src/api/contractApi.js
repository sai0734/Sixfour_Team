import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

export const reviewContractPost = async (file) => {
  const formData = new FormData();
  formData.append("file", file);

  const res = await jwtAxios.post(
    `${API_SERVER_HOST}/api/contract/review`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );

  return res.data;
};
