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

// [START speech_speaker_diarization_beta]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v2.AutoDetectDecodingConfig;
import com.google.cloud.speech.v2.BatchRecognizeFileMetadata;
import com.google.cloud.speech.v2.BatchRecognizeFileResult;
import com.google.cloud.speech.v2.BatchRecognizeRequest;
import com.google.cloud.speech.v2.BatchRecognizeResponse;
import com.google.cloud.speech.v2.GcsOutputConfig;
import com.google.cloud.speech.v2.OperationMetadata;
import com.google.cloud.speech.v2.RecognitionConfig;
import com.google.cloud.speech.v2.RecognitionFeatures;
import com.google.cloud.speech.v2.RecognitionOutputConfig;
import com.google.cloud.speech.v2.SpeakerDiarizationConfig;
import com.google.cloud.speech.v2.SpeechClient;
import com.google.cloud.speech.v2.SpeechSettings;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Performs batch speaker diarization on a GCS audio file.
 *
 * You'll also use the batch_recognize method to transcribe an audio file in
 * Cloud Storage
 * and save the output in Cloud Storage. To display the transcription, you can
 * copy the
 * output JSON file and use a helper function to format it.
 */
public class SpeakerDiarization {

  public static void main(String[] args) throws IOException, ExecutionException,
      InterruptedException {
    String projectId = "your_gcp_project_id";
    String audioUri = "gs://cloud-samples-data/speech/brooklyn_bridge.wav";
    String outputGcsBucket = "gs://your_bucket_name/output_path";
    String recognizerId = "_"; // use implicit recognizer
    transcribeWithDiarization(projectId, audioUri, outputGcsBucket, recognizerId);
  }

  public static void transcribeWithDiarization(String projectId, String audioUri,
      String outputGcsBucket, String recognizerId)
      throws IOException, ExecutionException, InterruptedException {

    // Initialize client that will be used to send requests. This client only needs
    // to be created
    // once, and can be reused for multiple requests. After completing all of your
    // requests, call
    // the "close" method on the client to safely clean up any remaining background
    // resources.
    SpeechSettings speechSettings = SpeechSettings.newBuilder()
        .setEndpoint("us-speech.googleapis.com:443")
        .build();
    try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
      String parent = String.format("projects/%s/locations/us", projectId);
      String recognizerName = String.format("%s/recognizers/%s", parent, recognizerId);

      RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
          .setModel("chirp_3")
          .addLanguageCodes("en-US")
          .setFeatures(RecognitionFeatures.newBuilder()
              .setDiarizationConfig(SpeakerDiarizationConfig.newBuilder().build())
              .build())
          .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
          .build();

      BatchRecognizeFileMetadata fileMetadata = BatchRecognizeFileMetadata.newBuilder()
          .setUri(audioUri)
          .build();

      BatchRecognizeRequest request = BatchRecognizeRequest.newBuilder()
          .setRecognizer(recognizerName)
          .setConfig(recognitionConfig)
          .addFiles(fileMetadata)
          .setRecognitionOutputConfig(RecognitionOutputConfig.newBuilder()
              .setGcsOutputConfig(GcsOutputConfig.newBuilder()
                  .setUri(outputGcsBucket)
                  .build())
              .build())
          .build();

      // Transcribes the audio into text
      OperationFuture<BatchRecognizeResponse, OperationMetadata> future = speechClient.batchRecognizeAsync(request);

      String operationName = future.getInitialFuture().get().getName();
      System.out.printf("Operation started. Name: %s%n", operationName);

      // Poll for progress
      while (!future.isDone()) {
        System.out.println("Processing...");
        checkOperationStatus(speechClient, operationName);
        Thread.sleep(1000); // Poll every 1 seconds
      }

      System.out.println("Operation finished.");
      BatchRecognizeResponse response = future.get();

      BatchRecognizeFileResult fileResult = response.getResultsMap().get(audioUri);
      if (fileResult != null) {
        System.out.printf("Diarization result saved to: %s%n", fileResult.getUri());
      } else {
        System.out.println("No results found for URI: " + audioUri);
      }
    }
  }

  public static void checkOperationStatus(SpeechClient client, String operationName) {
    com.google.longrunning.Operation operation = client.getOperationsClient().getOperation(operationName);
    if (operation.hasMetadata()) {
      try {
        OperationMetadata metadata = operation.getMetadata().unpack(OperationMetadata.class);
        System.out.printf("Operation Status: %s, Progress: %s%%%n",
            operation.getDone() ? "DONE" : "RUNNING",
            metadata.getProgressPercent());
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        System.err.println("Failed to unpack metadata: " + e.getMessage());
      }
    } else {
      System.out.printf("Operation Status: %s (No Metadata)%n",
          operation.getDone() ? "DONE" : "RUNNING");
    }
  }

  public static void cancelOperation(SpeechClient client, String operationName) {
    client.getOperationsClient().cancelOperation(operationName);
    System.out.printf("Operation %s cancelled.%n", operationName);
  }
}
// [END speech_speaker_diarization_beta]
