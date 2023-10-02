package de.numcodex.feasibility_gui_backend.query;

import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.QueryResultLine;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatcher;
import de.numcodex.feasibility_gui_backend.query.obfuscation.QueryResultObfuscator;
import de.numcodex.feasibility_gui_backend.query.persistence.ResultRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.stream.Collectors;

import static de.numcodex.feasibility_gui_backend.query.persistence.ResultType.SUCCESS;

@Service
@RequiredArgsConstructor
public class QueryHandlerService {

    @Value("${app.lowerboundarypatientresult}")
    private int lowerboundarypatientresult;

    @NonNull
    private final QueryDispatcher queryDispatcher;

    @NonNull
    private final ResultRepository resultRepository;

    @NonNull
    private final QueryResultObfuscator queryResultObfuscator;

    public Long runQuery(StructuredQuery structuredQuery) throws QueryDispatchException {
        var queryId = queryDispatcher.enqueueNewQuery(structuredQuery);
        queryDispatcher.dispatchEnqueuedQuery(queryId);
        return queryId;
    }

    public int getMyResult(long queryId) {
        try {
            return queryDispatcher.myGetResultInQueryDispatcher(queryId);
        } catch (QueryDispatchException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public QueryResult getQueryResult(Long queryId) {
        var singleSiteResults = resultRepository.findByQueryAndStatus(queryId, SUCCESS);

        var resultLines = singleSiteResults.stream()
                .map(ssr -> QueryResultLine.builder()
                        .siteName(queryResultObfuscator.tokenizeSiteName(ssr))
                        .numberOfPatients(ssr.getResult())
                        .build())
                .collect((Collectors.toList()));
        Collections.shuffle(resultLines);


        int lowerBound = singleSiteResults.stream().map(result -> result.getResult()-result.getResult()%10).reduce(0, Integer::sum);
        int higherBound = singleSiteResults.stream().map(result -> result.getResult()+ (10-result.getResult()%10)).reduce(0, Integer::sum);

        StringBuilder totalMatchesInPopulation = new StringBuilder();
        totalMatchesInPopulation.append(lowerBound).append("-").append(higherBound);

        QueryResult queryResult = QueryResult.builder()
                .queryId(queryId)
                .totalNumberOfPatients(lowerBound)
                .totalNumberOfPatientsRange(totalMatchesInPopulation.toString())
                .build();

        if(resultLines.stream().anyMatch(queryResultLine -> queryResultLine.getNumberOfPatients() < (lowerboundarypatientresult))) {
            resultLines.forEach((queryResultLine) -> queryResultLine.setNumberOfPatients(0));
            queryResult.setResultLines(resultLines);
        } else {
            resultLines.forEach((queryResultLine) -> queryResultLine.setNumberOfPatients(queryResultLine.getNumberOfPatients()-(queryResultLine.getNumberOfPatients()%10)));
            queryResult.setResultLines(resultLines);
        }

        System.out.println("Query ID: " + queryId);
        return queryResult;
    }

    public void service_change_fhir_base_url(String num) {
        queryDispatcher.qd_change_fhir_base_url(num);
    }
}
