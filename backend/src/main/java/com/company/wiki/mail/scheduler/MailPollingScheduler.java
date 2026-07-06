package com.company.wiki.mail.scheduler;

import com.company.wiki.mail.service.MailSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailPollingScheduler {

    private final MailSyncService mailSyncService;

    /**
     * 5분(300,000ms)마다 모든 IMAP 계정의 메일을 폴링한다.
     * fixedDelay: 이전 실행 완료 후 300초 대기 (동시 실행 방지)
     */
    @Scheduled(fixedDelay = 300000)
    public void pollAll() {
        log.info("메일 폴링 시작");
        mailSyncService.syncAll();
        log.info("메일 폴링 완료");
    }
}
