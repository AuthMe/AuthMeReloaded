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

package com.sun.mail.handlers;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.OutputStream;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;
import javax.mail.internet.ContentType;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * DataContentHandler for text/xml.
 *
 * @author Anil Vijendran
 * @author Bill Shannon
 */
public class text_xml extends text_plain {

    private final DataFlavor[] flavors;

    public text_xml() {
	flavors = new DataFlavor[] {
	    new ActivationDataFlavor(String.class, "text/xml", "XML String"),
	    new ActivationDataFlavor(String.class, "application/xml",
					"XML String"),
	    new ActivationDataFlavor(StreamSource.class, "text/xml", "XML"),
	    new ActivationDataFlavor(StreamSource.class, "application/xml",
					"XML")
	};
    }

    /**
     * Return the DataFlavors for this <code>DataContentHandler</code>.
     *
     * @return the DataFlavors
     */
    public DataFlavor[] getTransferDataFlavors() { // throws Exception;
	return (DataFlavor[])flavors.clone();
    }

    /**
     * Return the Transfer Data of type DataFlavor from InputStream.
     *
     * @param df the DataFlavor
     * @param ds the InputStream corresponding to the data
     * @return the constructed Object
     */
    public Object getTransferData(DataFlavor df, DataSource ds)
				throws IOException {

	for (int i = 0; i < flavors.length; i++) {
	    DataFlavor aFlavor = flavors[i];
	    if (aFlavor.equals(df)) {
		if (aFlavor.getRepresentationClass() == String.class)
		    return super.getContent(ds);
		else if (aFlavor.getRepresentationClass() == StreamSource.class)
		    return new StreamSource(ds.getInputStream());
		else
		    return null;        // XXX - should never happen
	    }
	}
	return null;
    }

    /**
     */
    public void writeTo(Object obj, String mimeType, OutputStream os)
				    throws IOException {
	if (!isXmlType(mimeType))
	    throw new IOException(
		"Invalid content type \"" + mimeType + "\" for text/xml DCH");
	if (obj instanceof String) {
	    super.writeTo(obj, mimeType, os);
	    return;
	}
	if (!(obj instanceof DataSource || obj instanceof Source)) {
	     throw new IOException("Invalid Object type = "+obj.getClass()+
		". XmlDCH can only convert DataSource or Source to XML.");
	}

	try {
	    Transformer transformer =
		TransformerFactory.newInstance().newTransformer();
	    StreamResult result = new StreamResult(os);
	    if (obj instanceof DataSource) {
		// Streaming transform applies only to
		// javax.xml.transform.StreamSource
		transformer.transform(
		    new StreamSource(((DataSource)obj).getInputStream()),
		    result);
	    } else {
		transformer.transform((Source)obj, result);
	    }
	} catch (Exception ex) {
	    throw new IOException(
		"Unable to run the JAXP transformer on a stream "
		    + ex.getMessage());
	}
    }

    private boolean isXmlType(String type) {
	try {
	    ContentType ct = new ContentType(type);
	    return ct.getSubType().equals("xml") &&
		    (ct.getPrimaryType().equals("text") ||
		    ct.getPrimaryType().equals("application"));
	} catch (Exception ex) {
	    return false;
	}
    }
}
