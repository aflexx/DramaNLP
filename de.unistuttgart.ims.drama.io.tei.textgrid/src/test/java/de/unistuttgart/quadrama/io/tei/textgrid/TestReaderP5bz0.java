package de.unistuttgart.quadrama.io.tei.textgrid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.unistuttgart.ims.drama.api.Act;
import de.unistuttgart.ims.drama.api.ActHeading;
import de.unistuttgart.ims.drama.api.Drama;
import de.unistuttgart.ims.drama.api.FrontMatter;
import de.unistuttgart.ims.drama.api.MainMatter;
import de.unistuttgart.ims.drama.api.Scene;
import de.unistuttgart.ims.drama.api.SceneHeading;

public class TestReaderP5bz0 {
	static CollectionReaderDescription description;
	static JCas jcas;

	@BeforeClass
	public void setUp() throws ResourceInitializationException {
		description = CollectionReaderFactory.createReaderDescription(TextgridTEIUrlReader.class,
				TextgridTEIUrlReader.PARAM_INPUT, "src/test/resources/textgridFiles/p5bz.0.xml");
		AggregateBuilder b = new AggregateBuilder();
		if (TestGenerics.debug)
			b.add(AnalysisEngineFactory.createEngineDescription(XmiWriter.class, XmiWriter.PARAM_TARGET_LOCATION,
					"target/doc"));
		jcas = SimplePipeline.iteratePipeline(description, b.createAggregateDescription()).iterator().next();
	}

	@Test
	public void testGeneral() {
		assertEquals("p5bz.0", JCasUtil.selectSingle(jcas, Drama.class).getDocumentId());

		TestGenerics.checkSanity(jcas);

		assertNotNull(JCasUtil.selectSingle(jcas, FrontMatter.class));
		assertNotNull(JCasUtil.selectSingle(jcas, MainMatter.class));

	}

	@Test
	public void testActsAndScenes() {
		assertTrue(JCasUtil.exists(jcas, Act.class));
		assertEquals(5, JCasUtil.select(jcas, Act.class).size());
		assertEquals(7, JCasUtil.select(jcas, Scene.class).size());

		assertEquals(5, JCasUtil.select(jcas, ActHeading.class).size());
		assertEquals(7, JCasUtil.select(jcas, SceneHeading.class).size());
	}

}
