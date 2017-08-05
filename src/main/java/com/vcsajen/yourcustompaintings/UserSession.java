package com.vcsajen.yourcustompaintings;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by VcSaJen on 05.08.2017 23:06.
 */
public class UserSession {
    private AtomicBoolean inProcess;

    public AtomicBoolean getInProcess() {
        return inProcess;
    }

    public void setInProcess(AtomicBoolean inProcess) {
        this.inProcess = inProcess;
    }

    public UserSession(boolean inProcess) {
        this.inProcess = new AtomicBoolean(inProcess);
    }
}
