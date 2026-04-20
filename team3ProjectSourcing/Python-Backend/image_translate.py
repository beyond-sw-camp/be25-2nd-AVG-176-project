import os
import base64
import requests
import time
from io import BytesIO
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv
from PIL import Image
from playwright.sync_api import sync_playwright

# 분리된 텍스트 번역 라우터 임포트
from text_translate import router as text_router
from item_sourcing import router as sourcing_router

"""
각자의 API키를 사용하도록 합시다!!!!!
*** 결제 수단이 등록되지 않은 새 프로젝트의 API 키를 사용하면 하루 최대 500장까지 무료로 사용할 수 있습니다. ***
"""
load_dotenv()

app = FastAPI()

# 텍스트 번역 라우터 추가 (이제 /translate_text 경로도 여기서 처리됨)
app.include_router(text_router)
app.include_router(sourcing_router)

# NANOBANANA_API_KEY = os.getenv("NANOBANANA_API_KEY")
NANOBANANA_API_KEY = os.getenv("NANOBANANA_API_KEY") # 여기에 자신의 geminiAI API키를 사용해주세요
NANOBANANA_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent"

# NANOBANANA_API_URL = os.getenv("NANOBANANA_API_URL")

# 원본 이미지 저장 경로
IMAGE_SAVE_DIR = Path(__file__).parent.parent / "src" / "main" / "resources" / "storage" / "image"
IMAGE_SAVE_DIR.mkdir(parents=True, exist_ok=True)

# 번역 결과 이미지 저장 경로
RESULT_IMAGE_SAVE_DIR = Path(__file__).parent.parent / "src" / "main" / "resources" / "storage" / "resultImage"
RESULT_IMAGE_SAVE_DIR.mkdir(parents=True, exist_ok=True)


# Spring Boot에서 받을 요청 형태
class TranslateRequest(BaseModel):
    image_url: str


# Spring Boot에 돌려줄 응답 형태
class TranslateResponse(BaseModel):
    local_image_path: str
    result_image_path: str

# 이미지 확인하고 파악하는데 꼭 필요한 기능.
def download_image_playwright(image_url: str) -> bytes:
    """Playwright로 실제 브라우저처럼 이미지 다운로드 (Amazon CDN 차단 우회)"""
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        page = context.new_page()

        # Amazon 메인 페이지 방문 → 쿠키 획득
        page.goto("https://www.amazon.com", timeout=15000)
        page.wait_for_timeout(2000)

        # Playwright 내장 HTTP 클라이언트로 이미지 요청 (쿠키 자동 포함)
        response = context.request.get(
            image_url,
            headers={"Referer": "https://www.amazon.com/"}
        )

        if not response.ok:
            raise Exception(f"이미지 요청 실패: HTTP {response.status}")

        image_bytes = response.body()
        browser.close()
        return image_bytes


