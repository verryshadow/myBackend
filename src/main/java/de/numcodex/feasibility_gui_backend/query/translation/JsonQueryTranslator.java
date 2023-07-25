package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A query translator for translating a {@link StructuredQuery} into its JSON representation.
 * Thus, it is less of a translator than a serializer.
 */
@RequiredArgsConstructor
class JsonQueryTranslator implements QueryTranslator {

    @NonNull
    private ObjectMapper jsonUtil;

    @Override
    public String translate(StructuredQuery query) throws QueryTranslationException {
        try {
            String my_result = jsonUtil.writeValueAsString(query);
            return my_result;
        } catch (JsonProcessingException e) {
            throw new QueryTranslationException("cannot encode structured query as JSON", e);
        }
    }

    // not implemented bc it is not needed
    @Override
    public int getTotalNumber(StructuredQuery query) {
        return 0;
    }

    // not implemented bc it is not needed

    @Override
    public void change_fhir_base_url(String num) {

    }
}
