import json
import time
from typing import Optional

import os
import requests
from dotenv import load_dotenv
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

"""
여기는 제목 및 옵션등 텍스트들 싹다 번역하는곳.
"""

load_dotenv()

router = APIRouter()

GEMINI_API_KEY = os.getenv("NANOBANANA_API_KEY")
GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
# 2.5-flash가 503/429 재시도 후에도 실패하면 1.5-flash로 폴백
GEMINI_FALLBACK_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

# 모델당 최대 재시도 횟수 (2.5-flash 2회 → 실패 시 1.5-flash 2회)
_MAX_RETRIES_PER_MODEL = 2
_INITIAL_BACKOFF_SEC = 1.5
_REQUEST_TIMEOUT_SEC = 120


@router.get("/text_health")
def health():
    return {"status": "ok"}


class TranslateTextRequest(BaseModel):
    text: str
    target_lang: str = "KO"


class TranslateTextResponse(BaseModel):
    translated_text: str


def _gemini_error_message(response: requests.Response) -> str:
    try:
        body = response.json()
        err = body.get("error") or {}
        return err.get("message") or response.text[:500]
    except (json.JSONDecodeError, ValueError):
        return response.text[:500] or f"HTTP {response.status_code}"


def _post_gemini_with_retry(url: str, payload: dict) -> requests.Response:
    """단일 모델 URL에 대해 재시도. 503/429면 지수 백오프 후 재시도."""
    last: Optional[requests.Response] = None
    for attempt in range(_MAX_RETRIES_PER_MODEL):
        last = requests.post(url, json=payload, timeout=_REQUEST_TIMEOUT_SEC)
        if last.ok:
            return last
        if last.status_code in (429, 503):
            wait = _INITIAL_BACKOFF_SEC * (2 ** attempt)
            print(
                f"Gemini 응답 {last.status_code}, {_gemini_error_message(last)[:120]}… "
                f"→ {wait:.1f}s 후 재시도 ({attempt + 1}/{_MAX_RETRIES_PER_MODEL})"
            )
            time.sleep(wait)
            continue
        break
    assert last is not None
    return last


def _post_gemini_with_fallback(payload: dict) -> tuple[requests.Response, str]:
    """2.5-flash 우선 시도 → 503/429 지속 시 1.5-flash로 폴백.
    반환값: (response, 실제로 사용된 모델명)
    """
    primary_url = f"{GEMINI_API_URL}?key={GEMINI_API_KEY}"
    fallback_url = f"{GEMINI_FALLBACK_URL}?key={GEMINI_API_KEY}"

    response = _post_gemini_with_retry(primary_url, payload)
    if response.ok:
        return response, "gemini-2.5-flash"

    # 2.5-flash가 503/429로 모두 실패한 경우에만 폴백
    if response.status_code in (429, 503):
        print(
            f"gemini-2.5-flash 재시도 모두 실패({response.status_code}), "
            f"gemini-1.5-flash로 폴백합니다."
        )
        response = _post_gemini_with_retry(fallback_url, payload)
        return response, "gemini-1.5-flash"

    # 그 외 에러(400, 401 등)는 폴백 없이 그대로 반환
    return response, "gemini-2.5-flash"


@router.post("/translate_text")
def translate_text(req: TranslateTextRequest):
    if not GEMINI_API_KEY:
        print("GEMINI API 키가 설정되지 않았습니다")
        return TranslateTextResponse(translated_text=req.text)

    prompt = (
        f"Translate the following text to {req.target_lang}. "
        f"Only provide the translated text without any additional comments.\n\nText: {req.text}"
    )
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"temperature": 0.1},
    }

    try:
        response, used_model = _post_gemini_with_fallback(payload)
        if used_model != "gemini-2.5-flash":
            print(f"[폴백] {used_model} 으로 번역 처리됨")
        if not response.ok:
            msg = _gemini_error_message(response)
            print(f"Gemini API 에러(재시도·폴백 후에도 실패): {response.status_code} {msg}")
            # 구글 쪽 일시 과부하·한도 → 클라이언트도 재시도 가능하도록 503
            if response.status_code in (429, 503):
                raise HTTPException(
                    status_code=503,
                    detail="Gemini 일시 과부하 또는 할당 한도입니다. 잠시 후 다시 시도하세요. "
                    + msg,
                )
            raise HTTPException(
                status_code=502,
                detail=f"Gemini API 오류: {msg}",
            )

        result = response.json()
        translated_text = result["candidates"][0]["content"]["parts"][0]["text"].strip()
        return TranslateTextResponse(translated_text=translated_text)

    except HTTPException:
        raise
    except (KeyError, IndexError) as e:
        print(f"텍스트 번역 응답 파싱 실패: {e}")
        raise HTTPException(
            status_code=502,
            detail="Gemini 응답 형식이 예상과 다릅니다.",
        ) from e
    except requests.RequestException as e:
        print(f"텍스트 번역 네트워크 오류: {e}")
        raise HTTPException(
            status_code=503,
            detail=f"Gemini 연결 실패: {e!s}",
        ) from e
