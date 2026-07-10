import { useEffect, useState } from "react";
import {
  getQnaList,
  postQna,
  putQna,
  postQnaReply,
  putQnaReply,
  deleteQna,
} from "../../api/qnaApi";

const QnaForm = ({ initialContent, onCancel, onSubmit, submitLabel }) => {
  const [content, setContent] = useState(initialContent ?? "");

  const handleSubmit = () => {
    if (!content.trim()) {
      alert("문의 내용을 입력해주세요.");
      return;
    }
    onSubmit(content);
  };

  return (
    <div className="bg-white rounded-2xl p-5 mb-6 border-2 border-lavender-light">
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={3}
        placeholder="상품에 대해 궁금한 점을 남겨주세요."
        className="w-full border border-line-soft rounded-xl p-3 text-sm focus:outline-none focus:border-lavender-dark resize-none"
      />
      <div className="flex flex-wrap justify-end gap-2 mt-3">
        <button
          onClick={onCancel}
          className="h-9 px-5 rounded-full border border-line-soft text-xs"
        >
          취소
        </button>
        <button
          onClick={handleSubmit}
          className="h-9 px-5 rounded-full bg-lavender-dark text-white text-xs font-medium"
        >
          {submitLabel}
        </button>
      </div>
    </div>
  );
};

const QABadge = ({ type }) => (
  <span
    className={`inline-flex items-center justify-center w-7 h-7 shrink-0 rounded-full text-xs font-bold ${
      type === "Q"
        ? "bg-lavender-dark text-white"
        : "bg-lavender-light text-lavender-dark"
    }`}
  >
    {type}
  </span>
);

