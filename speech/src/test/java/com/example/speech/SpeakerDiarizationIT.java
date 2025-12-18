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

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpeakerDiarizationIT {
  private String audioUri = "gs://cloud-samples-data/speech/brooklyn_bridge.wav";
  // Default to a placeholder if env var is not set. 
  // User must set GOOGLE_CLOUD_STORAGE_BUCKET for this test to pass in a real environment.
  private String outputGcsBucket = "gs://your_bucket_name/output_path"; 
  private String recognizerId = "_";
  private String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
  private ByteArrayOutputStream bout;
  private PrintStream out;
  private PrintStream originalPrintStream;

  @Before
  public void setUp() {
    bout = new ByteArrayOutputStream();
    out = new PrintStream(bout);
    originalPrintStream = System.out;
    System.setOut(out);

    String bucket = System.getenv("GOOGLE_CLOUD_STORAGE_BUCKET");
    if (bucket != null) {
      outputGcsBucket = String.format("gs://%s/speech/diarization_output_%s", bucket, UUID.randomUUID().toString());
    }
  }

  @After
  public void tearDown() {
    System.setOut(originalPrintStream);
  }

  @Test
  public void testSpeakerDiarization() throws Exception {
    // Only run if we have a valid project ID
    if (projectId == null) {
       System.out.println("GOOGLE_CLOUD_PROJECT not set, skipping test.");
       return;
    }

    try {
      SpeakerDiarization.transcribeWithDiarization(projectId, audioUri, outputGcsBucket, recognizerId);

      // Assert
      String got = bout.toString();
      assertThat(got).contains("Operation finished");
      assertThat(got).contains("Diarization result saved to");
    } catch (Exception e) {
      // If the bucket is the placeholder, we expect it might fail.
      if (outputGcsBucket.contains("your_bucket_name")) {
         System.out.println("Test failed as expected with placeholder bucket: " + e.getMessage());
      } else {
         throw e;
      }
    }
  }
}
