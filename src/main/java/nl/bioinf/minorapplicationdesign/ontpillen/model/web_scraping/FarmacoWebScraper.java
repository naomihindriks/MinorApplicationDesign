package nl.bioinf.minorapplicationdesign.ontpillen.model.web_scraping;

import nl.bioinf.minorapplicationdesign.ontpillen.model.data_storage.Drug;
import nl.bioinf.minorapplicationdesign.ontpillen.model.data_storage.DrugDao;
import nl.bioinf.minorapplicationdesign.ontpillen.model.data_storage.DrugSubstance;
import nl.bioinf.minorapplicationdesign.ontpillen.model.data_storage.content.ContentLeaf;
import nl.bioinf.minorapplicationdesign.ontpillen.model.data_storage.content.SideEffects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Class FarmacoWebScraper implements AbstractWebscraper. It fetches all drug
 * names from the drug dao and uses these to connect to the correct page on www.farmacotherapeutischkompas.nl.
 * It scrapes the side effects, description and interactions for each drug in the dao. This information
 * is saved to the drug object.
 * @author Larissa Bouwknegt, Jonathan Klimp, Naomi Hindriks
 */
@Component
@PropertySource("classpath:application.properties")
public class FarmacoWebScraper implements AbstractWebScraper {
    private DrugDao drugDao;
    private String basicUrl;
    private static final Logger LOGGER = LoggerFactory.getLogger(FarmacoWebScraper.class);

    private FarmacoWebScraper(@Value("${farmaco.basic.url}") String url) {
        this.basicUrl = url;
    }

    @Autowired
    public void setDrugDao(DrugDao drugDao) {
        this.drugDao = drugDao;
    }


    @Override
    public void parseHtml() throws IOException {
        LOGGER.info("Running parseHtml");
        List<DrugSubstance> drugSubstances = drugDao.getDrugSubstances();

        SSLHelper.bypassSSL();

        for (DrugSubstance drug : drugSubstances) {
            String drugName = drug.getName();
            Document doc = getConnection(drugName);
            Elements h2Tags = doc.getElementsByTag("h2");
            List<String> sideEffects = h2Tags.select(":contains(Bijwerkingen)").nextAll().select("p").eachText();
            List<String> drugDescription = h2Tags.select(":contains(Advies)").nextAll().eachText();
            List<String> interactions = h2Tags.select(":contains(interacties)").nextAll().eachText();
            Drug currentDrug = drugDao.getDrugByName(drugName);
            DrugSubstance drugsubstance = (DrugSubstance) currentDrug;

            drugsubstance.setDescriptionPractitioner(drugDescription);
            drugsubstance.setInteractionsPractitioner(interactions);

            this.addSideEffectsToDrug(drugsubstance, sideEffects);

            LOGGER.debug("Side effects for drug: " + drugName + "Side effects: " + sideEffects);
            LOGGER.debug("DrugDescription for drug: " + drugName + "Drug description: " + drugDescription);
            LOGGER.debug("Drug interactions for drug: " + drugName + "Drug interactions: " + interactions);
        }
    }


    private Document getConnection(String medicine) throws IOException {
        if (medicine.contains("(") | medicine.contains("/")){
            //Replaces any non word characters, including whitespaces
            medicine = medicine.replaceAll("\\W", "_");
        }
        if (medicine.contains("ï")){
            medicine =medicine.replace("ï", "i");
        }
        String completeUrl = (this.basicUrl + medicine.charAt(0) + "/" + medicine).toLowerCase(Locale.ROOT);
        LOGGER.debug("Connecting to: " + completeUrl);
        return Jsoup.connect(completeUrl).get();
    }

    /**
     * @param drugSubstance The drug to add the side effect to
     * @param content A list store as content. If the type is PARAGRAPH the items in the list will be separate paragraphs,
     */
    private void addSideEffectsToDrug(DrugSubstance drugSubstance, List<String> content) {
//        Get the SideEffects Object that belongs to specific drug
        SideEffects sideEffectsOfDrug = drugSubstance.getSideEffects();

//        Create a ContentLeaf object and store the scraped content in it
        ContentLeaf sideEffectContent = new ContentLeaf();
        sideEffectContent.setContentType("PARAGRAPH");
        sideEffectContent.setContent(content);

//        Add the newly created SideEffectContent to the SideEffect of the drug
        sideEffectsOfDrug.addSideEffectPractitioner("SideEffectsFk", sideEffectContent);
    }
}