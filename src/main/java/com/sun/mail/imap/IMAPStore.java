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

import java.lang.reflect.*;
import java.util.Vector;
import java.util.StringTokenizer;
import java.io.PrintStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

import javax.mail.*;
import javax.mail.event.*;

import com.sun.mail.iap.*;
import com.sun.mail.imap.protocol.*;
import com.sun.mail.util.PropUtil;
import com.sun.mail.util.MailLogger;
import com.sun.mail.util.SocketConnectException;
import com.sun.mail.util.MailConnectException;

/**
 * This class provides access to an IMAP message store. <p>
 *
 * Applications that need to make use of IMAP-specific features may cast
 * a <code>Store</code> object to an <code>IMAPStore</code> object and
 * use the methods on this class. The {@link #getQuota getQuota} and
 * {@link #setQuota setQuota} methods support the IMAP QUOTA extension.
 * Refer to <A HREF="http://www.ietf.org/rfc/rfc2087.txt">RFC 2087</A>
 * for more information. <p>
 *
 * See the <a href="package-summary.html">com.sun.mail.imap</a> package
 * documentation for further information on the IMAP protocol provider. <p>
 *
 * <strong>WARNING:</strong> The APIs unique to this class should be
 * considered <strong>EXPERIMENTAL</strong>.  They may be changed in the
 * future in ways that are incompatible with applications using the
 * current APIs.
 *
 * @author  John Mani
 * @author  Bill Shannon
 * @author  Jim Glennon
 */
/*
 * This package is implemented over the "imap.protocol" package, which
 * implements the protocol-level commands. <p>
 *
 * A connected IMAPStore maintains a pool of IMAP protocol objects for
 * use in communicating with the IMAP server. The IMAPStore will create
 * the initial AUTHENTICATED connection and seed the pool with this
 * connection. As folders are opened and new IMAP protocol objects are
 * needed, the IMAPStore will provide them from the connection pool,
 * or create them if none are available. When a folder is closed,
 * its IMAP protocol object is returned to the connection pool if the
 * pool is not over capacity. The pool size can be configured by setting
 * the mail.imap.connectionpoolsize property. <p>
 *
 * Note that all connections in the connection pool have their response
 * handler set to be the Store.  When the connection is removed from the
 * pool for use by a folder, the response handler is removed and then set
 * to either the Folder or to the special nonStoreResponseHandler, depending
 * on how the connection is being used.  This is probably excessive.
 * Better would be for the Protocol object to support only a single
 * response handler, which would be set before the connection is used
 * and cleared when the connection is in the pool and can't be used. <p>
 *
 * A mechanism is provided for timing out idle connection pool IMAP
 * protocol objects. Timed out connections are closed and removed (pruned)
 * from the connection pool. The time out interval can be configured via
 * the mail.imap.connectionpooltimeout property. <p>
 *
 * The connected IMAPStore object may or may not maintain a separate IMAP
 * protocol object that provides the store a dedicated connection to the
 * IMAP server. This is provided mainly for compatibility with previous
 * implementations of JavaMail and is determined by the value of the 
 * mail.imap.separatestoreconnection property. <p>
 *
 * An IMAPStore object provides closed IMAPFolder objects thru its list()
 * and listSubscribed() methods. A closed IMAPFolder object acquires an
 * IMAP protocol object from the store to communicate with the server. When
 * the folder is opened, it gets its own protocol object and thus its own,
 * separate connection to the server. The store maintains references to
 * all 'open' folders. When a folder is/gets closed, the store removes
 * it from its list. When the store is/gets closed, it closes all open 
 * folders in its list, thus cleaning up all open connections to the
 * server. <p>
 *
 * A mutex is used to control access to the connection pool resources.
 * Any time any of these resources need to be accessed, the following
 * convention should be followed:
 *
 *     synchronized (pool) { // ACQUIRE LOCK
 *         // access connection pool resources
 *     } // RELEASE LOCK <p>
 *
 * The locking relationship between the store and folders is that the
 * store lock must be acquired before a folder lock. This is currently only
 * applicable in the store's cleanup method. It's important that the
 * connection pool lock is not held when calling into folder objects.
 * The locking hierarchy is that a folder lock must be acquired before
 * any connection pool operations are performed.  You never need to hold
 * all three locks, but if you hold more than one this is the order you
 * have to acquire them in. <p>
 *
 * That is: Store > Folder, Folder > pool, Store > pool <p>
 *
 * The IMAPStore implements the ResponseHandler interface and listens to
 * BYE or untagged OK-notification events from the server as a result of
 * Store operations.  IMAPFolder forwards notifications that result from
 * Folder operations using the store connection; the IMAPStore ResponseHandler
 * is not used directly in this case. <p>
 */

