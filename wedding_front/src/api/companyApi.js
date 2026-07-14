import jwtAxios from "../util/jwtUtil";
import axios from "axios";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/companies`;
const imageHost = `${host}/images`;

export const getList = async (pageParam = {}) => {
  const {
    page = 1,
    size = 10,
    category,
    keyword,
    sort,
    minPrice,
    maxPrice,
  } = pageParam;

  try {
    return await getDummyList({
      page,
      size,
      category,
      keyword,
      sort,
      minPrice,
      maxPrice,
    });
  } catch (err) {
    console.warn("Company dummy list API failed. Try DB company list.", err);
  }

  const res = await axios.get(`${host}/list`, {
    params: { page, size, category, keyword, sort, minPrice, maxPrice },
  });

  return {
    ...res.data,
    dtoList: (res.data?.dtoList || []).map(normalizeCompany),
  };
};

export const getOne = async (cmno) => {
  try {
    return await getDummyOne(cmno);
  } catch (err) {
    console.warn("Company dummy read API failed. Try DB company read.", err);
  }

  const res = await axios.get(`${host}/${cmno}`);
  return normalizeCompany(res.data);
};

export const postAdd = async (company) => {
  const res = await jwtAxios.post(`${host}/`, company);
  return res.data;
};

export const putOne = async (cmno, company) => {
  const res = await jwtAxios.put(`${host}/${cmno}`, company);
  return res.data;
};

export const deleteOne = async (cmno) => {
  const res = await jwtAxios.delete(`${host}/${cmno}`);
  return res.data;
};

export const uploadCompanyImages = async (files) => {
  const formData = new FormData();
  Array.from(files || []).forEach((file) => {
    formData.append("files", file);
  });

  if (![...formData.keys()].length) return [];

  const res = await jwtAxios.post(`${imageHost}/upload`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.uploadFileNames || [];
};

export const getCompanyImageUrl = (fileName, thumbnail = false) => {
  if (!fileName) return "";
  const normalizedFileName = String(fileName).trim();
  if (/^(https?:)?\/\//.test(normalizedFileName)) return normalizedFileName;
  if (normalizedFileName.startsWith("/api/"))
    return `${API_SERVER_HOST}${normalizedFileName}`;
  if (normalizedFileName.startsWith("/"))
    return `${API_SERVER_HOST}${normalizedFileName}`;

  const viewFileName =
    thumbnail && !normalizedFileName.startsWith("s_")
      ? `s_${normalizedFileName}`
      : normalizedFileName;
  return `${imageHost}/view/${encodeURIComponent(viewFileName)}`;
};

// ===== 관리자 및 매니저 관련 기능 =====

export const assignCompanyManager = async (cmno, managerEmail) => {
  const res = await jwtAxios.put(`${host}/${cmno}/manager`, { managerEmail });
  return res.data;
};

export const unassignCompanyManager = async (cmno) => {
  const res = await jwtAxios.delete(`${host}/${cmno}/manager`);
  return res.data;
};

export const getMyManagedCompany = async (email) => {
  const res = await jwtAxios.get(`${host}/my-managed`, { params: { email } });
  return res.data;
};

export const getManagedCompanies = async () => {
  const res = await jwtAxios.get(`${host}/managers`);
  return res.data;
};

// 수정된 부분: 컨트롤러의 @GetMapping("/managed-by")와 경로 일치
export const getManagedCompanyByEmail = async (email) => {
  const res = await jwtAxios.get(`${host}/managed-by`, { params: { email } });
  return res.data;
};

// ===== 더미 데이터 및 보조 함수들 =====

export const getDummyList = async (pageParam = {}) => {
  const {
    page = 1,
    size = 10,
    category,
    keyword,
    sort,
    minPrice,
    maxPrice,
  } = pageParam;
  const res = await axios.get(`${host}/dummy`);
  let list = normalizeDummyCompanies(res.data);
  if (category) list = list.filter((company) => company.category === category);
  if (keyword) {
    const loweredKeyword = keyword.toLowerCase();
    list = list.filter((company) =>
      [company.name, company.address, company.phone, company.category].some(
        (v) =>
          String(v || "")
            .toLowerCase()
            .includes(loweredKeyword),
      ),
    );
  }
  if (minPrice)
    list = list.filter((c) => Number(c.priceAvg || 0) >= Number(minPrice));
  if (maxPrice)
    list = list.filter((c) => Number(c.priceAvg || 0) <= Number(maxPrice));

  list = sortCompanies(list, sort);
  const totalCount = list.length;
  const start = (page - 1) * size;
  return {
    dtoList: list.slice(start, start + size),
    totalCount,
    current: page,
  };
};

export const getDummyOne = async (cmno) => {
  const res = await axios.get(`${host}/dummy`);
  const company = normalizeDummyCompanies(res.data).find(
    (item) => String(item.cmno) === String(cmno),
  );
  if (!company) throw new Error(`Dummy company not found. cmno=${cmno}`);
  return company;
};

const normalizeDummyCompanies = (data) => {
  const list = Array.isArray(data)
    ? data
    : data?.companies || data?.dtoList || data?.list || [];
  return list
    .filter((i) => i && (i.cmno || i.companyId || i.id))
    .map(normalizeCompany);
};

const normalizeCompany = (company = {}) => {
  const uploadFileNames =
    company.uploadFileNames ||
    (company.imageList || []).map((img) => img.fileName || img) ||
    [];
  return {
    ...company,
    cmno: company.cmno || company.companyId || company.id,
    name: company.name || company.companyName || "",
    uploadFileNames,
    mainImage: company.mainImage || uploadFileNames[0] || "",
  };
};

const sortCompanies = (list, sort) => {
  const copied = [...list];
  if (sort === "name")
    return copied.sort((a, b) =>
      String(a.name || "").localeCompare(String(b.name || "")),
    );
  if (sort === "price")
    return copied.sort(
      (a, b) => Number(a.priceAvg || 0) - Number(b.priceAvg || 0),
    );
  return copied.sort((a, b) => Number(b.cmno || 0) - Number(a.cmno || 0));
};

export const updateMakeupDetail = async (cmno, dto) => {
  const res = await jwtAxios.put(`${host}/makeup/${cmno}`, dto);
  return res.data;
};

export const updateDressDetail = async (cmno, dto) => {
  const res = await jwtAxios.put(`${host}/dresses/${cmno}`, dto);
  return res.data;
};
