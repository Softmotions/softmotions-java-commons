package com.softmotions.weboot.mb;

import com.google.inject.Singleton;

import org.apache.ibatis.session.SqlSessionFactory;

import javax.inject.Inject;
import javax.inject.Provider;


/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Singleton
public class MBSqlSessionManagerProvider implements Provider<MBSqlSessionManager> {

    private MBSqlSessionManager sqlSessionManager;

    @Inject
    public void createNewSqlSessionManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionManager = MBSqlSessionManager.newInstance(sqlSessionFactory);
    }

    public MBSqlSessionManager get() {
        return sqlSessionManager;
    }
}