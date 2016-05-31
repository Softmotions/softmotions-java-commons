package com.softmotions.web.security;

import com.softmotions.commons.bean.BeanException;
import com.softmotions.commons.bean.BeanUtils;
import com.softmotions.commons.cont.ArrayUtils;
import com.softmotions.commons.io.Loader;

import com.google.common.collect.AbstractIterator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public class XMLWSUserDatabase implements WSUserDatabase {

    private static final Logger log = LoggerFactory.getLogger(XMLWSUserDatabase.class);

    private final XMLConfiguration xcfg;

    private final String databaseName;

    private final URL xmlLocationUrl;

    private final Map<String, WSGroup> groups = new HashMap<>();

    private final Map<String, WSUser> users = new HashMap<>();

    private final Map<String, WSRole> roles = new HashMap<>();

    private final Object lock = new Object();

    private boolean canSave;

    public boolean isCanUsersWrite() {
        return canSave;
    }

    public boolean isCanUsersAccessWrite() {
        return canSave;
    }

    public boolean isCanGroupsWrite() {
        return canSave;
    }

    public boolean isCanRolesWrite() {
        return canSave;
    }

    public int getWriteMask() {
        return canSave ? USERS_WRITABLE | GROUPS_WRITABLE | ROLES_WRITABLE | USERS_ACCESS_WRITABLE : 0;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public XMLWSUserDatabase(String dbName, String xmlLocation, boolean autoSave) {
        this.databaseName = dbName;
        this.xmlLocationUrl = Loader.getResourceAsUrl(xmlLocation, getClass());

        if (xmlLocationUrl == null) {
            throw new RuntimeException("Failed to find database xml file: " + xmlLocation);
        }

        try {
            xcfg = new XMLConfiguration(xmlLocationUrl);
        } catch (ConfigurationException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
        if (xmlLocationUrl.getProtocol() != null &&
            xmlLocationUrl.getProtocol().startsWith("file")) {
            try {
                File f = new File(xmlLocationUrl.toURI());
                canSave = f.canWrite();
            } catch (URISyntaxException e) {
                canSave = false;
            }
        } else {
            canSave = false;
        }

        if (canSave) {
            xcfg.setAutoSave(autoSave);
        }
        reload();
        log.info("XMLWSUserDatabase allocated " + this);
    }


    private void reload() {
        log.info("Loading xml file configuration");
        synchronized (lock) {
            roles.clear();
            groups.clear();
            users.clear();
            for (HierarchicalConfiguration cfg : xcfg.configurationsAt("role")) {
                String name = cfg.getString("[@name]");
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                roles.put(name, new WSRoleImpl(cfg));
            }

            for (HierarchicalConfiguration cfg : xcfg.configurationsAt("group")) {
                String name = cfg.getString("[@name]");
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                groups.put(name, new WSGroupImpl(cfg));
            }

            for (HierarchicalConfiguration cfg : xcfg.configurationsAt("user")) {
                String name = cfg.getString("[@name]");
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                users.put(name, new WSUserImpl(cfg));
            }
        }
    }


    public Iterator<WSGroup> getGroups() {
        synchronized (lock) {
            return IteratorUtils.arrayIterator(groups.values().toArray(new WSGroup[groups.size()]));
        }
    }

    public Iterator<WSRole> getRoles() {
        synchronized (lock) {
            return IteratorUtils.arrayIterator(roles.values().toArray(new WSRole[roles.size()]));
        }
    }

    public Iterator<WSUser> getUsers() {
        synchronized (lock) {
            return IteratorUtils.arrayIterator(users.values().toArray(new WSUser[users.size()]));
        }
    }

    public int getUsersCount() {
        synchronized (lock) {
            return users.size();
        }
    }

    public int getUsersCount(String query) {
        if (query != null) {
            query = query.trim().toLowerCase();
        }
        int c = 0;
        synchronized (lock) {
            for (final WSUser u : users.values()) {
                if (((WSUserImpl) u).isMatchedQuery(query)) {
                    ++c;
                }
            }
        }
        return c;
    }

    public int getActiveUsersCount(String query) {
        if (query != null) {
            query = query.trim().toLowerCase();
        }
        int c = 0;
        synchronized (lock) {
            for (final WSUser u : users.values()) {
                if (((WSUserImpl) u).isMatchedQuery(query) && (u.getRoles().hasNext() || u.getGroups().hasNext())) {
                    ++c;
                }
            }
        }
        return c;
    }

    public Iterator<WSUser> getUsers(String query,
                                     final String orderProperty,
                                     final boolean desc,
                                     int skip,
                                     int limit) {
        WSUser[] uarr;
        synchronized (lock) {
            uarr = users.values().toArray(new WSUser[users.size()]);
        }

        return getUsersInternal(uarr, query, orderProperty, desc, skip, limit);
    }

    public Iterator<WSUser> getActiveUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
        WSUser[] uarr;
        synchronized (lock) {
            Collection<WSUser> ucol = CollectionUtils.select(users.values(), new Predicate() {
                public boolean evaluate(Object u) {
                    return u instanceof WSUser && ((WSUser)u).getRoles().hasNext() || ((WSUser)u).getGroups().hasNext();
                }
            });

            uarr = ucol.toArray(new WSUser[ucol.size()]);
        }

        return getUsersInternal(uarr, query, orderProperty, desc, skip, limit);
    }

    private Iterator<WSUser> getUsersInternal(WSUser[] uarr, String query, final String orderProperty, final boolean desc, int skip, int limit) {
        int i = 0;
        if (query != null) {
            query = query.trim().toLowerCase();
        }
        if (orderProperty != null) { //sort users
            try {
                final Collator coll = Collator.getInstance();
                final Class pclazz = BeanUtils.getPropertyType(WSUser.class, orderProperty, false);
                final boolean comparable = Comparable.class.isAssignableFrom(pclazz);
                Arrays.sort(uarr, new Comparator<WSUser>() {
                    public int compare(WSUser u1, WSUser u2) {
                        int res = 0;
                        try {
                            Object v1 = BeanUtils.getProperty(u1, orderProperty);
                            Object v2 = BeanUtils.getProperty(u2, orderProperty);
                            if (comparable) {
                                res = ObjectUtils.compare((Comparable) v1, (Comparable) v2);
                            } else {
                                res = coll.compare(String.valueOf(v1), String.valueOf(v2));
                            }
                        } catch (BeanException e) {
                            log.error("", e);
                        }
                        if (desc) {
                            res *= -1;
                        }
                        return res;
                    }
                });
            } catch (BeanException e) {
                log.error("", e);
                throw new RuntimeException(e);
            }
        }
        if (skip > 0) {
            for (; skip > 0 && i < uarr.length; ++i) {
                if (((WSUserImpl) uarr[i]).isMatchedQuery(query)) {
                    --skip;
                }
            }
        }
        if (i >= uarr.length) {
            return Collections.emptyIterator();
        }
        return new WSUsersIterator(i, limit, uarr, query);
    }


    private static final class WSUsersIterator extends AbstractIterator<WSUser> {

        private int pos;

        private int limit;

        private final WSUser[] users;

        private final String query;

        private WSUsersIterator(int start, int limit, WSUser[] users, String query) {
            this.pos = start;
            this.limit = limit;
            this.query = query;
            this.users = users;
        }

        protected WSUser computeNext() {
            WSUser user = null;
            while (user == null) {
                if (pos >= users.length || limit <= 0) {
                    return endOfData();
                }
                if (((WSUserImpl) users[pos]).isMatchedQuery(query)) {
                    user = users[pos];
                    limit--;
                }
                pos++;
            }
            return user;
        }
    }

    public WSGroup findGroup(String groupname) {
        synchronized (lock) {
            return groups.get(groupname);
        }
    }

    public WSRole findRole(String rolename) {
        synchronized (lock) {
            return roles.get(rolename);
        }
    }

    public WSUser findUser(String username) {
        synchronized (lock) {
            return users.get(username);
        }
    }

    public WSGroup createGroup(String groupname, String description) {
        WSGroup group;
        synchronized (lock) {
            group = groups.get(groupname);
            if (group != null) {
                return group;
            }
            xcfg.addProperty("group(-1)[@name]", groupname);
            if (description != null) {
                xcfg.addProperty("group[@description]", description);
            }
            SubnodeConfiguration cfg = xcfg.configurationAt("group(" + groups.size() + ")", true);
            group = new WSGroupImpl(cfg);
            groups.put(groupname, group);
        }
        return group;
    }

    public WSRole createRole(String rolename, String description) {
        WSRole role;
        synchronized (lock) {
            role = roles.get(rolename);
            if (role != null) {
                return role;
            }
            xcfg.addProperty("role(-1)[@name]", rolename);
            if (description != null) {
                xcfg.addProperty("role[@description]", description);
            }
            SubnodeConfiguration cfg = xcfg.configurationAt("role(" + roles.size() + ")", true);
            role = new WSRoleImpl(cfg);
            roles.put(rolename, role);
        }
        return role;
    }

    public WSUser createUser(String username, String password, String fullName) {
        WSUser user;
        synchronized (lock) {
            user = users.get(username);
            if (user != null) {
                return user;
            }
            xcfg.addProperty("user(-1)[@name]", username);
            if (password != null) {
                xcfg.addProperty("user[@password]", password);
            }
            if (fullName != null) {
                xcfg.addProperty("user[@fullName]", fullName);
            }
            SubnodeConfiguration cfg = xcfg.configurationAt("user(" + users.size() + ")", true);
            user = new WSUserImpl(cfg);
            users.put(username, user);
        }
        return user;
    }

    public void removeGroup(WSGroup group) {
        synchronized (lock) {
            List<HierarchicalConfiguration> xgroups = xcfg.configurationsAt("group");
            for (int i = 0, l = xgroups.size(); i < l; ++i) {
                HierarchicalConfiguration hc = xgroups.get(i);
                String name = hc.getString("[@name]");
                if (group.getName().equals(name)) {
                    for (final WSUser u : users.values()) {
                        u.removeGroup(group);
                    }
                    hc.clear();
                    groups.remove(group.getName());
                    break;
                }
            }
        }
    }

    public void removeRole(WSRole role) {
        synchronized (lock) {
            List<HierarchicalConfiguration> xroles = xcfg.configurationsAt("role");
            for (int i = 0, l = xroles.size(); i < l; ++i) {
                HierarchicalConfiguration hc = xroles.get(i);
                String name = hc.getString("[@name]");
                if (role.getName().equals(name)) {
                    for (final WSGroup g : groups.values()) {
                        g.removeRole(role);
                    }
                    for (final WSUser u : users.values()) {
                        u.removeRole(role);
                    }
                    hc.clear();
                    roles.remove(role.getName());
                    break;
                }
            }
        }
    }

    public void removeUser(WSUser user) {
        synchronized (lock) {
            List<HierarchicalConfiguration> xusers = xcfg.configurationsAt("user");
            for (int i = 0, l = xusers.size(); i < l; ++i) {
                HierarchicalConfiguration hc = xusers.get(i);
                String name = hc.getString("[@name]");
                if (user.getName().equals(name)) {
                    hc.clear();
                    users.remove(user.getName());
                    break;
                }
            }
        }
    }

    public void close() throws IOException {
        synchronized (lock) {
            if (xcfg.isAutoSave()) {
                try {
                    xcfg.save();
                } catch (ConfigurationException e) {
                    log.error("", e);
                }
            }
        }
    }

    public void save() throws ConfigurationException {
        synchronized (lock) {
            xcfg.save();
        }
    }

    public void save(Writer w) throws ConfigurationException {
        synchronized (lock) {
            xcfg.save(w);
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("XMLWSUserDatabase{");
        sb.append("databaseName='").append(databaseName).append('\'');
        sb.append(", xmlLocationUrl=").append(xmlLocationUrl);
        sb.append(", canSave=").append(canSave);
        sb.append('}');
        return sb.toString();
    }

    private List<WSGroup> projectGroups(String... names) {
        List<WSGroup> res = new ArrayList<>(names.length);
        synchronized (lock) {
            for (final String name : names) {
                WSGroup v = groups.get(name);
                if (v != null) {
                    res.add(v);
                }
            }
        }
        return res;
    }

    private List<WSRole> projectRoles(String... names) {
        List<WSRole> res = new ArrayList<>(names.length);
        synchronized (lock) {
            for (final String name : names) {
                WSRole v = roles.get(name);
                if (v != null) {
                    res.add(v);
                }
            }
        }
        return res;
    }

    private List<WSRole> projectRoles(Collection<String> names) {
        List<WSRole> res = new ArrayList<>(names.size());
        synchronized (lock) {
            for (final String name : names) {
                WSRole v = roles.get(name);
                if (v != null) {
                    res.add(v);
                }
            }
        }
        return res;
    }

    private List<WSUser> projectUsers(WSGroupImpl grp) {
        String gname = grp.getName();
        List<WSUser> res = new ArrayList<>();
        synchronized (lock) {
            for (final WSUser o : users.values()) {
                WSUserImpl u = (WSUserImpl) o;
                int ind = ArrayUtils.indexOf(u.groupNames, gname);
                if (ind != -1) {
                    res.add(u);
                }
            }
        }
        return res;
    }


    private final class WSUserImpl extends AbstractWSUser {

        private final HierarchicalConfiguration cfg;

        private String[] roleNames;

        private String[] groupNames;

        private WSUserImpl(HierarchicalConfiguration cfg) {
            super(cfg.getString("[@name]"), cfg.getString("[@fullName]"), cfg.getString("[@password]"));
            this.cfg = cfg;
            this.email = cfg.getString("[@email]");
            this.roleNames = cfg.getStringArray("[@roles]");
            this.groupNames = cfg.getStringArray("[@groups]");
        }

        public WSUserDatabase getUserDatabase() {
            return XMLWSUserDatabase.this;
        }

        public Iterator<WSRole> getRoles() {
            synchronized (lock) {
                if (groupNames.length == 0) {
                    return projectRoles(roleNames).iterator();
                }
                Set<String> effectiveRoles = new HashSet<>();
                Collections.addAll(effectiveRoles, roleNames);
                for (final String gn : groupNames) {
                    WSGroupImpl group = (WSGroupImpl) groups.get(gn);
                    Collections.addAll(effectiveRoles, group.roleNames);
                }
                return projectRoles(effectiveRoles).iterator();
            }
        }

        public Iterator<WSGroup> getGroups() {
            synchronized (lock) {
                return projectGroups(groupNames).iterator();
            }
        }

        public boolean isInGroup(WSGroup group) {
            synchronized (lock) {
                String n = group.getName();
                for (final String g : groupNames) {
                    if (g.equals(n)) {
                        return true;
                    }
                }
            }
            return false;
        }


        public boolean isHasAnyRole(String... rlist) {
            synchronized (lock) {
                for (final String r : roleNames) {
                    if (ArrayUtils.indexOf(rlist, r) != -1) {
                        return true;
                    }
                }
                for (final String g : groupNames) {
                    WSGroupImpl gi = (WSGroupImpl) groups.get(g);
                    if (gi == null) {
                        continue;
                    }
                    for (final String gr : gi.roleNames) {
                        if (ArrayUtils.indexOf(rlist, gr) != -1) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public boolean isInRole(WSRole role) {
            synchronized (lock) {
                String n = role.getName();
                for (final String r : roleNames) {
                    if (r.equals(n)) {
                        return true;
                    }
                }
                for (final String g : groupNames) {
                    WSGroupImpl gi = (WSGroupImpl) groups.get(g);
                    if (gi != null) {
                        for (final String r : gi.roleNames) {
                            if (r.equals(n)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        public void addGroup(WSGroup group) {
            synchronized (lock) {
                if (ArrayUtils.indexOf(roleNames, group.getName()) == -1) {
                    cfg.addProperty("[@groups]", group.getName());
                    groupNames = cfg.getStringArray("[@groups]");
                }
            }
        }

        public void addRole(WSRole role) {
            synchronized (lock) {
                if (ArrayUtils.indexOf(roleNames, role.getName()) == -1) {
                    cfg.addProperty("[@roles]", role.getName());
                    roleNames = cfg.getStringArray("[@roles]");
                }
            }
        }

        public void removeGroup(WSGroup group) {
            synchronized (lock) {
                int ind = ArrayUtils.indexOf(groupNames, group.getName());
                if (ind == -1) {
                    return;
                }
                String[] nGroupNames = new String[groupNames.length - 1];
                for (int i = 0, j = 0; i < groupNames.length && j < nGroupNames.length; ++i) {
                    if (i != ind) {
                        nGroupNames[j] = groupNames[i];
                        ++j;
                    }
                }
                cfg.setProperty("[@groups]", nGroupNames);
                this.groupNames = nGroupNames;
            }
        }

        public void removeRole(WSRole role) {
            synchronized (lock) {
                int ind = ArrayUtils.indexOf(roleNames, role.getName());
                if (ind == -1) {
                    return;
                }
                String[] nRoleNames = new String[roleNames.length - 1];
                for (int i = 0, j = 0; i < roleNames.length && j < nRoleNames.length; ++i) {
                    if (i != ind) {
                        nRoleNames[j] = roleNames[i];
                        ++j;
                    }
                }
                cfg.setProperty("[@roles]", nRoleNames);
                this.roleNames = nRoleNames;
            }
        }

        public void removeGroups() {
            synchronized (lock) {
                cfg.clearProperty("[@groups]");
            }
        }

        public void removeRoles() {
            synchronized (lock) {
                cfg.clearProperty("[@roles]");
            }
        }

        private boolean isMatchedQuery(String query) {
            if (query == null || query.isEmpty()) {
                return true;
            }
            synchronized (lock) {
                if (getName() != null && getName().toLowerCase().startsWith(query)) {
                    return true;
                }
                if (getEmail() != null && getEmail().toLowerCase().startsWith(query)) {
                    return true;
                }
                if (getFullName() != null && getFullName().toLowerCase().contains(query)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final class WSRoleImpl extends AbstractWSRole {

        private final HierarchicalConfiguration cfg;

        private WSRoleImpl(HierarchicalConfiguration cfg) {
            super(cfg.getString("[@name]"), cfg.getString("[@description]"));
            this.cfg = cfg;
        }

        public WSUserDatabase getUserDatabase() {
            return XMLWSUserDatabase.this;
        }
    }


    private class WSGroupImpl extends AbstractWSGroup {

        private final HierarchicalConfiguration cfg;

        private String[] roleNames;

        private WSGroupImpl(HierarchicalConfiguration cfg) {
            super(cfg.getString("[@name]"), cfg.getString("[@description]"));
            this.cfg = cfg;
            this.roleNames = cfg.getStringArray("[@roles]");
        }

        public boolean isInRole(WSRole role) {
            synchronized (lock) {
                String rname = role.getName();
                for (final String r : roleNames) {
                    if (r.equals(rname)) {
                        return true;
                    }
                }
                return false;
            }
        }

        public Iterator<WSRole> getRoles() {
            synchronized (lock) {
                return projectRoles(roleNames).iterator();
            }
        }

        public WSUserDatabase getUserDatabase() {
            return XMLWSUserDatabase.this;
        }

        public Iterator<WSUser> getUsers() {
            synchronized (lock) {
                return projectUsers(this).iterator();
            }
        }

        public void addRole(WSRole role) {
            synchronized (lock) {
                if (ArrayUtils.indexOf(roleNames, role.getName()) == -1) {
                    cfg.addProperty("[@roles]", role.getName());
                    roleNames = cfg.getStringArray("[@roles]");
                }
            }
        }

        public void removeRole(WSRole role) {
            synchronized (lock) {
                int ind = ArrayUtils.indexOf(roleNames, role.getName());
                if (ind == -1) {
                    return;
                }
                String[] nRoleNames = new String[roleNames.length - 1];
                for (int i = 0, j = 0; i < roleNames.length && j < nRoleNames.length; ++i) {
                    if (i != ind) {
                        nRoleNames[j] = roleNames[i];
                        ++j;
                    }
                }
                cfg.setProperty("[@roles]", nRoleNames);
                this.roleNames = nRoleNames;
            }
        }

        public void removeRoles() {
            synchronized (lock) {
                cfg.clearProperty("[@roles]");
            }
        }
    }
}