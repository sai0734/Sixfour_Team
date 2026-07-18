// AiChatbotModal의 버튼 메뉴 데이터 + 안내 문구.
// 컴포넌트 파일(AiChatbotModal.jsx)에서 컴포넌트가 아닌 값을 같이 export하면
// Vite Fast Refresh가 깨지기 때문에 별도 파일로 분리했다.

// 챗봇을 열면(또는 메뉴로 돌아가면) 보여줄 버튼 메뉴. key가 null인 항목(자유롭게 질문하기)만
// AI가 여러 함수 중 자유 판단하는 모드로 이어진다. 예약 가능일 확인은 "업체 찾기" 안에서 같이 처리한다.
export const INTENT_OPTIONS = [
  { key: "SEARCH_COMPANIES", label: "🏛️ 업체 찾기" },
  { key: "SEARCH_GIFTS", label: "🎁 답례품 찾기" },
  { key: "FIND_SIMILAR_DRESS", label: "📷 드레스 사진으로 추천받기" },
  { key: null, label: "💬 자유롭게 질문하기" },
];

// 버튼을 눌렀을 때 채팅창에 assistant 말풍선으로 바로 보여줄 안내 문구 (서버 호출 없이 클라이언트에서 즉시 표시)
export const getIntentGuideMessage = (intentKey) => {
  switch (intentKey) {
    case "SEARCH_COMPANIES":
      return "어떤 종류의 업체를 찾으세요? 아래에서 골라주세요 🙂";
    case "SEARCH_GIFTS":
      return "어떤 답례품을 찾으시나요? 원하시는 종류나 가격대, 느낌을 말씀해주시면 잘 어울리는 걸로 찾아드릴게요!";
    case "FIND_SIMILAR_DRESS":
      return "마음에 드는 드레스 사진을 올려주세요! 비슷한 스타일의 드레스를 찾아드릴게요 📷";
    default:
      return "편하게 물어보세요! 결혼 준비에 관한 궁금한 점 뭐든 도와드릴게요 :)";
  }
};

// "업체 찾기" 선택 후 보여줄 카테고리 버튼 - 선택한 카테고리로 그 세션 내내 고정된다
export const COMPANY_CATEGORY_OPTIONS = [
  { key: "HALL", label: "🏛️ 웨딩홀" },
  { key: "DRESS", label: "👗 드레스" },
  { key: "STUDIO", label: "📸 스튜디오" },
  { key: "MAKEUP", label: "💄 메이크업" },
];

// 카테고리 버튼을 눌렀을 때 보여줄 안내 문구
export const getCompanyCategoryGuideMessage = (categoryKey) => {
  switch (categoryKey) {
    case "HALL":
      return "웨딩홀 관련해서 편하게 물어보세요! 지역, 예산, 날짜, 식사 옵션 등 뭐든 좋아요 🏛️";
    case "DRESS":
      return "드레스 관련해서 편하게 물어보세요! 스타일, 지역, 예산, 슈트 취급 여부 등 뭐든 좋아요 👗";
    case "STUDIO":
      return "스튜디오 관련해서 편하게 물어보세요! 원하는 분위기나 지역, 예산을 말씀해주세요 📸";
    case "MAKEUP":
      return "메이크업 관련해서 편하게 물어보세요! 원하는 패키지나 지역, 예산을 말씀해주세요 💄";
    default:
      return "편하게 물어보세요!";
  }
};
