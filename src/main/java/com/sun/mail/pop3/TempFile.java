/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.mail.pop3;

import java.util.*;
import java.net.*;
import java.io.*;
import java.security.*;

import com.sun.mail.util.PropUtil;
import javax.mail.util.SharedFileInputStream;

/**
 * A temporary file used to cache POP3 messages.
 */
class TempFile {

    private File file;	// the temp file name
    private WritableSharedFile sf;

    /**
     * Create a temp file in the specified directory (if not null).
     * The file will be deleted when the JVM exits.
     */
    public TempFile(File dir) throws IOException {
	file = File.createTempFile("pop3.", ".mbox", dir);
	// XXX - need JDK 6 to set permissions on the file to owner-only
	file.deleteOnExit();
	sf = new WritableSharedFile(file);
    }

    /**
     * Return a stream for appending to the temp file.
     */
    public AppendStream getAppendStream() throws IOException {
	return sf.getAppendStream();
    }

    /**
     * Close and remove this temp file.
     */
    public void close() {
	try {
	    sf.close();
	} catch (IOException ex) {
	    // ignore it
	}
	file.delete();
    }

    protected void finalize() throws Throwable {
	super.finalize();
	close();
    }
}

/**
 * A subclass of SharedFileInputStream that also allows writing.
 */
class WritableSharedFile extends SharedFileInputStream {
    private RandomAccessFile raf;
    private AppendStream af;

    public WritableSharedFile(File file) throws IOException {
	super(file);
	try {
	    raf = new RandomAccessFile(file, "rw");
	} catch (IOException ex) {
	    // if anything goes wrong opening the writable file,
	    // close the readable file too
	    super.close();
	}
    }

    /**
     * Return the writable version of this file.
     */
    public RandomAccessFile getWritableFile() {
	return raf;
    }

    /**
     * Close the readable and writable files.
     */
    public void close() throws IOException {
	try {
	    super.close();
	} finally {
	    raf.close();
	}
    }

    /**
     * Update the size of the readable file after writing
     * to the file.  Updates the length to be the current
     * size of the file.
     */
    synchronized long updateLength() throws IOException {
	datalen = in.length();
	af = null;
	return datalen;
    }

    /**
     * Return a new AppendStream, but only if one isn't in active use.
     */
    public synchronized AppendStream getAppendStream() throws IOException {
	if (af != null)
	    throw new IOException(
		"POP3 file cache only supports single threaded access");
	af = new AppendStream(this);
	return af;
    }
}

/**
 * A stream for writing to the temp file, and when done
 * can return a stream for reading the data just written.
 * NOTE: We assume that only one thread is writing to the
 * file at a time.
 */
class AppendStream extends OutputStream {
    private final WritableSharedFile tf;
    private RandomAccessFile raf;
    private final long start;
    private long end;

    public AppendStream(WritableSharedFile tf) throws IOException {
	this.tf = tf;
	raf = tf.getWritableFile();
	start = raf.length();
	raf.seek(start);
    }

    public void write(int b) throws IOException {
	raf.write(b);
    }

    public void write(byte[] b) throws IOException {
	raf.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
	raf.write(b, off, len);
    }

    public synchronized void close() throws IOException {
	end = tf.updateLength();
	raf = null;	// no more writing allowed
    }

    public synchronized InputStream getInputStream() throws IOException {
	return tf.newStream(start, end);
    }
}
