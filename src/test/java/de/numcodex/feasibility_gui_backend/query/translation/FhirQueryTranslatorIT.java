package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import de.numcodex.feasibility_gui_backend.common.api.Unit;
import de.numcodex.feasibility_gui_backend.common.api.Criterion;
import de.numcodex.feasibility_gui_backend.query.api.AttributeFilter;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.api.TimeRestriction;
import de.numcodex.feasibility_gui_backend.query.api.ValueFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_EQUAL;
import static de.numcodex.feasibility_gui_backend.common.api.Comparator.GREATER_THAN;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.CONCEPT;
import static de.numcodex.feasibility_gui_backend.query.api.ValueFilterType.QUANTITY_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.testcontainers.containers.BindMode.READ_ONLY;

@Tag("query")
@Tag("translation")
@Disabled("Until we have a version of Flare that actually supports translating a structured query v2.")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = QueryTranslatorSpringConfig.class,
        properties = {
                "app.cqlTranslationEnabled=true",
                "app.fhirTranslationEnabled=false",
                "app.mappingsFile=./ontology/codex-term-code-mapping.json",
                "app.conceptTreeFile=./ontology/codex-code-tree.json"
        }
)
@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
public class FhirQueryTranslatorIT {

    @Autowired
    @Qualifier("translation")
    private ObjectMapper jsonUtil;

    private FhirQueryTranslator fhirQueryTranslator;

    // Note that Flare does not come with a health endpoint. Thus, we will simply check if we get a response of any
    // kind in order to assume that the server is up rendering the container ready.
    @Container
    private final GenericContainer<?> flare = new GenericContainer<>(DockerImageName.parse("ghcr.io/num-codex/codex-flare:0.0.8"))
            .withExposedPorts(5111)
            .withFileSystemBind("ontology/codex-code-tree.json", "/opt/flare/src/query_parser/codex/codex-code-tree.json", READ_ONLY)
            .withFileSystemBind("ontology/codex-term-code-mapping.json", "/opt/flare/src/query_parser/codex/codex-mapping.json", READ_ONLY)
            .waitingFor(Wait.forHttp("/")
                    .forStatusCodeMatching(c -> c >= 200 && c <= 500))
            .withStartupAttempts(5);

    @BeforeEach
    public void setUp() {
        var flareRootUri = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(flare.getHost())
                .port(flare.getFirstMappedPort())
                .build()
                .toString();

        var client = new RestTemplateBuilder()
                .rootUri(flareRootUri)
                .build();

        fhirQueryTranslator = new FhirQueryTranslator(client, jsonUtil);
    }

    @Test
    public void testTranslate() {
        var bodyWeightTermCode = new TermCode();
        bodyWeightTermCode.setSystem("http://snomed.info/sct");
        bodyWeightTermCode.setDisplay("Body weight (observable entity)");
        bodyWeightTermCode.setCode("27113001");
        bodyWeightTermCode.setVersion("v1");

        var kgUnit = new Unit();
        kgUnit.setCode("kg");
        kgUnit.setDisplay("kilogram");

        var bodyWeightValueFilter = new ValueFilter();
        bodyWeightValueFilter.setType(QUANTITY_COMPARATOR);
        bodyWeightValueFilter.setQuantityUnit(kgUnit);
        bodyWeightValueFilter.setComparator(GREATER_EQUAL);
        bodyWeightValueFilter.setValue(50.0);

        var hasBmiGreaterThanFifty = new Criterion();
        hasBmiGreaterThanFifty.setTermCodes(new ArrayList<>(List.of(bodyWeightTermCode)));
        hasBmiGreaterThanFifty.setValueFilter(bodyWeightValueFilter);

        var testQuery = new StructuredQuery();
        testQuery.setVersion(URI.create("http://to_be_decided.com/draft-2/schema#"));
        testQuery.setInclusionCriteria(List.of(List.of(hasBmiGreaterThanFifty)));

        @SuppressWarnings("unused")
        var translationResult = assertDoesNotThrow(() -> fhirQueryTranslator.translate(testQuery));
        // TODO: add assertions!
    }

