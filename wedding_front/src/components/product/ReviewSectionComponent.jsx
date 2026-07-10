import { useEffect, useState } from "react";
import {
  getReviews,
  checkReviewEligibility,
  postReview,
  putReview,
  postReply,
  putReply,
  deleteReview,
} from "../../api/reviewApi";

const MAX_IMAGE_SIZE = 10 * 1024 * 1024;
const MAX_VIDEO_SIZE = 50 * 1024 * 1024;

const VIDEO_EXTENSIONS = ["mp4", "webm", "mov", "avi", "mkv"];
const isVideoFile = (fileName) => {
  const ext = fileName?.split(".").pop()?.toLowerCase();
  return VIDEO_EXTENSIONS.includes(ext);
};

const isGifFile = (fileName) => {
  return fileName?.split(".").pop()?.toLowerCase() === "gif";
};

const AVATAR_BG = ["bg-brand-light", "bg-lavender-light", "bg-blush-100"];

const StarDisplay = ({ rating }) => (
  <span className="text-[#C9A96A] text-xs">
    {"★".repeat(rating || 0)}
    <span className="text-line-soft">{"★".repeat(5 - (rating || 0))}</span>
  </span>
);

const StarPicker = ({ value, onChange }) => (
  <div className="flex gap-1 text-xl cursor-pointer">
    {[1, 2, 3, 4, 5].map((n) => (
      <span
        key={n}
        onClick={() => onChange(n)}
        className={n <= value ? "text-[#C9A96A]" : "text-line-soft"}
      >
        ★
      </span>
    ))}
  </div>
);

const ReviewMediaThumb = ({ fileName, host, onClick }) => {
  if (isVideoFile(fileName)) {
    return (
      <div
        onClick={onClick}
        className="relative w-16 h-16 rounded-lg overflow-hidden bg-black cursor-pointer"
      >
        <video
          className="w-full h-full object-cover"
          src={`${host}/api/product/view/${fileName}`}
        />
        <div className="absolute inset-0 flex items-center justify-center bg-black/30">
          <svg
            viewBox="0 0 24 24"
            className="w-6 h-6 text-white"
            fill="currentColor"
          >
            <path d="M8 5v14l11-7z" />
          </svg>
        </div>
      </div>
    );
  }

  const src = isGifFile(fileName)
    ? `${host}/api/product/view/${fileName}`
    : `${host}/api/product/view/s_${fileName}`;

  return (
    <div
      onClick={onClick}
      className="w-16 h-16 rounded-lg overflow-hidden bg-surface cursor-pointer"
    >
      <img alt="" className="w-full h-full object-cover" src={src} />
    </div>
  );
};

const MediaModal = ({ fileName, host, onClose }) => {
  const isVideo = isVideoFile(fileName);
  const src = `${host}/api/product/view/${fileName}`;

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-6"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="max-w-3xl max-h-[85vh]"
      >
        {isVideo ? (
          <video
            controls
            autoPlay
            className="max-w-full max-h-[85vh] rounded-lg"
            src={src}
          />
        ) : (
          <img
            alt=""
            className="max-w-full max-h-[85vh] rounded-lg object-contain"
            src={src}
          />
        )}
      </div>
      <button
        onClick={onClose}
        className="absolute top-5 right-5 w-9 h-9 rounded-full bg-white/20 text-white text-xl flex items-center justify-center"
      >
        ×
      </button>
    </div>
  );
};

const validateFiles = (fileList) => {
  for (const file of Array.from(fileList)) {
    const isImage = file.type.startsWith("image/");
    const isVideo = file.type.startsWith("video/");

    if (!isImage && !isVideo) {
      return `"${file.name}"은(는) 이미지 또는 동영상 파일만 첨부할 수 있습니다.`;
    }
    if (isImage && file.size > MAX_IMAGE_SIZE) {
      return `이미지는 1장당 최대 10MB까지 첨부할 수 있습니다. ("${file.name}")`;
    }
    if (isVideo && file.size > MAX_VIDEO_SIZE) {
      return `동영상은 1개당 최대 50MB까지 첨부할 수 있습니다. ("${file.name}")`;
    }
  }
  return null;
};

