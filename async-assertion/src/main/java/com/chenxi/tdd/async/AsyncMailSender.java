package com.chenxi.tdd.async;

import lombok.SneakyThrows;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/13
 */
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
        // simulate the time-consuming operation
        TimeUnit.MICROSECONDS.sleep(500);
        mailbox.receiveMail(msg);
    }
}
