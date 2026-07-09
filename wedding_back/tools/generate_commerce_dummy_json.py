#!/usr/bin/env python3
"""member.json ACTIVE 회원 기준 commerce_dummy.json 생성기."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src" / "main" / "resources" / "data"

REVIEW_TEXTS = [
    "포장이 깔끔하고 하객분들 반응이 좋았어요.",
    "실물 색감이 사진과 비슷해서 만족합니다.",
    "배송도 빠르고 구성품도 알차요.",
    "답례품으로 무난하게 쓰기 좋았습니다.",
    "수량 대비 가격이 괜찮아요.",
    "디자인이 심플해서 호불호 없이 좋아요.",
    "조금 아쉬운 점도 있지만 전반적으로 만족합니다.",
    "재구매 의사 있어요. 추천합니다.",
]
REVIEW_REPLIES = [
    "소중한 후기 감사합니다. 앞으로도 좋은 상품으로 보답하겠습니다.",
    "구매해 주셔서 감사합니다. 이용 중 불편하신 점은 언제든 문의해 주세요.",
    "상세한 리뷰 남겨주셔서 감사합니다!",
]
QNA_QUESTIONS = [
    "배송은 보통 며칠 정도 걸리나요?",
    "대량 주문도 가능한가요?",
    "포장 상태는 어떤가요?",
    "색상이 화면과 많이 다른 편인가요?",
    "교환/환불 정책이 궁금합니다.",
    "웨딩 답례품으로 무난할까요?",
    "유통기한/사용기한 확인 부탁드립니다.",
    "구성품 구성이 어떻게 되나요?",
]
QNA_ANSWERS = [
    "안녕하세요. 평균 2~3일 내 출고되며, 지역에 따라 1~2일 추가 소요될 수 있습니다.",
    "네, 대량 주문 가능합니다. 고객센터로 수량 알려주시면 안내드리겠습니다.",
    "완충재와 박스 포장으로 발송되고 있습니다.",
    "교환/환불은 미개봉/미사용 상태에서 접수 가능합니다.",
]

PRODUCT_COUNT = 31
ADMIN_EMAIL = "user1@naver.com"


def user_index(email: str) -> int:
    try:
        return int(email.replace("user", "").split("@")[0])
    except ValueError:
        return 0


def pick_status(user_idx: int, order_seq: int) -> str:
    return ["DELIVERED", "SHIPPING", "PAID"][(user_idx + order_seq) % 3]


def main() -> None:
    members = json.loads((DATA / "member.json").read_text(encoding="utf-8"))
    actives = [m for m in members if m.get("status") == "ACTIVE"]

    orders = []
    reviews = []
    qnas = []
    review_seq = 0
    qna_seq = 0

    for m in actives:
        email = m["email"]
        idx = user_index(email)
        order_count = 2 + (idx % 3)

        for o in range(order_count):
            item_count = 1 + ((idx + o) % 3)
            status = pick_status(idx, o)
            items = []
            for j in range(item_count):
                pno = ((idx + o + j) % PRODUCT_COUNT) + 1
                qty = 1 + ((idx + j) % 2)
                items.append({"productPno": pno, "qty": qty})

            order_number = f"ORD-{idx:03d}-{o + 1:02d}"
            orders.append({
                "memberEmail": email,
                "orderNumber": order_number,
                "orderStatus": status,
                "shippingFee": 3000,
                "zipcode": "06234",
                "address": "서울특별시 강남구 테헤란로 123",
                "addressDetail": f"더미아파트 {idx}동",
                "items": items,
            })

            if status not in ("DELIVERED", "PAID"):
                continue

            for item in items:
                if review_seq % 10 >= 7:
                    review_seq += 1
                    continue
                review = {
                    "orderNumber": order_number,
                    "productPno": item["productPno"],
                    "rating": 3 + (review_seq % 3),
                    "content": REVIEW_TEXTS[review_seq % len(REVIEW_TEXTS)],
                }
                if review_seq % 3 == 0:
                    review["adminReply"] = REVIEW_REPLIES[review_seq % len(REVIEW_REPLIES)]
                reviews.append(review)
                review_seq += 1

        question_count = 1 + (idx % 3)
        for q in range(question_count):
            pno = ((idx + q) % PRODUCT_COUNT) + 1
            qna = {
                "memberEmail": email,
                "productPno": pno,
                "content": QNA_QUESTIONS[(idx + q) % len(QNA_QUESTIONS)],
            }
            if qna_seq % 2 == 0:
                qna["adminAnswer"] = QNA_ANSWERS[qna_seq % len(QNA_ANSWERS)]
            qnas.append(qna)
            qna_seq += 1

    payload = {
        "meta": {
            "version": 1,
            "adminEmail": ADMIN_EMAIL,
            "description": "답례품 주문/리뷰/Q&A 더미 (자동 생성)",
        },
        "orders": orders,
        "reviews": reviews,
        "qnas": qnas,
    }

    out = DATA / "commerce_dummy.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"written: {out}")
    print(f"orders={len(orders)}, reviews={len(reviews)}, qnas={len(qnas)}")


if __name__ == "__main__":
    main()
