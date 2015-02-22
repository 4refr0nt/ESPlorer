/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * {@code Converter} is responsible for converting a value from one type
 * to another.
 * <p>
 * The conversion methods can throw {@code RuntimeExceptions} in response
 * to a problem in conversion. For example, a {@code String} to {@code Integer}
 * converter might throw a {@code NumberFormatException} if the {@code String}
 * can't be parsed properly into an {@code Integer}.
 *
 * @param <S> the {@code Converter's} source type
 * @param <T> the {@code Converter's} target type
 *
 * @author Shannon Hickey
 * @author Jan Stola
 * @author Scott Violet
 */
public abstract class Converter<S, T> {

    /**
     * Converts a value from the source type to the target type.
     * Can throw a {@code RuntimeException} to indicate a problem
     * with the conversion.
     *
     * @param value the source value to convert
     * @return the value, converted to the target type
     */
    public abstract T convertForward(S value);

    /**
     * Converts a value from the target type to the source type.
     * Can throw a {@code RuntimeException} to indicate a problem
     * with the conversion.
     *
     * @param value the target value to convert
     * @return the value, converted to the source type
     */
    public abstract S convertReverse(T value);

    static final Converter BYTE_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return Byte.toString((Byte)value);
        }

        public Object convertReverse(Object value) {
            return Byte.parseByte((String)value);
        }
    };

    static final Converter SHORT_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return Short.toString((Short)value);
        }

        public Object convertReverse(Object value) {
            return Short.parseShort((String)value);
        }
    };

    static final Converter INT_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return Integer.toString((Integer)value);
        }

        public Object convertReverse(Object value) {
            return Integer.parseInt((String)value);
        }
    };

    static final Converter LONG_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return Long.toString((Long)value);
        }

        public Object convertReverse(Object value) {
            return Long.parseLong((String)value);
        }
    };

    static final Converter FLOAT_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return Float.toString((Float)value);
        }

        public Object convertReverse(Object value) {
            return Float.parseFloat((String)value);
        }
    };

    static final Converter DOUBLE_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return Double.toString((Double)value);
        }

        public Object convertReverse(Object value) {
            return Double.parseDouble((String)value);
        }
    };

    static final Converter CHAR_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return ((Character)value).toString();
        }

        public Object convertReverse(Object value) {
            String strVal = (String)value;

            if (strVal.length() != 1) {
                throw new IllegalArgumentException("String doesn't represent a char");
            }

            return strVal.charAt(0);
        }
    };

    static final Converter BOOLEAN_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return ((Boolean)value).toString();
        }

        public Object convertReverse(Object value) {
            return new Boolean((String)value);
        }
    };

    static final Converter INT_TO_BOOLEAN_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            if (((Integer)value).intValue() == 0) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        public Object convertReverse(Object value) {
            if (((Boolean)value).booleanValue()) {
                return 1;
            }
            return 0;
        }
    };

    static final Converter BIGINTEGER_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return ((BigInteger)value).toString();
        }

        public Object convertReverse(Object value) {
            return new BigInteger((String)value);
        }
    };

    static final Converter BIGDECIMAL_TO_STRING_CONVERTER = new Converter() {
        public Object convertForward(Object value) {
            return ((BigDecimal)value).toString();
        }

        public Object convertReverse(Object value) {
            return new BigDecimal((String)value);
        }
    };

    static final Object defaultConvert(Object source, Class<?> targetType) {
        Class sourceType = source.getClass();

        if (sourceType == targetType) {
            return source;
        }

        if (targetType == String.class) {
            if (sourceType == Byte.class) {
                return BYTE_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == Short.class) {
                return SHORT_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == Integer.class) {
                return INT_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == Long.class) {
                return LONG_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == Float.class) {
                return FLOAT_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == Double.class) {
                return DOUBLE_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == Boolean.class) {
                return BOOLEAN_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == Character.class) {
                return CHAR_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == BigInteger.class) {
                return BIGINTEGER_TO_STRING_CONVERTER.convertForward(source);
            } else if (sourceType == BigDecimal.class) {
                return BIGDECIMAL_TO_STRING_CONVERTER.convertForward(source);
            }
        } else if (sourceType == String.class) {
            if (targetType == Byte.class) {
                return BYTE_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == Short.class) {
                return SHORT_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == Integer.class) {
                return INT_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == Long.class) {
                return LONG_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == Float.class) {
                return FLOAT_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == Double.class) {
                return DOUBLE_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == Boolean.class) {
                return BOOLEAN_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == Character.class) {
                return CHAR_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == BigInteger.class) {
                return BIGINTEGER_TO_STRING_CONVERTER.convertReverse(source);
            } else if (targetType == BigDecimal.class) {
                return BIGDECIMAL_TO_STRING_CONVERTER.convertReverse(source);
            }
        } else if (sourceType == Integer.class && targetType == Boolean.class) {
            return INT_TO_BOOLEAN_CONVERTER.convertForward(source);
        } else if (sourceType == Boolean.class && targetType == Integer.class) {
            return INT_TO_BOOLEAN_CONVERTER.convertReverse(source);
        }

        return source;
    }
}
