import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/admin/members`;

export const getMemberList = async (pageParam) => {
  const { page, size, keyword, status } = pageParam;

  const res = await jwtAxios.get(`${host}`, {
    params: { page, size, keyword, status },
  });

  return res.data;
};

export const suspendMember = async (email, reason, suspendDays) => {
  const res = await jwtAxios.put(
    `${host}/${encodeURIComponent(email)}/status`,
    { status: "BLACKLIST", reason, suspendDays },
  );

  return res.data;
};

export const reactivateMember = async (email) => {
  const res = await jwtAxios.put(
    `${host}/${encodeURIComponent(email)}/status`,
    { status: "ACTIVE" },
  );

  return res.data;
};

export const changeMemberRole = async (email, role) => {
  const res = await jwtAxios.put(
    `${host}/${encodeURIComponent(email)}/role`,
    { role },
  );

  return res.data;
};

export const getAdminList = async () => {
  const res = await jwtAxios.get(`${host}/admins`);

  return res.data;
};
