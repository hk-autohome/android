#include <WiFi.h>
#include <NetworkClient.h>
#include <WebServer.h>
#include <ESPmDNS.h>
#include <Preferences.h>  // Preferences library for storing SSID, password, and device ID

const int led = 13;  // Built-in LED


const int allowedPins[] = {2, 4, 13, 16, 17, 18, 19, 21, 22, 23, 25, 26, 27, 32, 33}; // Allowed pin numbers
const int numAllowedPins = sizeof(allowedPins) / sizeof(allowedPins[0]); // Calculate the number of allowed pins
int controlPinStates[numAllowedPins]; // Array to hold states for each allowed control pin


const char *apSSID = "ESP32_Setup";   // Access Point SSID
const char *apPassword = "12345678";  // Access Point password

Preferences preferences;  // Preferences instance to save SSID, password, and device ID

// WiFi timeout (in milliseconds)
const int wifiTimeout = 10000;  // 10 seconds

WebServer server(80);

void sendCORSHeaders() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
}

void handleRoot() {
    sendCORSHeaders();
    digitalWrite(led, HIGH);
    server.send(200, "text/plain", "Hello from ESP32!");
    digitalWrite(led, LOW);
}

void handleAPRoot() {
    String html = "<html><body><h2>ESP32 Configuration</h2>"
                  "<form action='/config' method='POST'>"
                  "Device ID: <input type='text' name='device_id'><br>"
                  "WiFi SSID: <input type='text' name='ssid'><br>"
                  "WiFi Password: <input type='password' name='password'><br>"
                  "<input type='submit' value='Save'>"
                  "</form></body></html>";
    server.send(200, "text/html", html);
}


void indicateSuccess() {
    digitalWrite(led, HIGH);
    delay(100);
    digitalWrite(led, LOW);
    delay(100);
}

void indicateError() {
    for (int i = 0; i < 3; i++) {
        digitalWrite(led, HIGH);
        delay(100);
        digitalWrite(led, LOW);
        delay(100);
    }
}

// Save WiFi credentials and device ID to non-volatile storage and restart the ESP32
void handleConfigPost() {
    sendCORSHeaders();

    if (server.hasArg("ssid") && server.hasArg("password") && server.hasArg("device_id")) {
        String newSSID = server.arg("ssid");
        String newPassword = server.arg("password");
        String newDeviceID = server.arg("device_id");

        // Save the new SSID, password, and device ID in non-volatile storage
        preferences.begin("wifiCreds", false);  // Open storage in write mode
        preferences.putString("ssid", newSSID);
        preferences.putString("password", newPassword);
        preferences.putString("device_id", newDeviceID);
        preferences.end();

        preferences.begin("wifiCreds", true);
        String savedSSID = preferences.getString("ssid", "Not Found");
        String savedPassword = preferences.getString("password", "Not Found");
        String savedDeviceID = preferences.getString("device_id", "Not Found");
        preferences.end();

        if (savedSSID != newSSID) {
            server.send(200, "text/plain", "SSID not updated");
            return;
        }

        if (savedPassword != newPassword ) {
            server.send(200, "text/plain", "Password not updated");
            return;
        }

        if (savedDeviceID != newDeviceID ) {
            server.send(200, "text/plain", "Device ID not updated");
            return;
        }

        server.send(200, "text/plain", "Configuration saved! Restarting...");

        delay(1000);
        ESP.restart();  // Restart the ESP32 to apply the new settings
    } else {
        server.send(400, "text/plain", "Missing parameters");
    }
}

void handleResetPreferences() {
    sendCORSHeaders();

    preferences.begin("wifiCreds", false);  // Open storage in write mode
    preferences.clear();  // Clear all stored preferences (SSID, password, etc.)
    preferences.end();

    server.send(200, "text/plain", "Preferences cleared! Restarting...");

    delay(1000);
    ESP.restart();  // Restart the ESP32 to apply the reset
}


void handleControl() {
    sendCORSHeaders();

    if (server.hasArg("pin")) {
        int pin = server.arg("pin").toInt(); // Parse the pin number
        bool pinFound = false;

        // Check if the pin number is valid (exists in allowedPins)
        for (int i = 0; i < numAllowedPins; i++) {
            if (allowedPins[i] == pin) {
                pinFound = true;

                // Control the pin based on its stored state in controlPinStates
                if (controlPinStates[i] == HIGH) {
                    digitalWrite(pin, LOW); // If it was HIGH, set it to LOW
                    controlPinStates[i] = LOW; // Update the state in the array
                    server.send(200, "text/plain", String(pin) + ":" + String(LOW));
                } else {
                    digitalWrite(pin, HIGH); // If it was LOW, set it to HIGH
                    controlPinStates[i] = HIGH; // Update the state in the array
                    server.send(200, "text/plain", String(pin) + ":" + String(HIGH));
                }
                break; // Exit the loop since the pin is found
            }
        }

        if (!pinFound) {
            server.send(400, "text/plain", "Invalid pin number, use one of the allowed pins: 2, 4, 13, 16-33");
        }
    } else {
        server.send(400, "text/plain", "Missing 'pin' parameter");
    }
}


