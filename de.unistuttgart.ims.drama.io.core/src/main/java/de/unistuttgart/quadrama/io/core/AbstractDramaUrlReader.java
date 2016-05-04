package de.unistuttgart.quadrama.io.core;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;

import de.unistuttgart.ims.drama.api.Drama;

public abstract class AbstractDramaUrlReader extends
		JCasCollectionReader_ImplBase {
	public static final String PARAM_URL_LIST = "URL List";
	public static final String PARAM_INPUT_DIRECTORY = "Input Directory";
	public static final String PARAM_LANGUAGE = "Language";
	public static final String PARAM_CLEANUP = "Cleanup";

	@ConfigurationParameter(name = PARAM_INPUT_DIRECTORY, mandatory = false)
	String inputDirectory = null;

	@ConfigurationParameter(name = PARAM_URL_LIST, mandatory = false)
	String urlListFilename = null;

	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false,
			defaultValue = "de")
	String language = "de";

	@ConfigurationParameter(name = PARAM_CLEANUP, mandatory = false)
	boolean cleanUp = false;

	List<URL> urls = new LinkedList<URL>();
	int currentUrlIndex = 0;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		if (urlListFilename != null) {
			CSVParser r = null;
			try {
				r =
						new CSVParser(
								new FileReader(new File(urlListFilename)),
								CSVFormat.TDF);
				List<CSVRecord> records = r.getRecords();
				for (CSVRecord rec : records) {
					String s = rec.get(0);
					if (s.startsWith("/")) {
						urls.add(new File(s).toURI().toURL());
					} else {
						urls.add(new URL(s));
					}
				}
				getLogger().log(Level.FINE, "Found " + urls.size() + " URLs.");
			} catch (Exception e) {
				throw new ResourceInitializationException(e);
			} finally {
				IOUtils.closeQuietly(r);
			}
		} else if (inputDirectory != null) {
			File inputDir = new File(inputDirectory);
			File[] files = inputDir.listFiles(new FilenameFilter() {

				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}

			});
			try {
				for (File file : files) {
					urls.add(file.toURI().toURL());
				}
			} catch (Exception e) {
				throw new ResourceInitializationException(e);

			}
		} else {
			throw new ResourceInitializationException(
					"You need to specify either PARAM_INPUT_DIRECTORY or PARAM_URL_LIST",
					null);
		}

	}

	public boolean hasNext() throws IOException, CollectionException {
		return currentUrlIndex < urls.size();
	}

	public Progress[] getProgress() {
		return null;
	}

	@Override
	public void getNext(JCas jcas) throws IOException, CollectionException {
		URL url = urls.get(currentUrlIndex++);

		getLogger().debug("Processing url " + url);

		Drama drama = new Drama(jcas);
		drama.setDocumentId(String.valueOf(currentUrlIndex));
		// drama.setDocumentBaseUri("https://textgridlab.org/1.0/tgcrud-public/rest/");
		drama.setDocumentUri(url.toString());
		drama.addToIndexes();
		jcas.setDocumentLanguage(language);

		getNext(jcas, url.openStream(), drama);

		if (cleanUp) {
			DramaIOUtil.cleanUp(jcas);
		}
	}

	public abstract void getNext(JCas jcas, InputStream is, Drama drama)
			throws IOException, CollectionException;

}