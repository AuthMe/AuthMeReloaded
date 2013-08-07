/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import com.sun.mail.util.*;
import com.sun.mail.iap.*;

/**
 * This class represents a FETCH response obtained from the input stream
 * of an IMAP server.
 *
 * @author  John Mani
 * @author  Bill Shannon
 */

public class FetchResponse extends IMAPResponse {
    /*
     * Regular Items are saved in the items array.
     * Extension items (items handled by subclasses
     * that extend the IMAP provider) are saved in the
     * extensionItems map, indexed by the FETCH item name.
     * The map is only created when needed.
     *
     * XXX - Should consider unifying the handling of
     * regular items and extension items.
     */
    private Item[] items;
    private Map extensionItems;
    private final FetchItem[] fitems;

    public FetchResponse(Protocol p) 
		throws IOException, ProtocolException {
	super(p);
	fitems = null;
	parse();
    }

    public FetchResponse(IMAPResponse r)
		throws IOException, ProtocolException {
	this(r, null);
    }

    /**
     * Construct a FetchResponse that handles the additional FetchItems.
     *
     * @since JavaMail 1.4.6
     */
    public FetchResponse(IMAPResponse r, FetchItem[] fitems)
		throws IOException, ProtocolException {
	super(r);
	this.fitems = fitems;
	parse();
    }

    public int getItemCount() {
	return items.length;
    }

    public Item getItem(int index) {
	return items[index];
    }

    public Item getItem(Class c) {
	for (int i = 0; i < items.length; i++) {
	    if (c.isInstance(items[i]))
		return items[i];
	}

	return null;
    }

    public static Item getItem(Response[] r, int msgno, Class c) {
	if (r == null)
	    return null;

	for (int i = 0; i < r.length; i++) {

	    if (r[i] == null ||
		!(r[i] instanceof FetchResponse) ||
		((FetchResponse)r[i]).getNumber() != msgno)
		continue;

	    FetchResponse f = (FetchResponse)r[i];
	    for (int j = 0; j < f.items.length; j++) {
		if (c.isInstance(f.items[j]))
		    return f.items[j];
	    }
	}

	return null;
    }

    /**
     * Return a map of the extension items found in this fetch response.
     * The map is indexed by extension item name.  Callers should not
     * modify the map.
     *
     * @since JavaMail 1.4.6
     */
    public Map getExtensionItems() {
	if (extensionItems == null)
	    extensionItems = new HashMap();
	return extensionItems;
    }

    private final static char[] HEADER = {'.','H','E','A','D','E','R'};
    private final static char[] TEXT = {'.','T','E','X','T'};

    private void parse() throws ParsingException {
	skipSpaces();
	if (buffer[index] != '(')
	    throw new ParsingException(
		"error in FETCH parsing, missing '(' at index " + index);

	Vector v = new Vector();
	Item i = null;
	do {
	    index++; // skip '(', or SPACE

	    if (index >= size)
		throw new ParsingException(
		"error in FETCH parsing, ran off end of buffer, size " + size);

	    i = parseItem();
	    if (i != null)
		v.addElement(i);
	    else if (!parseExtensionItem())
		throw new ParsingException(
		"error in FETCH parsing, unrecognized item at index " + index);
	} while (buffer[index] != ')');

	index++; // skip ')'
	items = new Item[v.size()];
	v.copyInto(items);
    }

    /**
     * Parse the item at the current position in the buffer,
     * skipping over the item if successful.  Otherwise, return null
     * and leave the buffer position unmodified.
     */
    private Item parseItem() throws ParsingException {
	switch (buffer[index]) {
	case 'E': case 'e':
	    if (match(ENVELOPE.name))
		return new ENVELOPE(this);
	    break;
	case 'F': case 'f':
	    if (match(FLAGS.name))
		return new FLAGS((IMAPResponse)this);
	    break;
	case 'I': case 'i':
	    if (match(INTERNALDATE.name))
		return new INTERNALDATE(this);
	    break;
	case 'B': case 'b':
	    if (match(BODYSTRUCTURE.name))
		return new BODYSTRUCTURE(this);
	    else if (match(BODY.name)) {
		if (buffer[index] == '[')
		    return new BODY(this);
		else
		    return new BODYSTRUCTURE(this);
	    }
	    break;
	case 'R': case 'r':
	    if (match(RFC822SIZE.name))
		return new RFC822SIZE(this);
	    else if (match(RFC822DATA.name)) {
		if (match(HEADER))
		    ;	// skip ".HEADER"
		else if (match(TEXT))
		    ;	// skip ".TEXT"
		return new RFC822DATA(this);
	    }
	    break;
	case 'U': case 'u':
	    if (match(UID.name))
		return new UID(this);
	    break;
	default: 
	    break;
	}
	return null;
    }

    /**
     * If this item is a known extension item, parse it.
     */
    private boolean parseExtensionItem() throws ParsingException {
	if (fitems == null)
	    return false;
	for (int i = 0; i < fitems.length; i++) {
	    if (match(fitems[i].getName())) {
		getExtensionItems().put(fitems[i].getName(),
				    fitems[i].parseItem(this));
		return true;
	    }
	}
	return false;
    }

    /**
     * Does the current buffer match the given item name?
     * itemName is the name of the IMAP item to compare against.
     * NOTE that itemName *must* be all uppercase.
     * If the match is successful, the buffer pointer (index)
     * is incremented past the matched item.
     */
    private boolean match(char[] itemName) {
	int len = itemName.length;
	for (int i = 0, j = index; i < len;)
	    // IMAP tokens are case-insensitive. We store itemNames in
	    // uppercase, so convert operand to uppercase before comparing.
	    if (Character.toUpperCase((char)buffer[j++]) != itemName[i++])
		return false;
	index += len;
	return true;
    }

    /**
     * Does the current buffer match the given item name?
     * itemName is the name of the IMAP item to compare against.
     * NOTE that itemName *must* be all uppercase.
     * If the match is successful, the buffer pointer (index)
     * is incremented past the matched item.
     */
    private boolean match(String itemName) {
	int len = itemName.length();
	for (int i = 0, j = index; i < len;)
	    // IMAP tokens are case-insensitive. We store itemNames in
	    // uppercase, so convert operand to uppercase before comparing.
	    if (Character.toUpperCase((char)buffer[j++]) !=
		    itemName.charAt(i++))
		return false;
	index += len;
	return true;
    }
}
