package com.softmotions.weboot.cayenne;

import java.math.BigInteger;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.postgresql.util.PGobject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
        binder.bindList(ExtendedType.class, Constants.SERVER_DEFAULT_TYPES_LIST)
              .add(new JacksonJSONType(ObjectNode.class.getName()))
              .add(new JacksonJSONType(ArrayNode.class.getName()))
              .add(new JacksonJSONType(JsonNode.class.getName()))
              .add(new UUIDType())
              .add(new StringArrayType())
              .add(new BigIntegerType());
    }

    private static class BigIntegerType implements ExtendedType<BigInteger> {
        
        @Override
        public String getClassName() {
            return BigInteger.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement ps, BigInteger value, int pos, int type, int scale) throws Exception {
            if (value == null) {
                ps.setNull(pos, type);
            } else {
                ps.setString(pos, value.toString());
            }
        }

        @Override
        public BigInteger materializeObject(ResultSet rs, int index, int type) throws Exception {
            String val = rs.getString(index);
            if (val == null) {
                return null;
            } else {
                return new BigInteger(val);
            }
        }

        @Override
        public BigInteger materializeObject(CallableStatement rs, int index, int type) throws Exception {
            String val = rs.getString(index);
            if (val == null) {
                return null;
            } else {
                return new BigInteger(val);
            }
        }

        @Override
        public String toString(BigInteger value) {
            if (value == null) {
                return "NULL";
            }
            return value.toString();
        }
    }

    private static class StringArrayType implements ExtendedType<String[]> {

        @Override
        public String getClassName() {
            return String[].class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement ps, String[] value, int pos, int type, int scale) throws Exception {
            Connection conn = ps.getConnection();
            Array arr = conn.createArrayOf("text", value == null ? new String[0] : value);
            ps.setArray(pos, arr);
        }

        @Override
        public String[] materializeObject(ResultSet rs, int index, int type) throws Exception {
            Array arr = rs.getArray(index);
            if (arr == null) return new String[0];
            return (String[]) arr.getArray();
        }

        @Override
        public String[] materializeObject(CallableStatement rs, int index, int type) throws Exception {
            Array arr = rs.getArray(index);
            if (arr == null) return new String[0];
            return (String[]) arr.getArray();
        }

        @Override
        public String toString(String[] value) {
            return Arrays.toString(value);
        }
    }

    private static class UUIDType implements ExtendedType {

        @Override
        public String getClassName() {
            return UUID.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement ps, Object value, int pos, int type, int scale) throws Exception {
            if (value == null) {
                ps.setNull(pos, type);
            } else {
                if (!(value instanceof UUID)) {
                    value = UUID.fromString(value.toString());
                }
                ps.setObject(pos, value);
            }
        }

        @Override
        public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
            return rs.getObject(index);
        }

        @Override
        public Object materializeObject(CallableStatement rs, int index, int type) throws Exception {
            return rs.getObject(index);
        }

        @Override
        public String toString(Object value) {
            if (value == null) {
                return "NULL";
            }
            return value.toString();
        }
    }

    private class JacksonJSONType implements ExtendedType {

        private final String type;

        JacksonJSONType(String type) {
            this.type = type;
        }

        @Override
        public String getClassName() {
            return type;
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

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JacksonJSONType that = (JacksonJSONType) o;
            return Objects.equals(type, that.type);
        }

        public int hashCode() {
            return Objects.hash(type);
        }

        @Override
        public String toString(Object value) {
            if (value == null) {
                return "NULL";
            }
            return value.toString();
        }
    }
}

