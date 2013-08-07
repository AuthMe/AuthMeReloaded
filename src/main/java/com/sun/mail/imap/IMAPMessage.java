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

package com.sun.mail.imap;

import java.util.Date;
import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import com.sun.mail.util.*;
import com.sun.mail.iap.*;
import com.sun.mail.imap.protocol.*;

/**
 * This class implements an IMAPMessage object. <p>
 *
 * An IMAPMessage object starts out as a light-weight object. It gets
 * filled-in incrementally when a request is made for some item. Or
 * when a prefetch is done using the FetchProfile. <p>
 *
 * An IMAPMessage has a messageNumber and a sequenceNumber. The 
 * messageNumber is its index into its containing folder's messageCache.
 * The sequenceNumber is its IMAP sequence-number.
 *
 * @author  John Mani
 * @author  Bill Shannon
 */
/*
 * The lock hierarchy is that the lock on the IMAPMessage object, if
 * it's acquired at all, must be acquired before the message cache lock.
 * The IMAPMessage lock protects the message flags, sort of.
 *
 * XXX - I'm not convinced that all fields of IMAPMessage are properly
 * protected by locks.
 */

public class IMAPMessage extends MimeMessage implements ReadableMime {
    protected BODYSTRUCTURE bs;		// BODYSTRUCTURE
    protected ENVELOPE envelope;	// ENVELOPE

    /**
     * A map of the extension FETCH items.  In addition to saving the
     * data in this map, an entry in this map indicates that we *have*
     * the data, and so it doesn't need to be fetched again.  The map
     * is created only when needed, to avoid significantly increasing
     * the effective size of an IMAPMessage object.
     *
     * @since JavaMail 1.4.6
     */
    protected Map items;		// Map<String,Object>

    private Date receivedDate;		// INTERNALDATE
    private int size = -1;		// RFC822.SIZE

    private boolean peek;		// use BODY.PEEK when fetching content?

    // this message's IMAP UID
    private long uid = -1;

    // this message's IMAP sectionId (null for toplevel message, 
    // 	non-null for a nested message)
    protected String sectionId;

    // processed values
    private String type;		// Content-Type (with params)
    private String subject;		// decoded (Unicode) subject
    private String description;		// decoded (Unicode) desc

    // Indicates that we've loaded *all* headers for this message
    private volatile boolean headersLoaded = false;

    /* Hashtable of names of headers we've loaded from the server.
     * Used in isHeaderLoaded() and getHeaderLoaded() to keep track
     * of those headers we've attempted to load from the server. We
     * need this table of names to avoid multiple attempts at loading
     * headers that don't exist for a particular message.
     *
     * Could this somehow be included in the InternetHeaders object ??
     */
    private Hashtable loadedHeaders = new Hashtable(1);

    // This is our Envelope
    static final String EnvelopeCmd = "ENVELOPE INTERNALDATE RFC822.SIZE";

    /**
     * Constructor.
     */
    protected IMAPMessage(IMAPFolder folder, int msgnum) {
	super(folder, msgnum);
	flags = null;
    }

    /**
     * Constructor, for use by IMAPNestedMessage.
     */
    protected IMAPMessage(Session session) {
	super(session);
    }

    /**
     * Get this message's folder's protocol connection.
     * Throws FolderClosedException, if the protocol connection
     * is not available.
     *
     * ASSERT: Must hold the messageCacheLock.
     */
    protected IMAPProtocol getProtocol()
			    throws ProtocolException, FolderClosedException {
	((IMAPFolder)folder).waitIfIdle();
	IMAPProtocol p = ((IMAPFolder)folder).protocol;
	if (p == null)
	    throw new FolderClosedException(folder);
	else
	    return p;
    }

    /*
     * Is this an IMAP4 REV1 server?
     */
    protected boolean isREV1() throws FolderClosedException {
	// access the folder's protocol object without waiting
	// for IDLE to complete
	IMAPProtocol p = ((IMAPFolder)folder).protocol;
	if (p == null)
	    throw new FolderClosedException(folder);
	else
	    return p.isREV1();
    }

    /**
     * Get the messageCacheLock, associated with this Message's
     * Folder.
     */
    protected Object getMessageCacheLock() {
	return ((IMAPFolder)folder).messageCacheLock;
    }

    /**
     * Get this message's IMAP sequence number.
     *
     * ASSERT: This method must be called only when holding the
     * 	messageCacheLock.
     */
    protected int getSequenceNumber() {
	return ((IMAPFolder)folder).messageCache.seqnumOf(getMessageNumber());
    }

