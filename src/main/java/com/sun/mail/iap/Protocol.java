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

package com.sun.mail.iap;

import java.util.Vector;
import java.util.Properties;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import javax.net.ssl.SSLSocket;
import com.sun.mail.util.*;

/**
 * General protocol handling code for IMAP-like protocols. <p>
 *
 * The Protocol object is multithread safe.
 *
 * @author  John Mani
 * @author  Max Spivak
 * @author  Bill Shannon
 */

public class Protocol {
    protected String host;
    private Socket socket;
    // in case we turn on TLS, we'll need these later
    protected boolean quote;
    protected MailLogger logger;
    protected MailLogger traceLogger;
    protected Properties props;
    protected String prefix;

    private boolean connected = false;		// did constructor succeed?
    private TraceInputStream traceInput;	// the Tracer
    private volatile ResponseInputStream input;

    private TraceOutputStream traceOutput;	// the Tracer
    private volatile DataOutputStream output;

    private int tagCounter = 0;

    private String localHostName;

    /*
     * handlers is a Vector, initialized here,
     * because we depend on it always existing and depend
     * on the synchronization that Vector provides.
     */
    private final Vector handlers = new Vector(); // response handlers

    private volatile long timestamp;

    private static final byte[] CRLF = { (byte)'\r', (byte)'\n'};
 
    /**
     * Constructor. <p>
     * 
     * Opens a connection to the given host at given port.
     *
     * @param host	host to connect to
     * @param port	portnumber to connect to
     * @param props     Properties object used by this protocol
     * @param prefix 	Prefix to prepend to property keys
     * @param isSSL 	use SSL?
     * @param logger 	log messages here
     */
    public Protocol(String host, int port, 
		    Properties props, String prefix,
		    boolean isSSL, MailLogger logger)
		    throws IOException, ProtocolException {
	try {
	    this.host = host;
	    this.props = props;
	    this.prefix = prefix;
	    this.logger = logger;
	    traceLogger = logger.getSubLogger("protocol", null);

	    socket = SocketFetcher.getSocket(host, port, props, prefix, isSSL);
	    quote = PropUtil.getBooleanProperty(props,
					"mail.debug.quote", false);

	    initStreams();

	    // Read server greeting
	    processGreeting(readResponse());

	    timestamp = System.currentTimeMillis();
 
	    connected = true;	// must be last statement in constructor
	} finally {
	    /*
	     * If we get here because an exception was thrown, we need
	     * to disconnect to avoid leaving a connected socket that
	     * no one will be able to use because this object was never
	     * completely constructed.
	     */
	    if (!connected)
		disconnect();
	}
    }

    private void initStreams() throws IOException {
	traceInput = new TraceInputStream(socket.getInputStream(), traceLogger);
	traceInput.setQuote(quote);
	input = new ResponseInputStream(traceInput);

	traceOutput =
	    new TraceOutputStream(socket.getOutputStream(), traceLogger);
	traceOutput.setQuote(quote);
	output = new DataOutputStream(new BufferedOutputStream(traceOutput));
    }

