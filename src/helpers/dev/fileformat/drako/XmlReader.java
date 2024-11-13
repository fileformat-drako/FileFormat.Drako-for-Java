package dev.fileformat.drako;


import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by lexchou on 9/26/2017.
 * .net compatible SAX reader
 */
@Internal
class XmlReader {
    static class XmlEvent {
        int type;
        String value = "";
        String localName = "";
        String namespaceURI = "";
        boolean emptyElement;
        Map<String, String> attributes = Collections.emptyMap();
        int depth;
        XmlEvent next;
    }
    private XMLStreamReader _reader;
    private int nextDepth = 0;
    private boolean eof = false;
    private XmlEvent event;
    public XmlReader(Reader reader)
    {
        try {
            this._reader = XMLInputFactory.newInstance().createXMLStreamReader(reader);
            event = new XmlEvent();
            event.next = nextEvent();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
    public XmlReader(String str)
    {
        this(new StringReader(str));
    }
    public XmlReader(Stream stream) throws IOException {
        this(new StreamReader(stream));
    }

    public boolean isEmptyElement()
    {
        return event == null ? false : event.emptyElement;

    }
    public int getDepth()
    {
        return event == null ? 0 : event.depth;
    }

    private XmlEvent nextEvent() throws XMLStreamException {
        if(!_reader.hasNext())
            return null;
        int event = _reader.next();
        return nextEvent(event);
    }

    private XmlEvent nextEvent(int event) throws XMLStreamException {
        XmlEvent e = new XmlEvent();
        e.depth = nextDepth;
        e.type = event;
        switch(event) {
            case XMLStreamConstants.START_ELEMENT: {
                e.localName = _reader.getLocalName();
                e.namespaceURI = _reader.getNamespaceURI();
                e.attributes = new HashMap<>();
                int numAttrs = _reader.getAttributeCount();
                for (int i = 0; i < numAttrs; i++) {
                    QName key = _reader.getAttributeName(i);
                    String value = _reader.getAttributeValue(i);
                    String attribute = key.getLocalPart();
                    if(key.getPrefix() != null && key.getPrefix().length() > 0)
                        attribute = StringUtils.concat(key.getPrefix(), ":", key.getLocalPart());
                    e.attributes.put(attribute, value);
                }
                int next = _reader.next();
                if (next != XMLStreamConstants.END_ELEMENT) {
                    nextDepth = e.depth + 1;
                    e.next = nextEvent(next);
                } else {
                    nextDepth = e.depth;
                    e.emptyElement = true;
                }
                break;
            }
            case XMLStreamConstants.END_ELEMENT:
                e.localName = _reader.getLocalName();
                e.namespaceURI = _reader.getNamespaceURI();
                nextDepth = e.depth - 1;
                e.depth = nextDepth;
                break;
            case XMLStreamConstants.END_DOCUMENT:
                eof = true;
                return null;
            case XMLStreamConstants.CDATA:
            case XMLStreamConstants.SPACE:
            case XMLStreamConstants.ENTITY_REFERENCE:
            case XMLStreamConstants.CHARACTERS: {
                StringBuilder sb = new StringBuilder();
                sb.append(_reader.getText());
                //next event may contains extra-long unfinished text
                while(true) {
                    int next = _reader.next();
                    if(next != e.type) {
                        e.next = nextEvent(next);
                        break;
                    } else {
                        sb.append(_reader.getText());
                    }
                }
                e.value = sb.toString();
                break;
            }
        }
        return e;
    }


    public boolean read()
            throws XMLStreamException
    {
        if(event == null)
            return false;
        if(event.next != null) {
            event = event.next;
            return true;
        }
        event = nextEvent();
        return event != null;
    }
    public int getNodeType()
    {
        return event == null ? 0 : event.type;
    }
    public void skip()
            throws XMLStreamException
    {
        int nodeType = getNodeType();
        if(nodeType == XMLStreamConstants.START_ELEMENT && !isEmptyElement()) {
            int d = getDepth();
            String nodeName = getLocalName();
            while (read()) {
                if (getDepth() == d && getNodeType() == XMLStreamConstants.END_ELEMENT && nodeName.equals(getLocalName())) {
                    break;
                }
            }
        }
        read();
    }
    public void readEndElement()
            throws XMLStreamException
    {
        if(getNodeType() != XMLStreamConstants.END_ELEMENT)
            throw new XMLStreamException("Invalid node type, must be end element");
        read();
    }
    public boolean isEOF()
    {
        return eof;

    }
    public String readContentAsString() throws XMLStreamException {
        int nodeType = getNodeType();
        switch(nodeType) {
            case XMLStreamConstants.START_ELEMENT:
            case XMLStreamConstants.DTD:
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
            case XMLStreamConstants.START_DOCUMENT:
                throw new XMLStreamException("Invalid content to read.");
        }
        String ret = getValue();
        read();
        return ret;
    }
    public String getNamespaceURI()
    {
        return event == null ? "" : event.namespaceURI;

    }
    public String getLocalName()
    {
        return event == null ? "" : event.localName;

    }
    public String getValue()
    {
        return event == null ? "" : event.value;

    }
    public String getAttribute(String key)
    {
        return event.attributes.get(key);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch(getNodeType()) {
            case XMLStreamConstants.END_ELEMENT:
                sb.append("EndElement");
                break;
            case XMLStreamConstants.START_ELEMENT:
                sb.append("Element");
                break;
            case XMLStreamConstants.CHARACTERS:
                sb.append("Text");
                break;
            case XMLStreamConstants.CDATA:
                sb.append("CDATA");
                break;
            case XMLStreamConstants.SPACE:
                sb.append("Whitespace");
                break;
            default:
                sb.append("????");
                break;
        }
        sb.append(" ");
        sb.append(getLocalName());
        sb.append(" ");
        sb.append(Integer.toString(getDepth()));
        return sb.toString();
    }
}
