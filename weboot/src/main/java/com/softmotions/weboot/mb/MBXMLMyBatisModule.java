package com.softmotions.weboot.mb;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.type.TypeHandler;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Properties;

import static org.apache.ibatis.io.Resources.getResourceAsReader;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class MBXMLMyBatisModule extends MBAbstractMyBatisModule {

    private static final String DEFAULT_CONFIG_RESOURCE = "mybatis-config.xml";

    private static final String DEFAULT_ENVIRONMENT_ID = "development";

    private String classPathResource = DEFAULT_CONFIG_RESOURCE;

    private String environmentId = DEFAULT_ENVIRONMENT_ID;

    private Properties properties = new Properties();

    /**
     * Set the MyBatis configuration class path resource.
     *
     * @param classPathResource the MyBatis configuration class path resource
     */
    protected final void setClassPathResource(String classPathResource) {
        if (classPathResource == null) {
            throw new IllegalArgumentException("Parameter 'classPathResource' must be not null");
        }
        this.classPathResource = classPathResource;
    }

    /**
     * Set the MyBatis configuration environment id.
     *
     * @param environmentId the MyBatis configuration environment id
     */
    protected final void setEnvironmentId(String environmentId) {
        if (environmentId == null) {
            throw new IllegalArgumentException("Parameter 'environmentId' must be not null");
        }
        this.environmentId = environmentId;
    }

    /**
     * Add the variables will be used to replace placeholders in the MyBatis configuration.
     *
     * @param properties the variables will be used to replace placeholders in the MyBatis configuration
     */
    protected final void addProperties(Properties properties) {
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void internalConfigure() {
        this.initialize();

        Reader reader = null;
        try {
            reader = getResourceAsReader(getResourceClassLoader(), classPathResource);
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(reader,
                                                                                    environmentId,
                                                                                    properties);
            bind(SqlSessionFactory.class).toInstance(sessionFactory);

            Configuration configuration = sessionFactory.getConfiguration();

            // bind mappers
            Collection<Class<?>> mapperClasses = configuration.getMapperRegistry().getMappers();
            for (Class<?> mapperType : mapperClasses) {
                bindMapper(mapperType);
            }

            // request injection for type handlers
            Collection<TypeHandler<?>> allTypeHandlers = configuration.getTypeHandlerRegistry().getTypeHandlers();
            for (TypeHandler<?> handler : allTypeHandlers) {
                requestInjection(handler);
            }

            // request injection for interceptors
            Collection<Interceptor> interceptors = configuration.getInterceptors();
            for (Interceptor interceptor : interceptors) {
                requestInjection(interceptor);
            }
        } catch (Exception e) {
            addError("Impossible to read classpath resource '%s', see nested exceptions: %s",
                     classPathResource,
                     e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // close quietly
                }
            }
        }
    }

}