# 이미지 번역하는 나노바나나 사용하는 함수.
@app.post("/translateImage", response_model=TranslateResponse)
def translate(req: TranslateRequest):
    # 1. Playwright로 이미지 다운로드
    try:
        image_bytes = download_image_playwright(req.image_url)
        image = Image.open(BytesIO(image_bytes))
        mime_type = Image.MIME.get(image.format, "image/jpeg")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"이미지 다운로드 실패: {str(e)}")

    # 2. 원본 이미지 로컬 저장
    filename = Path(req.image_url.split("?")[0]).name
    save_path = IMAGE_SAVE_DIR / filename
    save_path.write_bytes(image_bytes)
    local_image_path = f"/storage/image/{filename}"
    result_image_path = local_image_path  # Gemini 실패 시 원본으로 대체

    # 3. Gemini API 호출 (nanoBanana)
    try:
        image_b64 = base64.b64encode(image_bytes).decode("utf-8")

        payload = {
            "contents": [{
                "parts": [
                    {"inlineData": {"mimeType": mime_type, "data": image_b64}},
                    {"text": (
                        f"이 상품 이미지의 텍스트를 한국어로 번역한 새 이미지를 생성해줘. 근데 상품에 있는 텍스트는 번역하지말아줘. "
                        f"원본 이미지 레이아웃을 유지하고 영어를 한국어로 바꿔줘."
                    )}
                ]
            }],
            "generationConfig": {
                "responseModalities": ["TEXT", "IMAGE"]
            },
            # 여기는 잔인함, 선정적 등등 다양한 것들 못하게 막기.
            "safetySettings": [
                {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
                {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
                {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
                {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"}
            ]
        }

        nano_response = None
        max_retries = 4
        for attempt in range(max_retries):
            try:
                nano_response = requests.post(
                    f"{NANOBANANA_API_URL}?key={NANOBANANA_API_KEY}",
                    json=payload,
                    timeout=240
                )
            except requests.exceptions.Timeout:
                wait_time = 2 ** (attempt + 1)
                print(f"Gemini 응답 타임아웃. {wait_time:.1f}s 후 재시도 ({attempt + 1}/{max_retries})")
                if attempt < max_retries - 1:
                    time.sleep(wait_time)
                    continue
                raise

            if nano_response.status_code == 429:
                wait_time = 2 ** (attempt + 1)
                try:
                    error_data = nano_response.json()
                    details = error_data.get('error', {}).get('details', [])
                    for detail in details:
                        if detail.get('@type') == 'type.googleapis.com/google.rpc.RetryInfo':
                            delay_str = detail.get('retryDelay', '0s').replace('s', '')
                            wait_time = float(delay_str) + 1
                            break
                except Exception:
                    pass
                print(f"Quota 초과 (429). {wait_time:.1f}초 후 재시도합니다... ({attempt + 1}/{max_retries})")
                time.sleep(wait_time)
                continue

            if nano_response.status_code == 503:
                wait_time = 1.5 * (2 ** attempt)
                print(f"Gemini 응답 503, {nano_response.text[:120]}… → {wait_time:.1f}s 후 재시도 ({attempt + 1}/{max_retries})")
                if attempt < max_retries - 1:
                    time.sleep(wait_time)
                    continue
                nano_response.raise_for_status()

            nano_response.raise_for_status()
            break

        result = nano_response.json()

        if "candidates" not in result or not result["candidates"]:
            print(f"Gemini 응답 오류 (candidates 없음): {result}")
            raise ValueError("Gemini가 유효한 후보(candidates)를 반환하지 않았습니다. (Safety Filter 차단 등)")

        cand0 = result["candidates"][0]
        content = cand0.get("content") if isinstance(cand0, dict) else None
        if not isinstance(content, dict):
            print(f"Gemini 응답 오류 (content 없음): {result}")
            raise ValueError("Gemini candidates[0].content가 없습니다.")
        parts = content.get("parts")
        if not parts:
            print(f"Gemini 응답 오류 (parts 없음): {result}")
            raise ValueError("Gemini content.parts가 없습니다.")

        # 디버그: Gemini 응답 구조 확인
        print(f"=== Gemini 응답 parts 수: {len(parts)} ===")
        for i, part in enumerate(parts):
            print(f"part[{i}] keys: {list(part.keys())}")

        for part in parts:
            if "inlineData" in part:
                result_image_b64 = part["inlineData"]["data"]
                result_mime = part["inlineData"].get("mimeType", "image/png")
                ext = result_mime.split("/")[-1]
                result_filename = f"result_{filename.rsplit('.', 1)[0]}.{ext}"
                result_save_path = RESULT_IMAGE_SAVE_DIR / result_filename
                result_save_path.write_bytes(base64.b64decode(result_image_b64))
                result_image_path = f"/storage/resultImage/{result_filename}"
                print(f"결과 이미지 저장 완료: {result_image_path}")

    except Exception as e:
        error_msg = str(e)
        if NANOBANANA_API_KEY:
            error_msg = error_msg.replace(NANOBANANA_API_KEY, "***")
        print(f"Gemini 호출 실패: {type(e).__name__}: {error_msg}")

    return TranslateResponse(local_image_path=local_image_path, result_image_path=result_image_path)



@app.get("/health")
def health():
    return {"status": "ok"}


# 실제 서버 실행.
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
