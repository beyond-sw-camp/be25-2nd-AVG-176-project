# be25-2nd-avg(height)=174-project

# 업무 자동화 시스템

## 👥 팀원 소개

<table style="width:100%;">
  <thead>
    <tr align="center">
      <th>민정기</th>
      <th>이진</th>
      <th>김지연</th>
      <th>이용호</th>
      <th>현재진</th>
     </tr>
  </thead>
</table>

---

## 📚 목차
1. [📌 프로젝트 개요](#1-프로젝트-개요)
2. [🛠 기술 스택](#2-기술-스택)
3. [📝 요구사항 정의서](#3-요구사항-정의서)
4. [🗄 ERD](#4-erd)
5. [📑 테이블 정의서](#5-테이블-정의서)
6. [🏗 시스템 아키텍처](#6-시스템-아키텍처)
7. [📡 API 명세서](#7-api-명세서)
8. [🖥 화면 및 기능 설계서](#8-화면-및-기능-설계서)
9. [🧪 테스트 계획 및 결과 보고서](#9-테스트-계획-및-결과-보고서)
10. [🔎 회고](#10-회고)

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

| 분야 | 기술 |
|-----|-----|
| Backend | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) |
| Frontend | ![Vue.js](https://img.shields.io/badge/Vue.js-35495E?style=flat-square&logo=vuedotjs&logoColor=4FC08D) |
| Database | ![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white) |
| 협업 | ![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white) ![Notion](https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white) |

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

---

## 7. 📡 API 명세서

<details>
   <summary>세부사항</summary>
<div markdown="1">
  <a href="https://docs.google.com/spreadsheets/d/1dIV9X2avDBPIIPxYHD-rsDEI2gGXk_7wAITY9AfHpv4/edit?gid=2024832708#gid=2024832708">API 명세서</a>
</div>
</details>
<br>

---

## 8. 🖥 화면 및 기능 설계서
<details>
   <summary>세부사항</summary>
<div markdown="1">
  <a href="https://www.figma.com/design/m2ExYsceFRsQDmCImIB0cT/-3%EC%A1%B0--AVG-height--174cm?node-id=0-1&p=f&t=aDgmD25W6KrZuLQb-0">화면 및 기능 설계서</a>
</div>
</details>
<br>
---

## 9. 🧪 테스트 계획 및 결과 보고서

---

## 10. 🔎 회고

<details>
<summary>세부사항</summary>

<br>

| 조원이름 | 회고 |
|----------|------|
| 민정기 |  |
| 이진 |  |
| 김지연 |  |
| 이용호 |  |
| 현재진 |  |

</details>

<br>