    /**
     * Wrapper around the protected method Message.setMessageNumber() to 
     * make that method accessible to IMAPFolder.
     */
    protected void setMessageNumber(int msgnum) {
	super.setMessageNumber(msgnum);
    }

    protected long getUID() {
	return uid;
    }

    protected void setUID(long uid) {
	this.uid = uid;
    }

    // expose to MessageCache
    protected void setExpunged(boolean set) {
	super.setExpunged(set);
    }

    // Convenience routine
    protected void checkExpunged() throws MessageRemovedException {
	if (expunged)
	    throw new MessageRemovedException();
    }

    /**
     * Do a NOOP to force any untagged EXPUNGE responses
     * and then check if this message is expunged.
     */
    protected void forceCheckExpunged()
			throws MessageRemovedException, FolderClosedException {
	synchronized (getMessageCacheLock()) {
	    try {
		getProtocol().noop();
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		// ignore it
	    }
	}
	if (expunged)
	    throw new MessageRemovedException();
    }

    // Return the block size for FETCH requests
    // MUST be overridden by IMAPNestedMessage
    protected int getFetchBlockSize() {
	return ((IMAPStore)folder.getStore()).getFetchBlockSize();
    }

    // Return the block size for FETCH requests
    // MUST be overridden by IMAPNestedMessage
    protected boolean ignoreBodyStructureSize() {
	return ((IMAPStore)folder.getStore()).ignoreBodyStructureSize();
    }

    /**
     * Get the "From" attribute.
     */
    public Address[] getFrom() throws MessagingException {
	checkExpunged();
	loadEnvelope();
	InternetAddress[] a = envelope.from;
	/*
	 * Per RFC 2822, the From header is required, and thus the IMAP
	 * spec also requires that it be present, but we know that in
	 * practice it is often missing.  Some servers fill in the
	 * From field with the Sender field in this case, but at least
	 * Exchange 2007 does not.  Use the same fallback strategy used
	 * by MimeMessage.
	 */
	if (a == null || a.length == 0)
	    a = envelope.sender;
	return aaclone(a);
    }

