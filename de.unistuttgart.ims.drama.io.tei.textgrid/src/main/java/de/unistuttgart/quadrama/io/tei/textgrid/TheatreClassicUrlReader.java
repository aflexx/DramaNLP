package de.unistuttgart.quadrama.io.tei.textgrid;

import static de.unistuttgart.quadrama.io.core.DramaIOUtil.select2Annotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.unistuttgart.ims.drama.api.Act;
import de.unistuttgart.ims.drama.api.ActHeading;
import de.unistuttgart.ims.drama.api.Author;
import de.unistuttgart.ims.drama.api.CastFigure;
import de.unistuttgart.ims.drama.api.Drama;
import de.unistuttgart.ims.drama.api.Figure;
import de.unistuttgart.ims.drama.api.FrontMatter;
import de.unistuttgart.ims.drama.api.MainMatter;
import de.unistuttgart.ims.drama.api.Scene;
import de.unistuttgart.ims.drama.api.SceneHeading;
import de.unistuttgart.ims.drama.api.Speaker;
import de.unistuttgart.ims.drama.api.Speech;
import de.unistuttgart.ims.drama.api.StageDirection;
import de.unistuttgart.ims.drama.api.Utterance;
import de.unistuttgart.ims.drama.util.UimaUtil;
import de.unistuttgart.ims.uimautil.AnnotationUtil;
import de.unistuttgart.quadrama.io.core.AbstractDramaUrlReader;
import de.unistuttgart.quadrama.io.core.GenericXmlReader;
import de.unistuttgart.quadrama.io.core.type.XMLElement;

public class TheatreClassicUrlReader extends AbstractDramaUrlReader {

	public static final String PARAM_TEI_COMPAT = "TEI compatibility";

	public static final String PARAM_STRICT = "strict";

	@ConfigurationParameter(name = PARAM_STRICT, mandatory = false, defaultValue = "false")
	boolean strict = false;

	@ConfigurationParameter(name = PARAM_TEI_COMPAT, mandatory = false, defaultValue = "false")
	boolean teiCompatibility = false;

