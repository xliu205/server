### Project Details
- **Description**: The goal in this sprint is to develop a web API Server in Java that handles CSV and weather requests, and cache the weather data read from NWS API


### Design Choices
The structure of our application is as follows. The pkg `csv` defines a more robust CSV parser modified from Sprint1 (focusing on making defensive copies, improving error handling, etc). The pkg `server` defines the Spark web API Server. The endpoint handler packages define the handler related classes. Important classes are listed as follows.

- **LoadCSVHandler**: Load a CSV file if one is located at the specified path，we only allow loading files in `{projectPath}/data` directory
- **ViewCSVHandler**: Send back the entire CSV file's contents as a Json 2-dimensional array.
- **SearchCSVHandler**: Send back row matching the given search criteria (explained in _How to use_ section)
- **WeatherHandler**: Send back the temperature at the specified (U.S. only) location as reported by the U.S. National Weather Service.
- **NWSRequestConverter**: Convert the incoming request into the NWS request for hourly forecast.
- **CachedNWSRequestConverter**: `NWSRequestConverter` with cache, for points within 3 miles of existed points in the cache and request time within 1 hour, we fetch the corresponding data directly from cache.
- **GeneralResponse**: To keep the format simple, all responses are `GeneralResponse`, constructed with a `Map<String, Object>` object and then serialized to JSON 

### How to run
1. Clone the repository
2. Build and run main function in `Server.java`
3. Open `http://localhost:3232/` in your browser
4. View the next section to see the endpoints and queries you can use

### How to use
Send requests using `http://localhost:3232/{endpoint}?{queryname1}={query1}&{queryname2}={query2}...`
There are four endpoints supported:
- **loadcsv**: must have a query parameter `filepath` which contains the file path of the CSV file, a query parameter `header="true"/"false"` which indicates whether the CSV file has a header.
- **viewcsv**: no queries needed
- **searchcsv**: must have a query parameter `query` which contains the search query in the following format: `<and/or/not>(<value>;<column>;<name/idx>,...)`, which searches for the rows of the CSV where `<value>` is present in `<column>`, which uses name or index as identifier. `<column>;<name/idx>` are optional but they must come in pair. The search command supports "and", "or", "not" and nested queries.
- **weather**: must have a query parameter `lat` which contains the latitude, a query parameter `lon` which contains the longitude, and an optional parameter `datetime` which contains the time of the forecast the user is interested in, following the format `{yyyy}-{mm}-{dd}T{hh}:{mm}:{ss}-{timeZoneOffset}`

### Tests
- Unit tests
  - `NWSRequestConverterTest` tests the ability to convert a incoming weather request to weather data as well as error handling.
  - `CachedNWSRequestConverterTest` tests the ability to cache an incoming weather request to weather data as well as error handling, cache miss count, cache hit count, cache expire count, cache oversize count.
- Integration Test 
  - `IntegrationTest` tests the functionality of our Spark server --- whether it can respond to the incoming request to endpoints `weather`, `loadcsv`, `viewcsv`, `searchcsv` correctly. To test the robustness of our server --- it won't respond 500 with unexpected failure, we create many exceptions that cover all the possibilities of errors can test them correspondingly, including errors from the requests and errors from the servers.
