# Admin Page Package

이 압축파일은 프로젝트에 바로 적용하지 않고, 관리자 페이지 작업 범위를 구분해서 전달하기 위한 복사본입니다.

## common

공통 담당자와 맞물리는 영역입니다.

- `common/router/root.jsx`
  - `/admin` 페이지 경로가 연결되는 라우터 파일입니다.
  - 로그인 후 관리자 페이지로 이동시키는 버튼이나 메뉴 연결은 다른 담당자 영역입니다.
- `common/guard/AdminOnly.jsx`
  - 관리자 권한 확인용 공통 가드입니다.

## my-admin-page

내가 작업하는 관리자 페이지 화면 영역입니다.

- `my-admin-page/pages/admin/AdminDashboardPage.jsx`
  - 관리자 페이지 엔트리 페이지입니다.
- `my-admin-page/components/admin/AdminDashboardComponent.jsx`
  - 관리자 대시보드 UI 본문입니다.

## route

관리자 페이지 경로는 `/admin` 입니다.
