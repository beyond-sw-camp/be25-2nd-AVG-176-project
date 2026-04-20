"""
나노바나나(Gemini) 이미지 번역 - 세마포어 비동기 방식 테스트
- 입력: storage/image 폴더의 이미지들
- 출력: storage/test-image 폴더에 저장
"""

# import asyncio
# import base64
# import os
# import time
# from pathlib import Path

# import requests
# from dotenv import load_dotenv
# from PIL import Image

# load_dotenv()

# GEMINI_API_KEY = os.getenv("NANOBANANA_API_KEY")
# GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent"

# IMAGE_DIR = Path(__file__).parent.parent / "src" / "main" / "resources" / "storage" / "image"
# RESULT_DIR = Path(__file__).parent.parent / "src" / "main" / "resources" / "storage" / "test-image"
# RESULT_DIR.mkdir(parents=True, exist_ok=True)


# async def translate_image_async(semaphore: asyncio.Semaphore, image_path: Path) -> float:
#     """이미지 1장 번역 (비동기 + 세마포어 방식)"""
#     async with semaphore:
#         loop = asyncio.get_event_loop()

#         image_bytes = image_path.read_bytes()
#         image = Image.open(image_path)
#         mime_type = Image.MIME.get(image.format, "image/jpeg")
#         image_b64 = base64.b64encode(image_bytes).decode("utf-8")

#         payload = {
#             "contents": [{
#                 "parts": [
#                     {"inlineData": {"mimeType": mime_type, "data": image_b64}},
#                     {"text": "이 상품 이미지의 텍스트를 한국어로 번역한 새 이미지를 생성해줘. 원본 이미지 레이아웃을 유지하고 영어를 한국어로 바꿔줘."}
#                 ]
#             }],
#             "generationConfig": {"responseModalities": ["TEXT", "IMAGE"]},
#             "safetySettings": [
#                 {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
#                 {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
#                 {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
#                 {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"}
#             ]
#         }

#         start = time.time()
#         response = await loop.run_in_executor(
#             None,
#             lambda: requests.post(f"{GEMINI_API_URL}?key={GEMINI_API_KEY}", json=payload, timeout=480)
#         )
#         response.raise_for_status()
#         elapsed = time.time() - start

#         result = response.json()
#         parts = result["candidates"][0]["content"]["parts"]
#         for part in parts:
#             if "inlineData" in part:
#                 result_b64 = part["inlineData"]["data"]
#                 result_mime = part["inlineData"].get("mimeType", "image/png")
#                 ext = result_mime.split("/")[-1]
#                 out_path = RESULT_DIR / f"async_{image_path.stem}.{ext}"
#                 out_path.write_bytes(base64.b64decode(result_b64))
#                 print(f"  완료: {image_path.name} → {out_path.name} ({elapsed:.1f}초)")

#         return elapsed


# async def main():
#     images = sorted(IMAGE_DIR.glob("*.jpg"))[:6]
#     if not images:
#         print(f"이미지 없음: {IMAGE_DIR}")
#         return

#     print(f"테스트 이미지 {len(images)}장:")
#     for img in images:
#         print(f"  - {img.name}")

#     print("\n" + "="*55)
#     print("[ 세마포어 비동기 방식 (동시 3개 제한) ]")
#     print("="*55)

#     semaphore = asyncio.Semaphore(3)
#     start = time.time()
#     await asyncio.gather(*[translate_image_async(semaphore, img) for img in images])
#     total = time.time() - start

#     print(f"\n총 처리 시간: {total:.1f}초 ({len(images)}장)")
#     print(f"결과 이미지 저장 위치: {RESULT_DIR}")


# if __name__ == "__main__":
#     asyncio.run(main())
