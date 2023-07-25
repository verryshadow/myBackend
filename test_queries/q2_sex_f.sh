curl --location --request POST 'http://localhost:8090/api/v1/query-handler/run-query' \
--header 'Content-Type: application/json' \
--data-raw '{
  "version": "http://to_be_decided.com/draft-1/schema#",
  "display": "",
  "inclusionCriteria": [
    [
      {
        "termCodes": [
          {
            "code": "76689-9",
            "system": "http://loinc.org",
            "display": "Sex assigned at birth"
          }
        ],
        "valueFilter": {
          "selectedConcepts": [
            {
              "code": "female",
              "system": "http://hl7.org/fhir/administrative-gender",
              "display": "Female"
            }
          ],
          "type": "concept"
        }
      }
    ]
  ]
}'
