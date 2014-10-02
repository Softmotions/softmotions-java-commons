/*
 * Copyright (C) 2003, 2004, 2005 by Christian Lauer.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://sourceforge.net/projects/ujac
 */

package com.softmotions.commons.bean;

/**
 * Name: BeanException<br>
 * Description: A class for bean related exceptions.
 *
 * @author lauerc
 */
public class BeanException extends Exception {

    /**
     * The serial version UID.
     */
    static final long serialVersionUID = -593061662550540500L;

    /**
     * Creates an instance with no specific arguments.
     */
    public BeanException() {
    }

    /**
     * Creates an instance with specific arguments.
     *
     * @param msg The error message.
     */
    public BeanException(String msg) {
        super(msg);
    }

    /**
     * Constructs a BeanException instance with no specific arguments.
     *
     * @param msg   The error message.
     * @param cause The cause of the error.
     */
    public BeanException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
