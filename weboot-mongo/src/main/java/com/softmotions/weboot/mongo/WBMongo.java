package com.softmotions.weboot.mongo;

import com.mongodb.DB;
import com.mongodb.Mongo;

import org.jongo.Jongo;

import javax.annotation.Nonnull;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WBMongo {

    @Nonnull
    Mongo getMongo();

    @Nonnull
    Jongo getJongo(String dbName);

    @Nonnull
    Jongo getJongo();

    @Nonnull
    DB getDefaultDB();

}
