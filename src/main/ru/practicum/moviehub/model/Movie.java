package ru.practicum.moviehub.model;

public class Movie {
    private Long id;
    private String title;
    private int year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}