// 배송/교환/환불 정책 안내 (항상 펼쳐진 상태로 표시)
// 실제 배송업체/처리기간/비용 정책에 맞게 아래 content 문구를 교체해서 사용
const ShippingPolicyComponent = () => {
  const sections = [
    {
      key: "shipping",
      title: "배송 안내",
      content:
        "결제 완료 후 평균 2~3일 이내 출고되며, 출고 후 1~2일 내 도착 예정입니다. 도서/산간 지역은 추가 배송일이 소요될 수 있습니다.",
    },
    {
      key: "exchange",
      title: "교환 안내",
      content:
        "상품 수령일로부터 7일 이내, 상품과 포장 상태가 훼손되지 않은 경우 교환 신청이 가능합니다. 단순 변심에 의한 교환은 왕복 배송비가 발생할 수 있습니다.",
    },
    {
      key: "refund",
      title: "반품/환불 안내",
      content:
        "상품 수령일로부터 7일 이내 반품 신청이 가능하며, 상품 확인 후 3~5영업일 이내 환불이 진행됩니다. 상품 하자의 경우 배송비는 판매자가 부담합니다.",
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      {sections.map((sec) => (
        <div key={sec.key}>
          <p className="text-sm font-medium mb-2">{sec.title}</p>
          <p className="text-sm text-ink-soft whitespace-pre-line">
            {sec.content}
          </p>
        </div>
      ))}
    </div>
  );
};

export default ShippingPolicyComponent;
