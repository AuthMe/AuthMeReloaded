/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2009-2013 Jason Mehrens. All rights reserved.
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

package com.sun.mail.util.logging;

import com.sun.mail.smtp.SMTPTransport;
import java.io.*;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;

/**
 * <tt>Handler</tt> that formats log records as an email message.
 *
 * <p>
 * This <tt>Handler</tt> will store a fixed number of log records used to
 * generate a single email message.  When the internal buffer reaches capacity,
 * all log records are formatted and placed in an email which is sent to an
 * email server.  The code to manually setup this handler can be as simple as
 * the following:
 *
 * <tt><pre>
 *      Properties props = new Properties();
 *      props.put("mail.smtp.host", "my-mail-server");
 *      props.put("mail.to", "me@example.com");
 *      props.put("verify", "local");
 *      MailHandler h = new MailHandler(props);
 *      h.setLevel(Level.WARNING);
 * </pre></tt>
 *
 * <p>
 * <b>Configuration:</b>
 * The LogManager should define at least one or more recipient addresses and a
 * mail host for outgoing email.  The code to setup this handler via the
 * logging properties can be as simple as the following:
 *
 * <tt><pre>
 *      #Default MailHandler settings.
 *      com.sun.mail.util.logging.MailHandler.mail.smtp.host = my-mail-server
 *      com.sun.mail.util.logging.MailHandler.mail.to = me@example.com
 *      com.sun.mail.util.logging.MailHandler.level = WARNING
 *      com.sun.mail.util.logging.MailHandler.verify = local
 * </pre></tt>
 *
 * For a custom handler, e.g. com.foo.MyHandler, the properties would be:
 * <tt><pre>
 *      #Subclass com.foo.MyHandler settings.
 *      com.foo.MyHandler.mail.smtp.host = my-mail-server
 *      com.foo.MyHandler.mail.to = me@example.com
 *      com.foo.MyHandler.level = WARNING
 *      com.foo.MyHandler.verify = local
 * </pre></tt>
 *
 * All mail properties documented in the <tt>Java Mail API</tt> cascade to the
 * LogManager by prefixing a key using the fully qualified class name of this
 * <tt>MailHandler</tt> or the fully qualified derived class name dot mail
 * property.  If the prefixed property is not found, then the mail property
 * itself is searched in the LogManager. By default each <tt>MailHandler</tt> is
 * initialized using the following LogManager configuration properties where
 * <tt><handler-name></tt> refers to the fully-qualified class name of the
 * handler.  If properties are not defined, or contain invalid values, then the
 * specified default values are used.
 *
 * <ul>
 * <li>&lt;handler-name&gt;.attachment.filters a comma
 * separated list of <tt>Filter</tt> class names used to create each attachment.
 * The literal <tt>null</tt> is reserved for attachments that do not require
 * filtering. (defaults to the
 * {@linkplain java.util.logging.Handler#getFilter() body} filter)
 *
 * <li>&lt;handler-name&gt;.attachment.formatters a comma
 * separated list of <tt>Formatter</tt> class names used to create each
 * attachment. (default is no attachments)
 *
 * <li>&lt;handler-name&gt;.attachment.names a comma separated
 * list of names or <tt>Formatter</tt> class names of each attachment.  The
 * attachment file names must not contain any line breaks.
 * (default is {@linkplain java.util.logging.Formatter#toString() toString}
 * of the attachment formatter)
 *
 * <li>&lt;handler-name&gt;.authenticator name of an
 * {@linkplain javax.mail.Authenticator} class used to provide login credentials
 * to the email server or string literal that is the password used with the
 * {@linkplain Authenticator#getDefaultUserName() default} user name.
 * (default is <tt>null</tt>)
 *
 * <li>&lt;handler-name&gt;.capacity the max number of
 * <tt>LogRecord</tt> objects include in each email message.
 * (defaults to <tt>1000</tt>)
 *
 * <li>&lt;handler-name&gt;.comparator name of a
 * {@linkplain java.util.Comparator} class used to sort the published
 * <tt>LogRecord</tt> objects prior to all formatting.
 * (defaults to <tt>null</tt> meaning records are unsorted).
 *
 * <li>&lt;handler-name&gt;.comparator.reverse a boolean
 * <tt>true</tt> to reverse the order of the specified comparator or
 * <tt>false</tt> to retain the original order. (defaults to <tt>false</tt>)
 *
 * <li>&lt;handler-name&gt;.encoding the name of the Java
 * {@linkplain java.nio.charset.Charset#name() character set} to use for the
 * email message. (defaults to <tt>null</tt>, the
 * {@linkplain javax.mail.internet.MimeUtility#getDefaultJavaCharset() default}
 * platform encoding).
 *
 * <li>&lt;handler-name&gt;.errorManager name of an
 * <tt>ErrorManager</tt> class used to handle any configuration or mail
 * transport problems. (defaults to <tt>java.util.logging.ErrorManager</tt>)
 *
 * <li>&lt;handler-name&gt;.filter name of a <tt>Filter</tt>
 * class used for the body of the message. (defaults to <tt>null</tt>,
 * allow all records)
 *
 * <li>&lt;handler-name&gt;.formatter name of a
 * <tt>Formatter</tt> class used to format the body of this message.
 * (defaults to <tt>java.util.logging.SimpleFormatter</tt>)
 *
 * <li>&lt;handler-name&gt;.level specifies the default level
 * for this <tt>Handler</tt> (defaults to <tt>Level.WARNING</tt>).
 *
 * <li>&lt;handler-name&gt;.mail.bcc a comma separated list of
 * addresses which will be blind carbon copied.  Typically, this is set to the
 * recipients that may need to be privately notified of a log message or
 * notified that a log message was sent to a third party such as a support team.
 * The empty string can be used to specify no blind carbon copied address.
 * (defaults to <tt>null</tt>, none)
 *
 * <li>&lt;handler-name&gt;.mail.cc a comma separated list of
 * addresses which will be carbon copied.  Typically, this is set to the
 * recipients that may need to be notified of a log message but, are not
 * required to provide direct support.  The empty string can be used to specify
 * no carbon copied address.  (defaults to <tt>null</tt>, none)
 *
 * <li>&lt;handler-name&gt;.mail.from a comma separated list of
 * addresses which will be from addresses. Typically, this is set to the email
 * address identifying the user running the application.  The empty string can
 * be used to override the default behavior and specify no from address.
 * (defaults to the {@linkplain javax.mail.Message#setFrom() local address})
 *
 * <li>&lt;handler-name&gt;.mail.host the host name or IP
 * address of the email server. (defaults to <tt>null</tt>, use
 * {@linkplain Transport#protocolConnect default}
 * <tt>Java Mail</tt> behavior)
 *
 * <li>&lt;handler-name&gt;.mail.reply.to a comma separated
 * list of addresses which will be reply-to addresses.  Typically, this is set
 * to the recipients that provide support for the application itself.  The empty
 * string can be used to specify no reply-to address.
 * (defaults to <tt>null</tt>, none)
 *
 * <li>&lt;handler-name&gt;.mail.to a comma separated list of
 * addresses which will be send-to addresses. Typically, this is set to the
 * recipients that provide support for the application, system, and/or
 * supporting infrastructure.  The empty string can be used to specify no
 * send-to address which overrides the default behavior.  (defaults to
 * {@linkplain javax.mail.internet.InternetAddress.getLocalAddress
 * local address}.)
 *
 * <li>&lt;handler-name&gt;.mail.sender a single address
 * identifying sender of the email; never equal to the from address.  Typically,
 * this is set to the email address identifying the application itself.  The
 * empty string can be used to specify no sender address.
 * (defaults to <tt>null</tt>, none)
 *
 * <li>&lt;handler-name&gt;.subject the name of a
 * <tt>Formatter</tt> class or string literal used to create the subject line.
 * The empty string can be used to specify no subject.  The subject line must
 * not contain any line breaks. (defaults to the empty string)
 *
 * <li>&lt;handler-name&gt;.pushFilter the name of a
 * <tt>Filter</tt> class used to trigger an early push.
 * (defaults to <tt>null</tt>, no early push)
 *
 * <li>&lt;handler-name&gt;.pushLevel the level which will
 * trigger an early push. (defaults to <tt>Level.OFF</tt>, only push when full)
 *
 * <li>&lt;handler-name&gt;.verify <a name="verify">used</a> to
 * verify the <tt>Handler</tt> configuration prior to a push.
 * <ul>
 *      <li>If the value is not set, equal to an empty string, or equal to the
 *      literal <tt>null</tt> then no settings are verified prior to a push.
 *      <li>If set to a value of <tt>limited</tt> then the <tt>Handler</tt> will
 *      verify minimal local machine settings.
 *      <li>If set to a value of <tt>local</tt> the <tt>Handler</tt> will verify
 *      all of settings of the local machine.
 *      <li>If set to a value of <tt>resolve</tt>, the <tt>Handler</tt> will
 *      verify all local settings and try to resolve the remote host name with
 *      the domain name server.
 *      <li>If set to a value of <tt>remote</tt>, the <tt>Handler</tt> will
 *      verify all local settings and try to establish a connection with the
 *      email server.
 * </ul>
 * If this <tt>Handler</tt> is only implicitly closed by the
 * <tt>LogManager</tt>, then verification should be turned on.
 * (defaults to <tt>null</tt>, no verify).
 *
 * <p>
 * <b>Normalization:</b>
 * The error manager, filters, and formatters when loaded from the LogManager
 * are converted into canonical form inside the MailHandler.  The pool of
 * interned values is limited to each MailHandler object such that no two
 * MailHandler objects created by the LogManager will be created sharing
 * identical error managers, filters, or formatters.  If a filter or formatter
 * should <b>not</b> be interned then it is recommended to retain the identity
 * equals and identity hashCode methods as the implementation.  For a filter or
 * formatter to be interned the class must implement the
 * {@linkplain java.lang.Object#equals(java.lang.Object) equals}
 * and {@linkplain java.lang.Object#hashCode() hashCode} methods.
 * The recommended code to use for stateless filters and formatters is:
 * <tt><pre>
 * public boolean equals(Object obj) {
 *     return obj == null ? false : obj.getClass() == getClass();
 * }
 *
 * public int hashCode() {
 *     return 31 * getClass().hashCode();
 * }
 * </pre></tt>
 *
 * <p>
 * <b>Sorting:</b>
 * All <tt>LogRecord</tt> objects are ordered prior to formatting if this
 * <tt>Handler</tt> has a non null comparator.  Developers might be interested
 * in sorting the formatted email by thread id, time, and sequence properties
 * of a <tt>LogRecord</tt>.  Where as system administrators might be interested
 * in sorting the formatted email by thrown, level, time, and sequence
 * properties of a <tt>LogRecord</tt>.  If comparator for this handler is
 * <tt>null</tt> then the order is unspecified.
 *
 * <p>
 * <b>Formatting:</b>
 * The main message body is formatted using the <tt>Formatter</tt> returned by
 * <tt>getFormatter()</tt>.  Only records that pass the filter returned by
 * <tt>getFilter()</tt> will be included in the message body.  The subject
 * <tt>Formatter</tt> will see all <tt>LogRecord</tt> objects that were
 * published regardless of the current <tt>Filter</tt>.  The MIME type of the
 * message body can be {@linkplain FileTypeMap#setDefaultFileTypeMap overridden}
 * by adding a MIME {@linkplain MimetypesFileTypeMap entry} using the simple
 * class name of the body formatter as the file extension.  The MIME type of the
 * attachments can be overridden by changing the attachment file name extension
 * or by editing the default MIME entry for a specific file name extension.
 *
 * <p>
 * <b>Attachments:</b>
 * This <tt>Handler</tt> allows multiple attachments per each email.
 * The attachment order maps directly to the array index order in this
 * <tt>Handler</tt> with zero index being the first attachment.  The number of
 * attachment formatters controls the number of attachments per email and
 * the content type of each attachment.  The attachment filters determine if a
 * <tt>LogRecord</tt> will be included in an attachment.  If an attachment
 * filter is <tt>null</tt> then all records are included for that attachment.
 * Attachments without content will be omitted from email message.  The
 * attachment name formatters create the file name for an attachment.
 * Custom attachment name formatters can be used to generate an attachment name
 * based on the contents of the attachment.
 *
 * <p>
 * <b>Push Level and Push Filter:</b>
 * The push method, push level, and optional push filter can be used to
 * conditionally trigger a push at or prior to full capacity.  When a push
 * occurs, the current buffer is formatted into an email and is sent to the
 * email server.  If the push method, push level, or push filter trigger a push
 * then the outgoing email is flagged as high importance with urgent priority.
 *
 * <p>
 * <b>Buffering:</b>
 * Log records that are published are stored in an internal buffer.  When this
 * buffer reaches capacity the existing records are formatted and sent in an
 * email.  Any published records can be sent before reaching capacity by
 * explictly calling the <tt>flush</tt>, <tt>push</tt>, or <tt>close</tt>
 * methods.  If a circular buffer is required then this handler can be wrapped
 * with a {@linkplain java.util.logging.MemoryHandler} typically with an
 * equivalent capacity, level, and push level.
 *
 * <p>
 * <b>Error Handling:</b>
 * If the transport of an email message fails, the email is converted to
 * a {@linkplain javax.mail.internet.MimeMessage#writeTo raw}
 * {@linkplain java.io.ByteArrayOutputStream#toString(java.lang.String) string}
 * and is then passed as the <tt>msg</tt> parameter to
 * {@linkplain Handler#reportError reportError} along with the exception
 * describing the cause of the failure.  This allows custom error managers to
 * store, {@linkplain javax.mail.internet.MimeMessage#MimeMessage(
 * javax.mail.Session, java.io.InputStream) reconstruct}, and resend the
 * original MimeMessage.  The message parameter string is <b>not</b> a raw email
 * if it starts with value returned from <tt>Level.SEVERE.getName()</tt>.
 * Custom error managers can use the following test to determine if the
 * <tt>msg</tt> parameter from this handler is a raw email:
 *
 * <tt><pre>
 * public void error(String msg, Exception ex, int code) {
 *      if (msg == null || msg.length() == 0 || msg.startsWith(Level.SEVERE.getName())) {
 *          super.error(msg, ex, code);
 *      } else {
 *          //The 'msg' parameter is a raw email.
 *      }
 * }
 * </pre></tt>
 *
 * @author Jason Mehrens
 * @since JavaMail 1.4.3
 */
