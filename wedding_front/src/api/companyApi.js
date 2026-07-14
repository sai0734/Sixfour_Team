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

  if (![...formData.keys()].length) {
    return [];
  }

  const res = await jwtAxios.post(`${imageHost}/upload`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

  return res.data.uploadFileNames || [];
};

export const getCompanyImageUrl = (fileName, thumbnail = false) => {
  if (!fileName) {
    return "";
  }

  const normalizedFileName = String(fileName).trim();

  if (/^(https?:)?\/\//.test(normalizedFileName)) {
    return normalizedFileName;
  }

  if (normalizedFileName.startsWith("/api/")) {
    return `${API_SERVER_HOST}${normalizedFileName}`;
  }

  if (normalizedFileName.startsWith("/")) {
    return `${API_SERVER_HOST}${normalizedFileName}`;
  }

  const viewFileName =
    thumbnail && !normalizedFileName.startsWith("s_")
      ? `s_${normalizedFileName}`
      : normalizedFileName;

  return `${imageHost}/view/${encodeURIComponent(viewFileName)}`;
};

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

  if (category) {
    list = list.filter((company) => company.category === category);
  }

  if (keyword) {
    const loweredKeyword = keyword.toLowerCase();
    list = list.filter((company) =>
      [company.name, company.address, company.phone, company.category].some(
        (value) =>
          String(value || "")
            .toLowerCase()
            .includes(loweredKeyword),
      ),
    );
  }

  if (minPrice) {
    list = list.filter(
      (company) => Number(company.priceAvg || 0) >= Number(minPrice),
    );
  }

  if (maxPrice) {
    list = list.filter(
      (company) => Number(company.priceAvg || 0) <= Number(maxPrice),
    );
  }

  list = sortCompanies(list, sort);

  const totalCount = list.length;
  const start = (page - 1) * size;
  const dtoList = list.slice(start, start + size);
  const totalPage = Math.max(1, Math.ceil(totalCount / size));
  const pageNumList = Array.from(
    { length: totalPage },
    (_, index) => index + 1,
  );

  return {
    dtoList,
    pageNumList,
    pageRequestDTO: { page, size },
    prev: page > 1,
    next: page < totalPage,
    totalCount,
    prevPage: Math.max(1, page - 1),
    nextPage: Math.min(totalPage, page + 1),
    totalPage,
    current: page,
  };
};

export const getDummyOne = async (cmno) => {
  const res = await axios.get(`${host}/dummy`);
  const company = normalizeDummyCompanies(res.data).find(
    (item) => String(item.cmno) === String(cmno),
  );

  if (!company) {
    throw new Error(`Dummy company not found. cmno=${cmno}`);
  }

  return company;
};

const normalizeDummyCompanies = (data) => {
  const list = Array.isArray(data)
    ? data
    : data?.companies || data?.dtoList || data?.list || [];

  return list
    .filter(
      (item) =>
        item &&
        (item.cmno || item.companyId || item.id) &&
        (item.name || item.companyName || item.vendorName),
    )
    .map(normalizeCompany);
};

const normalizeCompany = (company = {}) => {
  const imageList =
    company.imageList || company.images || company.companyImages || [];
  const uploadFileNames =
    company.uploadFileNames ||
    imageList
      .map((image) =>
        typeof image === "string"
          ? image
          : image.fileName || image.fileUrl || image.url,
      )
      .filter(Boolean) ||
    [];

  return {
    ...company,
    cmno: company.cmno || company.companyId || company.id,
    category: company.category || company.companyCategory || company.type,
    name:
      company.name ||
      company.companyName ||
      company.vendorName ||
      company.businessName ||
      "",
    ceoName:
      company.ceoName || company.representative || company.ownerName || "",
    phone: company.phone || company.tel || company.contact || "",
    address: company.address || company.addr || "",
    description: company.description || company.desc || "",
    priceAvg: company.priceAvg || company.averagePrice || company.price || 0,
    uploadFileNames,
    mainImage: company.mainImage || uploadFileNames[0] || "",
  };
};

const sortCompanies = (list, sort) => {
  const copiedList = [...list];

  if (sort === "name") {
    return copiedList.sort((a, b) =>
      String(a.name || "").localeCompare(String(b.name || "")),
    );
  }

  if (sort === "price") {
    return copiedList.sort(
      (a, b) => Number(a.priceAvg || 0) - Number(b.priceAvg || 0),
    );
  }

  return copiedList.sort((a, b) => Number(b.cmno || 0) - Number(a.cmno || 0));
};

export const updateMakeupDetail = async (cmno, dto) => {
  const res = await jwtAxios.put(`${host}/makeup/${cmno}`, dto);
  return res.data;
};

export const updateDressDetail = async (cmno, dto) => {
  const res = await jwtAxios.put(`${host}/dresses/${cmno}`, dto);
  return res.data;
};

// ===== 업체 문의 담당자 임명 (관리자 전용) =====

export const assignCompanyManager = async (cmno, managerEmail) => {
  const res = await jwtAxios.put(`${host}/${cmno}/manager`, { managerEmail });
  return res.data;
};

export const unassignCompanyManager = async (cmno) => {
  const res = await jwtAxios.delete(`${host}/${cmno}/manager`);
  return res.data;
};

// 로그인한 회원 본인이 담당하고 있는 업체가 있는지 확인 (없으면 isManager: false)
// 백엔드는 JWT 토큰의 이메일만 사용 — email 파라미터 불필요
export const getMyManagedCompany = async () => {
  const res = await jwtAxios.get(`${host}/my-managed`);
  return res.data;
};

// 관리자 - 담당자 지정된 업체 전체 목록 (회원관리 "담당자 탭"용)
export const getManagedCompanies = async () => {
  const res = await jwtAxios.get(`${host}/managers`);
  return res.data;
};

export const updateMemberRole = async (email, role) => {
  const res = await jwtAxios.put(
    `${API_SERVER_HOST}/api/members/role/${email}`,
    { role },
  );
  return res.data;
};
