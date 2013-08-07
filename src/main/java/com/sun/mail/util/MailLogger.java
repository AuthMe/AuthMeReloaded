/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.mail.Session;

/**
 * A simplified logger used by JavaMail to handle logging to a
 * PrintStream and logging through a java.util.logging.Logger.
 * If debug is set, messages are written to the PrintStream and
 * prefixed by the specified prefix (which is not included in
 * Logger messages).
 * Messages are logged by the Logger based on the configuration
 * of the logging system.
 */

/*
 * It would be so much simpler to just subclass Logger and override
 * the log(LogRecord) method, as the javadocs suggest, but that doesn't
 * work because Logger makes the decision about whether to log the message
 * or not before calling the log(LogRecord) method.  Instead, we just
 * provide the few log methods we need here.
 */

public final class MailLogger {
    private final Logger logger;	// for log messages
    private final String prefix;	// for debug output
    private final boolean debug;	// produce debug output?
    private final PrintStream out;	// stream for debug output

    /**
     * Construct a new MailLogger using the specified Logger name,
     * debug prefix (e.g., "DEBUG"), debug flag, and PrintStream.
     *
     * @param	name	the Logger name
     * @param	prefix	the prefix for debug output, or null for none
     * @param	debug	if true, write to PrintStream
     * @param	out	the PrintStream to write to
     */
    public MailLogger(String name, String prefix, boolean debug,
				PrintStream out) {
	logger = Logger.getLogger(name);
	this.prefix = prefix;
	this.debug = debug;
	this.out = out != null ? out : System.out;
    }

    /**
     * Construct a new MailLogger using the specified class' package
     * name as the Logger name,
     * debug prefix (e.g., "DEBUG"), debug flag, and PrintStream.
     *
     * @param	clazz	the Logger name is the package name of this class
     * @param	prefix	the prefix for debug output, or null for none
     * @param	debug	if true, write to PrintStream
     * @param	out	the PrintStream to write to
     */
    public MailLogger(Class clazz, String prefix, boolean debug,
				PrintStream out) {
	String name = packageOf(clazz);
	logger = Logger.getLogger(name);
	this.prefix = prefix;
	this.debug = debug;
	this.out = out != null ? out : System.out;
    }

    /**
     * Construct a new MailLogger using the specified class' package
     * name combined with the specified subname as the Logger name,
     * debug prefix (e.g., "DEBUG"), debug flag, and PrintStream.
     *
     * @param	clazz	the Logger name is the package name of this class
     * @param	subname	the Logger name relative to this Logger name
     * @param	prefix	the prefix for debug output, or null for none
     * @param	debug	if true, write to PrintStream
     * @param	out	the PrintStream to write to
     */
    public MailLogger(Class clazz, String subname, String prefix, boolean debug,
				PrintStream out) {
	String name = packageOf(clazz) + "." + subname;
	logger = Logger.getLogger(name);
	this.prefix = prefix;
	this.debug = debug;
	this.out = out != null ? out : System.out;
    }

    /**
     * Construct a new MailLogger using the specified Logger name and
     * debug prefix (e.g., "DEBUG").  Get the debug flag and PrintStream
     * from the Session.
     *
     * @param	name	the Logger name
     * @param	prefix	the prefix for debug output, or null for none
     * @param	session	where to get the debug flag and PrintStream
     */
    public MailLogger(String name, String prefix, Session session) {
	this(name, prefix, session.getDebug(), session.getDebugOut());
    }

    /**
     * Construct a new MailLogger using the specified class' package
     * name as the Logger name and the specified
     * debug prefix (e.g., "DEBUG").  Get the debug flag and PrintStream
     * from the Session.
     *
     * @param	clazz	the Logger name is the package name of this class
     * @param	prefix	the prefix for debug output, or null for none
     * @param	session	where to get the debug flag and PrintStream
     */
    public MailLogger(Class clazz, String prefix, Session session) {
	this(clazz, prefix, session.getDebug(), session.getDebugOut());
    }

    /**
     * Create a MailLogger that uses a Logger with the specified name
     * and prefix.  The new MailLogger uses the same debug flag and
     * PrintStream as this MailLogger.
     *
     * @param	name	the Logger name
     * @param	prefix	the prefix for debug output, or null for none
     */
    public MailLogger getLogger(String name, String prefix) {
	return new MailLogger(name, prefix, debug, out);
    }

    /**
     * Create a MailLogger using the specified class' package
     * name as the Logger name and the specified prefix.
     * The new MailLogger uses the same debug flag and
     * PrintStream as this MailLogger.
     *
     * @param	clazz	the Logger name is the package name of this class
     * @param	prefix	the prefix for debug output, or null for none
     */
    public MailLogger getLogger(Class clazz, String prefix) {
	return new MailLogger(clazz, prefix, debug, out);
    }

    /**
     * Create a MailLogger that uses a Logger whose name is composed
     * of this MailLogger's name plus the specified sub-name, separated
     * by a dot.  The new MailLogger uses the new prefix for debug output.
     * This is used primarily by the protocol trace code that wants a
     * different prefix (none).
     *
     * @param	subname	the Logger name relative to this Logger name
     * @param	prefix	the prefix for debug output, or null for none
     */
    public MailLogger getSubLogger(String subname, String prefix) {
	return new MailLogger(logger.getName() + "." + subname, prefix,
				debug, out);
    }

