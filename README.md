# be25-2nd-avg(height)=174-project

# 업무 자동화 시스템
<img width="1435" height="690" alt="스크린샷 2026-04-16 시간: 16 07 39" src="https://github.com/user-attachments/assets/55df618a-f32d-4422-a822-97d0d49c901c" />

## 👥 팀원 소개

| 민정기 (팀장) | 김지연 (기획, 로그인) | 이진(가공, 프론트엔드) | 이용호(배송,결제) | 현재진(배송,결제)|
| --- | --- | --- | --- | --- |
|<img width="100" height="100" alt="image" src="https://github.com/user-attachments/assets/c5f9b4e1-5ff7-474c-8b7a-cb24c0aa5dff" /> |<img width="200" height="100" alt="image" src="https://github.com/user-attachments/assets/aa34b18a-3661-48e3-87e1-4f59fe0cb204" />|<img width="100" height="100" alt="image" src="https://github.com/user-attachments/assets/3a124a8c-f725-4d31-808a-c848257d795b" />|<img width="100" height="100" alt="image" src="https://github.com/user-attachments/assets/a8fe86da-a741-4b58-85ef-a4fbb1a4777f" />|<img width="100" height="100" alt="image" src="https://github.com/user-attachments/assets/7de0301e-afde-451a-8d10-8fa55c7ddf30" />|

---

