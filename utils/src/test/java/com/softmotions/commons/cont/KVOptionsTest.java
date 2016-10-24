package com.softmotions.commons.cont;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.softmotions.commons.json.JsonUtils;


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
        //System.out.println("opts=" + new KVOptions("foo=n,bar=b"));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode val = (ObjectNode)
                mapper.readTree("{\"allowPages\":true,\"allowFiles\":false,\"allowExternal\":false,\"nestingLevel\":2," +
                                "\"ncms.asm.am.RichRefAM\":{\"allowPages\":true,\"allowFiles\":false,\"allowName\":false," +
                                "\"optionalLinks\":false,\"allowDescription\":false,\"allowImage\":true," +
                                "\"image\":{\"width\":\"100\",\"height\":null,\"resize\":false,\"cover\":false,\"restrict\":false,\"skipSmall\":true}," +
                                "\"styles\":null,\"styles2\":null,\"styles3\":null}}");
        opts = new KVOptions();
        JsonUtils.populateMapByJsonNode(val, opts);
        String sval = opts.toString();
        Assert.assertTrue(sval.contains("\\\\,width=100\\\\,"));
        Assert.assertTrue(sval.contains(",skipSmall=true\\\\,"));
        //System.out.println(sval);
    }
}
