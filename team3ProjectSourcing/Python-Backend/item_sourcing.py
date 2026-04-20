from dotenv import load_dotenv
import asyncio
import aiohttp
import os
import google.generativeai as genai
import json
import time
from fastapi import APIRouter
from pydantic import BaseModel, Field
from typing import List, Optional

load_dotenv()

router = APIRouter()

# 제미나이 키 싹다 넣기.
GEMINI_API_KEY = os.getenv("NANOBANANA_API_KEY")
genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel('gemini-3-flash-preview')

OXYLABS_API_KEY = os.getenv("OXYLABS_API_KEY")
OXYLABS_API_KEY2 = os.getenv("OXYLABS_API_KEY2")
AUTH = aiohttp.BasicAuth(OXYLABS_API_KEY, OXYLABS_API_KEY2)
API_URL = 'https://realtime.oxylabs.io/v1/queries'

# Oxylabs 동시 요청 최대 3개 제한
semaphore = asyncio.Semaphore(3)

# 프롬프트 생성. 어떻게 사진을 제대로 만들건지를 나타내는 프롬프트 만들기.
def get_gemini_query(
    season: str,
    banned_words: Optional[List[str]] = None,
    exclude_keywords: Optional[List[str]] = None,
) -> str:
    """Gemini로 계절에 맞는 아마존 검색 키워드 생성"""
    banned_section = ""
    if banned_words:
        banned_section = f"\n        다음 단어들은 절대 포함하지 마: {', '.join(banned_words)}"

    exclude_section = ""
    if exclude_keywords:
        exclude_section = (
            f"\n        아래에 나온 키워드/상품명과는 다른 종류의 상품만 추천해. (동일하거나 거의 같은 의미면 안 됨):\n        "
            + ", ".join(exclude_keywords)
        )

    prompt = f"""
        내가 계절성을 알려줄테니 내가 미국 아마존에서 가져와서 사용할 물건의 이름을 영어로 알려줘.
        무조건 이름만 알려주면 되는거야.이것들은 그리고 금지어니까 이거 들어간 키워드는 넣지마:{banned_section}
        상품은 하나만 가져와.{exclude_section}

        계절성: {season}
        """
    response = model.generate_content(prompt)
    return response.text.strip()


async def search_asin(session: aiohttp.ClientSession, query: str) -> tuple[dict, str]:
    """1단계: amazon_search로 ASIN 가져오기"""
    async with semaphore:
        payload = {
            'source': 'amazon_search',
            'query': query,
            'domain': 'com',
            'parse': True
        }
        async with session.post(API_URL, json=payload) as resp:
            data = await resp.json()

    results = data.get('results')
    if not results:
        raise ValueError(f"Oxylabs 응답에 results 없음 (레이트리밋 또는 오류): {data}")
    content = results[0].get('content', {})
    organic = content.get('results', {}).get('organic', [])
    if not organic:
        raise ValueError(f"검색 결과 없음: {query}")

    first_item = organic[0]
    asin = first_item.get('asin')
    print(f"  검색 완료: {query} → ASIN: {asin}")
    return first_item, asin


async def get_product_detail(session: aiohttp.ClientSession, asin: str) -> dict:
    """2단계: amazon_product로 상세 정보 가져오기"""
    async with semaphore:
        payload = {
            'source': 'amazon_product',
            'query': asin,
            'domain': 'com',
            'parse': True
        }
        async with session.post(API_URL, json=payload) as resp:
            data = await resp.json()
    results = data.get('results')
    if not results:
        raise ValueError(f"Oxylabs 응답에 results 없음 (레이트리밋 또는 오류): {data}")
    return results[0].get('content', {})


