package com.Classroom_ai.Classroom.api;

/** Whether the caller is signed in, and if not, whether their one free use of the AI tools is still unused. */
public record TrialStatusResponse(boolean signedIn, boolean trialAvailable) {
}
