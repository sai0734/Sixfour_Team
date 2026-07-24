import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/checklists`;

export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (checklist) => {
  const res = await jwtAxios.post(`${prefix}/`, checklist);

  return res.data;
};

export const putOne = async (checklist) => {
  const res = await jwtAxios.put(
    `${prefix}/${checklist.checklistId}`,
    checklist,
  );

  return res.data;
};

export const deleteOne = async (checklistId) => {
  const res = await jwtAxios.delete(`${prefix}/${checklistId}`);

  return res.data;
};
