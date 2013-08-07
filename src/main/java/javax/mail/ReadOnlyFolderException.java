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

/**
 * This exception is thrown when an attempt is made to open a folder
 * read-write access when the folder is marked read-only. <p>
 *
 * The getMessage() method returns more detailed information about the
 * error that caused this exception. <p>
 *
 * @author Jim Glennon
 */

public class ReadOnlyFolderException extends MessagingException {
    transient private Folder folder;

    private static final long serialVersionUID = 5711829372799039325L;
    
    /**
     * Constructs a ReadOnlyFolderException with the specified
     * folder and no detail message.
     *
     * @param folder	the Folder
     * @since 		JavaMail 1.2
     */
    public ReadOnlyFolderException(Folder folder) {
	this(folder, null);
    }

    /**
     * Constructs a ReadOnlyFolderException with the specified
     * detail message.
     *
     * @param folder 	The Folder
     * @param message	The detailed error message
     * @since 		JavaMail 1.2
     */
    public ReadOnlyFolderException(Folder folder, String message) {
	super(message);
	this.folder = folder;
    }

    /**
     * Constructs a ReadOnlyFolderException with the specified
     * detail message and embedded exception.  The exception is chained
     * to this exception.
     *
     * @param folder 	The Folder
     * @param message	The detailed error message
     * @param e		The embedded exception
     * @since		JavaMail 1.5
     */
    public ReadOnlyFolderException(Folder folder, String message, Exception e) {
	super(message, e);
	this.folder = folder;
    }

    /**
     * Returns the dead Folder object.
     * @since 		JavaMail 1.2
     */
    public Folder getFolder() {
	return folder;
    }
}
