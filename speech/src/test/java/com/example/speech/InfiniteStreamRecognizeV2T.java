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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InfiniteStreamRecognizeV2T {

  @Test
  public void testConvertMillisToDate() {
    // Test 0 ms
    assertThat(InfiniteStreamRecognizeV2.convertMillisToDate(0)).isEqualTo("00:00 /");

    // Test 1 second
    assertThat(InfiniteStreamRecognizeV2.convertMillisToDate(1000)).isEqualTo("00:01 /");

    // Test 1 minute
    assertThat(InfiniteStreamRecognizeV2.convertMillisToDate(60000)).isEqualTo("01:00 /");

    // Test 1 minute 5 seconds
    assertThat(InfiniteStreamRecognizeV2.convertMillisToDate(65000)).isEqualTo("01:05 /");
  }
}