public class MailHandler extends Handler {
    /**
     * Use the emptyFilterArray method.
     */
    private static final Filter[] EMPTY_FILTERS = new Filter[0];
    /**
     * Use the emptyFormatterArray method.
     */
    private static final Formatter[] EMPTY_FORMATTERS = new Formatter[0];
    /**
     * Min byte size for header data.  Used for initial arrays sizing.
     */
    private static final int MIN_HEADER_SIZE = 1024;
    /**
     * Cache the off value.
     */
    private static final int offValue = Level.OFF.intValue();
    /**
     * The action to get and set the context class loader.
     * Load this before it is loaded in the close method.
     */
    private static final GetAndSetContext GET_AND_SET_CCL =
            new GetAndSetContext(MailHandler.class);
    /**
     * A thread local mutex used to prevent logging loops.
     * The MUTEX has 3 states:
     * 1. MUTEX_RESET which is the null state.
     * 2. MUTEX_PUBLISH on first entry of a push or publish.
     * 3. MUTEX_REPORT when cycle of records is detected.
     */
    private static final ThreadLocal<Level> MUTEX = new ThreadLocal<Level>();
    /**
     * The marker object used to report a publishing state.
     */
    private static final Level MUTEX_PUBLISH = Level.ALL;
    /**
     * The marker object used to report a error reporting state.
     */
    private static final Level MUTEX_REPORT = Level.OFF;
    /**
     * Used to turn off security checks.
     */
    private volatile boolean sealed;
    /**
     * Determines if we are inside of a push.
     * Makes the handler properties read-only during a push.
     */
    private boolean isWriting;
    /**
     * Holds all of the email server properties.
     */
    private Properties mailProps;
    /**
     * Holds the authenticator required to login to the email server.
     */
    private Authenticator auth;
    /**
     * Holds the session object used to generate emails.
     * Sessions can be shared by multiple threads.
     * See BUGID 6228391
     */
    private Session session;
    /**
     * Holds all of the log records that will be used to create the email.
     */
    private LogRecord[] data;
    /**
     * The number of log records in the buffer.
     */
    private int size;
    /**
     * The maximum number of log records to format per email.
     * Used to roughly bound the size of an email.
     * Every time the capacity is reached, the handler will push.
     * The capacity will be negative if this handler is closed.
     * Negative values are used to ensure all records are pushed.
     */
    private int capacity;
    /**
     * Used to order all log records prior to formatting.  The main email body
     * and all attachments use the order determined by this comparator.  If no
     * comparator is present the log records will be in no specified order.
     */
    private Comparator<? super LogRecord> comparator;
    /**
     * Holds the formatter used to create the subject line of the email.
     * A subject formatter is not required for the email message.
     * All published records pass through the subject formatter.
     */
    private Formatter subjectFormatter;
    /**
     * Holds the push level for this handler.
     * This is only required if an email must be sent prior to shutdown
     * or before the buffer is full.
     */
    private Level pushLevel;
    /**
     * Holds the push filter for trigger conditions requiring an early push.
     * Only gets called if the given log record is greater than or equal
     * to the push level and the push level is not Level.OFF.
     */
    private Filter pushFilter;
    /**
     * Holds the filters for each attachment.  Filters are optional for
     * each attachment.
     */
    private Filter[] attachmentFilters;
    /**
     * Holds the formatters that create the content for each attachment.
     * Each formatter maps directly to an attachment.  The formatters
     * getHead, format, and getTail methods are only called if one or more
     * log records pass through the attachment filters.
     */
    private Formatter[] attachmentFormatters;
    /**
     * Holds the formatters that create the file name for each attachment.
     * Each formatter must produce a non null and non empty name.
     * The final file name will be the concatenation of one getHead call, plus
     * all of the format calls, plus one getTail call.
     */
    private Formatter[] attachmentNames;
    /**
     * Used to override the content type for the body and set the content type
     * for each attachment.
     */
    private FileTypeMap contentTypes;

    /**
     * Creates a <tt>MailHandler</tt> that is configured by the
     * <tt>LogManager</tt> configuration properties.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     */
    public MailHandler() {
        init((Properties) null);
        sealed = true;
    }

    /**
     * Creates a <tt>MailHandler</tt> that is configured by the
     * <tt>LogManager</tt> configuration properties but overrides the
     * <tt>LogManager</tt> capacity with the given capacity.
     * @param capacity of the internal buffer.
     * @throws IllegalArgumentException if <tt>capacity</tt> less than one.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     */
    public MailHandler(final int capacity) {
        init((Properties) null);
        sealed = true;
        setCapacity0(capacity);
    }

    /**
     * Creates a mail handler with the given mail properties.
     * The key/value pairs are defined in the <tt>Java Mail API</tt>
     * documentation.  This <tt>Handler</tt> will also search the
     * <tt>LogManager</tt> for defaults if needed.
     * @param props a non <tt>null</tt> properties object.
     * @throws NullPointerException if <tt>props</tt> is <tt>null</tt>.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     */
    public MailHandler(final Properties props) {
        if (props == null) {
            throw new NullPointerException();
        }
        init(props);
        sealed = true;
        setMailProperties0(props);
    }

    /**
     * Check if this <tt>Handler</tt> would actually log a given
     * <tt>LogRecord</tt> into its internal buffer.
     * <p>
     * This method checks if the <tt>LogRecord</tt> has an appropriate level and
     * whether it satisfies any <tt>Filter</tt> including any attachment filters.
     * However it does <b>not</b> check whether the <tt>LogRecord</tt> would
     * result in a "push" of the buffer contents.
     * <p>
     * @param record  a <tt>LogRecord</tt>
     * @return true if the <tt>LogRecord</tt> would be logged.
     */
    @Override
    public boolean isLoggable(final LogRecord record) {
        int levelValue = getLevel().intValue();
        if (record.getLevel().intValue() < levelValue || levelValue == offValue) {
            return false;
        }

        Filter body = getFilter();
        if (body == null || body.isLoggable(record)) {
            return true;
        }

        return isAttachmentLoggable(record);
    }

    /**
     * Stores a <tt>LogRecord</tt> in the internal buffer.
     * <p>
     * The <tt>isLoggable</tt> method is called to check if the given log record
     * is loggable. If the given record is loggable, it is copied into
     * an internal buffer.  Then the record's level property is compared with
     * the push level. If the given level of the <tt>LogRecord</tt>
     * is greater than or equal to the push level then the push filter is
     * called.  If no push filter exists, the push filter returns true,
     * or the capacity of the internal buffer has been reached then all buffered
     * records are formatted into one email and sent to the server.
     *
     * @param  record  description of the log event.
     */
    public void publish(final LogRecord record) {
        /**
         * It is possible for the handler to be closed after the
         * call to isLoggable.  In that case, the current thread
         * will push to ensure that all published records are sent.
         * See close().
         */
        if (tryMutex()) {
            try {
                if (isLoggable(record)) {
                    record.getSourceMethodName(); //Infer caller.
                    publish0(record);
                }
            } finally {
                releaseMutex();
            }
        } else {
            reportUnPublishedError(record);
        }
    }

    /**
     * Performs the publish after the record has been filtered.
     * @param record the record.
     * @since JavaMail 1.4.5
     */
    private void publish0(final LogRecord record) {
        Message msg;
        boolean priority;
        synchronized (this) {
            if (size == data.length && size < capacity) {
                grow();
            }

            if (size < data.length) {
                data[size] = record;
                ++size; //Be nice to client compiler.
                priority = isPushable(record);
                if (priority || size >= capacity) {
                    msg = writeLogRecords(ErrorManager.WRITE_FAILURE);
                } else {
                    msg = null;
                }
            } else {
                priority = false;
                msg = null;
            }
        }

        if (msg != null) {
            send(msg, priority, ErrorManager.WRITE_FAILURE);
        }
    }

    /**
     * Report to the error manager that a logging loop was detected and
     * we are going to break the cycle of messages.  It is possible that
     * a custom error manager could continue the cycle in which case
     * we will stop trying to report errors.
     * @param record the record or null.
     * @since JavaMail 1.4.6
     */
    private void reportUnPublishedError(LogRecord record) {
        if (MUTEX_PUBLISH.equals(MUTEX.get())) {
            MUTEX.set(MUTEX_REPORT);
            try {
                final String msg;
                if (record != null) {
                    final SimpleFormatter f = new SimpleFormatter();
                    msg = "Log record " + record.getSequenceNumber()
                                + " was not published. "
                                + head(f) + format(f, record) + tail(f, "");
                } else {
                    msg = null;
                }
                Exception e = new IllegalStateException(
                            "Recursive publish detected by thread "
                            + Thread.currentThread());
                reportError(msg, e, ErrorManager.WRITE_FAILURE);
           } finally {
                MUTEX.set(MUTEX_PUBLISH);
           }
        }
    }

