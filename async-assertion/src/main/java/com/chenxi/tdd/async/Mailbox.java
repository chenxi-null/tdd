package com.chenxi.tdd.async;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/13
 */
public class Mailbox {

    private final Collection<String> contents = new HashSet<>();

    private Consumer<String> receivedHook;

    public int numOfReceivedMail() {
        return contents.size();
    }

    public boolean containsMail(String msg) {
        return contents.contains(msg);
    }

    public void receiveMail(String msg) {
        contents.add(msg);

        receivedHook.accept(msg);
    }

    public void setReceivedHook(Consumer<String> receivedHook) {
        this.receivedHook = receivedHook;
    }
}
