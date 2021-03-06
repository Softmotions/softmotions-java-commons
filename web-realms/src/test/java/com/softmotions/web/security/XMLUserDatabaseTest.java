package com.softmotions.web.security;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import org.testng.Assert;

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
                                      false, "sha256");

        List<WSGroup> groups = toList(db.getGroups());
        List<WSRole> roles = toList(db.getRoles());
        List<WSUser> users = toList(db.getUsers());

        Assert.assertEquals(4, roles.size());
        Assert.assertEquals(3, groups.size());
        Assert.assertEquals(3, users.size());

        for (final WSRole role : roles) {
            Assert.assertNotNull(role.getName());
            Assert.assertTrue(role.getUserDatabase() == db);
        }
        for (final WSGroup group : groups) {
            Assert.assertNotNull(group.getName());
            Assert.assertTrue(group.getUserDatabase() == db);
        }
        for (final WSUser user : users) {
            Assert.assertNotNull(user.getName());
            Assert.assertTrue(user.getUserDatabase() == db);
        }

        WSUser user = db.findUser("user1");
        Assert.assertNotNull(user);
        Assert.assertEquals("user1", user.getName());
        Assert.assertNull(user.getFullName());
        Assert.assertTrue(user.matchPassword("pw1"));

        roles = toList(user.getRoles());
        Assert.assertEquals(3, roles.size());
        for (WSRole role : roles) {
            Assert.assertTrue("role1".equals(role.getName()) ||
                       "role2".equals(role.getName()) ||
                       "role3".equals(role.getName())
            );
            Assert.assertTrue(user.isInRole(role));
            if ("role2".equals(role.getName())) {
                Assert.assertEquals("description of role2", role.getDescription());
            }
            Assert.assertNotNull(db.findRole(role.getName()));
        }

        WSGroup group = db.findGroup("group1");
        roles = toList(group.getRoles());
        Assert.assertEquals(3, roles.size());
        for (final WSRole role : roles) {
            Assert.assertTrue(group.isInRole(role));
        }

        group = db.findGroup("group2");
        roles = toList(group.getRoles());
        Assert.assertEquals(1, roles.size());
        for (final WSRole role : roles) {
            Assert.assertTrue(group.isInRole(role));
        }

        WSRole role = db.createRole("role5", null);
        roles = toList(db.getRoles());
        Assert.assertEquals(5, roles.size());

        user = db.findUser("user2");
        Assert.assertNotNull(user);
        Assert.assertFalse(user.matchPassword("pw1"));
        Assert.assertTrue(user.matchPassword("pw2"));
        Assert.assertNull(user.getEmail());
        user.setEmail("user2@users.com");
        Assert.assertEquals("user2 password", user.getFullName());
        user.setFullName("user2 fullname");
        Assert.assertEquals("user2", user.getName());
        user.setName("user2 name");
        Assert.assertFalse(user.isInRole(role));
        user.addRole(role);
        Assert.assertTrue(user.isInRole(role));
        role = db.createRole("role6", null);
        user.addRole(role);
        Assert.assertTrue(user.isInRole(role));
        user.removeRole(db.findRole("role5"));
        Assert.assertFalse(user.isInRole(db.findRole("role5")));
        user.addRole(db.createRole("role7", null));

        group = db.findGroup("group3");
        group.addRole(db.findRole("role5"));
        db.removeRole(db.findRole("role5"));

        user = db.findUser("user3");
        Assert.assertNotNull(user);
        Assert.assertTrue(user.matchPassword("pw3"));

        user = db.findUser("user1");
        db.removeUser(user);

        db.createUser("user4", "pw4", "user4added");
        user = db.findUser("user4");
        Assert.assertNotNull(user);
        String password = user.getPassword();
        Assert.assertTrue(password.startsWith("{sha256}"));
        Assert.assertTrue(password.length() == "{sha256}".length() + 64); // check hash length
        Assert.assertTrue(user.matchPassword("pw4"));

        StringWriter sw = new StringWriter();
        db.save(sw);

        String ncfg = sw.toString();
        log.info("Resulted configuration: \n{}", ncfg);

        Assert.assertFalse(ncfg.contains("role5"));
        Assert.assertTrue(ncfg.contains("role6"));
        Assert.assertTrue(ncfg.contains("role7"));
        Assert.assertTrue(ncfg.contains("group2"));
        Assert.assertTrue(ncfg.contains("group3"));
        Assert.assertFalse(ncfg.contains("user1"));
        Assert.assertTrue(ncfg.contains("user2"));
        Assert.assertTrue(ncfg.contains("user2@users.com"));
        Assert.assertFalse(ncfg.contains("user2 password"));
        Assert.assertTrue(ncfg.contains("user2 fullname"));
        Assert.assertTrue(ncfg.contains("user2 name"));
        Assert.assertTrue(ncfg.contains("user3"));
        Assert.assertTrue(ncfg.contains("user3 password"));
        Assert.assertTrue(ncfg.contains("user4"));
        Assert.assertTrue(ncfg.contains("user4added"));
    }

}
