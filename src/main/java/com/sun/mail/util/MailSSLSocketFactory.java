/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.mail.util;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

import javax.net.ssl.*;

/**
 * An SSL socket factory that makes it easier to specify trust.
 * This socket factory can be configured to trust all hosts or
 * trust a specific set of hosts, in which case the server's
 * certificate isn't verified.  Alternatively, a custom TrustManager
 * can be supplied. <p>
 *
 * An instance of this factory can be set as the value of the
 * <code>mail.&lt;protocol&gt;.ssl.socketFactory</code> property.
 *
 * @since	JavaMail 1.4.2
 * @author	Stephan Sann
 * @author	Bill Shannon
 */
public class MailSSLSocketFactory extends SSLSocketFactory {

    /** Should all hosts be trusted? */
    private boolean trustAllHosts;

    /** String-array of trusted hosts */
    private String[] trustedHosts = null;

    /** Holds a SSLContext to get SSLSocketFactories from */
    private SSLContext sslcontext;

    /** Holds the KeyManager array to use */
    private KeyManager[] keyManagers;

    /** Holds the TrustManager array to use */
    private TrustManager[] trustManagers;

    /** Holds the SecureRandom to use */
    private SecureRandom secureRandom;

    /** Holds a SSLSocketFactory to pass all API-method-calls to */
    private SSLSocketFactory adapteeFactory = null;

    /**
     * Initializes a new MailSSLSocketFactory.
     * 
     * @throws  GeneralSecurityException
     */
    public MailSSLSocketFactory() throws GeneralSecurityException {
	this("TLS");
    }

    /**
     * Initializes a new MailSSLSocketFactory with a given protocol.
     * Normally the protocol will be specified as "TLS".
     * 
     * @param   protocol  The protocol to use
     * @throws  NoSuchAlgorithmException if given protocol is not supported
     */
    public MailSSLSocketFactory(String protocol)
				throws GeneralSecurityException {

	// By default we do NOT trust all hosts.
	trustAllHosts = false;

	// Get an instance of an SSLContext.
	sslcontext = SSLContext.getInstance(protocol);

	// Default properties to init the SSLContext
	keyManagers = null;
	trustManagers = new TrustManager[] { new MailTrustManager() };
	secureRandom = null;

	// Assemble a default SSLSocketFactory to delegate all API-calls to.
	newAdapteeFactory();
    }


    /**
     * Gets an SSLSocketFactory based on the given (or default)
     * KeyManager array, TrustManager array and SecureRandom and
     * sets it to the instance var adapteeFactory.
     */
    private synchronized void newAdapteeFactory()
				throws KeyManagementException {
	sslcontext.init(keyManagers, trustManagers, secureRandom);

	// Get SocketFactory and save it in our instance var
	adapteeFactory = (SSLSocketFactory)sslcontext.getSocketFactory();
    }

    /**
     * @return the keyManagers
     */
    public synchronized KeyManager[] getKeyManagers() {
	return (KeyManager[])keyManagers.clone();
    }

    /**
     * @param keyManagers the keyManagers to set
     */
    public synchronized void setKeyManagers(KeyManager[] keyManagers)
				throws GeneralSecurityException  {
	this.keyManagers = (KeyManager[])keyManagers.clone();
	newAdapteeFactory();
    }

    /**
     * @return the secureRandom
     */
    public synchronized SecureRandom getSecureRandom() {
	return secureRandom;
    }

    /**
     * @param secureRandom the secureRandom to set
     */
    public synchronized void setSecureRandom(SecureRandom secureRandom)
				throws GeneralSecurityException  {
	this.secureRandom = secureRandom;
	newAdapteeFactory();
    }

    /**
     * @return the trustManagers
     */
    public synchronized TrustManager[] getTrustManagers() {
	return trustManagers;
    }

    /**
     * @param trustManagers the trustManagers to set
     */
    public synchronized void setTrustManagers(TrustManager[] trustManagers)
				throws GeneralSecurityException {
	this.trustManagers = trustManagers;
	newAdapteeFactory();
    }

    /**
     * @return	true if all hosts should be trusted
     */
    public synchronized boolean isTrustAllHosts() {
	return trustAllHosts;
    }

    /**
     * @param	trustAllHosts should all hosts be trusted?
     */
    public synchronized void setTrustAllHosts(boolean trustAllHosts) {
	this.trustAllHosts = trustAllHosts;
    }
    
