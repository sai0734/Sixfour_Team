import { Link } from "react-router-dom";
import { getKakaoLoginLink } from "../../api/kakaoAuthApi";

const KakaoLoginComponent = () => {
  const link = getKakaoLoginLink();

  return (
    <Link
      to={link}
      className="w-full flex items-center justify-center gap-2 py-3 rounded-full bg-[#FEE500] text-[#3C1E1E] font-semibold hover:brightness-95 transition font-body"
    >
      카카오로 로그인
    </Link>
  );
};

export default KakaoLoginComponent;