	@Override
	public void getNext(final JCas jcas, InputStream file, Drama drama) throws IOException, CollectionException {

		GenericXmlReader gxr = new GenericXmlReader();
		gxr.setTextRootSelector(teiCompatibility ? null : "TEI > text");
		gxr.setPreserveWhitespace(teiCompatibility);

		// title
		gxr.addAction("titleStmt > title[type=main]", Drama.class, (d, e) -> d.setDocumentTitle(e.text()));

		// id
		gxr.addAction("publicationStmt > idno[type=cligs]", Drama.class, (d, e) -> d.setDocumentId(e.text()));

		// author
		gxr.addAction("author", (jc, e) -> {
			Author author = new Author(jcas);
			author.setName(e.select("name[type=full]").text());
			author.addToIndexes();
		});

		// date printed
		gxr.addAction("sourceDesc > bibl[type=print-source] > date", Drama.class,
				(d, e) -> d.setDatePrinted(Integer.valueOf(e.text())));

		// data premiere
		gxr.addAction("sourceDesc > bibl[type=performance-first] > date", Drama.class,
				(d, e) -> d.setDatePrinted(Integer.valueOf(e.text())));

		gxr.addMapping("front", FrontMatter.class);
		gxr.addMapping("body", MainMatter.class);

		// segmentation
		gxr.addMapping("div[type=act]", Act.class, (a, e) -> a.setRegular(true));
		gxr.addMapping("div[type=act] > head", ActHeading.class);

		gxr.addMapping("div[type=scene]", Scene.class, (a, e) -> a.setRegular(true));
		gxr.addMapping("div[type=scene] > head", SceneHeading.class);

		gxr.addMapping("castList > castItem > role", CastFigure.class, (cf, e) -> {
			List<String> nameList = new LinkedList<String>();
			List<String> xmlIdList = new LinkedList<String>();

			if (e.hasAttr("xml:id"))
				xmlIdList.add(e.attr("xml:id"));
			if (e.hasAttr("sex"))
				cf.setGender(e.attr("sex"));
			if (e.hasAttr("age"))
				cf.setAge(e.attr("age"));

			// gather names
			nameList.add(e.text());
			cf.setXmlId(UimaUtil.toStringArray(jcas, xmlIdList));
			cf.setNames(UimaUtil.toStringArray(jcas, nameList));
			cf.setDisplayName(cf.getNames(0));

		});

		gxr.addMapping("speaker", Speaker.class);
		gxr.addMapping("stage", StageDirection.class);
		gxr.addMapping("l", Speech.class);
		gxr.addMapping("p", Speech.class);
		gxr.addMapping("ab", Speech.class);

		gxr.addMapping("sp", Utterance.class, (u, e) -> {
			Collection<Speaker> speakers = JCasUtil.selectCovered(Speaker.class, u);
			for (Speaker sp : speakers) {
				String[] whos = e.attr("who").split(" ");
				sp.setXmlId(new StringArray(jcas, whos.length));
				sp.setCastFigure(new FSArray(jcas, whos.length));
				for (int i = 0; i < whos.length; i++) {
					// theatreclassique does not use # before ids
					String xmlid = whos[i];
					sp.setXmlId(i, xmlid);
					if (gxr.exists(xmlid)) {
						sp.setCastFigure(i, (CastFigure) gxr.getAnnotation(xmlid).getValue());
						u.setCastFigure((CastFigure) gxr.getAnnotation(xmlid).getValue());
					}
				}
			}
		});

		gxr.read(jcas, file);

		AnnotationUtil.trim(new ArrayList<Figure>(JCasUtil.select(jcas, Figure.class)));
		try {
			AnnotationUtil.trim(new ArrayList<Speech>(JCasUtil.select(jcas, Speech.class)));
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			AnnotationUtil.trim(new ArrayList<Utterance>(JCasUtil.select(jcas, Utterance.class)));
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		AnnotationUtil.trim(new ArrayList<Scene>(JCasUtil.select(jcas, Scene.class)));
		AnnotationUtil.trim(new ArrayList<Act>(JCasUtil.select(jcas, Act.class)));

	}

	@Deprecated
	private static void readCast(JCas jcas, Drama drama, Document doc) {
		Map<String, CastFigure> idFigureMap = new HashMap<String, CastFigure>();
		Elements castEntries = doc.select("castList > castItem > role");
		FSArray castListArray = new FSArray(jcas, castEntries.size());
		for (int i = 0; i < castEntries.size(); i++) {
			Element castEntry = castEntries.get(i);
			CastFigure figure = TEIUtil.parsePersonElement(jcas, castEntry);
			for (int j = 0; j < figure.getXmlId().size(); j++) {
				idFigureMap.put(figure.getXmlId(j), figure);
			}
			castListArray.set(i, figure);
		}
		drama.setCastList(castListArray);

		for (Speaker speaker : JCasUtil.select(jcas, Speaker.class)) {
			speaker.setCastFigure(new FSArray(jcas, speaker.getXmlId().size()));
			for (int i = 0; i < speaker.getXmlId().size(); i++)
				speaker.setCastFigure(i, idFigureMap.get(speaker.getXmlId(i)));
		}
	}

	@Deprecated
	public static void readActs(JCas jcas, Element root, Map<String, XMLElement> map, boolean strict) {
		for (Act a : select2Annotation(jcas, root, map, "div[type=act]", Act.class, null)) {
			a.setRegular(true);
		}

		select2Annotation(jcas, root, map, "div[type=act] > head", ActHeading.class, null);
	}

	/**
	 * Detect scenes. The following things are checked:
	 * <ol>
	 * <li>if they are explicitly marked with <code>type=scnee</code>, we take
	 * them and return.</li>
	 * <li>if Act annotations do exist in the JCas, we search for divs that have
	 * head annotations.</li>
	 * </ol>
	 * 
	 * @param jcas
	 * @param root
	 * @param map
	 */
	@Deprecated
	public static void readScenes(JCas jcas, Element root, Map<String, XMLElement> map, boolean strict) {
		select2Annotation(jcas, root, map, "div[type=scene]", Scene.class, null);
		select2Annotation(jcas, root, map, "div[type=scene] > head", SceneHeading.class, null);

		for (Scene scene : JCasUtil.select(jcas, Scene.class))
			scene.setRegular(true);
	}

	public static void readActsAndScenes(JCas jcas, Element root, Map<String, XMLElement> map, boolean strict) {
		readActs(jcas, root, map, strict);
		readScenes(jcas, root, map, strict);
	}

	@Deprecated
	public static <T extends TOP> T select2Feature(JCas jcas, Document doc, String cssQuery, Class<T> type,
			String featureName) {
		if (!doc.select(cssQuery).isEmpty()) {
			Type t = JCasUtil.getType(jcas, type);
			T fs = jcas.getCas().createFS(t);
			Feature f = t.getFeatureByBaseName(featureName);
			if (f.getRange().getName().equalsIgnoreCase("uima.cas.Integer")) {
				fs.setIntValue(f, Integer.valueOf(doc.select(cssQuery).first().text()));
			}
			fs.addToIndexes();
			return fs;
		}
		return null;
	}

}
