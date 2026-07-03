import jwtAxios from "../util/jwtUtil";

//서버 주소
export const API_SERVER_HOST = "http://localhost:8080";

const prefix = `${API_SERVER_HOST}/api/reservations`;

export const getOne = async (reservationId) => {
  const res = await jwtAxios.get(`${prefix}/${reservationId}`);

  return res.data;
};

export const getList = async (pageParam) => {
  const { page, size } = pageParam;

  const res = await jwtAxios.get(`${prefix}/list`, {
    params: { page: page, size: size },
  });

  return res.data;
};

export const postAdd = async (reservationObj) => {
  const res = await jwtAxios.post(`${prefix}/`, reservationObj);

  return res.data;
};

export const deleteOne = async (reservationId) => {
  const res = await jwtAxios.delete(`${prefix}/${reservationId}`);

  return res.data;
};

export const putOne = async (reservation) => {
  const res = await jwtAxios.put(`${prefix}/${reservation.reservationId}`, reservation);

  return res.data;
};
