package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * A translator for translating a {@link StructuredQuery} into its FHIR search query format.
 */
@RequiredArgsConstructor
class FhirQueryTranslator implements QueryTranslator {

    private static final String FLARE_QUERY_TRANSLATE_ENDPOINT_PATH = "/query-translate";
    private static final String FLARE_QUERY_TRANSLATE_ENDPOINT_PATH2 = "/query-sync";

    private static final String FLARE_QUERY_CHANGE_FHIR_SERVER_BASE_URL = "/change_server_base_url/";
    private static final String FLARE_QUERY_TRANSLATE_CONTENT_TYPE = "CODEX/json";
    private static final String FLARE_QUERY_TRANSLATE_ACCEPT = "Result";

    // TODO: this one should be replaced with a WebClient instance for asynchronous translation support.
    //       However, this will require changes to the interface as well. Additional changes will propagate
    //       upstream. This would be too big of a change. Because of this and Flare being rewritten and potentially
    //       being present as a library (no client needed anymore), we don't pursue this change.
    @NonNull
    private final RestTemplate client;

    @NonNull
    private final ObjectMapper jsonUtil;

    @Override
    public String translate(StructuredQuery query) throws QueryTranslationException {

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.putAll(Map.of(
                // Done: Resolve this with the Flare team. This is NOT the header to be used.
                //       The accept encoding header should not change the content itself.
                //       Thus, it's mainly used for compression algorithms.
                HttpHeaders.ACCEPT, List.of(FLARE_QUERY_TRANSLATE_ACCEPT),
                HttpHeaders.CONTENT_TYPE, List.of(FLARE_QUERY_TRANSLATE_CONTENT_TYPE)
        ));

        try {
            HttpEntity<String> request = new HttpEntity<>(jsonUtil.writeValueAsString(query), requestHeaders);
            // <{"version":"http://to_be_decided.com/draft-1/schema#","inclusionCriteria":[[{"termCodes":[{"code":"76689-9","system":"http://loinc.org","display":"Sex assigned at birth"}],"valueFilter":{"type":"concept","selectedConcepts":[{"code":"female","system":"http://hl7.org/fhir/administrative-gender","display":"Female"}]}}]]},[Content-Type:"application/json", Accept-Encoding:"CSQ"]>
            // <{"version":"http://to_be_decided.com/draft-1/schema#","inclusionCriteria":[[{"termCodes":[{"code":"76689-9","system":"http://loinc.org","display":"Sex assigned at birth"}],"valueFilter":{"type":"concept","selectedConcepts":[{"code":"female","system":"http://hl7.org/fhir/administrative-gender","display":"Female"}]}}]]},[Content-Type:"application/json", Accept-Encoding:"CSQ"]>
            return client.postForObject(FLARE_QUERY_TRANSLATE_ENDPOINT_PATH, request, String.class);
        } catch (JsonProcessingException e) {
            throw new QueryTranslationException("cannot encode structured query as JSON", e);
        } catch (RestClientException e) {
            throw new QueryTranslationException("cannot translate structured query in FHIR search format using Flare", e);
        }
    }

    /*
    return a string with the number of patients
     */
    public int getTotalNumber(StructuredQuery query) {

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.putAll(Map.of(
                HttpHeaders.ACCEPT, List.of(FLARE_QUERY_TRANSLATE_ACCEPT),
                HttpHeaders.CONTENT_TYPE, List.of(FLARE_QUERY_TRANSLATE_CONTENT_TYPE)
        ));

        HttpEntity<String> request = null;
        try {
            request = new HttpEntity<>(jsonUtil.writeValueAsString(query), requestHeaders);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String response_sync = client.postForObject(FLARE_QUERY_TRANSLATE_ENDPOINT_PATH2, request, String.class);
        int response_sync_int = Integer.parseInt(response_sync);
        return response_sync_int;
    }

    public void change_fhir_base_url(String num) {
        String url = FLARE_QUERY_CHANGE_FHIR_SERVER_BASE_URL + num;
        client.getForEntity(url, String.class);
    }

}
