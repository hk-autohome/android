#include <WiFi.h>
#include <NetworkClient.h>
#include <WebServer.h>
#include <ESPmDNS.h>
#include <Preferences.h>

const int led = 2;
int ledState = LOW;
unsigned long previousMillis = 0;

const int allowedPins[] = {2, 4, 13, 16, 17, 18, 19, 21, 22, 23, 25, 26, 27, 32, 33};
const int numAllowedPins = sizeof(allowedPins) / sizeof(allowedPins[0]);
int controlPinStates[numAllowedPins];

const char *apSSID = "ESP32_Setup";
const char *apPassword = "12345678";

Preferences preferences;
const int wifiTimeout = 10000;  // 10 seconds
String devicePassword;

WebServer server(80);


void setup() {
  for (int i = 0; i < numAllowedPins; i++) {
    controlPinStates[i] = LOW;
    pinMode(allowedPins[i], OUTPUT);
    digitalWrite(allowedPins[i], LOW);
  }
  pinMode(led, OUTPUT);
  digitalWrite(led, LOW);

  Serial.begin(115200);

  preferences.begin("wifiCreds", true);
  String storedDeviceID = preferences.getString("device_id", "");
  devicePassword = preferences.getString("device_password", "");
  preferences.end();

  if (storedDeviceID.length() == 0) {
    setupAccessPoint();
  } else {
    if (!connectToWiFiWithTimeout()) {
      setupAccessPoint();
    } else {
      if (MDNS.begin("esp32")) {
        Serial.println("MDNS responder started");
        MDNS.addService("http", "tcp", 80);
      }

      server.on("/", handleRoot);
      server.on("/", HTTP_OPTIONS, handleOptions);
      server.on("/control", handleControl);
      server.on("/reset", HTTP_POST, handleResetPreferences);
      server.on("/device_id", HTTP_GET, handleGetDeviceID);
      server.on("/wifi_config", HTTP_POST, handleChangeWiFiConfig);
      server.on("/validate_password", HTTP_POST, handleValidatePassword);
      server.on("/update_password", HTTP_POST, handleUpdatePassword);

      server.onNotFound(handleNotFound);

      server.begin();
      Serial.println("HTTP server started");
    }
  }
}

void loop() {
  server.handleClient();
  if (WiFi.getMode() == WIFI_AP) {
    blinkLED(2000);
  }
  delay(2);
}
