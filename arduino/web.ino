void sendCORSHeaders() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
}

void handleRoot() {
    sendCORSHeaders();
    server.send(200, "text/plain", "Hello from ESP32!");
}

void handleConfigPost() {
    sendCORSHeaders();

    if (server.hasArg("ssid") &&
        server.hasArg("password") &&
        server.hasArg("device_password") &&
        server.hasArg("registration_number") &&
        server.hasArg("device_id")
    ) {
        String newSSID = server.arg("ssid");
        String newPassword = server.arg("password");
        String devicePassword = server.arg("device_password");
        String regNumber = server.arg("registration_number");
        String newDeviceID = server.arg("device_id");

        preferences.begin("wifiCreds", false);
        preferences.putString("ssid", newSSID);
        preferences.putString("password", newPassword);
        preferences.putString("device_password", devicePassword);
        preferences.putString("registration_number", regNumber);
        preferences.putString("device_id", newDeviceID);
        preferences.end();

        server.send(200, "text/plain", "Configuration saved! Restarting...");

        delay(1000);
        ESP.restart();
    } else {
        server.send(400, "text/plain", "Missing parameters");
    }
}

void handleResetPreferences() {
    sendCORSHeaders();

    preferences.begin("wifiCreds", false);
    preferences.clear();
    preferences.end();

    server.send(200, "text/plain", "Preferences cleared! Restarting...");

    delay(1000);
    ESP.restart();
}

void handleGetDeviceID() {
    sendCORSHeaders();
    preferences.begin("wifiCreds", true);
    String deviceID = preferences.getString("device_id", "");
    preferences.end();

    if (deviceID.length() > 0) {
        server.send(200, "text/plain", deviceID);
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
    server.send(204); // No content for OPTIONS
}

void handleGetPreferences() {
    sendCORSHeaders();
    preferences.begin("wifiCreds", true);

    String response = "Stored Preferences:\n";

    String ssid = preferences.getString("ssid", "Not Found");
    String password = preferences.getString("password", "Not Found");
    String deviceId = preferences.getString("device_id", "Not Found");
    String registrationNumber = preferences.getString("registration_number", "Not Found");

    response += "SSID: " + ssid + ",\n";
    response += "Password: " + password + ",\n";
    response += "Device ID: " + deviceId + ".\n";
    response += "Registration Number: " + registrationNumber + ".\n";

    server.send(200, "text/plain", response);
}

void handleValidatePassword() {
    sendCORSHeaders();
    if (server.hasArg("device_password")) {
        String inputPassword = server.arg("device_password");

        if (inputPassword == devicePassword) {
            server.send(200, "text/plain", "1");
        } else {
            server.send(401, "text/plain", "0");
        }
    } else {
        server.send(400, "application/json", "Missing parameter");
    }
}

void handleUpdatePassword() {
    sendCORSHeaders();

    if (server.hasArg("old_password") && server.hasArg("new_password")) {
        String oldPassword = server.arg("old_password");
        String newPassword = server.arg("new_password");

        if (oldPassword == devicePassword) {
            preferences.begin("wifiCreds", false);
            preferences.putString("device_password", newPassword);
            preferences.end();

            server.send(200, "text/plain", "1");

            delay(1000);
            ESP.restart();
        } else {
            server.send(401, "text/plain", "0");
        }
    } else {
        server.send(400, "text/plain", "missing parameters");
    }
}
