package com.example.weather.tools;

import com.example.weather.service.OpenMeteoService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {

    private final OpenMeteoService openMeteoService;

    public WeatherTools(OpenMeteoService openMeteoService) {
        this.openMeteoService = openMeteoService;
    }

    @Tool(
        name = "get_current_weather",
        description = "Get current real-time weather conditions for any location (city, state, country)."
    )
    public String getCurrentWeather(
            @ToolParam(description = "City name, region, or address (e.g. 'London', 'Tokyo', 'Paris')") String location,
            @ToolParam(description = "Temperature and speed unit standard: 'metric' (Celsius, km/h) or 'imperial' (Fahrenheit, mph). Default is 'metric'", required = false) String units
    ) {
        if (units == null || units.isBlank()) {
            units = "metric";
        }
        return openMeteoService.getCurrentWeather(location, units);
    }

    @Tool(
        name = "get_weather_forecast",
        description = "Get daily weather forecast for a location (up to 14 days)."
    )
    public String getWeatherForecast(
            @ToolParam(description = "City name or address (e.g. 'Berlin', 'New York')") String location,
            @ToolParam(description = "Number of forecast days to retrieve (1 to 14, default 5)", required = false) Integer days,
            @ToolParam(description = "'metric' or 'imperial'. Default is 'metric'", required = false) String units
    ) {
        if (days == null) days = 5;
        if (units == null || units.isBlank()) units = "metric";
        return openMeteoService.getWeatherForecast(location, days, units);
    }

    @Tool(
        name = "get_air_quality",
        description = "Get current air quality index (AQI) and atmospheric pollutant levels for a location."
    )
    public String getAirQuality(
            @ToolParam(description = "City name or address (e.g. 'Beijing', 'New Delhi', 'Los Angeles')") String location
    ) {
        return openMeteoService.getAirQuality(location);
    }

    @Tool(
        name = "search_locations",
        description = "Search and resolve geographical details (latitude, longitude, timezone, country) for a query."
    )
    public String searchLocations(
            @ToolParam(description = "Location query string (e.g. 'Springfield', 'Cambridge')") String query,
            @ToolParam(description = "Maximum number of search results (1 to 10, default 5)", required = false) Integer count
    ) {
        if (count == null) count = 5;
        return openMeteoService.searchLocations(query, count);
    }
}
