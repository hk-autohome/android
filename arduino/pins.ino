void blinkLED(unsigned long interval) {
    unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= interval) {
        previousMillis = currentMillis;
        ledState = !ledState;  // Toggle LED state
        digitalWrite(led, ledState);
    }
}

void handleControl() {
    sendCORSHeaders();

    if (server.hasArg("pin") && server.hasArg("password")) {
        int pin = server.arg("pin").toInt();
        String argPassword = server.arg("password");

        if (argPassword != devicePassword) {
            server.send(401, "text/plain", "Invalid password.");
            return;
        }

        if (pin < 0 || pin >= numAllowedPins) {
            server.send(400, "text/plain", "Invalid pin number, use one of the allowed pin numbers: 0-" + numAllowedPins);
        }

        if (controlPinStates[pin] == HIGH) {
            digitalWrite(allowedPins[pin], LOW); // If it was HIGH, set it to LOW
            controlPinStates[pin] = LOW; // Update the state in the array
            server.send(200, "text/plain", String(allowedPins[pin]) + ":" + String(LOW));
        } else {
            digitalWrite(allowedPins[pin], HIGH); // If it was LOW, set it to HIGH
            controlPinStates[pin] = HIGH; // Update the state in the array
            server.send(200, "text/plain", String(allowedPins[pin]) + ":" + String(HIGH));
        }
    } else {
        server.send(400, "text/plain", "Missing 'pin' or 'password' parameter");
    }
}
