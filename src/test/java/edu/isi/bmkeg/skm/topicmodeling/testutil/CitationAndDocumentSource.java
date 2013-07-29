package edu.isi.bmkeg.skm.topicmodeling.testutil;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.isi.bmkeg.digitalLibrary.model.citations.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.model.citations.Journal;
import edu.isi.bmkeg.digitalLibrary.model.citations.Author;
import edu.isi.bmkeg.ftd.model.FTD;
import edu.isi.bmkeg.ftd.model.FTDSection;

/*
 * XML Structure:
 * 
 *  <Articles>
 *    <Article UniqueBmkegId="1234" >
 *      <Citation pubYear="1999" startPage="" endPage="" pages="" 
 *             volume="" issue="" pmid="" >
 *        <Title>
 *          article title
 *        </Title>
 *        <Authors>
 *          <AuthorFullName>
 *            Joe Doe
 *          </AuthorFullName>
 *          ...
 *        </Authors>
 *        <Journal abbr="">
 *          <JournalName>
 *          </JournalName>
 *        </Journal>
 *        <AbstractText>
 *        </AbstractText>
 *      </Citation>
 *      <Document>
 *        <Sections>
 *          <Section name="" spanStart="" spanEnd="" />
 *          ...
 *        </Sections>
 *        <ArticleText>
 *        </ArticleText>
 *      </Document>
 *    </Article>
 *    ...
 *  </Articles>
 */

public class CitationAndDocumentSource {

	XMLEventReader reader;
	XMLEvent nextEvent = null;
	
	public CitationAndDocumentSource(URL inputFile) throws Exception {
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		
		reader = factory.createXMLEventReader(inputFile.openStream());
		
		XMLEvent ev = reader.nextEvent();
		if (! ev.isStartDocument()) {
			throw new Exception("Missing StartDocument element");
		}
		
		readStartElem("Articles");
		
		// Caches next elem
		readNextTag();
	}

	public boolean hasNext() throws XMLStreamException {
		
		return isStartElemWithName(nextEvent, "Article");

	}
	
	public ArticleCitation getNext() throws Exception {
	
		checkEventIsStartElemWithName(nextEvent, "Article");
		
		ArticleCitation a = readCitation();

		FTD d = null;	
		
		readNextTag();
		
		if (isStartElemWithName(nextEvent, "Document")) {
			d = readDocument();
			
			readNextTag();
		}
		
		
		if (d != null) {
			d.setCitation(a);
			a.setFullText(d);
		}
		
		checkEventIsEndElem(nextEvent); // </Artilce> 

		// caches next XMLEvent
		readNextTag();	
		
		return a;
	}

	public ArticleCitation readCitation() throws Exception {

		ArticleCitation a = new ArticleCitation();
		
		StartElement startElem;
		
		startElem = readStartElem("Citation");
		
		String attrValue;
		
		if ((attrValue = readAttrValue(startElem,"pmid")) != null)
			a.setPmid(Integer.parseInt(attrValue));
		if ((attrValue = readAttrValue(startElem,"pubYear")) != null)
			a.setPubYear(Integer.parseInt(attrValue));
		if ((attrValue = readAttrValue(startElem,"pages")) != null)
			a.setPages(attrValue);
		if ((attrValue = readAttrValue(startElem,"volume")) != null)
			a.setVolume(attrValue);
		if ((attrValue = readAttrValue(startElem,"issue")) != null)
			a.setIssue(attrValue);

		// Reads Title
		readStartElem("Title");
		a.setTitle(reader.getElementText());
//		readEndElem();	//reads /<Title>
		
		// Reads Authors
		readStartElem("Authors");
		
		a.setAuthorList(new ArrayList<Author>());
		
		readNextTag();
		while (isStartElemWithName(nextEvent, "Author")) {

			Author p = new Author();
			p.setFullName(reader.getElementText());
//			readEndElem();	// reads </Author>		
			
			a.getAuthorList().add(p);
			
			readNextTag();
		}
		checkEventIsEndElem(nextEvent);	// </Authors>
		
		// Reads Journal
		startElem = readStartElem("Journal");
		
		Journal j = new Journal();
		j.setAbbr(readAttrValue(startElem, "abbr"));
		
		readNextTag();		
		if (isStartElemWithName(nextEvent, "JournalName")) {
			j.setJournalTitle(reader.getElementText());
//			readEndElem(); // </JournalName>
			
			readNextTag();
		}
		checkEventIsEndElem(nextEvent);	// </Journal>
		
		a.setJournal(j);

		// Reads Abstract
		readNextTag();		
		if (isStartElemWithName(nextEvent, "AbstractText")) {
			a.setAbstractText(reader.getElementText());
//			readEndElem(); // </AbstractText>
			
			readNextTag();
		}
		checkEventIsEndElem(nextEvent);	// </Citation>

		return a;
	}

