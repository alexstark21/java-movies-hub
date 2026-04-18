package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Long, Movie> movies = new HashMap<>();
    private long id = 1;


    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Movie save(Movie movie) {
        if (movie.getId() == null) {
            movie.setId(id++);
        }
        movies.put(movie.getId(), movie);
        return movie;
    }

    public Optional<Movie> findById(long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean remove(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        id = 1;
    }
}