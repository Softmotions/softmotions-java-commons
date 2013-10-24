/*
 * All Rights Reserved.
 * Copyright (c) 2005
 * Adamansky Anton V. (anton@adamansky.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id: Stack.java 7811 2006-02-27 15:57:13Z adam $
 */
package com.softmotions.commons.cont;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 */
public final class Stack<E> extends ArrayList<E> {

    public Stack(int initialCapacity) {
        super(initialCapacity);
    }

    public Stack() {
    }

    public Stack(Collection<? extends E> es) {
        super(es);
    }

    public E push(E e) {
        add(e);
        return e;
    }

    public E peek() {
        int len = size();
        if (len == 0) throw new EmptyStackException();
        return get(len - 1);
    }

    public E pop() {
        E obj;
        int len = size();
        obj = peek();
        remove(len - 1);
        return obj;
    }
}