void handleGetDeviceID() {
    sendCORSHeaders();
    preferences.begin("wifiCreds", true);  // Open storage in read mode
    String deviceID = preferences.getString("device_id", "");
    preferences.end();

    if (deviceID.length() > 0) {
        server.send(200, "text/plain", deviceID);  // Return the stored device ID
    } else {
        server.send(404, "text/plain", "Device ID not found. Please configure the device.");
    }
}

void handleNotFound() {
    sendCORSHeaders();
    String message = "File Not Found\n\n";
    message += "URI: " + server.uri();
    message += "\nMethod: ";
    if (server.method() == HTTP_GET) {
        message += "GET";
    } else {
        message += "POST";
    }
    message += "\nArguments: " + String(server.args()) + "\n";

    for (uint8_t i = 0; i < server.args(); i++) {
        message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
    }

    server.send(404, "text/plain", message);
}

void handleOptions() {
    sendCORSHeaders();
    server.send(204);  // No content for OPTIONS
}

// Attempt to connect to WiFi using stored credentials
bool connectToWiFiWithTimeout() {
    preferences.begin("wifiCreds", true);  // Open storage in read mode
    String storedSSID = preferences.getString("ssid", "");
    String storedPassword = preferences.getString("password", "");
    preferences.end();

    if (storedSSID.length() > 0 && storedPassword.length() > 0) {
        Serial.print("Connecting to WiFi SSID: ");
        Serial.println(storedSSID);

        WiFi.begin(storedSSID.c_str(), storedPassword.c_str());

        unsigned long startTime = millis();
        while (WiFi.status() != WL_CONNECTED) {
            if (millis() - startTime >= wifiTimeout) {
                Serial.println("WiFi connection timed out");
                return false;
            }
            Serial.print(".");
            delay(500);
        }

        Serial.println("Connected to WiFi!");
        Serial.print("IP address: ");
        Serial.println(WiFi.localIP());
        return true;
    }

    return false;
}

void handleGetPreferences() {
    sendCORSHeaders();
    preferences.begin("wifiCreds", true);  // Open storage in read mode

    String response = "Stored Preferences:\n";

    String ssid = preferences.getString("ssid", "Not Found");
    String password = preferences.getString("password", "Not Found");
    String deviceId = preferences.getString("device_id", "Not Found");

    // Build the response string
    response += "SSID: " + ssid + ",\n";
    response += "Password: " + password + ",\n";
    response += "Device ID: " + deviceId + ".\n";

    server.send(200, "text/plain", response);  // Send the preferences as plain text
}

// Setup the access point for WiFi configuration
void setupAccessPoint() {
    Serial.println("Starting Access Point...");
    WiFi.softAP(apSSID, apPassword);

    Serial.print("AP IP address: ");
    Serial.println(WiFi.softAPIP());

    server.on("/", handleAPRoot);
    server.on("/config", HTTP_POST, handleConfigPost);
    server.on("/prefs", HTTP_GET, handleGetPreferences);

    server.begin();
    Serial.println("AP HTTP server started");
}

void setup() {
    for (int i = 0; i < numAllowedPins; i++) {
        controlPinStates[i] = LOW;
        pinMode(allowedPins[i], OUTPUT);
        digitalWrite(allowedPins[i], LOW);
    }
    pinMode(led, OUTPUT);
    digitalWrite(led, LOW);

    Serial.begin(115200);

    WiFi.mode(WIFI_STA);

    // Check if device ID is already stored
    preferences.begin("wifiCreds", true);
    String storedDeviceID = preferences.getString("device_id", "");
    preferences.end();

    if (storedDeviceID.length() == 0) {
        // Device ID not found, start access point for configuration
        setupAccessPoint();
    } else {
        // Attempt to connect to WiFi using stored credentials
        if (!connectToWiFiWithTimeout()) {
            // Failed to connect to WiFi, start access point for configuration
            setupAccessPoint();
        } else {
            // Connected to WiFi, start the web server
            if (MDNS.begin("esp32")) {
                Serial.println("MDNS responder started");
            }

            server.on("/", handleRoot);
            server.on("/control", handleControl);
            server.on("/control", HTTP_OPTIONS, handleOptions);
            server.on("/", HTTP_OPTIONS, handleOptions);
            server.on("/reset", HTTP_POST, handleResetPreferences);
            server.on("/device_id", HTTP_GET, handleGetDeviceID);  // API to get device ID

            server.onNotFound(handleNotFound);

            server.begin();
            Serial.println("HTTP server started");
        }
    }
}

void loop() {
    server.handleClient();
    delay(2);  // Allow time for other tasks
}