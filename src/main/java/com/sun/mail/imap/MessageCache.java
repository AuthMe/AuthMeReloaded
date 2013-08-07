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

package com.sun.mail.imap;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;

import javax.mail.*;
import com.sun.mail.util.PropUtil;
import com.sun.mail.util.MailLogger;

/**
 * A cache of IMAPMessage objects along with the
 * mapping from message number to IMAP sequence number.
 *
 * All operations on this object are protected by the messageCacheLock
 * in IMAPFolder.
 */
public class MessageCache {
    /*
     * The array of IMAPMessage objects.  Elements of the array might
     * be null if no one has asked for the message.  The array expands
     * as needed and might be larger than the number of messages in the
     * folder.  The "size" field indicates the number of entries that
     * are valid.
     */
    private IMAPMessage[] messages;

    /*
     * A parallel array of sequence numbers for each message.  If the
     * array pointer is null, the sequence number of a message is just
     * its message number.  This is the common case, until a message is
     * expunged.
     */
    private int[] seqnums;

    /*
     * The amount of the messages (and seqnum) array that is valid.
     * Might be less than the actual size of the array.
     */
    private int size;

    /**
     * The folder these messages belong to.
     */
    private IMAPFolder folder;

    // debugging logger
    private MailLogger logger;

    /**
     * Grow the array by at least this much, to avoid constantly
     * reallocating the array.
     */
    private static final int SLOP = 64;

    /**
     * Construct a new message cache of the indicated size.
     */
    MessageCache(IMAPFolder folder, IMAPStore store, int size) {
	this.folder = folder;
	logger = folder.logger.getSubLogger("messagecache", "DEBUG IMAP MC",
						store.getMessageCacheDebug());
	if (logger.isLoggable(Level.CONFIG))
	    logger.config("create cache of size " + size);
	ensureCapacity(size, 1);
    }

    /**
     * Constructor for debugging and testing.
     */
    MessageCache(int size, boolean debug) {
	this.folder = null;
	logger = new MailLogger(
		    this.getClass(), "messagecache",
		    "DEBUG IMAP MC", debug, System.out);
	if (logger.isLoggable(Level.CONFIG))
	    logger.config("create DEBUG cache of size " + size);
	ensureCapacity(size, 1);
    }

    /**
     * Size of cache.
     */
    public int size() {
	return size;
    }

    /**
     * Get the message object for the indicated message number.
     * If the message object hasn't been created, create it.
     */
    public IMAPMessage getMessage(int msgnum) {
	// check range
	if (msgnum < 1 || msgnum > size)
	    throw new ArrayIndexOutOfBoundsException(
		"message number (" + msgnum + ") out of bounds (" + size + ")");
	IMAPMessage msg = messages[msgnum-1];
	if (msg == null) {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("create message number " + msgnum);
	    msg = folder.newIMAPMessage(msgnum);
	    messages[msgnum-1] = msg;
	    // mark message expunged if no seqnum
	    if (seqnumOf(msgnum) <= 0) {
		logger.fine("it's expunged!");
		msg.setExpunged(true);
	    }
	}
	return msg;
    }

    /**
     * Get the message object for the indicated sequence number.
     * If the message object hasn't been created, create it.
     * Return null if there's no message with that sequence number.
     */
    public IMAPMessage getMessageBySeqnum(int seqnum) {
	int msgnum = msgnumOf(seqnum);
	if (msgnum < 0) {		// XXX - < 1 ?
	    if (logger.isLoggable(Level.FINE))
		logger.fine("no message seqnum " + seqnum);
	    return null;
	} else
	    return getMessage(msgnum);
    }

    /**
     * Expunge the message with the given sequence number.
     */
    public void expungeMessage(int seqnum) {
	int msgnum = msgnumOf(seqnum);
	if (msgnum < 0) {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("expunge no seqnum " + seqnum);
	    return;		// XXX - should never happen
	}
	IMAPMessage msg = messages[msgnum-1];
	if (msg != null) {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("expunge existing " + msgnum);
	    msg.setExpunged(true);
	}
	if (seqnums == null) {		// time to fill it in
	    logger.fine("create seqnums array");
	    seqnums = new int[messages.length];
	    for (int i = 1; i < msgnum; i++)
		seqnums[i-1] = i;
	    seqnums[msgnum - 1] = 0;
	    for (int i = msgnum + 1; i <= seqnums.length; i++)
		seqnums[i-1] = i - 1;
	} else {
	    seqnums[msgnum - 1] = 0;
	    for (int i = msgnum + 1; i <= seqnums.length; i++) {
		assert seqnums[i-1] != 1;
		if (seqnums[i-1] > 0)
		    seqnums[i-1]--;
	    }
	}
    }

