package com.example.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class OpenMeteoService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // WMO Weather interpretation code mappings (Description, Emoji)
    private static final Map<Integer, WmoCodeInfo> WMO_CODE_MAP = Map.ofEntries(
        Map.entry(0, new WmoCodeInfo("Clear sky", "☀️")),
        Map.entry(1, new WmoCodeInfo("Mainly clear", "🌤️")),
        Map.entry(2, new WmoCodeInfo("Partly cloudy", "⛅")),
        Map.entry(3, new WmoCodeInfo("Overcast", "☁️")),
        Map.entry(45, new WmoCodeInfo("Fog", "🌫️")),
        Map.entry(48, new WmoCodeInfo("Depositing rime fog", "🌫️")),
        Map.entry(51, new WmoCodeInfo("Light drizzle", "🌦️")),
        Map.entry(53, new WmoCodeInfo("Moderate drizzle", "🌦️")),
        Map.entry(55, new WmoCodeInfo("Dense drizzle", "🌧️")),
        Map.entry(56, new WmoCodeInfo("Light freezing drizzle", "🌧️❄️")),
        Map.entry(57, new WmoCodeInfo("Dense freezing drizzle", "🌧️❄️")),
        Map.entry(61, new WmoCodeInfo("Slight rain", "🌧️")),
        Map.entry(63, new WmoCodeInfo("Moderate rain", "🌧️")),
        Map.entry(65, new WmoCodeInfo("Heavy rain", "🌧️⛈️")),
        Map.entry(66, new WmoCodeInfo("Light freezing rain", "🌧️❄️")),
        Map.entry(67, new WmoCodeInfo("Heavy freezing rain", "🌧️❄️")),
        Map.entry(71, new WmoCodeInfo("Slight snow fall", "🌨️")),
        Map.entry(73, new WmoCodeInfo("Moderate snow fall", "🌨️")),
        Map.entry(75, new WmoCodeInfo("Heavy snow fall", "❄️🌨️")),
        Map.entry(77, new WmoCodeInfo("Snow grains", "❄️")),
        Map.entry(80, new WmoCodeInfo("Slight rain showers", "🌦️")),
        Map.entry(81, new WmoCodeInfo("Moderate rain showers", "🌦️")),
        Map.entry(82, new WmoCodeInfo("Violent rain showers", "⛈️")),
        Map.entry(85, new WmoCodeInfo("Slight snow showers", "🌨️")),
        Map.entry(86, new WmoCodeInfo("Heavy snow showers", "❄️🌨️")),
        Map.entry(95, new WmoCodeInfo("Thunderstorm", "⛈️")),
        Map.entry(96, new WmoCodeInfo("Thunderstorm with slight hail", "⛈️🌨️")),
        Map.entry(99, new WmoCodeInfo("Thunderstorm with heavy hail", "⛈️🌨️"))
    );

    public record WmoCodeInfo(String description, String emoji) {}

    public OpenMeteoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private WmoCodeInfo getWmoDescription(Integer code) {
        if (code == null) {
            return new WmoCodeInfo("Unknown", "❓");
        }
        return WMO_CODE_MAP.getOrDefault(code, new WmoCodeInfo("Weather code " + code, "🌡️"));
    }

    public JsonNode geocodeLocation(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery + "&count=1&language=en&format=json";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
                
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode results = root.get("results");
        if (results != null && results.isArray() && results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    public String getCurrentWeather(String location, String units) {
        try {
            JsonNode geo = geocodeLocation(location);
            if (geo == null) {
                return "❌ Location '" + location + "' not found. Please verify the spelling or specify a broader area.";
            }

            String name = geo.path("name").asText("");
            String country = geo.path("country").asText("");
            String admin1 = geo.path("admin1").asText("");
            double lat = geo.path("latitude").asDouble();
            double lon = geo.path("longitude").asDouble();
            String timezone = geo.path("timezone").asText("auto");

            StringBuilder place = new StringBuilder(name);
            if (!admin1.isEmpty() && !admin1.equals(name)) {
                place.append(", ").append(admin1);
            }
            if (!country.isEmpty()) {
                place.append(", ").append(country);
            }

            boolean isImperial = "imperial".equalsIgnoreCase(units);
            String tempUnit = isImperial ? "fahrenheit" : "celsius";
            String windUnit = isImperial ? "mph" : "kmh";
            String precipUnit = isImperial ? "inch" : "mm";
            String tempSymbol = isImperial ? "°F" : "°C";
            String speedSymbol = isImperial ? "mph" : "km/h";
            String precipSymbol = isImperial ? "in" : "mm";

            String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m,surface_pressure" +
                "&timezone=%s&temperature_unit=%s&wind_speed_unit=%s&precipitation_unit=%s",
                lat, lon, URLEncoder.encode(timezone, StandardCharsets.UTF_8), tempUnit, windUnit, precipUnit
            );

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.get("current");

            int weatherCode = current.path("weather_code").asInt(-1);
            WmoCodeInfo wmo = getWmoDescription(weatherCode);
            double temp = current.path("temperature_2m").asDouble();
            double feelsLike = current.path("apparent_temperature").asDouble();
            int humidity = current.path("relative_humidity_2m").asInt();
            double precip = current.path("precipitation").asDouble(0.0);
            double windSpeed = current.path("wind_speed_10m").asDouble();
            int windDir = current.path("wind_direction_10m").asInt();
            int cloudCover = current.path("cloud_cover").asInt();
            double pressure = current.path("surface_pressure").asDouble();
            String dayNight = current.path("is_day").asInt(1) == 1 ? "Day" : "Night";

            return String.format(Locale.US, """
                ### Current Weather: %s %s
                - **Condition:** %s (%s)
                - **Temperature:** %.1f%s (Feels like %.1f%s)
                - **Relative Humidity:** %d%%
                - **Precipitation:** %.2f %s
                - **Wind:** %.1f %s (Direction: %d°)
                - **Cloud Cover:** %d%%
                - **Surface Pressure:** %.1f hPa
                - **Coordinates:** %.4f°N, %.4f°E | Timezone: %s""",
                place, wmo.emoji(), wmo.description(), dayNight,
                temp, tempSymbol, feelsLike, tempSymbol,
                humidity, precip, precipSymbol,
                windSpeed, speedSymbol, windDir,
                cloudCover, pressure, lat, lon, timezone
            );
        } catch (Exception e) {
            return "❌ Error retrieving weather for '" + location + "': " + e.getMessage();
        }
    }

    public String getWeatherForecast(String location, int days, String units) {
        try {
            days = Math.max(1, Math.min(days, 14));
            JsonNode geo = geocodeLocation(location);
            if (geo == null) {
                return "❌ Location '" + location + "' not found.";
            }

            String name = geo.path("name").asText("");
            String country = geo.path("country").asText("");
            String admin1 = geo.path("admin1").asText("");
            double lat = geo.path("latitude").asDouble();
            double lon = geo.path("longitude").asDouble();
            String timezone = geo.path("timezone").asText("auto");

            StringBuilder place = new StringBuilder(name);
            if (!admin1.isEmpty() && !admin1.equals(name)) {
                place.append(", ").append(admin1);
            }
            if (!country.isEmpty()) {
                place.append(", ").append(country);
            }

            boolean isImperial = "imperial".equalsIgnoreCase(units);
            String tempUnit = isImperial ? "fahrenheit" : "celsius";
            String windUnit = isImperial ? "mph" : "kmh";
            String precipUnit = isImperial ? "inch" : "mm";
            String tempSymbol = isImperial ? "°F" : "°C";
            String speedSymbol = isImperial ? "mph" : "km/h";
            String precipSymbol = isImperial ? "in" : "mm";

            String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,uv_index_max" +
                "&forecast_days=%d&timezone=%s&temperature_unit=%s&wind_speed_unit=%s&precipitation_unit=%s",
                lat, lon, days, URLEncoder.encode(timezone, StandardCharsets.UTF_8), tempUnit, windUnit, precipUnit
            );

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode daily = root.get("daily");

            JsonNode dates = daily.get("time");
            JsonNode codes = daily.get("weather_code");
            JsonNode maxTemps = daily.get("temperature_2m_max");
            JsonNode minTemps = daily.get("temperature_2m_min");
            JsonNode precipSums = daily.get("precipitation_sum");
            JsonNode precipProbs = daily.get("precipitation_probability_max");
            JsonNode maxWinds = daily.get("wind_speed_10m_max");
            JsonNode uvIndices = daily.get("uv_index_max");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("### %d-Day Weather Forecast: %s\n\n", days, place));
            sb.append("| Date | Condition | High / Low | Precip Probability | Precip Sum | Max Wind | UV Index |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");

            for (int i = 0; i < dates.size(); i++) {
                String date = dates.get(i).asText();
                int code = codes.has(i) ? codes.get(i).asInt() : -1;
                WmoCodeInfo wmo = getWmoDescription(code);
                String tMax = maxTemps.has(i) ? maxTemps.get(i).asText() + tempSymbol : "N/A";
                String tMin = minTemps.has(i) ? minTemps.get(i).asText() + tempSymbol : "N/A";
                String prob = (precipProbs.has(i) && !precipProbs.get(i).isNull()) ? precipProbs.get(i).asText() + "%" : "N/A";
                String pSum = precipSums.has(i) ? precipSums.get(i).asText() + " " + precipSymbol : "N/A";
                String wind = maxWinds.has(i) ? maxWinds.get(i).asText() + " " + speedSymbol : "N/A";
                String uv = (uvIndices.has(i) && !uvIndices.get(i).isNull()) ? uvIndices.get(i).asText() : "N/A";

                sb.append(String.format("| %s | %s %s | %s / %s | %s | %s | %s | %s |\n",
                    date, wmo.emoji(), wmo.description(), tMax, tMin, prob, pSum, wind, uv));
            }

            return sb.toString();
        } catch (Exception e) {
            return "❌ Error retrieving forecast for '" + location + "': " + e.getMessage();
        }
    }

    public String getAirQuality(String location) {
        try {
            JsonNode geo = geocodeLocation(location);
            if (geo == null) {
                return "❌ Location '" + location + "' not found.";
            }

            String name = geo.path("name").asText("");
            String country = geo.path("country").asText("");
            double lat = geo.path("latitude").asDouble();
            double lon = geo.path("longitude").asDouble();
            String place = country.isEmpty() ? name : name + ", " + country;

            String url = String.format(Locale.US,
                "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.4f&longitude=%.4f" +
                "&current=us_aqi,european_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone&timezone=auto",
                lat, lon
            );

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.get("current");

            int usAqi = current.path("us_aqi").asInt(-1);
            int euAqi = current.path("european_aqi").asInt(-1);
            double pm25 = current.path("pm2_5").asDouble();
            double pm10 = current.path("pm10").asDouble();
            double o3 = current.path("ozone").asDouble();
            double no2 = current.path("nitrogen_dioxide").asDouble();
            double so2 = current.path("sulphur_dioxide").asDouble();
            double co = current.path("carbon_monoxide").asDouble();

            String aqiLevel;
            if (usAqi < 0) aqiLevel = "Unknown";
            else if (usAqi <= 50) aqiLevel = "🟢 Good";
            else if (usAqi <= 100) aqiLevel = "🟡 Moderate";
            else if (usAqi <= 150) aqiLevel = "🟠 Unhealthy for Sensitive Groups";
            else if (usAqi <= 200) aqiLevel = "🔴 Unhealthy";
            else if (usAqi <= 300) aqiLevel = "🟣 Very Unhealthy";
            else aqiLevel = "🟤 Hazardous";

            return String.format(Locale.US, """
                ### Air Quality Report: %s
                - **US Air Quality Index (AQI):** %d (%s)
                - **European AQI:** %d
                - **Fine Particulate Matter (PM2.5):** %.1f µg/m³
                - **Coarse Particulate Matter (PM10):** %.1f µg/m³
                - **Ozone (O3):** %.1f µg/m³
                - **Nitrogen Dioxide (NO2):** %.1f µg/m³
                - **Sulphur Dioxide (SO2):** %.1f µg/m³
                - **Carbon Monoxide (CO):** %.1f µg/m³""",
                place, usAqi, aqiLevel, euAqi, pm25, pm10, o3, no2, so2, co
            );
        } catch (Exception e) {
            return "❌ Error retrieving air quality for '" + location + "': " + e.getMessage();
        }
    }

    public String searchLocations(String query, int count) {
        try {
            count = Math.max(1, Math.min(count, 10));
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery + "&count=" + count + "&language=en&format=json";

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.get("results");

            if (results == null || !results.isArray() || results.size() == 0) {
                return "No locations found for query '" + query + "'.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("### Matching Locations for '").append(query).append("':\n\n");

            for (int i = 0; i < results.size(); i++) {
                JsonNode item = results.get(i);
                String name = item.path("name").asText("");
                String admin1 = item.path("admin1").asText("");
                String country = item.path("country").asText("");
                double lat = item.path("latitude").asDouble();
                double lon = item.path("longitude").asDouble();
                String tz = item.path("timezone").asText("N/A");
                String elevation = item.has("elevation") ? item.path("elevation").asText() + "m" : "N/A";

                List<String> parts = new ArrayList<>();
                if (!name.isEmpty()) parts.add(name);
                if (!admin1.isEmpty()) parts.add(admin1);
                if (!country.isEmpty()) parts.add(country);
                String fullName = String.join(", ", parts);

                sb.append(String.format(Locale.US, "%d. **%s**\n   - Lat: %.4f, Lon: %.4f | Elevation: %s | Timezone: %s\n",
                    i + 1, fullName, lat, lon, elevation, tz));
            }

            return sb.toString();
        } catch (Exception e) {
            return "❌ Error searching locations for '" + query + "': " + e.getMessage();
        }
    }
}
