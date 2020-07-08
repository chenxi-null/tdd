package com.chenxi.tdd.embeddeddb;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/8
 */
@Repository
@RequiredArgsConstructor
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public int save(String name) {
        return jdbcTemplate.update("insert into user (name) values (?)", name);
    }

    public String getName(int id) {
        return jdbcTemplate.queryForObject("select name from user where id = ?", String.class, id);
    }
}
