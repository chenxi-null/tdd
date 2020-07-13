package com.chenxi.tdd.async;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/13
 */
@Slf4j
public class AsyncMailSender {

    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    private Mailbox mailbox;

    public AsyncMailSender(Mailbox mailbox) {
        this.mailbox = mailbox;
    }

    public void sendMail(String msg) {
        executorService.submit(() -> doSendMail(msg));
    }

    @SneakyThrows
    private void doSendMail(String msg) {
        log.info("sending mail, msg: {}", msg);
        // simulate the time-consuming operation
        TimeUnit.SECONDS.sleep(1);
        mailbox.receiveMail(msg);
        log.info("sent mail, msg: {}", msg);
    }
}
