package com.softmotions.weboot.cayenne;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.annotation.Nullable;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.postgresql.util.PGobject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBCayennePostgresModule implements Module {

    private final ObjectMapper mapper;

    public WBCayennePostgresModule() {
        this(new ObjectMapper());
    }

    public WBCayennePostgresModule(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void configure(Binder binder) {
        binder
                .bindList(Constants.SERVER_DEFAULT_TYPES_LIST)
                .add(new JacksonJSONType());
    }

    public class JacksonJSONType implements ExtendedType {

        @Override
        public String getClassName() {
            return ObjectNode.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement ps,
                                  Object value,
                                  int pos,
                                  int type,
                                  int scale) throws Exception {
            if (value == null) {
                ps.setNull(pos, type);
            } else {
                PGobject po = new PGobject();
                po.setType("jsonb");
                po.setValue(mapper.writeValueAsString(value));
                ps.setObject(pos, po);
            }
        }

        @Nullable
        @Override
        public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
            String value = rs.getString(index);
            if (value == null) {
                return null;
            } else {
                return mapper.readTree(value);
            }
        }

        @Nullable
        @Override
        public Object materializeObject(CallableStatement rs, int index, int type) throws Exception {
            String value = rs.getString(index);
            if (value == null) {
                return null;
            } else {
                return mapper.readTree(value);
            }
        }
    }
}