	public FTD readDocument() throws Exception {

		checkEventIsStartElemWithName(nextEvent, "Document");
		
		FTD d = new FTD();
		
		d.setSections(new ArrayList<FTDSection>());

		// Reads Sections
		readStartElem("Sections");
		
		readNextTag();
		ArrayList<int[]> sectionSpans = new ArrayList<int[]>();

		while (isStartElemWithName(nextEvent, "Section")) {

			StartElement startElem = nextEvent.asStartElement();
			
			FTDSection s = new FTDSection();
						
			int startSpan = 0;
			int endSpan = 0;
			String attrValue;
			if ((attrValue = readAttrValue(startElem,"name")) != null)
				s.setSectionType(attrValue);
			if ((attrValue = readAttrValue(startElem,"spanStart")) != null)
				startSpan =  Integer.parseInt(attrValue);
			if ((attrValue = readAttrValue(startElem,"spanEnd")) != null)
				endSpan = Integer.parseInt(attrValue);

			sectionSpans.add(new int[] {startSpan, endSpan});
			
			readEndElem();	// reads </Section>			
			
			d.getSections().add(s);
			
			readNextTag();
		}
		checkEventIsEndElem(nextEvent);	// </Sections>
		
		readStartElem("ArticleText");
		String text = reader.getElementText();
		d.setText(text);

		//		readEndElem();	// </ArticleText>

		for (int i = 0; i < d.getSections().size(); i++) {
			FTDSection s = d.getSections().get(i);
			int[] span = sectionSpans.get(i);
			int sStart = span[0];
			int sEnd = span[1]; 
			if (sEnd >= text.length()) sEnd = text.length() - 1;
			s.setText(text.substring(sStart,sEnd));
		}

		readEndElem();	// </Document>
		
		return d;
	}
	
	private void readNextTag() throws XMLStreamException {
		nextEvent = reader.nextTag();
	}

	private void checkEventIsEndElem(XMLEvent event) throws Exception {
		if (! event.isEndElement()) {
			throw new Exception("Expected End Element but got: " + event);
		}
	}

	private void checkEventIsStartElemWithName(XMLEvent event, String tagName) throws Exception {
		if (! isStartElemWithName(event, tagName)) {
			throw new Exception("Expected " + tagName + " but got: " + event);
		}
	}

	private StartElement readStartElem(String tagName) throws XMLStreamException, Exception {
		readNextTag();
		
		if (! isStartElemWithName(nextEvent, tagName)) {
			throw new Exception("Expected " + tagName + " tag but got: " + nextEvent);
		}
		return nextEvent.asStartElement();
	}
	
	private boolean isStartElemWithName(XMLEvent event, String tagName) {
		return (event.isStartElement() && 
				event.asStartElement().getName().getLocalPart().equals(tagName)); 
	}
	
	private String readAttrValue(StartElement startElem, String attrName) {
		Attribute attr;
		String attrValue;
		if ((attr = startElem.getAttributeByName(QName.valueOf(attrName))) != null
				&& (attrValue = attr.getValue()).length() != 0) {
			return attrValue;
		}
		
		return null;
	}
	
	private void readEndElem() throws XMLStreamException, Exception {
		readNextTag();		
		checkEventIsEndElem(nextEvent);
	}
	
	public void close() throws XMLStreamException {
		
		reader.close();
		
	}


}
