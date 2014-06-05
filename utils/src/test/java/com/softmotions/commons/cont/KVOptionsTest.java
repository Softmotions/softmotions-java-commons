package com.softmotions.commons.cont;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

public class KVOptionsTest {

    @Test
    public void testOptions() throws Exception {
        KVOptions opts = new KVOptions("a=b");
        assertEquals("b", opts.get("a"));

        opts = new KVOptions("a=b,vvv=ccc,fff =xxx,esc\\,aped=some\\, escaped\\, text, k= 123");
        assertEquals("b", opts.get("a"));
        assertEquals("ccc", opts.get("vvv"));
        assertEquals("xxx", opts.get("fff"));
        assertEquals("123", opts.get("k"));
        assertEquals("some, escaped, text", opts.get("esc,aped"));
    }
}