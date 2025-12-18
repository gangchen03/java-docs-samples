/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.speech;

// [START speech_transcribe_streaming_v2]

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v2.RecognitionConfig;
import com.google.cloud.speech.v2.SpeechClient;
import com.google.cloud.speech.v2.SpeechRecognitionAlternative;
import com.google.cloud.speech.v2.SpeechSettings;
import com.google.cloud.speech.v2.StreamingRecognitionConfig;
import com.google.cloud.speech.v2.StreamingRecognitionFeatures;
import com.google.cloud.speech.v2.StreamingRecognitionResult;
import com.google.cloud.speech.v2.StreamingRecognizeRequest;
import com.google.cloud.speech.v2.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates how to stream audio from a file to Google Cloud Speech-to-Text
 * V2.
 *
 * Use mvn to run the example:
 * mvn compile exec:java -Dexec.mainClass="com.example.speech.StreamRecognizeV2"
 * \
 * -Dexec.args="/path/to/audio/file.wav"
 */
public class StreamRecognizeV2 {

  // Default project ID - update this or set GOOGLE_CLOUD_PROJECT env var
  private static String projectId = "your_gcp_project_id";

  public static void main(String... args) {
    if (System.getenv("GOOGLE_CLOUD_PROJECT") != null) {
      projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
    }

    // Path to the local audio file to stream
    String filePath = "path/to/your/audio.wav";
    // Check if user provided an argument
    if (args.length > 0) {
      filePath = args[0];
    }

    try {
      streamingRecognize(filePath);
    } catch (Exception e) {
      System.out.println("Exception caught: " + e);
    }
  }

  /**
   * Performs streaming speech recognition on a file with simulated real-time
   * delay.
   */
  public static void streamingRecognize(String filePath) throws Exception {

    SpeechSettings speechSettings = SpeechSettings.newBuilder()
        .setEndpoint("us-speech.googleapis.com:443")
        .build();

    final CountDownLatch finishLatch = new CountDownLatch(1);

    try (SpeechClient client = SpeechClient.create(speechSettings)) {
      ClientStream<StreamingRecognizeRequest> clientStream;
      ResponseObserver<StreamingRecognizeResponse> responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {
        public void onStart(StreamController controller) {
        }

        public void onResponse(StreamingRecognizeResponse response) {
          for (StreamingRecognitionResult result : response.getResultsList()) {
            if (result.getAlternativesCount() > 0) {
              SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
              if (result.getIsFinal()) {
                System.out.print("\033[2K\r"); // Clear the line
                System.out.printf("%s\n", alternative.getTranscript());
              } else {
                System.out.print("\033[2K\r"); // Clear the line
                System.out.printf("%s", alternative.getTranscript());
                System.out.flush();
              }
            }
          }
        }

        public void onComplete() {
          System.out.println(); // Ensure final line is committed
          finishLatch.countDown();
        }

        public void onError(Throwable t) {
          System.out.println("Error: " + t);
          finishLatch.countDown();
        }
      };

      clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);

      // Use AutoDetectDecodingConfig to automatically determine audio format
      RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
          .setAutoDecodingConfig(com.google.cloud.speech.v2.AutoDetectDecodingConfig.newBuilder().build())
          .addLanguageCodes("en-US")
          .setModel("chirp_3")
          .build();

      StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
          .setConfig(recognitionConfig)
          .setStreamingFeatures(StreamingRecognitionFeatures.newBuilder()
              .setInterimResults(true)
              .build())
          .build();

      String recognizerName = String.format("projects/%s/locations/us/recognizers/_", projectId);

      StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
          .setRecognizer(recognizerName)
          .setStreamingConfig(streamingRecognitionConfig)
          .build();

      // First request contains the configuration
      clientStream.send(request);

      // Read the file bytes
      Path path = Paths.get(filePath);
      byte[] data = Files.readAllBytes(path);

      // Stream the file content with rate limiting
      // 6400 bytes = 200ms at 16kHz sample rate, 16-bit (2 bytes per sample)
      // For other formats (e.g. MP3), this will still be roughly real-time or valid.
      int chunkSize = 6400;

      for (int i = 0; i < data.length; i += chunkSize) {
        int end = Math.min(data.length, i + chunkSize);
        ByteString audioBytes = ByteString.copyFrom(data, i, end - i);

        request = StreamingRecognizeRequest.newBuilder()
            .setRecognizer(recognizerName)
            .setAudio(audioBytes)
            .build();
        clientStream.send(request);

        // Simulate real-time streaming by waiting for the duration of the chunk.
        // For 16kHz WAV, 6400 bytes is 200ms. We sleep 50ms to stream faster (4x
        // real-time)
        // or to accommodate higher sample rates (e.g. 44.1kHz which needs faster
        // throughput).
        Thread.sleep(100);
      }

      // Allow brief time for server to process the last chunk before closing output
      Thread.sleep(2000);

      clientStream.closeSend();
      System.out.println("Audio upload complete. Waiting for final results...");
      // Wait for the final results to be received. Limit to 5 minutes (streaming
      // limit).
      finishLatch.await(5, TimeUnit.MINUTES);
    }
  }
}
// [END speech_transcribe_streaming_v2]
