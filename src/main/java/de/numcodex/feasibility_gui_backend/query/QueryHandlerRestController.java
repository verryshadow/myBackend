package de.numcodex.feasibility_gui_backend.query;

import de.numcodex.feasibility_gui_backend.query.api.QueryResult;
import de.numcodex.feasibility_gui_backend.query.api.StructuredQuery;
import de.numcodex.feasibility_gui_backend.query.dispatch.QueryDispatchException;
import de.numcodex.feasibility_gui_backend.query.persistence.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/*
Rest Interface for the UI to send queries from the ui to the ui backend.
*/
@RequestMapping("api/v1/query-handler")
@RestController
@CrossOrigin
@Slf4j
public class QueryHandlerRestController {

  @Autowired
  private SmallResultRepository sresultRepository;

  private final QueryHandlerService queryHandlerService;
  private final String apiBaseUrl;

  public QueryHandlerRestController(QueryHandlerService queryHandlerService,
      @Value("${app.apiBaseUrl}") String apiBaseUrl) {
    this.queryHandlerService = queryHandlerService;
    this.apiBaseUrl = apiBaseUrl;
  }

  @SneakyThrows
  @PostMapping("run-query")
  public Response runQuery(
      @RequestBody StructuredQuery query, @Context HttpServletRequest httpServletRequest) {
    Long queryId;
    try {
      queryId = queryHandlerService.runQuery(query);
    } catch (QueryDispatchException e) {
      log.error("Error while running query", e);
      return Response.status(INTERNAL_SERVER_ERROR).build();
    }

    UriComponentsBuilder uriBuilder = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
            ? ServletUriComponentsBuilder.fromUriString(apiBaseUrl)
            : ServletUriComponentsBuilder.fromRequestUri(httpServletRequest);

    var uri = uriBuilder.replacePath("")
            .pathSegment("api", "v1", "query-handler", "result", String.valueOf(queryId))
            .build()
            .toUri();

    queryHandlerService.service_change_fhir_base_url("1");
    SmallResult result = new SmallResult();
    result.setSiteId(1L);
    result.setResult(queryHandlerService.getMyResult(queryId));
    result.setQueryId(queryId);
    sresultRepository.save(result);

    queryHandlerService.service_change_fhir_base_url("2");
    SmallResult result2 = new SmallResult();
    result2.setSiteId(2L);
    result2.setResult(queryHandlerService.getMyResult(queryId));
    result2.setQueryId(queryId);
    sresultRepository.save(result2);


/*    queryHandlerService.service_change_fhir_base_url("3");

    SmallResult result3 = new SmallResult();
    result3.setSiteId(3L);
    result3.setResult(queryHandlerService.getMyResult(queryId));
    result3.setQueryId(queryId);
    sresultRepository.save(result3);*/

    System.out.println("the end");
    System.out.println(Response.created(uri).build());
    return Response.created(uri).build();
  }

  @GetMapping(path = "/result/{id}")
  public QueryResult getQueryResult(@PathVariable("id") Long queryId) {
    return queryHandlerService.getQueryResult(queryId);
  }
}
