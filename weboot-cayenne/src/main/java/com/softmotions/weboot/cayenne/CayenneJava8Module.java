package com.softmotions.weboot.cayenne;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class CayenneJava8Module implements Module {

    @Override
    public void configure(Binder binder) {
        binder
                .bindList(Constants.SERVER_DEFAULT_TYPES_LIST)
                .add(new LocalDateType())
                .add(new LocalTimeType())
                .add(new LocalDateTimeType());
    }


    public static class LocalDateTimeType implements ExtendedType {

        @Override
        public String getClassName() {
            return LocalDateTime.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement statement, Object value, int pos, int type, int scale) throws Exception {
            statement.setTimestamp(pos, Timestamp.valueOf((LocalDateTime) value));
        }

        @Override
        public LocalDateTime materializeObject(ResultSet rs, int index, int type) throws Exception {
            Timestamp timestamp = rs.getTimestamp(index);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }

        @Override
        public Object materializeObject(CallableStatement rs, int index, int type) throws Exception {
            Timestamp timestamp = rs.getTimestamp(index);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }
    }


    public static class LocalDateType implements ExtendedType {

        @Override
        public String getClassName() {
            return LocalDate.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement statement, Object value, int pos, int type, int scale) throws Exception {
            statement.setDate(pos, Date.valueOf((LocalDate) value));
        }

        @Override
        public LocalDate materializeObject(ResultSet rs, int index, int type) throws Exception {
            Date date = rs.getDate(index);
            return date != null ? date.toLocalDate() : null;
        }

        @Override
        public LocalDate materializeObject(CallableStatement rs, int index, int type) throws Exception {
            Date date = rs.getDate(index);
            return date != null ? date.toLocalDate() : null;
        }
    }

    public static class LocalTimeType implements ExtendedType {

        @Override
        public String getClassName() {
            return LocalTime.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement statement, Object value, int pos, int type, int scale) throws Exception {
            statement.setTime(pos, Time.valueOf((LocalTime) value));
        }

        @Override
        public LocalTime materializeObject(ResultSet rs, int index, int type) throws Exception {
            Time time = rs.getTime(index);
            return time != null ? time.toLocalTime() : null;
        }

        @Override
        public Object materializeObject(CallableStatement rs, int index, int type) throws Exception {
            Time time = rs.getTime(index);
            return time != null ? time.toLocalTime() : null;
        }
    }
}