    /**
     * Remove all the expunged messages from the array,
     * returning a list of removed message objects.
     */
    public IMAPMessage[] removeExpungedMessages() {
	logger.fine("remove expunged messages");
	List mlist = new ArrayList();	// list of expunged messages

	/*
	 * Walk through the array compressing it by copying
	 * higher numbered messages further down in the array,
	 * effectively removing expunged messages from the array.
	 * oldnum is the index we use to walk through the array.
	 * newnum is the index where we copy the next valid message.
	 * oldnum == newnum until we encounter an expunged message.
	 */
	int oldnum = 1;
	int newnum = 1;
	while (oldnum <= size) {
	    // is message expunged?
	    if (seqnumOf(oldnum) <= 0) {
		IMAPMessage m = getMessage(oldnum);
		mlist.add(m);
	    } else {
		// keep this message
		if (newnum != oldnum) {
		    // move message down in the array (compact array)
		    messages[newnum-1] = messages[oldnum-1];
		    if (messages[newnum-1] != null)
			messages[newnum-1].setMessageNumber(newnum);
		}
		newnum++;
	    }
	    oldnum++;
	}
	seqnums = null;
	shrink(newnum, oldnum);

	IMAPMessage[] rmsgs = new IMAPMessage[mlist.size()];
	if (logger.isLoggable(Level.FINE))
	    logger.fine("return " + rmsgs.length);
	mlist.toArray(rmsgs);
	return rmsgs;
    }

    /**
     * Remove expunged messages in msgs from the array,
     * returning a list of removed message objects.
     * All messages in msgs must be IMAPMessage objects
     * from this folder.
     */
    public IMAPMessage[] removeExpungedMessages(Message[] msgs) {
	logger.fine("remove expunged messages");
	List mlist = new ArrayList();	// list of expunged messages

	/*
	 * Copy the message numbers of the expunged messages into
	 * a separate array and sort the array to make it easier to
	 * process later.
	 */
	int[] mnum = new int[msgs.length];
	for (int i = 0; i < msgs.length; i++)
	    mnum[i] = msgs[i].getMessageNumber();
	Arrays.sort(mnum);

	/*
	 * Walk through the array compressing it by copying
	 * higher numbered messages further down in the array,
	 * effectively removing expunged messages from the array.
	 * oldnum is the index we use to walk through the array.
	 * newnum is the index where we copy the next valid message.
	 * oldnum == newnum until we encounter an expunged message.
	 *
	 * Even though we know the message number of the first possibly
	 * expunged message, we still start scanning at message number 1
	 * so that we can check whether there's any message whose
	 * sequence number is different than its message number.  If there
	 * is, we can't throw away the seqnums array when we're done.
	 */
	int oldnum = 1;
	int newnum = 1;
	int mnumi = 0;		// index into mnum
	boolean keepSeqnums = false;
	while (oldnum <= size) {
	    /*
	     * Are there still expunged messsages in msgs to consider,
	     * and is the message we're considering the next one in the
	     * list, and is it expunged?
	     */
	    if (mnumi < mnum.length &&
		    oldnum == mnum[mnumi] &&
		    seqnumOf(oldnum) <= 0) {
		IMAPMessage m = getMessage(oldnum);
		mlist.add(m);
		/*
		 * Just in case there are duplicate entries in the msgs array,
		 * we keep advancing mnumi past any duplicates, but of course
		 * stop when we get to the end of the array.
		 */
		while (mnumi < mnum.length && mnum[mnumi] <= oldnum)
		    mnumi++;	// consider next message in array
	    } else {
		// keep this message
		if (newnum != oldnum) {
		    // move message down in the array (compact array)
		    messages[newnum-1] = messages[oldnum-1];
		    if (messages[newnum-1] != null)
			messages[newnum-1].setMessageNumber(newnum);
		    if (seqnums != null)
			seqnums[newnum-1] = seqnums[oldnum-1];
		}
		if (seqnums != null && seqnums[newnum-1] != newnum)
		    keepSeqnums = true;
		newnum++;
	    }
	    oldnum++;
	}

	if (!keepSeqnums)
	    seqnums = null;
	shrink(newnum, oldnum);

	IMAPMessage[] rmsgs = new IMAPMessage[mlist.size()];
	if (logger.isLoggable(Level.FINE))
	    logger.fine("return " + rmsgs.length);
	mlist.toArray(rmsgs);
	return rmsgs;
    }

