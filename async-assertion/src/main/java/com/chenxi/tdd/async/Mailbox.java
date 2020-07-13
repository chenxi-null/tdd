package com.chenxi.tdd.async;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/13
 */
public class Mailbox {

    private final Collection<String> contents = new HashSet<>();

    public int numOfReceivedMail() {
        return contents.size();
    }

    public boolean containsMail(String msg) {
        return contents.contains(msg);
    }

    public void receiveMail(String msg) {
        contents.add(msg);
    }
}
