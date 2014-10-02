package com.softmotions.weboot.mb;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.guice.transactional.Transactional;

import javax.inject.Provider;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.not;
import static com.google.inject.name.Names.named;
import static com.google.inject.util.Providers.guicify;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class MBAbstractMyBatisModule extends AbstractModule {

    private ClassLoader resourcesClassLoader = getDefaultClassLoader();

    private ClassLoader driverClassLoader = getDefaultClassLoader();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        try {
            // sql session manager
            bind(MBSqlSessionManager.class).toProvider(createSqlSessionManagerProviderClass()).in(Scopes.SINGLETON);
            bind(SqlSession.class).to(MBSqlSessionManager.class).in(Scopes.SINGLETON);

            // transactional interceptor
            MethodInterceptor interceptor = createTransactionalMethodInterceptor();
            requestInjection(interceptor);
            bindInterceptor(any(), annotatedWith(Transactional.class), interceptor);
            // Intercept classes annotated with Transactional, but avoid "double"
            // interception when a mathod is also annotated inside an annotated
            // class.
            bindInterceptor(annotatedWith(Transactional.class), not(annotatedWith(Transactional.class)), interceptor);

            internalConfigure();

            bind(ClassLoader.class)
                    .annotatedWith(named("JDBC.driverClassLoader"))
                    .toInstance(driverClassLoader);
        } finally {
            resourcesClassLoader = getDefaultClassLoader();
            driverClassLoader = getDefaultClassLoader();
        }
    }


    protected MethodInterceptor createTransactionalMethodInterceptor() {
        return new MBTransactionalMethodInterceptor();
    }

    protected Class<? extends Provider<? extends MBSqlSessionManager>> createSqlSessionManagerProviderClass() {
        return MBSqlSessionManagerProvider.class;
    }

    /**
     * @param <T>
     * @param mapperType
     */
    final <T> void bindMapper(Class<T> mapperType) {
        bind(mapperType).toProvider(guicify(new MBMapperProvider<>(mapperType))).in(Scopes.SINGLETON);
    }

    /**
     * @return
     * @since 3.3
     */
    public void useResourceClassLoader(ClassLoader resourceClassLoader) {
        this.resourcesClassLoader = resourceClassLoader;
    }

    /**
     * @return
     * @since 3.3
     */
    protected final ClassLoader getResourceClassLoader() {
        return resourcesClassLoader;
    }

    /**
     * @return
     * @since 3.3
     */
    public void useJdbcDriverClassLoader(ClassLoader driverClassLoader) {
        this.driverClassLoader = driverClassLoader;
    }

    /**
     * @return
     * @since 3.3
     */
    private ClassLoader getDefaultClassLoader() {
        return getClass().getClassLoader();
    }

    /**
     * Configures a {@link com.google.inject.Binder} via the exposed methods.
     */
    abstract void internalConfigure();

    /**
     *
     */
    protected abstract void initialize();

}