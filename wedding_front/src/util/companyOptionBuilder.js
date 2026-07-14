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

    if (packages.length > 0) {
      const priceMap = {
        hair: Number(detail.hairPrice || 0),
        makeup: Number(detail.makeupPrice || 0),
        nail: Number(detail.nailPrice || 0),
      };
      const keysByType = {
        HAIR: ["hair"],
        MAKEUP: ["makeup"],
        NAIL: ["nail"],
        HAIR_MAKEUP: ["hair", "makeup"],
        HAIR_NAIL: ["hair", "nail"],
        MAKEUP_NAIL: ["makeup", "nail"],
        FULL: ["hair", "makeup", "nail"],
      };
      return packages.map((pkg, idx) => {
        const keys = keysByType[pkg.packageType] || [];
        const basePrice = keys.reduce((sum, k) => sum + (priceMap[k] || 0), 0);
        const rate = Number(pkg.discountRate || 0);
        const discountRate = rate > 1 ? rate / 100 : rate;
        const finalPrice =
          basePrice > 0 && discountRate > 0
            ? Math.round(basePrice * (1 - discountRate))
            : basePrice;
        return {
          key: `makeup-pkg-${idx}`,
          label:
            packageTypeLabel[pkg.packageType] ||
            pkg.packageType ||
            `패키지 ${idx + 1}`,
          detail:
            discountRate > 0
              ? `${Math.round(discountRate * 100)}% 할인 적용가`
              : "",
          price: finalPrice,
        };
      });
    }

    // 패키지가 없으면 단품 가격이라도 노출
    const single = [];
    if (detail?.hairPrice)
      single.push({
        key: "hair",
        label: "헤어",
        price: Number(detail.hairPrice),
      });
    if (detail?.makeupPrice)
      single.push({
        key: "makeup",
        label: "메이크업",
        price: Number(detail.makeupPrice),
      });
    if (detail?.nailPrice)
      single.push({
        key: "nail",
        label: "네일",
        price: Number(detail.nailPrice),
      });
    return single.map((s) => ({ ...s, key: `makeup-${s.key}`, detail: "" }));
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