const ReviewForm = ({
  initialRating,
  initialContent,
  initialFiles,
  host,
  onCancel,
  onSubmit,
  submitLabel,
}) => {
  const [rating, setRating] = useState(initialRating ?? 5);
  const [content, setContent] = useState(initialContent ?? "");
  const [keepFileNames, setKeepFileNames] = useState(initialFiles ?? []);
  const [newFiles, setNewFiles] = useState(null);

  const handleChangeFiles = (e) => {
    const errorMsg = validateFiles(e.target.files);
    if (errorMsg) {
      alert(errorMsg);
      e.target.value = "";
      setNewFiles(null);
      return;
    }
    setNewFiles(e.target.files);
  };

  const handleRemoveExistingFile = (fileName) => {
    setKeepFileNames((prev) => prev.filter((f) => f !== fileName));
  };

  const handleSubmit = () => {
    if (!content.trim()) {
      alert("리뷰 내용을 입력해주세요.");
      return;
    }
    onSubmit({ rating, content, keepFileNames, newFiles });
  };

  return (
    <div className="bg-white rounded-2xl p-5 mb-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
      <p className="text-xs text-ink-soft mb-2">별점</p>
      <StarPicker value={rating} onChange={setRating} />

      <p className="text-xs text-ink-soft mt-4 mb-2">리뷰 내용</p>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={4}
        placeholder="상품에 대한 솔직한 후기를 남겨주세요."
        className="w-full border border-line-soft rounded-xl p-3 text-sm focus:outline-none focus:border-brand resize-none"
      />

      {keepFileNames.length > 0 && (
        <div className="mt-4">
          <p className="text-xs text-ink-soft mb-2">
            기존 첨부 (X를 눌러 삭제)
          </p>
          <div className="flex gap-2 flex-wrap">
            {keepFileNames.map((file, i) => (
              <div key={i} className="relative">
                <ReviewMediaThumb
                  fileName={file}
                  host={host}
                  onClick={() => {}}
                />
                <button
                  onClick={() => handleRemoveExistingFile(file)}
                  className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-ink text-white text-xs flex items-center justify-center"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <p className="text-xs text-ink-soft mt-4 mb-2">
        사진/동영상 추가 첨부 (선택, 사진 최대 10MB · 동영상 최대 50MB)
      </p>
      <input
        type="file"
        multiple
        accept="image/*,video/*"
        onChange={handleChangeFiles}
        className="text-xs"
      />

      <div className="flex flex-wrap justify-end gap-2 mt-4">
        <button
          onClick={onCancel}
          className="h-9 px-5 rounded-full border border-line-soft text-xs"
        >
          취소
        </button>
        <button
          onClick={handleSubmit}
          className="h-9 px-5 rounded-full bg-brand text-white text-xs font-medium"
        >
          {submitLabel}
        </button>
      </div>
    </div>
  );
};

const ReplyEditForm = ({ initialContent, onCancel, onSubmit }) => {
  const [content, setContent] = useState(initialContent ?? "");

  const handleSubmit = () => {
    if (!content.trim()) {
      alert("답변 내용을 입력해주세요.");
      return;
    }
    onSubmit(content);
  };

  return (
    <div className="flex flex-col sm:flex-row gap-2">
      <input
        type="text"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        className="flex-1 border border-line-soft rounded-full px-4 py-1.5 text-xs focus:outline-none focus:border-brand"
      />
      <div className="flex gap-2 justify-end">
        <button
          onClick={onCancel}
          className="text-xs px-4 rounded-full border border-line-soft h-7"
        >
          취소
        </button>
        <button
          onClick={handleSubmit}
          className="text-xs px-4 rounded-full bg-brand text-white h-7"
        >
          수정 완료
        </button>
      </div>
    </div>
  );
};

const ReviewSectionComponent = ({
  pno,
  host,
  isLoggedIn,
  isAdmin,
  myEmail,
  onStatsChange,
}) => {
  const [reviews, setReviews] = useState([]);
  const [showWriteForm, setShowWriteForm] = useState(false);
  const [editingRno, setEditingRno] = useState(null);
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState("");
  const [editingReplyRno, setEditingReplyRno] = useState(null);
  const [modalFile, setModalFile] = useState(null);

  useEffect(() => {
    fetchReviews();
  }, [pno]);

  const fetchReviews = () => {
    getReviews(pno).then((data) => setReviews(data));
  };

  const checkIsMine = (memberEmail) => {
    return !!myEmail && memberEmail === myEmail;
  };

  const myReview = reviews.find((r) => checkIsMine(r.memberEmail));

  const handleClickWriteReview = () => {
    if (!isLoggedIn) {
      alert("리뷰는 구매 회원만 작성이 가능합니다.");
      return;
    }

    checkReviewEligibility(pno)
      .then(() => setShowWriteForm(true))
      .catch((err) => {
        const msg =
          err.response?.data?.msg || "리뷰는 구매 회원만 작성이 가능합니다.";
        alert(msg);
      });
  };

  const handleSubmitNewReview = ({
    rating,
    content,
    keepFileNames,
    newFiles,
  }) => {
    postReview(pno, { rating, content, files: newFiles })
      .then(() => {
        setShowWriteForm(false);
        fetchReviews();
        onStatsChange?.();
      })
      .catch((err) => {
        alert(err.response?.data?.msg || "리뷰 등록에 실패했습니다.");
      });
  };

  const handleSubmitEditReview = (
    rno,
    { rating, content, keepFileNames, newFiles },
  ) => {
    putReview(pno, rno, { rating, content, keepFileNames, newFiles })
      .then(() => {
        setEditingRno(null);
        fetchReviews();
        onStatsChange?.();
      })
      .catch((err) => {
        alert(err.response?.data?.msg || "리뷰 수정에 실패했습니다.");
      });
  };

  const handleSubmitReply = (rno) => {
    if (!replyContent.trim()) {
      alert("답변 내용을 입력해주세요.");
      return;
    }

    postReply(pno, rno, replyContent).then(() => {
      setReplyingTo(null);
      setReplyContent("");
      fetchReviews();
    });
  };

  const handleSubmitEditReply = (rno, content) => {
    putReply(pno, rno, content)
      .then(() => {
        setEditingReplyRno(null);
        fetchReviews();
      })
      .catch((err) => {
        alert(err.response?.data?.msg || "답변 수정에 실패했습니다.");
      });
  };

  const handleDeleteReview = (rno) => {
    if (!window.confirm("이 리뷰를 삭제하시겠습니까?")) return;

    deleteReview(pno, rno).then(() => {
      fetchReviews();
      onStatsChange?.();
    });
  };

  return (
    <div>
      {modalFile && (
        <MediaModal
          fileName={modalFile}
          host={host}
          onClose={() => setModalFile(null)}
        />
      )}

      <div className="flex flex-wrap justify-between items-center gap-3 mb-6">
        <span className="inline-block -rotate-1 bg-blush-100 px-3 py-1 font-['Gaegu'] text-[13px] text-brand-deep">
          하객들의 솔직한 후기 {reviews.length}개
        </span>
        {!myReview && !showWriteForm && (
          <button
            onClick={handleClickWriteReview}
            className="h-9 px-5 rounded-full bg-brand text-white text-xs font-medium"
          >
            리뷰 작성
          </button>
        )}
      </div>

      {showWriteForm && (
        <ReviewForm
          host={host}
          onCancel={() => setShowWriteForm(false)}
          onSubmit={handleSubmitNewReview}
          submitLabel="등록"
        />
      )}

      {reviews.length === 0 ? (
        <div className="py-16 text-center font-['Gaegu'] text-base text-ink-faint">
          아직 리뷰가 없어요. 첫 후기의 주인공이 되어보세요 🤍
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {reviews.map((review, idx) => {
            const reviewIsMine = checkIsMine(review.memberEmail);

            return editingRno === review.rno ? (
              <ReviewForm
                key={review.rno}
                initialRating={review.rating}
                initialContent={review.content}
                initialFiles={review.uploadFileNames}
                host={host}
                onCancel={() => setEditingRno(null)}
                onSubmit={(payload) =>
                  handleSubmitEditReview(review.rno, payload)
                }
                submitLabel="수정 완료"
              />
            ) : (
              <div
                key={review.rno}
                className="bg-white rounded-2xl p-5 shadow-[0_6px_18px_-10px_rgba(58,54,47,0.18)]"
              >
                <div className="flex flex-wrap justify-between items-start gap-2">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-9 h-9 shrink-0 rounded-full flex items-center justify-center font-['Gaegu'] text-sm text-ink-soft ${AVATAR_BG[idx % AVATAR_BG.length]}`}
                    >
                      {review.nickname?.[0] ?? "?"}
                    </div>
                    <div>
                      <StarDisplay rating={review.rating} />
                      <p className="text-sm mt-0.5 font-['Gaegu']">
                        {review.nickname}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-ink-faint">
                      {review.regDate?.slice(0, 10)}
                    </p>
                    {reviewIsMine && (
                      <button
                        onClick={() => setEditingRno(review.rno)}
                        className="text-xs text-ink-faint underline mt-1 mr-2"
                      >
                        수정
                      </button>
                    )}
                    {(reviewIsMine || isAdmin) && (
                      <button
                        onClick={() => handleDeleteReview(review.rno)}
                        className="text-xs text-ink-faint underline mt-1"
                      >
                        삭제
                      </button>
                    )}
                  </div>
                </div>

                <p className="text-sm text-ink-soft mt-3 pl-12">
                  {review.content}
                </p>

                {review.uploadFileNames?.length > 0 && (
                  <div className="flex gap-2 mt-3 pl-12 flex-wrap">
                    {review.uploadFileNames.map((file, i) => (
                      <ReviewMediaThumb
                        key={i}
                        fileName={file}
                        host={host}
                        onClick={() => setModalFile(file)}
                      />
                    ))}
                  </div>
                )}

                {review.replies?.map((reply) => (
                  <div
                    key={reply.rno}
                    className="bg-cream rounded-xl p-4 mt-3 ml-6 md:ml-12"
                  >
                    {editingReplyRno === reply.rno ? (
                      <ReplyEditForm
                        initialContent={reply.content}
                        onCancel={() => setEditingReplyRno(null)}
                        onSubmit={(content) =>
                          handleSubmitEditReply(reply.rno, content)
                        }
                      />
                    ) : (
                      <>
                        <div className="flex flex-wrap justify-between items-start gap-2">
                          <span className="inline-block bg-brand-deep/90 text-white text-[11px] px-2 py-0.5 rounded-full">
                            답변
                          </span>
                          {isAdmin && (
                            <div className="flex gap-2">
                              <button
                                onClick={() => setEditingReplyRno(reply.rno)}
                                className="text-xs text-ink-faint underline"
                              >
                                수정
                              </button>
                              <button
                                onClick={() => handleDeleteReview(reply.rno)}
                                className="text-xs text-ink-faint underline"
                              >
                                삭제
                              </button>
                            </div>
                          )}
                        </div>
                        <p className="text-sm text-ink-soft mt-1.5">
                          {reply.content}
                        </p>
                      </>
                    )}
                  </div>
                ))}

                {isAdmin && review.replies?.length === 0 && (
                  <div className="ml-6 md:ml-12 mt-3">
                    {replyingTo === review.rno ? (
                      <div className="flex flex-col sm:flex-row gap-2">
                        <input
                          type="text"
                          value={replyContent}
                          onChange={(e) => setReplyContent(e.target.value)}
                          placeholder="답변을 입력하세요"
                          className="flex-1 border border-line-soft rounded-full px-4 py-1.5 text-xs focus:outline-none focus:border-brand"
                        />
                        <button
                          onClick={() => handleSubmitReply(review.rno)}
                          className="text-xs px-4 py-1.5 rounded-full bg-brand text-white self-end sm:self-auto"
                        >
                          등록
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => setReplyingTo(review.rno)}
                        className="text-xs text-brand-deep underline"
                      >
                        답변 달기
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default ReviewSectionComponent;
