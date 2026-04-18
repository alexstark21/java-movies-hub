package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static ru.practicum.moviehub.api.ErrorResponse.*;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_RELEASE_YEAR = 1888;

    public MoviesHandler(MoviesStore moviesStore) {
        super();
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        try {
            if (method.equalsIgnoreCase("GET")) {
                handleGet(ex);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(ex, path);
            } else {
                sendUnsupportedMethod(ex);
            }
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(messageError("Внутренняя ошибка сервера")));
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        if (query == null || query.isBlank()) {
            List<Movie> movies = moviesStore.findAll();
            if (movies.isEmpty()) {
                sendJson(ex, 200, "[]");
            } else {
                String json = gson.toJson(movies);
                sendJson(ex, 200, json);
            }
        } else {
            filterByYear(ex, query);
        }
    }

    private void handlePost(HttpExchange ex, String path) throws IOException {
        if (path.equals("/movies")) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
                sendUnsupportedMediaType(ex);
                return;
            }

            String requestBody = readRequestBody(ex);
            Movie movie;
            try {
                movie = gson.fromJson(requestBody, Movie.class);
                if (movie == null) {
                    sendJson(ex, 400, gson.toJson(messageError("Тело запроса не может быть пустым")));
                    return;
                }
            } catch (JsonSyntaxException e) {
                sendJson(ex, 400, gson.toJson(messageError("Некорректный JSON")));
                return;
            }

            List<String> validationErrors = validateMovie(movie);
            if (!validationErrors.isEmpty()) {
                sendJson(ex, 422, gson.toJson(validationError(validationErrors)));
                return;
            }
            Movie savedMovie = moviesStore.save(movie);
            sendJson(ex, 201, gson.toJson(savedMovie));
        }
    }

    private void filterByYear(HttpExchange ex, String query) throws IOException {
        try {
            Integer year = extractYear(query);

            if (year == null) {
                sendJson(ex, 400, gson.toJson(messageError("Некорректный параметр запроса — 'year'")));
                return;
            }

            List<Movie> filtered = moviesStore.findAll().stream()
                    .filter(movie -> movie.getYear() == year)
                    .collect(Collectors.toList());

            sendJson(ex, 200, gson.toJson(filtered));

        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(messageError("Некорректный параметр запроса — 'year'")));
        }
    }

    private String readRequestBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        int maxReleaseYear = Year.now().getValue() + 1;

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            errors.add("название не должно быть длиннее 100 символов");
        }

        if (movie.getYear() < MIN_RELEASE_YEAR || movie.getYear() > maxReleaseYear) {
            errors.add(String.format("год должен быть между %d и %d", MIN_RELEASE_YEAR, maxReleaseYear));
        }
        return errors;
    }

    private Integer extractYear(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] pair = param.split("=");
            try {
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                if (pair.length == 2 && key.equals("year")) {
                    String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    return Integer.parseInt(value);
                }
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
