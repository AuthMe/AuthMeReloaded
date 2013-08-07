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
 * This exception is thrown when a method is invoked on a Messaging object
 * and the Store that owns that object has died due to some reason.
 * This exception should be treated as a fatal error; in particular any
 * messaging object belonging to that Store must be considered invalid. <p>
 *
 * The connect method may be invoked on the dead Store object to 
 * revive it. <p>
 *
 * The getMessage() method returns more detailed information about the
 * error that caused this exception. <p>
 *
 * @author John Mani
 */

public class StoreClosedException extends MessagingException {
    transient private Store store;

    private static final long serialVersionUID = -3145392336120082655L;

    /**
     * Constructs a StoreClosedException with no detail message.
     *
     * @param store	The dead Store object
     */
    public StoreClosedException(Store store) {
	this(store, null);
    }

    /**
     * Constructs a StoreClosedException with the specified
     * detail message.
     *
     * @param store	The dead Store object
     * @param message	The detailed error message
     */
    public StoreClosedException(Store store, String message) {
	super(message);
	this.store = store;
    }

    /**
     * Constructs a StoreClosedException with the specified
     * detail message and embedded exception.  The exception is chained
     * to this exception.
     *
     * @param store	The dead Store object
     * @param message	The detailed error message
     * @param e		The embedded exception
     * @since		JavaMail 1.5
     */
    public StoreClosedException(Store store, String message, Exception e) {
	super(message, e);
	this.store = store;
    }

    /**
     * Returns the dead Store object 
     */
    public Store getStore() {
	return store;
    }
}
