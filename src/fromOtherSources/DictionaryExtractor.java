package fromOtherSources;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.parsers.Char;
import basics.BaseTheme;
import basics.Fact;
import basics.FactComponent;
import basics.N4Reader;
import basics.Theme;
import extractors.FileExtractor;
import extractors.MultilingualExtractor;

/**
 * YAGO2s - MultilingualDictionariesExtractor
 * 
 * Extracts inter-language links from Wikidata and builds dictionaries.
 * 
 * @author Farzaneh Mahdisoltani
 * 
 */

public class DictionaryExtractor extends FileExtractor {

	/** Output theme */
	public static final BaseTheme ENTITY_DICTIONARY = new BaseTheme(
			"entityDictionary",
			"Maps a foreign entity to a YAGO entity. Data from (http://http://www.wikidata.org/).");

	/** Words for "category" in different languages */
	public static final Theme CATEGORYWORDS = new Theme("categoryWords",
			"Words for 'category' in different languages.");

	/** Translations of infobox templates */
	public static final BaseTheme INFOBOX_TEMPLATE_DICTIONARY = new BaseTheme(
			"infoboxTemplateDictionary",
			"Maps a foreign infobox template name to the English name.");

	/** Translations of categories */
	public static final BaseTheme CATEGORY_DICTIONARY = new BaseTheme(
			"categoryDictionary",
			"Maps a foreign category name to the English name.");

	public DictionaryExtractor(File inputFolder) {
		super(inputFolder.isFile() ? inputFolder : new File(inputFolder,
				"wikidata.rdf"));
		if (!inputData.exists())
			throw new RuntimeException("File not found: " + inputData);
	}

	@Override
	public Set<Theme> input() {
		return Collections.emptySet();
	}

	@Override
	public Set<Theme> output() {
		Set<Theme> result = new HashSet<Theme>();
		result.add(CATEGORYWORDS);
		result.addAll(CATEGORY_DICTIONARY
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		result.addAll(INFOBOX_TEMPLATE_DICTIONARY
				.inLanguages(MultilingualExtractor.wikipediaLanguages));
		return (result);
	}

	/** Returns the most English language in the set, or NULL */
	public static String mostEnglishLanguage(Collection<String> langs) {
		for (int i = 0; i < MultilingualExtractor.wikipediaLanguages.size(); i++) {
			if (langs.contains(MultilingualExtractor.wikipediaLanguages.get(i)))
				return (MultilingualExtractor.wikipediaLanguages.get(i));
		}
		return (null);
	}

	@Override
	public void extract() throws Exception {
		Announce.doing("Copying language links");
		Announce.message("Input file is", inputData);

		// Categories for which we have already translated the word "category"
		Set<String> categoryWordLanguages = new HashSet<>();

		N4Reader nr = new N4Reader(inputData);
		// Maps a language such as "en" to the name in that language
		Map<String, String> language2name = new HashMap<String, String>();
		while (nr.hasNext()) {
			Fact f = nr.next();
			// Record a new name in the map
			if (f.getRelation().endsWith("/inLanguage>")) {
				String lan = FactComponent.stripQuotes(f.getObject());
				if (!MultilingualExtractor.wikipediaLanguages.contains(lan))
					continue;
				language2name.put(lan, FactComponent.stripPrefix(Char
						.decodePercentage(f.getSubject())));
			} else if (f.getArg(2).endsWith("#Item>")
					&& !language2name.isEmpty()) {
				// New item starts, let's flush out the previous one
				String mostEnglishLan = mostEnglishLanguage(language2name
						.keySet());
				if (mostEnglishLan != null) {
					String mostEnglishName = language2name.get(mostEnglishLan);
					for (String lan : language2name.keySet()) {
						ENTITY_DICTIONARY.inLanguage(lan).write(
								new Fact(FactComponent.forForeignYagoEntity(
										language2name.get(lan), lan),
										"<_hasTranslation>", FactComponent
												.forForeignYagoEntity(
														mostEnglishName,
														mostEnglishLan)));
					}
					if (mostEnglishLan.equals("en")
							&& mostEnglishName.startsWith("Category:")) {
						for (String lan : language2name.keySet()) {
							String catword = language2name.get(lan);
							int cutpos = catword.indexOf(':');
							if (cutpos == -1)
								continue;
							String name = catword.substring(cutpos + 1);
							catword = catword.substring(cutpos);
							if (!categoryWordLanguages.contains(lan)) {
								CATEGORYWORDS.write(new Fact(FactComponent
										.forString(lan), "<_hasCategoryWord>",
										FactComponent.forString(catword)));
							}
							CATEGORY_DICTIONARY
									.inLanguage(lan)
									.write(new Fact(
											FactComponent
													.forForeignWikiCategory(
															name, lan),
											"<_hasTranslation>" + ">",
											FactComponent
													.forWikiCategory(mostEnglishName
															.substring(8))));
						}
					}
					if (mostEnglishLan.equals("en")
							&& mostEnglishName.startsWith("Template:Infobox_")) {
						for (String lan : language2name.keySet()) {
							String name = language2name.get(lan);
							int cutpos = name.indexOf('_');
							if (cutpos == -1)
								continue;
							name = name.substring(cutpos + 1);
							INFOBOX_TEMPLATE_DICTIONARY.inLanguage(lan).write(
									new Fact(FactComponent
											.forStringWithLanguage(name, lan),
											"<_hasTranslation>", FactComponent
													.forString(mostEnglishName
															.substring(17))));
						}
					}
				}
				language2name.clear();
			}
		}
		nr.close();
	}

	public static void main(String[] args) {
		// try {
		// new InterLanguageLinks(new File("D:/wikidata.rdf"))
		// .extract(new File("D:/data2/yago2s/"), "test");
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		try {
			new DictionaryExtractor(new File("./data/wikidata.rdf")).extract(
					new File("../"), "test");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}