    /**
     * Create a MailLogger that uses a Logger whose name is composed
     * of this MailLogger's name plus the specified sub-name, separated
     * by a dot.  The new MailLogger uses the new prefix for debug output.
     * This is used primarily by the protocol trace code that wants a
     * different prefix (none).
     *
     * @param	subname	the Logger name relative to this Logger name
     * @param	prefix	the prefix for debug output, or null for none
     * @param	debug	the debug flag for the sub-logger
     */
    public MailLogger getSubLogger(String subname, String prefix,
				boolean debug) {
	return new MailLogger(logger.getName() + "." + subname, prefix,
				debug, out);
    }

    /**
     * Log the message at the specified level.
     */
    public void log(Level level, String msg) {
	ifDebugOut(msg);
	if (logger.isLoggable(level)) {
	    final String[] frame = inferCaller();
	    logger.logp(level, frame[0], frame[1], msg);
	}
    }

    /**
     * Log the message at the specified level.
     */
    public void log(Level level, String msg, Object param1) {
	if (debug) {
	    msg = MessageFormat.format(msg, new Object[] { param1 });
	    debugOut(msg);
	}
	
	if (logger.isLoggable(level)) {
	    final String[] frame = inferCaller();
	    logger.logp(level, frame[0], frame[1], msg, param1);
	}
    }

    /**
     * Log the message at the specified level.
     */
    public void log(Level level, String msg, Object params[]) {
	if (debug) {
	    msg = MessageFormat.format(msg, params);
	    debugOut(msg);
	}
	
	if (logger.isLoggable(level)) {
	    final String[] frame = inferCaller();
	    logger.logp(level, frame[0], frame[1], msg, params);
	}
    }

    /*
     * Maybe for JavaMail 1.5...
     *
    public void logf(Level level, String msg, Object... params) {
	msg = String.format(msg, params);
	ifDebugOut(msg);
	logger.log(level, msg);
    }
     */

    /**
     * Log the message at the specified level.
     */
    public void log(Level level, String msg, Throwable thrown) {
	if (debug) {
	    if (thrown != null) {
		debugOut(msg + ", THROW: ");
		thrown.printStackTrace(out);
	    } else {
		debugOut(msg);
	    }
	}
 
	if (logger.isLoggable(level)) {
	    final String[] frame = inferCaller();
	    logger.logp(level, frame[0], frame[1], msg, thrown);
	}
    }

    /**
     * Log a message at the CONFIG level.
     */
    public void config(String msg) {
	log(Level.CONFIG, msg);
    }

    /**
     * Log a message at the FINE level.
     */
    public void fine(String msg) {
	log(Level.FINE, msg);
    }

    /**
     * Log a message at the FINER level.
     */
    public void finer(String msg) {
	log(Level.FINER, msg);
    }

    /**
     * Log a message at the FINEST level.
     */
    public void finest(String msg) {
	log(Level.FINEST, msg);
    }

    /**
     * If "debug" is set, or our embedded Logger is loggable at the
     * given level, return true.
     */
    public boolean isLoggable(Level level) {
	return debug || logger.isLoggable(level);
    }

    private final void ifDebugOut(String msg) {
	if (debug)
	    debugOut(msg);
    }

    private final void debugOut(String msg) {
	if (prefix != null)
	    out.println(prefix + ": " + msg);
	else
	    out.println(msg);
    }

    /**
     * Return the package name of the class.
     * Sometimes there will be no Package object for the class,
     * e.g., if the class loader hasn't created one (see Class.getPackage()).
     */
    private String packageOf(Class clazz) {
	Package p = clazz.getPackage();
	if (p != null)
	    return p.getName();		// hopefully the common case
	String cname = clazz.getName();
	int i = cname.lastIndexOf('.');
	if (i > 0)
	    return cname.substring(0, i);
	// no package name, now what?
	return "";
    }

    /*
     * A disadvantage of not being able to use Logger directly in JavaMail
     * code is that the "source class" information that Logger guesses will
     * always refer to this class instead of our caller.  This method
     * duplicates what Logger does to try to find *our* caller, so that
     * Logger doesn't have to do it (and get the wrong answer), and because
     * our caller is what's wanted.
     */
    private String[] inferCaller() {
	// Get the stack trace.
	StackTraceElement stack[] = (new Throwable()).getStackTrace();
	// First, search back to a method in the Logger class.
	int ix = 0;
	while (ix < stack.length) {
	    StackTraceElement frame = stack[ix];
	    String cname = frame.getClassName();
	    if (isLoggerImplFrame(cname)) {
		break;
	    }
	    ix++;
	}
	// Now search for the first frame before the "Logger" class.
	while (ix < stack.length) {
	    StackTraceElement frame = stack[ix];
	    String cname = frame.getClassName();
	    if (!isLoggerImplFrame(cname)) {
		// We've found the relevant frame.
		return new String[]{cname, frame.getMethodName()};
	    }
	    ix++;
	}
	// We haven't found a suitable frame, so just punt.  This is
	// OK as we are only committed to making a "best effort" here.
	return new String[]{null, null};
    }
    
    private boolean isLoggerImplFrame(String cname) {
	return MailLogger.class.getName().equals(cname);
    }
}
