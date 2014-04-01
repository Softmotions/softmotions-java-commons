package com.softmotions.commons.weboot.mb;

import org.apache.ibatis.session.SqlSession;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface MBAction<T> {

     T exec(SqlSession sess, Connection conn) throws SQLException;
}