## 📚 목차
1. [📌 프로젝트 개요](#1-프로젝트-개요)
2. [🛠 기술 스택](#2-기술-스택)
3. [📝 요구사항 정의서](#3-요구사항-정의서)
4. [🗄 ERD](#4-erd)
5. [📑 테이블 정의서](#5-테이블-정의서)
6. [🏗 시스템 아키텍처](#6-시스템-아키텍처)
7. [📡 API 명세서](#7-api-명세서)
8. [🔎 테스트 케이스](#8-테스트-케이스)
9. [🔎 회고](#9-회고)

---

## 1. 📌 프로젝트 개요
## 📌 프로젝트 개요

### 1. 프로젝트 배경
최근 해외 상품을 국내 오픈마켓에 판매하는 **구매대행 셀러 시장이 빠르게 성장**하고 있다.  
그러나 구매대행 업무는 상품 소싱, 상품 가공, 마켓 등록, 주문 수집, 해외몰 발주, 배송 추적 등 여러 단계를 거쳐야 하며 대부분의 작업이 **수작업 또는 여러 플랫폼을 오가며 처리**되고 있다.

이로 인해 다음과 같은 문제가 발생한다.

- 반복적인 업무로 인한 **업무 효율 저하**
- 주문 처리 과정에서의 **휴먼 에러 발생**
- 여러 마켓 및 해외몰 관리의 **복잡성 증가**

따라서 구매대행 셀러의 업무 부담을 줄이고 효율적인 운영을 돕기 위한 **통합 업무 자동화 시스템의 필요성**이 증가하고 있다.

---

### 2. 프로젝트 목적
본 프로젝트는 구매대행 셀러의 업무 프로세스를 하나의 시스템에서 관리할 수 있도록 하여  
**업무 효율성을 높이고 운영을 자동화하는 것을 목표**로 한다.

주요 목표는 다음과 같다.

- 구매대행 업무 프로세스 **통합 관리**
- 반복 작업 **자동화**
- 주문 및 발주 관리 **체계화**
- 데이터 기반 **업무 효율 향상**

---

### 3. 주요 기능
본 시스템은 구매대행 업무의 전 과정을 자동화하기 위해 다음과 같은 기능을 제공한다.

- **상품 소싱 관리**  
  해외 쇼핑몰에서 상품 정보를 수집하고 관리

- **상품 가공 관리**  
  상품 정보 번역 및 가공 데이터 관리

- **마켓 등록 관리**  
  국내 오픈마켓(쿠팡, 스마트스토어 등)에 상품 등록

- **주문 자동 수집**  
  마켓에서 발생한 주문 데이터를 자동으로 수집 및 관리

- **해외몰 발주 관리**  
  수집된 주문을 기반으로 해외 쇼핑몰에 발주 처리

- **배송 및 트래킹 관리**  
  배송 상태 조회 및 배송 정보 관리

---

### 4. 기대 효과
본 시스템을 통해 다음과 같은 효과를 기대할 수 있다.

- 구매대행 업무 프로세스 **자동화**
- 반복 작업 감소를 통한 **업무 효율 향상**
- 주문 및 발주 과정의 **오류 감소**
- 데이터 기반의 **체계적인 주문 관리**
---

## 2. 🛠 기술 스택
<details>
<summary>세부사항</summary>

<br>

| 구분 | 기술 |
|---|---|
| Language | ![Java](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white) ![Python](https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white) |
| Framework | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-6DB33F?style=flat-square&logo=spring&logoColor=white) ![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=flat-square&logo=fastapi&logoColor=white) ![Uvicorn](https://img.shields.io/badge/Uvicorn-222222?style=flat-square&logo=gunicorn&logoColor=white)|
| ORM / View | ![Spring Data JPA](https://img.shields.io/badge/JPA-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white) ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white) |
| Service Discovery | ![Eureka](https://img.shields.io/badge/Netflix_Eureka-E50914?style=flat-square&logo=netflix&logoColor=white) |
| Database / Storage | ![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white) ![MinIO](https://img.shields.io/badge/MinIO-C72E49?style=flat-square&logo=minio&logoColor=white) |
| AI / External API | ![Gemini](https://img.shields.io/badge/Google_Gemini-8E75B2?style=flat-square&logo=googlegemini&logoColor=white) ![Playwright](https://img.shields.io/badge/Playwright-2EAD33?style=flat-square&logo=playwright&logoColor=white) ![Oxylabs](https://img.shields.io/badge/Oxylabs-1A1A1A?style=flat-square) |
| Build | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white) ![Lombok](https://img.shields.io/badge/Lombok-BC4521?style=flat-square&logo=lombok&logoColor=white) |
| Test | ![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white) ![Testcontainers](https://img.shields.io/badge/Testcontainers-2D3E50?style=flat-square&logo=testcontainers&logoColor=white) |


</details>

<br>

---

## 3. 📝 요구사항 정의서

<details>
   <summary>세부사항</summary>
<div markdown="1">
  <a href="https://docs.google.com/spreadsheets/d/1dIV9X2avDBPIIPxYHD-rsDEI2gGXk_7wAITY9AfHpv4/edit?gid=437937772#gid=437937772">요구사항 정의서</a>
</div>
</details>
<br>

---

## 4. 🗄 ERD

<details>
   <summary>세부사항</summary>
<div markdown="1">
  <a href="https://www.erdcloud.com/d/3aqxuohvoJweoPJN2">ERD</a>
</div>
</details>
<br>

---

## 5. 📑 테이블 정의서

<details>
   <summary>세부사항</summary>
<div markdown="1">
  <a href="https://docs.google.com/spreadsheets/d/1dIV9X2avDBPIIPxYHD-rsDEI2gGXk_7wAITY9AfHpv4/edit?gid=1224156399#gid=1224156399">테이블 정의서</a>
</div>
</details>
<br>

---

## 6. 🏗 시스템 아키텍처
<details>
  <summary>세부사항</summary>
 <a> 아키텍트 다이어그램 </a> 
  <img width="3785" height="2597" alt="image" src="https://github.com/user-attachments/assets/7d245472-9999-4b80-bb97-5428cf7dc964" />


  <a> 로직 다이어그램 </a>
  
  ``` mermaid
graph TD
    %% 스타일 정의
    classDef user fill:#f9f,stroke:#333,stroke-width:2px;
    classDef gateway fill:#fff4dd,stroke:#d4a017,stroke-width:2px;
    classDef process fill:#e1f5fe,stroke:#01579b,stroke-width:2px;
    classDef db fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px;

    User((사용자))

    subgraph Entry_Layer [Access & Control]
        AGW[API Gateway / JWT Auth]
        Config[정책 및 마진 설정]
    end

    subgraph Core_Pipeline [Traders Main Engine]
        direction TB
        Sourcing[Oxylabs 아이템 소싱]
        
        subgraph Async_Processing [비동기/세마포어 가공]
            ImageProc[NanoBanana 이미지 번역]
            TextProc[Gemini 3-Flash 텍스트 번역]
        end
        
        Refinement[마진 측정 및 최종 데이터 정리]
    end

    subgraph Integration_Layer [Market & Storage]
        Coupang[쿠팡 판매자 사이트 등록]
        DB[(Central DataBase)]
    end

    %% 흐름 연결
    User -->|Access with JWT| AGW
    AGW -->|배송/결제 설정 관리| Config
    
    Config -->|필터링 조건 전달| Sourcing
    
    Sourcing -->|Async Trigger| ImageProc
    Sourcing -->|Async Trigger| TextProc
    
    ImageProc --> Refinement
    TextProc --> Refinement
    
    Refinement -->|Data Persistence| DB
    Refinement -->|Push to Market| Coupang
    
    Coupang -->|등록 상태 피드백| DB

    %% 클래스 적용
    class User user;
    class AGW,Config gateway;
    class Sourcing,ImageProc,TextProc,Refinement process;
    class DB db;

  ```
</details>


---

## 7. 📡 API 명세서

<details>
   <summary>세부사항</summary>
<div markdown="1">
  <a href="https://www.notion.so/API-343f85367d62809fbef7c5884b7ebce8?source=copy_link">API 명세서</a>
</div>
</details>
<br>

---

## 8. 📄 테스트 케이스

<details>
   <summary>세부사항</summary>
<div markdown="1">
  <a href="https://docs.google.com/spreadsheets/d/1dIV9X2avDBPIIPxYHD-rsDEI2gGXk_7wAITY9AfHpv4/edit?gid=522444149#gid=522444149"> 테스트 케이스 </a>
</div>
</details>
<br>

---

## 9. 🔎 회고

<details>
<summary>세부사항</summary>

<br>

| 조원이름 | 회고 |
|----------|------|
| 민정기 (팀장) | 첫 MSA 방식으로 설계한 프로젝트입니다. 모놀로식의 방식보다 장점은 많았지만 완벽한 준비 보다는 일단 하자 라는식으로 시작하게 된 프로젝트였습니다. 굉장히 많은 어려움도 있었고 불편함도 있지만 전체적인 서버가 어떻게 돌아가고 어떠한 방식의 네트워크가 필요한지에 대해서 알게된 프로젝트 였습니다. 일단 구현이 우선 목표 라는 생각으로 진행하다 보니 많은 아쉬운점이 있으며 특히 ELK나 AWS를 통하여 로그인 서버를 올려놓고 구현을 했다면 불편함의 반은 줄었지 않았나 싶습니다. 다음에는 이러한 점을 생각하여, 하는것이 좋을것 같다고 생각합니다. 팀원들이 믿고 무작정 MSA 설계 방식을 따라와주셔서 감사하고 다음에는 이러한 불편함을 지우고 해보고 싶습니다.|
| 김지연 | 백엔드와 프론트엔드를 직접 구현해보는 첫 프로젝트였기 때문에 처음에는 낯설고 어려운 부분도 많았지만, 이론으로 배우는 것보다 훨씬 더 많은 것을 익힐 수 있었던 경험이었다. 백엔드에서는 API 설계, 데이터베이스 연동, 예외 처리 등을 직접 다뤄보며 서비스가 동작하는 구조를 구체적으로 이해할 수 있었고, 프론트엔드에서는 화면 구성, 사용자 입력 처리, 백엔드와의 데이터 연동 과정을 경험하면서 전체적인 구현 흐름을 배울 수 있었다. 특히 프론트엔드와 백엔드가 어떻게 연결되고 요청과 응답이 어떤 방식으로 처리되는지 직접 확인해보면서 기술적인 이해도가 높아졌고, 문제를 해결하는 과정에서 실무적인 감각도 함께 기를 수 있었다. 또한 전공자 팀원분들이 많이 도와주신 덕분에 부족한 부분을 보완하며 프로젝트를 끝까지 진행할 수 있었고, 협업 방식과 개발 과정에 대해서도 많이 배울 수 있었다. 이번 프로젝트는 실제 서비스를 구현하는 과정 전반을 이해하고, 앞으로 더 성장해야겠다는 동기를 얻을 수 있었던 의미 있는 경험이었다. |
| 이진 |  |
| 이용호 |  |
| 현재진 |  |

</details>

<br>



