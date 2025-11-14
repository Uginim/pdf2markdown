# PDF to Markdown Converter

Kotlin으로 작성된 터미널 기반 PDF to Markdown 변환 도구입니다.

## 특징

- PDF 문서를 마크다운 형식으로 변환
- 제목, 단락, 리스트 등 문서 구조 자동 인식
- 이미지 추출 및 임베딩 지원
- GitHub Flavored Markdown 지원
- 사용하기 쉬운 CLI 인터페이스
- Apache License 2.0 (상업적 사용 가능)

## 기술 스택

- **언어**: Kotlin 1.9+
- **빌드**: Gradle 8.x (Kotlin DSL)
- **CLI 프레임워크**: Clikt 4.x
- **PDF 처리**: Apache PDFBox 3.x
- **로깅**: SLF4J + Logback

## 빠른 시작

### 필수 요구사항

- JDK 17 이상
- Gradle 8.x (또는 포함된 Gradle Wrapper 사용)

### 빌드

```bash
# 프로젝트 클론
git clone <repository-url>
cd pdf2markdown

# 빌드
./gradlew build

# Fat JAR 생성
./gradlew shadowJar
```

### 실행

```bash
# JAR 파일 직접 실행
java -jar build/libs/pdf2md-1.0.0.jar convert input.pdf

# 또는 Gradle을 통해 실행
./gradlew run --args="convert input.pdf"
```

## 사용법

### 기본 변환

```bash
pdf2md convert document.pdf
```

### 출력 파일 지정

```bash
pdf2md convert document.pdf -o output.md
```

### 이미지 추출 포함

```bash
pdf2md convert document.pdf --extract-images --images-dir ./images
```

### 상세 로그

```bash
pdf2md convert document.pdf -v
```

### 도움말

```bash
pdf2md --help
pdf2md convert --help
```

## 옵션

| 옵션 | 짧은 형식 | 설명 | 기본값 |
|------|----------|------|--------|
| `--output` | `-o` | 출력 마크다운 파일 경로 | `<입력파일>.md` |
| `--extract-images` | - | 이미지 추출 활성화 | `false` |
| `--images-dir` | - | 이미지 저장 디렉토리 | `./images` |
| `--format` | - | 마크다운 포맷 (`github`, `commonmark`) | `github` |
| `--encoding` | - | 출력 파일 인코딩 | `UTF-8` |
| `--verbose` | `-v` | 상세 로그 출력 | `false` |

## 프로젝트 구조

```
pdf2markdown/
├── build.gradle.kts           # Gradle 빌드 설정
├── settings.gradle.kts         # Gradle 프로젝트 설정
├── README.md                   # 프로젝트 개요
├── IMPLEMENTATION_PLAN.md      # 구현 계획서
├── ARCHITECTURE.md             # 아키텍처 설계서
├── API_DESIGN.md               # API 설계서
└── src/
    ├── main/
    │   └── kotlin/
    │       └── com/pdf2md/
    │           ├── Main.kt                    # 진입점
    │           ├── cli/                       # CLI 레이어
    │           ├── application/               # 애플리케이션 레이어
    │           ├── domain/                    # 도메인 레이어
    │           │   ├── pdf/                   # PDF 처리
    │           │   └── converter/             # 마크다운 변환
    │           ├── infrastructure/            # 인프라 레이어
    │           └── common/                    # 공통 유틸리티
    └── test/
        └── kotlin/
            └── com/pdf2md/                    # 테스트
```

## 개발 계획

프로젝트는 다음 단계로 진행됩니다:

### Phase 1: 프로젝트 초기 설정 ✅
- Gradle 프로젝트 생성
- 의존성 설정
- 기본 디렉토리 구조

### Phase 2: CLI 인터페이스 구현
- Clikt 명령어 구조
- 옵션 및 플래그 처리

### Phase 3: PDF 리더 구현
- PDF 로드 및 파싱
- 텍스트 추출
- 문서 구조 분석

### Phase 4: 마크다운 변환기 구현
- 제목 감지
- 단락 구분
- 리스트 변환

### Phase 5: 파일 출력 구현
- 마크다운 파일 저장
- 이미지 파일 처리

### Phase 6: 통합 및 에러 처리
- 전체 플로우 통합
- 에러 핸들링 개선

### Phase 7: 테스트 작성
- 단위 테스트
- 통합 테스트

### Phase 8: 빌드 및 배포
- Fat JAR 생성
- 실행 스크립트

자세한 내용은 [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md)를 참조하세요.

## 아키텍처

프로젝트는 레이어드 아키텍처를 따릅니다:

```
┌─────────────────────────┐
│   Presentation Layer    │  (CLI)
├─────────────────────────┤
│   Application Layer     │  (Orchestrator)
├─────────────────────────┤
│     Domain Layer        │  (PDF Reader, Converter)
├─────────────────────────┤
│  Infrastructure Layer   │  (File I/O, Logging)
└─────────────────────────┘
```

자세한 내용은 [ARCHITECTURE.md](ARCHITECTURE.md)를 참조하세요.

## API 문서

프로그래매틱 API 및 데이터 모델에 대한 자세한 내용은 [API_DESIGN.md](API_DESIGN.md)를 참조하세요.

## 라이선스

이 프로젝트는 Apache License 2.0 하에 배포됩니다.

### 사용된 라이브러리 라이선스

- **Kotlin**: Apache License 2.0
- **Clikt**: Apache License 2.0
- **Apache PDFBox**: Apache License 2.0

모든 라이브러리는 상업적 사용이 가능하며 소스코드 공개 의무가 없습니다.

## 기여

이슈 및 풀 리퀘스트를 환영합니다!

## 로드맵

### v1.0 (MVP)
- [x] 기본 텍스트 추출
- [x] 제목 감지
- [x] 단락 구분
- [x] CLI 인터페이스

### v1.1
- [ ] 리스트 감지 개선
- [ ] 이미지 추출
- [ ] 표 변환

### v1.2
- [ ] 배치 처리
- [ ] 설정 파일 지원
- [ ] 플러그인 시스템

### v2.0
- [ ] OCR 지원 (스캔 PDF)
- [ ] GUI 버전
- [ ] 웹 서비스 API

## 문의

문제가 발생하거나 질문이 있으시면 이슈를 등록해주세요.
