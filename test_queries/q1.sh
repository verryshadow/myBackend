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
            "code": "424144002",
            "system": "http://snomed.info/sct",
            "display": "Current chronological age (observable entity)"
          }
        ],
        "valueFilter": {
          "selectedConcepts": [],
          "type": "quantity-range",
          "unit": {
            "code": "",
            "display": ""
          },
          "minValue": 0,
          "maxValue": 200
        }
      }
    ]
  ]
}'
