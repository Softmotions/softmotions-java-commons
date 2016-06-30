package com.softmotions.web.security;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public class BeanUtils {

    /**
     * The argument type list for a getter method.
     */
    public static final Class[] GETTER_ARG_TYPES = new Class[]{};
    /**
     * The argument list for a getter method.
     */
    public static final Object[] GETTER_ARGS = new Object[]{};

    private BeanUtils() {
    }

    /**
     * Gets a property from the given bean.
     *
     * @param bean         The bean to read property from.
     * @param propertyName The name of the property to read.
     * @return The determined value.
     * @throws BeanException In case the bean access failed.
     */
    public static Object getProperty(Object bean, String propertyName) {
        return getProperty(bean, propertyName, false);
    }

    /**
     * Gets a property from the given bean.
     *
     * @param clazz        The class to determine the property type for.
     * @param propertyName The name of the property to read.
     * @param lenient      If true is passed for this attribute, null will returned for
     *                     in case no matching getter method is defined, else an Exception will be throw
     *                     in this case.
     * @return The determined value.
     * @throws BeanException In case the bean access failed.
     */
    public static Class getPropertyType(Class clazz, String propertyName, boolean lenient) {

        try {
            // getting property object from bean using "getNnnn", where nnnn is parameter name
            Method getterMethod = null;
            try {
                // first trying form getPropertyNaae for regular value
                String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                getterMethod = clazz.getMethod(getterName, GETTER_ARG_TYPES);
            } catch (NoSuchMethodException ex) {
                // next trying isPropertyNaae for possible boolean
                String getterName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                getterMethod = clazz.getMethod(getterName, GETTER_ARG_TYPES);
            }
            return getterMethod.getReturnType();
        } catch (NoSuchMethodError | NoSuchMethodException ex) {
            if (!lenient) {
                throw new RuntimeException("Property '" + propertyName + "' is undefined for given bean from class " + clazz.getName() + ".");
            }
        }
        return null;
    }

    /**
     * Gets a property from the given bean.
     *
     * @param bean         The bean to read property from.
     * @param propertyName The name of the property to read.
     * @param lenient      If true is passed for this attribute, null will returned for
     *                     in case no matching getter method is defined, else an Exception will be throw
     *                     in this case.
     * @return The determined value.
     * @throws BeanException In case the bean access failed.
     */
    public static Object getProperty(Object bean, String propertyName, boolean lenient) {

        try {
            // getting property object from bean using "getNnnn", where nnnn is parameter name
            Method getterMethod = null;
            try {
                // first trying form getPropertyNaae for regular value
                String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                Class paramClass = bean.getClass();
                getterMethod = paramClass.getMethod(getterName, GETTER_ARG_TYPES);
            } catch (NoSuchMethodException ignored) {
                // next trying isPropertyNaae for possible boolean
                String getterName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                Class paramClass = bean.getClass();
                getterMethod = paramClass.getMethod(getterName, GETTER_ARG_TYPES);
            }
            return getterMethod.invoke(bean, GETTER_ARGS);
        } catch (NoSuchMethodError ex) {
            if (!lenient) {
                throw new RuntimeException("Property '" + propertyName + "' is undefined for given bean from class " + bean.getClass().getName() + ".", ex);
            }
        } catch (NoSuchMethodException ignored) {
            if (!lenient) {
                throw new RuntimeException("Property '" + propertyName + "' is undefined for given bean from class " + bean.getClass().getName() + ".");
            }
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Property '" + propertyName + "' could not be evaluated for given bean from class " + bean.getClass().getName() + ".", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Property '" + propertyName + "' could not be accessed for given bean from class " + bean.getClass().getName() + ".", ex);
        }
        return null;
    }

    /**
     * Invokes a getter method with the given value.
     *
     * @param bean         The bean to set a property at.
     * @param getterMethod The setter method to invoke.
     * @return The determined value.
     * @throws BeanException In case the bean access failed.
     */
    public static Object invokeGetter(Object bean, Method getterMethod) {
        if (bean == null) {
            return null;
        }
        try {
            return getterMethod.invoke(bean, GETTER_ARGS);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException ex) {
            throw new RuntimeException("Failed to call getter method '" + getterMethod.getName() + "' for given bean from class " + bean.getClass().getName() + ".", ex);
        }
    }


    /**
     * Sets a property at the given bean.
     *
     * @param bean         The bean to set a property at.
     * @param propertyName The name of the property to set.
     * @param value        The value to set for the property.
     * @throws BeanException In case the bean access failed.
     */
    public static void setProperty(Object bean, String propertyName, Object value) {
        Class valueClass = null;
        try {
            // getting property object from bean using "setNnnn", where nnnn is parameter name
            Method setterMethod = null;
            // first trying form getPropertyNaae for regular value
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Class paramClass = bean.getClass();
            if (value != null) {
                valueClass = value.getClass();
                Class[] setterArgTypes = new Class[]{valueClass};
                setterMethod = paramClass.getMethod(setterName, setterArgTypes);
            } else {
                Method[] methods = paramClass.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method m = methods[i];
                    if (m.getName().equals(setterName) && (m.getParameterTypes().length == 1)) {
                        setterMethod = m;
                        break;
                    }
                }
            }
            if (setterMethod == null) {
                throw new NoSuchMethodException(setterName);
            }
            Object[] setterArgs = new Object[]{value};
            setterMethod.invoke(bean, setterArgs);
        } catch (NoSuchMethodError | NoSuchMethodException ex) {
            throw new RuntimeException("No setter method found for property '" + propertyName + "' and type " + valueClass + " at given bean from class " + bean.getClass().getName() + ".", ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Property '" + propertyName + "' could not be set for given bean from class " + bean.getClass().getName() + ".", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Property '" + propertyName + "' could not be accessed for given bean from class " + bean.getClass().getName() + ".", ex);
        }
    }

    /**
     * Checks if the given string is null or has zero length.
     *
     * @param value The string to check.
     * @return true if the given string is null or has zero length.
     */
    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Checks if the given objects are equal.
     *
     * @param a The object to compare with object b.
     * @param b The object to compare with object a.
     * @return true if the objects are equal or are both null.
     */
    public static boolean equals(Object a, Object b) {
        if ((a == null) || (b == null)) {
            return a == b;
        }
        return a.equals(b);
    }

    /**
     * Checks if the given objects are equal.
     *
     * @param a             The object to compare with object b.
     * @param b             The object to compare with object a.
     * @param caseSensitive Tells whether or not to compare string values case sensitive.
     *                      This parameter is only respected, in case a is a <code>java.lang.String</code> instance.
     * @return true if the objects are equal or are both null.
     */
    public static boolean equals(Object a, Object b, boolean caseSensitive) {
        if ((a == null) || (b == null)) {
            return a == b;
        }
        if (a instanceof String) {
            return caseSensitive ? Helpers.equalsIgnoreCase(a.toString(), b.toString()) :
                   Objects.equals(a.toString(), b.toString());
        }
        return a.equals(b);
    }

    /**
     * Compares the given values.
     *
     * @param a The object to compare with object b.
     * @param b The object to compare with object a.
     * @return true if the objects are equal or are both null.
     */
    public static int compare(Comparable a, Comparable b) {
        if ((a == null) || (b == null)) {
            if (a == b) {
                return 0;
            }
            if (a == null) {
                return -1;
            }
            return 1;
        }
        return a.compareTo(b);
    }

    /**
     * Checks if the given object is between the last two parameter values.
     *
     * @param obj  The object to check.
     * @param from The lower value to compare.
     * @param to   The upper value to compare.
     * @return true if <code>obj</code> is between <code>from</code> and <code>to</code>.
     */
    public static boolean between(Comparable obj, Comparable from, Comparable to) {
        int compareObjFrom = compare(obj, from);
        int compareObjTo = compare(obj, to);
        return (compareObjFrom >= 0) && (compareObjTo <= 0);
    }

    /**
     * Gets the textual representation of the given object.
     *
     * @param obj The object to get the extual representation of.
     * @return The textual representation of the given object or null.
     */
    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
}