    @Test
    public void testTranslate_SupportsTimeRestrictions() {
        var dementiaTermCode = new TermCode();
        dementiaTermCode.setCode("F00");
        dementiaTermCode.setSystem("http://fhir.de/CodeSystem/dimdi/icd-10-gm");
        dementiaTermCode.setDisplay("F00");

        var hasDementia = new Criterion();
        hasDementia.setTermCodes(new ArrayList<>(List.of(dementiaTermCode)));

        var psychologicalDysfunctionTermCode = new TermCode();
        psychologicalDysfunctionTermCode.setCode("F09");
        psychologicalDysfunctionTermCode.setSystem("http://fhir.de/CodeSystem/dimdi/icd-10-gm");
        psychologicalDysfunctionTermCode.setDisplay("F09");

        var timeRestriction = new TimeRestriction();
        timeRestriction.setAfterDate("2021-09-09");
        timeRestriction.setBeforeDate("2021-10-09");

        var hasPsychologicalDysfunction = new Criterion();
        hasPsychologicalDysfunction.setTermCodes(new ArrayList<>(List.of(psychologicalDysfunctionTermCode)));
        hasPsychologicalDysfunction.setTimeRestriction(timeRestriction);

        var testQuery = new StructuredQuery();
        testQuery.setVersion(URI.create("http://to_be_decided.com/draft-2/schema#"));
        testQuery.setInclusionCriteria(List.of(List.of(hasDementia, hasPsychologicalDysfunction)));

        @SuppressWarnings("unused")
        var translationResult = assertDoesNotThrow(() -> fhirQueryTranslator.translate(testQuery));
        // TODO: add assertions!
    }

    @Test
    public void testTranslate_SupportsAttributeFilters() {
        var ageTermCode = new TermCode();
        ageTermCode.setCode("30525-0");
        ageTermCode.setSystem("http://loinc.org");
        ageTermCode.setDisplay("Alter");

        var yearUnit = new Unit();
        yearUnit.setCode("a");
        yearUnit.setDisplay("Jahr");

        var ageValueFilter = new ValueFilter();
        ageValueFilter.setType(QUANTITY_COMPARATOR);
        ageValueFilter.setQuantityUnit(yearUnit);
        ageValueFilter.setComparator(GREATER_THAN);
        ageValueFilter.setValue(18.0);

        var olderThanEighteen = new Criterion();
        olderThanEighteen.setTermCodes(new ArrayList<>(List.of(ageTermCode)));
        olderThanEighteen.setValueFilter(ageValueFilter);

        var bodyTemperatureTermCode = new TermCode();
        bodyTemperatureTermCode.setCode("8310-5");
        bodyTemperatureTermCode.setSystem("http://loinc.org");
        bodyTemperatureTermCode.setDisplay("Körpertemperatur");

        var axillaryMeasureMethod = new TermCode();
        axillaryMeasureMethod.setCode("LA9370-3");
        axillaryMeasureMethod.setSystem("http://loinc.org");
        axillaryMeasureMethod.setDisplay("Axillary");

        var method = new TermCode();
        method.setCode("method");
        method.setSystem("abide");
        method.setDisplay("method");

        var axillaryMeasured = new AttributeFilter();
        axillaryMeasured.setType(CONCEPT);
        axillaryMeasured.setSelectedConcepts(List.of(axillaryMeasureMethod));
        axillaryMeasured.setAttributeCode(method);

        var bodyTemperatureAxillaryMeasured = new Criterion();
        bodyTemperatureAxillaryMeasured.setTermCodes(new ArrayList<>(List.of(bodyTemperatureTermCode)));
        bodyTemperatureAxillaryMeasured.setAttributeFilters(new ArrayList<>(List.of(axillaryMeasured)));

        var testQuery = new StructuredQuery();
        testQuery.setVersion(URI.create("http://to_be_decided.com/draft-2/schema#"));
        testQuery.setInclusionCriteria(List.of(List.of(olderThanEighteen)));
        testQuery.setExclusionCriteria(List.of(List.of(bodyTemperatureAxillaryMeasured)));

        @SuppressWarnings("unused")
        var translationResult = assertDoesNotThrow(() -> fhirQueryTranslator.translate(testQuery));
        // TODO: add assertions!
    }
}
