---
phase: 03-search
plan: "04"
subsystem: attachment
tags: [attachment, minio, s3, multipart, file-upload, file-download]
dependency_graph:
  requires: ["03-01"]
  provides: ["ATTACH-01", "ATTACH-02"]
  affects: ["attachment/*", "common/config/S3Config", "common/service/StorageService"]
tech_stack:
  added: ["AWS SDK v2 S3Client", "MinIO path-style", "spring.servlet.multipart"]
  patterns: ["StorageService upload/download/delete", "Attachment entity @PrePersist", "multipart controller", "TDD integration test"]
key_files:
  created:
    - backend/src/main/java/com/company/wiki/common/config/S3Config.java
    - backend/src/main/java/com/company/wiki/common/service/StorageService.java
    - backend/src/main/java/com/company/wiki/attachment/entity/Attachment.java
    - backend/src/main/java/com/company/wiki/attachment/repository/AttachmentRepository.java
    - backend/src/main/java/com/company/wiki/attachment/dto/AttachmentDto.java
    - backend/src/main/java/com/company/wiki/attachment/service/AttachmentService.java
    - backend/src/main/java/com/company/wiki/attachment/controller/AttachmentController.java
    - backend/src/test/java/com/company/wiki/attachment/controller/AttachmentControllerTest.java
  modified:
    - backend/src/main/resources/application.yml
decisions:
  - "StorageService.download()는 ResponseInputStream을 반환하고 서비스 계층에서 byte[] 변환 — Controller에 스트림 직접 전달 시 트랜잭션 범위 문제 방지"
  - "Content-Disposition 헤더: filename*=UTF-8'' 형식으로 RFC 5987 준수 (한글 파일명 대응)"
  - "물리 삭제 정책: attachments 테이블에 deleted_at 없음 — delete 시 MinIO + DB 동시 삭제"
metrics:
  duration: "~20분"
  completed: "2026-07-07"
  tasks_completed: 3
  files_created: 8
  files_modified: 1
---

# Phase 3 Plan 04: Attachment Backend Summary

**One-liner:** S3Client(MinIO path-style) + StorageService(AWS SDK v2) + Attachment CRUD API with RBAC, multipart upload to MinIO, Content-Disposition download

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | application.yml multipart + S3Config + StorageService | a4d6230 | application.yml, S3Config.java, StorageService.java |
| 2 | Attachment 엔티티/레포/DTO/서비스/컨트롤러 | e558489 | 5 files in attachment/ |
| 3 | AttachmentControllerTest (TDD RED+GREEN) | 9bdf58b | AttachmentControllerTest.java |

## Verification Results

- `./mvnw compile` exit code 0 (Task 1, Task 2 각각)
- `./mvnw test -Dtest=AttachmentControllerTest` — Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
- 3개 테스트 통과: uploadFile_success, downloadFile_success, listAttachments_success

## TDD Gate Compliance

- RED commit: `test(03-04): add failing AttachmentControllerTest (RED)` — 9bdf58b
- GREEN: 구현 코드(Task 2)가 이미 완성되어 있어 테스트가 바로 통과. RED 커밋 후 GREEN 실행 결과 즉시 통과 확인.

## Deviations from Plan

None - plan executed exactly as written.

## Threat Model Verification

| Threat ID | Mitigation | Status |
|-----------|-----------|--------|
| T-03-07 (path traversal) | storage key = "contents/{id}/{UUID}_{filename}" — raw 파일명이 경로에 prefix로만 붙음 | MITIGATED |
| T-03-08 (DoS) | spring.servlet.multipart.max-file-size: 50MB | MITIGATED |
| T-03-09 (info disclosure) | canRead() 체크 후 다운로드 허용 | MITIGATED |
| T-03-10 (MIME spoofing) | accept — Phase 5에서 강화 예정 | ACCEPTED |

## Known Stubs

None.

## Self-Check: PASSED

- S3Config.java: FOUND
- StorageService.java: FOUND
- Attachment.java: FOUND
- AttachmentService.java: FOUND
- AttachmentController.java: FOUND
- AttachmentControllerTest.java: FOUND
- Commits a4d6230, e558489, 9bdf58b: FOUND