async def get_variation_detail(session: aiohttp.ClientSession, var: dict) -> dict:
    """3단계: variation별 상세 정보 병렬 가져오기"""
    var_asin = var.get('asin')
    async with semaphore:
        payload = {
            'source': 'amazon_product',
            'query': var_asin,
            'domain': 'com',
            'parse': True
        }
        async with session.post(API_URL, json=payload) as resp:
            data = await resp.json()

    results = data.get('results')
    if not results:
        print(f"    variation [{var_asin}] Oxylabs 응답에 results 없음 (레이트리밋 또는 잘못된 ASIN): {data}")
        var_content = {}
    else:
        var_content = results[0].get('content', {})
    print(f"    variation [{var_asin}] 조회 완료")
    return {
        'asin': var_asin,
        'dimensions': var.get('dimensions', {}),
        'selected': var.get('selected', False),
        'price': var_content.get('price'),
        'currency': var_content.get('currency'),
        'stock': var_content.get('stock'),
        'rating': var_content.get('rating'),
        'reviews_count': var_content.get('reviews_count'),
        'images': var_content.get('images', []),
    }


async def source_one_item(session: aiohttp.ClientSession, keyword: str) -> dict:
    """키워드 하나에 대한 전체 소싱 파이프라인 (test.json과 동일한 필드 구조로 병합)."""
    print(f"\n[시작] {keyword}")

    # 1, 2단계는 순차 실행 (ASIN이 있어야 상세 조회 가능)
    first_item, asin = await search_asin(session, keyword)
    product_content = await get_product_detail(session, asin)

    # 검색 organic + amazon_product 상세를 합쳐 test.json 예시와 같은 키 세트를 갖춤
    merged = dict(first_item)
    for k, v in product_content.items():
        if k == 'variation':
            continue
        if v is not None:
            merged[k] = v
    merged['images'] = product_content.get('images') or merged.get('images', [])
    variations = product_content.get('variation') or []

    # 3단계: variation은 asyncio.gather로 병렬 처리
    if variations:
        detailed_variations = await asyncio.gather(*[
            get_variation_detail(session, var) for var in variations
        ])
        merged['variation'] = list(detailed_variations)
    else:
        merged['variation'] = []

    print(f"[완료] {keyword}")
    return merged




# ── FastAPI 엔드포인트 ──────────────────────────────────────────

class SourcingRequest(BaseModel):
    seasons: List[str]
    banned_words: List[str] = []
    # 계절성 태그 하나당 가져올 상품(키워드) 개수
    item_count: int = Field(default=1, ge=1, le=10)

# 요청시 소싱 시작.
@router.post("/sourcing")
async def run_sourcing(req: SourcingRequest):
    print(
        f"[FastAPI] 소싱 요청 - 계절성: {req.seasons}, 금지어: {req.banned_words}, "
        f"시즌당 상품 수: {req.item_count}"
    )
    print("Gemini 키워드 생성 중...")

    keywords: List[str] = []
    for season in req.seasons:
        used_for_season: List[str] = []
        for _ in range(req.item_count):
            kw = get_gemini_query(season, req.banned_words, used_for_season or None)
            keywords.append(kw)
            used_for_season.append(kw)
    print(f"추천 키워드: {keywords}")

    start = time.time()
    async with aiohttp.ClientSession(auth=AUTH) as session:
        results = await asyncio.gather(*[
            source_one_item(session, keyword) for keyword in keywords
        ])

    elapsed = time.time() - start
    print(f"=== 총 처리 시간: {elapsed:.1f}초 ===")

    result_list = list(results)
    return {
        "status": "success",
        "keywords": keywords,
        "item_count": req.item_count,
        "elapsed": round(elapsed, 1),
        "results": result_list,
        # test.json과 동일한 객체 배열을 문자열로도 제공 (클라이언트/저장용)
        "results_json": json.dumps(result_list, ensure_ascii=False, indent=2),
    }





# 여기는 그냥 비교용 바로 삭제 하면 됨. 비교용 코드
# def source_one_item_sync(keyword: str) -> dict:
#     """순차 방식 소싱 (비교용)"""
#     import requests
#     auth = ('calendar_CjSKG', 'DBy6x9xqL_Hv')
#     print(f"\n[순차 시작] {keyword}")

