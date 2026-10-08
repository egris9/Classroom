package com.Classroom_ai.Classroom.generation;

class FakeTextGenerationContractTest extends TextGenerationContract {

    @Override
    TextGeneration adapter() {
        return new FakeTextGeneration();
    }
}
