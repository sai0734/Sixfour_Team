import { useState } from "react";
import {
  joinPost,
  resendVerificationPost,
  checkEmailAvailable,
  checkNicknameAvailable,
  checkPhoneAvailable,
} from "../../api/authApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import AuthLayout from "./AuthLayout";
import AlertModal from "./AlertModal";

const initState = {
  email: "",
  pw: "",
  pwCheck: "",
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
  "w-full px-4 py-3 rounded-xl border border-rose-100 bg-blush-50/40 text-plum-900 placeholder:text-plum-500/50 focus:border-rose-400 focus:ring-4 focus:ring-rose-100 outline-none transition";
const labelClass = "block text-sm font-semibold text-plum-900/80 mb-1.5";
const okMsgClass = "text-green-600 text-xs mt-1";
const errMsgClass = "text-rose-600 text-xs mt-1";
const hintMsgClass = "text-plum-500 text-xs mt-1";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PW_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+=-]).{8,}$/;
const PHONE_REGEX = /^01[0-9]-?\d{3,4}-?\d{4}$/;

const JoinComponent = () => {
  const [joinParam, setJoinParam] = useState({ ...initState });
  const [pendingEmail, setPendingEmail] = useState(null);
  const [emailStatus, setEmailStatus] = useState(null);
  const [nicknameStatus, setNicknameStatus] = useState(null);
  const [phoneStatus, setPhoneStatus] = useState(null);
  const [touched, setTouched] = useState({
    email: false,
    pw: false,
    pwCheck: false,
    nickname: false,
    name: false,
    phone: false,
    address: false,
  });
  const [alertMessage, setAlertMessage] = useState("");
  const [pendingLoginRedirect, setPendingLoginRedirect] = useState(false);
  const { moveToLogin } = useCustomLogin();

  const closeAlert = () => {
    setAlertMessage("");
    if (pendingLoginRedirect) {
      moveToLogin();
    }
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
    joinParam.phone = formatPhoneNumber(e.target.value);
    setJoinParam({ ...joinParam });
    setPhoneStatus(null);
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;

    joinParam[name] = type === "checkbox" ? checked : value;

    setJoinParam({ ...joinParam });

    if (name === "email") setEmailStatus(null);
    if (name === "nickname") setNicknameStatus(null);
  };

  const markTouched = (name) => {
    setTouched((prev) => ({ ...prev, [name]: true }));
  };

  const handleClickAllAgree = (e) => {
    const checked = e.target.checked;
    joinParam.termsAgree = checked;
    joinParam.privacyAgree = checked;
    joinParam.marketing = checked;
    setJoinParam({ ...joinParam });
  };

  const handleBlurEmail = () => {
    markTouched("email");

    if (!joinParam.email) return;

    if (!EMAIL_REGEX.test(joinParam.email)) {
      setEmailStatus("invalid");
      return;
    }

    setEmailStatus("checking");

    checkEmailAvailable(joinParam.email)
      .then((data) =>
        setEmailStatus(data.available ? "available" : "unavailable"),
      )
      .catch((err) => {
        console.log(err);
        setEmailStatus(null);
      });
  };

  const handleBlurNickname = () => {
    markTouched("nickname");

    if (!joinParam.nickname) return;

    setNicknameStatus("checking");

    checkNicknameAvailable(joinParam.nickname)
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

    if (!joinParam.phone) return;

    if (!PHONE_REGEX.test(joinParam.phone)) {
      setPhoneStatus("invalid");
      return;
    }

    setPhoneStatus("checking");

    checkPhoneAvailable(joinParam.phone)
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

  const handleClickAddressSearch = () => {
    markTouched("address");

    if (!window.daum || !window.daum.Postcode) {
      setAlertMessage(
        "주소 검색 기능을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
      return;
    }

    new window.daum.Postcode({
      oncomplete: (data) => {
        joinParam.zipCode = data.zonecode;
        joinParam.address = data.roadAddress || data.jibunAddress;
        setJoinParam({ ...joinParam });
      },
    }).open();
  };

  const handleClickJoin = () => {
    setTouched({
      email: true,
      pw: true,
      pwCheck: true,
      nickname: true,
      name: true,
      phone: true,
      address: true,
    });

    if (!joinParam.email || !joinParam.pw || !joinParam.nickname) {
      setAlertMessage("이메일, 비밀번호, 닉네임은 필수입니다.");
      return;
    }

    if (!EMAIL_REGEX.test(joinParam.email)) {
      setAlertMessage("올바른 이메일 형식이 아닙니다.");
      return;
    }

    if (emailStatus === "unavailable") {
      setAlertMessage("이미 사용 중인 이메일입니다.");
      return;
    }

    if (!PW_REGEX.test(joinParam.pw)) {
      setAlertMessage(
        "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상이어야 합니다.",
      );
      return;
    }

    if (joinParam.pw !== joinParam.pwCheck) {
      setAlertMessage("비밀번호가 일치하지 않습니다.");
      return;
    }

    if (nicknameStatus === "unavailable") {
      setAlertMessage("이미 사용 중인 닉네임입니다. 다른 닉네임을 사용해주세요.");
      return;
    }

    if (!joinParam.name) {
      setAlertMessage("이름은 필수입니다.");
      return;
    }

    if (!joinParam.phone) {
      setAlertMessage("휴대폰 번호는 필수입니다.");
      return;
    }

    if (!PHONE_REGEX.test(joinParam.phone)) {
      setAlertMessage("올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)");
      return;
    }

    if (phoneStatus === "unavailable") {
      setAlertMessage("이미 사용 중인 휴대폰 번호입니다.");
      return;
    }

    if (phoneStatus === "blocked") {
      setAlertMessage(
        "정지 또는 휴면 처리된 회원과 동일한 전화번호입니다. 관리자에게 문의해주세요.",
      );
      return;
    }

    if (!joinParam.address) {
      setAlertMessage("주소는 필수입니다.");
      return;
    }

    if (!joinParam.termsAgree || !joinParam.privacyAgree) {
      setAlertMessage("이용약관과 개인정보처리방침에 동의해야 가입할 수 있습니다.");
      return;
    }

    const { pwCheck, ...joinData } = joinParam;
    joinData.birthDate = joinData.birthDate || null;

    joinPost(joinData)
      .then((data) => {
        console.log(data);
        setPendingEmail(joinData.email);
      })
      .catch((err) => {
        console.log(err);
        if (err.response && err.response.status === 409) {
          setAlertMessage("이미 가입된 이메일입니다.");
        } else if (err.response && err.response.status === 400) {
          setAlertMessage("입력값을 다시 확인해 주세요.");
        } else {
          setAlertMessage("회원가입 중 오류가 발생했습니다.");
        }
      });
  };

  const handleClickResend = () => {
    resendVerificationPost(pendingEmail)
      .then((data) => {
        console.log(data);
        if (data.result === "success") {
          setAlertMessage(
            "인증 메일을 다시 보냈습니다. 메일함(스팸함 포함)을 확인해 주세요.",
          );
        } else if (data.reason === "ALREADY_VERIFIED") {
          setPendingLoginRedirect(true);
          setAlertMessage("이미 인증이 완료된 계정입니다. 로그인해 주세요.");
        } else {
          setAlertMessage("재발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
      })
      .catch((err) => {
        console.log(err);
        setAlertMessage("재발송 중 오류가 발생했습니다.");
      });
  };

  const renderEmailMsg = () => {
    if (touched.email && !joinParam.email)
      return <div className={errMsgClass}>이메일은 필수 입력 항목입니다.</div>;
    if (emailStatus === "checking")
      return <div className={hintMsgClass}>확인 중...</div>;
    if (emailStatus === "invalid")
      return <div className={errMsgClass}>올바른 이메일 형식이 아니에요</div>;
    if (emailStatus === "available")
      return <div className={okMsgClass}>✓ 사용가능!</div>;
    if (emailStatus === "unavailable")
      return <div className={errMsgClass}>이미 사용 중인 이메일이에요</div>;
    return null;
  };

  const renderPwMsg = () => {
    if (touched.pw && !joinParam.pw)
      return (
        <div className={errMsgClass}>비밀번호는 필수 입력 항목입니다.</div>
      );
    if (joinParam.pw.length > 0 && !PW_REGEX.test(joinParam.pw))
      return (
        <div className={errMsgClass}>
          영문+숫자+특수문자 조합 8자 이상이어야 해요
        </div>
      );
    if (joinParam.pw.length > 0 && PW_REGEX.test(joinParam.pw))
      return <div className={okMsgClass}>✓ 사용가능!</div>;
    return null;
  };

  const renderPwCheckMsg = () => {
    if (!PW_REGEX.test(joinParam.pw)) return null;

    if (touched.pwCheck && !joinParam.pwCheck)
      return (
        <div className={errMsgClass}>비밀번호 확인은 필수 입력 항목입니다.</div>
      );
    if (joinParam.pwCheck.length > 0 && joinParam.pw !== joinParam.pwCheck)
      return <div className={errMsgClass}>비밀번호가 서로 달라요</div>;
    if (joinParam.pwCheck.length > 0 && joinParam.pw === joinParam.pwCheck)
      return <div className={okMsgClass}>✓ 일치해요!</div>;
    return null;
  };

  const renderNicknameMsg = () => {
    if (touched.nickname && !joinParam.nickname)
      return <div className={errMsgClass}>닉네임은 필수 입력 항목입니다.</div>;
    if (nicknameStatus === "checking")
      return <div className={hintMsgClass}>확인 중...</div>;
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
    if (touched.name && !joinParam.name)
      return <div className={errMsgClass}>이름은 필수 입력 항목입니다.</div>;
    return null;
  };

  const renderPhoneMsg = () => {
    if (touched.phone && !joinParam.phone)
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
      return <div className={hintMsgClass}>확인 중...</div>;
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
    if (touched.address && !joinParam.address)
      return <div className={errMsgClass}>주소는 필수 입력 항목입니다.</div>;
    return null;
  };

  if (pendingEmail) {
    return (
      <AuthLayout
        eyebrow="메일함을 확인해 주세요"
        title={
          <>
            거의 다
            <br />
            왔어요!
          </>
        }
        subtitle="인증 메일 속 링크를 눌러야 가입이 완료돼요"
      >
        <div className="max-w-sm w-full mx-auto text-center">
          <div className="w-16 h-16 rounded-full bg-blush-100 flex items-center justify-center mx-auto mb-6 text-3xl">
            ✉️
          </div>
          <h2 className="font-body text-2xl text-plum-900 mb-2">
            메일함을 확인해 주세요
          </h2>
          <p className="text-plum-500 text-sm mb-1">
            <span className="font-semibold text-rose-600">{pendingEmail}</span>
          </p>
          <p className="text-plum-500 text-sm mb-8">
            주소로 인증 메일을 보냈습니다. 스팸함도 확인해 주세요.
          </p>

          <button
            type="button"
            className="w-full py-3.5 rounded-xl bg-gradient-to-r from-rose-500 to-pink-500 text-white font-semibold shadow-lg shadow-rose-200 hover:shadow-rose-300 hover:-translate-y-0.5 transition-all mb-3"
            onClick={() => moveToLogin()}
          >
            로그인 화면으로
          </button>
          <button
            type="button"
            className="w-full py-3 rounded-xl border border-rose-200 text-rose-600 font-semibold hover:bg-blush-50 transition"
            onClick={handleClickResend}
          >
            메일 다시 받기
          </button>

          <button
            type="button"
            className="text-sm text-plum-500 underline mt-6"
            onClick={() => setPendingEmail(null)}
          >
            이메일을 잘못 입력했나요? 다시 입력하기
          </button>
        </div>

        <AlertModal message={alertMessage} onClose={closeAlert} />
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      eyebrow="회원가입"
      title={
        <>
          환영합니다,
          <br />
          새 계정을
          <br />
          만들어보세요
        </>
      }
      subtitle="가입 후 모든 서비스를 자유롭게 이용하세요"
    >
      <div className="max-w-sm w-full mx-auto">
        <h2 className="font-body text-2xl text-plum-900 mb-1">
          계정 만들기
        </h2>
        <p className="text-plum-500 text-sm mb-6">
          정보를 입력해 가입을 완료하세요
        </p>

        <div className="mb-4">
          <label className={labelClass}>이메일 *</label>
          <input
            className={inputClass}
            name="email"
            type="text"
            placeholder="hong@example.com"
            value={joinParam.email}
            onChange={handleChange}
            onBlur={handleBlurEmail}
          ></input>
          {renderEmailMsg()}
        </div>

        <div className="grid grid-cols-2 gap-3 mb-1">
          <div>
            <label className={labelClass}>비밀번호 *</label>
            <input
              className={inputClass}
              name="pw"
              type="password"
              value={joinParam.pw}
              onChange={handleChange}
              onBlur={() => markTouched("pw")}
            ></input>
            {renderPwMsg()}
          </div>
          <div>
            <label className={labelClass}>비밀번호 확인 *</label>
            <input
              className={inputClass}
              name="pwCheck"
              type="password"
              value={joinParam.pwCheck}
              onChange={handleChange}
              onBlur={() => markTouched("pwCheck")}
            ></input>
            {renderPwCheckMsg()}
          </div>
        </div>
        <div className="mb-3"></div>

        <div className="mb-4">
          <label className={labelClass}>닉네임 *</label>
          <input
            className={inputClass}
            name="nickname"
            type="text"
            value={joinParam.nickname}
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
            value={joinParam.name}
            onChange={handleChange}
            onBlur={() => markTouched("name")}
          ></input>
          {renderNameMsg()}
        </div>

        <div className="grid grid-cols-2 gap-3 mb-4">
          <div>
            <label className={labelClass}>휴대폰 번호 *</label>
            <input
              className={inputClass}
              name="phone"
              type="text"
              placeholder="010-1234-5678"
              value={joinParam.phone}
              onChange={handleChangePhone}
              onBlur={handleBlurPhone}
            ></input>
            {renderPhoneMsg()}
          </div>
          <div>
            <label className={labelClass}>생년월일</label>
            <input
              className={inputClass}
              name="birthDate"
              type="date"
              value={joinParam.birthDate}
              onChange={handleChange}
            ></input>
          </div>
        </div>

        <div className="mb-4">
          <label className={labelClass}>주소 *</label>
          <div className="flex gap-2">
            <input
              className={inputClass + " bg-blush-100/60"}
              name="zipCode"
              type="text"
              placeholder="우편번호"
              value={joinParam.zipCode}
              readOnly
              onBlur={() => markTouched("address")}
            ></input>
            <button
              type="button"
              className="shrink-0 px-4 rounded-xl border border-rose-200 text-rose-600 text-sm font-semibold hover:bg-blush-50 transition whitespace-nowrap"
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
            value={joinParam.address}
            readOnly
          ></input>
          <input
            className={inputClass + " mt-2"}
            name="addressDetail"
            type="text"
            placeholder="상세주소를 입력해 주세요"
            value={joinParam.addressDetail}
            onChange={handleChange}
          ></input>
          {renderAddressMsg()}
        </div>

        <div className="bg-blush-50 rounded-xl p-4 mb-6">
          <label className="flex items-center gap-2 font-semibold text-plum-900 pb-2 mb-2 border-b border-rose-100">
            <input
              type="checkbox"
              checked={
                joinParam.termsAgree &&
                joinParam.privacyAgree &&
                joinParam.marketing
              }
              onChange={handleClickAllAgree}
              className="accent-rose-500"
            />
            전체 동의
          </label>
          <label className="flex items-center gap-2 text-sm text-plum-900/80 mb-1.5">
            <input
              type="checkbox"
              name="termsAgree"
              checked={joinParam.termsAgree}
              onChange={handleChange}
              className="accent-rose-500"
            />
            [필수] 이용약관에 동의합니다.
          </label>
          <label className="flex items-center gap-2 text-sm text-plum-900/80 mb-1.5">
            <input
              type="checkbox"
              name="privacyAgree"
              checked={joinParam.privacyAgree}
              onChange={handleChange}
              className="accent-rose-500"
            />
            [필수] 개인정보처리방침에 동의합니다.
          </label>
          <label className="flex items-center gap-2 text-sm text-plum-900/80">
            <input
              type="checkbox"
              name="marketing"
              checked={joinParam.marketing}
              onChange={handleChange}
              className="accent-rose-500"
            />
            [선택] 마케팅 정보 수신에 동의합니다.
          </label>
        </div>

        <button
          type="button"
          className="w-full py-4 rounded-xl bg-gradient-to-r from-rose-100 to-pink-100 text-rose-700 font-semibold shadow-md shadow-rose-100 hover:shadow-rose-200 hover:-translate-y-0.5 transition-all"
          onClick={handleClickJoin}
        >
          가입하기
        </button>

        <div className="text-center mt-6 mb-10 text-sm text-plum-500">
          이미 계정이 있으신가요?{" "}
          <button
            type="button"
            className="text-rose-600 font-semibold hover:text-rose-700 ml-1"
            onClick={() => moveToLogin()}
          >
            로그인
          </button>
        </div>
      </div>

      <AlertModal message={alertMessage} onClose={closeAlert} />
    </AuthLayout>
  );
};

export default JoinComponent;
