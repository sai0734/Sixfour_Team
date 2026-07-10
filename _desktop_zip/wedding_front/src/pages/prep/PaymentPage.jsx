import PrepLayout from "../../layouts/PrepLayout";
import HallPaymentTab from "../../components/hallpayment/HallPaymentTab";

const PaymentPage = () => {
  return (
    <PrepLayout
      eyebrow="HALL PAYMENT"
      title="납부 관리"
      subtitle="웨딩홀 계약금·잔금 납부 현황을 관리하세요"
    >
      <HallPaymentTab />
    </PrepLayout>
  );
};

export default PaymentPage;
