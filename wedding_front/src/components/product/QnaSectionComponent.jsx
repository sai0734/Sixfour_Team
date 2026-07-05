import { useEffect, useState } from "react";
import {
  getQnaList,
  postQna,
  putQna,
  postQnaReply,
  putQnaReply,
  deleteQna,
} from "../../api/qnaApi";

// 질문/답변 공용 폼
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
    <div className="border border-line rounded-2xl p-5 mb-6">
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={3}
        placeholder="상품에 대해 궁금한 점을 남겨주세요."
        className="w-full border border-line-soft rounded-xl p-3 text-sm focus:outline-none focus:border-brand resize-none"
      />
      <div className="flex justify-end gap-2 mt-3">
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

const QnaSectionComponent = ({ pno, isLoggedIn, isAdmin, myEmail }) => {
  const [qnaList, setQnaList] = useState([]);
  const [showWriteForm, setShowWriteForm] = useState(false);
  const [editingQno, setEditingQno] = useState(null);
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState("");
  const [editingReplyQno, setEditingReplyQno] = useState(null);

  useEffect(() => {
    fetchQnaList();
  }, [pno]);

  const fetchQnaList = () => {
    getQnaList(pno).then((data) => setQnaList(data));
  };

  // 서버가 아닌 프론트에서 memberEmail을 직접 비교
  // (Q&A 목록 조회는 비회원도 가능한 공개 API라 서버가 로그인 여부를 못 읽기 때문)
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
      <div className="flex justify-between items-center mb-6">
        <p className="text-sm text-ink-soft">총 {qnaList.length}개의 문의</p>
        {!showWriteForm && (
          <button
            onClick={handleClickWrite}
            className="h-9 px-5 rounded-full bg-brand text-white text-xs font-medium"
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
        <div className="py-16 text-center text-ink-faint text-sm">
          아직 등록된 문의가 없습니다.
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          {qnaList.map((qna) => {
            const qnaIsMine = checkIsMine(qna.memberEmail);

            return (
              <div key={qna.qno} className="border-b border-line pb-6">
                {editingQno === qna.qno ? (
                  <QnaForm
                    initialContent={qna.content}
                    onCancel={() => setEditingQno(null)}
                    onSubmit={(content) => handleSubmitEdit(qna.qno, content)}
                    submitLabel="수정 완료"
                  />
                ) : (
                  <>
                    <div className="flex justify-between items-start">
                      <p className="text-sm">{qna.nickname}</p>
                      <div className="text-right">
                        <p className="text-xs text-ink-faint">
                          {qna.regDate?.slice(0, 10)}
                        </p>
                        {qnaIsMine && (
                          <button
                            onClick={() => setEditingQno(qna.qno)}
                            className="text-xs text-ink-faint underline mt-1 mr-2"
                          >
                            수정
                          </button>
                        )}
                        {(qnaIsMine || isAdmin) && (
                          <button
                            onClick={() => handleDelete(qna.qno)}
                            className="text-xs text-ink-faint underline mt-1"
                          >
                            삭제
                          </button>
                        )}
                      </div>
                    </div>

                    <p className="text-sm text-ink-soft mt-2">{qna.content}</p>

                    {qna.answers?.map((answer) => (
                      <div
                        key={answer.qno}
                        className="bg-cream rounded-xl p-4 mt-3 ml-4"
                      >
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
                          <>
                            <div className="flex justify-between items-start">
                              <p className="text-xs font-medium text-brand-accent">
                                답변
                              </p>
                              {isAdmin && (
                                <div className="flex gap-2">
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
                            <p className="text-sm text-ink-soft mt-1">
                              {answer.content}
                            </p>
                          </>
                        )}
                      </div>
                    ))}

                    {isAdmin && qna.answers?.length === 0 && (
                      <div className="ml-4 mt-3">
                        {replyingTo === qna.qno ? (
                          <div className="flex gap-2">
                            <input
                              type="text"
                              value={replyContent}
                              onChange={(e) => setReplyContent(e.target.value)}
                              placeholder="답변을 입력하세요"
                              className="flex-1 border border-line-soft rounded-full px-4 py-1.5 text-xs focus:outline-none focus:border-brand"
                            />
                            <button
                              onClick={() => handleSubmitReply(qna.qno)}
                              className="text-xs px-4 rounded-full bg-brand text-white"
                            >
                              등록
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => setReplyingTo(qna.qno)}
                            className="text-xs text-brand-accent underline"
                          >
                            답변 달기
                          </button>
                        )}
                      </div>
                    )}
                  </>
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
