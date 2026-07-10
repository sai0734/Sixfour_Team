import jwtAxios from "../util/jwtUtil";

// 서버 주소 - 다른 api 파일들도 이 값을 가져다 씀, 이름/위치 바꾸지 말 것
export const API_SERVER_HOST = "http://localhost:8080";

const prefix = `${API_SERVER_HOST}/api/reservations`;

export const getOne = async (reservationId) => {
  const res = await jwtAxios.get(`${prefix}/${reservationId}`);

  return res.data;
};

export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (reservation) => {
  const res = await jwtAxios.post(`${prefix}/`, reservation);

  return res.data;
};

export const putOne = async (reservation) => {
  const res = await jwtAxios.put(
    `${prefix}/${reservation.reservationId}`,
    reservation,
  );

  return res.data;
};

export const deleteOne = async (reservationId) => {
  const res = await jwtAxios.delete(`${prefix}/${reservationId}`);

  return res.data;
};
