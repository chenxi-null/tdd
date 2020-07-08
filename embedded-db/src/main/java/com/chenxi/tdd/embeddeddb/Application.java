package com.chenxi.tdd.embeddeddb;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/7
 */
@SpringBootApplication
@RequiredArgsConstructor
public class Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private final UserDao userDao;

    @Override
    public void run(String... args) throws Exception {
        int id = userDao.save("chenxi");
        String name = userDao.getName(id);
        System.out.println("name = " + name);
    }
}
