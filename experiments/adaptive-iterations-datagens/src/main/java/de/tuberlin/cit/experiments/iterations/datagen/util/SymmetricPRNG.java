package de.tuberlin.cit.experiments.iterations.datagen.util;


public interface SymmetricPRNG {

    void seed(long seed);

    void skipTo(long pos);

    double next();

    int nextInt(int k);
}
