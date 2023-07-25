package de.numcodex.feasibility_gui_backend.query.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.sq2cql.PrintContext;
import de.numcodex.sq2cql.Translator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
/**
 * A translator for translating a {@link StructuredQuery} into its CQL representation.
 */
@RequiredArgsConstructor
class CqlQueryTranslator implements QueryTranslator {

    @NonNull
    private final Translator translator;

    @NonNull
    private final ObjectMapper jsonUtil;

    @Override
    public String translate(StructuredQuery query) throws QueryTranslationException {
        de.numcodex.sq2cql.model.structured_query.StructuredQuery structuredQuery;
        try {
            structuredQuery = jsonUtil.readValue(jsonUtil.writeValueAsString(query),
                    de.numcodex.sq2cql.model.structured_query.StructuredQuery.class);
        } catch (JsonProcessingException e) {
            throw new QueryTranslationException("cannot encode/decode structured query as JSON", e);
        }

        try {
            String my_return_string ="";
            // String my_return_string = translator.toCql(structuredQuery).print(PrintContext.ZERO);
            return my_return_string;
        } catch (Exception e) {
            throw new QueryTranslationException("cannot translate structured query to CQL format", e);
        }
    }

    // not implemented bc it is not needed
    @Override
    public void change_fhir_base_url(String num) {

    }

    // not implemented bc it is not needed
    @Override
    public int getTotalNumber(StructuredQuery query) {
        return 0;
    }
}
