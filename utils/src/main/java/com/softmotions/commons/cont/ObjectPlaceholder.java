package com.softmotions.commons.cont;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ObjectPlaceholder<T> {

    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
