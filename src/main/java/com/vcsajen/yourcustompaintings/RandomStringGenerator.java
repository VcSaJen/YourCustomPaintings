package com.vcsajen.yourcustompaintings;

import java.security.SecureRandom;
import java.math.BigInteger;

/**
 * Created by VcSaJen on 30.07.2017 18:29.
 */
public class RandomStringGenerator {
    private SecureRandom random = new SecureRandom();
    private int characterCount;

    public String generate() {
        return new BigInteger(characterCount*5, random).toString(32);
    }

    public RandomStringGenerator(int characterCount) {
        this.characterCount=characterCount;
    }
}
