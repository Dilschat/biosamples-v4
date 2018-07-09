package uk.ac.ebi.biosamples.phenopackets_exportation_test;

import org.json.JSONException;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenopackets.schema.v1.PhenoPacket;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.ac.ebi.biosamples.model.ga4gh_model.*;
import uk.ac.ebi.biosamples.service.ga4ghService.BiosampleToGA4GHMapper;
import uk.ac.ebi.biosamples.service.ga4ghService.GeoLocationDataHelper;
import uk.ac.ebi.biosamples.service.ga4ghService.OLSDataRetriever;
import uk.ac.ebi.biosamples.service.phenopackets_exportation_service.BiosampleToPhenopacketExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class BiosampleToPhenopacketExporterUnitTest {
    @Mock
    Biosample biosample;
    @Mock
    Biocharacteristics biocharacteristic;
    @Mock
    Attributes attributes;
    @Mock
    AttributeValue attributeValue;
    @Mock
    ExternalIdentifier externalIdentifier;
    @Mock
    GeoLocation geoLocation;
    BiosampleToPhenopacketExporter exporter;
    @Mock
    BiosampleToGA4GHMapper mapper;
    @Mock
    Age age;
    @Mock
    OntologyTerm term;
    @Mock
    OLSDataRetriever retriever;

    public BiosampleToPhenopacketExporterUnitTest() {
        setupMock();
        mapper = new BiosampleToGA4GHMapper(new Biosample(new Attributes()), new GeoLocationDataHelper());
        exporter = new BiosampleToPhenopacketExporter(null, mapper, retriever);
    }



    @Test
    public void testMapBiosampleToPhenopacketBasic() throws IOException, JSONException {
        PhenoPacket phenoPacket = exporter.map(biosample);
        String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
        String expectedJson = new String(Files.readAllBytes(Paths.get("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/unit_test_cases/basicPhenopacketJson.json")));
        JSONAssert.assertEquals(expectedJson, phenpacketJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void testIndividualMapping() throws IOException, JSONException {
        when(biosample.getIndividual_id()).thenReturn(null);
        Biocharacteristics sex = createBiocharacteristicByDescription("sex");
        Biocharacteristics organism = createBiocharacteristicByDescription("organism");
        SortedSet<Biocharacteristics> biocharacteristics = new TreeSet<>();
        biocharacteristics.add(sex);
        biocharacteristics.add(organism);
        when(biosample.getBio_characteristic()).thenReturn(biocharacteristics);
        PhenoPacket phenoPacket = exporter.map(biosample);
        String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
        System.out.println(phenpacketJson);
        String expectedJson = new String(Files.readAllBytes(Paths.get("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/unit_test_cases/individualMappingTestPhenopacket.json")));
        JSONAssert.assertEquals(expectedJson, phenpacketJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void testDiseaseMapping() throws IOException, JSONException {
        when(biosample.getIndividual_id()).thenReturn(null);
        Biocharacteristics disease = createBiocharacteristicByDescription("disease");
        SortedSet<Biocharacteristics> biocharacteristics = new TreeSet<>();
        biocharacteristics.add(disease);
        when(biosample.getBio_characteristic()).thenReturn(biocharacteristics);
        PhenoPacket phenoPacket = exporter.map(biosample);
        String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
        System.out.println(phenpacketJson);
        String expectedJson = new String(Files.readAllBytes(Paths.get("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/unit_test_cases/diseaseTestPhenopacket.json")));
        JSONAssert.assertEquals(expectedJson, phenpacketJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void testBiocharacteristics() throws IOException, JSONException {
        when(biosample.getIndividual_id()).thenReturn(null);
        Biocharacteristics phenotype = createBiocharacteristicByDescription("phenotype");
        Biocharacteristics phenotype1 = createBiocharacteristicByDescription("phenotype1");
        SortedSet<Biocharacteristics> biocharacteristics = new TreeSet<>();
        biocharacteristics.add(phenotype);
        biocharacteristics.add(phenotype1);
        when(biosample.getBio_characteristic()).thenReturn(biocharacteristics);
        PhenoPacket phenoPacket = exporter.map(biosample);
        String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
        System.out.println(phenpacketJson);
        String expectedJson = new String(Files.readAllBytes(Paths.get("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/unit_test_cases/phenotypeTestPhenopacket.json")));
        JSONAssert.assertEquals(expectedJson, phenpacketJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    public Biocharacteristics createBiocharacteristicByDescription(String description){
        Biocharacteristics biocharacteristics = new Biocharacteristics();
        biocharacteristics.setDescription(description);
        SortedSet<OntologyTerm> bioTerm = new TreeSet<>();
        bioTerm.add(term);
        biocharacteristics.setOntology_terms(bioTerm);
        return biocharacteristics;
    }
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
        setupTerm();
        setupAge();
        setupBiocharacteristic();
        setupAttributeVlaue();
        setupAttributes();
        setupGeolocation();
        setupExternalIdentifier();
        setupOLSDataRetreiver();
        setupBiosample();
    }

    public void setupOLSDataRetreiver() {
        doNothing().when(retriever).readResourceInfoFromUrl(isA(String.class));
        doNothing().when(retriever).readOntologyJsonFromUrl(isA(String.class));
        when(retriever.getOntologyTermId()).thenReturn("term id");
        when(retriever.getOntologyTermLabel()).thenReturn("term label");
        when(retriever.getResourceId()).thenReturn("resource id");
        when(retriever.getResourceName()).thenReturn("resource name");
        when(retriever.getResourcePrefix()).thenReturn("resource prefix");
        when(retriever.getResourceUrl()).thenReturn("url");
        when(retriever.getResourceVersion()).thenReturn("version");


    }

    public void setupBiosample() {
        when(biosample.getName()).thenReturn("name");
        when(biosample.getId()).thenReturn("id");
        when(biosample.getDataset_id()).thenReturn("dataset_id");
        when(biosample.getDescription()).thenReturn("description");
        when(biosample.getIndividual_id()).thenReturn("individual-id");
        when(biosample.getReleasedDate()).thenReturn("2018-01-20T13:55:29.870Z");
        when(biosample.getUpdatedDate()).thenReturn("2018-01-20T13:55:29.870Z");
        when(biosample.getAttributes()).thenReturn(attributes);
        SortedSet<Biocharacteristics> biocharacteristics = new TreeSet<>();
        biocharacteristics.add(biocharacteristic);
        when(biosample.getBio_characteristic()).thenReturn(biocharacteristics);
        when(biosample.getLocation()).thenReturn(geoLocation);
        when(biosample.getIndividual_age_at_collection()).thenReturn(age);
        SortedSet<ExternalIdentifier> externalIdentifiers = new TreeSet<>();
        externalIdentifiers.add(externalIdentifier);
        when(biosample.getExternal_identifiers()).thenReturn(externalIdentifiers);
    }

    public void setupAge() {
        when(age.getAge()).thenReturn("0");
        when(age.getAge_class()).thenReturn(term);
    }

    public void setupBiocharacteristic() {
        when(biocharacteristic.getDescription()).thenReturn("description");
        SortedSet<OntologyTerm> biocharacteristics = new TreeSet<>();
        biocharacteristics.add(term);
        when(biocharacteristic.getOntology_terms()).thenReturn(biocharacteristics);
        when(biocharacteristic.getScope()).thenReturn("scope");
    }

    public void setupTerm() {
        when(term.getTerm_label()).thenReturn("term label");
        when(term.getTerm_id()).thenReturn("term id");
    }

    public void setupExternalIdentifier() {
        when(externalIdentifier.getIdentifier()).thenReturn("identifier");
        when(externalIdentifier.getRelation()).thenReturn("relation");
    }

    public void setupGeolocation() {
        when(geoLocation.getLabel()).thenReturn("location");
        when(geoLocation.getAltitude()).thenReturn(0.0);
        when(geoLocation.getLatitude()).thenReturn(0.0);
        when(geoLocation.getLongtitude()).thenReturn(0.0);
        when(geoLocation.getPrecision()).thenReturn("world");
    }

    public void setupAttributes() {
        TreeMap<String, List<AttributeValue>> attributesMap = new TreeMap();
        List<AttributeValue> attributeValues = new ArrayList<>();
        attributeValues.add(attributeValue);
        attributesMap.put("attribute", attributeValues);
        when(attributes.getAttributes()).thenReturn(attributesMap);
    }

    public void setupAttributeVlaue() {
        when(attributeValue.getType()).thenReturn("type");
        when(attributeValue.getValue()).thenReturn("typeValue");
    }

}