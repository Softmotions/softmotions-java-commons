package com.softmotions.commons;

/**
 * Simple user data store.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface UserDataStore {

    <T> T getUserData();

    <T> void setUserData(T data);
}
