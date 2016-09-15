package com.softmotions.commons.cont;

import org.testng.annotations.Test;

import org.testng.Assert;


/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

public class KVOptionsTest {

    @Test
    public void testOptions() throws Exception {
        KVOptions opts = new KVOptions("a=b");
        Assert.assertEquals("b", opts.get("a"));

        opts = new KVOptions("a=b,vvv=ccc,fff =xxx,esc\\,aped=some\\, escaped\\, text, k= 123");
        Assert.assertEquals("b", opts.get("a"));
        Assert.assertEquals("ccc", opts.get("vvv"));
        Assert.assertEquals("xxx", opts.get("fff"));
        Assert.assertEquals("123", opts.get("k"));
        Assert.assertEquals("some, escaped, text", opts.get("esc,aped"));


        System.out.println("opts=" + new KVOptions("foo=n,bar=b"));


        //allowPages=true,ncms.asm.am.RichRefAM=image=restrict=false\\,width=10\\,skipSmall=true\\,resize=false\,allowDescription=true\,allowImage=true,nestingLevel=2
        /*opts = new KVOptions("allowPages=true,ncms.asm.am.RichRefAM=image=restrict=false\\\\,width=10\\\\,skipSmall=true\\\\,resize=false\\,allowDescription=true\\,allowImage=true,nestingLevel=2");
        System.out.println("O=" + opts.keySet());
        System.out.println("V=" + opts.values());*/

    }
}
