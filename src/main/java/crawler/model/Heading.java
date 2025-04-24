package main.java.crawler.model;

public class Heading {
    private final int level; // 1 = h1, 2 = h2, ...
    private final String text;

    public Heading(int level, String text) {
        this.level = level;
        this.text = text;
    }

    public int getLevel() {
        return level;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Heading{" + "level=" + level + ", text='" + text + '\'' + '}';
    }
}

