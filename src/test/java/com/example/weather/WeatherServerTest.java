package com.example.weather;

import com.example.weather.service.OpenMeteoService;
import com.example.weather.tools.WeatherTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WeatherServerTest {

    @Autowired
    private OpenMeteoService openMeteoService;

    @Autowired
    private WeatherTools weatherTools;

    @Test
    void testCurrentWeather() {
        String result = weatherTools.getCurrentWeather("London", "metric");
        assertNotNull(result);
        assertTrue(result.contains("Current Weather: London"));
        System.out.println("=== CURRENT WEATHER TEST ===");
        System.out.println(result);
    }

    @Test
    void testWeatherForecast() {
        String result = weatherTools.getWeatherForecast("Tokyo", 3, "metric");
        assertNotNull(result);
        assertTrue(result.contains("3-Day Weather Forecast: Tokyo"));
        System.out.println("=== WEATHER FORECAST TEST ===");
        System.out.println(result);
    }

    @Test
    void testAirQuality() {
        String result = weatherTools.getAirQuality("Paris");
        assertNotNull(result);
        assertTrue(result.contains("Air Quality Report: Paris"));
        System.out.println("=== AIR QUALITY TEST ===");
        System.out.println(result);
    }

    @Test
    void testSearchLocations() {
        String result = weatherTools.searchLocations("San Francisco", 3);
        assertNotNull(result);
        assertTrue(result.contains("Matching Locations for 'San Francisco'"));
        System.out.println("=== SEARCH LOCATIONS TEST ===");
        System.out.println(result);
    }
}
