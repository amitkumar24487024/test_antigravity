# Weather MCP Server 🌤️ (Java & Spring Boot - Gradle)

A Java & Spring Boot **Model Context Protocol (MCP)** server providing real-time weather conditions, multi-day forecasts, air quality metrics, and location search to any MCP-compatible AI assistant (Antigravity, Claude Desktop, Cursor, Windsurf, Cline, etc.).

Powered by [Open-Meteo](https://open-meteo.com/) — **100% free with no API key or account registration required!**

---

## 🚀 Features

- 🌡️ **Current Weather (`get_current_weather`)**:
  - Temperature & "Feels Like" temperature
  - Weather condition summary & emoji icon
  - Relative humidity, precipitation amount, and cloud cover
  - Wind speed & direction
  - Day / Night detection & atmospheric surface pressure
  - Metric (°C, km/h, mm) or Imperial (°F, mph, in) units

- 📅 **Weather Forecast (`get_weather_forecast`)**:
  - Up to 14 days of detailed daily weather forecasts
  - High & low temperatures
  - Precipitation probability (%) and precipitation sum
  - Maximum wind speeds & UV index ratings

- 🍃 **Air Quality (`get_air_quality`)**:
  - US Air Quality Index (AQI) with health severity ratings
  - European AQI
  - Particulate matter ($PM_{2.5}$, $PM_{10}$)
  - Gas concentrations ($O_3$, $NO_2$, $SO_2$, $CO$)

- 🔍 **Location Geocoding (`search_locations`)**:
  - Resolves ambiguous city names to coordinates, state/region, country, elevation, and timezone

---

## 🛠️ Project Structure

```
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
    │   │       └── example
    │   │           └── weather
    │   │               ├── WeatherMcpApplication.java  (Spring Boot Entrypoint)
    │   │               ├── config
    │   │               │   └── McpConfig.java          (MCP Java SDK Server & Tool Config)
    │   │               ├── service
    │   │               │   └── OpenMeteoService.java   (Open-Meteo REST Client Service)
    │   │               └── tools
    │   │                   └── WeatherTools.java       (Spring AI Annotated Tools)
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── weather
│                       ├── WeatherServerTest.java      (Tool Integration Tests)
│                       └── McpResourceTest.java        (Resource Endpoint Tests)
```

---

## 🏗️ Building & Running

### Prerequisites
- **Java 17 or higher** (Tested on Java 17 LTS)

### 1. Build the Jar
```powershell
.\gradlew.bat bootJar
```

### 2. Run Integration Tests
```powershell
.\gradlew.bat test
```

### 3. Run the Application
```powershell
.\gradlew.bat bootRun
```
*or run the built jar:*
```powershell
java -jar build/libs/weather-mcp-server-1.0.0.jar
```

---

## ⚙️ Connecting to MCP Clients

### 1. Antigravity Configuration
Add the server to your Antigravity MCP settings:

```json
{
  "mcpServers": {
    "weather-java": {
      "command": "java",
      "args": [
        "-jar",
        "c:\\Users\\Amit Kumsr Singh\\Documents\\Codebase\\test_antigravity\\build\\libs\\weather-mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

### 2. Claude Desktop
Add to your Claude Desktop configuration (`%APPDATA%\Claude\claude_desktop_config.json` on Windows):

```json
{
  "mcpServers": {
    "weather-java": {
      "command": "java",
      "args": [
        "-jar",
        "c:\\Users\\Amit Kumsr Singh\\Documents\\Codebase\\test_antigravity\\build\\libs\\weather-mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

---

## 📖 Tool Reference

### `get_current_weather`
| Parameter | Type | Required | Description | Default |
| :--- | :--- | :--- | :--- | :--- |
| `location` | `string` | **Yes** | City name or location (e.g. `"Paris"`, `"Tokyo"`, `"New York"`) | - |
| `units` | `string` | No | `"metric"` (°C, km/h) or `"imperial"` (°F, mph) | `"metric"` |

### `get_weather_forecast`
| Parameter | Type | Required | Description | Default |
| :--- | :--- | :--- | :--- | :--- |
| `location` | `string` | **Yes** | City name or location | - |
| `days` | `integer` | No | Number of days (1 to 14) | `5` |
| `units` | `string` | No | `"metric"` or `"imperial"` | `"metric"` |

### `get_air_quality`
| Parameter | Type | Required | Description | Default |
| :--- | :--- | :--- | :--- | :--- |
| `location` | `string` | **Yes** | City name or location | - |

### `search_locations`
| Parameter | Type | Required | Description | Default |
| :--- | :--- | :--- | :--- | :--- |
| `query` | `string` | **Yes** | Query string (e.g. `"Springfield"`) | - |
| `count` | `integer` | No | Number of matches (1 to 10) | `5` |

---

## 📄 License
MIT License.
