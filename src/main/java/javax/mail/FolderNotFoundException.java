/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.mail;

import java.lang.*;

/**
 * This exception is thrown by Folder methods, when those
 * methods are invoked on a non existent folder.
 *
 * @author John Mani
 */

public class FolderNotFoundException extends MessagingException {
    transient private Folder folder;

    private static final long serialVersionUID = 472612108891249403L;

    /**
     * Constructs a FolderNotFoundException with no detail message.
     */
    public FolderNotFoundException() {
	super();
    }

    /**
     * Constructs a FolderNotFoundException.
     *
     * @param folder	The Folder
     * @since		JavaMail 1.2 
     */
    public FolderNotFoundException(Folder folder) {
	super();
        this.folder = folder;
    }

    /**
     * Constructs a FolderNotFoundException with the specified
     * detail message.
     *
     * @param folder	The Folder
     * @param s		The detailed error message
     * @since		JavaMail 1.2
     */
    public FolderNotFoundException(Folder folder, String s) {
	super(s);
	this.folder = folder;
    }

    /**
     * Constructs a FolderNotFoundException with the specified
     * detail message and embedded exception.  The exception is chained
     * to this exception.
     *
     * @param folder	The Folder
     * @param s		The detailed error message
     * @param e		The embedded exception
     * @since		JavaMail 1.5
     */
    public FolderNotFoundException(Folder folder, String s, Exception e) {
	super(s, e);
	this.folder = folder;
    }

    /**
     * Constructs a FolderNotFoundException with the specified detail message
     * and the specified folder.
     *
     * @param s		The detail message
     * @param folder	The Folder
     */
    public FolderNotFoundException(String s, Folder folder) {
	super(s);
	this.folder = folder;
    }

    /**
     * Returns the offending Folder object.
     *
     * @return	the Folder object. Note that the returned value can be
     * 		<code>null</code>.
     */
    public Folder getFolder() {
	return folder;
    }
}
