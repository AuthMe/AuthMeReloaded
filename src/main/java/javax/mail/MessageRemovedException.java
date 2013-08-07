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
 * The exception thrown when an invalid method is invoked on an expunged
 * Message. The only valid methods on an expunged Message are
 * <code>isExpunged()</code> and <code>getMessageNumber()</code>.
 *
 * @see	   javax.mail.Message#isExpunged()
 * @see	   javax.mail.Message#getMessageNumber()
 * @author John Mani
 */

public class MessageRemovedException extends MessagingException {

    private static final long serialVersionUID = 1951292550679528690L;

    /**
     * Constructs a MessageRemovedException with no detail message.
     */
    public MessageRemovedException() {
	super();
    }

    /**
     * Constructs a MessageRemovedException with the specified
     * detail message.
     *
     * @param s		The detailed error message
     */
    public MessageRemovedException(String s) {
	super(s);
    }

    /**
     * Constructs a MessageRemovedException with the specified
     * detail message and embedded exception.  The exception is chained
     * to this exception.
     *
     * @param s		The detailed error message
     * @param e		The embedded exception
     * @since		JavaMail 1.5
     */
    public MessageRemovedException(String s, Exception e) {
	super(s, e);
    }
}
