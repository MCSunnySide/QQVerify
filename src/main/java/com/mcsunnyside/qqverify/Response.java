package com.mcsunnyside.qqverify;

import lombok.Data;

@Data
public class Response {
    /**
     * name : KrisJelbring
     * id : 7125ba8b1c864508b92bb5c042ccfe2b
     */
    private String name;
    private String id;
    private boolean legacy;
    private boolean demo;

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