    /**
     * Used to detect reentrance by the current thread to the publish method.
     * This mutex is thread local scope and will not block other threads.
     * The state is advanced on if the current thread is in a reset state.
     * @return true if the mutex was acquired.
     * @since JavaMail 1.4.6
     */
    private boolean tryMutex() {
        if (MUTEX.get() == null) {
            MUTEX.set(MUTEX_PUBLISH);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Releases the mutex held by the current thread.
     * This mutex is thread local scope and will not block other threads.
     * @since JavaMail 1.4.6
     */
    private void releaseMutex() {
        MUTEX.remove();
    }

    /**
     * Pushes any buffered records to the email server as high importance with
     * urgent priority.  The internal buffer is then cleared.  Does nothing if
     * called from inside a push.
     * @see #flush()
     */
    public void push() {
        push(true, ErrorManager.FLUSH_FAILURE);
    }

    /**
     * Pushes any buffered records to the email server as normal priority.
     * The internal buffer is then cleared.  Does nothing if called from inside
     * a push.
     * @see #push()
     */
    public void flush() {
        push(false, ErrorManager.FLUSH_FAILURE);
    }

    /**
     * Prevents any other records from being published.
     * Pushes any buffered records to the email server as normal priority.
     * The internal buffer is then cleared.  Once this handler is closed it
     * will remain closed.
     * <p>
     * If this <tt>Handler</tt> is only implicitly closed by the
     * <tt>LogManager</tt>, then <a href="#verify">verification</a> should be
     * turned on.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @see #flush()
     */
    public void close() {
        checkAccess(); //Security check first.
        //The LogManager$Cleaner has a context class loader set to null.
        //Set the CCL to this class loader for loading content handlers.
        final Object ccl = getAndSetContextClassLoader();
        try {
            Message msg = null;
            synchronized (this) {
                try {
                    msg = writeLogRecords(ErrorManager.CLOSE_FAILURE);
                } finally {
                    super.setLevel(Level.OFF); //Change level after formatting.
                   /**
                    * The sign bit of the capacity is set to ensure that records
                    * that have passed isLoggable, but have yet to be added to
                    * the internal buffer, are immediately pushed as an email.
                    */
                    if (this.capacity > 0) {
                        this.capacity = -this.capacity;
                    }

                    //Ensure not inside a push.
                    if (size == 0 && data.length != 1) {
                        this.data = new LogRecord[1];
                    }
                }
            }

            if (msg != null) {
                send(msg, false, ErrorManager.CLOSE_FAILURE);
            }
        } finally {
            setContextClassLoader(ccl);
        }
    }

    /**
     * Set the log level specifying which message levels will be
     * logged by this <tt>Handler</tt>.  Message levels lower than this
     * value will be discarded.
     * @param newLevel   the new value for the log level
     * @throws NullPointerException if <tt>newLevel</tt> is <tt>null</tt>.
     * @throws SecurityException  if a security manager exists and
     *          the caller does not have <tt>LoggingPermission("control")</tt>.
     */
    @Override
    public synchronized void setLevel(final Level newLevel) {
        if (this.capacity > 0) {
            super.setLevel(newLevel);
        } else { //Don't allow a closed handler to be opened (half way).
            if (newLevel == null) {
                throw new NullPointerException();
            }
            checkAccess();
        }
    }

    /**
     * Gets the push level.  The default is <tt>Level.OFF</tt> meaning that
     * this <tt>Handler</tt> will only push when the internal buffer is full.
     * @return the push level.
     */
    public final synchronized Level getPushLevel() {
        return this.pushLevel;
    }

    /**
     * Sets the push level.  This level is used to trigger a push so that
     * all pending records are formatted and sent to the email server.  When
     * the push level triggers a send, the resulting email is flagged as
     * high importance with urgent priority.
     * @param level Level object.
     * @throws NullPointerException if <tt>level</tt> is <tt>null</tt>.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws IllegalStateException if called from inside a push.
     */
    public final synchronized void setPushLevel(final Level level) {
        checkAccess();
        if (level == null) {
            throw new NullPointerException();
        }

        if (isWriting) {
            throw new IllegalStateException();
        }
        this.pushLevel = level;
    }

    /**
     * Gets the push filter.  The default is <tt>null</tt>.
     * @return the push filter or <tt>null</tt>.
     */
    public final synchronized Filter getPushFilter() {
        return this.pushFilter;
    }

    /**
     * Sets the push filter.  This filter is only called if the given
     * <tt>LogRecord</tt> level was greater than the push level.  If this
     * filter returns <tt>true</tt>, all pending records are formatted and sent
     * to the email server.  When the push filter triggers a send, the resulting
     * email is flagged as high importance with urgent priority.
     * @param filter push filter or <tt>null</tt>
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws IllegalStateException if called from inside a push.
     */
    public final synchronized void setPushFilter(final Filter filter) {
        checkAccess();
        if (isWriting) {
            throw new IllegalStateException();
        }
        this.pushFilter = filter;
    }

    /**
     * Gets the comparator used to order all <tt>LogRecord</tt> objects prior
     * to formatting.  If <tt>null</tt> then the order is unspecified.
     * @return the <tt>LogRecord</tt> comparator.
     */
    public final synchronized Comparator<? super LogRecord> getComparator() {
        return this.comparator;
    }

    /**
     * Sets the comparator used to order all <tt>LogRecord</tt> objects prior
     * to formatting.  If <tt>null</tt> then the order is unspecified.
     * @param c the <tt>LogRecord</tt> comparator.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws IllegalStateException if called from inside a push.
     */
    public final synchronized void setComparator(Comparator<? super LogRecord> c) {
        checkAccess();
        if (isWriting) {
            throw new IllegalStateException();
        }
        this.comparator = c;
    }

    /**
     * Gets the number of log records the internal buffer can hold.  When
     * capacity is reached, <tt>Handler</tt> will format all <tt>LogRecord</tt>
     * objects into one email message.
     * @return the capacity.
     */
    public final synchronized int getCapacity()  {
        assert capacity != Integer.MIN_VALUE && capacity != 0 : capacity;
        return Math.abs(capacity);
    }

    /**
     * Gets the <tt>Authenticator</tt> used to login to the email server.
     * @return an <tt>Authenticator</tt> or <tt>null</tt> if none is required.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     */
    public final synchronized Authenticator getAuthenticator() {
        checkAccess();
        return this.auth;
    }

    /**
     * Sets the <tt>Authenticator</tt> used to login to the email server.
     * @param auth an <tt>Authenticator</tt> object or null if none is required.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws IllegalStateException if called from inside a push.
     */
    public final void setAuthenticator(final Authenticator auth) {
        this.setAuthenticator0(auth);
    }

    /**
     * Sets the <tt>Authenticator</tt> used to login to the email server.
     * @param password a password or null if none is required.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws IllegalStateException if called from inside a push.
     * @see String#toCharArray()
     * @since JavaMail 1.4.6
     */
    public final void setAuthenticator(final char... password) {
        if (password == null) {
            setAuthenticator0((Authenticator) null);
        } else {
            setAuthenticator0(new DefaultAuthenticator(new String(password)));
        }
    }

    private void setAuthenticator0(final Authenticator auth) {
        checkAccess();

        Session settings;
        synchronized (this) {
            if (isWriting) {
                throw new IllegalStateException();
            }
            this.auth = auth;
            settings = fixUpSession();
        }
        verifySettings(settings);
    }

    /**
     * Sets the mail properties used for the session.  The key/value pairs
     * are defined in the <tt>Java Mail API</tt> documentation.  This
     * <tt>Handler</tt> will also search the <tt>LogManager</tt> for defaults
     * if needed.
     * @param props a non <tt>null</tt> properties object.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws NullPointerException if <tt>props</tt> is <tt>null</tt>.
     * @throws IllegalStateException if called from inside a push.
     */
    public final void setMailProperties(Properties props) {
        this.setMailProperties0(props);
    }

    private void setMailProperties0(Properties props) {
        checkAccess();
        props = (Properties) props.clone(); //Allow subclass.
        Session settings;
        synchronized (this) {
            if (isWriting) {
                throw new IllegalStateException();
            }
            this.mailProps = props;
            settings = fixUpSession();
        }
        verifySettings(settings);
    }

    /**
     * Gets a copy of the mail properties used for the session.
     * @return a non null properties object.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     */
    public final Properties getMailProperties() {
        checkAccess();
        final Properties props;
        synchronized (this) {
            props = this.mailProps;
        }
        return (Properties) props.clone();
    }

    /**
     * Gets the attachment filters.  If the attachment filter does not
     * allow any <tt>LogRecord</tt> to be formatted, the attachment may
     * be omitted from the email.
     * @return a non null array of attachment filters.
     */
    public final Filter[] getAttachmentFilters() {
        return readOnlyAttachmentFilters().clone();
    }

    /**
     * Sets the attachment filters.
     * @param filters a non <tt>null</tt> array of filters.  A <tt>null</tt>
     * index value is allowed.  A <tt>null</tt> value means that all
     * records are allowed for the attachment at that index.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws NullPointerException if <tt>filters</tt> is <tt>null</tt>
     * @throws IndexOutOfBoundsException if the number of attachment
     * name formatters do not match the number of attachment formatters.
     * @throws IllegalStateException if called from inside a push.
     */
    public final void setAttachmentFilters(Filter... filters) {
        checkAccess();
        filters = copyOf(filters, filters.length, Filter[].class);
        synchronized (this) {
            if (this.attachmentFormatters.length != filters.length) {
                throw attachmentMismatch(this.attachmentFormatters.length, filters.length);
            }

            if (isWriting) {
                throw new IllegalStateException();
            }
            this.attachmentFilters = filters;
        }
    }

    /**
     * Gets the attachment formatters.  This <tt>Handler</tt> is using
     * attachments only if the returned array length is non zero.
     * @return a non <tt>null</tt> array of formatters.
     */
    public final Formatter[] getAttachmentFormatters() {
        Formatter[] formatters;
        synchronized (this) {
            formatters = this.attachmentFormatters;
        }
        return formatters.clone();
    }

    /**
     * Sets the attachment <tt>Formatter</tt> object for this handler.
     * The number of formatters determines the number of attachments per
     * email.  This method should be the first attachment method called.
     * To remove all attachments, call this method with empty array.
     * @param formatters a non null array of formatters.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws NullPointerException if the given array or any array index is
     * <tt>null</tt>.
     * @throws IllegalStateException if called from inside a push.
     */
    public final void setAttachmentFormatters(Formatter... formatters) {
        checkAccess();
        if (formatters.length == 0) { //Null check and length check.
            formatters = emptyFormatterArray();
        } else {
            formatters = copyOf(formatters,
                    formatters.length, Formatter[].class);
            for (int i = 0; i < formatters.length; ++i) {
                if (formatters[i] == null) {
                    throw new NullPointerException(atIndexMsg(i));
                }
            }
        }

        synchronized (this) {
            if (isWriting) {
                throw new IllegalStateException();
            }

            this.attachmentFormatters = formatters;
            this.fixUpAttachmentFilters();
            this.fixUpAttachmentNames();
        }
    }

    /**
     * Gets the attachment name formatters.
     * If the attachment names were set using explicit names then
     * the names can be returned by calling <tt>toString</tt> on each
     * attachment name formatter.
     * @return non <tt>null</tt> array of attachment name formatters.
     */
    public final Formatter[] getAttachmentNames() {
        final Formatter[] formatters;
        synchronized (this) {
            formatters = this.attachmentNames;
        }
        return formatters.clone();
    }

    /**
     * Sets the attachment file name for each attachment.  The caller must
     * ensure that the attachment file names do not contain any line breaks.
     * This method will create a set of custom formatters.
     * @param names an array of names.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws IndexOutOfBoundsException if the number of attachment
     * names do not match the number of attachment formatters.
     * @throws IllegalArgumentException  if any name is empty.
     * @throws NullPointerException if any given array or name is <tt>null</tt>.
     * @throws IllegalStateException if called from inside a push.
     * @see Character#isISOControl(char)
     * @see Character#isISOControl(int)
     */
    public final void setAttachmentNames(final String... names) {
        checkAccess();

        final Formatter[] formatters;
        if (names.length == 0) {
            formatters = emptyFormatterArray();
        } else {
            formatters = new Formatter[names.length];
        }

        for (int i = 0; i < names.length; ++i) {
            final String name = names[i];
            if (name != null) {
                if (name.length() > 0) {
                    formatters[i] = new TailNameFormatter(name);
                } else {
                    throw new IllegalArgumentException(atIndexMsg(i));
                }
            } else {
                throw new NullPointerException(atIndexMsg(i));
            }
        }

        synchronized (this) {
            if (this.attachmentFormatters.length != names.length) {
                throw attachmentMismatch(this.attachmentFormatters.length, names.length);
            }

            if (isWriting) {
                throw new IllegalStateException();
            }
            this.attachmentNames = formatters;
        }
    }

    /**
     * Sets the attachment file name formatters.  The format method of each
     * attachment formatter will see only the <tt>LogRecord</tt> objects that
     * passed its attachment filter during formatting. The format method will
     * typically return an empty string. Instead of being used to format
     * records, it is used to gather information about the contents of an
     * attachment.  The <tt>getTail</tt> method should be used to construct the
     * attachment file name and reset any formatter collected state.  The
     * formatter must ensure that the attachment file name does not contain any
     * line breaks.  The <tt>toString</tt> method of the given formatter should
     * be overridden to provide a useful attachment file name, if possible.
     * @param formatters and array of attachment name formatters.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws IndexOutOfBoundsException if the number of attachment
     * name formatters do not match the number of attachment formatters.
     * @throws NullPointerException if any given array or name is <tt>null</tt>.
     * @throws IllegalStateException if called from inside a push.
     * @see Character#isISOControl(char)
     * @see Character#isISOControl(int)
     */
    public final void setAttachmentNames(Formatter... formatters) {
        checkAccess();

        formatters = copyOf(formatters, formatters.length, Formatter[].class);
        for (int i = 0; i < formatters.length; ++i) {
            if (formatters[i] == null) {
                throw new NullPointerException(atIndexMsg(i));
            }
        }

        synchronized (this) {
            if (this.attachmentFormatters.length != formatters.length) {
                throw attachmentMismatch(this.attachmentFormatters.length, formatters.length);
            }

            if (isWriting) {
                throw new IllegalStateException();
            }

            this.attachmentNames = formatters;
        }
    }

    /**
     * Gets the formatter used to create the subject line.
     * If the subject was created using a literal string then
     * the <tt>toString</tt> method can be used to get the subject line.
     * @return the formatter.
     */
    public final synchronized Formatter getSubject() {
        return this.subjectFormatter;
    }

    /**
     * Sets a literal string for the email subject.  The caller must ensure that
     * the subject line does not contain any line breaks.
     * @param subject a non <tt>null</tt> string.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws NullPointerException if <tt>subject</tt> is <tt>null</tt>.
     * @throws IllegalStateException if called from inside a push.
     * @see Character#isISOControl(char)
     * @see Character#isISOControl(int)
     */
    public final void setSubject(final String subject) {
        if (subject != null) {
            this.setSubject(new TailNameFormatter(subject));
        } else {
            checkAccess();
            throw new NullPointerException();
        }
    }

    /**
     * Sets the subject formatter for email.  The format method of the subject
     * formatter will see all <tt>LogRecord</tt> objects that were published to
     * this <tt>Handler</tt> during formatting and will typically return an
     * empty string.  This formatter is used to gather information to create a
     * summary about what information is contained in the email.  The
     * <tt>getTail</tt> method should be used to construct the subject and reset
     * any formatter collected state.  The formatter must ensure that the
     * subject line does not contain any line breaks.  The <tt>toString</tt>
     * method of the given formatter should be overridden to provide a useful
     * subject, if possible.
     * @param format the subject formatter.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     * @throws NullPointerException if <tt>format</tt> is <tt>null</tt>.
     * @throws IllegalStateException if called from inside a push.
     * @see Character#isISOControl(char)
     * @see Character#isISOControl(int)
     */
    public final void setSubject(final Formatter format) {
        checkAccess();
        if (format == null) {
            throw new NullPointerException();
        }

        synchronized (this) {
            if (isWriting) {
                throw new IllegalStateException();
            }
            this.subjectFormatter = format;
        }
    }

    /**
     * Protected convenience method to report an error to this Handler's
     * ErrorManager.  This method will prefix all non null error messages with
     * <tt>Level.SEVERE.getName()</tt>.  This allows the receiving error
     * manager to determine if the <tt>msg</tt> parameter is a simple error
     * message or a raw email message.
     * @param msg    a descriptive string (may be null)
     * @param ex     an exception (may be null)
     * @param code   an error code defined in ErrorManager
     */
    @Override
    protected void reportError(String msg, Exception ex, int code) {
        if (msg != null) {
            super.reportError(Level.SEVERE.getName() + ": " + msg, ex, code);
        } else {
            super.reportError(null, ex, code);
        }
    }

    /**
     * Calls log manager checkAccess if this is sealed.
     */
    final void checkAccess() {
        if (sealed) {
            LogManagerProperties.getLogManager().checkAccess();
        }
    }

    /**
     * Determines the mimeType of a formatter from the getHead call.
     * This could be made protected, or a new class could be created to do
     * this type of conversion.  Currently, this is only used for the body
     * since the attachments are computed by filename.
     * Package-private for unit testing.
     * @param head any head string.
     * @return return the mime type or null for text/plain.
     */
    final String contentTypeOf(String head) {
        if (!isEmpty(head)) {
            final int MAX_CHARS = 25;
            if (head.length() > MAX_CHARS) {
                head = head.substring(0, MAX_CHARS);
            }
            try {
                final String encoding = getEncodingName();
                final ByteArrayInputStream in
                        = new ByteArrayInputStream(head.getBytes(encoding));

                assert in.markSupported() : in.getClass().getName();
                return URLConnection.guessContentTypeFromStream(in);
            } catch (final IOException IOE) {
                reportError(IOE.getMessage(), IOE, ErrorManager.FORMAT_FAILURE);
            }
        }
        return null; //text/plain
    }

    /**
     * Determines if the given throwable is a no content exception.
     * Package-private for unit testing.
     * @param msg the message without content.
     * @param t the throwable to test.
     * @return true if the throwable is a missing content exception.
     * @throws NullPointerException if any of the arguments are null.
     * @since JavaMail 1.4.5
     */
    final boolean isMissingContent(Message msg, Throwable t) {
        for (Throwable cause = t.getCause(); cause != null;) {
            t = cause;
            cause = cause.getCause();
        }

        try {
            msg.writeTo(new ByteArrayOutputStream(MIN_HEADER_SIZE));
        } catch (final RuntimeException RE) {
            throw RE; //Avoid catch all.
        } catch (final Exception noContent) {
            final String txt = noContent.getMessage();
            if (!isEmpty(txt) && noContent.getClass() == t.getClass()) {
               return txt.equals(t.getMessage());
            }
        }
        return false;
    }

    /**
     * Converts a mime message to a raw string or formats the reason
     * why message can't be changed to raw string and reports it.
     * @param msg the mime message.
     * @param ex the original exception.
     * @param code the ErrorManager code.
     * @since JavaMail 1.4.5
     */
    private void reportError(Message msg, Exception ex, int code) {
        try { //Use super call so we do not prefix raw email.
            super.reportError(toRawString(msg), ex, code);
        } catch (final MessagingException rawMe) {
            reportError(toMsgString(rawMe), ex, code);
        } catch (final IOException rawIo) {
            reportError(toMsgString(rawIo), ex, code);
        }
    }

    /**
     * Determines the mimeType from the given file name.
     * Used to override the body content type and used for all attachments.
     * @param name the file name or class name.
     * @return the mime type or null for text/plain.
     */
    private String getContentType(final String name) {
        assert Thread.holdsLock(this);
        final String type = contentTypes.getContentType(name);
        if ("application/octet-stream".equalsIgnoreCase(type)) {
            return null; //Formatters return strings, default to text/plain.
        }
        return type;
    }

    /**
     * Gets the encoding set for this handler, mime encoding, or file encoding.
     * @return the java charset name, never null.
     * @since JavaMail 1.4.5
     */
    private String getEncodingName() {
        String encoding = getEncoding();
        if (encoding == null) {
            encoding = MimeUtility.getDefaultJavaCharset();
        }
        return encoding;
    }

    /**
     * Set the content for a part using the encoding assigned to the handler.
     * @param part the part to assign.
     * @param buf the formatted data.
     * @param type the mime type.
     * @throws MessagingException if there is a problem.
     */
    private void setContent(MimeBodyPart part, CharSequence buf, String type) throws MessagingException {
        final String encoding = getEncodingName();
        if (type != null && !"text/plain".equalsIgnoreCase(type)) {
            type = contentWithEncoding(type, encoding);
            try {
                DataSource source = new ByteArrayDataSource(buf.toString(), type);
                part.setDataHandler(new DataHandler(source));
            } catch (final IOException IOE) {
                reportError(IOE.getMessage(), IOE, ErrorManager.FORMAT_FAILURE);
                part.setText(buf.toString(), encoding);
            }
        } else {
            part.setText(buf.toString(), MimeUtility.mimeCharset(encoding));
        }
    }

    /**
     * Replaces the charset parameter with the current encoding.
     * @param type the content type.
     * @param encoding the java charset name.
     * @return the type with a specified encoding.
     */
    private String contentWithEncoding(String type, String encoding) {
        assert encoding != null;
        try {
            final ContentType ct = new ContentType(type);
            ct.setParameter("charset", MimeUtility.mimeCharset(encoding));
            encoding = ct.toString(); //See javax.mail.internet.ContentType.
            if (!isEmpty(encoding)) {
                type = encoding;
            }
        } catch (final MessagingException ME) {
            reportError(type, ME, ErrorManager.FORMAT_FAILURE);
        }
        return type;
    }

    /**
     * Sets the capacity for this handler.  This method is kept private
     * because we would have to define a public policy for when the size is
     * greater than the capacity.
     * I.E. do nothing, flush now, truncate now, push now and resize.
     * @param newCapacity the max number of records.
     * @throws IllegalStateException if called from inside a push.
     */
    private synchronized void setCapacity0(final int newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero.");
        }

        if (isWriting) {
            throw new IllegalStateException();
        }

        if (this.capacity < 0) { //If closed, remain closed.
            this.capacity = -newCapacity;
        } else {
            this.capacity = newCapacity;
        }
    }

    /**
     * Gets the attachment filters under a lock.  The attachment filters
     * are treated as copy-on-write, so the returned array must never be
     * modified or published outside this class.
     * @return a read only array of filters.
     */
    private synchronized Filter[] readOnlyAttachmentFilters() {
        return this.attachmentFilters;
    }

    /**
     * Factory for empty formatter arrays.
     * @return an empty array.
     */
    private static Formatter[] emptyFormatterArray() {
        return EMPTY_FORMATTERS;
    }

    /**
     * Factory for empty filter arrays.
     * @return an empty array.
     */
    private static Filter[] emptyFilterArray() {
        return EMPTY_FILTERS;
    }

    /**
     * Expand or shrink the attachment name formatters.
     * @return true if fixed.
     */
    private boolean fixUpAttachmentNames() {
        assert Thread.holdsLock(this);
        boolean fixed = false;
        final int expect = this.attachmentFormatters.length;
        final int current = this.attachmentNames.length;
        if (current != expect) {
            this.attachmentNames = copyOf(attachmentNames, expect);
            fixed = current != 0;
        }

        //Copy of zero length array is cheap, warm up copyOf.
        if (expect == 0) {
            this.attachmentNames = emptyFormatterArray();
            assert this.attachmentNames.length == 0;
        } else {
            for (int i = 0; i < expect; ++i) {
                if (this.attachmentNames[i] == null) {
                    this.attachmentNames[i] = new TailNameFormatter(
                            toString(this.attachmentFormatters[i]));
                }
            }
        }
        return fixed;
    }

    /**
     * Expand or shrink the attachment filters.
     * @return true if fixed.
     */
    private boolean fixUpAttachmentFilters() {
        assert Thread.holdsLock(this);

        boolean fixed = false;
        final int expect = this.attachmentFormatters.length;
        final int current = this.attachmentFilters.length;
        if (current != expect) {
            this.attachmentFilters = copyOf(attachmentFilters, expect);
            fixed = current != 0;

            //Array elements default to null so skip filling if body filter
            //is null.  If not null then only assign to expanded elements.
            final Filter body = super.getFilter();
            if (body != null) {
                for (int i = current; i < expect; ++i) {
                    this.attachmentFilters[i] = body;
                }
            }
        }

        //Copy of zero length array is cheap, warm up copyOf.
        if (expect == 0) {
           this.attachmentFilters = emptyFilterArray();
           assert this.attachmentFilters.length == 0;
        }
        return fixed;
    }

    /**
     * Copies the given array. Can be removed when Java Mail requires Java 1.6.
     * @param a the original array.
     * @param len the new size.
     * @return new copy
     */
    private static <T> T[] copyOf(final T[] a, final int len) {
        return (T[]) copyOf(a, len, a.getClass());
    }

    /**
     * Copies the given array to a new array type.
     * Can be removed when Java Mail requires Java 1.6.
     * @param a the original array.
     * @param len the new size.
     * @param type the array type.
     * @return new copy
     */
    private static <T,U> T[] copyOf(U[] a, int len, Class<? extends T[]> type) {
        final T[] copy = (T[]) Array.newInstance(type.getComponentType(), len);
        System.arraycopy(a, 0, copy, 0, Math.min(len, a.length));
        return copy;
    }

    /**
     * Sets the size to zero and clears the current buffer.
     */
    private void reset() {
        assert Thread.holdsLock(this);
        if (size < data.length) {
            Arrays.fill(data, 0, size, null);
        } else {
            Arrays.fill(data, null);
        }
        this.size = 0;
    }

    /**
     * Expands the internal buffer up to the capacity.
     */
    private void grow() {
        assert Thread.holdsLock(this);
        final int len = data.length;
        int newCapacity = len + (len >> 1) + 1;
        if (newCapacity > capacity || newCapacity < len) {
            newCapacity = capacity;
        }
        assert len != capacity : len;
        this.data = copyOf(data, newCapacity);
    }

    /**
     * Configures the handler properties from the log manager.
     * @param props the given mail properties.  Maybe null and are never
     * captured by this handler.
     * @throws SecurityException  if a security manager exists and the
     * caller does not have <tt>LoggingPermission("control")</tt>.
     */
    private synchronized void init(final Properties props) {
        final LogManager manager = LogManagerProperties.getLogManager();
        final String p = getClass().getName();
        this.mailProps = new Properties(); //See method comments.
        this.contentTypes = FileTypeMap.getDefaultFileTypeMap();

        //Assign any custom error manager first so it can detect all failures.
        initErrorManager(manager, p);

        initLevel(manager, p);
        initFilter(manager, p);
        initCapacity(manager, p);
        initAuthenticator(manager, p);

        initEncoding(manager, p);
        initFormatter(manager, p);
        initComparator(manager, p);
        initPushLevel(manager, p);
        initPushFilter(manager, p);

        initSubject(manager, p);

        initAttachmentFormaters(manager, p);
        initAttachmentFilters(manager, p);
        initAttachmentNames(manager, p);

        if (props == null && manager.getProperty(p.concat(".verify")) != null) {
            verifySettings(initSession());
        }
        intern(); //Show verify warnings first.
    }

    /**
     * Interns the error manager, formatters, and filters contained in this
     * handler.  The comparator is not interned.  This method can only be
     * called from init after all of formatters and filters are in a constructed
     * and in a consistent state.
     * @since JavaMail 1.5.0
     */
    private void intern() {
        assert Thread.holdsLock(this);
        try {
            Object canidate;
            Object result;
            final Map<Object, Object> seen = new HashMap<Object, Object>();
            try {
                intern(seen, super.getErrorManager());
            } catch (final SecurityException se) {
                reportError(se.getMessage(), se, ErrorManager.OPEN_FAILURE);
            }

            try {
                canidate = super.getFilter();
                result = intern(seen, canidate);
                if (result != canidate) {
                    super.setFilter(Filter.class.cast(result));
                }

                canidate = super.getFormatter();
                result = intern(seen, canidate);
                if (result != canidate) {
                    super.setFormatter(Formatter.class.cast(result));
                }
            } catch (final SecurityException se) {
                reportError(se.getMessage(), se, ErrorManager.OPEN_FAILURE);
            }

            canidate = this.subjectFormatter;
            result = intern(seen, canidate);
            if (result != canidate) {
                this.subjectFormatter = Formatter.class.cast(result);
            }

            canidate = this.pushFilter;
            result = intern(seen, canidate);
            if (result != canidate) {
                this.pushFilter = Filter.class.cast(result);
            }

            for (int i = 0; i < attachmentFormatters.length; ++i) {
                canidate = attachmentFormatters[i];
                result = intern(seen, canidate);
                if (result != canidate) {
                    attachmentFormatters[i] = Formatter.class.cast(result);
                }

                canidate = attachmentFilters[i];
                result = intern(seen, canidate);
                if (result != canidate) {
                    attachmentFilters[i] = Filter.class.cast(result);
                }

                canidate = attachmentNames[i];
                result = intern(seen, canidate);
                if (result != canidate) {
                    attachmentNames[i] = Formatter.class.cast(result);
                }
            }
        } catch (final Exception skip) {
            reportError(skip.getMessage(), skip, ErrorManager.OPEN_FAILURE);
        }
    }

    /**
     * If possible performs an intern of the given object into the
     * map.  If the object can not be interned the given object is returned.
     * @param m the map used to record the interned values.
     * @param o the object to try an intern.
     * @return the original object or an intern replacement.
     * @throws SecurityException if this operation is not allowed by the
     * security manager.
     * @throws Exception if there is an unexpected problem.
     * @since JavaMail 1.5.0
     */
    private Object intern(Map<Object, Object> m, Object o) throws Exception {
        if (o == null) {
            return null;
        }

        /**
         * The common case is that most objects will not intern.  The given
         * object has a public no argument constructor or is an instance of a
         * TailNameFormatter.  TailNameFormatter is safe use as a map key.
         * For everything else we create a clone of the given object.
         * This is done because of the following:
         * 1. Clones can be used to test that a class provides an equals method
         * and that the equals method works correctly.
         * 2. Calling equals on the given object is assumed to be cheap.
         * 3. The intern map can be filtered so it only contains objects that
         * can be interned, which reduces the memory footprint.
         * 4. Clones are method local garbage.
         * 5. Hash code is only called on the clones so bias locking is not
         * disabled on the objects the handler will use.
         */
        final Object key;
        if (o.getClass().getName().equals(TailNameFormatter.class.getName())) {
            key = o;
        } else {
            //This call was already made in the LogManagerProperties so this
            //shouldn't trigger loading of any lazy reflection code.
            key = o.getClass().getConstructor().newInstance();
        }

        final Object use;
        //Check the classloaders of each object avoiding the security manager.
        if (key.getClass() == o.getClass()) {
            Object found = m.get(key); //Transitive equals test.
            if (found == null) {
                //Ensure that equals is symmetric to prove intern is safe.
                final boolean right = key.equals(o);
                final boolean left = o.equals(key);
                if (right && left) {
                    //Assume hashCode is defined at this point.
                    found = m.put(o, o);
                    if (found != null) {
                        reportNonDiscriminating(key, found);
                        found = m.remove(key);
                        if (found != o) {
                            reportNonDiscriminating(key, found);
                            m.clear(); //Try to restore order.
                        }
                    }
                } else {
                    if (right != left) {
                        reportNonSymmetric(o, key);
                    }
                }
                use = o;
            } else {
                //Check for a discriminating equals method.
                if (o.getClass() == found.getClass()) {
                    use = found;
                } else {
                    reportNonDiscriminating(o, found);
                    use = o;
                }
            }
        } else {
            use = o;
        }
        return use;
    }

    /**
     * Checks a string value for null or empty.
     * @param s the string.
     * @return true if the given string is null or zero length.
     */
    private static boolean isEmpty(final String s) {
        return s == null || s.length() == 0;
    }

    /**
     * Checks that a string is not empty and not equal to the literal "null".
     * @param name the string to check for a value.
     * @return true if the string has a valid value.
     */
    private static boolean hasValue(final String name) {
        return !isEmpty(name) && !"null".equalsIgnoreCase(name);
    }

    private void initAttachmentFilters(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        assert this.attachmentFormatters != null;
        final String list = manager.getProperty(p.concat(".attachment.filters"));
        if (!isEmpty(list)) {
            final String[] names = list.split(",");
            Filter[] a = new Filter[names.length];
            for (int i = 0; i < a.length; ++i) {
                names[i] = names[i].trim();
                if (!"null".equalsIgnoreCase(names[i])) {
                    try {
                        a[i] = LogManagerProperties.newFilter(names[i]);
                    } catch (final SecurityException SE) {
                        throw SE; //Avoid catch all.
                    } catch (final Exception E) {
                        reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
                    }
                }
            }

            this.attachmentFilters = a;
            if (fixUpAttachmentFilters()) {
               reportError("Attachment filters.",
                    attachmentMismatch("Length mismatch."), ErrorManager.OPEN_FAILURE);
            }
        } else {
            this.attachmentFilters = emptyFilterArray();
            fixUpAttachmentFilters();
        }
    }

    private void initAttachmentFormaters(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        final String list = manager.getProperty(p.concat(".attachment.formatters"));
        if (!isEmpty(list)) {
            final Formatter[] a;
            final String[] names = list.split(",");
            if (names.length == 0) {
                a = emptyFormatterArray();
            } else {
                a = new Formatter[names.length];
            }

            for (int i = 0; i < a.length; ++i) {
                names[i] = names[i].trim();
                if (!"null".equalsIgnoreCase(names[i])) {
                    try {
                        a[i] = LogManagerProperties.newFormatter(names[i]);
                        if (a[i] instanceof TailNameFormatter) {
                            final Exception CNFE = new ClassNotFoundException(a[i].toString());
                            reportError("Attachment formatter.", CNFE, ErrorManager.OPEN_FAILURE);
                            a[i] = new SimpleFormatter();
                        }
                    } catch (final SecurityException SE) {
                        throw SE; //Avoid catch all.
                    } catch (final Exception E) {
                        reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
                        a[i] = new SimpleFormatter();
                    }
                } else {
                    final Exception NPE = new NullPointerException(atIndexMsg(i));
                    reportError("Attachment formatter.", NPE, ErrorManager.OPEN_FAILURE);
                    a[i] = new SimpleFormatter();
                }
            }

            this.attachmentFormatters = a;
        } else {
            this.attachmentFormatters = emptyFormatterArray();
        }
    }

    private void initAttachmentNames(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        assert this.attachmentFormatters != null;

        final String list = manager.getProperty(p.concat(".attachment.names"));
        if (!isEmpty(list)) {
            final String[] names = list.split(",");
            final Formatter[] a = new Formatter[names.length];
            for (int i = 0; i < a.length; ++i) {
                names[i] = names[i].trim();
                if (!"null".equalsIgnoreCase(names[i])) {
                    try {
                        try {
                            a[i] = LogManagerProperties.newFormatter(names[i]);
                        } catch (final ClassNotFoundException literal) {
                            a[i] = new TailNameFormatter(names[i]);
                        } catch (final ClassCastException literal) {
                            a[i] = new TailNameFormatter(names[i]);
                        }
                    } catch (final SecurityException SE) {
                        throw SE; //Avoid catch all.
                    } catch (final Exception E) {
                        reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
                    }
                } else {
                    final Exception NPE = new NullPointerException(atIndexMsg(i));
                    reportError("Attachment names.", NPE, ErrorManager.OPEN_FAILURE);
                }
            }

            this.attachmentNames = a;
            if (fixUpAttachmentNames()) { //Any null indexes are repaired.
               reportError("Attachment names.",
                    attachmentMismatch("Length mismatch."), ErrorManager.OPEN_FAILURE);
            }
        } else {
            this.attachmentNames = emptyFormatterArray();
            fixUpAttachmentNames();
        }
    }

    private void initAuthenticator(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        String name = manager.getProperty(p.concat(".authenticator"));
        if (hasValue(name)) {
            try {
                this.auth = LogManagerProperties.newAuthenticator(name);
            } catch (final SecurityException SE) {
                throw SE;
            } catch (final ClassNotFoundException literalAuth) {
                this.auth = new DefaultAuthenticator(name);
            } catch (final ClassCastException literalAuth) {
                this.auth = new DefaultAuthenticator(name);
            } catch (final Exception E) {
                reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
            }
        }
    }

    private void initLevel(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        try {
            final String val = manager.getProperty(p.concat(".level"));
            if (val != null) {
                super.setLevel(Level.parse(val));
            } else {
                super.setLevel(Level.WARNING);
            }
        } catch (final SecurityException SE) {
            throw SE; //Avoid catch all.
        } catch (final RuntimeException RE) {
            reportError(RE.getMessage(), RE, ErrorManager.OPEN_FAILURE);
            try {
                super.setLevel(Level.WARNING);
            } catch (final RuntimeException fail) {
                reportError(fail.getMessage(), fail, ErrorManager.OPEN_FAILURE);
            }
        }
    }

    private void initFilter(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        try {
            String name = manager.getProperty(p.concat(".filter"));
            if (hasValue(name)) {
                super.setFilter(LogManagerProperties.newFilter(name));
            }
        } catch (final SecurityException SE) {
            throw SE; //Avoid catch all.
        } catch (final Exception E) {
            reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
        }
    }

    private void initCapacity(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        final int DEFAULT_CAPACITY = 1000;
        try {
            final String value = manager.getProperty(p.concat(".capacity"));
            if (value != null) {
                this.setCapacity0(Integer.parseInt(value));
            } else {
                this.setCapacity0(DEFAULT_CAPACITY);
            }
        } catch (final RuntimeException RE) {
            reportError(RE.getMessage(), RE, ErrorManager.OPEN_FAILURE);
        }

        if (capacity <= 0) {
            capacity = DEFAULT_CAPACITY;
        }

        this.data = new LogRecord[1];
    }

    private void initEncoding(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        try {
            super.setEncoding(manager.getProperty(p.concat(".encoding")));
        } catch (final SecurityException SE) {
            throw SE; //Avoid catch all.
        } catch (final UnsupportedEncodingException UEE) {
            reportError(UEE.getMessage(), UEE, ErrorManager.OPEN_FAILURE);
        } catch (final RuntimeException RE) {
            reportError(RE.getMessage(), RE, ErrorManager.OPEN_FAILURE);
        }
    }

    private void initErrorManager(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        String name = manager.getProperty(p.concat(".errorManager"));
        if (name != null) {
            try {
                ErrorManager em = LogManagerProperties.newErrorManager(name);
                super.setErrorManager(em);
            } catch (final SecurityException SE) {
                throw SE; //Avoid catch all.
            } catch (final Exception E) {
                reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
            }
        }
    }

    private void initFormatter(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        String name = manager.getProperty(p.concat(".formatter"));
        if (hasValue(name)) {
            try {
                final Formatter formatter = LogManagerProperties.newFormatter(name);
                assert formatter != null;
                if (formatter instanceof TailNameFormatter == false) {
                    super.setFormatter(formatter);
                } else {
                    super.setFormatter(new SimpleFormatter());
                }
            } catch (final SecurityException SE) {
                throw SE; //Avoid catch all.
            } catch (final Exception E) {
                reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
                try {
                    super.setFormatter(new SimpleFormatter());
                } catch (final RuntimeException fail) {
                    reportError(fail.getMessage(), fail, ErrorManager.OPEN_FAILURE);
                }
            }
        } else {
            super.setFormatter(new SimpleFormatter());
        }
    }

    private void initComparator(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        String name = manager.getProperty(p.concat(".comparator"));
        String reverse = manager.getProperty(p.concat(".comparator.reverse"));
        try {
            if (hasValue(name)) {
                comparator = LogManagerProperties.newComparator(name);
                if (Boolean.parseBoolean(reverse)) {
                    assert comparator != null : "null";
                    comparator = LogManagerProperties.reverseOrder(comparator);
                }
            } else {
                if (!isEmpty(reverse)) {
                    throw new IllegalArgumentException(
                            "No comparator to reverse.");
                }
            }
        } catch (final SecurityException SE) {
            throw SE; //Avoid catch all.
        } catch (final Exception E) {
            reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
        }
    }

    private void initPushLevel(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        try {
            final String val = manager.getProperty(p.concat(".pushLevel"));
            if (val != null) {
                this.pushLevel = Level.parse(val);
            }
        } catch (final RuntimeException RE) {
            reportError(RE.getMessage(), RE, ErrorManager.OPEN_FAILURE);
        }

        if (this.pushLevel == null) {
            this.pushLevel = Level.OFF;
        }
    }

    private void initPushFilter(LogManager manager, String p) {
        assert Thread.holdsLock(this);
        String name = manager.getProperty(p.concat(".pushFilter"));
        if (hasValue(name)) {
            try {
                this.pushFilter = LogManagerProperties.newFilter(name);
            } catch (final SecurityException SE) {
                throw SE; //Avoid catch all.
            } catch (final Exception E) {
                reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
            }
        }
    }

    private void initSubject(LogManager manager, final String p) {
        assert Thread.holdsLock(this);
        String name = manager.getProperty(p.concat(".subject"));
        if (hasValue(name)) {
            try {
                this.subjectFormatter = LogManagerProperties.newFormatter(name);
            } catch (final SecurityException SE) {
                throw SE; //Avoid catch all.
            } catch (final ClassNotFoundException literalSubject) {
                this.subjectFormatter = new TailNameFormatter(name);
            } catch (final ClassCastException literalSubject) {
                this.subjectFormatter = new TailNameFormatter(name);
            } catch (final Exception E) {
                this.subjectFormatter = new TailNameFormatter(name);
                reportError(E.getMessage(), E, ErrorManager.OPEN_FAILURE);
            }
        } else {
            if (name != null) {
                this.subjectFormatter = new TailNameFormatter(name);
            }
        }

        if (this.subjectFormatter == null) { //Ensure not null.
            this.subjectFormatter = new TailNameFormatter("");
        }
    }

    /**
     * Check if any attachment would actually format the given
     * <tt>LogRecord</tt>.  This method does not check if the handler
     * is level is set to OFF or if the handler is closed.
     * @param record  a <tt>LogRecord</tt>
     * @return true if the <tt>LogRecord</tt> would be formatted.
     */
    private boolean isAttachmentLoggable(final LogRecord record) {
        final Filter[] filters = readOnlyAttachmentFilters();
        for (int i = 0; i < filters.length; ++i) {
            final Filter f = filters[i];
            if (f == null || f.isLoggable(record)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this <tt>Handler</tt> would push after storing the
     * <tt>LogRecord</tt> into its internal buffer.
     * @param record  a <tt>LogRecord</tt>
     * @return true if the <tt>LogRecord</tt> triggers an email push.
     */
    private boolean isPushable(final LogRecord record) {
        assert Thread.holdsLock(this);
        final int value = getPushLevel().intValue();
        if (value == offValue || record.getLevel().intValue() < value) {
            return false;
        }

        final Filter filter = getPushFilter();
        return filter == null || filter.isLoggable(record);
    }

    /**
     * Used to perform push or flush.
     * @param priority true for high priority otherwise false for normal.
     * @param code the error manager code.
     */
    private void push(final boolean priority, final int code) {
        if (tryMutex()) {
            try {
                final Message msg = writeLogRecords(code);
                if (msg != null) {
                    send(msg, priority, code);
                }
            } finally {
                releaseMutex();
            }
        } else {
            reportUnPublishedError(null);
        }
    }

    /**
     * Used to send the generated email or write its contents to the
     * error manager for this handler.  This method does not hold any
     * locks so new records can be added to this handler during a send or
     * failure.
     * @param msg the message or null.
     * @param priority true for high priority or false for normal.
     * @param code the ErrorManager code.
     * @throws NullPointerException if message is null.
     */
    private void send(Message msg, boolean priority, int code) {
        try {
            envelopeFor(msg, priority);
            Transport.send(msg); //Calls save changes.
        } catch (final Exception E) {
            reportError(msg, E, code);
        }
    }

    /**
     * Performs a sort on the records if needed.
     * Any exception thrown during a sort is considered a formatting error.
     */
    private void sort() {
        assert Thread.holdsLock(this);
        if (comparator != null) {
            try {
                if (size != 1) {
                    Arrays.sort(data, 0, size, comparator);
                } else {
                    comparator.compare(data[0], data[0]);
                }
            } catch (final RuntimeException RE) {
                reportError(RE.getMessage(), RE, ErrorManager.FORMAT_FAILURE);
            }
        }
    }

    /**
     * Formats all records in the buffer and places the output in a Message.
     * This method holds a lock on this handler.
     * @param code the error manager code.
     * @return null if there are no records or is currently in a push.
     * Otherwise a new message is created with a formatted message and
     * attached session.
     */
    private synchronized Message writeLogRecords(final int code) {
        if (size == 0 || isWriting) {
            return null;
        }

        isWriting = true;
        try {
            sort();
            if (session == null) {
                initSession();
            }
            MimeMessage msg = new MimeMessage(session);
            msg.setDescription(descriptionFrom(comparator, pushLevel, pushFilter));

            /**
             * Parts are lazily created when an attachment performs a getHead
             * call.  Therefore, a null part at an index means that the head is
             * required.
             */
            MimeBodyPart[] parts = new MimeBodyPart[attachmentFormatters.length];

            /**
             * The buffers are lazily created when the part requires a getHead.
             */
            StringBuilder[] buffers = new StringBuilder[parts.length];

            String contentType = null;
            StringBuilder buf = null;

            appendSubject(msg, head(subjectFormatter));

            final MimeBodyPart body = createBodyPart();
            final Formatter bodyFormat = getFormatter();
            final Filter bodyFilter = getFilter();

            Locale lastLocale = null;
            for (int ix = 0; ix < size; ++ix) {
                boolean formatted = false;
                final LogRecord r = data[ix];
                data[ix] = null; //Clear while formatting.

                final Locale locale = localeFor(r);
                appendSubject(msg, format(subjectFormatter, r));

                if (bodyFilter == null || bodyFilter.isLoggable(r)) {
                    if (buf == null) {
                        buf = new StringBuilder();
                        final String head = head(bodyFormat);
                        buf.append(head);
                        contentType = contentTypeOf(head);
                    }
                    formatted = true;
                    buf.append(format(bodyFormat, r));
                    if (locale != null && !locale.equals(lastLocale)) {
                        appendContentLang(body, locale);
                    }
                }

                for (int i = 0; i < parts.length; ++i) {
                    final Filter af = attachmentFilters[i];
                    if (af == null || af.isLoggable(r)) {
                        if (parts[i] == null) {
                            parts[i] = createBodyPart(i);
                            buffers[i] = new StringBuilder();
                            buffers[i].append(head(attachmentFormatters[i]));
                            appendFileName(parts[i], head(attachmentNames[i]));
                        }
                        formatted = true;
                        appendFileName(parts[i], format(attachmentNames[i], r));
                        buffers[i].append(format(attachmentFormatters[i], r));
                        if (locale != null && !locale.equals(lastLocale)) {
                           appendContentLang(parts[i], locale);
                        }
                    }
                }

                if (formatted) {
                    if (locale != null && !locale.equals(lastLocale)) {
                        appendContentLang(msg, locale);
                    }
                } else {  //Belongs to no mime part.
                    reportFilterError(r);
                }
                lastLocale = locale;
            }
            this.size = 0;

            for (int i = parts.length - 1; i >= 0; --i) {
                if (parts[i] != null) {
                    appendFileName(parts[i], tail(attachmentNames[i], "err"));
                    buffers[i].append(tail(attachmentFormatters[i], ""));

                    if (buffers[i].length() > 0) {
                        String name = parts[i].getFileName();
                        if (isEmpty(name)) { //Exceptional case.
                            name = toString(attachmentFormatters[i]);
                            parts[i].setFileName(name);
                        }
                        setContent(parts[i], buffers[i], getContentType(name));
                    } else {
                        setIncompleteCopy(msg);
                        parts[i] = null; //Skip this part.
                    }
                    buffers[i] = null;
                }
            }

            if (buf != null) {
                buf.append(tail(bodyFormat, ""));
                //This body part is always added, even if the buffer is empty,
                //so the body is never considered an incomplete-copy.
            } else {
                buf = new StringBuilder(0);
            }

            appendSubject(msg, tail(subjectFormatter, ""));

            MimeMultipart multipart = new MimeMultipart();
            String altType = getContentType(bodyFormat.getClass().getName());
            setContent(body, buf, altType == null ? contentType : altType);
            multipart.addBodyPart(body);

            for (int i = 0; i < parts.length; ++i) {
                if (parts[i] != null) {
                    multipart.addBodyPart(parts[i]);
                }
            }

            msg.setContent(multipart);
            return msg;
        } catch (final RuntimeException re) {
            reportError(re.getMessage(), re, code);
        } catch (final Exception e) {
            reportError(e.getMessage(), e, code);
        } finally {
            isWriting = false;
            if (size > 0) {
                reset();
            }
        }
        return null;
    }

    /**
     * Checks all of the settings if the caller requests a verify and a verify
     * was not performed yet and no verify is in progress.  A verify is
     * performed on create because this handler may be at the end of a handler
     * chain and therefore may not see any log records until LogManager.reset()
     * is called and at that time all of the settings have been cleared.
     * @param session the current session or null.
     * @since JavaMail 1.4.4
     */
    private void verifySettings(final Session session) {
        if (session != null) {
            final Properties props = session.getProperties();
            final Object check = props.put("verify", "");
            if (check instanceof String) {
                String value = (String) check;
                //Perform the verify if needed.
                if (hasValue(value)) {
                    verifySettings0(session, value);
                }
            } else {
                if (check != null) { //This call will fail.
                    verifySettings0(session, check.getClass().toString());
                }
            }
        }
    }

    /**
     * Checks all of the settings using the given setting.
     * This triggers the LogManagerProperties to copy all of the mail
     * settings without explictly knowing them.  Once all of the properties
     * are copied this handler can handle LogManager.reset clearing all of the
     * properties.  It is expected that this method is, at most, only called
     * once per session.
     * @param session the current session.
     * @since JavaMail 1.4.4
     */
    private void verifySettings0(Session session, String verify) {
        assert verify != null : (String) null;
        if (!"local".equals(verify) && !"remote".equals(verify)
                && !"limited".equals(verify) && !"resolve".equals(verify)) {
            reportError("Verify must be 'limited', local', "
                    + "'resolve' or 'remote'.",
                    new IllegalArgumentException(verify),
                    ErrorManager.OPEN_FAILURE);
            return;
        }

        final MimeMessage abort = new MimeMessage(session);
        final String msg;
        if (!"limited".equals(verify)) {
            msg = "Local address is "
                + InternetAddress.getLocalAddress(session) + '.';

            try { //Verify subclass or declared mime charset.
                Charset.forName(getEncodingName());
            } catch (final RuntimeException RE) {
                UnsupportedEncodingException UEE =
                        new UnsupportedEncodingException(RE.toString());
                UEE.initCause(RE);
                reportError(msg, UEE, ErrorManager.FORMAT_FAILURE);
            }
        } else {
            msg = "Skipping local address check.";
        }

        //Perform all of the copy actions first.
        synchronized (this) { //Create the subject.
            appendSubject(abort, head(subjectFormatter));
            appendSubject(abort, tail(subjectFormatter, ""));
        }

        setIncompleteCopy(abort); //Original body part is never added.
        envelopeFor(abort, true);
        try {
            abort.saveChanges();
        } catch (final MessagingException ME) {
            reportError(msg, ME, ErrorManager.FORMAT_FAILURE);
        }

        try {
            //Ensure transport provider is installed.
            Address[] all = abort.getAllRecipients();
            if (all == null) { //Don't pass null to sendMessage.
               all = new InternetAddress[0];
            }
            Transport t;
            try {
                final Address[] any = all.length != 0 ? all : abort.getFrom();
                if (any != null && any.length != 0) {
                    t = session.getTransport(any[0]);
                    session.getProperty("mail.transport.protocol"); //Force copy
                } else {
                    MessagingException me = new MessagingException(
                            "No recipient or from address.");
                    reportError(msg, me, ErrorManager.OPEN_FAILURE);
                    throw me;
                }
            } catch (final MessagingException protocol) {
                try {
                    t = session.getTransport();
                } catch (final MessagingException fail) {
                    throw attach(protocol, fail);
                }
            }

            String local = null;
            if ("remote".equals(verify)) {
                MessagingException closed = null;
                t.connect();
                try {
                    try {
                        //Capture localhost while connection is open.
                        if (t instanceof SMTPTransport) {
                            local = ((SMTPTransport) t).getLocalHost();
                        }
                        //A message without content will fail at message writeTo
                        //when sendMessage is called.  This allows the handler
                        //to capture all mail properties set in the LogManager.
                        t.sendMessage(abort, all);
                    } finally {
                        try { //Close the transport before reporting errors.
                            t.close();
                        } catch (final MessagingException ME) {
                            closed = ME;
                        }
                    }
                    reportUnexpectedSend(abort, verify, null);
                } catch (final SendFailedException sfe) {
                    Address[] recip = sfe.getInvalidAddresses();
                    if (recip != null && recip.length != 0) {
                        fixUpContent(abort, verify, sfe);
                        reportError(abort, sfe, ErrorManager.OPEN_FAILURE);
                    }

                    recip = sfe.getValidSentAddresses();
                    if (recip != null && recip.length != 0) {
                        reportUnexpectedSend(abort, verify, sfe);
                    }
                } catch (final MessagingException ME) {
                    if (!isMissingContent(abort, ME)) {
                        fixUpContent(abort, verify, ME);
                        reportError(abort, ME, ErrorManager.OPEN_FAILURE);
                    }
                }

                if (closed != null) {
                    fixUpContent(abort, verify, closed);
                    reportError(abort, closed, ErrorManager.CLOSE_FAILURE);
                }
            } else {
                //Force a property copy.
                final String protocol = t.getURLName().getProtocol();
                session.getProperty("mail.host");
                session.getProperty("mail.user");
                session.getProperty("mail." + protocol + ".host");
                session.getProperty("mail." + protocol + ".port");
                session.getProperty("mail." + protocol + ".user");
                local = session.getProperty("mail." + protocol + ".localhost");
                if (isEmpty(local)) {
                    local = session.getProperty("mail."
                            + protocol + ".localaddress");
                }

                if ("resolve".equals(verify)) {
                    try { //Resolve the remote host name.
                        verifyHost(t.getURLName().getHost());
                    } catch (final IOException IOE) {
                        MessagingException ME =
                                new MessagingException(msg, IOE);
                        fixUpContent(abort, verify, ME);
                        reportError(abort, ME, ErrorManager.OPEN_FAILURE);
                    } catch (final RuntimeException RE) {
                        MessagingException ME =
                                new MessagingException(msg, RE);
                        fixUpContent(abort, verify, RE);
                        reportError(abort, ME, ErrorManager.OPEN_FAILURE);
                    }
                }
            }

            if (!"limited".equals(verify)) {
                try { //Verify host name and hit the host name cache.
                    if (!"remote".equals(verify)
                            && t instanceof SMTPTransport) {
                       local = ((SMTPTransport) t).getLocalHost();
                    }
                    verifyHost(local);
                } catch (final IOException IOE) {
                    MessagingException ME = new MessagingException(msg, IOE);
                    fixUpContent(abort, verify, ME);
                    reportError(abort, ME, ErrorManager.OPEN_FAILURE);
                } catch (final RuntimeException RE) {
                    MessagingException ME = new MessagingException(msg, RE);
                    fixUpContent(abort, verify, ME);
                    reportError(abort, ME, ErrorManager.OPEN_FAILURE);
                }


                try { //Verify that the DataHandler can be loaded.
                    final MimeMultipart multipart = new MimeMultipart();
                    final MimeBodyPart body = new MimeBodyPart();
                    body.setDisposition(Part.INLINE);
                    body.setDescription(verify);
                    setAcceptLang(body);
                    setContent(body, "", "text/plain");
                    multipart.addBodyPart(body);
                    abort.setContent(multipart);
                    abort.saveChanges();
                    abort.writeTo(new ByteArrayOutputStream(MIN_HEADER_SIZE));
                } catch (final IOException IOE) {
                    MessagingException ME = new MessagingException(msg, IOE);
                    fixUpContent(abort, verify, ME);
                    reportError(abort, ME, ErrorManager.FORMAT_FAILURE);
                }
            }

            //Verify all recipients.
            if (all.length != 0) {
                verifyAddresses(all);
            } else {
                throw new MessagingException("No recipient addresses.");
            }

            //Verify from and sender addresses.
            Address[] from = abort.getFrom();
            Address sender = abort.getSender();
            if (sender instanceof InternetAddress) {
                ((InternetAddress) sender).validate();
            }

            //If from address is declared then check sender.
            if (abort.getHeader("From", ",") != null && from.length != 0) {
                verifyAddresses(from);
                for (int i = 0; i < from.length; ++i) {
                    if (from[i].equals(sender)) {
                        MessagingException ME = new MessagingException(
                            "Sender address '" + sender
                            + "' equals from address.");
                        throw new MessagingException(msg, ME);
                    }
                }
            } else {
                if (sender == null) {
                    MessagingException ME = new MessagingException(
                            "No from or sender address.");
                    throw new MessagingException(msg, ME);
                }
            }

            //Verify reply-to addresses.
            verifyAddresses(abort.getReplyTo());
        } catch (final MessagingException ME) {
            fixUpContent(abort, verify, ME);
            reportError(abort, ME, ErrorManager.OPEN_FAILURE);
        } catch (final RuntimeException RE) {
            fixUpContent(abort, verify, RE);
            reportError(abort, RE, ErrorManager.OPEN_FAILURE);
        }
    }

    /**
     * Perform a lookup of the host address or FQDN.
     * @param host the host or null.
     * @return the address.
     * @throws IOException if the host name is not valid.
     * @since JavaMail 1.5.0
     */
    private static InetAddress verifyHost(String host) throws IOException {
        InetAddress a;
        if (isEmpty(host)) {
            a = InetAddress.getLocalHost();
        } else {
            a = InetAddress.getByName(host);
        }
        if (a.getCanonicalHostName().length() == 0) {
            throw new UnknownHostException();
        }
        return a;
    }

    /**
     * Calls validate for every address given.
     * If the addresses given are null, empty or not an InternetAddress then
     * the check is skipped.
     * @param all any address array, null or empty.
     * @throws AddressException if there is a problem.
     * @since JavaMail 1.4.5
     */
    private static void verifyAddresses(Address[] all) throws AddressException {
        if (all != null) {
            for (int i = 0; i < all.length; ++i) {
                final Address a = all[i];
                if (a instanceof InternetAddress) {
                    ((InternetAddress) a).validate();
                }
            }
        }
    }

    /**
     * Reports that an empty content message was sent and should not have been.
     * @param msg the MimeMessage.
     * @param verify the verify enum.
     * @param cause the exception that caused the problem or null.
     * @since JavaMail 1.4.5
     */
    private void reportUnexpectedSend(MimeMessage msg, String verify, Exception cause) {
        final MessagingException write = new MessagingException(
                "An empty message was sent.", cause);
        fixUpContent(msg, verify, write);
        reportError(msg, write, ErrorManager.OPEN_FAILURE);
    }

    /**
     * Creates and sets the message content from the given Throwable.
     * When verify fails, this method fixes the 'abort' message so that any
     * created envelope data can be used in the error manager.
     * @param msg the message with or without content.
     * @param verify the verify enum.
     * @param t the throwable or null.
     * @since JavaMail 1.4.5
     */
    private void fixUpContent(MimeMessage msg, String verify, Throwable t) {
        try { //Add content so toRawString doesn't fail.
            final MimeBodyPart body;
            final String subjectType;
            final String msgDesc;
            synchronized (this) {
                body = createBodyPart();
                msgDesc = descriptionFrom(comparator, pushLevel, pushFilter);
                subjectType = getClassId(subjectFormatter);
            }

            body.setDescription("Formatted using "
                        + (t == null ? Throwable.class.getName()
                        : t.getClass().getName()) + ", filtered with "
                        + verify + ", and named by "
                        + subjectType + '.');
            setContent(body, toMsgString(t), "text/plain");
            final MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(body);
            msg.setContent(multipart);
            msg.setDescription(msgDesc);
            setAcceptLang(msg);
            msg.saveChanges();
        } catch (final MessagingException ME) {
            reportError("Unable to create body.", ME, ErrorManager.OPEN_FAILURE);
        } catch (final RuntimeException RE) {
            reportError("Unable to create body.", RE, ErrorManager.OPEN_FAILURE);
        }
    }

    /**
     * Used to update the cached session object based on changes in
     * mail properties or authenticator.
     * @return the current session or null if no verify is required.
     */
    private Session fixUpSession() {
        assert Thread.holdsLock(this);
        final Session settings;
        if (mailProps.getProperty("verify") != null) {
            settings = initSession();
            assert settings == session;
        } else {
            session = null; //Remove old session.
            settings = null;
        }
        return settings;
    }

    private Session initSession() {
        assert Thread.holdsLock(this);
        final String p = getClass().getName();
        LogManagerProperties proxy = new LogManagerProperties(mailProps, p);
        session = Session.getInstance(proxy, auth);
        return session;
    }

    /**
     * Creates all of the envelope information for a message.
     * This method is safe to call outside of a lock because the message
     * provides the safe snapshot of the mail properties.
     * @param msg the Message to write the envelope information.
     * @param priority true for high priority.
     */
    private void envelopeFor(Message msg, boolean priority) {
        setAcceptLang(msg);
        setFrom(msg);
        if (!setRecipient(msg, "mail.to", Message.RecipientType.TO)) {
            setDefaultRecipient(msg, Message.RecipientType.TO);
        }
        setRecipient(msg, "mail.cc", Message.RecipientType.CC);
        setRecipient(msg, "mail.bcc", Message.RecipientType.BCC);
        setReplyTo(msg);
        setSender(msg);
        setMailer(msg);
        setAutoSubmitted(msg);
        if (priority) {
            setPriority(msg);
        }

        try {
            msg.setSentDate(new java.util.Date());
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    private MimeBodyPart createBodyPart() throws MessagingException {
        assert Thread.holdsLock(this);
        final MimeBodyPart part = new MimeBodyPart();
        part.setDisposition(Part.INLINE);
        part.setDescription(descriptionFrom(getFormatter(),
                getFilter(), subjectFormatter));
        setAcceptLang(part);
        return part;
    }

    private MimeBodyPart createBodyPart(int index) throws MessagingException {
        assert Thread.holdsLock(this);
        final MimeBodyPart part = new MimeBodyPart();
        part.setDisposition(Part.ATTACHMENT);
        part.setDescription(descriptionFrom(
                attachmentFormatters[index],
                attachmentFilters[index],
                attachmentNames[index]));
        setAcceptLang(part);
        return part;
    }

    /**
     * Gets the description for the MimeMessage itself.
     * The push level and filter are included because they play a role in
     * formatting of a message when triggered or not triggered.
     * @param c the comparator.
     * @param l the pushLevel.
     * @param f the pushFilter
     * @return the description.
     * @throws NullPointerException if level is null.
     * @since JavaMail 1.4.5
     */
    private String descriptionFrom(Comparator c, Level l, Filter f) {
        return "Sorted using "+ (c == null ? "no comparator"
                : c.getClass().getName()) + ", pushed when "+ l.getName()
                + ", and " + (f == null ? "no push filter"
                : f.getClass().getName()) + '.';
    }

    private String descriptionFrom(Formatter f, Filter filter, Formatter name) {
        return "Formatted using " + getClassId(f)
                + ", filtered with " + (filter == null ? "no filter"
                : filter.getClass().getName()) +", and named by "
                + getClassId(name) + '.';
    }

    /**
     * Gets a class name represents the behavior of the formatter.
     * The class name may not be assignable to a Formatter.
     * @param f the formatter.
     * @return a class name that represents the given formatter.
     * @throws NullPointerException if the parameter is null.
     * @since JavaMail 1.4.5
     */
    private String getClassId(final Formatter f) {
        if (f instanceof TailNameFormatter) {
            return String.class.getName(); //Literal string.
        } else {
            return f.getClass().getName();
        }
    }

    /**
     * Ensure that a formatter creates a valid string for a part name.
     * @param f the formatter.
     * @return the to string value or the class name.
     */
    private String toString(final Formatter f) {
        //Should never be null but, guard against formatter bugs.
        final String name = f.toString();
        if (!isEmpty(name)) {
            return name;
        } else {
            return getClassId(f);
        }
    }

    /**
     * Constructs a file name from a formatter.  This method is called often
     * but, rarely does any work.
     * @param part to append to.
     * @param chunk non null string to append.
     */
    private void appendFileName(final Part part, final String chunk) {
        if (chunk != null) {
            if (chunk.length() > 0) {
                appendFileName0(part, chunk);
            }
        } else {
            reportNullError(ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * It is assumed that file names are short and that in most cases
     * getTail will be the only method that will produce a result.
     * @param part to append to.
     * @param chunk non null string to append.
     */
    private void appendFileName0(final Part part, final String chunk) {
        try {
            final String old = part.getFileName();
            part.setFileName(old != null ? old.concat(chunk) : chunk);
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Constructs a subject line from a formatter.
     * @param msg to append to.
     * @param chunk non null string to append.
     */
    private void appendSubject(final Message msg, final String chunk) {
        if (chunk != null) {
            if (chunk.length() > 0) {
                appendSubject0(msg, chunk);
            }
        } else {
            reportNullError(ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * It is assumed that subject lines are short and that in most cases
     * getTail will be the only method that will produce a result.
     * @param msg to append to.
     * @param chunk non null string to append.
     */
    private void appendSubject0(final Message msg, final String chunk) {
        try {
            final String encoding = getEncodingName();
            final String old = msg.getSubject();
            assert msg instanceof MimeMessage;
            ((MimeMessage) msg).setSubject(old != null ? old.concat(chunk)
                    : chunk, MimeUtility.mimeCharset(encoding));
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Gets the locale for the given log record from the resource bundle.
     * If the resource bundle is using the root locale then the default locale
     * is returned.
     * @param r the log record.
     * @return null if not localized otherwise, the locale of the record.
     * @since JavaMail 1.4.5
     */
    private Locale localeFor(final LogRecord r) {
        Locale l;
        final ResourceBundle rb = r.getResourceBundle();
        if (rb != null) {
            l = rb.getLocale();
            if (l == null || isEmpty(l.getLanguage())) {
                //The language of the fallback bundle (root) is unknown.
                //1. Use default locale.  Should only be wrong if the app is
                //   used with a langauge that was unintended. (unlikely)
                //2. Mark it as not localized (force null, info loss).
                //3. Use the bundle name (encoded) as an experimental language.
                l = Locale.getDefault();
            }
        } else {
            l = null;
        }
        return l;
    }

    /**
     * Appends the content language to the given mime part.
     * The language tag is only appended if the given language has not been
     * specified.  This method is only used when we have LogRecords that are
     * localized with an assigned resource bundle.
     * @param p the mime part.
     * @param l the locale to append.
     * @throws NullPointerException if any argument is null.
     * @since JavaMail 1.4.5
     */
    private void appendContentLang(final MimePart p, final Locale l) {
        try {
            String lang = LogManagerProperties.toLanguageTag(l);
            if (lang.length() != 0) {
                String header = p.getHeader("Content-Language", null);
                if (isEmpty(header)) {
                    p.setHeader("Content-Language", lang);
                } else if (!header.equalsIgnoreCase(lang)) {
                    lang = ",".concat(lang);
                    int idx = 0;
                    while ((idx = header.indexOf(lang, idx)) > -1) {
                        idx += lang.length();
                        if (idx == header.length()
                            || header.charAt(idx) == ',') {
                            break;
                        }
                    }

                    if (idx < 0) {
                        int len = header.lastIndexOf("\r\n\t");
                        if (len < 0) { //If not folded.
                            len = (18 + 2) + header.length();
                        } else {
                            len = (header.length() - len) + 8;
                        }

                        //Perform folding of header if needed.
                        if ((len + lang.length()) > 76) {
                            header = header.concat("\r\n\t".concat(lang));
                        } else {
                            header = header.concat(lang);
                        }
                        p.setHeader("Content-Language", header);
                    }
                }
            }
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Sets the accept language to the default locale of the JVM.
     * If the locale is the root locale the header is not added.
     * @param p the part to set.
     * @since JavaMail 1.4.5
     */
    private void setAcceptLang(final Part p) {
        try {
            final String lang = LogManagerProperties
                    .toLanguageTag(Locale.getDefault());
            if (lang.length() != 0) {
                p.setHeader("Accept-Language", lang);
            }
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Used when a log record was loggable prior to being inserted
     * into the buffer but at the time of formatting was no longer loggable.
     * Filters were changed after publish but prior to a push or a bug in the
     * body filter or one of the attachment filters.
     * @param record that was not formatted.
     * @since JavaMail 1.4.5
     */
    private void reportFilterError(final LogRecord record) {
        assert Thread.holdsLock(this);
        final SimpleFormatter f = new SimpleFormatter();
        final String msg = "Log record " + record.getSequenceNumber()
                + " was filtered from all message parts.  "
                + head(f) + format(f, record) + tail(f, "");
        final String txt = getFilter() + ", "
                + Arrays.asList(readOnlyAttachmentFilters());
        reportError(msg, new IllegalArgumentException(txt),
                ErrorManager.FORMAT_FAILURE);
    }

    /**
     * Reports symmetric contract violations an equals implementation.
     * @param o the test object must be non null.
     * @param found the possible intern, must be non null.
     * @throws NullPointerException if any argument is null.
     * @since JavaMail 1.5.0
     */
    private void reportNonSymmetric(final Object o, final Object found) {
        reportError("Non symmetric equals implementation."
                , new IllegalArgumentException(o.getClass().getName()
                + " is not equal to " + found.getClass().getName())
                , ErrorManager.OPEN_FAILURE);
    }

    /**
     * Reports equals implementations that do not discriminate between objects
     * of different types or subclass types.
     * @param o the test object must be non null.
     * @param found the possible intern, must be non null.
     * @throws NullPointerException if any argument is null.
     * @since JavaMail 1.5.0
     */
    private void reportNonDiscriminating(final Object o, final Object found) {
        reportError("Non discriminating equals implementation."
                , new IllegalArgumentException(o.getClass().getName()
                + " should not be equal to " + found.getClass().getName())
                , ErrorManager.OPEN_FAILURE);
    }

    private void reportNullError(final int code) {
        reportError("null", new NullPointerException(), code);
    }

    private String head(final Formatter f) {
        try {
            return f.getHead(this);
        } catch (final RuntimeException RE) {
            reportError(RE.getMessage(), RE, ErrorManager.FORMAT_FAILURE);
            return "";
        }
    }

    private String format(final Formatter f, final LogRecord r) {
        try {
            return f.format(r);
        } catch (final RuntimeException RE) {
            reportError(RE.getMessage(), RE, ErrorManager.FORMAT_FAILURE);
            return "";
        }
    }

    private String tail(final Formatter f, final String def) {
        try {
            return f.getTail(this);
        } catch (final RuntimeException RE) {
            reportError(RE.getMessage(), RE, ErrorManager.FORMAT_FAILURE);
            return def;
        }
    }

    private void setMailer(final Message msg) {
        try {
            final Class mail = MailHandler.class;
            final Class k = getClass();
            String value;
            if (k == mail) {
                value = mail.getName();
            } else {
                try {
                    value = MimeUtility.encodeText(k.getName());
                } catch (final UnsupportedEncodingException E) {
                    reportError(E.getMessage(), E, ErrorManager.FORMAT_FAILURE);
                    value = k.getName().replaceAll("[^\\x00-\\x7F]", "\uu001A");
                }
                value = MimeUtility.fold(10, mail.getName() + " using the "
                            + value + " extension.");
            }
            msg.setHeader("X-Mailer", value);
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    private void setPriority(final Message msg) {
        try {
            msg.setHeader("Importance", "High");
            msg.setHeader("Priority", "urgent");
            msg.setHeader("X-Priority", "2"); //High
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Used to signal that body parts are missing from a message.  Also used
     * when LogRecords were passed to an attachment formatter but the formatter
     * produced no output, which is allowed.  Used during a verify because all
     * parts are omitted, none of the content formatters are used.  This is
     * not used when a filter prevents LogRecords from being formatted.
     * This header is defined in RFC 2156 and RFC 4021.
     * @param msg the message.
     * @since JavaMail 1.4.5
     */
    private void setIncompleteCopy(final Message msg) {
        try {
            msg.setHeader("Incomplete-Copy", "");
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Signals that this message was generated by automatic process.
     * This header is defined in RFC 3834 section 5.
     * @param msg the message.
     * @since JavaMail 1.4.6
     */
    private void setAutoSubmitted(final Message msg) {
        try {
            msg.setHeader("auto-submitted", "auto-generated"); //RFC 3834 (5.2)
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    private void setFrom(final Message msg) {
        final String from = msg.getSession().getProperty("mail.from");
        if (from != null) {
            try {
                final Address[] address = InternetAddress.parse(from, false);
                if (address.length > 0) {
                    if (address.length == 1) {
                        msg.setFrom(address[0]);
                    } else { //Greater than 1 address.
                        msg.addFrom(address);
                    }
                }
                //Can't place an else statement here because the 'from' is
                //not null which causes the local address computation
                //to fail.  Assume the user wants to omit the from address
                //header.
            } catch (final MessagingException ME) {
                reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
                setDefaultFrom(msg);
            }
        } else {
            setDefaultFrom(msg);
        }
    }

    private void setDefaultFrom(final Message msg) {
        try {
            msg.setFrom();
        } catch (final MessagingException ME) {
            reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Computes the default to-address if none was specified.  This can
     * fail if the local address can't be computed.
     * @param msg the message
     * @param type the recipient type.
     * @since JavaMail 1.5.0
     */
    private void setDefaultRecipient(final Message msg,
            final Message.RecipientType type) {
        try {
            Address a = InternetAddress.getLocalAddress(msg.getSession());
            if (a != null) {
                msg.setRecipient(type, a);
            } else {
                final MimeMessage m = new MimeMessage(msg.getSession());
                m.setFrom(); //Should throw an exception with a cause.
                Address[] from = m.getFrom();
                if (from.length > 0) {
                    msg.setRecipients(type, from);
                } else {
                    throw new MessagingException("No local address.");
                }
            }
        } catch (final MessagingException ME) {
            reportError("Unable to compute a default recipient.",
                    ME, ErrorManager.FORMAT_FAILURE);
        } catch (final RuntimeException RE) {
            reportError("Unable to compute a default recipient.",
                    RE, ErrorManager.FORMAT_FAILURE);
        }
    }

    private void setReplyTo(final Message msg) {
        final String reply = msg.getSession().getProperty("mail.reply.to");
        if (!isEmpty(reply)) {
            try {
                final Address[] address = InternetAddress.parse(reply, false);
                if (address.length > 0) {
                    msg.setReplyTo(address);
                }
            } catch (final MessagingException ME) {
                reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
            }
        }
    }

    private void setSender(final Message msg) {
        assert msg instanceof MimeMessage : msg;
        final String sender = msg.getSession().getProperty("mail.sender");
        if (!isEmpty(sender)) {
            try {
                final InternetAddress[] address =
                        InternetAddress.parse(sender, false);
                if (address.length > 0) {
                    ((MimeMessage) msg).setSender(address[0]);
                    if (address.length > 1) {
                        reportError("Ignoring other senders.",
                                tooManyAddresses(address, 1),
                                ErrorManager.FORMAT_FAILURE);
                    }
                }
            } catch (final MessagingException ME) {
                reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
            }
        }
    }

    private AddressException tooManyAddresses(Address[] address, int offset) {
        Object l = Arrays.asList(address).subList(offset, address.length);
        return new AddressException(l.toString());
    }

    /**
     * Sets the recipient for the given message.
     * @param msg the message.
     * @param key the key to search in the session.
     * @param type the recipient type.
     * @return true if the key was contained in the session.
     */
    private boolean setRecipient(final Message msg,
            final String key, final Message.RecipientType type) {
        boolean containsKey;
        final String value = msg.getSession().getProperty(key);
        containsKey = value != null;
        if (!isEmpty(value)) {
            try {
                final Address[] address = InternetAddress.parse(value, false);
                if (address.length > 0) {
                    msg.setRecipients(type, address);
                }
            } catch (final MessagingException ME) {
                reportError(ME.getMessage(), ME, ErrorManager.FORMAT_FAILURE);
            }
        }
        return containsKey;
    }

    /**
     * Converts an email message to a raw string.  This raw string
     * is passed to the error manager to allow custom error managers
     * to recreate the original MimeMessage object.
     * @param msg a Message object.
     * @return the raw string or null if msg was null.
     * @throws MessagingException if there was a problem with the message.
     * @throws IOException if there was a problem.
     */
    private String toRawString(final Message msg) throws MessagingException, IOException {
        if (msg != null) {
            final int nbytes = Math.max(msg.getSize() + MIN_HEADER_SIZE, MIN_HEADER_SIZE);
            final ByteArrayOutputStream out = new ByteArrayOutputStream(nbytes);
            msg.writeTo(out);
            return out.toString("US-ASCII"); //Raw message is always ASCII.
        } else { //Must match this.reportError behavior, see push method.
            return null; //Null is the safe choice.
        }
    }

    /**
     * Converts a throwable to a message string.
     * @param t any throwable or null.
     * @return the throwable with a stack trace or the literal null.
     */
    private String toMsgString(final Throwable t) {
        if (t == null) {
           return "null";
        }

        final String encoding = getEncodingName();
        try {
            final ByteArrayOutputStream out =
                    new ByteArrayOutputStream(MIN_HEADER_SIZE);

            //Create an output stream writer so streams are not double buffered.
            final PrintWriter pw =
                    new PrintWriter(new OutputStreamWriter(out, encoding));
            pw.println(t.getMessage());
            t.printStackTrace(pw);
            pw.flush();
            pw.close(); //BUG ID 6995537
            return out.toString(encoding);
        } catch (IOException badMimeCharset) {
            return t.toString() + ' ' + badMimeCharset.toString();
        }
    }

    /**
     * Replaces the current context class loader with our class loader.
     * @return null for the boot class loader, a class loader, or a marker
     * object to signal that no modification was required.
     * @since JavaMail 1.4.6
     */
    private Object getAndSetContextClassLoader() {
        try {
            return AccessController.doPrivileged(GET_AND_SET_CCL);
        } catch (final SecurityException ignore) {
            return GET_AND_SET_CCL; //return not modified.
        }
    }

    /**
     * Restores the original context class loader.
     * @param ccl null for the boot class loader, a class loader, or a
     * marker object to signal that no modification is required.
     * @since JavaMail 1.4.6
     */
    private void setContextClassLoader(final Object ccl) {
        //Boot class loader or a new context class loader.
        if (ccl == null || ccl instanceof ClassLoader) {
            AccessController.doPrivileged(new GetAndSetContext(ccl));
        }
    }

    private static RuntimeException attachmentMismatch(final String msg) {
        return new IndexOutOfBoundsException(msg);
    }

    private static RuntimeException attachmentMismatch(int expected, int found) {
        return attachmentMismatch("Attachments mismatched, expected "
                + expected + " but given " + found + '.');
    }

    private static MessagingException attach(
            MessagingException required, Exception optional) {
        if (optional != null && !required.setNextException(optional)) {
            if (optional instanceof MessagingException) {
                final MessagingException head = (MessagingException) optional;
                if (head.setNextException(required)) {
                    return head;
                }
            }
        }
        return required;
    }

    private static String atIndexMsg(final int i) {
        return "At index: " + i + '.';
    }

    /**
     * Used for storing a password from the LogManager or literal string.
     * @since JavaMail 1.4.6
     */
    private static final class DefaultAuthenticator extends Authenticator {

        private final String pass;

        DefaultAuthenticator(final String pass) {
            assert pass != null;
            this.pass = pass;
        }

        @Override
        protected final PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(getDefaultUserName(), pass);
        }
    }

    /**
     * Performs a get and set of the context class loader with privileges
     * enabled.
     * @since JavaMail 1.4.6
     */
    private static final class GetAndSetContext implements PrivilegedAction<Object> {
        /**
         * The source containing the class loader.
         */
        private final Object source;
        /**
         * Create the action.
         * @param source null for boot class loader, a class loader, a class
         * used to get the class loader, or a source object to get the class
         * loader.
         */
        GetAndSetContext(final Object source) {
            this.source = source;
        }

        /**
         * Gets the class loader from the source and sets the CCL only if
         * the source and CCL are not the same.
         * @return the replaced context class loader which can be null or this
         * to indicate that nothing was modified.
         */
        public final Object run() {
            final Thread current = Thread.currentThread();
            final ClassLoader ccl = current.getContextClassLoader();
            final ClassLoader loader;
            if (source == null) {
                loader = null; //boot class loader
            } else if (source instanceof ClassLoader) {
                loader = (ClassLoader) source;
            } else if (source instanceof Class) {
                loader = ((Class) source).getClassLoader();
            } else {
                loader = source.getClass().getClassLoader();
            }

            if (ccl != loader) {
                current.setContextClassLoader(loader);
                return ccl;
            } else {
                return this; //Unchanged, return non null and non classloader.
            }
        }
    }

    /**
     * Used for naming attachment file names and the main subject line.
     */
    private static final class TailNameFormatter extends Formatter {

        private final String name;

        /**
         * Creates the formatter with the given name.
         * Default access to avoid extra generated class files.
         * @param name any not null string.
         */
        TailNameFormatter(final String name) {
            assert name != null;
            this.name = name;
        }

        public final String format(LogRecord record) {
            return "";
        }

        @Override
        public final String getTail(Handler h) {
            return name;
        }

        /**
         * Equals method.
         * @param o the other object.
         * @return true if equal
         * @since JavaMail 1.4.4
         */
        @Override
        public final boolean equals(Object o) {
            if (o instanceof TailNameFormatter) {
                return name.equals(((TailNameFormatter) o).name);
            }
            return false;
        }

        /**
         * Hash code method.
         * @return the hash code.
         * @since JavaMail 1.4.4
         */
        @Override
        public final int hashCode() {
            return getClass().hashCode() + name.hashCode();
        }

        @Override
        public final String toString() {
            return name;
        }
    }
}
