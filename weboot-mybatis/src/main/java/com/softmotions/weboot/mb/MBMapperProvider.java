package com.softmotions.weboot.mb;

import org.apache.ibatis.session.SqlSessionManager;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public final class MBMapperProvider<T> implements javax.inject.Provider<T> {

    private final Class<T> mapperType;

    @javax.inject.Inject
    private SqlSessionManager sqlSessionManager;

    public MBMapperProvider(Class<T> mapperType) {
        this.mapperType = mapperType;
    }

    public void setSqlSessionManager(SqlSessionManager sqlSessionManager) {
        this.sqlSessionManager = sqlSessionManager;
    }

    @Override
    public T get() {
        return this.sqlSessionManager.getMapper(mapperType);
    }

}

