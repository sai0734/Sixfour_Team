import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { getMyManagedCompany } from "../api/companyApi";

const emptyResult = {
  isManager: false,
  company: null,
};

// 같은 이메일로 동시에 여러 컴포넌트가 호출해도 API는 1번만
const cache = new Map();

const fetchManagedCompany = (email) => {
  if (!email) {
    return Promise.resolve(emptyResult);
  }

  if (cache.has(email)) {
    return cache.get(email);
  }

  const request = getMyManagedCompany()
    .then((data) => {
      const isManager = Boolean(data?.isManager);
      return {
        isManager,
        company: isManager && data?.company ? data.company : null,
      };
    })
    .catch(() => emptyResult);

  cache.set(email, request);
  return request;
};

const useManagedCompany = ({ enabled = true } = {}) => {
  const email = useSelector((state) => state.loginSlice?.email);
  const shouldFetch = enabled && Boolean(email);

  const [loading, setLoading] = useState(shouldFetch);
  const [isManager, setIsManager] = useState(false);
  const [company, setCompany] = useState(null);

  useEffect(() => {
    if (!shouldFetch) {
      setIsManager(false);
      setCompany(null);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    fetchManagedCompany(email).then((result) => {
      if (cancelled) return;
      setIsManager(result.isManager);
      setCompany(result.company);
      setLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, [email, shouldFetch]);

  return {
    isManager,
    company,
    loading,
    checked: !shouldFetch || !loading,
  };
};

export default useManagedCompany;