    /**
     * Shrink the messages and seqnums arrays.  newend is one past last
     * valid element.  oldend is one past the previous last valid element.
     */
    private void shrink(int newend, int oldend) {
	size = newend - 1;
	if (logger.isLoggable(Level.FINE))
	    logger.fine("size now " + size);
	if (size == 0) {	// no messages left
	    messages = null;
	    seqnums = null;
	} else if (size > SLOP && size < messages.length / 2) {
	    // if array shrinks by too much, reallocate it
	    logger.fine("reallocate array");
	    IMAPMessage[] newm = new IMAPMessage[size + SLOP];
	    System.arraycopy(messages, 0, newm, 0, size);
	    messages = newm;
	    if (seqnums != null) {
		int[] news = new int[size + SLOP];
		System.arraycopy(seqnums, 0, news, 0, size);
		seqnums = news;
	    }
	} else {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("clean " + newend + " to " + oldend);
	    // clear out unused entries in array
	    for (int msgnum = newend; msgnum < oldend; msgnum++) {
		messages[msgnum-1] = null;
		if (seqnums != null)
		    seqnums[msgnum-1] = 0;
	    }
	}
    }

    /**
     * Add count messages to the cache.
     * newSeqNum is the sequence number of the first message added.
     */
    public void addMessages(int count, int newSeqNum) {
	if (logger.isLoggable(Level.FINE))
	    logger.fine("add " + count + " messages");
	// don't have to do anything other than making sure there's space
	ensureCapacity(size + count, newSeqNum);
    }

    /*
     * Make sure the arrays are at least big enough to hold
     * "newsize" messages.
     */
    private void ensureCapacity(int newsize, int newSeqNum) {
	if (messages == null)
	    messages = new IMAPMessage[newsize + SLOP];
	else if (messages.length < newsize) {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("expand capacity to " + newsize);
	    IMAPMessage[] newm = new IMAPMessage[newsize + SLOP];
	    System.arraycopy(messages, 0, newm, 0, messages.length);
	    messages = newm;
	    if (seqnums != null) {
		int[] news = new int[newsize + SLOP];
		System.arraycopy(seqnums, 0, news, 0, seqnums.length);
		for (int i = size; i < news.length; i++)
		    news[i] = newSeqNum++;
		seqnums = news;
		if (logger.isLoggable(Level.FINE))
		    logger.fine("message " + newsize +
			" has sequence number " + seqnums[newsize-1]);
	    }
	} else if (newsize < size) {		// shrinking?
	    // this should never happen
	    if (logger.isLoggable(Level.FINE))
		logger.fine("shrink capacity to " + newsize);
	    for (int msgnum = newsize + 1; msgnum <= size; msgnum++) {
		messages[msgnum-1] = null;
		if (seqnums != null)
		    seqnums[msgnum-1] = -1;
	    }
	}
	size = newsize;
    }

    /**
     * Return the sequence number for the given message number.
     */
    public int seqnumOf(int msgnum) {
	if (seqnums == null)
	    return msgnum;
	else {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("msgnum " + msgnum + " is seqnum " +
			    seqnums[msgnum-1]);
	    return seqnums[msgnum-1];
	}
    }

    /**
     * Return the message number for the given sequence number.
     */
    private int msgnumOf(int seqnum) {
	if (seqnums == null)
	    return seqnum;
	if (seqnum < 1) {		// should never happen
	    if (logger.isLoggable(Level.FINE))
		logger.fine("bad seqnum " + seqnum);
	    return -1;
	}
	for (int msgnum = seqnum; msgnum <= size; msgnum++) {
	    if (seqnums[msgnum-1] == seqnum)
		return msgnum;
	    if (seqnums[msgnum-1] > seqnum)
		break;		// message doesn't exist
	}
	return -1;
    }
}
