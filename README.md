# Real-Time Commente using SSE

## Introduction

이 프로젝트는 Server-Sent Events (SSE)를 사용하여 실시간 댓글 기능을 구현한 예제입니다.

사용자가 댓글을 작성하면, 이를 실시간으로 다른 사용자들에게 전달하는 기능이 포함되어 있습니다.

## Features

- **Real-Time Commente:** 댓글이 실시간으로 다른 사용자에게 전달됩니다.
- **Test-Driven Development:** 단위 테스트를 통해 기능을 검증하며 개발합니다.

## Technologies

- **Spring Boot:** 서버 사이드 로직을 구현합니다.
- **SSE (Server-Sent Events):** 클라이언트와 서버 간의 실시간 통신을 가능하게 합니다.
- **JUnit & OkHttp:** 기능을 검증하기 위한 테스트 코드를 작성합니다.

## Getting Started

### Prerequisites

- SpringBoot 3 이상
- Java 17 이상
- Gradle 8 이상

### Installation

1. 저장소를 클론합니다.

    ```bash
    git clone https://github.com/Bue-von-hon/SSE-samples.git
    ```

2. 프로젝트 디렉토리로 이동합니다.

    ```bash
    cd SSE-samples
    ```

3. 프로젝트를 빌드합니다.

    ```bash
    ./gradlew build
    ```

4. 프로젝트를 실행합니다.

    ```bash
    ./gradlew bootRun
    ```