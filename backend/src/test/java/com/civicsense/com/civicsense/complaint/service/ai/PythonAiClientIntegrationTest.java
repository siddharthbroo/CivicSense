package com.civicsense.com.civicsense.complaint.service.ai;

import com.civicsense.complaint.service.ai.ComplaintTextSimilarityResponse;
import com.civicsense.complaint.service.ai.PythonAiClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonAiClientIntegrationTest {

    @Test
    @Tag("integration")
    @EnabledIf(value = "isPythonAiServiceAvailable", disabledReason = "Requires the local Python AI service on 127.0.0.1:8001")
    void shouldCompareComplaintsUsingPythonAiService() {

        PythonAiClient pythonAiClient =
                new PythonAiClient(
                        "http://127.0.0.1:8001"
                );

        ComplaintTextSimilarityResponse response =
                pythonAiClient.compareComplaints(
                        "Road mein pothole hai",
                        "Market ke paas road mein bada pothole hai",
                        "Large pothole near market road",
                        "ROAD_DAMAGE",

                        "Road par bada gaddha hai",
                        "Market ke paas road mein ek bada gaddha hai",
                        "Large pothole near market road",
                        "ROAD_DAMAGE"
                );

        assertNotNull(response);

        System.out.println("Title similarity: "
                + response.title_similarity());

        System.out.println("Description similarity: "
                + response.description_similarity());

        System.out.println("Summary similarity: "
                + response.summary_similarity());

        System.out.println("Category match: "
                + response.category_match());

        assertTrue(response.category_match());
        assertTrue(response.summary_similarity() > 0.8);
    }

    static boolean isPythonAiServiceAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 8001), 250);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
