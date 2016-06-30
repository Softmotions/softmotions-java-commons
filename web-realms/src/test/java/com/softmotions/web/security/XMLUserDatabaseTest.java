package com.softmotions.web.security;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public class XMLUserDatabaseTest {

    private static final Logger log = LoggerFactory.getLogger(XMLUserDatabaseTest.class);


    public static <E> List<E> toList(final Iterator<? extends E> iterator) {
        if (iterator == null) {
            throw new NullPointerException("Iterator must not be null");
        }
        final List<E> list = new ArrayList<E>(10);
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }


    @Test
    public void testXMLUserdatabase() throws Exception {
        XMLWSUserDatabase db =
                new XMLWSUserDatabase("db1",
                                      "com/softmotions/web/security/test-users-db.xml",
                                      false);

        List<WSGroup> groups = toList(db.getGroups());
        List<WSRole> roles = toList(db.getRoles());
        List<WSUser> users = toList(db.getUsers());

        assertEquals(4, roles.size());
        assertEquals(3, groups.size());
        assertEquals(2, users.size());

        for (final WSRole role : roles) {
            assertNotNull(role.getName());
            assertTrue(role.getUserDatabase() == db);
        }
        for (final WSGroup group : groups) {
            assertNotNull(group.getName());
            assertTrue(group.getUserDatabase() == db);
        }
        for (final WSUser user : users) {
            assertNotNull(user.getName());
            assertTrue(user.getUserDatabase() == db);
        }

        WSUser user = db.findUser("user1");
        assertNotNull(user);
        assertEquals("user1", user.getName());
        assertNull(user.getFullName());

        roles = toList(user.getRoles());
        assertEquals(3, roles.size());
        for (WSRole role : roles) {
            assertTrue("role1".equals(role.getName()) ||
                       "role2".equals(role.getName()) ||
                       "role3".equals(role.getName())
            );
            assertTrue(user.isInRole(role));
            if ("role2".equals(role.getName())) {
                assertEquals("description of role2", role.getDescription());
            }
            assertNotNull(db.findRole(role.getName()));
        }

        WSGroup group = db.findGroup("group1");
        roles = toList(group.getRoles());
        assertEquals(3, roles.size());
        for (final WSRole role : roles) {
            assertTrue(group.isInRole(role));
        }

        group = db.findGroup("group2");
        roles = toList(group.getRoles());
        assertEquals(1, roles.size());
        for (final WSRole role : roles) {
            assertTrue(group.isInRole(role));
        }

        WSRole role = db.createRole("role5", null);
        roles = toList(db.getRoles());
        assertEquals(5, roles.size());

        user = db.findUser("user2");
        assertNotNull(user);
        assertFalse(user.isInRole(role));
        user.addRole(role);
        assertTrue(user.isInRole(role));
        role = db.createRole("role6", null);
        user.addRole(role);
        assertTrue(user.isInRole(role));
        user.removeRole(db.findRole("role5"));
        assertFalse(user.isInRole(db.findRole("role5")));
        user.addRole(db.createRole("role7", null));

        group = db.findGroup("group3");
        group.addRole(db.findRole("role5"));
        db.removeRole(db.findRole("role5"));


        StringWriter sw = new StringWriter();
        db.save(sw);

        String ncfg = sw.toString();
        log.info("Resulted configuration: \n{}", ncfg);

        assertFalse(ncfg.contains("role5"));
        assertTrue(ncfg.contains("role6"));
        assertTrue(ncfg.contains("role7"));
        assertTrue(ncfg.contains("group2"));
        assertTrue(ncfg.contains("group3"));
        assertTrue(ncfg.contains("user1"));
        assertTrue(ncfg.contains("user2"));
    }

}