#     # 1단계: ASIN 검색
#     search_resp = requests.post(API_URL, auth=auth, json={
#         'source': 'amazon_search', 'query': keyword, 'domain': 'com', 'parse': True
#     })
#     content = search_resp.json()['results'][0].get('content', {})
#     organic = content.get('results', {}).get('organic', [])
#     first_item = organic[0]
#     asin = first_item.get('asin')
#     print(f"  검색 완료: {keyword} → ASIN: {asin}")

#     # 2단계: 상품 상세
#     product_resp = requests.post(API_URL, auth=auth, json={
#         'source': 'amazon_product', 'query': asin, 'domain': 'com', 'parse': True
#     })
#     product_content = product_resp.json()['results'][0].get('content', {})
#     first_item['images'] = product_content.get('images', [])
#     variations = product_content.get('variation', [])

#     # 3단계: variation 순차 처리 (하나씩)
#     detailed_variations = []
#     for var in variations:
#         var_asin = var.get('asin')
#         var_resp = requests.post(API_URL, auth=auth, json={
#             'source': 'amazon_product', 'query': var_asin, 'domain': 'com', 'parse': True
#         })
#         var_content = var_resp.json()['results'][0].get('content', {})
#         detailed_variations.append({
#             'asin': var_asin,
#             'dimensions': var.get('dimensions', {}),
#             'selected': var.get('selected', False),
#             'price': var_content.get('price'),
#             'currency': var_content.get('currency'),
#             'stock': var_content.get('stock'),
#             'rating': var_content.get('rating'),
#             'reviews_count': var_content.get('reviews_count'),
#             'images': var_content.get('images', []),
#         })
#         print(f"    variation [{var_asin}] 조회 완료")

#     first_item['variation'] = detailed_variations
#     print(f"[순차 완료] {keyword}")
#     return first_item


# def main_sync(keywords: list[str]):
#     """순차 방식 전체 실행"""
#     print("\n" + "="*50)
#     print("[ 순차 방식 (비교용) ]")
#     print("="*50)

#     start = time.time()
#     results = [source_one_item_sync(k) for k in keywords]
#     elapsed = time.time() - start

#     print(f"\n>>> 순차 처리 시간: {elapsed:.1f}초 ({len(keywords)}개 상품)")
#     return results, elapsed


# async def main_async(keywords: list[str]):
#     """비동기 방식 전체 실행"""
#     print("\n" + "="*50)
#     print("[ 비동기 + 세마포어 방식 ]")
#     print("="*50)

#     start = time.time()
#     async with aiohttp.ClientSession(auth=AUTH) as session:
#         results = await asyncio.gather(*[
#             source_one_item(session, keyword) for keyword in keywords
#         ])
#     elapsed = time.time() - start

#     print(f"\n>>> 비동기 처리 시간: {elapsed:.1f}초 ({len(keywords)}개 상품)")
#     return list(results), elapsed


# async def main():
#     seasons = ["1월", "7월"]
#     print("Gemini 키워드 생성 중...")
#     keywords = [get_gemini_query(season) for season in seasons]
#     print(f"추천 키워드: {keywords}")

#     # 순차 방식 먼저 실행
#     _, sync_time = main_sync(keywords)

#     # 비동기 방식 실행
#     _, async_time = await main_async(keywords)

#     # 결과 비교
#     print("\n" + "="*50)
#     print("[ 성능 비교 결과 ]")
#     print("="*50)
#     print(f"  순차 방식:          {sync_time:.1f}초")
#     print(f"  비동기+세마포어:    {async_time:.1f}초")
#     print(f"  단축 시간:          {sync_time - async_time:.1f}초")
#     print(f"  성능 향상:          {((sync_time - async_time) / sync_time * 100):.1f}%")


# if __name__ == "__main__":
#     asyncio.run(main())
