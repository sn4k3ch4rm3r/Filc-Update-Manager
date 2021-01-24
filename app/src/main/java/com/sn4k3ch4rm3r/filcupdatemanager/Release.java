package com.sn4k3ch4rm3r.filcupdatemanager;

public class Release {
    private String version;
    private String downloadURL;
    public Release(String version, String downloadURL) {
        this.version = version;
        this.downloadURL = downloadURL;
    }

    public String getVersion() {
        return version;
    }

    public String getDownloadURL() {
        return downloadURL;
    }
}
