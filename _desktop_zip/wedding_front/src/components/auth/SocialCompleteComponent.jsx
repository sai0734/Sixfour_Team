import { useState } from "react";
import { useLocation } from "react-router-dom";
import { useDispatch } from "react-redux";
import {
  completeKakaoSignupPost,
  checkNicknameAvailable,
  checkPhoneAvailable,
} from "../../api/authApi";
import { login } from "../../slices/loginSlice";
import useCustomLogin from "../../hooks/useCustomLogin";
import AuthLayout from "./AuthLayout";

const initState = {
  nickname: "",
  name: "",
  phone: "",
  birthDate: "",
  zipCode: "",
  address: "",
  addressDetail: "",
  termsAgree: false,
  privacyAgree: false,
  marketing: false,
};

const inputClass =
  "w-full px-4 py-3 rounded-xl border border-line bg-white/70 text-ink placeholder:text-ink-faint focus:border-brand focus:ring-4 focus:ring-blush-100 outline-none transition font-body";
const labelClass = "block text-sm font-semibold text-ink-soft mb-1.5 font-body";
const okMsgClass = "text-green-600 text-xs mt-1 font-body";
const errMsgClass = "text-rose-600 text-xs mt-1 font-body";

const PHONE_REGEX = /^01[0-9]-?\d{3,4}-?\d{4}$/;

