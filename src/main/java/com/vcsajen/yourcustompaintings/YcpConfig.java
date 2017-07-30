package com.vcsajen.yourcustompaintings;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

/**
 * Configuration class
 * Created by VcSaJen on 26.07.2017 21:15.
 */
@ConfigSerializable
public class YcpConfig {
    @Setting(value="max-img-file-size", comment="Maximal size of imported image file in bytes. This shouldn't be TOO big, otherwise it will cause OutOfMemory errors, even if file itself is small.")
    private int maxImgFileSize = 1048576;

    @Setting(value="progress-report-time", comment="Report progress of operation where possible every X milliseconds")
    private int progressReportTime = 30000;

    @Setting(value="debug-mode", comment="Debug mode. Will write some trash info in plugin's config folder, so not recommended")
    private boolean debugMode = false;

    public int getProgressReportTime() {
        return progressReportTime;
    }

    public int getMaxImgFileSize() {
        return maxImgFileSize;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public YcpConfig() {

    }
}
