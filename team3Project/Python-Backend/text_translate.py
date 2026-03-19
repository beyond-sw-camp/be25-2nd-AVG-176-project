from fastapi import APIRouter, HTTPException
import os
import requests
from pydantic import BaseModel
from dotenv import load_dotenv


"""
여기는 제목 및 옵션등 텍스트들 싹다 번역하는곳.
"""

# .env 실행시켜 가져오기.
load_dotenv()


# 메인 앱이 아니라 라우터(부품)로 변경
router = APIRouter()

# image_translate.py에서 사용하는 것과 동일한 API 키 환경변수를 사용
GEMINI_API_KEY = os.getenv("NANOBANANA_API_KEY") 
GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

@router.get("/text_health")
def health():
    return {"status": "ok"}

class TranslateTextRequest(BaseModel):
    text: str
    target_lang: str = "KO"

class TranslateTextResponse(BaseModel):
    translated_text: str


@router.post("/translate_text")
def translate_text(req: TranslateTextRequest):
    if not GEMINI_API_KEY:
        print("GEMINI API 키가 설정되지 않았습니다")
        return TranslateTextResponse(translated_text = req.text)
    
    try:
        # 프롬프트를 통해 사족 없이 번역 결과만 출력하도록 유도
        prompt = f"Translate the following text to {req.target_lang}. Only provide the translated text without any additional comments.\n\nText: {req.text}"
        
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.1 # 일관된 번역 결과를 위해 낮은 temperature 설정
            }
        }

        url = f"{GEMINI_API_URL}?key={GEMINI_API_KEY}"
        response = requests.post(url, json=payload)

         # 에러 발생 시 원인을 정확히 파악하기 위해 응답 본문 출력
        if not response.ok:
            print(f"Gemini API 에러 상세 내용: {response.text}")

        response.raise_for_status()

        result = response.json()
        translated_text = result["candidates"][0]["content"]["parts"][0]["text"].strip()
        
        return TranslateTextResponse(translated_text = translated_text)
    
    except Exception as e:
        print("텍스트 번역 실패")
        raise HTTPException(status_code = 500)
        