const SocialCompleteComponent = () => {
  const location = useLocation();
  const dispatch = useDispatch();
  const { moveToPath } = useCustomLogin();

  // 카카오 로그인 직후 KakaoRedirectPage가 navigate()로 들려준 값.
  // 아직 로그인 전 상태라 Redux(loginSlice)에는 아무것도 없고, 이 라우터 state가 유일한 신원 증명 수단임
  const pendingToken = location.state?.pendingToken;
  const kakaoEmail = location.state?.kakaoEmail;

  const [completeParam, setCompleteParam] = useState({ ...initState });
  const [nicknameStatus, setNicknameStatus] = useState(null);
  const [phoneStatus, setPhoneStatus] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [touched, setTouched] = useState({
    nickname: false,
    name: false,
    phone: false,
    address: false,
  });

  // pendingToken 없이 이 화면에 바로 들어온 경우(새로고침으로 state 유실 등) - 진행 불가
  if (!pendingToken) {
    return (
      <AuthLayout
        eyebrow="세션 만료"
        title={
          <>
            다시
            <br />
            시도해 주세요
          </>
        }
        subtitle="카카오 로그인 정보를 찾을 수 없어요"
        stickerEmoji="😢"
      >
        <div className="max-w-sm w-full mx-auto text-center">
          <p className="text-ink-muted text-sm mb-8">
            새로고침 등으로 인증 정보가 사라졌어요. 카카오 로그인을 다시 시도해
            주세요.
          </p>
          <button
            className="w-full py-3 rounded-full bg-brand-gradient text-ink font-semibold shadow-lg shadow-blush-200/60 hover:shadow-blush-300/70 hover:-translate-y-0.5 transition-all"
            onClick={() => moveToPath("/auth/login")}
          >
            로그인 화면으로
          </button>
        </div>
      </AuthLayout>
    );
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;

    completeParam[name] = type === "checkbox" ? checked : value;

    setCompleteParam({ ...completeParam });

    if (name === "nickname") setNicknameStatus(null);
  };

  const handleBlurNickname = () => {
    markTouched("nickname");

    if (!completeParam.nickname) return;

    setNicknameStatus("checking");

    checkNicknameAvailable(completeParam.nickname)
      .then((data) =>
        setNicknameStatus(data.available ? "available" : "unavailable"),
      )
      .catch((err) => {
        console.log(err);
        setNicknameStatus(null);
      });
  };

  const handleBlurPhone = () => {
    markTouched("phone");

    if (!completeParam.phone) return;

    if (!PHONE_REGEX.test(completeParam.phone)) {
      setPhoneStatus("invalid");
      return;
    }

    setPhoneStatus("checking");

    checkPhoneAvailable(completeParam.phone)
      .then((data) => {
        if (data.blocked) {
          setPhoneStatus("blocked");
        } else {
          setPhoneStatus(data.available ? "available" : "unavailable");
        }
      })
      .catch((err) => {
        console.log(err);
        setPhoneStatus(null);
      });
  };

  const markTouched = (name) => {
    setTouched((prev) => ({ ...prev, [name]: true }));
  };

  const formatPhoneNumber = (value) => {
    const digits = value.replace(/[^0-9]/g, "").slice(0, 11);

    if (digits.length < 4) return digits;
    if (digits.length < 8) {
      return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    }
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  };

  const handleChangePhone = (e) => {
    completeParam.phone = formatPhoneNumber(e.target.value);
    setCompleteParam({ ...completeParam });
    setPhoneStatus(null);
  };

  const handleClickAllAgree = (e) => {
    const checked = e.target.checked;
    completeParam.termsAgree = checked;
    completeParam.privacyAgree = checked;
    completeParam.marketing = checked;
    setCompleteParam({ ...completeParam });
  };

  const handleClickAddressSearch = () => {
    markTouched("address");

    if (!window.daum || !window.daum.Postcode) {
      alert(
        "주소 검색 기능을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
      return;
    }

    new window.daum.Postcode({
      oncomplete: (data) => {
        completeParam.zipCode = data.zonecode;
        completeParam.address = data.roadAddress || data.jibunAddress;
        setCompleteParam({ ...completeParam });
      },
    }).open();
  };

  const handleClickComplete = () => {
    setTouched({ nickname: true, name: true, phone: true, address: true });

    if (!completeParam.nickname) {
      alert("닉네임은 필수입니다.");
      return;
    }

    if (nicknameStatus === "unavailable") {
      alert("이미 사용 중인 닉네임입니다. 다른 닉네임을 사용해주세요.");
      return;
    }

    if (!completeParam.name) {
      alert("이름은 필수입니다.");
      return;
    }

    if (!completeParam.phone) {
      alert("휴대폰 번호는 필수입니다.");
      return;
    }

    if (!PHONE_REGEX.test(completeParam.phone)) {
      alert("올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)");
      return;
    }

    if (phoneStatus === "unavailable") {
      alert("이미 사용 중인 휴대폰 번호입니다.");
      return;
    }

    if (phoneStatus === "blocked") {
      alert(
        "정지 또는 휴면 처리된 회원과 동일한 전화번호입니다. 관리자에게 문의해주세요.",
      );
      return;
    }

    if (!completeParam.address) {
      alert("주소는 필수입니다.");
      return;
    }

    if (!completeParam.termsAgree || !completeParam.privacyAgree) {
      alert(
        "이용약관과 개인정보처리방침에 동의해야 가입을 완료할 수 있습니다.",
      );
      return;
    }

    const payload = {
      pendingToken,
      ...completeParam,
      birthDate: completeParam.birthDate || null,
    };

    setSubmitting(true);

    completeKakaoSignupPost(payload)
      .then((data) => {
        console.log(data);

        // 이 응답에 처음으로 accessToken/refreshToken이 들어있음 - 여기서 비로소 로그인 처리
        dispatch(login(data));

        alert("가입이 완료되었습니다.");
        moveToPath("/");
      })
      .catch((err) => {
        console.log(err);

        const msg = err.response?.data?.msg;

        alert(msg || "처리 중 오류가 발생했습니다.");
      })
      .finally(() => setSubmitting(false));
  };

  const renderNicknameMsg = () => {
    if (touched.nickname && !completeParam.nickname)
      return <div className={errMsgClass}>닉네임은 필수 입력 항목입니다.</div>;
    if (nicknameStatus === "checking")
      return <div className="text-ink-faint text-xs mt-1">확인 중...</div>;
    if (nicknameStatus === "available")
      return <div className={okMsgClass}>✓ 사용가능!</div>;
    if (nicknameStatus === "unavailable")
      return (
        <div className={errMsgClass}>
          이미 사용 중인 닉네임이에요. 다른 닉네임을 사용해주세요.
        </div>
      );
    return null;
  };

  const renderNameMsg = () => {
    if (touched.name && !completeParam.name)
      return <div className={errMsgClass}>이름은 필수 입력 항목입니다.</div>;
    return null;
  };

  const renderPhoneMsg = () => {
    if (touched.phone && !completeParam.phone)
      return (
        <div className={errMsgClass}>휴대폰 번호는 필수 입력 항목입니다.</div>
      );
    if (phoneStatus === "invalid")
      return (
        <div className={errMsgClass}>
          올바른 휴대폰 번호 형식이 아니에요 (예: 010-1234-5678)
        </div>
      );
    if (phoneStatus === "checking")
      return <div className="text-ink-faint text-xs mt-1">확인 중...</div>;
    if (phoneStatus === "available")
      return <div className={okMsgClass}>✓ 사용가능!</div>;
    if (phoneStatus === "unavailable")
      return <div className={errMsgClass}>이미 사용 중인 휴대폰 번호예요</div>;
    if (phoneStatus === "blocked")
      return (
        <div className={errMsgClass}>
          차단(정지·휴면)된 회원의 번호예요. 관리자에게 문의해주세요.
        </div>
      );
    return null;
  };

  const renderAddressMsg = () => {
    if (touched.address && !completeParam.address)
      return <div className={errMsgClass}>주소는 필수 입력 항목입니다.</div>;
    return null;
  };

  return (
    <AuthLayout
      eyebrow="카카오 신규 회원"
      title={
        <>
          거의 다
          <br />
          왔어요!
        </>
      }
      subtitle="추가 정보만 입력하면 가입이 완료돼요"
      stickerEmoji="🎉"
      footer={
        <div className="bg-white/80 border border-line rounded-xl p-4">
          <div className="text-sm font-semibold text-ink mb-1">
            카카오 계정 연동됨
          </div>
          <div className="text-xs text-ink-muted">{kakaoEmail}</div>
          <div className="text-xs text-brand-deep mt-1">✓ 인증 완료</div>
        </div>
      }
    >
      <div className="max-w-sm w-full mx-auto">
        <h2 className="font-serifkr text-2xl text-ink mb-1">추가 정보 입력</h2>
        <p className="text-ink-muted text-sm mb-6">
          나머지 정보를 입력해 가입을 완료해요
        </p>

        <div className="mb-4">
          <label className={labelClass}>닉네임 *</label>
          <input
            className={inputClass}
            name="nickname"
            type="text"
            placeholder="사용하실 닉네임을 입력하세요"
            value={completeParam.nickname}
            onChange={handleChange}
            onBlur={handleBlurNickname}
          ></input>
          {renderNicknameMsg()}
        </div>

        <div className="mb-4">
          <label className={labelClass}>이름 *</label>
          <input
            className={inputClass}
            name="name"
            type="text"
            placeholder="실명을 입력하세요"
            value={completeParam.name}
            onChange={handleChange}
            onBlur={() => markTouched("name")}
          ></input>
          {renderNameMsg()}
        </div>

        <div className="mb-4">
          <label className={labelClass}>휴대폰 번호 *</label>
          <input
            className={inputClass}
            name="phone"
            type="text"
            placeholder="010-1234-5678"
            value={completeParam.phone}
            onChange={handleChangePhone}
            onBlur={handleBlurPhone}
          ></input>
          {renderPhoneMsg()}
        </div>

        <div className="mb-4">
          <label className={labelClass}>생년월일</label>
          <input
            className={inputClass}
            name="birthDate"
            type="date"
            value={completeParam.birthDate}
            onChange={handleChange}
          ></input>
        </div>

        <div className="mb-4">
          <label className={labelClass}>주소 *</label>
          <div className="flex gap-2">
            <input
              className={inputClass + " bg-blush-100/60"}
              name="zipCode"
              type="text"
              placeholder="우편번호"
              value={completeParam.zipCode}
              readOnly
              onBlur={() => markTouched("address")}
            ></input>
            <button
              type="button"
              className="shrink-0 px-4 rounded-xl border border-line text-brand-deep text-sm font-semibold hover:bg-blush-50 transition whitespace-nowrap"
              onClick={handleClickAddressSearch}
            >
              📍 주소 검색
            </button>
          </div>
          <input
            className={inputClass + " mt-2 bg-blush-100/60"}
            name="address"
            type="text"
            placeholder="주소 검색을 눌러주세요"
            value={completeParam.address}
            readOnly
          ></input>
          <input
            className={inputClass + " mt-2"}
            name="addressDetail"
            type="text"
            placeholder="상세주소를 입력해 주세요"
            value={completeParam.addressDetail}
            onChange={handleChange}
          ></input>
          {renderAddressMsg()}
        </div>

        <div className="bg-blush-50 rounded-xl p-4 mb-6">
          <label className="flex items-center gap-2 font-semibold text-ink pb-2 mb-2 border-b border-line">
            <input
              type="checkbox"
              checked={
                completeParam.termsAgree &&
                completeParam.privacyAgree &&
                completeParam.marketing
              }
              onChange={handleClickAllAgree}
              className="accent-brand-deep"
            />
            전체 동의
          </label>
          <label className="flex items-center gap-2 text-sm text-ink-soft mb-1.5">
            <input
              type="checkbox"
              name="termsAgree"
              checked={completeParam.termsAgree}
              onChange={handleChange}
              className="accent-brand-deep"
            />
            [필수] 이용약관에 동의합니다.
          </label>
          <label className="flex items-center gap-2 text-sm text-ink-soft mb-1.5">
            <input
              type="checkbox"
              name="privacyAgree"
              checked={completeParam.privacyAgree}
              onChange={handleChange}
              className="accent-brand-deep"
            />
            [필수] 개인정보처리방침에 동의합니다.
          </label>
          <label className="flex items-center gap-2 text-sm text-ink-soft">
            <input
              type="checkbox"
              name="marketing"
              checked={completeParam.marketing}
              onChange={handleChange}
              className="accent-brand-deep"
            />
            [선택] 마케팅 정보 수신에 동의합니다.
          </label>
        </div>

        <button
          disabled={submitting}
          className="w-full py-3 rounded-full bg-brand-gradient text-ink font-semibold shadow-lg shadow-blush-200/60 hover:shadow-blush-300/70 hover:-translate-y-0.5 transition-all disabled:opacity-50"
          onClick={handleClickComplete}
        >
          {submitting ? "처리 중..." : "가입 완료하기"}
        </button>
      </div>
    </AuthLayout>
  );
};

export default SocialCompleteComponent;
