package de.unistuttgart.quadrama.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PR;
import de.unistuttgart.ims.drama.api.CastFigure;
import de.unistuttgart.ims.drama.api.Drama;
import de.unistuttgart.ims.drama.api.FigureMention;
import de.unistuttgart.ims.drama.api.Speech;
import de.unistuttgart.ims.drama.api.Utterance;
import de.unistuttgart.ims.drama.util.DramaUtil;

@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
		"de.unistuttgart.ims.drama.api.Figure", "de.unistuttgart.ims.drama.api.Speech",
		"de.unistuttgart.ims.drama.api.Speaker", "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PR",
		"de.unistuttgart.ims.drama.api.Utterance" }, outputs = { "de.unistuttgart.ims.drama.api.FigureMention" })
public class FigureMentionDetection extends JCasAnnotator_ImplBase {

	Map<String, List<String>> firstPersonPronouns = new HashMap<String, List<String>>();
	String[] posExceptions = new String[] { "ART" };

	@Override
	public void initialize(final UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		try {
			firstPersonPronouns.put("de", IOUtils.readLines(
					getClass().getResourceAsStream("/pronouns/de/person1-sg.csv"), Charset.forName("UTF-8")));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		// we create indices with figure names, figure reference strings and
		// pronouns
		Map<String, CastFigure> figureMap = new HashMap<String, CastFigure>();
		for (CastFigure figure : JCasUtil.select(jcas, CastFigure.class)) {
			for (int i = 0; i < figure.getNames().size(); i++)
				figureMap.put(figure.getNames(i).toLowerCase(), figure);
		}
		List<String> pronouns = firstPersonPronouns.get(jcas.getDocumentLanguage());
		if (pronouns == null) {
			pronouns = new LinkedList<String>();
		}

		Drama d = JCasUtil.selectSingle(jcas, Drama.class);
		Pattern p;
		Matcher m;
		for (int i = 0; i < d.getCastList().size(); i++) {
			CastFigure cf = d.getCastList(i);
			for (int j = 0; j < cf.getNames().size(); j++) {
				String name = cf.getNames(j);

				p = Pattern.compile("\\b" + Pattern.quote(name) + "\\b", Pattern.CASE_INSENSITIVE);
				m = p.matcher(jcas.getDocumentText());
				while (m.find()) {
					if (!matches(jcas, m.start(), m.end())) {
						FigureMention fm = AnnotationFactory.createAnnotation(jcas, m.start(), m.end(),
								FigureMention.class);
						fm.setCastFigure(cf);
					}
				}
			}
		}

		for (Utterance utterance : JCasUtil.select(jcas, Utterance.class)) {
			Collection<CastFigure> figures = DramaUtil.getCastFigures(utterance);
			for (CastFigure currentFigure : figures) {
				for (Speech speech : JCasUtil.selectCovered(jcas, Speech.class, utterance)) {
					/*
					 * for (Token token : JCasUtil.selectCovered(Token.class,
					 * speech)) { String surface = token.getCoveredText(); if
					 * (figureMap.containsKey(surface.toLowerCase())) {
					 * FigureMention fm =
					 * AnnotationFactory.createAnnotation(jcas,
					 * token.getBegin(), token.getEnd(), FigureMention.class);
					 * fm.setCastFigure(figureMap.get(surface.toLowerCase())); }
					 * }
					 */
					if (figures.size() <= 1)
						for (PR pronoun : JCasUtil.selectCovered(jcas, PR.class, speech)) {
							if (pronouns.contains(pronoun.getCoveredText())) {
								AnnotationFactory.createAnnotation(jcas, pronoun.getBegin(), pronoun.getEnd(),
										FigureMention.class).setCastFigure(currentFigure);
							}
						}
				}
			}
		}

	}

	boolean matches(JCas jcas, int begin, int end) {
		Collection<POS> poss = JCasUtil.selectCovered(jcas, POS.class, begin, end);
		StringBuilder b = new StringBuilder();
		for (POS pos : poss) {
			b.append(pos.getPosValue()).append(' ');
		}
		String profile = b.toString().trim();
		return ArrayUtils.contains(posExceptions, profile);
	}

}
