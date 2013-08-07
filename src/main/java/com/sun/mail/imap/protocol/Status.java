/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.mail.imap.protocol;

import com.sun.mail.iap.*;

/**
 * STATUS response.
 *
 * @author  John Mani
 */

public class Status { 
    public String mbox = null;
    public int total = -1;
    public int recent = -1;
    public long uidnext = -1;
    public long uidvalidity = -1;
    public int unseen = -1;

    static final String[] standardItems =
	{ "MESSAGES", "RECENT", "UNSEEN", "UIDNEXT", "UIDVALIDITY" };

    public Status(Response r) throws ParsingException {
	mbox = r.readAtomString(); // mailbox := astring

	// Workaround buggy IMAP servers that don't quote folder names
	// with spaces.
	final StringBuffer buffer = new StringBuffer();
	boolean onlySpaces = true;

	while (r.peekByte() != '(' && r.peekByte() != 0) {
	    final char next = (char)r.readByte();

	    buffer.append(next);

	    if (next != ' ') {
		onlySpaces = false;
	    }
	}

	if (!onlySpaces) {
	    mbox = (mbox + buffer).trim();
	}

	if (r.readByte() != '(')
	    throw new ParsingException("parse error in STATUS");
	
	do {
	    String attr = r.readAtom();
	    if (attr.equalsIgnoreCase("MESSAGES"))
		total = r.readNumber();
	    else if (attr.equalsIgnoreCase("RECENT"))
		recent = r.readNumber();
	    else if (attr.equalsIgnoreCase("UIDNEXT"))
		uidnext = r.readLong();
	    else if (attr.equalsIgnoreCase("UIDVALIDITY"))
		uidvalidity = r.readLong();
	    else if (attr.equalsIgnoreCase("UNSEEN"))
		unseen = r.readNumber();
	} while (r.readByte() != ')');
    }

    public static void add(Status s1, Status s2) {
	if (s2.total != -1)
	    s1.total = s2.total;
	if (s2.recent != -1)
	    s1.recent = s2.recent;
	if (s2.uidnext != -1)
	    s1.uidnext = s2.uidnext;
	if (s2.uidvalidity != -1)
	    s1.uidvalidity = s2.uidvalidity;
	if (s2.unseen != -1)
	    s1.unseen = s2.unseen;
    }
}
