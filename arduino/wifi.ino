bool connectToWiFiWithTimeout() {
    preferences.begin("wifiCreds", true);  // Open storage in read mode
    String storedSSID = preferences.getString("ssid", "");
    String storedPassword = preferences.getString("password", "");
    preferences.end();

    if (storedSSID.length() > 0 && storedPassword.length() > 0) {
        Serial.print("Connecting to WiFi SSID: ");
        Serial.println(storedSSID);

        WiFi.mode(WIFI_STA);
        WiFi.begin(storedSSID.c_str(), storedPassword.c_str());

        unsigned long startTime = millis();
        while (WiFi.status() != WL_CONNECTED) {
            blinkLED(200);
            if (millis() - startTime >= wifiTimeout) {
                Serial.println("WiFi connection timed out");
                return false;
            }
            delay(200);
        }

        digitalWrite(led, HIGH);
        Serial.println("Connected to WiFi!");
        Serial.print("IP address: ");
        Serial.println(WiFi.localIP());
        return true;
    }

    return false;
}

void handleAPRoot() {
    String html = "<html><body><h2>ESP32 Configuration</h2>"
                  "<form action='/config' method='POST'>"
                  "Device ID: <input type='text' name='device_id'><br>"
                  "WiFi SSID: <input type='text' name='ssid'><br>"
                  "WiFi Password: <input type='password' name='password'><br>"
                  "Device Password: <input type='password' name='device_password'><br>"
                  "Registration number: <input type='password' name='registration_number'><br>"
                  "<input type='submit' value='Save'>"
                  "</form></body></html>";
    server.send(200, "text/html", html);
}

void setupAccessPoint() {
    Serial.println("Starting Access Point...");

    WiFi.mode(WIFI_AP);
    WiFi.softAP(apSSID, apPassword);

    Serial.print("AP IP address: ");
    Serial.println(WiFi.softAPIP());

    server.on("/", handleAPRoot);
    server.on("/config", HTTP_POST, handleConfigPost);
    server.on("/prefs", HTTP_GET, handleGetPreferences);

    server.begin();
    Serial.println("AP HTTP server started");
}

void handleChangeWiFiConfig() {
    sendCORSHeaders();

    if (server.hasArg("ssid") && server.hasArg("password") && server.hasArg("device_password")) {
        String newSSID = server.arg("ssid");
        String newPassword = server.arg("password");
        String argPassword = server.arg("device_password");

        if (argPassword != devicePassword) {
            server.send(401, "text/plain", "Invalid password.");
            return;
        }

        preferences.begin("wifiCreds", false);  // Open in write mode
        preferences.putString("ssid", newSSID);
        preferences.putString("password", newPassword);
        preferences.end();

        server.send(200, "text/plain", "WiFi and Device Password updated! Restarting...");

        delay(1000);
        ESP.restart();  // Restart ESP to apply new WiFi settings
    } else {
        server.send(400, "text/plain", "Missing parameters");
    }
}