    /**
     * Constructor for debugging.
     */
    public Protocol(InputStream in, PrintStream out, boolean debug)
				throws IOException {
	this.host = "localhost";
	this.quote = false;
	logger = new MailLogger(this.getClass(), "DEBUG", debug, out);
	traceLogger = logger.getSubLogger("protocol", null);

	// XXX - inlined initStreams, won't allow later startTLS
	traceInput = new TraceInputStream(in, traceLogger);
	traceInput.setQuote(quote);
	input = new ResponseInputStream(traceInput);

	traceOutput = new TraceOutputStream(out, traceLogger);
	traceOutput.setQuote(quote);
	output = new DataOutputStream(new BufferedOutputStream(traceOutput));

        timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the timestamp.
     */
 
    public long getTimestamp() {
        return timestamp;
    }
 
    /**
     * Adds a response handler.
     */
    public void addResponseHandler(ResponseHandler h) {
	handlers.addElement(h);
    }

    /**
     * Removed the specified response handler.
     */
    public void removeResponseHandler(ResponseHandler h) {
	handlers.removeElement(h);
    }

    /**
     * Notify response handlers
     */
    public void notifyResponseHandlers(Response[] responses) {
	if (handlers.size() == 0)
	    return;
	
	for (int i = 0; i < responses.length; i++) { // go thru responses
	    Response r = responses[i];

	    // skip responses that have already been handled
	    if (r == null)
		continue;

	    // Need to copy handlers list because handlers can be removed
	    // when handling a response.
	    Object[] h = handlers.toArray();

	    // dispatch 'em
	    for (int j = 0; j < h.length; j++) {
		if (h[j] != null)
		    ((ResponseHandler)h[j]).handleResponse(r);
	    }
	}
    }

    protected void processGreeting(Response r) throws ProtocolException {
	if (r.isBYE())
	    throw new ConnectionException(this, r);
    }

    /**
     * Return the Protocol's InputStream.
     */
    protected ResponseInputStream getInputStream() {
	return input;
    }

    /**
     * Return the Protocol's OutputStream
     */
    protected OutputStream getOutputStream() {
	return output;
    }

    /**
     * Returns whether this Protocol supports non-synchronizing literals
     * Default is false. Subclasses should override this if required
     */
    protected synchronized boolean supportsNonSyncLiterals() {
	return false;
    }

    public Response readResponse() 
		throws IOException, ProtocolException {
	return new Response(this);
    }

    /**
     * Return a buffer to be used to read a response.
     * The default implementation returns null, which causes
     * a new buffer to be allocated for every response.
     *
     * @since	JavaMail 1.4.1
     */
    protected ByteArray getResponseBuffer() {
	return null;
    }

    public String writeCommand(String command, Argument args) 
		throws IOException, ProtocolException {
	// assert Thread.holdsLock(this);
	// can't assert because it's called from constructor
	String tag = "A" + Integer.toString(tagCounter++, 10); // unique tag

	output.writeBytes(tag + " " + command);
    
	if (args != null) {
	    output.write(' ');
	    args.write(this);
	}

	output.write(CRLF);
	output.flush();
	return tag;
    }

    /**
     * Send a command to the server. Collect all responses until either
     * the corresponding command completion response or a BYE response 
     * (indicating server failure).  Return all the collected responses.
     *
     * @param	command	the command
     * @param	args	the arguments
     * @return		array of Response objects returned by the server
     */
    public synchronized Response[] command(String command, Argument args) {
	commandStart(command);
	Vector v = new Vector();
	boolean done = false;
	String tag = null;
	Response r = null;

	// write the command
	try {
	    tag = writeCommand(command, args);
	} catch (LiteralException lex) {
	    v.addElement(lex.getResponse());
	    done = true;
	} catch (Exception ex) {
	    // Convert this into a BYE response
	    v.addElement(Response.byeResponse(ex));
	    done = true;
	}

	Response byeResp = null;
	while (!done) {
	    try {
		r = readResponse();
	    } catch (IOException ioex) {
		if (byeResp != null)	// connection closed after BYE was sent
		    break;
		// convert this into a BYE response
		r = Response.byeResponse(ioex);
	    } catch (ProtocolException pex) {
		continue; // skip this response
	    }

	    if (r.isBYE()) {
		byeResp = r;
		continue;
	    }

	    v.addElement(r);

	    // If this is a matching command completion response, we are done
	    if (r.isTagged() && r.getTag().equals(tag))
		done = true;
	}

	if (byeResp != null)
		v.addElement(byeResp);	// must be last
	Response[] responses = new Response[v.size()];
	v.copyInto(responses);
        timestamp = System.currentTimeMillis();
	commandEnd();
	return responses;
    }

    /**
     * Convenience routine to handle OK, NO, BAD and BYE responses.
     */
    public void handleResult(Response response) throws ProtocolException {
	if (response.isOK())
	    return;
	else if (response.isNO())
	    throw new CommandFailedException(response);
	else if (response.isBAD())
	    throw new BadCommandException(response);
	else if (response.isBYE()) {
	    disconnect();
	    throw new ConnectionException(this, response);
	}
    }

    /**
     * Convenience routine to handle simple IAP commands
     * that do not have responses specific to that command.
     */
    public void simpleCommand(String cmd, Argument args)
			throws ProtocolException {
	// Issue command
	Response[] r = command(cmd, args);

	// dispatch untagged responses
	notifyResponseHandlers(r);

	// Handle result of this command
	handleResult(r[r.length-1]);
    }

    /**
     * Start TLS on the current connection.
     * <code>cmd</code> is the command to issue to start TLS negotiation.
     * If the command succeeds, we begin TLS negotiation.
     * If the socket is already an SSLSocket this is a nop and the command
     * is not issued.
     */
    public synchronized void startTLS(String cmd)
				throws IOException, ProtocolException {
	if (socket instanceof SSLSocket)
	    return;	// nothing to do
	simpleCommand(cmd, null);
	socket = SocketFetcher.startTLS(socket, host, props, prefix);
	initStreams();
    }

    /**
     * Is this connection using an SSL socket?
     *
     * @return	true if using SSL
     * @since	JavaMail 1.4.6
     */
    public boolean isSSL() {
	return socket instanceof SSLSocket;
    }

    /**
     * Disconnect.
     */
    protected synchronized void disconnect() {
	if (socket != null) {
	    try {
		socket.close();
	    } catch (IOException e) {
		// ignore it
	    }
	    socket = null;
	}
    }

    /**
     * Get the name of the local host.
     * The property <prefix>.localhost overrides <prefix>.localaddress,
     * which overrides what InetAddress would tell us.
     */
    protected synchronized String getLocalHost() {
	// get our hostname and cache it for future use
	if (localHostName == null || localHostName.length() <= 0)
	    localHostName =
		    props.getProperty(prefix + ".localhost");
	if (localHostName == null || localHostName.length() <= 0)
	    localHostName =
		    props.getProperty(prefix + ".localaddress");
	try {
	    if (localHostName == null || localHostName.length() <= 0) {
		InetAddress localHost = InetAddress.getLocalHost();
		localHostName = localHost.getCanonicalHostName();
		// if we can't get our name, use local address literal
		if (localHostName == null)
		    // XXX - not correct for IPv6
		    localHostName = "[" + localHost.getHostAddress() + "]";
	    }
	} catch (UnknownHostException uhex) {
	}

	// last chance, try to get our address from our socket
	if (localHostName == null || localHostName.length() <= 0) {
	    if (socket != null && socket.isBound()) {
		InetAddress localHost = socket.getLocalAddress();
		localHostName = localHost.getCanonicalHostName();
		// if we can't get our name, use local address literal
		if (localHostName == null)
		    // XXX - not correct for IPv6
		    localHostName = "[" + localHost.getHostAddress() + "]";
	    }
	}
	return localHostName;
    }

    /**
     * Is protocol tracing enabled?
     */
    protected boolean isTracing() {
	return traceLogger.isLoggable(Level.FINEST);
    }

    /**
     * Temporarily turn off protocol tracing, e.g., to prevent
     * tracing the authentication sequence, including the password.
     */
    protected void suspendTracing() {
	if (traceLogger.isLoggable(Level.FINEST)) {
	    traceInput.setTrace(false);
	    traceOutput.setTrace(false);
	}
    }

    /**
     * Resume protocol tracing, if it was enabled to begin with.
     */
    protected void resumeTracing() {
	if (traceLogger.isLoggable(Level.FINEST)) {
	    traceInput.setTrace(true);
	    traceOutput.setTrace(true);
	}
    }

    /**
     * Finalizer.
     */
    protected void finalize() throws Throwable {
	super.finalize();
	disconnect();
    }

    /*
     * Probe points for GlassFish monitoring.
     */
    private void commandStart(String command) { }
    private void commandEnd() { }
}
