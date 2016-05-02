package de.unistuttgart.quadrama.io.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import de.unistuttgart.quadrama.api.Drama;

@Deprecated
public abstract class AbstractDramaFileReader extends
JCasCollectionReader_ImplBase {
	public static final String PARAM_INPUT_DIRECTORY = "Input Directory";
	public static final String PARAM_LANGUAGE = "Language";
	public static final String PARAM_CLEANUP = "Cleanup";

	@ConfigurationParameter(name = PARAM_INPUT_DIRECTORY, mandatory = true)
	String inputDirectory;

	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false,
			defaultValue = "de")
	protected String language = "de";

	@ConfigurationParameter(name = PARAM_CLEANUP, mandatory = false)
	boolean cleanUp = false;

	protected File[] files;

	protected int current = 0;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		File inputDir = new File(inputDirectory);
		files = inputDir.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}

		});
	}

	public boolean hasNext() throws IOException, CollectionException {
		return current <= files.length - 1;
	}

	public Progress[] getProgress() {
		// TODO: implement
		return null;
	}

	@Override
	public void getNext(JCas jcas) throws IOException, CollectionException {
		File file = files[current++];
		getLogger().debug("Processing file " + file.getAbsolutePath());

		Drama drama = new Drama(jcas);
		drama.setDocumentId(file.getName());
		drama.addToIndexes();
		jcas.setDocumentLanguage(language);

		getNext(jcas, new FileInputStream(file), drama);

		if (cleanUp) {
			DramaIOUtil.cleanUp(jcas);
		}
	}

	public abstract void getNext(JCas jcas, InputStream file, Drama drama)
			throws IOException, CollectionException;

}
