import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/coupleprofiles`;

export const getByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const listOthers = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/others/${memberEmail}`);

  return res.data;
};

export const postAdd = async (profile) => {
  const res = await jwtAxios.post(`${prefix}/`, profile);

  return res.data;
};

export const putOne = async (profile) => {
  const res = await jwtAxios.put(`${prefix}/${profile.profileId}`, profile);

  return res.data;
};

export const deleteOne = async (profileId) => {
  const res = await jwtAxios.delete(`${prefix}/${profileId}`);

  return res.data;
};
