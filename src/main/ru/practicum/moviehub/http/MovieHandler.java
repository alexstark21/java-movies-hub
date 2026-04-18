package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Optional;

import static ru.practicum.moviehub.api.ErrorResponse.messageError;

public class MovieHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MovieHandler(MoviesStore moviesStore) {
        super();
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if (!path.startsWith("/movies/")) {
            sendNotFound(ex);
            return;
        }

        String idPart = path.substring("/movies/".length());
        if (!idPart.matches("^\\d+$")) {
            sendJson(ex, 400, gson.toJson(messageError("Некорректный ID")));
            return;
        }

        long id = Long.parseLong(idPart);

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGetById(ex, id);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(ex, id);
            } else {
                sendUnsupportedMethod(ex);
            }
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(messageError("Внутренняя ошибка сервера")));
        }
    }

    private void handleGetById(HttpExchange ex, long id) throws IOException {
        Optional<Movie> movieOptional = moviesStore.findById(id);

        if (movieOptional.isPresent()) {
            Movie movie = movieOptional.get();
            sendJson(ex, 200, gson.toJson(movie));
        } else {
            sendJson(ex, 404, gson.toJson(messageError("Фильм не найден")));
        }
    }

    private void handleDelete(HttpExchange ex, long id) throws IOException {
        boolean removed = moviesStore.remove(id);

        if (removed) {
            sendNoContent(ex);
        } else {
            sendNotFound(ex);
        }
    }
}
