package com.nidoham.hdstreamztv.model;

public class ChannelUrl {
    private String tittle = "";
    private String link = "";

    public ChannelUrl(String tittle, String link) {
        this.tittle = tittle;
        this.link = link;
    }

    public ChannelUrl() {}

    public String getTittle() {
        return this.tittle;
    }

    public void setTittle(String tittle) {
        this.tittle = tittle;
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
