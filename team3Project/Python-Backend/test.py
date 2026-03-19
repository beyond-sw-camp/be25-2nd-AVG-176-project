import requests
import json

AUTH = ('calendar_CjSKG', 'DBy6x9xqL_Hv')
API_URL = 'https://realtime.oxylabs.io/v1/queries'

# 1단계: amazon_search로 검색하여 첫 번째 상품의 ASIN 가져오기
search_payload = {
    'source': 'amazon_search',
    'query': 'Coca-Cola Zero Sugar Soda',
    'domain': 'com',
    'parse': True
}

search_response = requests.request('POST', API_URL, auth=AUTH, json=search_payload)
search_data = search_response.json()

if not (search_data and 'results' in search_data and search_data['results']):
    print("검색 API 응답에서 'results'를 찾을 수 없습니다.")
    exit()

content = search_data['results'][0].get('content', {})
organic = content.get('results', {}).get('organic', [])

if not organic:
    print("검색 결과('organic' 리스트)를 찾을 수 없습니다.")
    exit()

first_item = organic[0]
asin = first_item.get('asin')
print(f"=== 검색된 ASIN: {asin} ===")

# 2단계: amazon_product로 상세 정보(이미지 포함) 가져오기
product_payload = {
    'source': 'amazon_product',
    'query': asin,
    'domain': 'com',
    'parse': True
}

product_response = requests.request('POST', API_URL, auth=AUTH, json=product_payload)
product_data = product_response.json()

product_content = product_data['results'][0].get('content', {})

# 검색 결과 + 상세 이미지 + 옵션 합치기
first_item['images'] = product_content.get('images', [])
variations = product_content.get('variation', [])

# 3단계: 각 옵션(variation)별로 amazon_product 재조회하여 가격/재고 추가
detailed_variations = []
for var in variations:
    var_asin = var.get('asin')
    var_payload = {
        'source': 'amazon_product',
        'query': var_asin,
        'domain': 'com',
        'parse': True
    }
    var_response = requests.request('POST', API_URL, auth=AUTH, json=var_payload)
    var_content = var_response.json()['results'][0].get('content', {})

    detailed_variations.append({
        'asin': var_asin,
        'dimensions': var.get('dimensions', {}),
        'selected': var.get('selected', False),
        'price': var_content.get('price'),
        'currency': var_content.get('currency'),
        'stock': var_content.get('stock'),
        'rating': var_content.get('rating'),
        'reviews_count': var_content.get('reviews_count'),
        'images': var_content.get('images', []),
    })
    print(f"  - [{var_asin}] 조회 완료: {var.get('dimensions')}")

first_item['variation'] = detailed_variations

print("\n=== 최종 결과 (옵션별 가격/재고 포함) ===")
print(json.dumps(first_item, indent=2, ensure_ascii=False))