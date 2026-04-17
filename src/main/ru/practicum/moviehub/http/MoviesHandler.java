package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.moviehub.api.ErrorResponse.*;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        try {
            if (method.equalsIgnoreCase("GET")) {
                handleGet(ex, path);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(ex, path);
            } else if (method.equalsIgnoreCase("DELETE")) {
                if (path.matches("/movies/\\d+")) {
                    handleDelete(ex, path);
                } else {
                    sendJson(ex, 400, gson.toJson(messageError("Некорректный ID")));
                }
            } else {
                sendUnsupportedMethod(ex);
            }
        } catch (Exception e) {
            sendJson(ex,500, gson.toJson(messageError("Внутренняя ошибка сервера")));
        }
    }

    private void handleGet(HttpExchange ex, String path) throws IOException {
        if (path.equals("/movies")) {
            String query = ex.getRequestURI().getQuery();
            if (query == null || query.isBlank()) {
                List<Movie> movies = moviesStore.findAll();
                if (movies.isEmpty()) {
                    sendJson(ex, 200, "[]");
                } else {
                    String json = gson.toJson(movies);
                    sendJson(ex, 200, json);
                }
            } else  {
                filterByYear(ex, query);
            }
        } else if (path.matches("/movies/\\d+")) {
            handleGetById(ex, path);
        } else if (path.startsWith("/movies/")) {
            sendJson(ex, 400, gson.toJson(messageError("Некорректный ID")));
        }
    }

    private void handlePost(HttpExchange ex, String path) throws IOException {
        if (path.equals("/movies")) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.equals("application/json; charset=UTF-8")) {
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

    private void handleGetById(HttpExchange ex, String path) throws IOException {
        String idPart = path.substring("/movies/".length());
        long id = Long.parseLong(idPart);

        Movie movie = moviesStore.findById(id);

        if (movie != null) {
            sendJson(ex, 200, gson.toJson(movie));
        } else {
            sendJson(ex, 404, gson.toJson(messageError("Фильм не найден")));
        }
    }

    private void handleDelete(HttpExchange ex, String path) throws IOException {
        String idPart = path.substring("/movies/".length());
        long id = Long.parseLong(idPart);

        boolean removed = moviesStore.remove(id);

        if (removed) {
            sendNoContent(ex);
        } else {
            sendNotFound(ex);
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

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название не должно быть длиннее 100 символов");
        }

        int currentYear = Year.now().getValue();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между 1888 и 2026");
        }
        return errors;
    }

    private Integer extractYear(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals("year")) {
                return Integer.parseInt(pair[1]);
            }
        }
        return null;
    }
}