    /**
     * @return	the trusted hosts
     */
    public synchronized String[] getTrustedHosts() {
	return (String[])trustedHosts.clone();
    }

    /**
     * @param	trustedHosts the hosts to trust
     */
    public synchronized void setTrustedHosts(String[] trustedHosts) {
	this.trustedHosts = (String[])trustedHosts.clone();
    }

    /**
     * After a successful conection to the server, this method is
     * called to ensure that the server should be trusted.
     * 
     * @param	server		name of the server we connected to
     * @param   sslSocket	SSLSocket connected to the server
     * @return  true  if "trustAllHosts" is set to true OR the server
     *		is contained in the "trustedHosts" array;
     */
    public synchronized boolean isServerTrusted(String server,
				SSLSocket sslSocket) {

	//System.out.println("DEBUG: isServerTrusted host " + server);

	// If "trustAllHosts" is set to true, we return true
	if (trustAllHosts)
	    return true;

	// If the socket host is contained in the "trustedHosts" array,
	// we return true
	if (trustedHosts != null)
	    return Arrays.asList(trustedHosts).contains(server); // ignore case?

	// If we get here, trust of the server was verified by the trust manager
	return true;
    }


    // SocketFactory methods

    /* (non-Javadoc)
     * @see javax.net.ssl.SSLSocketFactory#createSocket(java.net.Socket,
     *						java.lang.String, int, boolean)
     */
    //@Override
    public synchronized Socket createSocket(Socket socket, String s, int i,
				boolean flag) throws IOException {
	return adapteeFactory.createSocket(socket, s, i, flag);
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.SSLSocketFactory#getDefaultCipherSuites()
     */
    //@Override
    public synchronized String[] getDefaultCipherSuites() {
	return adapteeFactory.getDefaultCipherSuites();
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.SSLSocketFactory#getSupportedCipherSuites()
     */
    //@Override
    public synchronized String[] getSupportedCipherSuites() {
	return adapteeFactory.getSupportedCipherSuites();
    }

    /* (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket()
     */
    //@Override
    public synchronized Socket createSocket() throws IOException {
	return adapteeFactory.createSocket();
    }

    /* (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int,
     *						java.net.InetAddress, int)
     */
    //@Override
    public synchronized Socket createSocket(InetAddress inetaddress, int i,
			InetAddress inetaddress1, int j) throws IOException {
	return adapteeFactory.createSocket(inetaddress, i, inetaddress1, j);
    }

    /* (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
     */
    //@Override
    public synchronized Socket createSocket(InetAddress inetaddress, int i)
				throws IOException {
	return adapteeFactory.createSocket(inetaddress, i);
    }

    /* (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int,
     *						java.net.InetAddress, int)
     */
    //@Override
    public synchronized Socket createSocket(String s, int i,
				InetAddress inetaddress, int j)
				throws IOException, UnknownHostException {
	return adapteeFactory.createSocket(s, i, inetaddress, j);
    }

    /* (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
     */
    //@Override
    public synchronized Socket createSocket(String s, int i)
				throws IOException, UnknownHostException {
	return adapteeFactory.createSocket(s, i);
    }


    // inner classes

    /**
     * A default Trustmanager.
     * 
     * @author  Stephan Sann
     */
    private class MailTrustManager implements X509TrustManager {

	/** A TrustManager to pass method calls to */
	private X509TrustManager adapteeTrustManager = null;

	/**
	 * Initializes a new TrustManager instance.
	 */
	private MailTrustManager() throws GeneralSecurityException {
	    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
	    tmf.init((KeyStore)null);
	    adapteeTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#checkClientTrusted(
	 *		java.security.cert.X509Certificate[], java.lang.String)
	 */
	public void checkClientTrusted(X509Certificate[] certs, String authType)
					throws CertificateException {
	    if (!(isTrustAllHosts() || getTrustedHosts() != null))
		adapteeTrustManager.checkClientTrusted(certs, authType);
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#checkServerTrusted(
	 *		java.security.cert.X509Certificate[], java.lang.String)
	 */
	public void checkServerTrusted(X509Certificate[] certs, String authType)
					throws CertificateException {

	    if (!(isTrustAllHosts() || getTrustedHosts() != null))
		adapteeTrustManager.checkServerTrusted(certs, authType);
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
	 */
	public X509Certificate[] getAcceptedIssuers() {
	    return adapteeTrustManager.getAcceptedIssuers();
	}
    }
}