    public void setFrom(Address address) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    public void addFrom(Address[] addresses) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }
    
    /**
     * Get the "Sender" attribute.
     */
    public Address getSender() throws MessagingException {
	checkExpunged();
	loadEnvelope();
	if (envelope.sender != null)
		return (envelope.sender)[0];	// there can be only one sender
	else 
		return null;
    }
	

    public void setSender(Address address) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }    

    /**
     * Get the desired Recipient type.
     */
    public Address[] getRecipients(Message.RecipientType type)
				throws MessagingException {
	checkExpunged();
	loadEnvelope();

	if (type == Message.RecipientType.TO)
	    return aaclone(envelope.to);
	else if (type == Message.RecipientType.CC)
	    return aaclone(envelope.cc);
	else if (type == Message.RecipientType.BCC)
	    return aaclone(envelope.bcc);
	else
	    return super.getRecipients(type);
    }

    public void setRecipients(Message.RecipientType type, Address[] addresses)
			throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    public void addRecipients(Message.RecipientType type, Address[] addresses)
			throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the ReplyTo addresses.
     */
    public Address[] getReplyTo() throws MessagingException {
	checkExpunged();
	loadEnvelope();
	/*
	 * The IMAP spec requires that the Reply-To field never be
	 * null, but at least Exchange 2007 fails to fill it in in
	 * some cases.  Use the same fallback strategy used by
	 * MimeMessage.
	 */
	if (envelope.replyTo == null || envelope.replyTo.length == 0)
	    return getFrom();
	return aaclone(envelope.replyTo);
    }

    public void setReplyTo(Address[] addresses) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the decoded subject.
     */
    public String getSubject() throws MessagingException {
	checkExpunged();

	if (subject != null) // already cached ?
	    return subject;

	loadEnvelope();
	if (envelope.subject == null) // no subject
	    return null;

	// Cache and return the decoded value.
	try {
	    // The server *should* unfold the value, but just in case it
	    // doesn't we unfold it here.
	    subject =
		MimeUtility.decodeText(MimeUtility.unfold(envelope.subject));
	} catch (UnsupportedEncodingException ex) {
	    subject = envelope.subject;
	}

	return subject;
    }

    public void setSubject(String subject, String charset) 
		throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the SentDate.
     */
    public Date getSentDate() throws MessagingException {
	checkExpunged();
	loadEnvelope();
	if (envelope.date == null)
	    return null;
	else
	    return new Date(envelope.date.getTime());
    }

    public void setSentDate(Date d) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the recieved date (INTERNALDATE)
     */
    public Date getReceivedDate() throws MessagingException {
	checkExpunged();
	loadEnvelope();
	if (receivedDate == null)
	    return null;
	else
	    return new Date(receivedDate.getTime());
    }

    /**
     * Get the message size. <p>
     *
     * Note that this returns RFC822.SIZE.  That is, it's the
     * size of the whole message, header and body included.
     */
    public int getSize() throws MessagingException {
	checkExpunged();
	if (size == -1)
	    loadEnvelope();	// XXX - could just fetch the size
	return size;
    }

    /**
     * Get the total number of lines. <p>
     *
     * Returns the "body_fld_lines" field from the
     * BODYSTRUCTURE. Note that this field is available
     * only for text/plain and message/rfc822 types
     */
    public int getLineCount() throws MessagingException {
	checkExpunged();
	loadBODYSTRUCTURE();
	return bs.lines;
    }

    /** 
     * Get the content language.
     */
    public String[] getContentLanguage() throws MessagingException {
    	checkExpunged();
    	loadBODYSTRUCTURE();
    	if (bs.language != null)
	    return (String[])(bs.language).clone();
    	else
	    return null;
    }
 
    public void setContentLanguage(String[] languages)
				throws MessagingException {
    	throw new IllegalWriteException("IMAPMessage is read-only");
    }
 
    /**
     * Get the In-Reply-To header.
     *
     * @since	JavaMail 1.3.3
     */
    public String getInReplyTo() throws MessagingException {
    	checkExpunged();
    	loadEnvelope();
    	return envelope.inReplyTo;
    }
 
    /**
     * Get the Content-Type.
     *
     * Generate this header from the BODYSTRUCTURE. Append parameters
     * as well.
     */
    public synchronized String getContentType() throws MessagingException {
	checkExpunged();

	// If we haven't cached the type yet ..
	if (type == null) {
	    loadBODYSTRUCTURE();
	    // generate content-type from BODYSTRUCTURE
	    ContentType ct = new ContentType(bs.type, bs.subtype, bs.cParams);
	    type = ct.toString();
	}
	return type;
    }

    /**
     * Get the Content-Disposition.
     */
    public String getDisposition() throws MessagingException {
	checkExpunged();
	loadBODYSTRUCTURE();
	return bs.disposition;
    }

    public void setDisposition(String disposition) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the Content-Transfer-Encoding.
     */
    public String getEncoding() throws MessagingException {
	checkExpunged();
	loadBODYSTRUCTURE();
	return bs.encoding;
    }

    /**
     * Get the Content-ID.
     */
    public String getContentID() throws MessagingException {
	checkExpunged();
	loadBODYSTRUCTURE();
	return bs.id;
    }

    public void setContentID(String cid) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the Content-MD5.
     */
    public String getContentMD5() throws MessagingException {
	checkExpunged();
	loadBODYSTRUCTURE();
	return bs.md5;
    }

    public void setContentMD5(String md5) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the decoded Content-Description.
     */
    public String getDescription() throws MessagingException {
	checkExpunged();

	if (description != null) // cached value ?
	    return description;
	
	loadBODYSTRUCTURE();
	if (bs.description == null)
	    return null;
	
	try {
	    description = MimeUtility.decodeText(bs.description);
	} catch (UnsupportedEncodingException ex) {
	    description = bs.description;
	}

	return description;
    }

    public void setDescription(String description, String charset) 
			throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the Message-ID.
     */
    public String getMessageID() throws MessagingException {
	checkExpunged();
	loadEnvelope();
	return envelope.messageId;
    }

    /**
     * Get the "filename" Disposition parameter. (Only available in
     * IMAP4rev1). If thats not available, get the "name" ContentType
     * parameter.
     */
    public String getFileName() throws MessagingException {
	checkExpunged();

	String filename = null;
	loadBODYSTRUCTURE();

	if (bs.dParams != null)
	    filename = bs.dParams.get("filename");
	if (filename == null && bs.cParams != null)
	    filename = bs.cParams.get("name");
	return filename;
    }

    public void setFileName(String filename) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get all the bytes for this message. Overrides getContentStream()
     * in MimeMessage. This method is ultimately used by the DataHandler
     * to obtain the input stream for this message.
     *
     * @see javax.mail.internet.MimeMessage#getContentStream
     */
    protected InputStream getContentStream() throws MessagingException {
	InputStream is = null;
	boolean pk = getPeek();	// get before acquiring message cache lock

        // Acquire MessageCacheLock, to freeze seqnum.
        synchronized(getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();

		// This message could be expunged when we were waiting
		// to acquire the lock ...
		checkExpunged();

		if (p.isREV1() && (getFetchBlockSize() != -1)) // IMAP4rev1
		    return new IMAPInputStream(this, toSection("TEXT"),
				    bs != null && !ignoreBodyStructureSize() ?
					bs.size : -1, pk);

		if (p.isREV1()) {
		    BODY b;
		    if (pk)
			b = p.peekBody(getSequenceNumber(), toSection("TEXT"));
		    else
			b = p.fetchBody(getSequenceNumber(), toSection("TEXT"));
		    if (b != null)
			is = b.getByteArrayInputStream();
		} else {
		    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), "TEXT");
		    if (rd != null)
			is = rd.getByteArrayInputStream();
		}
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }
	}

	if (is == null)
	    throw new MessagingException("No content");
	else
	    return is;
    }

    /**
     * Get the DataHandler object for this message.
     */
    public synchronized DataHandler getDataHandler()
		throws MessagingException {
	checkExpunged();

	if (dh == null) {
	    loadBODYSTRUCTURE();
	    if (type == null) { // type not yet computed
		// generate content-type from BODYSTRUCTURE
		ContentType ct = new ContentType(bs.type, bs.subtype,
						 bs.cParams);
		type = ct.toString();
	    }

	    /* Special-case Multipart and Nested content. All other
	     * cases are handled by the superclass.
	     */
	    if (bs.isMulti())
		dh = new DataHandler(
			new IMAPMultipartDataSource(this, bs.bodies, 
						    sectionId, this)
		     );
	    else if (bs.isNested() && isREV1() && bs.envelope != null)
		/* Nested messages are handled specially only for
		 * IMAP4rev1. IMAP4 doesn't provide enough support to 
		 * FETCH the components of nested messages
		 */
		dh = new DataHandler(
			    new IMAPNestedMessage(this, 
				bs.bodies[0], 
				bs.envelope,
				sectionId == null ? "1" : sectionId + ".1"),
			    type
		     );
	}

	return super.getDataHandler();
    }

    public void setDataHandler(DataHandler content) 
			throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Return the MIME format stream corresponding to this message.
     *
     * @return	the MIME format stream
     * @since	JavaMail 1.4.5
     */
    public InputStream getMimeStream() throws MessagingException {
	InputStream is = null;
	boolean pk = getPeek();	// get before acquiring message cache lock

        // Acquire MessageCacheLock, to freeze seqnum.
        synchronized(getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();

		checkExpunged(); // insure this message is not expunged

		if (p.isREV1() && (getFetchBlockSize() != -1)) // IMAP4rev1
		    return new IMAPInputStream(this, sectionId, -1, pk);

		if (p.isREV1()) {
		    BODY b;
		    if (pk)
			b = p.peekBody(getSequenceNumber(), sectionId);
		    else
			b = p.fetchBody(getSequenceNumber(), sectionId);
		    if (b != null)
			is = b.getByteArrayInputStream();
		} else {
		    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), null);
		    if (rd != null)
			is = rd.getByteArrayInputStream();
		}
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }
	}

	if (is == null) {
	    forceCheckExpunged();	// may throw MessageRemovedException
	    // nope, the server doesn't think it's expunged,
	    // something else is wrong
	    throw new MessagingException("No content");
	}
	return is;
    }

    /**
     * Write out the bytes into the given OutputStream.
     */
    public void writeTo(OutputStream os)
				throws IOException, MessagingException {
	InputStream is = getMimeStream();
	try {
	    // write out the bytes
	    byte[] bytes = new byte[16*1024];
	    int count;
	    while ((count = is.read(bytes)) != -1)
		os.write(bytes, 0, count);
	} finally {
	    is.close();
	}
    }

    /**
     * Get the named header.
     */
    public String[] getHeader(String name) throws MessagingException {
	checkExpunged();

	if (isHeaderLoaded(name)) // already loaded ?
	    return headers.getHeader(name);

	// Load this particular header
	InputStream is = null;

        // Acquire MessageCacheLock, to freeze seqnum.
        synchronized(getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();

		// This message could be expunged when we were waiting
		// to acquire the lock ...
		checkExpunged();

		if (p.isREV1()) {
		    BODY b = p.peekBody(getSequenceNumber(), 
				toSection("HEADER.FIELDS (" + name + ")")
			     );
		    if (b != null)
			is = b.getByteArrayInputStream();
		} else {
		    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), 
					"HEADER.LINES (" + name + ")");
		    if (rd != null)
			is = rd.getByteArrayInputStream();
		}
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }
	}

	// if we get this far without "is" being set, something has gone
	// wrong; prevent a later NullPointerException and return null here
	if (is == null)
	    return null;

	if (headers == null)
	    headers = new InternetHeaders();
	headers.load(is); // load this header into the Headers object.
	setHeaderLoaded(name); // Mark this header as loaded

	return headers.getHeader(name);
    }

    /**
     * Get the named header.
     */
    public String getHeader(String name, String delimiter)
			throws MessagingException {
	checkExpunged();

	// force the header to be loaded by invoking getHeader(name)
	if (getHeader(name) == null)
	    return null;
	return headers.getHeader(name, delimiter);
    }

    public void setHeader(String name, String value)
			throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    public void addHeader(String name, String value)
			throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }
	    
    public void removeHeader(String name)
			throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get all headers.
     */
    public Enumeration getAllHeaders() throws MessagingException {
	checkExpunged();
	loadHeaders();
	return super.getAllHeaders();
    }

    /**
     * Get matching headers.
     */
    public Enumeration getMatchingHeaders(String[] names)
			throws MessagingException {
	checkExpunged();
	loadHeaders();
	return super.getMatchingHeaders(names);
    }

    /**
     * Get non-matching headers.
     */
    public Enumeration getNonMatchingHeaders(String[] names)
			throws MessagingException {
	checkExpunged();
	loadHeaders();
	return super.getNonMatchingHeaders(names);
    }

    public void addHeaderLine(String line) throws MessagingException {
	throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get all header-lines.
     */
    public Enumeration getAllHeaderLines() throws MessagingException {
	checkExpunged();
	loadHeaders();
	return super.getAllHeaderLines();
    }

    /**
     * Get all matching header-lines.
     */
    public Enumeration getMatchingHeaderLines(String[] names)
			throws MessagingException {
	checkExpunged();
	loadHeaders();
	return super.getMatchingHeaderLines(names);
    }

    /**
     * Get all non-matching headerlines.
     */
    public Enumeration getNonMatchingHeaderLines(String[] names)
			throws MessagingException {
	checkExpunged();
	loadHeaders();
	return super.getNonMatchingHeaderLines(names);
    }

    /**
     * Get the Flags for this message.
     */
    public synchronized Flags getFlags() throws MessagingException {
	checkExpunged();
	loadFlags();
	return super.getFlags();
    }

    /**
     * Test if the given Flags are set in this message.
     */
    public synchronized boolean isSet(Flags.Flag flag)
				throws MessagingException {
	checkExpunged();
	loadFlags();
	return super.isSet(flag);
    }

    /**
     * Set/Unset the given flags in this message.
     */
    public synchronized void setFlags(Flags flag, boolean set)
			throws MessagingException {
        // Acquire MessageCacheLock, to freeze seqnum.
        synchronized(getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();
		checkExpunged(); // Insure that this message is not expunged
		p.storeFlags(getSequenceNumber(), flag, set);
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		throw new MessagingException(pex.getMessage(), pex);
	    }
	}
    }

    /**
     * Set whether or not to use the PEEK variant of FETCH when
     * fetching message content.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized void setPeek(boolean peek) {
	this.peek = peek;
    }

    /**
     * Get whether or not to use the PEEK variant of FETCH when
     * fetching message content.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized boolean getPeek() {
	return peek;
    }

    /**
     * Invalidate cached header and envelope information for this
     * message.  Subsequent accesses of this information will
     * cause it to be fetched from the server.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized void invalidateHeaders() {
	headersLoaded = false;
	loadedHeaders.clear();
	headers = null;
	envelope = null;
	bs = null;
	receivedDate = null;
	size = -1;
	type = null;
	subject = null;
	description = null;
	flags = null;
    }

    /**
     * This class implements the test to be done on each
     * message in the folder. The test is to check whether the
     * message has already cached all the items requested in the
     * FetchProfile. If any item is missing, the test succeeds and
     * breaks out.
     */
    public static class FetchProfileCondition implements Utility.Condition {
	private boolean needEnvelope = false;
	private boolean needFlags = false;
	private boolean needBodyStructure = false;
	private boolean needUID = false;
	private boolean needHeaders = false;
	private boolean needSize = false;
	private String[] hdrs = null;
	private Set need = new HashSet();	// Set<FetchItem>

	/**
	 * Create a FetchProfileCondition to determine if we need to fetch
	 * any of the information specified in the FetchProfile.
	 */
	public FetchProfileCondition(FetchProfile fp, FetchItem[] fitems) {
	    if (fp.contains(FetchProfile.Item.ENVELOPE))
		needEnvelope = true;
	    if (fp.contains(FetchProfile.Item.FLAGS))
		needFlags = true;
	    if (fp.contains(FetchProfile.Item.CONTENT_INFO))
		needBodyStructure = true;
	    if (fp.contains(FetchProfile.Item.SIZE))
		needSize = true;
	    if (fp.contains(UIDFolder.FetchProfileItem.UID))
		needUID = true;
	    if (fp.contains(IMAPFolder.FetchProfileItem.HEADERS))
		needHeaders = true;
	    if (fp.contains(IMAPFolder.FetchProfileItem.SIZE))
		needSize = true;
	    hdrs = fp.getHeaderNames();
	    for (int i = 0; i < fitems.length; i++) {
		if (fp.contains(fitems[i].getFetchProfileItem()))
		    need.add(fitems[i]);
	    }
	}

	/**
	 * Return true if we NEED to fetch the requested information
	 * for the specified message.
	 */
	public boolean test(IMAPMessage m) {
	    if (needEnvelope && m._getEnvelope() == null)
		return true; // no envelope
	    if (needFlags && m._getFlags() == null)
		return true; // no flags
	    if (needBodyStructure && m._getBodyStructure() == null)
		return true; // no BODYSTRUCTURE
	    if (needUID && m.getUID() == -1)	// no UID
		return true;
	    if (needHeaders && !m.areHeadersLoaded()) // no headers
		return true;
	    if (needSize && m.size == -1)		// no size
		return true;

	    // Is the desired header present ?
	    for (int i = 0; i < hdrs.length; i++) {
		if (!m.isHeaderLoaded(hdrs[i]))
		    return true; // Nope, return
	    }
	    Iterator it = need.iterator();
	    while (it.hasNext()) {
		FetchItem fitem = (FetchItem)it.next();
		if (m.items == null || m.items.get(fitem.getName()) == null)
		    return true;
	    }

	    return false;
	}
    }

    /**
     * Apply the data in the FETCH item to this message.
     *
     * ASSERT: Must hold the messageCacheLock.
     *
     * @since JavaMail 1.4.6
     */
    protected boolean handleFetchItem(Item item,
				String[] hdrs, boolean allHeaders)
				throws MessagingException {
	// Check for the FLAGS item
	if (item instanceof Flags)
	    flags = (Flags)item;
	// Check for ENVELOPE items
	else if (item instanceof ENVELOPE)
	    envelope = (ENVELOPE)item;
	else if (item instanceof INTERNALDATE)
	    receivedDate = ((INTERNALDATE)item).getDate();
	else if (item instanceof RFC822SIZE)
	    size = ((RFC822SIZE)item).size;

	// Check for the BODYSTRUCTURE item
	else if (item instanceof BODYSTRUCTURE)
	    bs = (BODYSTRUCTURE)item;
	// Check for the UID item
	else if (item instanceof UID) {
	    UID u = (UID)item;
	    uid = u.uid; // set uid
	    // add entry into uid table
	    if (((IMAPFolder)folder).uidTable == null)
		((IMAPFolder)folder).uidTable = new Hashtable();
	    ((IMAPFolder)folder).uidTable.put(new Long(u.uid), this);
	}

	// Check for header items
	else if (item instanceof RFC822DATA ||
		 item instanceof BODY) {
	    InputStream headerStream;
	    if (item instanceof RFC822DATA) // IMAP4
		headerStream = 
		    ((RFC822DATA)item).getByteArrayInputStream();
	    else	// IMAP4rev1
		headerStream = 
		    ((BODY)item).getByteArrayInputStream();
	    
	    // Load the obtained headers.
	    InternetHeaders h = new InternetHeaders();
	    // Some IMAP servers (e.g., gmx.net) return NIL 
	    // instead of a string just containing a CR/LF
	    // when the header list is empty.
	    if (headerStream != null)
		h.load(headerStream);
	    if (headers == null || allHeaders)
		headers = h;
	    else {
		/*
		 * This is really painful.  A second fetch
		 * of the same headers (which might occur because
		 * a new header was added to the set requested)
		 * will return headers we already know about.
		 * In this case, only load the headers we haven't
		 * seen before to avoid adding duplicates of
		 * headers we already have.
		 *
		 * XXX - There's a race condition here if another
		 * thread is reading headers in the same message
		 * object, because InternetHeaders is not thread
		 * safe.
		 */
		Enumeration e = h.getAllHeaders();
		while (e.hasMoreElements()) {
		    Header he = (Header)e.nextElement();
		    if (!isHeaderLoaded(he.getName()))
			headers.addHeader(
				    he.getName(), he.getValue());
		}
	    }

	    // if we asked for all headers, assume we got them
	    if (allHeaders)
		setHeadersLoaded(true);
	    else {
		// Mark all headers we asked for as 'loaded'
		for (int k = 0; k < hdrs.length; k++)
		    setHeaderLoaded(hdrs[k]);
	    }
	} else
	    return false;	// not handled
	return true;		// something above handled it
    }

    /**
     * Apply the data in the extension FETCH items to this message.
     * This method adds all the items to the items map.
     * Subclasses may override this method to call super and then
     * also copy the data to a more convenient form.
     *
     * ASSERT: Must hold the messageCacheLock.
     *
     * @since JavaMail 1.4.6
     */
    protected void handleExtensionFetchItems(Map extensionItems)
				throws MessagingException {
	if (items == null)
	    items = extensionItems;
	else
	    items.putAll(extensionItems);
    }

    /**
     * Fetch an individual item for the current message.
     * Note that handleExtensionFetchItems will have been called
     * to store this item in the message before this method
     * returns.
     *
     * @since JavaMail 1.4.6
     */
    protected Object fetchItem(FetchItem fitem)
				throws MessagingException {

	// Acquire MessageCacheLock, to freeze seqnum.
	synchronized(getMessageCacheLock()) {
	    Object robj = null;

	    try {
		IMAPProtocol p = getProtocol();

		checkExpunged(); // Insure that this message is not expunged

		int seqnum = getSequenceNumber();
		Response[] r = p.fetch(seqnum, fitem.getName());

		for (int i = 0; i < r.length; i++) {
		    // If this response is NOT a FetchResponse or if it does
		    // not match our seqnum, skip.
		    if (r[i] == null ||
			!(r[i] instanceof FetchResponse) ||
			((FetchResponse)r[i]).getNumber() != seqnum)
			continue;

		    FetchResponse f = (FetchResponse)r[i];
		    Object o = f.getExtensionItems().get(fitem.getName());
		    if (o != null)
			robj = o;
		}

		// ((IMAPFolder)folder).handleResponses(r);
		p.notifyResponseHandlers(r);
		p.handleResult(r[r.length - 1]);
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }
	    return robj;

	} // Release MessageCacheLock
    }

    /**
     * Return the data associated with the FetchItem.
     * If the data hasn't been fetched, call the fetchItem
     * method to fetch it.  Returns null if there is no
     * data for the FetchItem.
     *
     * @since JavaMail 1.4.6
     */
    public synchronized Object getItem(FetchItem fitem)
				throws MessagingException {
	Object item = items == null ? null : items.get(fitem.getName());
	if (item == null)
	    item = fetchItem(fitem);
	return item;
    }

    /*
     * Load the Envelope for this message.
     */
    private synchronized void loadEnvelope() throws MessagingException {
	if (envelope != null) // already loaded
	    return;

	Response[] r = null;

	// Acquire MessageCacheLock, to freeze seqnum.
	synchronized(getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();

		checkExpunged(); // Insure that this message is not expunged

		int seqnum = getSequenceNumber();
		r = p.fetch(seqnum, EnvelopeCmd);

		for (int i = 0; i < r.length; i++) {
		    // If this response is NOT a FetchResponse or if it does
		    // not match our seqnum, skip.
		    if (r[i] == null ||
			!(r[i] instanceof FetchResponse) ||
			((FetchResponse)r[i]).getNumber() != seqnum)
			continue;

		    FetchResponse f = (FetchResponse)r[i];
		    
		    // Look for the Envelope items.
		    int count = f.getItemCount();
		    for (int j = 0; j < count; j++) {
			Item item = f.getItem(j);
			
			if (item instanceof ENVELOPE)
			    envelope = (ENVELOPE)item;
			else if (item instanceof INTERNALDATE)
			    receivedDate = ((INTERNALDATE)item).getDate();
			else if (item instanceof RFC822SIZE)
			    size = ((RFC822SIZE)item).size;
		    }
		}

		// ((IMAPFolder)folder).handleResponses(r);
		p.notifyResponseHandlers(r);
		p.handleResult(r[r.length - 1]);
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }

	} // Release MessageCacheLock

	if (envelope == null)
	    throw new MessagingException("Failed to load IMAP envelope");
    }

    /*
     * Load the BODYSTRUCTURE
     */
    private synchronized void loadBODYSTRUCTURE() 
		throws MessagingException {
	if (bs != null) // already loaded
	    return;

	// Acquire MessageCacheLock, to freeze seqnum.
	synchronized(getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();

		// This message could be expunged when we were waiting 
		// to acquire the lock ...
		checkExpunged();

		bs = p.fetchBodyStructure(getSequenceNumber());
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }
	    if (bs == null) {
		// if the FETCH is successful, we should always get a
		// BODYSTRUCTURE, but some servers fail to return it
		// if the message has been expunged
		forceCheckExpunged();
		throw new MessagingException("Unable to load BODYSTRUCTURE");
	    }
	}
    }

    /*
     * Load all headers.
     */
    private synchronized void loadHeaders() throws MessagingException {
	if (headersLoaded)
	    return;

	InputStream is = null;

	// Acquire MessageCacheLock, to freeze seqnum.
	synchronized (getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();

		// This message could be expunged when we were waiting 
		// to acquire the lock ...
		checkExpunged();

		if (p.isREV1()) {
		    BODY b = p.peekBody(getSequenceNumber(), 
					 toSection("HEADER"));
		    if (b != null)
			is = b.getByteArrayInputStream();
		} else {
		    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), 
						  "HEADER");
		    if (rd != null)
			is = rd.getByteArrayInputStream();
		}
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }
	} // Release MessageCacheLock

	if (is == null)
	    throw new MessagingException("Cannot load header");
	headers = new InternetHeaders(is);
	headersLoaded = true;
    }

    /*
     * Load this message's Flags
     */
    private synchronized void loadFlags() throws MessagingException {
	if (flags != null)
	    return;
	
	// Acquire MessageCacheLock, to freeze seqnum.
	synchronized(getMessageCacheLock()) {
	    try {
		IMAPProtocol p = getProtocol();

		// This message could be expunged when we were waiting 
		// to acquire the lock ...
		checkExpunged();

		flags = p.fetchFlags(getSequenceNumber());
		// make sure flags is always set, even if server is broken
		if (flags == null)
		    flags = new Flags();
	    } catch (ConnectionException cex) {
		throw new FolderClosedException(folder, cex.getMessage());
	    } catch (ProtocolException pex) {
		forceCheckExpunged();
		throw new MessagingException(pex.getMessage(), pex);
	    }
	} // Release MessageCacheLock
    }

    /*
     * Are all headers loaded?
     */
    private boolean areHeadersLoaded() {
	return headersLoaded;
    }

    /*
     * Set whether all headers are loaded.
     */
    private void setHeadersLoaded(boolean loaded) {
	headersLoaded = loaded;
    }

    /* 
     * Check if the given header was ever loaded from the server
     */
    private boolean isHeaderLoaded(String name) {
	if (headersLoaded) // All headers for this message have been loaded
	    return true;
	
	return loadedHeaders.containsKey(name.toUpperCase(Locale.ENGLISH));
    }

    /*
     * Mark that the given headers have been loaded from the server.
     */
    private void setHeaderLoaded(String name) {
	loadedHeaders.put(name.toUpperCase(Locale.ENGLISH), name);
    }

    /*
     * Convert the given FETCH item identifier to the approriate 
     * section-string for this message.
     */
    private String toSection(String what) {
	if (sectionId == null)
	    return what;
	else
	    return sectionId + "." + what;
    }

    /*
     * Clone an array of InternetAddresses.
     */
    private InternetAddress[] aaclone(InternetAddress[] aa) {
	if (aa == null)
	    return null;
	else
	    return (InternetAddress[])aa.clone();
    }

    private Flags _getFlags() {
	return flags;
    }

    private ENVELOPE _getEnvelope() {
	return envelope;
    }

    private BODYSTRUCTURE _getBodyStructure() {
	return bs;
    }

    /***********************************************************
     * accessor routines to make available certain private/protected
     * fields to other classes in this package.
     ***********************************************************/

    /*
     * Called by IMAPFolder.
     * Must not be synchronized.
     */
    void _setFlags(Flags flags) {
	this.flags = flags;
    }

    /*
     * Called by IMAPNestedMessage.
     */
    Session _getSession() {
	return session;
    }
}
