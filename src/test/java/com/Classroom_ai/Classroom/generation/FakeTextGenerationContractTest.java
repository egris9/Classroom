package com.Classroom_ai.Classroom.generation;

class FakeTextGenerationContractTest extends TextGenerationContract {

    @Override
    boolean quotesItsSource() {
        return true;
    }

    @Override
    TextGeneration adapter() {
        return new FakeTextGeneration();
    }
}