public class IMAPStore extends Store 
	     implements QuotaAwareStore, ResponseHandler {
    
    /**
     * A special event type for a StoreEvent to indicate an IMAP
     * response, if the mail.imap.enableimapevents property is set.
     */
    public static final int RESPONSE = 1000;

    protected final String name;	// name of this protocol
    protected final int defaultPort;	// default IMAP port
    protected final boolean isSSL;	// use SSL?

    private final int blksize;		// Block size for data requested
					// in FETCH requests. Defaults to
					// 16K

    private boolean ignoreSize;		// ignore the size in BODYSTRUCTURE?

    private final int statusCacheTimeout;	// cache Status for 1 second

    private final int appendBufferSize;	// max size of msg buffered for append

    private final int minIdleTime;	// minimum idle time

    private volatile int port = -1;	// port to use

    // Auth info
    protected String host;
    protected String user;
    protected String password;
    protected String proxyAuthUser;
    protected String authorizationID;
    protected String saslRealm;

    private Namespaces namespaces;

    private boolean disableAuthLogin = false;	// disable AUTH=LOGIN
    private boolean disableAuthPlain = false;	// disable AUTH=PLAIN
    private boolean disableAuthNtlm = false;	// disable AUTH=NTLM
    private boolean enableStartTLS = false;	// enable STARTTLS
    private boolean requireStartTLS = false;	// require STARTTLS
    private boolean usingSSL = false;		// using SSL?
    private boolean enableSASL = false;		// enable SASL authentication
    private String[] saslMechanisms;
    private boolean forcePasswordRefresh = false;
    // enable notification of IMAP responses
    private boolean enableImapEvents = false;
    private String guid;			// for Yahoo! Mail IMAP

    /*
     * This field is set in the Store's response handler if we see
     * a BYE response.  The releaseStore method checks this field
     * and if set it cleans up the Store.  Field is volatile because
     * there's no lock we consistently hold while manipulating it.
     *
     * Because volatile doesn't really work before JDK 1.5,
     * use a lock to protect these two fields.
     */
    private volatile boolean connectionFailed = false;
    private volatile boolean forceClose = false;
    private final Object connectionFailedLock = new Object();

    private boolean debugusername;	// include username in debug output?
    private boolean debugpassword;	// include password in debug output?
    protected MailLogger logger;	// for debug output

    private boolean messageCacheDebug;

    // constructors for IMAPFolder class provided by user
    private volatile Constructor folderConstructor = null;
    private volatile Constructor folderConstructorLI = null;

    // Connection pool info

    static class ConnectionPool {

        // container for the pool's IMAP protocol objects
        private Vector authenticatedConnections = new Vector();

        // vectore of open folders
        private Vector folders;

        // is the store connection being used?
        private boolean storeConnectionInUse = false; 

        // the last time (in millis) the pool was checked for timed out
        // connections
        private long lastTimePruned;

        // flag to indicate whether there is a dedicated connection for
        // store commands
        private final boolean separateStoreConnection;

        // client timeout interval
        private final long clientTimeoutInterval;

        // server timeout interval
        private final long serverTimeoutInterval;

        // size of the connection pool
        private final int poolSize;

        // interval for checking for timed out connections
        private final long pruningInterval;
    
        // connection pool logger
        private final MailLogger logger;

	/*
	 * The idleState field supports the IDLE command.
	 * Normally when executing an IMAP command we hold the
	 * store's lock.
	 * While executing the IDLE command we can't hold the
	 * lock or it would prevent other threads from
	 * entering Store methods even far enough to check whether
	 * an IDLE command is in progress.  We need to check before
	 * issuing another command so that we can abort the IDLE
	 * command.
	 *
	 * The idleState field is protected by the store's lock.
	 * The RUNNING state is the normal state and means no IDLE
	 * command is in progress.  The IDLE state means we've issued
	 * an IDLE command and are reading responses.  The ABORTING
	 * state means we've sent the DONE continuation command and
	 * are waiting for the thread running the IDLE command to
	 * break out of its read loop.
	 *
	 * When an IDLE command is in progress, the thread calling
	 * the idle method will be reading from the IMAP connection
	 * while not holding the store's lock.
	 * It's obviously critical that no other thread try to send a
	 * command or read from the connection while in this state.
	 * However, other threads can send the DONE continuation
	 * command that will cause the server to break out of the IDLE
	 * loop and send the ending tag response to the IDLE command.
	 * The thread in the idle method that's reading the responses
	 * from the IDLE command will see this ending response and
	 * complete the idle method, setting the idleState field back
	 * to RUNNING, and notifying any threads waiting to use the
	 * connection.
	 *
	 * All uses of the IMAP connection (IMAPProtocol object) must
	 * be preceeded by a check to make sure an IDLE command is not
	 * running, and abort the IDLE command if necessary.  This check
	 * is made while holding the connection pool lock.  While
	 * waiting for the IDLE command to complete, these other threads
	 * will give up the connection pool lock.  This check is done by
	 * the getStoreProtocol() method.
	 */
	private static final int RUNNING = 0;	// not doing IDLE command
	private static final int IDLE = 1;	// IDLE command in effect
	private static final int ABORTING = 2;	// IDLE command aborting
	private int idleState = RUNNING;
	private IMAPProtocol idleProtocol;	// protocol object when IDLE

	ConnectionPool(String name, MailLogger plogger, Session session) {
	    lastTimePruned = System.currentTimeMillis();

	    boolean debug = PropUtil.getBooleanSessionProperty(session,
		"mail." + name + ".connectionpool.debug", false);
	    logger = plogger.getSubLogger("connectionpool",
					    "DEBUG IMAP CP", debug);

	    // check if the default connection pool size is overridden
	    int size = PropUtil.getIntSessionProperty(session,
		"mail." + name + ".connectionpoolsize", -1);
	    if (size > 0) {
		poolSize = size;
		if (logger.isLoggable(Level.CONFIG))
		    logger.config("mail.imap.connectionpoolsize: " + poolSize);
	    } else
		poolSize = 1;

	    // check if the default client-side timeout value is overridden
	    int connectionPoolTimeout = PropUtil.getIntSessionProperty(session,
		"mail." + name + ".connectionpooltimeout", -1);
	    if (connectionPoolTimeout > 0) {
		clientTimeoutInterval = connectionPoolTimeout;
		if (logger.isLoggable(Level.CONFIG))
		    logger.config("mail.imap.connectionpooltimeout: " +
			clientTimeoutInterval);
	    } else 
		clientTimeoutInterval = 45 * 1000;	// 45 seconds

	    // check if the default server-side timeout value is overridden
	    int serverTimeout = PropUtil.getIntSessionProperty(session,
		"mail." + name + ".servertimeout", -1);
	    if (serverTimeout > 0) {
		serverTimeoutInterval = serverTimeout;
		if (logger.isLoggable(Level.CONFIG))
		    logger.config("mail.imap.servertimeout: " +
			serverTimeoutInterval);
	    }  else
		serverTimeoutInterval = 30 * 60 * 1000;	// 30 minutes

	    // check if the default server-side timeout value is overridden
	    int pruning = PropUtil.getIntSessionProperty(session,
		"mail." + name + ".pruninginterval", -1);
	    if (pruning > 0) {
		pruningInterval = pruning;
		if (logger.isLoggable(Level.CONFIG))
		    logger.config("mail.imap.pruninginterval: " +
			pruningInterval);
	    }  else
		pruningInterval = 60 * 1000;		// 1 minute
     
	    // check to see if we should use a separate (i.e. dedicated)
	    // store connection
	    separateStoreConnection =
		PropUtil.getBooleanSessionProperty(session,
		    "mail." + name + ".separatestoreconnection", false);
	    if (separateStoreConnection)
		logger.config("dedicate a store connection");

	}
    }
 
    private final ConnectionPool pool;

    /**
     * A special response handler for connections that are being used
     * to perform operations on behalf of an object other than the Store.
     * It DOESN'T cause the Store to be cleaned up if a BYE is seen.
     * The BYE may be real or synthetic and in either case just indicates
     * that the connection is dead.
     */
    private ResponseHandler nonStoreResponseHandler = new ResponseHandler() {
	public void handleResponse(Response r) {
	    // Any of these responses may have a response code.
	    if (r.isOK() || r.isNO() || r.isBAD() || r.isBYE())
		handleResponseCode(r);
	    if (r.isBYE())
		logger.fine("IMAPStore non-store connection dead");
	}
    };
 
    /**
     * Constructor that takes a Session object and a URLName that
     * represents a specific IMAP server.
     */
    public IMAPStore(Session session, URLName url) {
	this(session, url, "imap", false);
    }

    /**
     * Constructor used by this class and by IMAPSSLStore subclass.
     */
    protected IMAPStore(Session session, URLName url,
				String name, boolean isSSL) {
	super(session, url); // call super constructor
	if (url != null)
	    name = url.getProtocol();
	this.name = name;
	if (!isSSL)
	    isSSL = PropUtil.getBooleanSessionProperty(session,
				"mail." + name + ".ssl.enable", false);
	if (isSSL)
	    this.defaultPort = 993;
	else
	    this.defaultPort = 143;
	this.isSSL = isSSL;

        debug = session.getDebug();
	debugusername = PropUtil.getBooleanSessionProperty(session,
			"mail.debug.auth.username", true);
	debugpassword = PropUtil.getBooleanSessionProperty(session,
			"mail.debug.auth.password", false);
	logger = new MailLogger(this.getClass(),
				"DEBUG " + name.toUpperCase(), session);

	boolean partialFetch = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".partialfetch", true);
	if (!partialFetch) {
	    blksize = -1;
	    logger.config("mail.imap.partialfetch: false");
	} else {
	    blksize = PropUtil.getIntSessionProperty(session,
		"mail." + name +".fetchsize", 1024 * 16);
	    if (logger.isLoggable(Level.CONFIG))
		logger.config("mail.imap.fetchsize: " + blksize);
	}

	ignoreSize = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name +".ignorebodystructuresize", false);
	if (logger.isLoggable(Level.CONFIG))
	    logger.config("mail.imap.ignorebodystructuresize: " + ignoreSize);

	statusCacheTimeout = PropUtil.getIntSessionProperty(session,
	    "mail." + name + ".statuscachetimeout", 1000);
	if (logger.isLoggable(Level.CONFIG))
	    logger.config("mail.imap.statuscachetimeout: " +
						statusCacheTimeout);

	appendBufferSize = PropUtil.getIntSessionProperty(session,
	    "mail." + name + ".appendbuffersize", -1);
	if (logger.isLoggable(Level.CONFIG))
	    logger.config("mail.imap.appendbuffersize: " + appendBufferSize);

	minIdleTime = PropUtil.getIntSessionProperty(session,
	    "mail." + name + ".minidletime", 10);
	if (logger.isLoggable(Level.CONFIG))
	    logger.config("mail.imap.minidletime: " + minIdleTime);

	// check if we should do a PROXYAUTH login
	String s = session.getProperty("mail." + name + ".proxyauth.user");
	if (s != null) {
	    proxyAuthUser = s;
	    if (logger.isLoggable(Level.CONFIG))
		logger.config("mail.imap.proxyauth.user: " + proxyAuthUser);
	}

	// check if AUTH=LOGIN is disabled
	disableAuthLogin = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".auth.login.disable", false);
	if (disableAuthLogin)
	    logger.config("disable AUTH=LOGIN");

	// check if AUTH=PLAIN is disabled
	disableAuthPlain = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".auth.plain.disable", false);
	if (disableAuthPlain)
	    logger.config("disable AUTH=PLAIN");

	// check if AUTH=NTLM is disabled
	disableAuthNtlm = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".auth.ntlm.disable", false);
	if (disableAuthNtlm)
	    logger.config("disable AUTH=NTLM");

	// check if STARTTLS is enabled
	enableStartTLS = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".starttls.enable", false);
	if (enableStartTLS)
	    logger.config("enable STARTTLS");

	// check if STARTTLS is required
	requireStartTLS = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".starttls.required", false);
	if (requireStartTLS)
	    logger.config("require STARTTLS");

	// check if SASL is enabled
	enableSASL = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".sasl.enable", false);
	if (enableSASL)
	    logger.config("enable SASL");

	// check if SASL mechanisms are specified
	if (enableSASL) {
	    s = session.getProperty("mail." + name + ".sasl.mechanisms");
	    if (s != null && s.length() > 0) {
		if (logger.isLoggable(Level.CONFIG))
		    logger.config("SASL mechanisms allowed: " + s);
		Vector v = new Vector(5);
		StringTokenizer st = new StringTokenizer(s, " ,");
		while (st.hasMoreTokens()) {
		    String m = st.nextToken();
		    if (m.length() > 0)
			v.addElement(m);
		}
		saslMechanisms = new String[v.size()];
		v.copyInto(saslMechanisms);
	    }
	}

	// check if an authorization ID has been specified
	s = session.getProperty("mail." + name + ".sasl.authorizationid");
	if (s != null) {
	    authorizationID = s;
	    logger.log(Level.CONFIG, "mail.imap.sasl.authorizationid: {0}",
						authorizationID);
	}

	// check if a SASL realm has been specified
	s = session.getProperty("mail." + name + ".sasl.realm");
	if (s != null) {
	    saslRealm = s;
	    logger.log(Level.CONFIG, "mail.imap.sasl.realm: {0}", saslRealm);
	}

	// check if forcePasswordRefresh is enabled
	forcePasswordRefresh = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".forcepasswordrefresh", false);
	if (forcePasswordRefresh)
	    logger.config("enable forcePasswordRefresh");

	// check if enableimapevents is enabled
	enableImapEvents = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".enableimapevents", false);
	if (enableImapEvents)
	    logger.config("enable IMAP events");

	// check if message cache debugging set
	messageCacheDebug = PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".messagecache.debug", false);

	guid = session.getProperty("mail." + name + ".yahoo.guid");
	if (guid != null)
	    logger.log(Level.CONFIG, "mail.imap.yahoo.guid: {0}", guid);

	s = session.getProperty("mail." + name + ".folder.class");
	if (s != null) {
	    logger.log(Level.CONFIG, "IMAP: folder class: {0}", s);
	    try {
		ClassLoader cl = this.getClass().getClassLoader();

		// now load the class
		Class folderClass = null;
		try {
		    // First try the "application's" class loader.
		    // This should eventually be replaced by
		    // Thread.currentThread().getContextClassLoader().
		    folderClass = Class.forName(s, false, cl);
		} catch (ClassNotFoundException ex1) {
		    // That didn't work, now try the "system" class loader.
		    // (Need both of these because JDK 1.1 class loaders
		    // may not delegate to their parent class loader.)
		    folderClass = Class.forName(s);
		}

		Class[] c = { String.class, char.class, IMAPStore.class,
				Boolean.class };
		folderConstructor = folderClass.getConstructor(c);
		Class[] c2 = { ListInfo.class, IMAPStore.class };
		folderConstructorLI = folderClass.getConstructor(c2);
	    } catch (Exception ex) {
		logger.log(Level.CONFIG,
			"IMAP: failed to load folder class", ex);
	    }
	}

	pool = new ConnectionPool(name, logger, session);
    }

    /**
     * Implementation of protocolConnect().  Will create a connection
     * to the server and authenticate the user using the mechanisms
     * specified by various properties. <p>
     *
     * The <code>host</code>, <code>user</code>, and <code>password</code>
     * parameters must all be non-null.  If the authentication mechanism
     * being used does not require a password, an empty string or other
     * suitable dummy password should be used.
     */
    protected synchronized boolean 
    protocolConnect(String host, int pport, String user, String password)
		throws MessagingException {
        
        IMAPProtocol protocol = null;

	// check for non-null values of host, password, user
	if (host == null || password == null || user == null) {
	    if (logger.isLoggable(Level.FINE))
		logger.fine("protocolConnect returning false" +
				", host=" + host +
				", user=" + traceUser(user) +
				", password=" + tracePassword(password));
	    return false;
	}

	// set the port correctly
	if (pport != -1) {
	    port = pport;
	} else {
	    port = PropUtil.getIntSessionProperty(session,
					"mail." + name + ".port", port);
	} 
	
	// use the default if needed
	if (port == -1) {
	    port = defaultPort;
	}
	
	try {
            boolean poolEmpty;
            synchronized (pool) {
                poolEmpty = pool.authenticatedConnections.isEmpty();
            }

            if (poolEmpty) {
		if (logger.isLoggable(Level.FINE))
		    logger.fine("trying to connect to host \"" + host +
				"\", port " + port + ", isSSL " + isSSL);
                protocol = newIMAPProtocol(host, port);
		if (logger.isLoggable(Level.FINE))
		    logger.fine("protocolConnect login" +
				", host=" + host +
				", user=" + traceUser(user) +
				", password=" + tracePassword(password));
	        login(protocol, user, password);

	        protocol.addResponseHandler(this);

		usingSSL = protocol.isSSL();	// in case anyone asks

	        this.host = host;
	        this.user = user;
	        this.password = password;

                synchronized (pool) {
                    pool.authenticatedConnections.addElement(protocol);
                }
            }
	} catch (CommandFailedException cex) {
	    // login failure, close connection to server
	    if (protocol != null)
		protocol.disconnect();
	    protocol = null;
	    throw new AuthenticationFailedException(
					cex.getResponse().getRest());
	} catch (ProtocolException pex) { // any other exception
	    // failure in login command, close connection to server
	    if (protocol != null)
		protocol.disconnect();
	    protocol = null;
	    throw new MessagingException(pex.getMessage(), pex);
	} catch (SocketConnectException scex) {
	    throw new MailConnectException(scex);
	} catch (IOException ioex) {
	    throw new MessagingException(ioex.getMessage(), ioex);
	} 

        return true;
    }

    /**
     * Create an IMAPProtocol object connected to the host and port.
     * Subclasses of IMAPStore may override this method to return a
     * subclass of IMAPProtocol that supports product-specific extensions.
     *
     * @since JavaMail 1.4.6
     */
    protected IMAPProtocol newIMAPProtocol(String host, int port)
				throws IOException, ProtocolException {
	return new IMAPProtocol(name, host, port, 
					    session.getProperties(),
					    isSSL,
					    logger
					   );
    }

    private void login(IMAPProtocol p, String u, String pw) 
		throws ProtocolException {
	// turn on TLS if it's been enabled or required and is supported
	if (enableStartTLS || requireStartTLS) {
	    if (p.hasCapability("STARTTLS")) {
		p.startTLS();
		// if startTLS succeeds, refresh capabilities
		p.capability();
	    } else if (requireStartTLS) {
		logger.fine("STARTTLS required but not supported by server");
		throw new ProtocolException(
		    "STARTTLS required but not supported by server");
	    }
	}
	if (p.isAuthenticated())
	    return;		// no need to login

	// allow subclasses to issue commands before login
	preLogin(p);

	// issue special ID command to Yahoo! Mail IMAP server
	if (guid != null)
	    p.id(guid);

	/*
	 * Put a special "marker" in the capabilities list so we can
	 * detect if the server refreshed the capabilities in the OK
	 * response.
	 */
	p.getCapabilities().put("__PRELOGIN__", "");
	String authzid;
	if (authorizationID != null)
	    authzid = authorizationID;
	else if (proxyAuthUser != null)
	    authzid = proxyAuthUser;
	else
	    authzid = null;

	if (enableSASL)
	    p.sasllogin(saslMechanisms, saslRealm, authzid, u, pw);

	if (p.isAuthenticated())
	    ;	// SASL login succeeded, go to bottom
	else if (p.hasCapability("AUTH=PLAIN") && !disableAuthPlain)
	    p.authplain(authzid, u, pw);
	else if ((p.hasCapability("AUTH-LOGIN") ||
		p.hasCapability("AUTH=LOGIN")) && !disableAuthLogin)
	    p.authlogin(u, pw);
	else if (p.hasCapability("AUTH=NTLM") && !disableAuthNtlm)
	    p.authntlm(authzid, u, pw);
	else if (!p.hasCapability("LOGINDISABLED"))
	    p.login(u, pw);
	else
	    throw new ProtocolException("No login methods supported!");

	if (proxyAuthUser != null)
	    p.proxyauth(proxyAuthUser);

	/*
	 * If marker is still there, capabilities haven't been refreshed,
	 * refresh them now.
	 */
	if (p.hasCapability("__PRELOGIN__")) {
	    try {
		p.capability();
	    } catch (ConnectionException cex) {
		throw cex;	// rethrow connection failures
		// XXX - assume connection has been closed
	    } catch (ProtocolException pex) {
		// ignore other exceptions that "should never happen"
	    }
	}
    }

    /**
     * This method is called after the connection is made and
     * TLS is started (if needed), but before any authentication
     * is attempted.  Subclasses can override this method to
     * issue commands that are needed in the "not authenticated"
     * state.  Note that if the connection is pre-authenticated,
     * this method won't be called. <p>
     *
     * The implementation of this method in this class does nothing.
     *
     * @since JavaMail 1.4.4
     */
    protected void preLogin(IMAPProtocol p) throws ProtocolException {
    }

    /**
     * Does this IMAPStore use SSL when connecting to the server?
     *
     * @return	true if using SSL
     * @since	JavaMail 1.4.6
     */
    public boolean isSSL() {
        return usingSSL;
    }

    /**
     * Set the user name that will be used for subsequent connections
     * after this Store is first connected (for example, when creating
     * a connection to open a Folder).  This value is overridden
     * by any call to the Store's connect method. <p>
     *
     * Some IMAP servers may provide an authentication ID that can
     * be used for more efficient authentication for future connections.
     * This authentication ID is provided in a server-specific manner
     * not described here. <p>
     *
     * Most applications will never need to use this method.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized void setUsername(String user) {
	this.user = user;
    }

    /**
     * Set the password that will be used for subsequent connections
     * after this Store is first connected (for example, when creating
     * a connection to open a Folder).  This value is overridden
     * by any call to the Store's connect method. <p>
     *
     * Most applications will never need to use this method.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized void setPassword(String password) {
	this.password = password;
    }

    /*
     * Get a new authenticated protocol object for this Folder.
     * Also store a reference to this folder in our list of
     * open folders.
     */
    IMAPProtocol getProtocol(IMAPFolder folder) 
		throws MessagingException {
	IMAPProtocol p = null;

	// keep looking for a connection until we get a good one
	while (p == null) {
 
        // New authenticated protocol objects are either acquired
        // from the connection pool, or created when the pool is
        // empty or no connections are available. None are available
        // if the current pool size is one and the separate store
        // property is set or the connection is in use.

        synchronized (pool) {

            // If there's none available in the pool,
            // create a new one.
            if (pool.authenticatedConnections.isEmpty() ||
                (pool.authenticatedConnections.size() == 1 &&
                (pool.separateStoreConnection || pool.storeConnectionInUse))) {

		logger.fine("no connections in the pool, creating a new one");
                try {
		    if (forcePasswordRefresh)
			refreshPassword();
                    // Use cached host, port and timeout values.
                    p = newIMAPProtocol(host, port);
                    // Use cached auth info
                    login(p, user, password);
                } catch(Exception ex1) {
                    if (p != null)
                        try {
                            p.disconnect();
                        } catch (Exception ex2) { }
                    p = null;
                }
                 
                if (p == null)
                    throw new MessagingException("connection failure");
            } else {
		if (logger.isLoggable(Level.FINE))
                    logger.fine("connection available -- size: " +
                        pool.authenticatedConnections.size());

                // remove the available connection from the Authenticated queue
                p = (IMAPProtocol)pool.authenticatedConnections.lastElement();
                pool.authenticatedConnections.removeElement(p);

		// check if the connection is still live
		long lastUsed = System.currentTimeMillis() - p.getTimestamp();
		if (lastUsed > pool.serverTimeoutInterval) {
		    try {
			/*
			 * Swap in a special response handler that will handle
			 * alerts, but won't cause the store to be closed and
			 * cleaned up if the connection is dead.
			 */
			p.removeResponseHandler(this);
			p.addResponseHandler(nonStoreResponseHandler);
			p.noop();
			p.removeResponseHandler(nonStoreResponseHandler);
			p.addResponseHandler(this);
		    } catch (ProtocolException pex) {
			try {
			    p.removeResponseHandler(nonStoreResponseHandler);
			    p.disconnect();
			} finally {
			    // don't let any exception stop us
			    p = null;
			    continue;	// try again, from the top
			}
		    }
		}

                // remove the store as a response handler.
                p.removeResponseHandler(this);
	    }

            // check if we need to look for client-side timeouts
            timeoutConnections();

	    // Add folder to folder-list
	    if (folder != null) {
                if (pool.folders == null)
                    pool.folders = new Vector();
		pool.folders.addElement(folder);
	    }
        }

	}
	
	return p;
    }

    /**
     * Get this Store's protocol connection.
     *
     * When acquiring a store protocol object, it is important to
     * use the following steps:
     *
     *     IMAPProtocol p = null;
     *     try {
     *         p = getStoreProtocol();
     *         // perform the command
     *     } catch (ConnectionException cex) {
     *         throw new StoreClosedException(this, cex.getMessage());
     *     } catch (WhateverException ex) {
     *         // handle it
     *     } finally {
     *         releaseStoreProtocol(p);
     *     }
     */
    private IMAPProtocol getStoreProtocol() throws ProtocolException {
        IMAPProtocol p = null;

	while (p == null) {
        synchronized (pool) {
	    waitIfIdle();

            // If there's no authenticated connections available create a 
            // new one and place it in the authenticated queue.
            if (pool.authenticatedConnections.isEmpty()) {
		pool.logger.fine("getStoreProtocol() - no connections " +
                        "in the pool, creating a new one");
                try {
		    if (forcePasswordRefresh)
			refreshPassword();
                    // Use cached host, port and timeout values.
                    p = newIMAPProtocol(host, port);
                    // Use cached auth info
                    login(p, user, password);
                } catch(Exception ex1) {
                    if (p != null)
                        try {
                            p.logout();
                        } catch (Exception ex2) { }
                    p = null;
                }
 
                if (p == null)
                    throw new ConnectionException(
				"failed to create new store connection");
             
	        p.addResponseHandler(this);
                pool.authenticatedConnections.addElement(p);
 
            } else {
                // Always use the first element in the Authenticated queue.
		if (pool.logger.isLoggable(Level.FINE))
                    pool.logger.fine("getStoreProtocol() - " +
                        "connection available -- size: " +
                        pool.authenticatedConnections.size());
                p = (IMAPProtocol)pool.authenticatedConnections.firstElement();
            }
 
	    if (pool.storeConnectionInUse) {
		try {
		    // someone else is using the connection, give up
		    // and wait until they're done
		    p = null;
		    pool.wait();
		} catch (InterruptedException ex) { }
	    } else {
		pool.storeConnectionInUse = true;

		pool.logger.fine("getStoreProtocol() -- storeConnectionInUse");
	    }
 
            timeoutConnections();
        }
	}
	return p;
    }

    /**
     * Get a store protocol object for use by a folder.
     */
    IMAPProtocol getFolderStoreProtocol() throws ProtocolException {
	IMAPProtocol p = getStoreProtocol();
	p.removeResponseHandler(this);
	p.addResponseHandler(nonStoreResponseHandler);
	return p;
    }

    /*
     * Some authentication systems use one time passwords
     * or tokens, so each authentication request requires
     * a new password.  This "kludge" allows a callback
     * to application code to get a new password.
     *
     * XXX - remove this when SASL support is added
     */
    private void refreshPassword() {
	if (logger.isLoggable(Level.FINE))
	    logger.fine("refresh password, user: " + traceUser(user));
	InetAddress addr;
	try {
	    addr = InetAddress.getByName(host);
	} catch (UnknownHostException e) {
	    addr = null;
	}
	PasswordAuthentication pa =
	    session.requestPasswordAuthentication(addr, port,
					name, null, user);
	if (pa != null) {
	    user = pa.getUserName();
	    password = pa.getPassword();
	}
    }

    /**
     * If a SELECT succeeds, but indicates that the folder is
     * READ-ONLY, and the user asked to open the folder READ_WRITE,
     * do we allow the open to succeed?
     */
    boolean allowReadOnlySelect() {
	return PropUtil.getBooleanSessionProperty(session,
	    "mail." + name + ".allowreadonlyselect", false);
    }

    /**
     * Report whether the separateStoreConnection is set.
     */
    boolean hasSeparateStoreConnection() {
        return pool.separateStoreConnection;
    }

    /** 
     * Return the connection pool logger.
     */ 
    MailLogger getConnectionPoolLogger() {
        return pool.logger; 
    } 
 
    /** 
     * Report whether message cache debugging is enabled. 
     */ 
    boolean getMessageCacheDebug() {
        return messageCacheDebug; 
    } 
 
    /**
     * Report whether the connection pool is full.
     */
    boolean isConnectionPoolFull() {

        synchronized (pool) {
	    if (pool.logger.isLoggable(Level.FINE))
                pool.logger.fine("connection pool current size: " +
                    pool.authenticatedConnections.size() + 
                    "   pool size: " + pool.poolSize);

            return (pool.authenticatedConnections.size() >= pool.poolSize);

        }
    }

    /**
     * Release the protocol object back to the connection pool.
     */
    void releaseProtocol(IMAPFolder folder, IMAPProtocol protocol) {

        synchronized (pool) {
            if (protocol != null) {
                // If the pool is not full, add the store as a response handler
                // and return the protocol object to the connection pool.
                if (!isConnectionPoolFull()) {
                    protocol.addResponseHandler(this);
                    pool.authenticatedConnections.addElement(protocol);

		    if (logger.isLoggable(Level.FINE))
                        logger.fine(
			    "added an Authenticated connection -- size: " +
                            pool.authenticatedConnections.size());
                } else {
		    logger.fine(
			"pool is full, not adding an Authenticated connection");
                    try {
                        protocol.logout();
                    } catch (ProtocolException pex) {};
                }
            }

            if (pool.folders != null)
                pool.folders.removeElement(folder);

            timeoutConnections();
        }
    }

    /**
     * Release the store connection.
     */
    private void releaseStoreProtocol(IMAPProtocol protocol) {

	// will be called from idle() without the Store lock held,
	// but cleanup is synchronized and will acquire the Store lock

	if (protocol == null) {
	    cleanup();		// failed to ever get the connection
	    return;		// nothing to release
	}

	/*
	 * Read out the flag that says whether this connection failed
	 * before releasing the protocol object for others to use.
	 */
	boolean failed;
	synchronized (connectionFailedLock) {
	    failed = connectionFailed;
	    connectionFailed = false;	// reset for next use
	}

	// now free the store connection
        synchronized (pool) {
	    pool.storeConnectionInUse = false;
	    pool.notifyAll();	// in case anyone waiting

	    pool.logger.fine("releaseStoreProtocol()");

            timeoutConnections();
        }

	/*
	 * If the connection died while we were using it, clean up.
	 * It's critical that the store connection be freed and the
	 * connection pool not be locked while we do this.
	 */
	assert !Thread.holdsLock(pool);
	if (failed)
	    cleanup();
    }

    /**
     * Release a store protocol object that was being used by a folder.
     */
    void releaseFolderStoreProtocol(IMAPProtocol protocol) {
	if (protocol == null)
	    return;		// should never happen
	protocol.removeResponseHandler(nonStoreResponseHandler);
	protocol.addResponseHandler(this);
        synchronized (pool) {
	    pool.storeConnectionInUse = false;
	    pool.notifyAll();	// in case anyone waiting

	    pool.logger.fine("releaseFolderStoreProtocol()");

            timeoutConnections();
        }
    }

    /**
     * Empty the connection pool.
     */ 
    private void emptyConnectionPool(boolean force) {

        synchronized (pool) {
            for (int index = pool.authenticatedConnections.size() - 1;
		    index >= 0; --index) {
                try {
		    IMAPProtocol p = (IMAPProtocol)
			pool.authenticatedConnections.elementAt(index);
		    p.removeResponseHandler(this);
		    if (force)
			p.disconnect();
		    else
			p.logout();
                } catch (ProtocolException pex) {};
            }

            pool.authenticatedConnections.removeAllElements();
        }
        
	pool.logger.fine("removed all authenticated connections from pool");
    }

    /**  
     * Check to see if it's time to shrink the connection pool.
     */  
    private void timeoutConnections() {

        synchronized (pool) {

            // If we've exceeded the pruning interval, look for stale
            // connections to logout.
            if (System.currentTimeMillis() - pool.lastTimePruned > 
                pool.pruningInterval && 
                pool.authenticatedConnections.size() > 1) {

		if (pool.logger.isLoggable(Level.FINE)) {
                    pool.logger.fine("checking for connections to prune: " +
                        (System.currentTimeMillis() - pool.lastTimePruned));
                    pool.logger.fine("clientTimeoutInterval: " +
                        pool.clientTimeoutInterval);
                }   
 
                IMAPProtocol p;
 
                // Check the timestamp of the protocol objects in the pool and
                // logout if the interval exceeds the client timeout value
                // (leave the first connection).
                for (int index = pool.authenticatedConnections.size() - 1; 
                     index > 0; index--) {
                    p = (IMAPProtocol)pool.authenticatedConnections.
                        elementAt(index);
		    if (pool.logger.isLoggable(Level.FINE))
                        pool.logger.fine("protocol last used: " +
                            (System.currentTimeMillis() - p.getTimestamp()));
                    if (System.currentTimeMillis() - p.getTimestamp() >
                        pool.clientTimeoutInterval) {
 
			pool.logger.fine(
			    "authenticated connection timed out, " +
			    "logging out the connection");
 
                        p.removeResponseHandler(this);
                        pool.authenticatedConnections.removeElementAt(index);

                        try {
                            p.logout();
                        } catch (ProtocolException pex) {}
                    }
                }
                pool.lastTimePruned = System.currentTimeMillis();
            }
        }
    }

    /**
     * Get the block size to use for fetch requests on this Store.
     */
    int getFetchBlockSize() {
	return blksize;
    }

    /**
     * Ignore the size reported in the BODYSTRUCTURE when fetching data?
     */
    boolean ignoreBodyStructureSize() {
	return ignoreSize;
    }

    /**
     * Get a reference to the session.
     */
    Session getSession() {
        return session;
    }

    /**
     * Get the number of milliseconds to cache STATUS response.
     */
    int getStatusCacheTimeout() {
	return statusCacheTimeout;
    }

    /**
     * Get the maximum size of a message to buffer for append.
     */
    int getAppendBufferSize() {
	return appendBufferSize;
    }

    /**
     * Get the minimum amount of time to delay when returning from idle.
     */
    int getMinIdleTime() {
	return minIdleTime;
    }

    /**
     * Return true if the specified capability string is in the list
     * of capabilities the server announced.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized boolean hasCapability(String capability)
				throws MessagingException {
        IMAPProtocol p = null;
	try {
	    p = getStoreProtocol();
            return p.hasCapability(capability);
	} catch (ProtocolException pex) {
	    throw new MessagingException(pex.getMessage(), pex);
        } finally {
            releaseStoreProtocol(p);
        }
    }

    /**
     * Check whether this store is connected. Override superclass
     * method, to actually ping our server connection.
     */
    public synchronized boolean isConnected() {
	if (!super.isConnected()) {
	    // if we haven't been connected at all, don't bother with
	    // the NOOP.
	    return false;
	}

	/*
	 * The below noop() request can:
	 * (1) succeed - in which case all is fine.
	 *
	 * (2) fail because the server returns NO or BAD, in which
	 * 	case we ignore it since we can't really do anything.
	 * (2) fail because a BYE response is obtained from the 
	 *	server
	 * (3) fail because the socket.write() to the server fails,
	 *	in which case the iap.protocol() code converts the
	 *	IOException into a BYE response.
	 *
	 * Thus, our BYE handler will take care of closing the Store
	 * in case our connection is really gone.
	 */
   
        IMAPProtocol p = null;
	try {
	    p = getStoreProtocol();
            p.noop();
	} catch (ProtocolException pex) {
	    // will return false below
        } finally {
            releaseStoreProtocol(p);
        }


	return super.isConnected();
    }

    /**
     * Close this Store.
     */
    public synchronized void close() throws MessagingException {
	if (!super.isConnected()) // Already closed.
	    return;

        IMAPProtocol protocol = null;
	try {
	    boolean isEmpty;
	    synchronized (pool) {
		// If there's no authenticated connections available
		// don't create a new one
		isEmpty = pool.authenticatedConnections.isEmpty();
	    }
	    /*
	     * Have to drop the lock before calling cleanup.
	     * Yes, there's a potential race here.  The pool could
	     * become empty after we check, in which case we'll just
	     * waste time getting a new connection and closing it.
	     * Or, the pool could be empty now and not empty by the
	     * time we get into cleanup, but that's ok because cleanup
	     * will just close the connection.
	     */
	    if (isEmpty) {
		pool.logger.fine("close() - no connections ");
		cleanup();
		return;
	    }

            protocol = getStoreProtocol();
	    /*
	     * We have to remove the protocol from the pool so that,
	     * when our response handler processes the BYE response
	     * and calls cleanup, which calls emptyConnection, that
	     * we don't try to log out this connection twice.
	     */
	    synchronized (pool) {
                pool.authenticatedConnections.removeElement(protocol);
	    }

	    /*
	     * LOGOUT. 
	     *
	     * Note that protocol.logout() closes the server socket
	     * connection, regardless of what happens ..
	     *
	     * Also note that protocol.logout() results in a BYE
	     * response (As per rfc 2060, BYE is a *required* response
	     * to LOGOUT). In fact, even if protocol.logout() fails
	     * with an IOException (if the server connection is dead),
	     * iap.Protocol.command() converts that exception into a 
	     * BYE response. So, I depend on my BYE handler to set the
	     * flag that causes releaseStoreProtocol to do the
	     * Store cleanup.
	     */
	    protocol.logout();
	} catch (ProtocolException pex) { 
	    // Hmm .. will this ever happen ?
	    throw new MessagingException(pex.getMessage(), pex);
        } finally {
            releaseStoreProtocol(protocol);
        }
    }

    protected void finalize() throws Throwable {
	super.finalize();
	close();
    }

    /**
     * Cleanup before dying.
     */
    private synchronized void cleanup() {
	// if we're not connected, someone beat us to it
	if (!super.isConnected()) {
	    logger.fine("IMAPStore cleanup, not connected");
	    return;
	}

	/*
	 * If forceClose is true, some thread ran into an error that suggests
	 * the server might be dead, so we force the folders to close
	 * abruptly without waiting for the server.  Used when
	 * the store connection times out, for example.
	 */
	boolean force;
	synchronized (connectionFailedLock) {
	    force = forceClose;
	    forceClose = false;
	    connectionFailed = false;
	}
	if (logger.isLoggable(Level.FINE))
	    logger.fine("IMAPStore cleanup, force " + force);

        Vector foldersCopy = null;
        boolean done = true;

	// To avoid violating the locking hierarchy, there's no lock we
	// can hold that prevents another thread from trying to open a
	// folder at the same time we're trying to close all the folders.
	// Thus, there's an inherent race condition here.  We close all
	// the folders we know about and then check whether any new folders
	// have been opened in the mean time.  We keep trying until we're
	// successful in closing all the folders.
	for (;;) {
	    // Make a copy of the folders list so we do not violate the
	    // folder-connection pool locking hierarchy.
	    synchronized (pool) {
		if (pool.folders != null) {
		    done = false;
		    foldersCopy = pool.folders;
		    pool.folders = null;
		} else {
                    done = true;
                }
	    }
	    if (done)
		break;

	    // Close and remove any open folders under this Store.
	    for (int i = 0, fsize = foldersCopy.size(); i < fsize; i++) {
		IMAPFolder f = (IMAPFolder)foldersCopy.elementAt(i);

		try {
		    if (force) {
			logger.fine("force folder to close");
			// Don't want to wait for folder connection to timeout
			// (if, for example, the server is down) so we close
			// folders abruptly.
			f.forceClose();
		    } else {
			logger.fine("close folder");
			f.close(false);
		    }
		} catch (MessagingException mex) {
		    // Who cares ?! Ignore 'em.
		} catch (IllegalStateException ex) {
		    // Ditto
		}
	    }

	}

        synchronized (pool) {
	    emptyConnectionPool(force);
	}

	// to set the state and send the closed connection event
	try {
	    super.close();
	} catch (MessagingException mex) { }
	logger.fine("IMAPStore cleanup done");
    }

    /**
     * Get the default folder, representing the root of this user's 
     * namespace. Returns a closed DefaultFolder object.
     */
    public synchronized Folder getDefaultFolder() throws MessagingException {
	checkConnected();
	return new DefaultFolder(this);
    }

    /**
     * Get named folder. Returns a new, closed IMAPFolder.
     */
    public synchronized Folder getFolder(String name)
				throws MessagingException {
	checkConnected();
	return newIMAPFolder(name, IMAPFolder.UNKNOWN_SEPARATOR);
    }

    /**
     * Get named folder. Returns a new, closed IMAPFolder.
     */
    public synchronized Folder getFolder(URLName url)
				throws MessagingException {
	checkConnected();
	return newIMAPFolder(url.getFile(), IMAPFolder.UNKNOWN_SEPARATOR);
    }

    /**
     * Create an IMAPFolder object.  If user supplied their own class,
     * use it.  Otherwise, call the constructor.
     */
    protected IMAPFolder newIMAPFolder(String fullName, char separator,
				Boolean isNamespace) {
	IMAPFolder f = null;
	if (folderConstructor != null) {
	    try {
		Object[] o =
		    { fullName, new Character(separator), this, isNamespace };
		f = (IMAPFolder)folderConstructor.newInstance(o);
	    } catch (Exception ex) {
		logger.log(Level.FINE,
			    "exception creating IMAPFolder class", ex);
	    }
	}
	if (f == null)
	    f = new IMAPFolder(fullName, separator, this, isNamespace);
	return f;
    }

    /**
     * Create an IMAPFolder object.  Call the newIMAPFolder method
     * above with a null isNamespace.
     */
    protected IMAPFolder newIMAPFolder(String fullName, char separator) {
	return newIMAPFolder(fullName, separator, null);
    }

    /**
     * Create an IMAPFolder object.  If user supplied their own class,
     * use it.  Otherwise, call the constructor.
     */
    protected IMAPFolder newIMAPFolder(ListInfo li) {
	IMAPFolder f = null;
	if (folderConstructorLI != null) {
	    try {
		Object[] o = { li, this };
		f = (IMAPFolder)folderConstructorLI.newInstance(o);
	    } catch (Exception ex) {
		logger.log(Level.FINE,
			"exception creating IMAPFolder class LI", ex);
	    }
	}
	if (f == null)
	    f = new IMAPFolder(li, this);
	return f;
    }

    /**
     * Using the IMAP NAMESPACE command (RFC 2342), return a set
     * of folders representing the Personal namespaces.
     */
    public Folder[] getPersonalNamespaces() throws MessagingException {
	Namespaces ns = getNamespaces();
	if (ns == null || ns.personal == null)
	    return super.getPersonalNamespaces();
	return namespaceToFolders(ns.personal, null);
    }

    /**
     * Using the IMAP NAMESPACE command (RFC 2342), return a set
     * of folders representing the User's namespaces.
     */
    public Folder[] getUserNamespaces(String user)
				throws MessagingException {
	Namespaces ns = getNamespaces();
	if (ns == null || ns.otherUsers == null)
	    return super.getUserNamespaces(user);
	return namespaceToFolders(ns.otherUsers, user);
    }

    /**
     * Using the IMAP NAMESPACE command (RFC 2342), return a set
     * of folders representing the Shared namespaces.
     */
    public Folder[] getSharedNamespaces() throws MessagingException {
	Namespaces ns = getNamespaces();
	if (ns == null || ns.shared == null)
	    return super.getSharedNamespaces();
	return namespaceToFolders(ns.shared, null);
    }

    private synchronized Namespaces getNamespaces() throws MessagingException {
	checkConnected();

        IMAPProtocol p = null;

	if (namespaces == null) {
	    try {
                p = getStoreProtocol();
		namespaces = p.namespace();
	    } catch (BadCommandException bex) { 
		// NAMESPACE not supported, ignore it
	    } catch (ConnectionException cex) {
		throw new StoreClosedException(this, cex.getMessage());
	    } catch (ProtocolException pex) { 
		throw new MessagingException(pex.getMessage(), pex);
	    } finally {
		releaseStoreProtocol(p);
	    }
	}
	return namespaces;
    }

    private Folder[] namespaceToFolders(Namespaces.Namespace[] ns,
					String user) {
	Folder[] fa = new Folder[ns.length];
	for (int i = 0; i < fa.length; i++) {
	    String name = ns[i].prefix;
	    if (user == null) {
		// strip trailing delimiter
		int len = name.length();
		if ( len > 0 && name.charAt(len - 1) == ns[i].delimiter)
		    name = name.substring(0, len - 1);
	    } else {
		// add user
		name += user;
	    }
	    fa[i] = newIMAPFolder(name, ns[i].delimiter,
					Boolean.valueOf(user == null));
	}
	return fa;
    }

    /**
     * Get the quotas for the named quota root.
     * Quotas are controlled on the basis of a quota root, not
     * (necessarily) a folder.  The relationship between folders
     * and quota roots depends on the IMAP server.  Some servers
     * might implement a single quota root for all folders owned by
     * a user.  Other servers might implement a separate quota root
     * for each folder.  A single folder can even have multiple
     * quota roots, perhaps controlling quotas for different
     * resources.
     *
     * @param	root	the name of the quota root
     * @return		array of Quota objects
     * @exception MessagingException	if the server doesn't support the
     *					QUOTA extension
     */
    public synchronized Quota[] getQuota(String root)
				throws MessagingException {
	checkConnected();
	Quota[] qa = null;

        IMAPProtocol p = null;
	try {
	    p = getStoreProtocol();
	    qa = p.getQuotaRoot(root);
	} catch (BadCommandException bex) {
	    throw new MessagingException("QUOTA not supported", bex);
	} catch (ConnectionException cex) {
	    throw new StoreClosedException(this, cex.getMessage());
	} catch (ProtocolException pex) {
	    throw new MessagingException(pex.getMessage(), pex);
	} finally {
	    releaseStoreProtocol(p);
	}
	return qa;
    }

    /**
     * Set the quotas for the quota root specified in the quota argument.
     * Typically this will be one of the quota roots obtained from the
     * <code>getQuota</code> method, but it need not be.
     *
     * @param	quota	the quota to set
     * @exception MessagingException	if the server doesn't support the
     *					QUOTA extension
     */
    public synchronized void setQuota(Quota quota) throws MessagingException {
	checkConnected();
        IMAPProtocol p = null;
	try {
	    p = getStoreProtocol();
	    p.setQuota(quota);
	} catch (BadCommandException bex) {
	    throw new MessagingException("QUOTA not supported", bex);
	} catch (ConnectionException cex) {
	    throw new StoreClosedException(this, cex.getMessage());
	} catch (ProtocolException pex) {
	    throw new MessagingException(pex.getMessage(), pex);
	} finally {
	    releaseStoreProtocol(p);
	}
    }

    private void checkConnected() {
	assert Thread.holdsLock(this);
	if (!super.isConnected())
	    throw new IllegalStateException("Not connected");
    }

    /**
     * Response handler method.
     */
    public void handleResponse(Response r) {
	// Any of these responses may have a response code.
	if (r.isOK() || r.isNO() || r.isBAD() || r.isBYE())
	    handleResponseCode(r);
	if (r.isBYE()) {
	    logger.fine("IMAPStore connection dead");
	    // Store's IMAP connection is dead, save the response so that
	    // releaseStoreProtocol will cleanup later.
	    synchronized (connectionFailedLock) {
		connectionFailed = true;
		if (r.isSynthetic())
		    forceClose = true;
	    }
	    return;
	}
    }

    /**
     * Use the IMAP IDLE command (see
     * <A HREF="http://www.ietf.org/rfc/rfc2177.txt">RFC 2177</A>),
     * if supported by the server, to enter idle mode so that the server
     * can send unsolicited notifications
     * without the need for the client to constantly poll the server.
     * Use a <code>ConnectionListener</code> to be notified of
     * events.  When another thread (e.g., the listener thread)
     * needs to issue an IMAP comand for this Store, the idle mode will
     * be terminated and this method will return.  Typically the caller
     * will invoke this method in a loop. <p>
     *
     * If the mail.imap.enableimapevents property is set, notifications
     * received while the IDLE command is active will be delivered to
     * <code>ConnectionListener</code>s as events with a type of
     * <code>IMAPStore.RESPONSE</code>.  The event's message will be
     * the raw IMAP response string.
     * Note that most IMAP servers will not deliver any events when
     * using the IDLE command on a connection with no mailbox selected
     * (i.e., this method).  In most cases you'll want to use the
     * <code>idle</code> method on <code>IMAPFolder</code>. <p>
     *
     * NOTE: This capability is highly experimental and likely will change
     * in future releases. <p>
     *
     * The mail.imap.minidletime property enforces a minimum delay
     * before returning from this method, to ensure that other threads
     * have a chance to issue commands before the caller invokes this
     * method again.  The default delay is 10 milliseconds.
     *
     * @exception MessagingException	if the server doesn't support the
     *					IDLE extension
     * @exception IllegalStateException	if the store isn't connected
     *
     * @since	JavaMail 1.4.1
     */
    public void idle() throws MessagingException {
	IMAPProtocol p = null;
	// ASSERT: Must NOT be called with the connection pool
	// synchronization lock held.
	assert !Thread.holdsLock(pool);
	synchronized (this) {
	    checkConnected();
	}
	boolean needNotification = false;
	try {
	    synchronized (pool) {
		p = getStoreProtocol();
		if (pool.idleState != ConnectionPool.RUNNING) {
		    // some other thread must be running the IDLE
		    // command, we'll just wait for it to finish
		    // without aborting it ourselves
		    try {
			// give up lock and wait to be not idle
			pool.wait();
		    } catch (InterruptedException ex) { }
		    return;
		}
		p.idleStart();
		needNotification = true;
		pool.idleState = ConnectionPool.IDLE;
		pool.idleProtocol = p;
	    }

	    /*
	     * We gave up the pool lock so that other threads
	     * can get into the pool far enough to see that we're
	     * in IDLE and abort the IDLE.
	     *
	     * Now we read responses from the IDLE command, especially
	     * including unsolicited notifications from the server.
	     * We don't hold the pool lock while reading because
	     * it protects the idleState and other threads need to be
	     * able to examine the state.
	     *
	     * We hold the pool lock while processing the responses.
	     */
	    for (;;) {
		Response r = p.readIdleResponse();
		synchronized (pool) {
		    if (r == null || !p.processIdleResponse(r)) {
			pool.idleState = ConnectionPool.RUNNING;
			pool.idleProtocol = null;
			pool.notifyAll();
			needNotification = false;
			break;
		    }
		}
		if (enableImapEvents && r.isUnTagged()) {
		    notifyStoreListeners(IMAPStore.RESPONSE, r.toString());
		}
	    }

	    /*
	     * Enforce a minimum delay to give time to threads
	     * processing the responses that came in while we
	     * were idle.
	     */
	    int minidle = getMinIdleTime();
	    if (minidle > 0) {
		try {
		    Thread.sleep(minidle);
		} catch (InterruptedException ex) { }
	    }

	} catch (BadCommandException bex) {
	    throw new MessagingException("IDLE not supported", bex);
	} catch (ConnectionException cex) {
	    throw new StoreClosedException(this, cex.getMessage());
	} catch (ProtocolException pex) {
	    throw new MessagingException(pex.getMessage(), pex);
	} finally {
	    if (needNotification) {
		synchronized (pool) {
		    pool.idleState = ConnectionPool.RUNNING;
		    pool.idleProtocol = null;
		    pool.notifyAll();
		}
	    }
	    releaseStoreProtocol(p);
	}
    }

    /*
     * If an IDLE command is in progress, abort it if necessary,
     * and wait until it completes.
     * ASSERT: Must be called with the pool's lock held.
     */
    private void waitIfIdle() throws ProtocolException {
	assert Thread.holdsLock(pool);
	while (pool.idleState != ConnectionPool.RUNNING) {
	    if (pool.idleState == ConnectionPool.IDLE) {
		pool.idleProtocol.idleAbort();
		pool.idleState = ConnectionPool.ABORTING;
	    }
	    try {
		// give up lock and wait to be not idle
		pool.wait();
	    } catch (InterruptedException ex) { }
	}
    }

    /**
     * Handle notifications and alerts.
     * Response must be an OK, NO, BAD, or BYE response.
     */
    void handleResponseCode(Response r) {
	String s = r.getRest();	// get the text after the response
	boolean isAlert = false;
	if (s.startsWith("[")) {	// a response code
	    int i = s.indexOf(']');
	    // remember if it's an alert
	    if (i > 0 && s.substring(0, i + 1).equalsIgnoreCase("[ALERT]"))
		isAlert = true;
	    // strip off the response code in any event
	    s = s.substring(i + 1).trim();
	}
	if (isAlert)
	    notifyStoreListeners(StoreEvent.ALERT, s);
	else if (r.isUnTagged() && s.length() > 0)
	    // Only send notifications that come with untagged
	    // responses, and only if there is actually some
	    // text there.
	    notifyStoreListeners(StoreEvent.NOTICE, s);
    }

    private String traceUser(String user) {
	return debugusername ? user : "<user name suppressed>";
    }

    private String tracePassword(String password) {
	return debugpassword ? password :
				(password == null ? "<null>" : "<non-null>");
    }
}
