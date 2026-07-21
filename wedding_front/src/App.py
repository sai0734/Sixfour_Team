import base64
import io
from typing import Optional

from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from PIL import Image

app = FastAPI(title="CatVTON Try-On API")

# pipeline = CatVTONPipeline(
#     attn_ckpt_version="vitonhd",
#     attn_ckpt="...",
#     base_ckpt="...",
#     weight_dtype=torch.float16,
#     device="cuda",
#     skip_safety_check=True,
# )
pipeline = None  # TODO: 위 주석 해제 후 실제 초기화


async def load_image(upload: UploadFile, mode: str = "RGB") -> Image.Image:
    data = await upload.read()
    return Image.open(io.BytesIO(data)).convert(mode)


def make_mask(person_img: Image.Image) -> Image.Image:
    """
    CatVTON은 mask가 필요합니다.
    - AutoMask / DensePose 등으로 생성하거나
    - 임시로 전신 흰 마스크(테스트용)를 쓸 수 있음
    """
    # TODO: 실제 AutoMask 연동
    return Image.new("L", person_img.size, 255)


@app.get("/health")
def health():
    return {"status": "ok", "pipeline_ready": pipeline is not None}


@app.post("/try-on")
async def try_on(
    person: UploadFile = File(...),
    cloth: UploadFile = File(...),
    cloth_type: str = Form("overall"),
    mask: Optional[UploadFile] = File(None),
):
    if pipeline is None:
        raise HTTPException(status_code=503, detail="CatVTON pipeline not loaded")

    person_img = await load_image(person, "RGB")
    cloth_img = await load_image(cloth, "RGB")

    if mask is not None:
        mask_img = await load_image(mask, "L")
    else:
        mask_img = make_mask(person_img)

    try:
        results = pipeline(
            image=person_img,
            condition_image=cloth_img,
            mask=mask_img,
            num_inference_steps=50,
            guidance_scale=2.5,
        )
        result_img = results[0]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"inference failed: {e}")

    buf = io.BytesIO()
    result_img.save(buf, format="PNG")
    b64 = base64.b64encode(buf.getvalue()).decode("utf-8")

    return JSONResponse({"result_base64": b64})