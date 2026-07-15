// 재원 추가 - 업체 카테고리(HALL/DRESS/MAKEUP/STUDIO)별 "선택 가능한 옵션" 목록을 만드는 공용 로직.
// 원래 ReservationReserveComponent.jsx 안에 있던 buildOptions를 그대로 옮겨온 것.
// 예약(결제) 화면과, 업체 찜하기(옵션 선택) 양쪽에서 같은 옵션 목록을 써야 해서 분리함.

export const categoryLabel = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

export const packageTypeLabel = {
  HAIR: "헤어 패키지",
  MAKEUP: "메이크업 패키지",
  NAIL: "네일 패키지",
  HAIR_MAKEUP: "헤어+메이크업 패키지",
  HAIR_NAIL: "헤어+네일 패키지",
  MAKEUP_NAIL: "메이크업+네일 패키지",
  FULL: "풀 패키지",
};

// 업체 카테고리별로 선택 가능한 "옵션" 목록을 만들어주는 함수
export const buildCompanyOptions = (company) => {
  if (!company) return [];

  if (company.category === "HALL") {
    const items =
      company.hallDetail?.items || company.hallDetail?.hallItems || [];
    return items.map((item, idx) => ({
      key: `hall-${idx}`,
      label: item.itemName || `홀 옵션 ${idx + 1}`,
      detail: [
        item.capacity ? `${Number(item.capacity).toLocaleString()}명` : null,
        item.mealType || null,
      ]
        .filter(Boolean)
        .join(" · "),
      price: Number(item.price || 0),
      // 재원 추가 - 홀도 아이템별 사진이 있어서, 찜할 때 이 옵션 이미지를 같이 저장
      // (아이템 상세 "크게 보기" 모달의 찜하기 버튼과 동일한 이미지가 저장되도록)
      image: item.imageUrl || null,
    }));
  }

  if (company.category === "DRESS") {
    const items =
      company.dressDetail?.items || company.dressDetail?.dressItems || [];
    return items.map((item, idx) => ({
      key: `dress-${idx}`,
      label: item.itemName || `드레스 옵션 ${idx + 1}`,
      detail: item.sizeRange || "",
      price: Number(item.price || 0),
      // 재원 추가 - 드레스는 아이템마다 이미지가 달라서, 찜할 때 이 옵션 이미지를 같이 저장해야
      // 마이페이지 찜 목록에서 실제 고른 드레스 사진이 보임 (업체 대표사진이 아니라)
      image: item.imageUrl || null,
    }));
  }

  if (company.category === "MAKEUP") {
    const detail = company.makeupDetail;
    const packages = (detail?.packages || []).filter(Boolean);

    const services = [
      { key: "hair", label: "헤어", price: Number(detail?.hairPrice || 0) },
      {
        key: "makeup",
        label: "메이크업",
        price: Number(detail?.makeupPrice || 0),
      },
      { key: "nail", label: "네일", price: Number(detail?.nailPrice || 0) },
    ].filter((service) => service.price > 0);

    if (services.length === 0) return [];

    // 할인율은 백엔드 더미데이터의 TWO/THREE 타입과
    // 명시적인 조합 타입(HAIR_MAKEUP 등)을 모두 지원한다.
    const normalizeRate = (value) => {
      const rate = Number(value || 0);
      return rate > 1 ? rate / 100 : rate;
    };

    const packageRateMap = new Map(
      packages.map((pkg) => [pkg.packageType, normalizeRate(pkg.discountRate)]),
    );

    const getPairRate = (firstKey, secondKey) => {
      const explicitType = {
        "hair|makeup": "HAIR_MAKEUP",
        "hair|nail": "HAIR_NAIL",
        "makeup|nail": "MAKEUP_NAIL",
      }[[firstKey, secondKey].sort().join("|")];

      return packageRateMap.get(explicitType) ?? packageRateMap.get("TWO") ?? 0;
    };

    const getTripleRate = () =>
      packageRateMap.get("FULL") ?? packageRateMap.get("THREE") ?? 0;

    const applyDiscount = (basePrice, discountRate) =>
      discountRate > 0 ? Math.round(basePrice * (1 - discountRate)) : basePrice;

    const options = [];

    // 1) 실제 취급하는 서비스는 각각 단품으로 항상 선택 가능
    services.forEach((service) => {
      options.push({
        key: `makeup-single-${service.key}`,
        label: service.label,
        detail: "단품",
        price: service.price,
      });
    });

    // 2) 실제 취급 서비스 중 가능한 모든 2개 조합 생성
    for (let i = 0; i < services.length; i += 1) {
      for (let j = i + 1; j < services.length; j += 1) {
        const first = services[i];
        const second = services[j];
        const discountRate = getPairRate(first.key, second.key);
        const basePrice = first.price + second.price;

        options.push({
          key: `makeup-pair-${first.key}-${second.key}`,
          label: `${first.label}+${second.label} 패키지`,
          detail:
            discountRate > 0
              ? `${Math.round(discountRate * 100)}% 할인 적용가`
              : "",
          price: applyDiscount(basePrice, discountRate),
        });
      }
    }

    // 3) 3개를 모두 취급하는 업체는 전체 패키지도 생성
    if (services.length === 3) {
      const discountRate = getTripleRate();
      const basePrice = services.reduce(
        (sum, service) => sum + service.price,
        0,
      );

      options.push({
        key: "makeup-triple-hair-makeup-nail",
        label: `${services.map((service) => service.label).join("+")} 패키지`,
        detail:
          discountRate > 0
            ? `${Math.round(discountRate * 100)}% 할인 적용가`
            : "",
        price: applyDiscount(basePrice, discountRate),
      });
    }

    return options;
  }

  // STUDIO 등 별도 아이템 가격이 없는 카테고리 - 대표 가격(priceAvg) 하나로 대체
  if (company.priceAvg) {
    return [
      {
        key: "base",
        label: `${categoryLabel[company.category] || "기본"} 촬영/이용`,
        detail: "",
        price: Number(company.priceAvg),
      },
    ];
  }

  return [];
};
