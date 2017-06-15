/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.source;

import android.test.InstrumentationTestCase;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.TimelineTest;
import com.google.android.exoplayer2.TimelineTest.FakeTimeline;
import com.google.android.exoplayer2.TimelineTest.StubMediaSource;
import com.google.android.exoplayer2.TimelineTest.TimelineVerifier;

/**
 * Unit tests for {@link ClippingMediaSource}.
 */
public final class ClippingMediaSourceTest extends InstrumentationTestCase {

  private static final long TEST_PERIOD_DURATION_US = 1000000;
  private static final long TEST_CLIP_AMOUNT_US = 300000;

  private Window window;
  private Period period;

  @Override
  protected void setUp() throws Exception {
    window = new Timeline.Window();
    period = new Timeline.Period();
  }

  public void testNoClipping() {
    Timeline timeline = new SinglePeriodTimeline(C.msToUs(TEST_PERIOD_DURATION_US), true);

    Timeline clippedTimeline = getClippedTimeline(timeline, 0, TEST_PERIOD_DURATION_US);

    assertEquals(1, clippedTimeline.getWindowCount());
    assertEquals(1, clippedTimeline.getPeriodCount());
    assertEquals(TEST_PERIOD_DURATION_US, clippedTimeline.getWindow(0, window).getDurationUs());
    assertEquals(TEST_PERIOD_DURATION_US, clippedTimeline.getPeriod(0, period).getDurationUs());
  }

  public void testClippingUnseekableWindowThrows() {
    Timeline timeline = new SinglePeriodTimeline(C.msToUs(TEST_PERIOD_DURATION_US), false);

    // If the unseekable window isn't clipped, clipping succeeds.
    getClippedTimeline(timeline, 0, TEST_PERIOD_DURATION_US);
    try {
      // If the unseekable window is clipped, clipping fails.
      getClippedTimeline(timeline, 1, TEST_PERIOD_DURATION_US);
      fail("Expected clipping to fail.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  public void testClippingStart() {
    Timeline timeline = new SinglePeriodTimeline(C.msToUs(TEST_PERIOD_DURATION_US), true);

    Timeline clippedTimeline = getClippedTimeline(timeline, TEST_CLIP_AMOUNT_US,
        TEST_PERIOD_DURATION_US);
    assertEquals(TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US,
        clippedTimeline.getWindow(0, window).getDurationUs());
    assertEquals(TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US,
        clippedTimeline.getPeriod(0, period).getDurationUs());
  }

  public void testClippingEnd() {
    Timeline timeline = new SinglePeriodTimeline(C.msToUs(TEST_PERIOD_DURATION_US), true);

    Timeline clippedTimeline = getClippedTimeline(timeline, 0,
        TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US);
    assertEquals(TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US,
        clippedTimeline.getWindow(0, window).getDurationUs());
    assertEquals(TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US,
        clippedTimeline.getPeriod(0, period).getDurationUs());
  }

  public void testClippingStartAndEnd() {
    Timeline timeline = new SinglePeriodTimeline(C.msToUs(TEST_PERIOD_DURATION_US), true);

    Timeline clippedTimeline = getClippedTimeline(timeline, TEST_CLIP_AMOUNT_US,
        TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US * 2);
    assertEquals(TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US * 3,
        clippedTimeline.getWindow(0, window).getDurationUs());
    assertEquals(TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US * 3,
        clippedTimeline.getPeriod(0, period).getDurationUs());
  }

  public void testWindowAndPeriodIndices() {
    Timeline timeline = new FakeTimeline(1, 111);
    Timeline clippedTimeline = getClippedTimeline(timeline, TEST_CLIP_AMOUNT_US,
        TEST_PERIOD_DURATION_US - TEST_CLIP_AMOUNT_US);
    new TimelineVerifier(clippedTimeline)
        .assertWindowIds(111)
        .assertPeriodCounts(1)
        .assertPreviousWindowIndices(ExoPlayer.REPEAT_MODE_OFF, C.INDEX_UNSET)
        .assertPreviousWindowIndices(ExoPlayer.REPEAT_MODE_ONE, 0)
        .assertPreviousWindowIndices(ExoPlayer.REPEAT_MODE_ALL, 0)
        .assertNextWindowIndices(ExoPlayer.REPEAT_MODE_OFF, C.INDEX_UNSET)
        .assertNextWindowIndices(ExoPlayer.REPEAT_MODE_ONE, 0)
        .assertNextWindowIndices(ExoPlayer.REPEAT_MODE_ALL, 0);
  }

  /**
   * Wraps the specified timeline in a {@link ClippingMediaSource} and returns the clipped timeline.
   */
  private static Timeline getClippedTimeline(Timeline timeline, long startMs, long endMs) {
    MediaSource mediaSource = new StubMediaSource(timeline);
    return TimelineTest.extractTimelineFromMediaSource(
        new ClippingMediaSource(mediaSource, startMs, endMs));
  }

}
