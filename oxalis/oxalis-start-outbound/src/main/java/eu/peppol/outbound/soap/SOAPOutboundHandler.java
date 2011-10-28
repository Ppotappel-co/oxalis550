/*
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://www.osor.eu/eupl/european-union-public-licence-eupl-v.1.1
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package eu.peppol.outbound.soap;

import eu.peppol.outbound.util.Log;
import org.w3._2009._02.ws_tra.DocumentIdentifierType;
import org.w3._2009._02.ws_tra.ObjectFactory;
import org.w3._2009._02.ws_tra.ParticipantIdentifierType;
import org.w3._2009._02.ws_tra.ProcessIdentifierType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Set;

/**
 * The SOAPOutboundHandler class is used to handle an Ourbound SOAP message
 * in order to  include the BUSDOX defined headers.
 *
 * @author Dante Malaga(dante@alfa1lab.com)
 *         Jose Gorvenia Narvaez(jose@alfa1lab.com)
 */
public class SOAPOutboundHandler implements SOAPHandler<SOAPMessageContext> {

    /**
     * Holds an static SOAPHeaderObject object.
     */
    private static SoapHeader soapHeader;
    private SOAPMessage message;

    /**
     * @return the soapHeader
     */
    public static SoapHeader getSoapHeader() {
        return soapHeader;
    }

    /**
     * @param aSoapHeader the soapHeader to set
     */
    public static void setSoapHeader(SoapHeader aSoapHeader) {
        soapHeader = aSoapHeader;
    }

    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleMessage(SOAPMessageContext soapMessageContext) {

        Log.debug("SOAP outbound handler called");

        try {

            Boolean isOutboundMessage = (Boolean) soapMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

            if (isOutboundMessage) {
                createSOAPHeader(soapMessageContext);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while marshalling SOAP headers", e);
        }

        return true;
    }

    /**
     * Adds the BUSDOX headers to the header part of the given SOAP-envelope.
     */
    private void createSOAPHeader(SOAPMessageContext soapMessageContext) throws SOAPException, JAXBException {

        Log.debug("Adding BUSDOX headers to SOAP-envelope");

        SOAPEnvelope envelope = soapMessageContext.getMessage().getSOAPPart().getEnvelope();
        SOAPHeader header = envelope.getHeader();

        if (header == null) {
            header = envelope.addHeader();
        }

        ObjectFactory objFactory = new ObjectFactory();
        Marshaller marshaller = null;

        String channelId = getSoapHeader().getChannelIdentifier();
        String messageId = getSoapHeader().getMessageIdentifier();

        ParticipantIdentifierType recipientId = new ParticipantIdentifierType();
        recipientId.setValue(getSoapHeader().getRecipientIdentifier().getValue());
        recipientId.setScheme(getSoapHeader().getRecipientIdentifier().getScheme());

        ParticipantIdentifierType senderId = new ParticipantIdentifierType();
        senderId.setValue(getSoapHeader().getSenderIdentifier().getValue());
        senderId.setScheme(getSoapHeader().getSenderIdentifier().getScheme());

        DocumentIdentifierType documentId = new DocumentIdentifierType();
        documentId.setValue(getSoapHeader().getDocumentIdentifier().getValue());
        documentId.setScheme(getSoapHeader().getDocumentIdentifier().getScheme());

        ProcessIdentifierType processId = new ProcessIdentifierType();
        processId.setValue(getSoapHeader().getProcessIdentifier().getValue());
        processId.setScheme(getSoapHeader().getProcessIdentifier().getScheme());

        /* Proceed to put information as headers in the header block */

        marshaller = JAXBContext.newInstance(String.class).createMarshaller();
        marshaller.marshal(objFactory.createMessageIdentifier(messageId),
                new DOMResult(header));

        JAXBElement auxChannelId = objFactory.createChannelIdentifier(channelId);
        auxChannelId.setNil(true);
        marshaller.marshal(auxChannelId,
                new DOMResult(header));

        marshaller = JAXBContext.newInstance(ParticipantIdentifierType.class).createMarshaller();
        marshaller.marshal(objFactory.createRecipientIdentifier(recipientId),
                new DOMResult(header));

        marshaller.marshal(objFactory.createSenderIdentifier(senderId),
                new DOMResult(header));

        marshaller = JAXBContext.newInstance(DocumentIdentifierType.class).createMarshaller();
        marshaller.marshal(objFactory.createDocumentIdentifier(documentId),
                new DOMResult(header));

        marshaller = JAXBContext.newInstance(ProcessIdentifierType.class).createMarshaller();
        marshaller.marshal(objFactory.createProcessIdentifier(processId),
                new DOMResult(header));
    }

    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    public void close(MessageContext context) {
    }
}