const QnaSectionComponent = ({ pno, isLoggedIn, isAdmin, myEmail }) => {
  const [qnaList, setQnaList] = useState([]);
  const [showWriteForm, setShowWriteForm] = useState(false);
  const [editingQno, setEditingQno] = useState(null);
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState("");
  const [editingReplyQno, setEditingReplyQno] = useState(null);
  const [expandedQnos, setExpandedQnos] = useState(new Set());

  const toggleExpand = (qno) => {
    setExpandedQnos((prev) => {
      const next = new Set(prev);
      if (next.has(qno)) {
        next.delete(qno);
      } else {
        next.add(qno);
      }
      return next;
    });
  };

  useEffect(() => {
    fetchQnaList();
  }, [pno]);

  const fetchQnaList = () => {
    getQnaList(pno).then((data) => setQnaList(data));
  };

  const checkIsMine = (memberEmail) => {
    return !!myEmail && memberEmail === myEmail;
  };

  const handleClickWrite = () => {
    if (!isLoggedIn) {
      alert("로그인이 필요한 기능입니다.");
      return;
    }
    setShowWriteForm(true);
  };

  const handleSubmitNew = (content) => {
    postQna(pno, content).then(() => {
      setShowWriteForm(false);
      fetchQnaList();
    });
  };

  const handleSubmitEdit = (qno, content) => {
    putQna(pno, qno, content).then(() => {
      setEditingQno(null);
      fetchQnaList();
    });
  };

  const handleSubmitReply = (qno) => {
    if (!replyContent.trim()) {
      alert("답변 내용을 입력해주세요.");
      return;
    }
    postQnaReply(pno, qno, replyContent).then(() => {
      setReplyingTo(null);
      setReplyContent("");
      fetchQnaList();
    });
  };

  const handleSubmitEditReply = (qno, content) => {
    putQnaReply(pno, qno, content).then(() => {
      setEditingReplyQno(null);
      fetchQnaList();
    });
  };

  const handleDelete = (qno) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    deleteQna(pno, qno).then(() => fetchQnaList());
  };

  return (
    <div>
      <div className="flex flex-wrap justify-between items-center gap-3 mb-6">
        <span className="inline-block -rotate-1 bg-lavender-light px-3 py-1 font-['Gaegu'] text-[13px] text-lavender-dark">
          궁금한 점 물어보기 {qnaList.length}건
        </span>
        {!showWriteForm && (
          <button
            onClick={handleClickWrite}
            className="h-9 px-5 rounded-full bg-lavender-dark text-white text-xs font-medium"
          >
            문의하기
          </button>
        )}
      </div>

      {showWriteForm && (
        <QnaForm
          onCancel={() => setShowWriteForm(false)}
          onSubmit={handleSubmitNew}
          submitLabel="등록"
        />
      )}

      {qnaList.length === 0 ? (
        <div className="py-16 text-center font-['Gaegu'] text-base text-ink-faint">
          아직 문의가 없어요. 궁금한 점을 편하게 남겨주세요 🤍
        </div>
      ) : (
        <div className="flex flex-col gap-2.5">
          {qnaList.map((qna) => {
            const qnaIsMine = checkIsMine(qna.memberEmail);
            const isExpanded = expandedQnos.has(qna.qno);
            const isEditing = editingQno === qna.qno;
            const hasAnswer = qna.answers?.length > 0;

            return (
              <div
                key={qna.qno}
                className="rounded-xl border border-line bg-white overflow-hidden"
              >
                <button
                  type="button"
                  onClick={() => !isEditing && toggleExpand(qna.qno)}
                  className="w-full flex items-center gap-3 px-4 py-3.5 text-left"
                >
                  <QABadge type="Q" />
                  <p
                    className={`flex-1 text-sm text-ink ${
                      isExpanded ? "" : "line-clamp-1"
                    }`}
                  >
                    {qna.content}
                  </p>
                  {!hasAnswer && (
                    <span className="shrink-0 text-[11px] text-ink-faint bg-cream px-2 py-0.5 rounded-full">
                      답변대기
                    </span>
                  )}
                  <span className="text-xs text-ink-faint shrink-0">
                    {qna.regDate?.slice(0, 10)}
                  </span>
                  <svg
                    viewBox="0 0 24 24"
                    className={`w-4 h-4 shrink-0 text-ink-faint transition-transform ${
                      isExpanded ? "rotate-180" : ""
                    }`}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="m6 9 6 6 6-6" />
                  </svg>
                </button>

                {isExpanded && (
                  <div className="px-4 pb-4 pl-[52px] border-t border-line-soft pt-3">
                    {isEditing ? (
                      <QnaForm
                        initialContent={qna.content}
                        onCancel={() => setEditingQno(null)}
                        onSubmit={(content) =>
                          handleSubmitEdit(qna.qno, content)
                        }
                        submitLabel="수정 완료"
                      />
                    ) : (
                      <>
                        <div className="flex justify-end gap-2 mb-3">
                          {qnaIsMine && (
                            <button
                              onClick={() => setEditingQno(qna.qno)}
                              className="text-xs text-ink-faint underline"
                            >
                              수정
                            </button>
                          )}
                          {(qnaIsMine || isAdmin) && (
                            <button
                              onClick={() => handleDelete(qna.qno)}
                              className="text-xs text-ink-faint underline"
                            >
                              삭제
                            </button>
                          )}
                        </div>

                        {qna.answers?.map((answer) => (
                          <div
                            key={answer.qno}
                            className="flex gap-3 bg-cream rounded-xl p-4 mb-2"
                          >
                            <QABadge type="A" />
                            <div className="flex-1">
                              {editingReplyQno === answer.qno ? (
                                <QnaForm
                                  initialContent={answer.content}
                                  onCancel={() => setEditingReplyQno(null)}
                                  onSubmit={(content) =>
                                    handleSubmitEditReply(answer.qno, content)
                                  }
                                  submitLabel="수정 완료"
                                />
                              ) : (
                                <div className="flex justify-between items-start gap-2">
                                  <p className="text-sm text-ink-soft">
                                    {answer.content}
                                  </p>
                                  {isAdmin && (
                                    <div className="flex gap-2 shrink-0">
                                      <button
                                        onClick={() =>
                                          setEditingReplyQno(answer.qno)
                                        }
                                        className="text-xs text-ink-faint underline"
                                      >
                                        수정
                                      </button>
                                      <button
                                        onClick={() => handleDelete(answer.qno)}
                                        className="text-xs text-ink-faint underline"
                                      >
                                        삭제
                                      </button>
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          </div>
                        ))}

                        {!hasAnswer && (
                          <div className="flex gap-3 bg-cream/60 rounded-xl p-4 mb-2 border border-dashed border-line">
                            <QABadge type="A" />
                            <p className="flex-1 text-sm text-ink-faint font-['Gaegu']">
                              아직 답변 전이에요. 곧 답변드릴게요 🤍
                            </p>
                          </div>
                        )}

                        {isAdmin && !hasAnswer && (
                          <div className="mt-2">
                            {replyingTo === qna.qno ? (
                              <div className="flex flex-col sm:flex-row gap-2">
                                <input
                                  type="text"
                                  value={replyContent}
                                  onChange={(e) =>
                                    setReplyContent(e.target.value)
                                  }
                                  placeholder="답변을 입력하세요"
                                  className="flex-1 border border-line-soft rounded-full px-4 py-1.5 text-xs focus:outline-none focus:border-lavender-dark"
                                />
                                <button
                                  onClick={() => handleSubmitReply(qna.qno)}
                                  className="text-xs px-4 py-1.5 rounded-full bg-lavender-dark text-white self-end sm:self-auto"
                                >
                                  등록
                                </button>
                              </div>
                            ) : (
                              <button
                                onClick={() => setReplyingTo(qna.qno)}
                                className="text-xs text-lavender-dark underline"
                              >
                                답변 달기
                              </button>
                            )}
                          </div>
                        )}
                      </>
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

export default QnaSectionComponent;
