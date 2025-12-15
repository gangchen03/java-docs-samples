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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BatchRecognitionV2IT {
  private String audioUri = "gs://cloud-samples-data/speech/brooklyn_bridge.wav";
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
  }

  @After
  public void tearDown() {
    System.setOut(originalPrintStream);
  }

  @Test
  public void testBatchRecognize() throws Exception {
    // Act
    BatchRecognitionV2.batchRecognize(projectId, audioUri, recognizerId);

    // Assert
    String got = bout.toString();
    assertThat(got).contains("Operation finished");
    assertThat(got).contains("Transcript");
    // The sample audio "brooklyn_bridge.wav" typically contains "Brooklyn Bridge"
    assertThat(got).contains("Brooklyn Bridge");
  }
}
